package com.stel.gemmunch.agent

import java.time.Instant

/** A temporary data class to hold the raw output from the AI model. */
data class IdentifiedFoodItem(
    val food: String,
    val quantity: Double,
    val unit: String
)

/** A data class representing a food item after its nutritional info has been looked up. */
data class AnalyzedFoodItem(
    val foodName: String,
    val quantity: Double,
    val unit: String,
    val calories: Int,
    val protein: Double? = null,
    val totalFat: Double? = null,
    val saturatedFat: Double? = null,
    val cholesterol: Double? = null,
    val sodium: Double? = null,
    val totalCarbs: Double? = null,
    val dietaryFiber: Double? = null,
    val sugars: Double? = null,
    val glycemicIndex: Int? = null,
    val glycemicLoad: Double? = null
)

/** A data class to hold detailed performance metrics for an AI analysis run. */
data class PerformanceMetrics(
    val sessionCreation: Long,
    val textPromptAdd: Long,
    val imageAdd: Long,
    val llmInference: Long,
    val jsonParsing: Long,
    val nutrientLookup: Long,
    val totalTime: Long
) {
    /** Formats a millisecond duration into a human-readable string (e.g., "1.2s"). */
    fun getFormattedTime(millis: Long): String {
        return when {
            millis < 1000 -> "${millis}ms"
            else -> "${String.format("%.1f", millis / 1000.0)}s"
        }
    }
}

/**
 * The final, complete analysis of a meal from a photo. This is the primary
 * data object that the UI will display.
 */
data class MealAnalysis(
    val totalCalories: Int,
    val items: List<AnalyzedFoodItem>,
    val generatedAt: Instant,
    val modelName: String,
    val performanceMetrics: PerformanceMetrics? = null
)