package com.stel.gemmunch.data.api

import android.util.Log
import com.stel.gemmunch.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

private const val TAG = "UsdaApiService"
private const val BASE_URL = "https://api.nal.usda.gov/fdc/v1/"

interface UsdaApi {
    @POST("foods/search")
    suspend fun searchFoods(
        @Query("api_key") apiKey: String,
        @Body request: UsdaSearchRequest
    ): Response<UsdaSearchResponse>

    @GET("food/{fdcId}")
    suspend fun getFoodDetails(
        @Path("fdcId") fdcId: Long,
        @Query("api_key") apiKey: String,
        @Query("nutrients") nutrients: String? = null // To request specific nutrients
    ): Response<UsdaFoodDetails>
}

class UsdaApiService {
    private val apiKey = BuildConfig.USDA_API_KEY
    private val api: UsdaApi

    init {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(UsdaApi::class.java)
    }

    suspend fun searchAndGetBestCalories(foodName: String): Int? {
        try {
            // 1. Search for the food
            val searchRequest = UsdaSearchRequest(query = foodName)
            val searchResponse = api.searchFoods(apiKey, searchRequest)
            if (!searchResponse.isSuccessful || searchResponse.body()?.foods.isNullOrEmpty()) {
                Log.w(TAG, "No USDA search results for '$foodName'")
                return null
            }

            // 2. Get the best match (highest score from a reliable data type)
            val bestMatch = searchResponse.body()!!.foods
                .filter { it.dataType in listOf("Foundation", "SR Legacy") }
                .maxByOrNull { it.score }
                ?: searchResponse.body()!!.foods.first()

            // 3. Get detailed nutrition info for the best match
            val detailsResponse = api.getFoodDetails(bestMatch.fdcId, apiKey)
            if (!detailsResponse.isSuccessful) {
                Log.w(TAG, "Failed to get details for FDC ID ${bestMatch.fdcId}")
                return null
            }

            // 4. Extract calories and return
            val foodDetails = detailsResponse.body()
            if (foodDetails == null) {
                Log.w(TAG, "No food details in response for '$foodName'")
                return null
            }
            
            // Log nutrients for debugging
            Log.d(TAG, "Found ${foodDetails.foodNutrients.size} nutrients for '${foodDetails.description}'")
            foodDetails.foodNutrients.take(5).forEach { nutrient ->
                Log.d(TAG, "  - ${nutrient.nutrient.name}: ${nutrient.amount} ${nutrient.nutrient.unitName}")
            }
            
            val calories = foodDetails.getCaloriesPer100g().toInt()
            if (calories == 0) {
                Log.w(TAG, "Zero calories found for '$foodName' - logging all nutrients:")
                foodDetails.foodNutrients
                    .filter { it.nutrient.name.contains("energy", ignoreCase = true) }
                    .forEach { nutrient ->
                        Log.d(TAG, "  Energy nutrient: ${nutrient.nutrient.name} (ID: ${nutrient.nutrient.id}): ${nutrient.amount} ${nutrient.nutrient.unitName}")
                    }
            }
            
            Log.i(TAG, "USDA lookup successful for '$foodName': $calories kcal/100g")
            return calories

        } catch (e: Exception) {
            Log.e(TAG, "USDA full lookup failed for '$foodName'", e)
            return null
        }
    }

    /**
     * Searches for a food and returns complete nutrition information.
     * This is an enhanced version that returns all nutrients, not just calories.
     */
    suspend fun searchAndGetFullNutrition(foodName: String): UsdaNutritionData? {
        try {
            // 1. Search for the food
            val searchRequest = UsdaSearchRequest(query = foodName)
            val searchResponse = api.searchFoods(apiKey, searchRequest)
            if (!searchResponse.isSuccessful || searchResponse.body()?.foods.isNullOrEmpty()) {
                Log.w(TAG, "No USDA search results for '$foodName'")
                return null
            }

            // 2. Get the best match (highest score from a reliable data type)
            val bestMatch = searchResponse.body()!!.foods
                .filter { it.dataType in listOf("Foundation", "SR Legacy") }
                .maxByOrNull { it.score }
                ?: searchResponse.body()!!.foods.first()

            // 3. Get detailed nutrition info for the best match
            val detailsResponse = api.getFoodDetails(bestMatch.fdcId, apiKey)
            if (!detailsResponse.isSuccessful) {
                Log.w(TAG, "Failed to get details for FDC ID ${bestMatch.fdcId}")
                return null
            }

            // 4. Extract all nutrients
            val foodDetails = detailsResponse.body()
            if (foodDetails == null) {
                Log.w(TAG, "No food details in response for '$foodName'")
                return null
            }
            
            // Extract key nutrients (per 100g)
            val nutrients = foodDetails.foodNutrients
            
            return UsdaNutritionData(
                foodName = foodDetails.description,
                calories = foodDetails.getCaloriesPer100g().toInt(),
                protein = nutrients.find { it.nutrient.name == "Protein" }?.amount,
                totalFat = nutrients.find { it.nutrient.name == "Total lipid (fat)" }?.amount,
                saturatedFat = nutrients.find { it.nutrient.name == "Fatty acids, total saturated" }?.amount,
                cholesterol = nutrients.find { it.nutrient.name == "Cholesterol" }?.amount,
                sodium = nutrients.find { it.nutrient.name == "Sodium, Na" }?.amount,
                totalCarbs = nutrients.find { it.nutrient.name == "Carbohydrate, by difference" }?.amount,
                dietaryFiber = nutrients.find { it.nutrient.name == "Fiber, total dietary" }?.amount,
                sugars = nutrients.find { it.nutrient.name == "Total Sugars" }?.amount
            )

        } catch (e: Exception) {
            Log.e(TAG, "USDA full nutrition lookup failed for '$foodName'", e)
            return null
        }
    }

    fun isConfigured(): Boolean {
        return apiKey.isNotBlank() && apiKey != "YOUR_API_KEY_HERE"
    }
}

/**
 * Data class to hold complete nutrition information from USDA
 */
data class UsdaNutritionData(
    val foodName: String,
    val calories: Int,
    val protein: Double? = null,
    val totalFat: Double? = null,
    val saturatedFat: Double? = null,
    val cholesterol: Double? = null,
    val sodium: Double? = null,
    val totalCarbs: Double? = null,
    val dietaryFiber: Double? = null,
    val sugars: Double? = null
)