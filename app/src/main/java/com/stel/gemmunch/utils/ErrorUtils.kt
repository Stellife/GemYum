package com.stel.gemmunch.utils

/**
 * Utility functions for error handling, inspired by Google AI Edge Gallery
 */
object ErrorUtils {
    
    /**
     * Cleans up MediaPipe error messages by removing source location traces.
     * This provides cleaner, more user-friendly error messages.
     * 
     * Based on Google AI Edge Gallery's cleanUpMediapipeTaskErrorMessage function.
     */
    fun cleanUpMediaPipeError(errorMessage: String?): String {
        if (errorMessage == null) return "Unknown error"
        
        // Remove source location traces like "mediapipe/tasks/cc/..."
        val cleanedMessage = errorMessage
            .split("\n")
            .firstOrNull { !it.contains("mediapipe/tasks/cc/") && it.isNotBlank() }
            ?: errorMessage
            
        // Further cleanup common MediaPipe error patterns
        return cleanedMessage
            .replace("Error:", "")
            .replace("INTERNAL:", "")
            .replace("INVALID_ARGUMENT:", "Invalid input:")
            .replace("OUT_OF_RANGE:", "")
            .trim()
    }
    
    /**
     * Converts technical error messages to user-friendly ones
     */
    fun getUserFriendlyError(error: Exception): String {
        val message = error.message ?: "Unknown error"
        
        return when {
            message.contains("timeout", ignoreCase = true) -> 
                "Analysis timed out. Please try again with a simpler photo."
            
            message.contains("memory", ignoreCase = true) || 
            message.contains("oom", ignoreCase = true) -> 
                "Not enough memory to process image. Please close other apps and try again."
            
            message.contains("session", ignoreCase = true) -> 
                "AI model session error. Please restart the app."
            
            message.contains("token", ignoreCase = true) || 
            message.contains("context length", ignoreCase = true) ->
                "Image too complex for analysis. Please try a simpler photo."
            
            message.contains("model not found", ignoreCase = true) ->
                "AI model not loaded. Please wait a moment and try again."
            
            else -> cleanUpMediaPipeError(message)
        }
    }
}