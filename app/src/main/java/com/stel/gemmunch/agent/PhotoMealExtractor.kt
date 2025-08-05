package com.stel.gemmunch.agent

import android.graphics.Bitmap
import android.util.Log
import com.stel.gemmunch.AppContainer
import com.stel.gemmunch.data.models.EnhancedNutrientDbHelper
import com.stel.gemmunch.data.models.AnalysisProgress
import com.stel.gemmunch.data.models.AnalysisStep
import com.stel.gemmunch.utils.GsonProvider
import com.stel.gemmunch.model.AppMode
import com.stel.gemmunch.utils.VisionModelPreferencesManager
import com.stel.gemmunch.utils.ImageReasoningPreferencesManager
import com.stel.gemmunch.utils.ErrorUtils
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.time.Instant
import kotlin.system.measureTimeMillis
import java.security.MessageDigest

private const val TAG = "PhotoMealExtractor"

/**
 * Exception thrown when the AI model returns a response that cannot be parsed as JSON
 */
class InvalidJsonResponseException(
    message: String,
    val errorType: String? = null,
    val rawResponse: String? = null
) : Exception(message)

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
    
    Step 2: Based on your observations, identify each visible food component.
    Think carefully:
    - For composite foods (tacos, burritos, sandwiches), identify the main components AND visible ingredients
    - Tacos have folded tortillas with exposed fillings on top
    - Burritos are fully wrapped cylinders
    - Burgers have round buns with patties between them
    - Look for proteins, vegetables, sauces, and other identifiable ingredients
    
    Step 3: Provide your final answer as a JSON array at the end.
    Include both the main item AND clearly visible ingredients when possible.
    
    Example response:
    "I see three folded corn tortillas arranged in a standing V-shape. The tortillas appear crispy and golden. Visible fillings include ground meat (appears to be seasoned beef), shredded lettuce, diced tomatoes, and shredded cheese on top. The arrangement and exposed fillings clearly indicate these are hard-shell tacos.
    
    Final answer:
    [
        {"food": "hard shell taco", "quantity": 3, "unit": "item", "confidence": 0.9},
        {"food": "ground beef", "quantity": 0.5, "unit": "cup", "confidence": 0.8},
        {"food": "shredded lettuce", "quantity": 0.25, "unit": "cup", "confidence": 0.9},
        {"food": "diced tomatoes", "quantity": 2, "unit": "tablespoon", "confidence": 0.8},
        {"food": "shredded cheese", "quantity": 2, "unit": "tablespoon", "confidence": 0.9}
    ]"
    
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
    
    // Strict JSON-only prompt for SNAP_AND_LOG mode
    private val strictJsonPromptText = """
    You are a food recognition API. Your only job is to analyze the image and return a valid JSON array listing the identified foods.
    Do not add any explanation or conversational text.
    
    Output format: [{"food": "item name", "quantity": number, "unit": "unit", "confidence": number}]
    
    If you cannot identify any food with high confidence, return: []
    
    Valid units: item, piece, slice, cup, serving, tablespoon, teaspoon, grams, ounces
    Confidence range: 0.0 to 1.0
    
    Examples:
    - [{"food": "apple", "quantity": 1, "unit": "item", "confidence": 0.95}]
    - [{"food": "pasta", "quantity": 2, "unit": "cup", "confidence": 0.8}, {"food": "marinara sauce", "quantity": 0.5, "unit": "cup", "confidence": 0.7}]
    - []
    
    Analyze the image and return only the JSON array:
    """.trimIndent()

    suspend fun extract(
        bitmap: Bitmap, 
        userContext: String? = null,
        appMode: AppMode = AppMode.SNAP_AND_LOG,
        onProgress: ((AnalysisProgress) -> Unit)? = null
    ): MealAnalysis = withContext(Dispatchers.IO) {
        val modelKey = VisionModelPreferencesManager.getSelectedVisionModel()
        val modelDisplayName = VisionModelPreferencesManager.getVisionModelDisplayName(modelKey)
        
        // Calculate image hash for debugging duplicate analyses
        val imageHash = calculateImageHash(bitmap)
        Log.i(TAG, "Starting meal photo analysis with $modelDisplayName...")
        Log.i(TAG, "Image hash: $imageHash (size: ${bitmap.width}x${bitmap.height})")

        val timings = mutableMapOf<String, Long>()
        lateinit var visionSession: LlmInferenceSession
        
        // Initialize progress tracking
        var progress = AnalysisProgress()
        
        // Step 1: Get a pre-warmed, high-performance session from the AppContainer.
        progress = progress.updateStep("Preparing AI Session", AnalysisStep.StepStatus.IN_PROGRESS)
        onProgress?.invoke(progress)
        
        timings["Session Creation"] = measureTimeMillis {
            visionSession = appContainer.getReadyVisionSession()
            Log.d(TAG, "Got fresh AI session for analysis (image hash: $imageHash)")
        }
        
        progress = progress.updateStep("Preparing AI Session", AnalysisStep.StepStatus.COMPLETED, timings["Session Creation"])
        onProgress?.invoke(progress)

        try {
            // Step 2: Add the text prompt and the image to the session.
            val reasoningMode = ImageReasoningPreferencesManager.getSelectedMode()
            val basePrompt = when {
                // Use strict JSON prompt for SNAP_AND_LOG mode
                appMode == AppMode.SNAP_AND_LOG -> strictJsonPromptText
                // Otherwise use the configured reasoning mode
                reasoningMode == ImageReasoningPreferencesManager.ImageReasoningMode.REASONING -> reasoningPromptText
                else -> singleShotPromptText
            }
            
            // Build the final prompt with optional user context
            val promptToUse = buildPromptWithContext(basePrompt, userContext)
            
            // Step 2: Add context/prompt
            progress = progress.updateStep("Adding Context", AnalysisStep.StepStatus.IN_PROGRESS)
            onProgress?.invoke(progress)
            
            timings["Text Prompt Add"] = measureTimeMillis {
                Log.d(TAG, "Adding text prompt to session...")
                Log.d(TAG, "Reasoning mode: ${reasoningMode.displayName}")
                if (!userContext.isNullOrBlank()) {
                    Log.d(TAG, "User context provided: $userContext")
                }
                Log.d(TAG, "Final prompt: $promptToUse")
                visionSession.addQueryChunk(promptToUse)
                Log.d(TAG, "Text prompt added successfully")
            }
            
            progress = progress.updateStep("Adding Context", AnalysisStep.StepStatus.COMPLETED, timings["Text Prompt Add"])
            onProgress?.invoke(progress)
            
            // Step 3: Process image
            progress = progress.updateStep("Processing Image", AnalysisStep.StepStatus.IN_PROGRESS)
            onProgress?.invoke(progress)
            
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
            
            progress = progress.updateStep("Processing Image", AnalysisStep.StepStatus.COMPLETED, timings["Image Add"])
            onProgress?.invoke(progress)

            // Step 4: Run the AI inference. This is the most computationally expensive part.
            lateinit var llmResponse: String
            progress = progress.updateStep("AI Analysis", AnalysisStep.StepStatus.IN_PROGRESS)
            onProgress?.invoke(progress)
            
            timings["LLM Inference"] = measureTimeMillis {
                try {
                    Log.i(TAG, "🚀 Starting AI inference with optimized acceleration...")
                    Log.i(TAG, "📊 Context: ${userContext?.length ?: 0} chars, Image: ${bitmap.width}x${bitmap.height}")
                    Log.i(TAG, "🎯 Model: $modelDisplayName, Mode: ${reasoningMode.displayName}")
                    
                    val inferenceStartTime = System.currentTimeMillis()
                    llmResponse = visionSession.generateResponse()
                    val totalInferenceTime = System.currentTimeMillis() - inferenceStartTime
                    Log.i(TAG, "✅ AI inference completed in ${totalInferenceTime / 1000.0}s")
                    
                    // Log performance insights
                    when {
                        totalInferenceTime < 10000 -> Log.i(TAG, "🚀 Excellent performance - likely using NPU or high-end GPU")
                        totalInferenceTime < 25000 -> Log.i(TAG, "⚡ Good performance - likely using GPU acceleration")
                        totalInferenceTime < 45000 -> Log.i(TAG, "👍 Acceptable performance - may be using CPU with optimizations")
                        else -> Log.w(TAG, "🐌 Slow performance - check acceleration configuration")
                    }
                } catch (e: Exception) {
                    val (errorType, errorMessage) = when {
                        e.message?.contains("Input is too long for the model") == true -> {
                            Log.e(TAG, "Token limit exceeded. This typically happens when a session is reused.", e)
                            "TOKEN_LIMIT_EXCEEDED" to ErrorUtils.getUserFriendlyError(e)
                        }
                        e.message?.contains("OUT_OF_RANGE") == true -> {
                            Log.e(TAG, "Session is in an invalid state", e)
                            "SESSION_ERROR" to ErrorUtils.getUserFriendlyError(e)
                        }
                        else -> {
                            Log.e(TAG, "LLM inference failed", e)
                            "INFERENCE_ERROR" to ErrorUtils.getUserFriendlyError(e)
                        }
                    }
                    
                    // Create error MealAnalysis with timing data collected so far
                    val totalTime = timings.values.sum()
                    val performanceMetrics = PerformanceMetrics(
                        sessionCreation = timings["Session Creation"] ?: 0,
                        textPromptAdd = timings["Text Prompt Add"] ?: 0,
                        imageAdd = timings["Image Add"] ?: 0,
                        llmInference = timings["LLM Inference"] ?: 0,
                        jsonParsing = 0,
                        nutrientLookup = 0,
                        totalTime = totalTime
                    )
                    
                    return@withContext MealAnalysis(
                        totalCalories = 0,
                        items = emptyList(),
                        generatedAt = Instant.now(),
                        modelName = modelDisplayName,
                        performanceMetrics = performanceMetrics,
                        rawAiResponse = null, // No response available for inference errors
                        isError = true,
                        errorType = errorType,
                        errorMessage = errorMessage
                    )
                }
            }
            
            progress = progress.updateStep("AI Analysis", AnalysisStep.StepStatus.COMPLETED, timings["LLM Inference"])
            onProgress?.invoke(progress)
            
            Log.d(TAG, "Raw AI Response: $llmResponse")

            // Step 5: Parse the JSON response from the AI.
            lateinit var foodItems: List<IdentifiedFoodItem>
            progress = progress.updateStep("Parsing Results", AnalysisStep.StepStatus.IN_PROGRESS)
            onProgress?.invoke(progress)
            
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
                    // Create an error MealAnalysis with all the performance data
                    val totalTime = timings.values.sum()
                    val performanceMetrics = PerformanceMetrics(
                        sessionCreation = timings["Session Creation"] ?: 0,
                        textPromptAdd = timings["Text Prompt Add"] ?: 0,
                        imageAdd = timings["Image Add"] ?: 0,
                        llmInference = timings["LLM Inference"] ?: 0,
                        jsonParsing = timings["JSON Parsing"] ?: 0,
                        nutrientLookup = 0, // No nutrient lookup on error
                        totalTime = totalTime
                    )
                    
                    return@withContext MealAnalysis(
                        totalCalories = 0,
                        items = emptyList(),
                        generatedAt = Instant.now(),
                        modelName = modelDisplayName,
                        performanceMetrics = performanceMetrics,
                        rawAiResponse = e.rawResponse ?: llmResponse,
                        isError = true,
                        errorType = e.errorType,
                        errorMessage = "${modelDisplayName}: ${e.message}"
                    )
                }
            }
            
            progress = progress.updateStep("Parsing Results", AnalysisStep.StepStatus.COMPLETED, timings["JSON Parsing"])
            onProgress?.invoke(progress)
            
            Log.i(TAG, "Parsed ${foodItems.size} food items: $foodItems")

            // Step 6: Look up nutritional information for each identified item.
            lateinit var analyzedItems: List<AnalyzedFoodItem>
            progress = progress.updateStep("Looking Up Nutrition", AnalysisStep.StepStatus.IN_PROGRESS)
            onProgress?.invoke(progress)
            
            timings["Nutrient Lookup"] = measureTimeMillis {
                analyzedItems = foodItems.mapIndexed { index, item ->
                    // Update progress with current item being looked up
                    val lookupProgress = "Looking up '${item.food}' (${index + 1}/${foodItems.size})"
                    progress = progress.updateStep("Looking Up Nutrition", AnalysisStep.StepStatus.IN_PROGRESS, details = lookupProgress)
                    onProgress?.invoke(progress)
                    
                    val nutrients = enhancedNutrientDbHelper.lookup(
                        food = item.food, 
                        qty = item.quantity, 
                        unit = item.unit,
                        onUsdaFallback = { foodName ->
                            // Update progress when falling back to USDA API
                            val usdaProgress = "Local miss for '${foodName}' - trying USDA API..."
                            progress = progress.updateStep("Looking Up Nutrition", AnalysisStep.StepStatus.IN_PROGRESS, details = usdaProgress)
                            onProgress?.invoke(progress)
                        }
                    )
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
            
            progress = progress.updateStep("Looking Up Nutrition", AnalysisStep.StepStatus.COMPLETED, timings["Nutrient Lookup"])
            onProgress?.invoke(progress)
            
            Log.i(TAG, "Nutrient analysis complete.")

            val totalCalories = analyzedItems.sumOf { it.calories }
            val totalTime = timings.values.sum()
            
            // Log detailed performance breakdown
            Log.i(TAG, "=== Performance Breakdown ===")
            Log.i(TAG, "Session Creation: ${timings["Session Creation"]}ms")
            Log.i(TAG, "Text Prompt Add: ${timings["Text Prompt Add"]}ms")
            Log.i(TAG, "Image Add: ${timings["Image Add"]}ms")
            Log.i(TAG, "LLM Inference: ${timings["LLM Inference"]}ms (${(timings["LLM Inference"] ?: 0) / 1000.0}s)")
            Log.i(TAG, "JSON Parsing: ${timings["JSON Parsing"]}ms")
            Log.i(TAG, "Nutrient Lookup: ${timings["Nutrient Lookup"]}ms")
            Log.i(TAG, "Total Time: ${totalTime}ms (${totalTime / 1000.0}s)")
            Log.i(TAG, "=== Analysis Summary ===")
            Log.i(TAG, "Model: $modelDisplayName")
            Log.i(TAG, "Items Identified: ${analyzedItems.size}")
            Log.i(TAG, "Total Calories: $totalCalories")
            Log.i(TAG, "Inference Speed: ${if (timings["LLM Inference"] != null && timings["LLM Inference"]!! > 0) "%.2f".format(1000.0 / timings["LLM Inference"]!!) else "N/A"} inferences/second")
            Log.i(TAG, "========================")

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
                performanceMetrics = performanceMetrics,
                rawAiResponse = llmResponse, // Store raw response for feedback
                isEmptyResult = appMode == AppMode.SNAP_AND_LOG && analyzedItems.isEmpty()
            )
        } finally {
            // Step 7: Clean up the session to release memory.
            // Following Google AI Edge Gallery pattern for proper resource cleanup
            try {
                // Update status to cleanup
                appContainer.updateModelStatus(com.stel.gemmunch.ModelStatus.CLEANUP)
                
                visionSession.close()
                Log.d(TAG, "✅ Session closed successfully")
                
                // Notify container that session is closed so it can pre-warm a new one
                appContainer.onSessionClosed()
                
                // Update status back to preparing session after cleanup
                appContainer.updateModelStatus(com.stel.gemmunch.ModelStatus.PREPARING_SESSION)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to close vision session", e)
            }
            
            // Note: Removing System.gc() as it's generally discouraged and 
            // MediaPipe should handle its own memory management
            Log.d(TAG, "Session cleanup completed for image hash: $imageHash")
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
            
            // Determine the specific type of JSON error
            val errorType = when {
                response.contains("][") || response.split("\n").count { it.trim().startsWith("[") } > 1 -> {
                    Log.e(TAG, "AI returned multiple JSON arrays instead of a single array")
                    "MULTIPLE_JSON_ARRAYS"
                }
                response.endsWith("...") || !response.contains("]") -> {
                    Log.e(TAG, "AI response was truncated or incomplete")
                    "MALFORMED_JSON"
                }
                else -> "INVALID_JSON"
            }
            
            throw InvalidJsonResponseException(
                "Invalid JSON format from AI model ($errorType). Please try again or switch to a different model.",
                errorType = errorType,
                rawResponse = response
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

    /**
     * Builds the final prompt by combining the base prompt with optional user context.
     * This allows users to provide hints that can improve AI accuracy.
     */
    private fun buildPromptWithContext(basePrompt: String, userContext: String?): String {
        if (userContext.isNullOrBlank()) {
            return basePrompt
        }
        
        // Enhance the prompt with user context, prioritizing restaurant-specific items
        val contextHint = buildContextHint(userContext)
        
        return """
        ADDITIONAL CONTEXT: $contextHint
        
        $basePrompt
        """.trimIndent()
    }
    
    /**
     * Analyzes user context and builds specific hints for the AI model.
     */
    private fun buildContextHint(userContext: String): String {
        val lowerContext = userContext.lowercase().trim()
        val hints = mutableListOf<String>()
        
        // Restaurant-specific hints
        when {
            lowerContext.contains("chipotle") -> {
                hints.add("This meal is from Chipotle Mexican Grill. Look for items like burritos, bowls, tacos with ingredients such as barbacoa, carnitas, sofritas, cilantro-lime rice, black beans, pinto beans, guacamole, and various salsas.")
                hints.add("Prioritize Chipotle menu items in your identification.")
            }
            lowerContext.contains("mexican") -> {
                hints.add("This appears to be Mexican cuisine. Look for tacos, burritos, quesadillas, rice, beans, and traditional Mexican ingredients.")
            }
            lowerContext.contains("fast food") || lowerContext.contains("fast-food") -> {
                hints.add("This is likely from a fast food restaurant. Consider standardized portion sizes and common fast food items.")
            }
        }
        
        // Meal type hints
        when {
            lowerContext.contains("breakfast") -> hints.add("This is a breakfast meal. Look for typical breakfast foods.")
            lowerContext.contains("lunch") -> hints.add("This is a lunch meal.")
            lowerContext.contains("dinner") -> hints.add("This is a dinner meal.")
            lowerContext.contains("snack") -> hints.add("This is a snack or light meal.")
        }
        
        // Dietary hints
        when {
            lowerContext.contains("vegetarian") -> hints.add("This meal is vegetarian - no meat products.")
            lowerContext.contains("vegan") -> hints.add("This meal is vegan - no animal products.")
            lowerContext.contains("gluten free") || lowerContext.contains("gluten-free") -> {
                hints.add("This meal is gluten-free.")
            }
        }
        
        // Specific food hints
        if (lowerContext.contains("bowl")) {
            hints.add("This appears to be served in a bowl format.")
        }
        if (lowerContext.contains("burrito")) {
            hints.add("Look carefully for burrito characteristics - wrapped tortilla format.")
        }
        if (lowerContext.contains("taco")) {
            hints.add("Look for taco characteristics - folded tortillas with exposed fillings.")
        }
        
        // If no specific hints identified, use the raw context
        if (hints.isEmpty()) {
            return "User notes: $userContext"
        }
        
        return hints.joinToString(" ") + if (hints.size == 1) " User notes: $userContext" else ""
    }
    
    /**
     * Calculates a hash of the bitmap for debugging purposes.
     * This helps identify when the same image is being analyzed multiple times.
     */
    private fun calculateImageHash(bitmap: Bitmap): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            
            // Sample pixels for faster hashing (every 10th pixel)
            val step = 10
            for (y in 0 until bitmap.height step step) {
                for (x in 0 until bitmap.width step step) {
                    val pixel = bitmap.getPixel(x, y)
                    digest.update(pixel.toByte())
                }
            }
            
            // Convert to hex string
            digest.digest().joinToString("") { "%02x".format(it) }.take(16)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate image hash", e)
            "unknown"
        }
    }

}