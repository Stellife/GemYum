package com.stel.gemmunch.utils

import android.content.Context
import android.content.SharedPreferences

object VisionModelPreferencesManager {
    private const val PREFS_NAME = "vision_model_preferences"
    private const val KEY_SELECTED_VISION_MODEL = "selected_vision_model"
    // Default to the highest quality model.
    private const val DEFAULT_VISION_MODEL = "GEMMA_3N_E4B_MODEL"

    private var sharedPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSelectedVisionModel(): String {
        return sharedPreferences?.getString(KEY_SELECTED_VISION_MODEL, DEFAULT_VISION_MODEL)
            ?: DEFAULT_VISION_MODEL
    }

    fun setSelectedVisionModel(modelKey: String) {
        sharedPreferences?.edit()?.putString(KEY_SELECTED_VISION_MODEL, modelKey)?.apply()
    }

    fun getVisionModelDisplayName(modelKey: String): String {
        return getAvailableVisionModels().find { it.first == modelKey }?.second ?: modelKey
    }

    fun getAvailableVisionModels(): List<Pair<String, String>> {
        // This list defines the models the user can choose from in the UI.
        // Only .task models are compatible with MediaPipe LlmInference for vision
        return listOf(
            "GEMMA_3N_E4B_MODEL" to "Gemma 3n E4B",
            "GEMMA_3N_E2B_MODEL" to "Gemma 3n E2B"
        )
    }
}