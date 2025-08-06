package com.stel.gemmunch.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user preferences for media quality settings.
 * This singleton handles the selected image resolution for food photo analysis.
 */
object MediaQualityPreferencesManager {
    private const val PREF_NAME = "media_quality_preferences"
    private const val KEY_SELECTED_QUALITY = "selected_media_quality"
    
    private lateinit var sharedPreferences: SharedPreferences
    
    /**
     * Available media quality options with their resolutions
     */
    enum class MediaQuality(val resolution: Int, val displayName: String) {
        LOW(256, "256x256 - Fast"),
        MEDIUM(512, "512x512 - Balanced"),
        HIGH(768, "768x768 - Best Quality")
    }
    
    /**
     * Initialize the preferences manager with application context.
     * Must be called before using other methods.
     */
    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Get the currently selected media quality.
     * Defaults to MEDIUM if not set.
     */
    fun getSelectedQuality(): MediaQuality {
        val qualityName = sharedPreferences.getString(KEY_SELECTED_QUALITY, MediaQuality.MEDIUM.name)
        return try {
            MediaQuality.valueOf(qualityName!!)
        } catch (e: Exception) {
            MediaQuality.MEDIUM
        }
    }
    
    /**
     * Set the selected media quality.
     */
    fun setSelectedQuality(quality: MediaQuality) {
        sharedPreferences.edit()
            .putString(KEY_SELECTED_QUALITY, quality.name)
            .apply()
    }
    
    /**
     * Get the resolution value for the current quality setting.
     */
    fun getCurrentResolution(): Int {
        return getSelectedQuality().resolution
    }
}