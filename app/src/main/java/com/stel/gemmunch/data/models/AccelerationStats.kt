package com.stel.gemmunch.data.models

import com.google.mediapipe.tasks.genai.llminference.LlmInference

/**
 * Data class representing a specific optimization applied to the device.
 */
data class AppliedOptimization(
    val setting: String,
    val value: String,
    val reason: String,
    val expectedImprovement: String
)

/**
 * Data class to hold acceleration analysis results for display in UI.
 */
data class AccelerationStats(
    val optimalBackend: LlmInference.Backend,
    val confidence: Float,
    val benchmarkTimeMs: Long,
    val deviceModel: String,
    val chipset: String,
    val cpuCores: Int,
    val hasGPU: Boolean,
    val hasNPU: Boolean,
    val hasNNAPI: Boolean,
    val androidVersion: Int,
    val analysisTimestamp: Long = System.currentTimeMillis(),
    val isCachedResult: Boolean = false,
    val optimizations: List<AppliedOptimization> = emptyList(),
    val playServicesEnabled: Boolean = false
) {
    /**
     * Returns a user-friendly backend name.
     */
    val backendDisplayName: String
        get() = when {
            playServicesEnabled && optimalBackend == LlmInference.Backend.GPU -> "Play Services GPU/TPU"
            playServicesEnabled && optimalBackend == LlmInference.Backend.CPU -> "Play Services CPU"
            optimalBackend == LlmInference.Backend.GPU -> "GPU Acceleration"
            optimalBackend == LlmInference.Backend.CPU -> "CPU Optimization"
            else -> optimalBackend.name
        }
    
    /**
     * Returns a confidence percentage as an integer.
     */
    val confidencePercentage: Int
        get() = (confidence * 100).toInt()
    
    /**
     * Returns a performance category based on confidence score.
     */
    val performanceCategory: String
        get() = when {
            confidence >= 0.9f -> "Excellent"
            confidence >= 0.8f -> "Very Good" 
            confidence >= 0.7f -> "Good"
            confidence >= 0.6f -> "Fair"
            else -> "Basic"
        }
    
    /**
     * Returns an emoji representing the acceleration type.
     */
    val accelerationEmoji: String
        get() = when {
            playServicesEnabled && hasNPU -> "🧠" // TPU/NPU with Play Services
            playServicesEnabled && optimalBackend == LlmInference.Backend.GPU -> "🚀" // GPU with Play Services
            playServicesEnabled -> "⚡" // Play Services optimization
            optimalBackend == LlmInference.Backend.GPU -> "🎮" // Basic GPU
            optimalBackend == LlmInference.Backend.CPU -> "💻" // Basic CPU
            else -> "⚡"
        }
    
    /**
     * Returns hardware capabilities summary.
     */
    val hardwareCapabilities: List<String>
        get() = mutableListOf<String>().apply {
            if (hasGPU) add("GPU")
            if (hasNPU) add("NPU/TPU") 
            if (hasNNAPI) add("NNAPI")
            add("${cpuCores}-Core CPU")
        }
    
    /**
     * Returns estimated inference time range based on backend.
     */
    val estimatedInferenceTime: String
        get() = when (optimalBackend) {
            LlmInference.Backend.GPU -> {
                if (hasNPU) "8-15s (NPU-capable GPU)" 
                else "15-25s (GPU)"
            }
            LlmInference.Backend.CPU -> "35-60s (CPU)"
            else -> "15-45s"
        }
    
    /**
     * Returns NPU explanation based on device capabilities.
     */
    val npuExplanation: String
        get() = when {
            hasNPU && deviceModel.contains("Pixel", ignoreCase = true) -> 
                "Has Tensor TPU (Google's AI chip)"
            hasNPU && chipset.contains("Qualcomm", ignoreCase = true) -> 
                "Has Hexagon NPU (Qualcomm AI chip)"
            hasNPU -> "Has Neural Processing Unit"
            deviceModel.contains("Pixel", ignoreCase = true) -> 
                "Older Pixel (no Tensor chip)"
            else -> "No dedicated AI chip (uses GPU/CPU)"
        }
    
    /**
     * Returns confidence explanation.
     */
    val confidenceExplanation: String
        get() = when {
            isCachedResult -> "Confidence: ${confidencePercentage}% (from cache)"
            benchmarkTimeMs > 0 -> "Confidence: ${confidencePercentage}% (benchmarked)"
            else -> "Confidence: ${confidencePercentage}% (estimated)"
        }
}