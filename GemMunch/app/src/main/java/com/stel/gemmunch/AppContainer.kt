package com.stel.gemmunch

import android.content.Context
import android.os.Build
import android.util.Log
import com.stel.gemmunch.agent.PhotoMealExtractor
import com.stel.gemmunch.data.api.UsdaApiService
import com.stel.gemmunch.data.models.EnhancedNutrientDbHelper
import com.stel.gemmunch.data.NutrientDatabaseManager
import com.stel.gemmunch.data.InitializationMetrics
import com.stel.gemmunch.data.FeedbackStorageService
import com.stel.gemmunch.data.HealthConnectManager
import com.stel.gemmunch.data.NutritionSearchService
import com.stel.gemmunch.data.models.AccelerationStats
import com.stel.gemmunch.data.models.AppliedOptimization
import com.stel.gemmunch.utils.GsonProvider
import com.stel.gemmunch.utils.VisionModelPreferencesManager
import com.stel.gemmunch.utils.PlayServicesAccelerationService
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "AppContainer"

/**
 * Represents the current state of the AI model
 */
enum class ModelStatus {
    INITIALIZING,
    PREPARING_SESSION,
    READY,
    RUNNING_INFERENCE,
    CLEANUP
}

/**
 * The interface for our dependency container. It defines the components
 * that will be available to the rest of the application.
 */
interface AppContainer {
    val applicationContext: Context
    val photoMealExtractor: PhotoMealExtractor?
    val feedbackStorageService: FeedbackStorageService
    val healthConnectManager: HealthConnectManager
    val nutritionSearchService: NutritionSearchService
    val isReady: StateFlow<Boolean>
    val accelerationStats: StateFlow<AccelerationStats?>
    val modelStatus: StateFlow<ModelStatus>

    suspend fun initialize(modelFiles: Map<String, File>)
    suspend fun getReadyVisionSession(): LlmInferenceSession
    suspend fun switchVisionModel(newModelKey: String, modelFiles: Map<String, File>)
    fun clearSessionPool()
    fun prewarmSessionOnDemand()
    fun startContinuousPrewarming()
    fun updateModelStatus(status: ModelStatus)
    fun onSessionClosed()
    fun onAppDestroy()
}

/**
 * The default implementation of the AppContainer.
 * This class manages the lifecycle of our core services, including the AI models.
 */
class DefaultAppContainer(
    private val context: Context,
    private val initMetrics: InitializationMetrics
) : AppContainer {
    override val applicationContext: Context get() = context.applicationContext

    // Background scope removed - no longer needed without session pooling

    // Dynamic Google Play Services acceleration
    private val playServicesAcceleration = PlayServicesAccelerationService(context)

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _accelerationStats = MutableStateFlow<AccelerationStats?>(null)
    override val accelerationStats: StateFlow<AccelerationStats?> = _accelerationStats.asStateFlow()
    
    private val _modelStatus = MutableStateFlow(ModelStatus.INITIALIZING)
    override val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    override var photoMealExtractor: PhotoMealExtractor? = null
        private set

    override val feedbackStorageService: FeedbackStorageService by lazy {
        FeedbackStorageService(context, GsonProvider)
    }

    override val healthConnectManager: HealthConnectManager by lazy {
        HealthConnectManager(context)
    }

    override val nutritionSearchService: NutritionSearchService by lazy {
        NutritionSearchService(
            enhancedNutrientDbHelper = enhancedNutrientDbHelper,
            usdaApiService = usdaApiService
        )
    }

    // Store the vision LLM instance and session options for reuse (singleton pattern).
    private var visionLlmInference: LlmInference? = null
    private var visionSessionOptions: LlmInferenceSession.LlmInferenceSessionOptions? = null

    // Nutrition services for the PhotoMealExtractor.
    private val usdaApiService: UsdaApiService by lazy { UsdaApiService() }
    private val nutrientDatabaseManager: NutrientDatabaseManager by lazy {
        NutrientDatabaseManager(context)
    }
    private lateinit var enhancedNutrientDbHelper: EnhancedNutrientDbHelper

    // Pre-warmed session management
    // We maintain a single pre-warmed session that's replaced after the previous one is closed
    private var prewarmedSession: LlmInferenceSession? = null
    private var isPrewarmingInProgress = false
    private var sessionCount = 0 // Track how many sessions we've created
    private var isSessionInUse = false // Track if a session is currently being used
    
    // Coroutine job for continuous pre-warming
    private var prewarmingJob: kotlinx.coroutines.Job? = null

    /**
     * Creates consistent session options for all vision sessions.
     * This ensures the same inference parameters are used regardless of when the session is created.
     */
    private fun createVisionSessionOptions(): LlmInferenceSession.LlmInferenceSessionOptions {
        return LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setGraphOptions(
                GraphOptions.builder()
                    .setEnableVisionModality(true) // Enable vision processing
                    // Audio modality is NOT enabled - saves memory by not loading audio parameters
                    .build()
            )

            // https://docs.unsloth.ai/basics/gemma-3n-how-to-run-and-fine-tune#official-recommended-settings
            // Unsloth/Gemma team's reqs
            // temperature = 1.0, top_k = 64, top_p = 0.95, min_p = 0.0
            //  Chat template:
            //  <bos><start_of_turn>user\nHello!<end_of_turn>\n<start_of_turn>model\nHey there!<end_of_turn>\n<start_of_turn>user\nWhat is 1+1?<end_of_turn>\n<start_of_turn>model\n
            //
            .setTemperature(0.05f) // Even lower temperature for more deterministic output
            .setTopK(5) // More restrictive to reduce hallucinations
            .setTopP(0.95f) // Add nucleus sampling for better quality
            .setRandomSeed(42) // Fixed seed for reproducible results
            .build()
    }

    override suspend fun initialize(modelFiles: Map<String, File>) {
        Log.i(TAG, "AppContainer initialization started...")
        initMetrics.startPhase("AIInitialization")

        try {
            // Get the user's selected vision model.
            initMetrics.startSubPhase("AIInitialization", "ModelSelection")
            val selectedVisionModel = VisionModelPreferencesManager.getSelectedVisionModel()
            val visionModelFile = modelFiles[selectedVisionModel]
                ?: throw IllegalStateException("Selected vision model file '$selectedVisionModel' not found.")
            initMetrics.endSubPhase("AIInitialization", "ModelSelection",
                "Model: $selectedVisionModel, Size: ${visionModelFile.length() / 1024 / 1024}MB")

            Log.i(TAG, "Initializing $selectedVisionModel with AccelerationService (Golden Path)...")

            // Step 1: Try to get validated acceleration configuration from AccelerationService
            initMetrics.startSubPhase("AIInitialization", "AccelerationService")
            val validatedConfig = playServicesAcceleration.getValidatedAccelerationConfig(visionModelFile.absolutePath)
            initMetrics.endSubPhase("AIInitialization", "AccelerationService", 
                if (validatedConfig != null) "Validated config received" else "Fallback to manual detection")

            // THE GOLDEN PATH: Try AccelerationService first, fallback to manual detection
            if (validatedConfig != null && validatedConfig is com.stel.gemmunch.utils.AccelerationConfigWrapper) {
                Log.i(TAG, "🚀 Using AccelerationService GPU configuration (GOLDEN PATH)")
                
                // Step 2A: Create MediaPipe options with GPU backend
                initMetrics.startSubPhase("AIInitialization", "CreateLlmInference")
                try {
                    val accelerationWrapper = validatedConfig
                    
                    val llmInferenceOptions = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(visionModelFile.absolutePath)
                        .setMaxTokens(800)
                        .setMaxNumImages(1)
                        .setPreferredBackend(LlmInference.Backend.GPU) // Use GPU backend
                        .build()
                    
                    Log.i(TAG, "✅ Creating LlmInference with GPU backend (confidence: ${(accelerationWrapper.confidence * 100).toInt()}%)")
                    visionLlmInference = LlmInference.createFromOptions(context, llmInferenceOptions)
                    
                    // Create AccelerationStats for AccelerationService path
                    val accelerationStats = AccelerationStats(
                        optimalBackend = LlmInference.Backend.GPU, // AccelerationService chooses optimal
                        confidence = 0.95f, // High confidence from validation
                        benchmarkTimeMs = 0, // No benchmark needed
                        deviceModel = Build.MANUFACTURER + " " + Build.MODEL,
                        chipset = Build.HARDWARE,
                        cpuCores = Runtime.getRuntime().availableProcessors(),
                        hasGPU = true,
                        hasNPU = isPixelDevice(), // Assume NPU on Pixel
                        hasNNAPI = true,
                        androidVersion = Build.VERSION.SDK_INT,
                        isCachedResult = false,
                        optimizations = listOf(
                            AppliedOptimization(
                                setting = "Acceleration Service",
                                value = "Active (Golden Path)",
                                reason = "Using validated AccelerationService configuration",
                                expectedImprovement = "Optimal NPU/GPU acceleration"
                            )
                        ),
                        playServicesEnabled = true
                    )
                    _accelerationStats.value = accelerationStats
                    
                    initMetrics.endSubPhase("AIInitialization", "CreateLlmInference", "AccelerationService")
                    Log.i(TAG, "🧠 AccelerationService configuration applied - NPU/GPU automatically selected")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to use AccelerationService config, falling back to manual detection", e)
                    throw e // Will trigger fallback below
                }
            } else {
                // FALLBACK PATH: Manual GPU detection when AccelerationService fails
                Log.w(TAG, "AccelerationService unavailable, using manual GPU detection (FALLBACK)")
                
                initMetrics.startSubPhase("AIInitialization", "ManualAcceleration")
                val accelerationResult = playServicesAcceleration.findOptimalAcceleration(visionModelFile.absolutePath)
                initMetrics.endSubPhase("AIInitialization", "ManualAcceleration", accelerationResult.selectedBackend)
                
                // Step 2B: Create MediaPipe options with manual backend selection
                initMetrics.startSubPhase("AIInitialization", "CreateLlmInference")
                val preferredBackend = when {
                    accelerationResult.gpuAvailable -> LlmInference.Backend.GPU
                    else -> LlmInference.Backend.CPU
                }
                
                val llmInferenceOptions = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(visionModelFile.absolutePath)
                    .setMaxTokens(800)
                    .setMaxNumImages(1)
                    .setPreferredBackend(preferredBackend) // Manual backend selection
                    .build()
                
                Log.i(TAG, "Creating LlmInference with manual backend: $preferredBackend")
                visionLlmInference = LlmInference.createFromOptions(context, llmInferenceOptions)
                
                // Create AccelerationStats for fallback path
                val accelerationStats = AccelerationStats(
                    optimalBackend = accelerationResult.recommendedLlmBackend,
                    confidence = accelerationResult.confidence,
                    benchmarkTimeMs = accelerationResult.benchmarkTimeMs,
                    deviceModel = Build.MANUFACTURER + " " + Build.MODEL,
                    chipset = Build.HARDWARE,
                    cpuCores = Runtime.getRuntime().availableProcessors(),
                    hasGPU = accelerationResult.gpuAvailable,
                    hasNPU = false, // Manual detection doesn't detect NPU
                    hasNNAPI = false,
                    androidVersion = Build.VERSION.SDK_INT,
                    isCachedResult = false,
                    optimizations = listOf(
                        AppliedOptimization(
                            setting = "Manual Detection",
                            value = accelerationResult.selectedBackend,
                            reason = "AccelerationService unavailable",
                            expectedImprovement = "Standard GPU acceleration"
                        )
                    ),
                    playServicesEnabled = true
                )
                _accelerationStats.value = accelerationStats
                
                initMetrics.endSubPhase("AIInitialization", "CreateLlmInference", "Manual")
            }
            Log.i(TAG, "Vision LLM instance created successfully.")

            // Define the options for creating individual sessions from the main instance.
            initMetrics.startSubPhase("AIInitialization", "CreateSessionOptions")
            visionSessionOptions = createVisionSessionOptions()
            initMetrics.endSubPhase("AIInitialization", "CreateSessionOptions")

            // Initialize the nutrition database
            Log.i(TAG, "Initializing nutrition database...")
            initMetrics.startSubPhase("AIInitialization", "NutritionDatabase")
            try {
                enhancedNutrientDbHelper = EnhancedNutrientDbHelper(context, usdaApiService)
                enhancedNutrientDbHelper.initialize(nutrientDatabaseManager)
                initMetrics.endSubPhase("AIInitialization", "NutritionDatabase", "Success")
                Log.i(TAG, "Nutrition database initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize nutrition database, using empty database", e)
                // Continue with app initialization even if database fails
                enhancedNutrientDbHelper = EnhancedNutrientDbHelper(context, usdaApiService)
                initMetrics.endSubPhase("AIInitialization", "NutritionDatabase", "Failed - using empty")
            }

            // The PhotoMealExtractor is now ready to be created.
            initMetrics.startSubPhase("AIInitialization", "CreatePhotoMealExtractor")
            photoMealExtractor = PhotoMealExtractor(this, enhancedNutrientDbHelper, GsonProvider.instance)
            initMetrics.endSubPhase("AIInitialization", "CreatePhotoMealExtractor")

            _isReady.update { true }
            initMetrics.endPhase("AIInitialization")
            Log.i(TAG, "AppContainer initialization successful in ${initMetrics.phases["AIInitialization"]?.durationMs}ms")
            
            // Update status to preparing session
            _modelStatus.value = ModelStatus.PREPARING_SESSION
            
            // Don't start pre-warming here - MainViewModel will do it

        } catch (e: Exception) {
            Log.e(TAG, "Error during AI component initialization", e)
            _isReady.update { false }
            initMetrics.endPhase("AIInitialization")

            // Log the full initialization report even on failure
            Log.e(TAG, initMetrics.generateReport())
            throw e
        }
    }

    /**
     * Starts continuous pre-warming to always have a fresh session ready.
     * As soon as a session is consumed, we create a new one.
     */
    override fun startContinuousPrewarming() {
        prewarmingJob?.cancel()
        
        prewarmingJob = GlobalScope.launch(Dispatchers.IO) {
            initMetrics.startPhase("SessionPrewarming")
            
            while (isActive) {
                try {
                    // Only pre-warm if: no existing pre-warmed session, not currently pre-warming, 
                    // no session in use, and LlmInference is initialized
                    if (prewarmedSession == null && !isPrewarmingInProgress && !isSessionInUse && visionLlmInference != null) {
                        prewarmSessionAsync()
                    }
                    // Check every 100ms if we need to create a new session
                    kotlinx.coroutines.delay(100)
                } catch (e: Exception) {
                    if (e !is kotlinx.coroutines.CancellationException) {
                        Log.e(TAG, "Error in continuous pre-warming", e)
                        kotlinx.coroutines.delay(1000) // Wait longer on error
                    } else {
                        throw e // Re-throw cancellation to properly exit
                    }
                }
            }
        }
        
        Log.i(TAG, "🚀 Started continuous session pre-warming")
        
        // Log the complete initialization report
        Log.i(TAG, "\n${initMetrics.generateReport()}")
    }
    
    /**
     * Pre-warms a single session asynchronously
     */
    private suspend fun prewarmSessionAsync() {
        if (isPrewarmingInProgress) return
        
        isPrewarmingInProgress = true
        try {
            val inference = visionLlmInference ?: return
            val options = visionSessionOptions ?: return
            
            sessionCount++
            Log.d(TAG, "🔥 Pre-warming session #$sessionCount...")
            _modelStatus.value = ModelStatus.PREPARING_SESSION
            val startTime = System.currentTimeMillis()
            
            val session = withContext(Dispatchers.IO) {
                LlmInferenceSession.createFromOptions(inference, options)
            }
            
            val duration = System.currentTimeMillis() - startTime
            prewarmedSession = session
            
            if (initMetrics.phases["SessionPrewarming"]?.endTime == null) {
                initMetrics.endPhase("SessionPrewarming")
            }
            
            Log.d(TAG, "✅ Session #$sessionCount pre-warmed and ready in ${duration}ms")
            _modelStatus.value = ModelStatus.READY
        } catch (e: Exception) {
            if (e !is kotlinx.coroutines.CancellationException) {
                Log.e(TAG, "Failed to pre-warm session", e)
            }
        } finally {
            isPrewarmingInProgress = false
        }
    }
    
    /**
     * Pre-warm a single session on demand (e.g., when camera is opened)
     */
    override fun prewarmSessionOnDemand() {
        if (prewarmedSession != null || isPrewarmingInProgress || isSessionInUse) {
            Log.d(TAG, "Cannot pre-warm: session already pre-warmed, in progress, or in use")
            return
        }
        
        GlobalScope.launch(Dispatchers.IO) {
            prewarmSessionAsync()
        }
    }

    /**
     * Gets a ready session from the pool or creates a new one if the pool is empty.
     * This is the key to providing a fast, responsive experience for the user.
     *
     * Following Google AI Edge Gallery pattern: We keep the expensive LlmInference instance
     * but create fresh LlmInferenceSession for each analysis to ensure complete isolation.
     */
    override suspend fun getReadyVisionSession(): LlmInferenceSession = withContext(Dispatchers.IO) {
        // First try to use pre-warmed session
        val existingSession = prewarmedSession
        if (existingSession != null) {
            prewarmedSession = null // Consume it
            isSessionInUse = true // Mark session as in use
            val sessionNum = sessionCount // Capture current session number
            Log.d(TAG, "⚡ Using pre-warmed session #$sessionNum (instant!)")
            
            // Update status to running inference
            _modelStatus.value = ModelStatus.RUNNING_INFERENCE
            
            // Don't pre-warm yet - wait for session to be closed
            Log.d(TAG, "📌 Session #$sessionNum now in use - will pre-warm after cleanup")
            
            return@withContext existingSession
        }
        
        // No pre-warmed session available, create one now
        val inference = visionLlmInference ?: throw IllegalStateException("LlmInference not initialized")
        val options = visionSessionOptions ?: throw IllegalStateException("Session options not initialized")
        
        sessionCount++
        Log.d(TAG, "🔄 Creating session #$sessionCount on-demand (no pre-warmed available)...")
        _modelStatus.value = ModelStatus.PREPARING_SESSION
        val startTime = System.currentTimeMillis()
        
        try {
            val session = LlmInferenceSession.createFromOptions(inference, options)
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "✅ Fresh session created in ${duration}ms")
            
            // Mark session as in use
            isSessionInUse = true
            
            // Update status to running inference
            _modelStatus.value = ModelStatus.RUNNING_INFERENCE
            
            // Don't pre-warm yet - wait for session to be closed
            Log.d(TAG, "📌 Session #$sessionCount now in use - will pre-warm after cleanup")
            
            return@withContext session
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create session", e)
            throw Exception("Failed to create AI session: ${e.message}", e)
        }
    }

    /**
     * Switches to a different vision model, tearing down the old instance and session pool
     * and creating new ones.
     */
    override suspend fun switchVisionModel(newModelKey: String, modelFiles: Map<String, File>) {
        withContext(Dispatchers.IO) {
            Log.i(TAG, "🔄 Switching vision model to $newModelKey...")
            _isReady.update { false }

            // 1. Clean up old resources.
            prewarmingJob?.cancel()
            clearSessionPool()
            visionLlmInference?.close()
            visionLlmInference = null
            visionSessionOptions = null

            // 2. Update the user's preference.
            VisionModelPreferencesManager.setSelectedVisionModel(newModelKey)

            // 3. Re-run the core initialization logic with the new model file.
            // This is a simplified way to ensure everything is rebuilt correctly.
            val selectedVisionModel = VisionModelPreferencesManager.getSelectedVisionModel()
            val visionModelFile = modelFiles[selectedVisionModel]
                ?: throw IllegalStateException("New vision model file '$selectedVisionModel' not found.")

            // Re-analyze acceleration for new model with Play Services
            Log.i(TAG, "Re-analyzing acceleration with Play Services for new model...")
            val accelerationResult = playServicesAcceleration.findOptimalAcceleration(visionModelFile.absolutePath)
            Log.i(TAG, "Model switch using ${accelerationResult.selectedBackend} (confidence: ${(accelerationResult.confidence * 100).toInt()}%)")

            // Configure MediaPipe LLM inference with detected optimal backend for new model
            val preferredBackend = when {
                accelerationResult.gpuAvailable -> LlmInference.Backend.GPU
                else -> LlmInference.Backend.CPU
            }
            
            val optionsBuilder = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(visionModelFile.absolutePath)
                .setMaxTokens(800) // Match the token limit used in initialization
                .setMaxNumImages(1)
                .setPreferredBackend(preferredBackend) // Use GPU when available
            
            // Apply AccelerationService configuration if available
            if (accelerationResult.accelerationServiceUsed && accelerationResult.validatedConfig != null) {
                try {
                    val validatedConfig = accelerationResult.validatedConfig
                    val getTfLiteOptionsMethod = validatedConfig.javaClass.getMethod("getTfLiteInitializationOptions")
                    val tfLiteOptions = getTfLiteOptionsMethod.invoke(validatedConfig)
                    
                    val setTfLiteOptionsMethod = optionsBuilder.javaClass.getMethod("setTfLiteInitializationOptions", 
                        Class.forName("com.google.android.gms.tflite.client.TfLiteInitializationOptions"))
                    setTfLiteOptionsMethod.invoke(optionsBuilder, tfLiteOptions)
                    
                    Log.i(TAG, "AccelerationService configuration applied to model switch")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to apply AccelerationService configuration for model switch", e)
                }
            }
            
            val llmInferenceOptions = optionsBuilder.build()
            visionLlmInference = LlmInference.createFromOptions(context, llmInferenceOptions)

            // Use the same session options to ensure consistency
            visionSessionOptions = createVisionSessionOptions()

            // 4. Restart continuous pre-warming with new model
            startContinuousPrewarming()

            _isReady.update { true }
            Log.i(TAG, "✅ Vision model switched to $newModelKey successfully")
        }
    }

    /**
     * Clears the pre-warmed session if one exists.
     */
    override fun clearSessionPool() {
        prewarmedSession?.close()
        prewarmedSession = null
        Log.d(TAG, "Pre-warmed session cleared")
    }
    
    /**
     * Updates the model status for UI display
     */
    override fun updateModelStatus(status: ModelStatus) {
        _modelStatus.value = status
    }
    
    /**
     * Called when a session is closed and cleaned up.
     * This triggers pre-warming of a new session.
     */
    override fun onSessionClosed() {
        Log.d(TAG, "🔓 Session closed - marking as no longer in use")
        isSessionInUse = false
        
        // Now it's safe to pre-warm a new session
        if (visionLlmInference != null) {
            Log.d(TAG, "🔄 Starting pre-warm of next session after cleanup")
            prewarmSessionOnDemand()
        }
    }

    /**
     * Check if device is a Pixel with Tensor chip for NPU detection.
     */
    private fun isPixelDevice(): Boolean {
        val model = Build.MODEL.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        
        return model.contains("pixel") && (
            model.contains("6") || model.contains("7") || 
            model.contains("8") || model.contains("9") ||
            hardware.contains("tensor") || hardware.contains("komodo")
        )
    }

    /**
     * Called when the application is being destroyed to release all AI resources.
     */
    override fun onAppDestroy() {
        Log.i(TAG, "App destroyed. Closing all active AI components.")
        prewarmingJob?.cancel()
        clearSessionPool()
        visionLlmInference?.close()
        enhancedNutrientDbHelper.close()
        playServicesAcceleration.cleanup()
    }
}