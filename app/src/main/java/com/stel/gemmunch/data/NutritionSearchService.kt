package com.stel.gemmunch.data

import android.util.Log
import com.stel.gemmunch.agent.AnalyzedFoodItem
import com.stel.gemmunch.data.api.UsdaApiService
import com.stel.gemmunch.data.models.EnhancedNutrientDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "NutritionSearchService"

/**
 * Service for searching nutrition information from multiple sources.
 * This is designed to be modular and reusable across different features.
 */
class NutritionSearchService(
    private val enhancedNutrientDbHelper: EnhancedNutrientDbHelper,
    private val usdaApiService: UsdaApiService? = null
) {
    
    /**
     * Searches for nutrition information for a given food item and serving size.
     * First tries local database, then falls back to USDA API if available.
     * 
     * @param foodName The name of the food to search for
     * @param servingSize The serving size (e.g., 1.25 for 1.25 servings)
     * @param servingUnit Optional unit (e.g., "cup", "oz", "g"). Defaults to "serving"
     * @return AnalyzedFoodItem with nutrition data, or null if not found
     */
    suspend fun searchNutrition(
        foodName: String,
        servingSize: Double,
        servingUnit: String = "serving"
    ): AnalyzedFoodItem? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Searching nutrition for: $foodName, serving: $servingSize $servingUnit")
        
        // First try local database
        try {
            val localResult = enhancedNutrientDbHelper.lookup(foodName, servingSize, servingUnit)
            
            // If we found complete data locally, return it
            // Check if we have calories AND at least some other nutrient data
            val hasCompleteData = localResult.calories > 0 && 
                (localResult.protein != null || localResult.totalFat != null || 
                 localResult.totalCarbs != null || localResult.sodium != null)
                
            if (hasCompleteData) {
                Log.d(TAG, "Found complete data in local database: ${localResult.calories} cal")
                return@withContext AnalyzedFoodItem(
                    foodName = foodName,
                    quantity = servingSize,
                    unit = servingUnit,
                    calories = localResult.calories,
                    protein = localResult.protein,
                    totalFat = localResult.totalFat,
                    saturatedFat = localResult.saturatedFat,
                    cholesterol = localResult.cholesterol,
                    sodium = localResult.sodium,
                    totalCarbs = localResult.totalCarbs,
                    dietaryFiber = localResult.dietaryFiber,
                    sugars = localResult.sugars,
                    glycemicIndex = localResult.glycemicIndex,
                    glycemicLoad = localResult.glycemicLoad
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching local database", e)
        }
        
        // If not found locally and we have USDA API service, try that
        if (usdaApiService != null && usdaApiService.isConfigured()) {
            try {
                Log.d(TAG, "Searching USDA API for: $foodName")
                val nutritionData = usdaApiService.searchAndGetFullNutrition(foodName)
                
                if (nutritionData != null) {
                    // Scale all nutrients by serving size (assuming 100g base)
                    val scaleFactor = servingSize * (if (servingUnit == "g" || servingUnit == "grams") 1.0 / 100.0 else 1.0)
                    
                    Log.d(TAG, "Found in USDA: ${nutritionData.calories} cal/100g with full nutrition data")
                    
                    return@withContext AnalyzedFoodItem(
                        foodName = foodName,
                        quantity = servingSize,
                        unit = servingUnit,
                        calories = (nutritionData.calories * scaleFactor).toInt(),
                        protein = nutritionData.protein?.let { it * scaleFactor },
                        totalFat = nutritionData.totalFat?.let { it * scaleFactor },
                        saturatedFat = nutritionData.saturatedFat?.let { it * scaleFactor },
                        cholesterol = nutritionData.cholesterol?.let { it * scaleFactor },
                        sodium = nutritionData.sodium?.let { it * scaleFactor },
                        totalCarbs = nutritionData.totalCarbs?.let { it * scaleFactor },
                        dietaryFiber = nutritionData.dietaryFiber?.let { it * scaleFactor },
                        sugars = nutritionData.sugars?.let { it * scaleFactor }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching USDA API", e)
            }
        }
        
        // If all else fails, return null
        Log.w(TAG, "No nutrition data found for: $foodName")
        return@withContext null
    }
    
    /**
     * Adjusts nutrition values based on a new serving size.
     * This is used when the user changes the serving size in the UI.
     * 
     * @param item The original analyzed food item
     * @param newServingSize The new serving size
     * @return A new AnalyzedFoodItem with adjusted values
     */
    fun adjustServingSize(item: AnalyzedFoodItem, newServingSize: Double): AnalyzedFoodItem {
        val scaleFactor = newServingSize / item.quantity
        
        return item.copy(
            quantity = newServingSize,
            calories = (item.calories * scaleFactor).toInt(),
            protein = item.protein?.let { it * scaleFactor },
            totalFat = item.totalFat?.let { it * scaleFactor },
            saturatedFat = item.saturatedFat?.let { it * scaleFactor },
            cholesterol = item.cholesterol?.let { it * scaleFactor },
            sodium = item.sodium?.let { it * scaleFactor },
            totalCarbs = item.totalCarbs?.let { it * scaleFactor },
            dietaryFiber = item.dietaryFiber?.let { it * scaleFactor },
            sugars = item.sugars?.let { it * scaleFactor },
            glycemicIndex = item.glycemicIndex, // GI doesn't change with serving size
            glycemicLoad = item.glycemicLoad?.let { it * scaleFactor }
        )
    }
    
    /**
     * Combines nutrition data from multiple sources.
     * Used when writing to Health Connect with both AI-detected and user-provided items.
     * 
     * @param items List of analyzed food items to combine
     * @return A single AnalyzedFoodItem with summed nutrition values
     */
    fun combineNutritionData(items: List<AnalyzedFoodItem>): AnalyzedFoodItem {
        if (items.isEmpty()) {
            return AnalyzedFoodItem(
                foodName = "Combined meal",
                quantity = 1.0,
                unit = "meal",
                calories = 0
            )
        }
        
        return AnalyzedFoodItem(
            foodName = "Combined meal",
            quantity = 1.0,
            unit = "meal",
            calories = items.sumOf { it.calories },
            protein = sumNullableDoubles(items.map { it.protein }),
            totalFat = sumNullableDoubles(items.map { it.totalFat }),
            saturatedFat = sumNullableDoubles(items.map { it.saturatedFat }),
            cholesterol = sumNullableDoubles(items.map { it.cholesterol }),
            sodium = sumNullableDoubles(items.map { it.sodium }),
            totalCarbs = sumNullableDoubles(items.map { it.totalCarbs }),
            dietaryFiber = sumNullableDoubles(items.map { it.dietaryFiber }),
            sugars = sumNullableDoubles(items.map { it.sugars }),
            glycemicIndex = null, // Can't meaningfully combine GI
            glycemicLoad = sumNullableDoubles(items.map { it.glycemicLoad })
        )
    }
    
    private fun sumNullableDoubles(values: List<Double?>): Double? {
        val nonNullValues = values.filterNotNull()
        return if (nonNullValues.isEmpty()) null else nonNullValues.sum()
    }
    
}