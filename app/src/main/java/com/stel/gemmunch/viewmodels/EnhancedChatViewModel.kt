package com.stel.gemmunch.viewmodels

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.stel.gemmunch.AppContainer
import com.stel.gemmunch.ModelStatus
import com.stel.gemmunch.agent.AnalyzedFoodItem
import com.stel.gemmunch.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID

private const val TAG = "EnhancedChatViewModel"

/**
 * Enhanced ChatViewModel that simulates function calling behavior
 * Uses structured prompts to achieve agent-like behavior without FC SDK
 */
class EnhancedChatViewModel(
    private val appContainer: AppContainer,
    private val isMultimodal: Boolean = false,
    private val initialImagePath: String? = null
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentMealNutrition = MutableStateFlow<List<AnalyzedFoodItem>>(emptyList())
    val currentMealNutrition: StateFlow<List<AnalyzedFoodItem>> = _currentMealNutrition.asStateFlow()

    private val _showHealthConnectDialog = MutableStateFlow(false)
    val showHealthConnectDialog: StateFlow<Boolean> = _showHealthConnectDialog.asStateFlow()

    private val _showResetDialog = MutableStateFlow(false)
    val showResetDialog: StateFlow<Boolean> = _showResetDialog.asStateFlow()

    // Track current image and conversation state
    private var currentImagePath: String? = initialImagePath
    private var currentImageBitmap: Bitmap? = null
    private var awaitingUserResponse = false
    private var conversationContext = mutableListOf<String>()
    
    // Track current generation job for cancellation
    private var currentGenerationJob: kotlinx.coroutines.Job? = null
    private var isGenerationCancelled = false

    init {
        viewModelScope.launch {
            if (isMultimodal && initialImagePath != null) {
                loadImageBitmap(initialImagePath)
                startImageAnalysis()
            }
        }
    }

    private suspend fun loadImageBitmap(imagePath: String) {
        withContext(Dispatchers.IO) {
            try {
                currentImageBitmap = BitmapFactory.decodeFile(imagePath)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image", e)
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // Reset cancellation flag for new generation
        isGenerationCancelled = false
        
        currentGenerationJob = viewModelScope.launch {
            // Add user message
            addMessage(ChatMessage(
                id = UUID.randomUUID().toString(),
                text = text,
                isFromUser = true
            ))

            _isLoading.value = true
            appContainer.updateModelStatus(ModelStatus.RUNNING_INFERENCE)
            
            try {
                if (awaitingUserResponse) {
                    // Continue with clarification
                    processClarification(text)
                } else {
                    // Check if this is a dish correction for multimodal analysis
                    val correctionKeywords = listOf("no", "wrong", "not", "actually", "it's", "this is")
                    val isDishCorrection = isMultimodal && correctionKeywords.any { keyword -> 
                        text.lowercase().contains(keyword) 
                    }
                    
                    if (isDishCorrection && currentImageBitmap != null) {
                        // User is correcting the dish identification - re-analyze
                        reAnalyzeWithCorrection(text)
                    } else {
                        // Start new analysis
                        startNewAnalysis(text)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message", e)
                addMessage(ChatMessage(
                    text = "I encountered an error. Please try again.",
                    isFromUser = false
                ))
            } finally {
                _isLoading.value = false
                appContainer.updateModelStatus(ModelStatus.READY)
            }
        }
    }

    private suspend fun startImageAnalysis() {
        // Reset cancellation flag for new generation
        isGenerationCancelled = false
        _isLoading.value = true
        appContainer.updateModelStatus(ModelStatus.RUNNING_INFERENCE)
        
        try {
            addMessage(ChatMessage(
                text = "Analyzing your meal photo...",
                isFromUser = false
            ))

            // Use vision session to analyze image
            val analysisResult = analyzeImageWithVision()
            processAnalysisResult(analysisResult)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image", e)
            addMessage(ChatMessage(
                text = "I'm having trouble analyzing the image. Could you describe what you see?",
                isFromUser = false
            ))
        } finally {
            _isLoading.value = false
            appContainer.updateModelStatus(ModelStatus.READY)
        }
    }

    private suspend fun startNewAnalysis(userInput: String) {
        _currentMealNutrition.value = emptyList()
        
        val prompt = if (isMultimodal && currentImageBitmap != null) {
            buildImageAnalysisPrompt(userInput)
        } else {
            buildTextAnalysisPrompt(userInput)
        }

        val response = callAI(prompt, includeImage = isMultimodal)
        processAnalysisResult(response)
    }
    
    private suspend fun reAnalyzeWithCorrection(correctedDishName: String) {
        // Clear previous nutrition data
        _currentMealNutrition.value = emptyList()
        
        // Re-run the image analysis with the correct dish information
        val prompt = """Thank you for the correction! You told me this is: "$correctedDishName"

Now let me re-analyze the image with this correct information.

STEP 1 - RE-ANALYSIS WITH CORRECT DISH:
Looking at the meal photo again, knowing this is $correctedDishName, let me identify the ingredients properly.

STEP 2 - DETAILED INGREDIENT ANALYSIS:
For $correctedDishName, reason through:
- What ingredients are clearly visible in the photo
- What ingredients are typically part of $correctedDishName but might not be clearly visible
- Appropriate portion size estimates for each component

STEP 3 - STRUCTURED RESPONSE:
**My corrected analysis of your meal:**
This is $correctedDishName as you specified.

**Ingredients I can identify:**
- [List ingredients you can actually see in the photo with portions]

**Ingredients that might be present but I can't see clearly:**
- [List typical ingredients for $correctedDishName that might be present with estimated portions]

**Questions:**
1. Are any identified foods incorrect or wrong portion size?
2. Are any typical ingredients present but not visible in the photo?

Provide the detailed ingredient analysis for $correctedDishName:"""

        val response = callAI(prompt, includeImage = isMultimodal)
        processAnalysisResult(response)
    }

    private suspend fun analyzeImageWithVision(): String {
        return withContext(Dispatchers.IO) {
            val session = appContainer.getConversationalSession()
            
            // Add image to session if available
            currentImageBitmap?.let { bitmap ->
                Log.d(TAG, "Adding image to vision session: ${bitmap.width}x${bitmap.height}")
                val mpImage = BitmapImageBuilder(bitmap).build()
                session.addImage(mpImage)
            }

            val prompt = """You are a nutrition expert analyzing a meal photo. First identify the main dish, then ask for confirmation before detailed analysis.

STEP 1 - INITIAL DISH IDENTIFICATION:
Look at the meal photo and identify what you think the main dish is.

STEP 2 - VERIFICATION REQUEST:
Present your identification and ask the user to confirm before proceeding to detailed ingredient analysis.

RESPONSE FORMAT:
**My initial assessment:**
I'm looking at your meal photo, and this appears to be [your best guess of the main dish/food].

**Before I analyze the ingredients in detail, is this correct?**
- If yes, I'll proceed with detailed ingredient analysis
- If no, please tell me what dish this actually is so I can analyze it properly

EXAMPLE RESPONSE:
**My initial assessment:**
I'm looking at your meal photo, and this appears to be a Pad Thai dish with noodles.

**Before I analyze the ingredients in detail, is this correct?**
- If yes, I'll proceed with detailed ingredient analysis  
- If no, please tell me what dish this actually is so I can analyze it properly

Now provide your initial dish identification:"""
            
            session.addQueryChunk(prompt)
            
            // Use streaming response for natural conversation
            generateStreamingResponse(session)
        }
    }

    private fun buildImageAnalysisPrompt(userInput: String): String {
        // Check if user is correcting the initial dish identification
        val correctionKeywords = listOf("no", "wrong", "not", "actually", "it's", "this is")
        val isCorrection = correctionKeywords.any { keyword -> 
            userInput.lowercase().contains(keyword) 
        }
        
        return if (isCorrection) {
            // User is correcting the dish identification - re-analyze with their input
            """Thank you for the correction! You told me: "$userInput"

Now let me re-analyze the image with this correct information.

STEP 1 - RE-ANALYSIS WITH CORRECT DISH:
Looking at the meal photo again, knowing this is actually $userInput, let me identify the ingredients properly.

STEP 2 - DETAILED INGREDIENT ANALYSIS:
Based on this being $userInput, here's my analysis:

**My corrected analysis of your meal:**
This is $userInput as you specified.

**Ingredients I can identify:**
- [Ingredient 1]: [portion estimate]
- [Ingredient 2]: [portion estimate]
- [etc.]

**Ingredients that might be present but I can't see clearly:**
- [Potential ingredient 1]: [estimated portion]  
- [Potential ingredient 2]: [estimated portion]
- [etc.]

**Questions:**
1. Are any identified foods incorrect or wrong portion size?
2. Are any typical ingredients present but not visible in the photo?

Provide the detailed ingredient analysis for $userInput:"""
        } else {
            // Check if user confirmed the dish - trigger structured ingredient analysis
            val confirmationKeywords = listOf("yes", "correct", "right", "that's right", "confirmed", "chicken pad thai")
            val isConfirmation = confirmationKeywords.any { keyword -> 
                userInput.lowercase().contains(keyword) 
            }
            
            if (isConfirmation) {
                // User confirmed - do structured ingredient analysis
                """Great! Now that we've confirmed this is Chicken Pad Thai, let me analyze the ingredients systematically.

REASONING PROCESS:
1. First, identify clearly visible ingredients in the photo
2. Second, consider typical Chicken Pad Thai ingredients that might be present but hard to see
3. Present structured lists with estimated quantities
4. Ask for user confirmation and corrections

STRUCTURED INGREDIENT ANALYSIS:

**Ingredients I can identify from the photo:**
[Count the visible ingredients and list them with quantities]

**Ingredients that may be present but I can't see clearly (typical for Chicken Pad Thai):**
[List common Chicken Pad Thai ingredients not clearly visible]

**Questions:**
1. Are any identified foods incorrect or wrong portion size?
2. Are any typical ingredients present but not visible in the photo?

Format your response with clear counts and specific quantities:"""
            } else {
                // Continue with normal conversation flow
                """You are continuing a nutrition analysis conversation. The user provided feedback: "$userInput"

ANALYZE their feedback and respond appropriately:

IF they are providing corrections or additions to ingredients:
- Acknowledge their corrections
- Update your ingredient list based on their input
- Ask if there are any other changes needed
- Once they seem satisfied, offer to proceed to nutritional analysis

IF they want to proceed to nutritional analysis:
- Present the final confirmed ingredient list
- Ask: "Should I proceed to calculate the nutritional information for these items?"

IF they want corrections to nutritional data:
- Acknowledge their feedback
- Update the nutritional information
- Present updated totals
- Ask if they want to save to Health Connect

MAINTAIN the structured, reasoning-based approach throughout the conversation.

User's feedback: "$userInput"

Respond appropriately based on where we are in the analysis process:"""
            }
        }
    }

    private fun buildTextAnalysisPrompt(userInput: String): String {
        return """You are a nutrition expert analyzing a meal description. Use structured reasoning to identify foods and ingredients.

The user described: "$userInput"

STEP 1 - MEAL INTERPRETATION:
Analyze what they described and identify the main components.

STEP 2 - INGREDIENT REASONING:
For each food item mentioned, reason through:
- What ingredients are explicitly mentioned
- What ingredients are typically part of this food
- Reasonable portion size estimates
- Missing details that need clarification

STEP 3 - STRUCTURED RESPONSE FORMAT:
Present your analysis as:

**My interpretation of your meal:**
[State your understanding of what they described]

**Ingredients/components I understand:**
- [Item 1]: [portion estimate]
- [Item 2]: [portion estimate]
- [etc.]

**Additional details I'm assuming (please correct if wrong):**
- [Assumption 1]: [portion estimate]
- [Assumption 2]: [portion estimate]
- [etc.]

**Questions to clarify:**
- [Specific question about portions/preparation]
- [Specific question about ingredients]
- [etc.]

EXAMPLE:
**My interpretation of your meal:**
You had a turkey sandwich with avocado on whole wheat bread.

**Ingredients/components I understand:**
- Turkey: (portion size unclear)
- Avocado: (amount unclear)
- Whole wheat bread: 2 slices

**Additional details I'm assuming (please correct if wrong):**
- Turkey slices: 3-4 oz (deli-style)
- Avocado: 1/4 to 1/2 avocado
- Condiments/seasonings: minimal

**Questions to clarify:**
- How much turkey was in the sandwich?
- How much avocado did you use?
- Any other ingredients like lettuce, tomato, mayo, etc.?

Now analyze their meal description: "$userInput"

Use this structured approach:"""
    }

    private suspend fun callAI(prompt: String, includeImage: Boolean = false): String {
        return withContext(Dispatchers.IO) {
            try {
                val session = appContainer.getConversationalSession()
                
                // Add image to session if needed
                if (includeImage && currentImageBitmap != null) {
                    Log.d(TAG, "Adding image for analysis: ${currentImageBitmap!!.width}x${currentImageBitmap!!.height}")
                    val mpImage = BitmapImageBuilder(currentImageBitmap!!).build()
                    session.addImage(mpImage)
                }
                
                // Build proper Gemma chat template
                val fullPrompt = buildGemmaPrompt(prompt)
                
                session.addQueryChunk(fullPrompt)
                
                // Use streaming response for natural conversation
                val response = generateStreamingResponse(session)
                
                // Add to conversation history
                conversationContext.add("User: ${prompt.take(100)}...")
                conversationContext.add("Assistant: ${response.take(100)}...")
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "Error calling AI", e)
                throw e
            }
        }
    }
    
    private suspend fun generateStreamingResponse(session: com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession): String {
        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            val responseBuilder = StringBuilder()
            var currentMessageId: String? = null
            var hasResumed = false // Track if continuation has been resumed
            
            continuation.invokeOnCancellation {
                Log.d(TAG, "Streaming response cancelled via coroutine cancellation")
                if (!hasResumed) {
                    hasResumed = true
                    // Note: This will be called if the coroutine is cancelled externally
                }
            }
            
            try {
                session.generateResponseAsync { partialResult, done ->
                    Log.d(TAG, "Streaming token: '$partialResult', done: $done")
                    
                    // Check if we've already resumed or if generation was cancelled
                    if (hasResumed || isGenerationCancelled) {
                        Log.d(TAG, "Generation cancelled or already completed - ignoring token")
                        return@generateResponseAsync
                    }
                    
                    responseBuilder.append(partialResult)
                    
                    // Update UI with streaming text
                    viewModelScope.launch {
                        // Double-check cancellation in coroutine context
                        if (isGenerationCancelled) {
                            Log.d(TAG, "Generation cancelled during UI update")
                            return@launch
                        }
                        
                        if (currentMessageId == null) {
                            // Create initial streaming message
                            currentMessageId = UUID.randomUUID().toString()
                            val streamingMessage = ChatMessage(
                                id = currentMessageId!!,
                                text = partialResult,
                                isFromUser = false,
                                isStreaming = true
                            )
                            addMessage(streamingMessage)
                        } else {
                            // Update existing message with accumulated response
                            updateStreamingMessage(currentMessageId!!, responseBuilder.toString(), !done)
                        }
                        
                        if (done) {
                            // Mark message as complete
                            val finalResponse = responseBuilder.toString()
                            
                            // Only update UI if not cancelled
                            if (!isGenerationCancelled) {
                                updateStreamingMessage(currentMessageId!!, finalResponse, false)
                                
                                // Log the complete response for debugging
                                Log.d(TAG, "=== COMPLETE AI RESPONSE ===")
                                Log.d(TAG, finalResponse)
                                Log.d(TAG, "=== END RESPONSE (${finalResponse.length} chars) ===")
                            } else {
                                Log.d(TAG, "Generation was cancelled - not updating final message")
                            }
                            
                            hasResumed = true
                            continuation.resume(if (isGenerationCancelled) "" else finalResponse) {}
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in streaming response", e)
                if (continuation.isActive && !hasResumed) {
                    hasResumed = true
                    continuation.resume("") {}
                }
            }
        }
    }

    private fun buildGemmaPrompt(content: String): String {
        // Use Gemma chat template format with structured reasoning guidance
        val conversationFraming = """You are a nutrition expert conducting a structured meal analysis conversation. Follow these guidelines:

STRUCTURED APPROACH:
- Use clear reasoning steps to analyze meals
- Present information in organized sections with bold headers
- Ask specific, targeted questions for clarification
- Guide the conversation toward complete nutritional analysis

CONVERSATION FLOW:
1. Initial analysis with ingredient reasoning
2. Iterative refinement based on user feedback
3. Final ingredient list confirmation  
4. Nutritional calculation and presentation
5. Health Connect storage offer

FORMAT REQUIREMENTS:
- Use **bold headers** for sections
- Use bullet points for ingredient lists
- Be specific about portions and measurements
- Maintain professional but friendly tone

"""
        
        return """<bos><start_of_turn>user
$conversationFraming

$content<end_of_turn>
<start_of_turn>model
"""
    }

    private suspend fun processAnalysisResult(response: String) {
        Log.d(TAG, "Processing conversational response: ${response.take(100)}...")
        
        // Check if the response contains ingredient lists that we should process
        if (response.contains("**Ingredients I can identify") || 
            response.contains("**Ingredients that may be present") ||
            response.contains("**Final Ingredient List:**") || 
            response.contains("**Nutritional Information:**")) {
            
            Log.d(TAG, "Response contains ingredient analysis - extracting for nutrition lookup")
            
            // Try to extract any ingredient lists or nutritional data
            tryExtractNutritionalData(response)
        }
        
        // Check if the response asks about Health Connect
        if (response.contains("Health Connect") || response.contains("save") || response.contains("track")) {
            // If we have nutrition data, show the Health Connect dialog
            if (_currentMealNutrition.value.isNotEmpty()) {
                _showHealthConnectDialog.value = true
            }
        }
        
        // The streaming has already shown this message to the user
        // Additional processing is done based on conversation state
    }
    
    private suspend fun tryExtractNutritionalData(response: String) {
        // Look for structured ingredient lists in the response
        val ingredientListPattern = """- ([^:]+):\s*([^\n]+)""".toRegex()
        val matches = ingredientListPattern.findAll(response)
        
        val extractedItems = mutableListOf<com.stel.gemmunch.agent.AnalyzedFoodItem>()
        
        for (match in matches) {
            val ingredient = match.groupValues[1].trim()
            val portionText = match.groupValues[2].trim()
            
            // Try to parse portion and unit
            val (quantity, unit) = parsePortionText(portionText)
            
            if (quantity > 0 && ingredient.isNotBlank()) {
                Log.d(TAG, "Extracted ingredient: $ingredient, $quantity $unit")
                
                // Look up nutrition data
                try {
                    val nutritionData = withContext(Dispatchers.IO) {
                        appContainer.nutritionSearchService.searchNutrition(
                            foodName = ingredient,
                            servingSize = quantity,
                            servingUnit = unit
                        )
                    }
                    
                    nutritionData?.let { extractedItems.add(it) }
                } catch (e: Exception) {
                    Log.e(TAG, "Error looking up nutrition for $ingredient", e)
                }
            }
        }
        
        if (extractedItems.isNotEmpty()) {
            _currentMealNutrition.value = extractedItems
            Log.d(TAG, "Updated meal nutrition with ${extractedItems.size} items")
        }
    }
    
    private fun parsePortionText(portionText: String): Pair<Double, String> {
        // Simple parsing of portion text like "1.5 cups", "3-4 oz", "2 slices"
        val numberPattern = """(\d+(?:\.\d+)?|\d+-\d+)""".toRegex()
        val match = numberPattern.find(portionText)
        
        return if (match != null) {
            val numberText = match.value
            val quantity = if (numberText.contains("-")) {
                // Handle ranges like "3-4" by taking the middle
                val parts = numberText.split("-")
                (parts[0].toDouble() + parts[1].toDouble()) / 2
            } else {
                numberText.toDouble()
            }
            
            val unit = portionText.replace(numberText, "").trim()
                .takeIf { it.isNotBlank() } ?: "serving"
            
            Pair(quantity, unit)
        } else {
            Pair(1.0, "serving")
        }
    }

    private fun extractJsonFromResponse(response: String): String {
        // Try to extract JSON from response
        val startIndex = response.indexOf('{')
        var endIndex = response.lastIndexOf('}')
        
        if (startIndex >= 0) {
            // If we found a start but no valid end, try to complete the JSON
            if (endIndex <= startIndex) {
                // Look for incomplete JSON patterns and try to complete them
                val partialJson = response.substring(startIndex)
                
                // Count open brackets to determine how many closes we need
                var openBrackets = 0
                var openSquare = 0
                for (char in partialJson) {
                    when (char) {
                        '{' -> openBrackets++
                        '}' -> openBrackets--
                        '[' -> openSquare++
                        ']' -> openSquare--
                    }
                }
                
                // Build completion
                val completion = StringBuilder(partialJson)
                
                // Close any open arrays
                repeat(openSquare) { completion.append(']') }
                
                // Close any open objects
                repeat(openBrackets) { completion.append('}') }
                
                Log.w(TAG, "Completed incomplete JSON. Added $openSquare ] and $openBrackets }")
                return completion.toString()
            }
            
            return response.substring(startIndex, endIndex + 1)
        } else {
            // Construct JSON from plain text response
            return """{"confirmedIngredients": [], "needsClarification": true, "question": "Could you please describe your meal in more detail?"}"""
        }
    }

    private suspend fun parseSimpleTextResponse(response: String) {
        // Simple fallback parsing
        val foodPattern = Regex("([\\w\\s]+)\\s*[-:]\\s*(\\d+)\\s*cal", RegexOption.IGNORE_CASE)
        val matches = foodPattern.findAll(response)
        
        for (match in matches) {
            val foodName = match.groupValues[1].trim()
            addMessage(ChatMessage(
                text = "Looking up: $foodName...",
                isFromUser = false
            ))
            
            val nutritionData = withContext(Dispatchers.IO) {
                appContainer.nutritionSearchService.searchNutrition(
                    foodName = foodName,
                    servingSize = 1.0,
                    servingUnit = "serving"
                )
            }
            
            nutritionData?.let {
                _currentMealNutrition.value = _currentMealNutrition.value + it
            }
        }
        
        if (_currentMealNutrition.value.isNotEmpty()) {
            showNutritionSummary()
            _showHealthConnectDialog.value = true
        } else {
            addMessage(ChatMessage(
                text = "I couldn't identify specific foods. Could you list them individually?",
                isFromUser = false
            ))
            awaitingUserResponse = true
        }
    }

    private suspend fun processClarification(userResponse: String) {
        awaitingUserResponse = false
        
        // Check if user wants to proceed to nutritional analysis
        val proceedKeywords = listOf("looks good", "correct", "yes", "proceed", "calculate", "nutrition", "analyze", "continue", "go ahead", "sure")
        val userWantsToProceed = proceedKeywords.any { keyword -> 
            userResponse.lowercase().contains(keyword) 
        }
        
        if (userWantsToProceed && userResponse.length < 100) {
            // User confirmed ingredient list - get JSON and process nutrition directly
            Log.d(TAG, "User confirmed ingredients - requesting JSON for direct nutrition lookup")
            
            // Request structured JSON data instead of streaming response
            val prompt = """The user confirmed the ingredient list: "$userResponse"

Output ONLY a JSON object with the confirmed ingredients for nutrition lookup:

{
  "confirmedIngredients": [
    {"name": "rice noodles", "quantity": 1.5, "unit": "cups cooked"},
    {"name": "chicken breast", "quantity": 4, "unit": "oz"},
    {"name": "bean sprouts", "quantity": 0.25, "unit": "cup"},
    {"name": "lime", "quantity": 1, "unit": "wedge"}
  ]
}

Output ONLY the JSON, nothing else:"""
            
            // Use one-shot session for JSON response (no streaming)
            val jsonResponse = callAIForJSON(prompt, includeImage = isMultimodal)
            processJSONIngredients(jsonResponse)
        } else {
            // Continue refining the ingredient list
            val prompt = """You are continuing a nutrition analysis conversation. The user provided feedback: "$userResponse"

RESPOND based on their feedback:

IF they are correcting or adding ingredients:
- Acknowledge their corrections
- Update the ingredient list using the format:
  **[n] ingredients I can identify from the photo:**
  **[y] ingredients that may be present but I can't see clearly:**
- Ask the 2 essential questions only

IF they seem satisfied with the ingredient list:
- Present the **Final Confirmed Ingredient List** with specific portions
- Ask: "Should I proceed to calculate the nutritional information for these items?"

MAINTAIN the structured format with numbered counts and bullet points.

User feedback: "$userResponse"

Continue the structured analysis:"""
            
            val response = callAI(prompt, includeImage = isMultimodal)
            processAnalysisResult(response)
        }
    }
    
    private suspend fun showNutritionAnalysisWithCounts() {
        val items = _currentMealNutrition.value
        if (items.isEmpty()) return
        
        val knownItems = items.filter { it.calories > 0 }
        val unknownItems = items.filter { it.calories == 0 }
        
        val totalCalories = knownItems.sumOf { it.calories }
        val totalProtein = knownItems.mapNotNull { it.protein }.sum()
        val totalCarbs = knownItems.mapNotNull { it.totalCarbs }.sum()
        val totalFat = knownItems.mapNotNull { it.totalFat }.sum()
        val totalGlycemicLoad = knownItems.mapNotNull { it.glycemicLoad }.sum()
        val hasGlycemicData = knownItems.any { it.glycemicIndex != null }

        val nutritionSummary = buildString {
            appendLine("**Nutrition Analysis Complete!**")
            appendLine()
            
            if (knownItems.isNotEmpty()) {
                appendLine("**Known Items (${knownItems.size}):**")
                knownItems.forEach { item ->
                    append("• ${item.foodName} - ${item.calories} cal")
                    // Add glycemic index if available
                    item.glycemicIndex?.let { gi ->
                        append(" (GI: $gi")
                        item.glycemicLoad?.let { gl ->
                            append(", GL: ${String.format("%.1f", gl)}")
                        }
                        append(")")
                    }
                    appendLine()
                }
                appendLine()
                appendLine("**Total from Known Items:**")
                appendLine("• Calories: $totalCalories cal")
                appendLine("• Protein: ${totalProtein.toInt()}g")
                appendLine("• Carbs: ${totalCarbs.toInt()}g")
                appendLine("• Fat: ${totalFat.toInt()}g")
                
                // Add glycemic load summary if available
                if (hasGlycemicData) {
                    val glCategory = when {
                        totalGlycemicLoad <= 10 -> "Low"
                        totalGlycemicLoad <= 19 -> "Medium"
                        else -> "High"
                    }
                    appendLine("• Glycemic Load: ${String.format("%.1f", totalGlycemicLoad)} ($glCategory)")
                }
                appendLine()
            }
            
            if (unknownItems.isNotEmpty()) {
                appendLine("**Unknown Items (${unknownItems.size}) - No nutrition data found:**")
                unknownItems.forEach { item ->
                    appendLine("• ${item.foodName}")
                }
                appendLine()
            }
            
            appendLine("**Is this correct? Any adjustments needed?**")
            if (totalCalories > 0) {
                appendLine("If this looks good, would you like to save to Health Connect?")
            }
        }

        addMessage(ChatMessage(
            text = nutritionSummary,
            isFromUser = false
        ))
    }
    
    private suspend fun callAIForJSON(prompt: String, includeImage: Boolean = false): String {
        return withContext(Dispatchers.IO) {
            try {
                // Use one-shot session with deterministic settings for JSON
                val session = appContainer.getReadyVisionSession() // Uses deterministic settings
                
                // Add image to session if needed
                if (includeImage && currentImageBitmap != null) {
                    Log.d(TAG, "Adding image for JSON analysis: ${currentImageBitmap!!.width}x${currentImageBitmap!!.height}")
                    val mpImage = BitmapImageBuilder(currentImageBitmap!!).build()
                    session.addImage(mpImage)
                }
                
                // Build Gemma prompt for JSON output
                val fullPrompt = buildGemmaPrompt(prompt)
                session.addQueryChunk(fullPrompt)
                
                // Use blocking call for JSON (no streaming needed)
                val response = session.generateResponse()
                
                Log.d(TAG, "JSON Response received: ${response.take(200)}...")
                response
            } catch (e: Exception) {
                Log.e(TAG, "Error calling AI for JSON", e)
                throw e
            }
        }
    }
    
    private suspend fun processJSONIngredients(jsonResponse: String) {
        try {
            // Extract JSON from response
            val jsonString = extractJsonFromResponse(jsonResponse)
            Log.d(TAG, "Extracted JSON for parsing: $jsonString")
            
            val json = try {
                JSONObject(jsonString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse JSON, attempting repair", e)
                // Try to repair common JSON issues
                val repairedJson = jsonString
                    .replace(",]", "]")  // Remove trailing commas in arrays
                    .replace(",}", "}")   // Remove trailing commas in objects
                    .replace(",,", ",")   // Remove double commas
                JSONObject(repairedJson)
            }
            
            val confirmedIngredients = json.optJSONArray("confirmedIngredients") ?: JSONArray()
            val extractedItems = mutableListOf<com.stel.gemmunch.agent.AnalyzedFoodItem>()
            
            // Show progress message
            addMessage(ChatMessage(
                text = "Processing ${confirmedIngredients.length()} ingredients for nutrition lookup...",
                isFromUser = false
            ))
            
            // Process each ingredient
            for (i in 0 until confirmedIngredients.length()) {
                val ingredient = confirmedIngredients.getJSONObject(i)
                val foodName = ingredient.getString("name")
                val quantity = ingredient.getDouble("quantity")
                val unit = ingredient.getString("unit")
                
                Log.d(TAG, "Looking up nutrition: $foodName ($quantity $unit)")
                
                // Lookup nutrition data
                try {
                    val nutritionData = withContext(Dispatchers.IO) {
                        appContainer.nutritionSearchService.searchNutrition(
                            foodName = foodName,
                            servingSize = quantity,
                            servingUnit = unit
                        )
                    }
                    
                    nutritionData?.let { extractedItems.add(it) }
                } catch (e: Exception) {
                    Log.e(TAG, "Error looking up nutrition for $foodName", e)
                }
            }
            
            // Update nutrition data and show results
            _currentMealNutrition.value = extractedItems
            showNutritionAnalysisWithCounts()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing JSON ingredients: $jsonResponse", e)
            addMessage(ChatMessage(
                text = "I had trouble processing the ingredient list. Let me try again with a different approach.",
                isFromUser = false
            ))
        }
    }

    private fun showNutritionSummary() {
        val items = _currentMealNutrition.value
        if (items.isEmpty()) return

        val totalCalories = items.sumOf { it.calories }
        val totalProtein = items.mapNotNull { it.protein }.sum()
        val totalCarbs = items.mapNotNull { it.totalCarbs }.sum()
        val totalFat = items.mapNotNull { it.totalFat }.sum()

        val summary = buildString {
            appendLine("\n**Nutrition Summary:**")
            appendLine()
            items.forEach { item ->
                appendLine("✓ ${item.foodName} - ${item.calories} cal")
            }
            appendLine()
            appendLine("**Total: $totalCalories calories**")
            appendLine("• Protein: ${totalProtein.toInt()}g")
            appendLine("• Carbs: ${totalCarbs.toInt()}g")
            appendLine("• Fat: ${totalFat.toInt()}g")
        }

        addMessage(ChatMessage(
            text = summary,
            isFromUser = false
        ))
    }

    fun saveToHealthConnect() {
        viewModelScope.launch {
            try {
                val items = _currentMealNutrition.value
                if (items.isNotEmpty()) {
                    // Combine all items into a single meal record
                    val combinedNutrition = appContainer.nutritionSearchService.combineNutritionData(items)
                    
                    withContext(Dispatchers.IO) {
                        appContainer.healthConnectManager.writeNutritionRecords(
                            items = listOf(combinedNutrition),
                            mealDateTime = java.time.Instant.now()
                        )
                    }
                    
                    addMessage(ChatMessage(
                        text = "✓ Meal saved to Health Connect!",
                        isFromUser = false
                    ))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving to Health Connect", e)
                addMessage(ChatMessage(
                    text = "Failed to save to Health Connect. Make sure you've granted permissions.",
                    isFromUser = false
                ))
            } finally {
                _showHealthConnectDialog.value = false
            }
        }
    }

    fun dismissHealthConnectDialog() {
        _showHealthConnectDialog.value = false
    }

    fun showResetDialog() {
        _showResetDialog.value = true
    }

    fun dismissResetDialog() {
        _showResetDialog.value = false
    }

    fun addImageToConversation(imagePath: String) {
        currentImagePath = imagePath
        _hasImage.value = true
        viewModelScope.launch {
            loadImageBitmap(imagePath)
            if (isMultimodal) {
                startImageAnalysis()
            }
        }
    }

    // Track if an image has been added to the conversation
    private val _hasImage = MutableStateFlow(false)
    val hasImage: StateFlow<Boolean> = _hasImage.asStateFlow()
    
    fun addImageFromGallery(uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Save the image to a temporary file
                    val context = appContainer.applicationContext
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val tempFile = File(context.cacheDir, "chat_gallery_${System.currentTimeMillis()}.jpg")
                    
                    inputStream?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    addImageToConversation(tempFile.absolutePath)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing gallery image", e)
                addMessage(ChatMessage(
                    text = "Sorry, I couldn't process that image. Please try again.",
                    isFromUser = false
                ))
            }
        }
    }
    
    fun stopGeneration() {
        // Set cancellation flag first
        isGenerationCancelled = true
        
        // Cancel any ongoing generation without clearing conversation
        currentGenerationJob?.cancel()
        currentGenerationJob = null
        _isLoading.value = false
        
        // Update model status
        appContainer.updateModelStatus(ModelStatus.READY)
        
        // Clean up any streaming messages
        viewModelScope.launch {
            val currentMessages = _messages.value
            val lastMessage = currentMessages.lastOrNull()
            if (lastMessage != null && lastMessage.isStreaming) {
                // Mark the last streaming message as complete with current content
                val updatedMessages = currentMessages.dropLast(1) + lastMessage.copy(isStreaming = false)
                _messages.value = updatedMessages
                Log.d(TAG, "Marked streaming message as complete due to cancellation")
            }
        }
        
        Log.d(TAG, "Generation stopped by user")
    }
    
    fun clearChatAndGoHome() {
        // Cancel any ongoing generation
        isGenerationCancelled = true
        currentGenerationJob?.cancel()
        currentGenerationJob = null
        
        // Clear all conversation state
        _messages.value = emptyList()
        _isLoading.value = false
        _currentMealNutrition.value = emptyList()
        _showHealthConnectDialog.value = false
        _showResetDialog.value = false
        _hasImage.value = false
        
        // Clear image state
        currentImagePath = null
        currentImageBitmap = null
        awaitingUserResponse = false
        conversationContext.clear()
        
        // Reset cancellation flag
        isGenerationCancelled = false
    }

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }
    
    private fun updateStreamingMessage(messageId: String, newText: String, isStillStreaming: Boolean) {
        _messages.value = _messages.value.map { message ->
            if (message.id == messageId) {
                message.copy(text = newText, isStreaming = isStillStreaming)
            } else {
                message
            }
        }
    }
}

// Factory for creating EnhancedChatViewModel
class EnhancedChatViewModelFactory(
    private val appContainer: AppContainer,
    private val isMultimodal: Boolean = false
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EnhancedChatViewModel::class.java)) {
            return EnhancedChatViewModel(appContainer, isMultimodal) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}