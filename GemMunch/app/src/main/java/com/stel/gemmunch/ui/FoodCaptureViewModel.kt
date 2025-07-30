package com.stel.gemmunch.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stel.gemmunch.AppContainer
import com.stel.gemmunch.agent.AnalyzedFoodItem
import com.stel.gemmunch.agent.InvalidJsonResponseException
import com.stel.gemmunch.agent.MealAnalysis
import com.stel.gemmunch.utils.VisionModelPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

private const val TAG = "FoodCaptureViewModel"

/** Defines the different states for the food capture UI screen. */
sealed interface FoodCaptureState {
    data object Idle : FoodCaptureState
    data object Loading : FoodCaptureState
    data object SwitchingModel : FoodCaptureState
    data class Success(
        val originalAnalysis: MealAnalysis,
        val editableItems: List<AnalyzedFoodItem> = originalAnalysis.items,
        val lastDeletedItem: Pair<Int, AnalyzedFoodItem>? = null, // For the "Undo" feature
        val shouldNavigateToFeedback: Boolean = false, // Trigger feedback navigation after save
        val analyzedBitmap: Bitmap? = null // Store the analyzed bitmap for metadata extraction
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

    fun analyzeMealPhoto(bitmap: Bitmap) {
        val extractor = appContainer.photoMealExtractor ?: run {
            _uiState.value = FoodCaptureState.Error("Food analyzer is not initialized.")
            return
        }

        viewModelScope.launch {
            _uiState.value = FoodCaptureState.Loading
            try {
                val resultAnalysis = withContext(Dispatchers.IO) {
                    extractor.extract(bitmap)
                }
                
                // Always go to Success state, even for errors, so user can provide feedback
                _uiState.value = FoodCaptureState.Success(
                    originalAnalysis = resultAnalysis,
                    analyzedBitmap = bitmap
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

    fun saveMeal(context: Context) {
        val currentState = _uiState.value
        if (currentState is FoodCaptureState.Success) {
            val totalCalories = currentState.editableItems.sumOf { it.calories }
            // Show confirmation toast
            Toast.makeText(context, "Meal with $totalCalories Calories saved!", Toast.LENGTH_LONG).show()
            
            // Reset to idle state after saving
            reset()
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

    fun reset() {
        _uiState.value = FoodCaptureState.Idle
        // Also reset photo metadata
        photoUniqueId = null
        photoTimestamp = null
        isFromGallery = false
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