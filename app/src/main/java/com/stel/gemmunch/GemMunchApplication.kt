package com.stel.gemmunch

import android.app.Application
import android.util.Log
import com.stel.gemmunch.data.InitializationMetrics
import com.stel.gemmunch.utils.ModelPreferencesManager
import com.stel.gemmunch.utils.VisionModelPreferencesManager
import com.stel.gemmunch.utils.MediaQualityPreferencesManager
import com.stel.gemmunch.utils.ImageReasoningPreferencesManager

/**
 * A custom Application class for one-time initialization tasks.
 * This is the official entry point when the application process is created.
 */
class GemMunchApplication : Application() {
    // The container is created once and lives for the lifetime of the app.
    // We will create the AppContainer class in a later step.
    lateinit var container: AppContainer
    
    // Global initialization metrics
    val initMetrics = InitializationMetrics()
    
    companion object {
        private const val TAG = "GemMunchApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Application onCreate started")
        initMetrics.startPhase("ApplicationStartup")
        
        // Create app container
        initMetrics.startSubPhase("ApplicationStartup", "CreateAppContainer")
        container = DefaultAppContainer(this, initMetrics)
        initMetrics.endSubPhase("ApplicationStartup", "CreateAppContainer")
        
        // Initialize preference managers
        initMetrics.startSubPhase("ApplicationStartup", "InitializePreferences")
        ModelPreferencesManager.initialize(this)
        VisionModelPreferencesManager.initialize(this)
        MediaQualityPreferencesManager.initialize(this)
        ImageReasoningPreferencesManager.initialize(this)
        initMetrics.endSubPhase("ApplicationStartup", "InitializePreferences")
        
        initMetrics.endPhase("ApplicationStartup")
        Log.i(TAG, "Application onCreate completed in ${initMetrics.phases["ApplicationStartup"]?.durationMs}ms")
    }
}