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

            // 2. Get the best match using improved scoring strategy
            val allResults = searchResponse.body()!!.foods
            Log.d(TAG, "Found ${allResults.size} USDA results for '$foodName':")
            
            // Log top 3 results for debugging
            allResults.take(3).forEach { food ->
                Log.d(TAG, "  - '${food.description}' (score: ${food.score}, type: ${food.dataType})")
            }
            
            val bestMatch = findBestFoodMatch(foodName, allResults)
            if (bestMatch == null) {
                Log.w(TAG, "No suitable match found in initial USDA search for '$foodName'")
                
                // Try alternative search strategies
                val alternativeMatch = tryAlternativeSearches(foodName)
                if (alternativeMatch == null) {
                    Log.w(TAG, "No suitable match found after trying alternative searches for '$foodName'")
                    return null
                }
                
                Log.d(TAG, "Found alternative match: '${alternativeMatch.description}' for '$foodName'")
                return getCaloriesFromFoodDetails(alternativeMatch.fdcId, foodName)
            }
            
            Log.d(TAG, "Selected best match: '${bestMatch.description}' (score: ${bestMatch.score})")

            // 3. Get detailed nutrition info for the best match
            return getCaloriesFromFoodDetails(bestMatch.fdcId, foodName)

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

            // 2. Get the best match using improved scoring strategy
            val allResults = searchResponse.body()!!.foods
            val bestMatch = findBestFoodMatch(foodName, allResults)
            if (bestMatch == null) {
                Log.w(TAG, "No suitable match found in initial USDA search for '$foodName'")
                
                // Try alternative search strategies
                val alternativeMatch = tryAlternativeSearches(foodName)
                if (alternativeMatch == null) {
                    Log.w(TAG, "No suitable match found after trying alternative searches for '$foodName'")
                    return null
                }
                
                Log.d(TAG, "Found alternative match: '${alternativeMatch.description}' for '$foodName'")
                // Use the alternative match for the rest of the function
                return getFullNutritionFromFoodDetails(alternativeMatch.fdcId, foodName)
            }

            // 3. Get detailed nutrition info for the best match
            return getFullNutritionFromFoodDetails(bestMatch.fdcId, foodName)

        } catch (e: Exception) {
            Log.e(TAG, "USDA full nutrition lookup failed for '$foodName'", e)
            return null
        }
    }

    fun isConfigured(): Boolean {
        return apiKey.isNotBlank() && apiKey != "YOUR_API_KEY_HERE"
    }
    
    /**
     * Improved food matching algorithm that prioritizes:
     * 1. Exact name matches
     * 2. Food items with reasonable calorie ranges
     * 3. Reliable data sources (Foundation, SR Legacy)
     * 4. Meaningful food descriptions (not soups, supplements, etc.)
     */
    private fun findBestFoodMatch(searchTerm: String, results: List<UsdaFoodSummary>): UsdaFoodSummary? {
        val lowerSearchTerm = searchTerm.lowercase()
        
        // Step 1: Filter by reliable data types first
        val reliableResults = results.filter { it.dataType in listOf("Foundation", "SR Legacy") }
        val workingResults = if (reliableResults.isNotEmpty()) reliableResults else results
        
        // Step 2: Score each result based on multiple factors
        val scoredResults = workingResults.map { food ->
            var customScore = 0.0
            val description = food.description.lowercase()
            
            // Bonus for exact matches
            if (description.contains(lowerSearchTerm)) {
                customScore += if (description == lowerSearchTerm) 1000.0 else 500.0
            }
            
            // Bonus for containing all words from search term
            val searchWords = lowerSearchTerm.split(" ")
            val matchingWords = searchWords.count { word -> description.contains(word) }
            customScore += (matchingWords.toDouble() / searchWords.size) * 300.0
            
            // Penalty for unwanted food types
            val unwantedTerms = listOf(
                "soup", "sauce", "dressing", "supplement", "powder", "mix", 
                "flavoring", "extract", "syrup", "topping", "seasoning"
            )
            if (unwantedTerms.any { description.contains(it) }) {
                customScore -= 200.0
            }
            
            // Penalty for overly long descriptions (usually processed foods)
            if (description.length > 50) {
                customScore -= 50.0
            }
            
            // Small bonus for USDA's original score
            customScore += food.score * 0.1
            
            Pair(food, customScore)
        }
        
        // Step 3: Return the highest scoring result, but only if it meets minimum quality threshold
        val bestResult = scoredResults.maxByOrNull { it.second }
        
        // Log the top 3 candidates for debugging
        val topCandidates = scoredResults.sortedByDescending { it.second }.take(3)
        Log.d(TAG, "Top candidates for '$searchTerm':")
        topCandidates.forEach { (food, score) ->
            Log.d(TAG, "  - '${food.description}' (custom score: %.1f, USDA score: %.1f)".format(score, food.score))
        }
        
        // Reject results with negative custom scores (poor matches)
        val minimumAcceptableScore = 50.0
        if (bestResult == null || bestResult.second < minimumAcceptableScore) {
            Log.w(TAG, "Best match score (${bestResult?.second ?: "null"}) below minimum threshold ($minimumAcceptableScore). Rejecting poor match.")
            return null
        }
        
        Log.d(TAG, "Accepting match: '${bestResult.first.description}' with score ${bestResult.second}")
        return bestResult.first
    }
    
    /**
     * Try alternative search strategies when the initial search fails or returns poor matches.
     */
    private suspend fun tryAlternativeSearches(originalTerm: String): UsdaFoodSummary? {
        val alternatives = generateAlternativeSearchTerms(originalTerm)
        
        for (altTerm in alternatives) {
            Log.d(TAG, "Trying alternative search: '$altTerm' for original term '$originalTerm'")
            
            try {
                val searchRequest = UsdaSearchRequest(query = altTerm)
                val searchResponse = api.searchFoods(apiKey, searchRequest)
                
                if (!searchResponse.isSuccessful || searchResponse.body()?.foods.isNullOrEmpty()) {
                    continue
                }
                
                val results = searchResponse.body()!!.foods
                val bestMatch = findBestFoodMatch(originalTerm, results) // Still score against original term
                
                if (bestMatch != null) {
                    Log.i(TAG, "Alternative search '$altTerm' found suitable match: '${bestMatch.description}'")
                    return bestMatch
                }
            } catch (e: Exception) {
                Log.w(TAG, "Alternative search '$altTerm' failed", e)
                continue
            }
        }
        
        return null
    }
    
    /**
     * Generate alternative search terms for common food items.
     */
    private fun generateAlternativeSearchTerms(foodName: String): List<String> {
        val lowerFood = foodName.lowercase()
        val alternatives = mutableListOf<String>()
        
        // Common food mappings and variations
        when {
            lowerFood.contains("pad thai") -> {
                alternatives.addAll(listOf("thai noodles", "rice noodles", "stir fry noodles", "noodles"))
            }
            lowerFood.contains("burger") -> {
                alternatives.addAll(listOf("hamburger", "beef patty", "ground beef"))
            }
            lowerFood.contains("taco") -> {
                alternatives.addAll(listOf("mexican food", "tortilla", "ground beef"))
            }
            lowerFood.contains("pizza") -> {
                alternatives.addAll(listOf("cheese pizza", "italian food"))
            }
            lowerFood.contains("pasta") -> {
                alternatives.addAll(listOf("spaghetti", "noodles", "italian food"))
            }
            lowerFood.contains("salad") -> {
                alternatives.addAll(listOf("lettuce", "mixed greens", "vegetables"))
            }
        }
        
        // Generic fallbacks
        val words = lowerFood.split(" ")
        if (words.size > 1) {
            // Try individual words
            alternatives.addAll(words.filter { it.length > 3 })
            
            // Try combinations
            if (words.size == 2) {
                alternatives.add(words[1]) // Try second word first
                alternatives.add(words[0]) // Then first word
            }
        }
        
        return alternatives.distinct().take(3) // Limit to 3 alternatives to avoid too many API calls
    }
    
    /**
     * Extract calories from food details, separated for reuse.
     */
    private suspend fun getCaloriesFromFoodDetails(fdcId: Long, foodName: String): Int? {
        try {
            val detailsResponse = api.getFoodDetails(fdcId, apiKey)
            if (!detailsResponse.isSuccessful) {
                Log.w(TAG, "Failed to get details for FDC ID $fdcId")
                return null
            }

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
            Log.e(TAG, "Failed to get food details for FDC ID $fdcId", e)
            return null
        }
    }
    
    /**
     * Extract full nutrition information from food details, separated for reuse.
     */
    private suspend fun getFullNutritionFromFoodDetails(fdcId: Long, foodName: String): UsdaNutritionData? {
        try {
            val detailsResponse = api.getFoodDetails(fdcId, apiKey)
            if (!detailsResponse.isSuccessful) {
                Log.w(TAG, "Failed to get details for FDC ID $fdcId")
                return null
            }

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
            Log.e(TAG, "Failed to get full nutrition details for FDC ID $fdcId", e)
            return null
        }
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