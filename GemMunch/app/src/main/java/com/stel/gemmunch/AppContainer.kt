package com.stel.gemmunch

import android.content.Context
import android.util.Log
import com.stel.gemmunch.agent.PhotoMealExtractor
import com.stel.gemmunch.data.api.UsdaApiService
import com.stel.gemmunch.data.models.EnhancedNutrientDbHelper
import com.stel.gemmunch.data.NutrientDatabaseManager
import com.stel.gemmunch.data.InitializationMetrics
import com.stel.gemmunch.utils.GsonProvider
import com.stel.gemmunch.utils.VisionModelPreferencesManager
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "AppContainer"

/**
 * The interface for our dependency container. It defines the components
 * that will be available to the rest of the application.
 */
interface AppContainer {
    val applicationContext: Context
    val photoMealExtractor: PhotoMealExtractor?
    val isReady: StateFlow<Boolean>

    suspend fun initialize(modelFiles: Map<String, File>)
    suspend fun getReadyVisionSession(): LlmInferenceSession
    suspend fun switchVisionModel(newModelKey: String, modelFiles: Map<String, File>)
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

    // A background coroutine scope for pre-warming AI sessions.
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    override var photoMealExtractor: PhotoMealExtractor? = null
        private set

    // Store the vision LLM instance and session options for reuse (singleton pattern).
    private var visionLlmInference: LlmInference? = null
    private var visionSessionOptions: LlmInferenceSession.LlmInferenceSessionOptions? = null

    // Nutrition services for the PhotoMealExtractor.
    private val usdaApiService: UsdaApiService by lazy { UsdaApiService() }
    private val nutrientDatabaseManager: NutrientDatabaseManager by lazy { 
        NutrientDatabaseManager(context) 
    }
    private lateinit var enhancedNutrientDbHelper: EnhancedNutrientDbHelper

    // A pool of ready-to-use vision sessions for instant performance.
    private val visionSessionPool = mutableListOf<LlmInferenceSession>()
    private val maxPoolSize = 2 // Pre-warm 2 sessions for immediate and follow-up use.
    private var isPrewarmingActive = false
    
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

            Log.i(TAG, "Initializing $selectedVisionModel for vision with GPU acceleration...")

            // Create the main LlmInference instance. This is heavy and is only done once.
            initMetrics.startSubPhase("AIInitialization", "CreateLlmInference")
            val llmInferenceOptions = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(visionModelFile.absolutePath)
                .setMaxTokens(800) // Reduced for faster JSON-only responses
                .setMaxNumImages(1)
                .setPreferredBackend(LlmInference.Backend.GPU) // Force GPU for best performance
                .build()
            
            Log.i(TAG, "LLM options configured with GPU backend")
            visionLlmInference = LlmInference.createFromOptions(context, llmInferenceOptions)
            initMetrics.endSubPhase("AIInitialization", "CreateLlmInference", "GPU backend")
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

            // Start pre-warming sessions in the background for a snappy UI experience.
            initMetrics.startPhase("SessionPrewarming")
            startSessionPrewarming()

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
     * Fills the session pool in the background.
     */
    private fun startSessionPrewarming() {
        if (isPrewarmingActive) return
        isPrewarmingActive = true

        backgroundScope.launch {
            Log.i(TAG, "🔥 Starting vision session pre-warming for instant food analysis...")
            repeat(maxPoolSize) { index ->
                try {
                    val subPhaseName = "PrewarmSession${index + 1}"
                    initMetrics.startSubPhase("SessionPrewarming", subPhaseName)
                    val startTime = System.currentTimeMillis()
                    val session = LlmInferenceSession.createFromOptions(visionLlmInference!!, visionSessionOptions!!)
                    val duration = System.currentTimeMillis() - startTime
                    synchronized(visionSessionPool) {
                        visionSessionPool.add(session)
                    }
                    initMetrics.endSubPhase("SessionPrewarming", subPhaseName, "${duration}ms")
                    Log.i(TAG, "✅ Pre-warmed session ${index + 1} ready in ${duration}ms")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to pre-warm session ${index + 1}", e)
                }
            }
            initMetrics.endPhase("SessionPrewarming")
            Log.i(TAG, "🚀 Session pool ready!")
            
            // Log the complete initialization report
            Log.i(TAG, "\n${initMetrics.generateReport()}")
        }
    }

    /**
     * Gets a ready session from the pool or creates a new one if the pool is empty.
     * This is the key to providing a fast, responsive experience for the user.
     */
    override suspend fun getReadyVisionSession(): LlmInferenceSession = withContext(Dispatchers.IO) {
        synchronized(visionSessionPool) {
            if (visionSessionPool.isNotEmpty()) {
                val session = visionSessionPool.removeFirst()
                Log.d(TAG, "⚡ Using pre-warmed session from pool (${visionSessionPool.size} remaining)")
                // Immediately start creating a replacement session in the background.
                backgroundScope.launch {
                    try {
                        val replacementSession = LlmInferenceSession.createFromOptions(visionLlmInference!!, visionSessionOptions!!)
                        synchronized(visionSessionPool) {
                            if (visionSessionPool.size < maxPoolSize) {
                                visionSessionPool.add(replacementSession)
                                Log.d(TAG, "🔄 Replacement session added to pool")
                            } else {
                                replacementSession.close() // Pool is full, close the extra one.
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to create replacement session", e)
                    }
                }
                return@withContext session
            }
        }

        // Fallback: If the pool is empty, create a session directly. This may cause a slight UI delay.
        Log.w(TAG, "⚠️ Session pool empty, creating session directly...")
        LlmInferenceSession.createFromOptions(visionLlmInference!!, visionSessionOptions!!)
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
            synchronized(visionSessionPool) {
                visionSessionPool.forEach { it.close() }
                visionSessionPool.clear()
            }
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

            val llmInferenceOptions = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(visionModelFile.absolutePath)
                .setMaxTokens(1024)
                .setPreferredBackend(LlmInference.Backend.GPU)
                .setMaxNumImages(1)
                .build()
            visionLlmInference = LlmInference.createFromOptions(context, llmInferenceOptions)

            // Use the same session options to ensure consistency
            visionSessionOptions = createVisionSessionOptions()

            // 4. Restart the session pre-warming process.
            isPrewarmingActive = false
            startSessionPrewarming()

            _isReady.update { true }
            Log.i(TAG, "✅ Vision model switched to $newModelKey successfully")
        }
    }

    /**
     * Called when the application is being destroyed to release all AI resources.
     */
    override fun onAppDestroy() {
        Log.i(TAG, "App destroyed. Closing all active AI components.")
        synchronized(visionSessionPool) {
            visionSessionPool.forEach { it.close() }
            visionSessionPool.clear()
        }
        visionLlmInference?.close()
        enhancedNutrientDbHelper.close()
    }
}