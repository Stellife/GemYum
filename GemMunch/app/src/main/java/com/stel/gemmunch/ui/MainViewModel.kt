package com.stel.gemmunch.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stel.gemmunch.AppContainer
import com.stel.gemmunch.agent.ModelDownloader
import com.stel.gemmunch.agent.ModelRegistry
import com.stel.gemmunch.agent.MultiDownloadState
import com.stel.gemmunch.data.InitializationMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private const val TAG = "MainViewModel"

/** Defines the overall state of the application for the main UI. */
data class MainUiState(
    val downloadState: MultiDownloadState = MultiDownloadState.Checking,
    val isAiReady: Boolean = false,
    val initializationProgress: String? = null,
    val initializationReport: String? = null,
    val healthConnectAvailable: Boolean = false,
    val healthConnectPermissionsGranted: Boolean = false
)

class MainViewModel(
    private val application: Application,
    val appContainer: AppContainer
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    val initMetrics: InitializationMetrics = 
        (application as com.stel.gemmunch.GemMunchApplication).initMetrics
    
    // Expose live metrics updates
    val initMetricsUpdates = initMetrics.liveUpdates

    init {
        // Start the AI initialization process in the background as soon as the app starts.
        viewModelScope.launch(Dispatchers.IO) {
            initMetrics.startPhase("ViewModelInitialization")
            initializeAiComponents()
        }

        // Check Health Connect status in parallel
        viewModelScope.launch {
            checkHealthConnectStatus()
        }
    }

    /**
     * Checks if models are downloaded, and if so, initializes the AppContainer.
     * If not, it sets the state to Idle to prompt the user to download them.
     */
    private suspend fun initializeAiComponents() {
        _uiState.update { it.copy(isAiReady = false, downloadState = MultiDownloadState.Checking) }

        initMetrics.startSubPhase("ViewModelInitialization", "CheckModels")
        val essentialModels = ModelRegistry.getEssentialModelsForSetup()
        val modelFiles = essentialModels.mapNotNull { asset ->
            val file = File(application.filesDir, asset.fileName)
            if (file.exists() && file.length() > 0) asset.logicalName to file else null
        }.toMap()
        initMetrics.endSubPhase("ViewModelInitialization", "CheckModels", 
            "Found ${modelFiles.size}/${essentialModels.size} models")

        if (modelFiles.size == essentialModels.size) {
            try {
                // Update UI to show we're initializing
                _uiState.update { it.copy(
                    downloadState = MultiDownloadState.AllComplete(modelFiles), 
                    initializationProgress = "Initializing AI models..."
                ) }
                
                // All models are present, initialize the heavy AI components.
                initMetrics.startSubPhase("ViewModelInitialization", "InitializeAppContainer")
                appContainer.initialize(modelFiles)
                initMetrics.endSubPhase("ViewModelInitialization", "InitializeAppContainer")
                
                // Start continuous pre-warming for instant session availability
                appContainer.startContinuousPrewarming()
                
                initMetrics.endPhase("ViewModelInitialization")
                
                // Generate the final report
                val report = initMetrics.generateReport()
                
                _uiState.update { it.copy(
                    downloadState = MultiDownloadState.AllComplete(modelFiles), 
                    isAiReady = true,
                    initializationProgress = null,
                    initializationReport = report
                ) }
                
                Log.i(TAG, "AI Components initialized successfully.")
                Log.i(TAG, "\n$report")
            } catch (e: Exception) {
                Log.e(TAG, "AI Component Initialization Failed", e)
                val errorMessage = when {
                    e.message?.contains("database", ignoreCase = true) == true -> 
                        "Failed to initialize nutrition database. Please try reinstalling the app."
                    e.message?.contains("model", ignoreCase = true) == true -> 
                        "Failed to load AI model. Please check your device has enough storage."
                    else -> 
                        "Failed to initialize app components: ${e.message ?: "Unknown error"}"
                }
                _uiState.update { it.copy(downloadState = MultiDownloadState.Failed(errorMessage)) }
                initMetrics.endPhase("ViewModelInitialization")
            }
        } else {
            // Models are missing, move to Idle state to show the download button.
            _uiState.update { it.copy(downloadState = MultiDownloadState.Idle) }
            initMetrics.endPhase("ViewModelInitialization")
            Log.w(TAG, "Essential models not found. Waiting for user to download.")
        }
    }

    /**
     * Starts the download process for all required models.
     */
    fun startModelDownload() {
        if (_uiState.value.downloadState is MultiDownloadState.InProgress) return

        viewModelScope.launch {
            ModelDownloader.downloadAllModels(application, ModelRegistry.getAllModels())
                .collect { downloadStatus ->
                    _uiState.update { it.copy(downloadState = downloadStatus) }
                    // When download completes, trigger the AI initialization.
                    if (downloadStatus is MultiDownloadState.AllComplete) {
                        viewModelScope.launch(Dispatchers.IO) {
                            initializeAiComponents()
                        }
                    }
                }
        }
    }
    
    /**
     * Downloads only the optional language models.
     */
    fun downloadOptionalModels() {
        if (_uiState.value.downloadState is MultiDownloadState.InProgress) return
        
        viewModelScope.launch {
            val languageModels = ModelRegistry.getOptionalLanguageModels()
            ModelDownloader.downloadAllModels(application, languageModels)
                .collect { downloadStatus ->
                    _uiState.update { it.copy(downloadState = downloadStatus) }
                }
        }
    }
    
    /**
     * Bypasses setup and proceeds to main screen even without models.
     * This allows users to access model selection if initialization fails.
     */
    fun bypassSetup() {
        Log.w(TAG, "User bypassing setup - proceeding without AI models")
        _uiState.update { it.copy(
            downloadState = MultiDownloadState.AllComplete(emptyMap()),
            isAiReady = false,
            initializationProgress = "AI not initialized - manual configuration required"
        ) }
    }
    
    /**
     * Checks Health Connect availability and permissions status.
     */
    private suspend fun checkHealthConnectStatus() {
        val healthConnect = appContainer.healthConnectManager
        val isAvailable = healthConnect.isHealthConnectAvailable()
        
        _uiState.update { it.copy(healthConnectAvailable = isAvailable) }
        
        if (isAvailable) {
            val hasPermissions = healthConnect.hasNutritionPermissions()
            _uiState.update { it.copy(healthConnectPermissionsGranted = hasPermissions) }
            
            Log.i(TAG, "Health Connect available: $isAvailable, permissions granted: $hasPermissions")
        } else {
            Log.i(TAG, "Health Connect not available on this device")
        }
    }
    
    /**
     * Re-checks Health Connect permissions after a permission request.
     */
    fun refreshHealthConnectPermissions() {
        viewModelScope.launch {
            checkHealthConnectStatus()
        }
    }
    
    /**
     * Clears acceleration cache and triggers re-analysis.
     */
    fun reanalyzeAcceleration() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.i(TAG, "User requested acceleration re-analysis")
            
            try {
                // Run comprehensive acceleration test
                Log.i(TAG, "Running comprehensive acceleration API test...")
                val tester = com.stel.gemmunch.utils.ComprehensiveAccelerationTest(application)
                tester.runAllTests()
                
                // Update UI state to show that re-analysis was requested
                _uiState.update { it.copy(
                    initializationProgress = "Acceleration test complete - check logs"
                )}
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle re-analysis request", e)
            }
        }
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, appContainer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}