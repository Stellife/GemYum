package com.stel.gemmunch.utils

import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.stel.gemmunch.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages different session types for optimized performance based on app mode.
 * This allows text-only sessions to skip vision components for faster initialization
 * and lower memory usage.
 */
class SessionManager(private val appContainer: AppContainer) {
    
    companion object {
        private const val TAG = "SessionManager"
    }
    
    // Note: Session options don't support setMaxNumImages or setMaxTokens
    // Those are set at inference level, not session level
    // For now, we just use the default session configuration
    
    // Pre-warm based on user navigation
    suspend fun prewarmForDestination(destination: String) {
        when (destination) {
            "chat/text" -> prewarmTextSession()
            "chat/multimodal" -> prewarmMultimodalSession()
            "camera/singleshot" -> prewarmSingleShotSession()
        }
    }
    
    private suspend fun prewarmTextSession() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Pre-warming text-only session")
            // For now, we just pre-warm the regular vision session
            // TODO: Implement actual text-only session when SDK supports it
            appContainer.prewarmSessionOnDemand()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pre-warm text session", e)
        }
    }
    
    private suspend fun prewarmMultimodalSession() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Pre-warming multimodal session")
            appContainer.prewarmSessionOnDemand()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pre-warm multimodal session", e)
        }
    }
    
    private suspend fun prewarmSingleShotSession() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Pre-warming single-shot session")
            appContainer.prewarmSessionOnDemand()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pre-warm single-shot session", e)
        }
    }
    
    /**
     * Get a session optimized for the given mode.
     * For now, all modes return the same vision session, but this structure
     * allows for future optimization when the SDK supports text-only sessions.
     */
    suspend fun getSessionForMode(mode: SessionMode): LlmInferenceSession {
        return when (mode) {
            SessionMode.TEXT_ONLY -> {
                // TODO: Return actual text-only session when available
                appContainer.getReadyVisionSession()
            }
            SessionMode.MULTIMODAL -> {
                appContainer.getReadyVisionSession()
            }
            SessionMode.SINGLE_SHOT -> {
                appContainer.getReadyVisionSession()
            }
        }
    }
}

enum class SessionMode {
    TEXT_ONLY,
    MULTIMODAL,
    SINGLE_SHOT
}