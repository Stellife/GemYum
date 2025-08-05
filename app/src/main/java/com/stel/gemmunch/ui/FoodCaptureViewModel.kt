package com.stel.gemmunch.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stel.gemmunch.AppContainer
import com.stel.gemmunch.agent.AnalyzedFoodItem
import com.stel.gemmunch.agent.InvalidJsonResponseException
import com.stel.gemmunch.agent.MealAnalysis
import com.stel.gemmunch.data.models.AnalysisProgress
import com.stel.gemmunch.model.AppMode
import com.stel.gemmunch.model.ChatMessage
import com.stel.gemmunch.utils.VisionModelPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

private const val TAG = "FoodCaptureViewModel"

/** Defines the different states for the food capture UI screen. */
sealed interface FoodCaptureState {
    data object Idle : FoodCaptureState
    data class Loading(val progress: AnalysisProgress = AnalysisProgress()) : FoodCaptureState
    data object SwitchingModel : FoodCaptureState
    data class Success(
        val originalAnalysis: MealAnalysis,
        val editableItems: List<AnalyzedFoodItem> = originalAnalysis.items,
        val lastDeletedItem: Pair<Int, AnalyzedFoodItem>? = null, // For the "Undo" feature
        val shouldNavigateToFeedback: Boolean = false, // Trigger feedback navigation after save
        val analyzedBitmap: Bitmap? = null, // Store the analyzed bitmap for metadata extraction
        val appMode: AppMode = AppMode.SNAP_AND_LOG // Track which mode was used
    ) : FoodCaptureState
    data class Error(val message: String) : FoodCaptureState
}

class FoodCaptureViewModel(
    private val appContainer: AppContainer
) : ViewModel() {

    private val _uiState = MutableStateFlow<FoodCaptureState>(FoodCaptureState.Idle)
    val uiState: StateFlow<FoodCaptureState> = _uiState.asStateFlow()
    
    // Photo metadata that needs to persist across navigation
    var photoUniqueId: String? = null
    var photoTimestamp: Instant? = null
    var isFromGallery: Boolean = false
    
    // User context text for enhanced analysis
    private val _contextText = MutableStateFlow("")
    val contextText: StateFlow<String> = _contextText.asStateFlow()
    
    // Current app mode
    private val _currentAppMode = MutableStateFlow(AppMode.SNAP_AND_LOG)
    val currentAppMode: StateFlow<AppMode> = _currentAppMode.asStateFlow()
    
    
    // Captured image for chat mode
    var capturedImageForChat: String? = null
        private set
    
    fun updateContextText(newText: String) {
        _contextText.value = newText
    }
    
    fun setAppMode(mode: AppMode) {
        _currentAppMode.value = mode
        Log.d(TAG, "App mode changed to: $mode")
    }
    

    fun analyzeMealPhoto(bitmap: Bitmap) {
        val extractor = appContainer.photoMealExtractor ?: run {
            _uiState.value = FoodCaptureState.Error("Food analyzer is not initialized.")
            return
        }

        viewModelScope.launch {
            _uiState.value = FoodCaptureState.Loading()
            try {
                val resultAnalysis = withContext(Dispatchers.IO) {
                    extractor.extract(
                        bitmap = bitmap, 
                        userContext = _contextText.value.takeIf { it.isNotBlank() },
                        appMode = _currentAppMode.value,
                        onProgress = { progress ->
                            // Update UI with progress
                            _uiState.value = FoodCaptureState.Loading(progress)
                        }
                    )
                }
                
                // Always go to Success state, even for errors, so user can provide feedback
                _uiState.value = FoodCaptureState.Success(
                    originalAnalysis = resultAnalysis,
                    analyzedBitmap = bitmap,
                    appMode = _currentAppMode.value
                )
                
                // If it's an error, log it
                if (resultAnalysis.isError) {
                    Log.e(TAG, "AI analysis failed: ${resultAnalysis.errorType} - ${resultAnalysis.errorMessage}")
                    Log.e(TAG, "Raw AI response: ${resultAnalysis.rawAiResponse}")
                }
            } catch (e: Exception) {
                // Only catch unexpected errors, not InvalidJsonResponseException
                Log.e(TAG, "Unexpected error during meal analysis", e)
                val errorMessage = when {
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Analysis timed out. Please try again with a simpler photo."
                    e.message?.contains("memory", ignoreCase = true) == true ->
                        "Not enough memory to process image. Please close other apps and try again."
                    e.message?.contains("session", ignoreCase = true) == true ->
                        "AI model session error. Please restart the app."
                    else ->
                        "Failed to analyze photo: ${e.localizedMessage ?: "Unknown error"}"
                }
                _uiState.value = FoodCaptureState.Error(errorMessage)
            }
        }
    }

    /** Switches the vision model and forces the AppContainer to rebuild its AI instances. */
    fun switchVisionModel(newModelKey: String, modelFiles: Map<String, File>) {
        if (newModelKey == VisionModelPreferencesManager.getSelectedVisionModel()) return

        viewModelScope.launch {
            _uiState.value = FoodCaptureState.SwitchingModel
            try {
                appContainer.switchVisionModel(newModelKey, modelFiles)
                _uiState.value = FoodCaptureState.Idle
            } catch (e: Exception) {
                Log.e(TAG, "Failed to switch vision model", e)
                _uiState.value = FoodCaptureState.Error("Failed to switch model: ${e.localizedMessage}")
            }
        }
    }

    // --- UI State Management Functions ---

    fun deleteItem(index: Int) {
        val currentState = _uiState.value
        if (currentState is FoodCaptureState.Success) {
            val itemToDelete = currentState.editableItems[index]
            val updatedItems = currentState.editableItems.toMutableList().apply { removeAt(index) }
            _uiState.update {
                currentState.copy(
                    editableItems = updatedItems,
                    lastDeletedItem = Pair(index, itemToDelete)
                )
            }
        }
    }

    fun undoDeleteItem() {
        val currentState = _uiState.value
        if (currentState is FoodCaptureState.Success && currentState.lastDeletedItem != null) {
            val (index, itemToRestore) = currentState.lastDeletedItem
            val updatedItems = currentState.editableItems.toMutableList().apply { add(index, itemToRestore) }
            _uiState.update {
                currentState.copy(
                    editableItems = updatedItems,
                    lastDeletedItem = null
                )
            }
        }
    }

    fun updateItem(index: Int, updatedItem: AnalyzedFoodItem) {
        val currentState = _uiState.value
        if (currentState is FoodCaptureState.Success) {
            val updatedItems = currentState.editableItems.toMutableList().also { it[index] = updatedItem }
            _uiState.update { currentState.copy(editableItems = updatedItems, lastDeletedItem = null) }
        }
    }

    
    fun onFeedbackNavigated() {
        // Reset the navigation flag without clearing the analysis
        val currentState = _uiState.value
        if (currentState is FoodCaptureState.Success) {
            _uiState.update {
                currentState.copy(shouldNavigateToFeedback = false)
            }
        }
    }

    fun captureForChat(bitmap: Bitmap) {
        // Save the bitmap for chat mode
        viewModelScope.launch {
            try {
                // Save bitmap to a temporary file
                val tempFile = withContext(Dispatchers.IO) {
                    val file = File(appContainer.applicationContext.cacheDir, "chat_image_${System.currentTimeMillis()}.jpg")
                    file.outputStream().use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    file
                }
                capturedImageForChat = tempFile.absolutePath
                Log.d(TAG, "Image captured for chat: $capturedImageForChat")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to capture image for chat", e)
            }
        }
    }
    
    fun reset() {
        _uiState.value = FoodCaptureState.Idle
        // Also reset photo metadata
        photoUniqueId = null
        photoTimestamp = null
        isFromGallery = false
        // Clear context text
        _contextText.value = ""
        // Reset to default app mode
        _currentAppMode.value = AppMode.SNAP_AND_LOG
        // Clear captured image for chat
        capturedImageForChat = null
        
        // Note: We no longer clear the session pool here to preserve pre-warmed sessions
        // Sessions are already isolated and can be safely reused
    }
}

class FoodCaptureViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FoodCaptureViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FoodCaptureViewModel(appContainer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}