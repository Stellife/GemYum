package com.stel.gemmunch.data.api

import com.google.gson.annotations.SerializedName

// Models for the USDA FoodData Central API.

data class UsdaSearchRequest(
    @SerializedName("query") val query: String,
    @SerializedName("dataType") val dataType: List<String> = listOf("Foundation", "SR Legacy"),
    @SerializedName("pageSize") val pageSize: Int = 5
)

data class UsdaSearchResponse(
    @SerializedName("foods") val foods: List<UsdaFoodSummary>
)

data class UsdaFoodSummary(
    @SerializedName("fdcId") val fdcId: Long,
    @SerializedName("description") val description: String,
    @SerializedName("dataType") val dataType: String,
    @SerializedName("score") val score: Double
)

data class UsdaFoodDetails(
    @SerializedName("description") val description: String,
    @SerializedName("foodNutrients") val foodNutrients: List<UsdaFoodNutrient>
)

data class UsdaFoodNutrient(
    @SerializedName("nutrient") val nutrient: UsdaNutrient,
    @SerializedName("amount") val amount: Double?
)

data class UsdaNutrient(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("unitName") val unitName: String
)

object UsdaNutrientIds {
    const val ENERGY_KCAL = 1008
    const val ENERGY_KCAL_ATWATER = 2047 // Energy (Atwater General Factors)
    const val ENERGY_KCAL_NME = 2048 // Energy (Atwater Specific Factors) 
}

// Extension function for easier data access
fun UsdaFoodDetails.getCaloriesPer100g(): Double {
    // Try multiple energy nutrient IDs as USDA uses different ones for different foods
    return foodNutrients.find { 
        it.nutrient.id in listOf(
            UsdaNutrientIds.ENERGY_KCAL,
            UsdaNutrientIds.ENERGY_KCAL_ATWATER,
            UsdaNutrientIds.ENERGY_KCAL_NME
        )
    }?.amount ?: 0.0
}