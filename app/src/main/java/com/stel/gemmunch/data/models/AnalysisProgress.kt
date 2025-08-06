package com.stel.gemmunch.data.models

import java.time.Instant

/**
 * Represents a single step in the analysis process with timing information.
 */
data class AnalysisStep(
    val name: String,
    val status: StepStatus,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val durationMs: Long? = null,
    val details: String? = null
) {
    enum class StepStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }
}

/**
 * Tracks the overall progress of meal analysis with detailed step information.
 */
data class AnalysisProgress(
    val totalSteps: Int = 6,
    val currentStep: Int = 0,
    val overallProgress: Float = 0f,
    val steps: List<AnalysisStep> = listOf(
        AnalysisStep("Preparing AI Session", AnalysisStep.StepStatus.PENDING),
        AnalysisStep("Processing Image", AnalysisStep.StepStatus.PENDING),
        AnalysisStep("Adding Context", AnalysisStep.StepStatus.PENDING),
        AnalysisStep("AI Analysis", AnalysisStep.StepStatus.PENDING),
        AnalysisStep("Parsing Results", AnalysisStep.StepStatus.PENDING),
        AnalysisStep("Looking Up Nutrition", AnalysisStep.StepStatus.PENDING)
    ),
    val isComplete: Boolean = false,
    val error: String? = null
) {
    fun updateStep(stepName: String, status: AnalysisStep.StepStatus, durationMs: Long? = null, details: String? = null): AnalysisProgress {
        val updatedSteps = steps.map { step ->
            if (step.name == stepName) {
                step.copy(
                    status = status,
                    endTime = if (status == AnalysisStep.StepStatus.COMPLETED) Instant.now() else step.endTime,
                    durationMs = durationMs ?: step.durationMs,
                    details = details ?: step.details
                )
            } else {
                step
            }
        }
        
        val completedCount = updatedSteps.count { it.status == AnalysisStep.StepStatus.COMPLETED }
        val inProgressCount = updatedSteps.count { it.status == AnalysisStep.StepStatus.IN_PROGRESS }
        val currentStepIndex = if (inProgressCount > 0) {
            updatedSteps.indexOfFirst { it.status == AnalysisStep.StepStatus.IN_PROGRESS } + 1
        } else {
            completedCount
        }
        
        return copy(
            steps = updatedSteps,
            currentStep = currentStepIndex,
            overallProgress = completedCount.toFloat() / totalSteps,
            isComplete = completedCount == totalSteps
        )
    }
    
    fun setError(error: String): AnalysisProgress {
        return copy(error = error, isComplete = true)
    }
}