package com.stel.gemmunch.agent

import android.graphics.Bitmap
import android.util.Log
import com.stel.gemmunch.AppContainer
import com.stel.gemmunch.data.models.EnhancedNutrientDbHelper
import com.stel.gemmunch.utils.GsonProvider
import com.stel.gemmunch.utils.VisionModelPreferencesManager
import com.stel.gemmunch.utils.ImageReasoningPreferencesManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.system.measureTimeMillis

private const val TAG = "PhotoMealExtractor"

/**
 * Exception thrown when the AI model returns a response that cannot be parsed as JSON
 */
class InvalidJsonResponseException(message: String) : Exception(message)

/**
 * A data class matching the JSON structure we expect from the Gemma-3N model.
 * Includes the model's confidence in its own identification.
 */
private data class EnhancedFoodItem(
    val food: String,
    val quantity: Double,
    val unit: String?,
    val confidence: Double
)

/**
 * Orchestrates the entire process of analyzing a food photo to extract nutritional information.
 */
class PhotoMealExtractor(
    private val appContainer: AppContainer,
    private val enhancedNutrientDbHelper: EnhancedNutrientDbHelper,
    private val gson: Gson
) {
    // Reasoning prompt that includes chain of thought but still returns JSON
    private val reasoningPromptText = """
    Analyze the food in this image step by step. Show your reasoning, then provide the JSON.
    
    Step 1: Describe the main visual elements:
    - Shape and structure of the food
    - Colors you observe
    - Any visible ingredients or toppings
    - How the food is arranged or presented
    
    Step 2: Based on your observations, identify the food items.
    Think carefully:
    - Tacos have folded tortillas with exposed fillings on top
    - Burritos are fully wrapped cylinders
    - Burgers have round buns with patties between them
    
    Step 3: Provide your final answer as a JSON array at the end.
    
    Example response:
    "I see three folded corn tortillas arranged in a standing V-shape. The tortillas appear crispy and golden. Visible fillings include ground meat (appears to be seasoned beef), shredded lettuce, diced tomatoes, and shredded cheese on top. The arrangement and exposed fillings clearly indicate these are hard-shell tacos.
    
    Final answer:
    [{"food": "taco", "quantity": 3, "unit": "item", "confidence": 0.9}]"
    
    Your analysis:
    """.trimIndent()
    
    // Single-shot prompt for fast, direct JSON response
    private val singleShotPromptText = """
    Analyze the food items in the image carefully and systematically.
    
    Think step by step:
    1. What is the main food item visible? Look for shape, color, and structure.
    2. Are there any secondary items or condiments?
    3. What quantity can you count or estimate?
    
    Common food identifications:
    - Tacos: folded corn/flour tortillas with fillings visible on top
    - Burritos: fully wrapped cylindrical tortillas
    - Burgers: round buns with patties between them
    - Pizza: flat round/square base with toppings
    
    Instructions:
    - Focus on what you actually see, not what might be common combinations
    - If you see folded tortillas with exposed fillings, they are likely tacos
    - Count individual items when possible
    - Output ONLY a JSON array with your final answer
    
    Example outputs:
    [{"food": "taco", "quantity": 3, "unit": "item", "confidence": 0.85}]
    [{"food": "burger", "quantity": 1, "unit": "item", "confidence": 0.9}]
    
    Valid units: item, piece, slice, cup, serving, tablespoon, teaspoon, grams, ounces
    
    JSON output:
    """.trimIndent()

    suspend fun extract(bitmap: Bitmap): MealAnalysis = withContext(Dispatchers.IO) {
        val modelKey = VisionModelPreferencesManager.getSelectedVisionModel()
        val modelDisplayName = VisionModelPreferencesManager.getVisionModelDisplayName(modelKey)
        Log.i(TAG, "Starting meal photo analysis with $modelDisplayName...")

        val timings = mutableMapOf<String, Long>()
        lateinit var visionSession: LlmInferenceSession

        // Step 1: Get a pre-warmed, high-performance session from the AppContainer.
        timings["Session Creation"] = measureTimeMillis {
            visionSession = appContainer.getReadyVisionSession()
        }

        try {
            // Step 2: Add the text prompt and the image to the session.
            val reasoningMode = ImageReasoningPreferencesManager.getSelectedMode()
            val promptToUse = when (reasoningMode) {
                ImageReasoningPreferencesManager.ImageReasoningMode.REASONING -> reasoningPromptText
                ImageReasoningPreferencesManager.ImageReasoningMode.SINGLE_SHOT -> singleShotPromptText
            }
            
            timings["Text Prompt Add"] = measureTimeMillis {
                Log.d(TAG, "Adding text prompt to session...")
                Log.d(TAG, "Reasoning mode: ${reasoningMode.displayName}")
                Log.d(TAG, "Prompt text: $promptToUse")
                visionSession.addQueryChunk(promptToUse)
                Log.d(TAG, "Text prompt added successfully")
            }
            timings["Image Add"] = measureTimeMillis {
                Log.d(TAG, "Adding image to session...")
                
                // Log image dimensions for debugging
                Log.d(TAG, "Original image dimensions: ${bitmap.width}x${bitmap.height}")
                Log.d(TAG, "Image config: ${bitmap.config}")
                Log.d(TAG, "Has alpha: ${bitmap.hasAlpha()}")
                
                val mpImage = BitmapImageBuilder(bitmap).build()
                visionSession.addImage(mpImage)
                Log.d(TAG, "Image added successfully")
            }

            // Step 3: Run the AI inference. This is the most computationally expensive part.
            lateinit var llmResponse: String
            timings["LLM Inference"] = measureTimeMillis {
                llmResponse = visionSession.generateResponse()
            }
            Log.d(TAG, "Raw AI Response: $llmResponse")

            // Step 4: Parse the JSON response from the AI.
            lateinit var foodItems: List<IdentifiedFoodItem>
            timings["JSON Parsing"] = measureTimeMillis {
                try {
                    val reasoningMode = ImageReasoningPreferencesManager.getSelectedMode()
                    if (reasoningMode == ImageReasoningPreferencesManager.ImageReasoningMode.REASONING) {
                        // Extract and log the reasoning part before parsing JSON
                        val jsonStartIndex = llmResponse.lastIndexOf("[")
                        if (jsonStartIndex > 0) {
                            val reasoning = llmResponse.substring(0, jsonStartIndex).trim()
                            if (reasoning.isNotEmpty()) {
                                Log.i(TAG, "=== MODEL REASONING ===")
                                Log.i(TAG, reasoning)
                                Log.i(TAG, "======================")
                            }
                            // Extract just the JSON part for parsing
                            val jsonPart = llmResponse.substring(jsonStartIndex)
                            foodItems = parseGemmaResponse(jsonPart)
                        } else {
                            // No reasoning found, parse as normal
                            foodItems = parseGemmaResponse(llmResponse)
                        }
                    } else {
                        // Single-shot mode - parse directly
                        foodItems = parseGemmaResponse(llmResponse)
                    }
                } catch (e: InvalidJsonResponseException) {
                    // Re-throw with the model name for better error context
                    throw InvalidJsonResponseException("${modelDisplayName}: ${e.message}")
                }
            }
            Log.i(TAG, "Parsed ${foodItems.size} food items: $foodItems")

            // Step 5: Look up nutritional information for each identified item.
            lateinit var analyzedItems: List<AnalyzedFoodItem>
            timings["Nutrient Lookup"] = measureTimeMillis {
                analyzedItems = foodItems.map { item ->
                    val nutrients = enhancedNutrientDbHelper.lookup(item.food, item.quantity, item.unit)
                    AnalyzedFoodItem(
                        foodName = item.food,
                        quantity = item.quantity,
                        unit = item.unit,
                        calories = nutrients.calories,
                        protein = nutrients.protein,
                        totalFat = nutrients.totalFat,
                        saturatedFat = nutrients.saturatedFat,
                        cholesterol = nutrients.cholesterol,
                        sodium = nutrients.sodium,
                        totalCarbs = nutrients.totalCarbs,
                        dietaryFiber = nutrients.dietaryFiber,
                        sugars = nutrients.sugars,
                        glycemicIndex = nutrients.glycemicIndex,
                        glycemicLoad = nutrients.glycemicLoad
                    )
                }
            }
            Log.i(TAG, "Nutrient analysis complete.")

            val totalCalories = analyzedItems.sumOf { it.calories }
            val totalTime = timings.values.sum()
            
            // Log detailed performance breakdown
            Log.i(TAG, "=== Performance Breakdown ===")
            Log.i(TAG, "Session Creation: ${timings["Session Creation"]}ms")
            Log.i(TAG, "Text Prompt Add: ${timings["Text Prompt Add"]}ms")
            Log.i(TAG, "Image Add: ${timings["Image Add"]}ms")
            Log.i(TAG, "LLM Inference: ${timings["LLM Inference"]}ms")
            Log.i(TAG, "JSON Parsing: ${timings["JSON Parsing"]}ms")
            Log.i(TAG, "Nutrient Lookup: ${timings["Nutrient Lookup"]}ms")
            Log.i(TAG, "Total Time: ${totalTime}ms")
            Log.i(TAG, "Meal analysis complete! Total Calories: $totalCalories")

            // Step 6: Assemble the final MealAnalysis object with performance metrics.
            val performanceMetrics = PerformanceMetrics(
                sessionCreation = timings["Session Creation"] ?: 0,
                textPromptAdd = timings["Text Prompt Add"] ?: 0,
                imageAdd = timings["Image Add"] ?: 0,
                llmInference = timings["LLM Inference"] ?: 0,
                jsonParsing = timings["JSON Parsing"] ?: 0,
                nutrientLookup = timings["Nutrient Lookup"] ?: 0,
                totalTime = totalTime
            )

            return@withContext MealAnalysis(
                totalCalories = totalCalories,
                items = analyzedItems,
                generatedAt = Instant.now(),
                modelName = modelDisplayName,
                performanceMetrics = performanceMetrics
            )
        } finally {
            // Step 7: Clean up the session to release memory.
            visionSession.close()
            // Force garbage collection to help release native GPU/CPU memory used by the model.
            System.gc()
            System.runFinalization()
            Log.d(TAG, "Session cleaned up and memory released.")
        }
    }

    /**
     * Parses the raw text response from the AI model into a list of food items.
     * Throws an exception if the response is not valid JSON.
     */
    @Throws(InvalidJsonResponseException::class)
    private fun parseGemmaResponse(response: String): List<IdentifiedFoodItem> {
        try {
            // Clean the raw string to make it valid JSON.
            var cleanJson = response
                .replace("```json", "")
                .replace("```", "")
                .trim()

            // Try to extract JSON array if it's embedded in text
            if (!cleanJson.startsWith("[")) {
                // Look for JSON array pattern in the response - improved regex for nested objects
                val jsonPattern = "\\[\\s*\\{[^\\]]*\\}\\s*\\]".toRegex(RegexOption.DOT_MATCHES_ALL)
                val match = jsonPattern.find(cleanJson)
                if (match != null) {
                    cleanJson = match.value
                    Log.w(TAG, "Extracted JSON from descriptive response")
                } else {
                    // Check if it's a single object without array brackets
                    if (cleanJson.startsWith("{") && cleanJson.endsWith("}")) {
                        cleanJson = "[$cleanJson]"
                        Log.w(TAG, "Wrapped single object in array")
                    } else {
                        // No valid JSON found
                        Log.e(TAG, "AI returned descriptive text instead of JSON: $response")
                        throw InvalidJsonResponseException(
                            "The AI model did not return valid JSON. Please try switching to a different model (E4B recommended) or try taking a clearer photo."
                        )
                    }
                }
            }

            // Parse the JSON into our temporary data class.
            val listType = object : TypeToken<List<EnhancedFoodItem>>() {}.type
            val enhancedItems: List<EnhancedFoodItem> = gson.fromJson(cleanJson, listType) ?: emptyList()

            // Validate and convert items
            val validItems = enhancedItems
                .filter { it.confidence >= 0.3 } // Only include items the AI is reasonably sure about.
                .map {
                    IdentifiedFoodItem(
                        food = it.food.trim(),
                        quantity = it.quantity.coerceAtLeast(0.1), // Ensure positive quantity
                        unit = (it.unit ?: "item").trim().lowercase() // Normalize unit
                    )
                }
                .filter { it.food.isNotBlank() } // Filter out empty food names
            
            if (validItems.isEmpty()) {
                Log.w(TAG, "No valid items found in JSON response")
                throw InvalidJsonResponseException(
                    "No food items were identified. Please ensure the photo clearly shows food items."
                )
            }
            
            return validItems
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Failed to parse JSON response from AI", e)
            Log.e(TAG, "Raw response was: $response")
            throw InvalidJsonResponseException(
                "Invalid JSON format from AI model. Please try again or switch to a different model."
            )
        } catch (e: InvalidJsonResponseException) {
            throw e // Re-throw our custom exception
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing AI response", e)
            throw InvalidJsonResponseException(
                "Failed to process AI response. Please try again."
            )
        }
    }

}