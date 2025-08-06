package com.stel.gemmunch.utils

import android.content.Context
import android.content.SharedPreferences

object ModelPreferencesManager {
    private const val PREFS_NAME = "model_preferences"
    private const val KEY_SELECTED_MODEL = "selected_generative_model"
    private const val DEFAULT_MODEL = "GEMMA_3N_E4B_MODEL" // Default to the vision model

    private var sharedPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setSelectedModel(modelLogicalName: String) {
        sharedPreferences?.edit()?.putString(KEY_SELECTED_MODEL, modelLogicalName)?.apply()
    }

    fun getSelectedModel(): String {
        return sharedPreferences?.getString(KEY_SELECTED_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }
}