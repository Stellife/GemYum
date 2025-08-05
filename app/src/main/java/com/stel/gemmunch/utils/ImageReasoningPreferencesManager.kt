package com.stel.gemmunch.utils

import android.content.Context
import android.content.SharedPreferences

object ImageReasoningPreferencesManager {
    private const val PREFS_NAME = "image_reasoning_preferences"
    private const val KEY_IMAGE_REASONING_MODE = "image_reasoning_mode"
    
    enum class ImageReasoningMode(val displayName: String, val description: String) {
        SINGLE_SHOT("Single Shot", "Fast, direct JSON response"),
        REASONING("Reasoning", "Step-by-step analysis with chain of thought")
    }
    
    private var sharedPreferences: SharedPreferences? = null
    
    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun getSelectedMode(): ImageReasoningMode {
        val modeName = sharedPreferences?.getString(KEY_IMAGE_REASONING_MODE, ImageReasoningMode.SINGLE_SHOT.name)
            ?: ImageReasoningMode.SINGLE_SHOT.name
        return ImageReasoningMode.valueOf(modeName)
    }
    
    fun setSelectedMode(mode: ImageReasoningMode) {
        sharedPreferences?.edit()?.putString(KEY_IMAGE_REASONING_MODE, mode.name)?.apply()
    }
}