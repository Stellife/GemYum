package com.stel.gemmunch.data.models

import java.time.Instant

/**
 * Comprehensive feedback document for RAG library population
 */
data class MealAnalysisFeedback(
    // Analysis metadata
    val insightGeneratedDate: Instant,
    val modelDetails: ModelDetails,
    val performanceMetrics: PerformanceMetrics,
    
    // Meal timing
    val mealDateTime: Instant,
    val mealDateTimeSource: String, // "photo_metadata", "photo_capture_time", "user_provided", "error_loading_time"
    val mealDateTimeZone: String,
    
    // Model results
    val modelReturnStatus: ModelReturnStatus,
    val aiResponseRaw: String, // Raw JSON or error response
    val aiResponsePerItem: List<FoodItemAnalysis>,
    val aiResponseTotal: NutritionalTotals?,
    
    // User feedback
    val humanScore: Int?, // 0-5 scale
    val humanReportedErrors: List<ErrorType>,
    val humanErrorNotes: String?,
    val restaurantMealDetails: RestaurantMealInfo?,
    val humanCorrectedNutrition: CorrectedNutritionInfo?,
    
    // Manual items added by user
    val manuallyAddedItems: List<ManualFoodItem>?,
    
    // Health Connect
    val healthConnectWriteIntention: HealthConnectWriteChoice?,
    val healthConnectDataSources: HealthConnectDataSources?,
    val wasWrittenToHealthConnect: Boolean = false,
    
    // Image metadata (if easily available)
    val imageMetadata: ImageMetadata?,
    
    // Unique photo identifier for cross-model comparison
    val photoUniqueId: String?
)

data class ModelDetails(
    val modelName: String,
    val mediaQualitySize: String,
    val imageAnalysisMode: String, // "Single Shot" or "Reasoning"
    val promptText: String
)

data class PerformanceMetrics(
    val totalAnalysisTime: Long,
    val sessionCreationTime: Long,
    val textPromptAddTime: Long,
    val imageAddTime: Long,
    val llmInferenceTime: Long,
    val jsonParsingTime: Long,
    val nutrientLookupTime: Long
)

enum class ModelReturnStatus {
    SUCCESS,
    FAILED_NOT_JSON,
    FAILED_MULTIPLE_JSON_ARRAYS, // AI returned multiple arrays instead of single array
    FAILED_MALFORMED_JSON, // JSON was incomplete or corrupted
    FAILED_NO_ITEMS_IDENTIFIED,
    FAILED_TOKEN_LIMIT_EXCEEDED,
    FAILED_SESSION_ERROR,
    FAILED_INFERENCE_ERROR,
    FAILED_LOW_CONFIDENCE // Future: when all confidence scores below threshold
}

data class FoodItemAnalysis(
    val foodName: String,
    val quantity: Double,
    val unit: String,
    val nutritionalInfo: NutritionalInfo
)

data class NutritionalInfo(
    val calories: Int,
    val caloriesDV: Int?, // Daily Value percentage
    val protein: Double?,
    val proteinDV: Int?,
    val totalFat: Double?,
    val totalFatDV: Int?,
    val saturatedFat: Double?,
    val saturatedFatDV: Int?,
    val cholesterol: Double?,
    val cholesterolDV: Int?,
    val sodium: Double?,
    val sodiumDV: Int?,
    val totalCarbs: Double?,
    val totalCarbsDV: Int?,
    val dietaryFiber: Double?,
    val dietaryFiberDV: Int?,
    val sugars: Double?,
    val glycemicIndex: Int?,
    val glycemicLoad: Double?
)

data class NutritionalTotals(
    val totalNutrition: NutritionalInfo,
    val itemCount: Int
)

enum class ErrorType(val displayName: String) {
    FOOD_COMPLETELY_WRONG("Incorrect food identified"),
    FOOD_SLIGHTLY_WRONG("Food was slightly wrong (missing ingredients or mis-sized)"),
    QUANTITY_WRONG("Quantity was wrong"),
    NUTRITION_WRONG("Nutritional information feels wrong")
}

data class RestaurantMealInfo(
    val isRestaurant: Boolean,
    val restaurantName: String?,
    val mealDescription: String?
)

data class CorrectedNutritionInfo(
    val nutritionalValuePerItem: Map<String, Double>, // nutrient name -> value
    val informationSource: String?
)

enum class HealthConnectWriteChoice {
    WRITE_USER_VALUES,
    WRITE_COMPUTED_VALUES,
    DO_NOT_WRITE
}

data class ImageMetadata(
    val originalImagePath: String?,
    val imageWidth: Int,
    val imageHeight: Int,
    val imageSizeBytes: Long?,
    val wasCropped: Boolean,
    val cropCoordinates: CropCoordinates?,
    val exifDateTime: Instant? = null,
    val imageConfig: String?, // e.g., "ARGB_8888"
    val hasAlpha: Boolean?
)

data class CropCoordinates(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

data class ManualFoodItem(
    val foodName: String,
    val quantity: Double,
    val unit: String,
    val source: String, // "user_search", "web_search", "usda_api"
    val nutritionalInfo: NutritionalInfo
)

data class HealthConnectDataSources(
    val includeVisionComputed: Boolean = true,
    val includeManualItems: Boolean = true
)