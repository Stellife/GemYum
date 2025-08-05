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

    // Track current image and conversation state
    private var currentImagePath: String? = initialImagePath
    private var currentImageBitmap: Bitmap? = null
    private var awaitingUserResponse = false
    private var conversationContext = mutableListOf<String>()

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

        viewModelScope.launch {
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
                    // Start new analysis
                    startNewAnalysis(text)
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

    private suspend fun analyzeImageWithVision(): String {
        return withContext(Dispatchers.IO) {
            val session = appContainer.getConversationalSession()
            
            // Add image to session if available
            currentImageBitmap?.let { bitmap ->
                Log.d(TAG, "Adding image to vision session: ${bitmap.width}x${bitmap.height}")
                val mpImage = BitmapImageBuilder(bitmap).build()
                session.addImage(mpImage)
            }

            val prompt = """You are a nutrition expert analyzing a meal photo. Use structured reasoning to identify foods and ingredients.

STEP 1 - INITIAL FOOD IDENTIFICATION:
Look at the meal photo and identify the main dish(es) you can see.

STEP 2 - INGREDIENT REASONING:
For each food item, reason through:
- What ingredients are clearly visible
- What ingredients are likely present but not clearly visible
- Typical ingredients for this type of dish
- Portion size estimates

STEP 3 - STRUCTURED RESPONSE FORMAT:
Present your analysis as:

**My analysis of your meal:**
[State your best guess of what the main dish/food is]

**Ingredients I can identify:**
- [Ingredient 1]: [portion estimate]
- [Ingredient 2]: [portion estimate]
- [etc.]

**Ingredients that might be present but I can't see clearly:**
- [Potential ingredient 1]: [estimated portion]
- [Potential ingredient 2]: [estimated portion]
- [etc.]

**Questions for you:**
- Did I miss any ingredients you can see?
- Are any of my ingredient guesses wrong?
- Can you help me with more accurate portion sizes?

EXAMPLE RESPONSE:
**My analysis of your meal:**
This looks like a serving of Pad Thai with rice noodles.

**Ingredients I can identify:**
- Rice noodles: 1.5 cups cooked
- Lime wedges: 2 pieces
- Bean sprouts: 1/4 cup

**Ingredients that might be present but I can't see clearly:**
- Shrimp or chicken: 3-4 oz
- Peanuts: 1-2 tablespoons crushed
- Fish sauce/seasonings: 1-2 teaspoons
- Vegetable oil: 1 tablespoon

**Questions for you:**
- Did I miss any ingredients you can see?
- Are any of my ingredient guesses wrong?
- Can you help me with more accurate portion sizes?

Now analyze this meal photo using this structured approach:"""
            
            session.addQueryChunk(prompt)
            
            // Use streaming response for natural conversation
            generateStreamingResponse(session)
        }
    }

    private fun buildImageAnalysisPrompt(userInput: String): String {
        return """You are continuing a nutrition analysis conversation. The user provided feedback: "$userInput"

ANALYZE their feedback and respond appropriately:

IF they are providing corrections or additions to ingredients:
- Acknowledge their corrections
- Update your ingredient list based on their input
- Ask if there are any other changes needed
- Once they seem satisfied, offer to proceed to nutritional analysis

IF they want to proceed to nutritional analysis:
- Present the final confirmed ingredient list in this format:
**Final Ingredient List:**
- [Ingredient]: [portion]
- [Ingredient]: [portion]
- [etc.]

Then ask: "Should I proceed to calculate the nutritional information for these items?"

IF they confirm nutritional analysis:
- Convert the ingredients to a structured nutritional breakdown
- Show calories, protein, carbs, fat for each item
- Provide totals
- Ask if they want to make adjustments or save to Health Connect

MAINTAIN the structured, reasoning-based approach throughout the conversation.

User's feedback: "$userInput"

Respond appropriately based on where we are in the analysis process:"""
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
            
            continuation.invokeOnCancellation {
                Log.d(TAG, "Streaming response cancelled")
            }
            
            try {
                session.generateResponseAsync { partialResult, done ->
                    Log.d(TAG, "Streaming token: '$partialResult', done: $done")
                    
                    responseBuilder.append(partialResult)
                    
                    // Update UI with streaming text
                    viewModelScope.launch {
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
                            updateStreamingMessage(currentMessageId!!, responseBuilder.toString(), false)
                            continuation.resume(responseBuilder.toString()) {}
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in streaming response", e)
                if (continuation.isActive) {
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
        
        // Check if the response contains a final ingredient list that we should process
        if (response.contains("**Final Ingredient List:**") || 
            response.contains("**Nutritional Information:**") ||
            response.contains("should I proceed to calculate")) {
            
            Log.d(TAG, "Response may contain nutritional analysis - checking for structured data")
            
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
        val endIndex = response.lastIndexOf('}')
        
        return if (startIndex >= 0 && endIndex > startIndex) {
            response.substring(startIndex, endIndex + 1)
        } else {
            // Construct JSON from plain text response
            """{"foods": [], "needsClarification": true, "question": "Could you please describe your meal in more detail?"}"""
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
        val proceedKeywords = listOf("yes", "proceed", "calculate", "nutrition", "analyze", "continue", "go ahead", "sure")
        val userWantsToProceed = proceedKeywords.any { keyword -> 
            userResponse.lowercase().contains(keyword) 
        }
        
        val prompt = if (userWantsToProceed && userResponse.length < 50) {
            // User seems to want nutritional analysis
            """The user confirmed they want to proceed with nutritional analysis: "$userResponse"

Now provide a structured nutritional breakdown following this format:

**Final Confirmed Ingredient List:**
- [List each ingredient with final confirmed portions]

**Nutritional Analysis:**
For each ingredient, provide:
- [Ingredient]: [portion] → [calories] cal, [protein]g protein, [carbs]g carbs, [fat]g fat

**Total Meal Summary:**
- Total Calories: [sum] cal
- Total Protein: [sum]g
- Total Carbs: [sum]g  
- Total Fat: [sum]g

**Would you like to save this meal data to Health Connect for tracking?**

Provide the complete nutritional breakdown:"""
        } else {
            // Continue refining the ingredient list
            """You are continuing a nutrition analysis conversation. The user provided feedback: "$userResponse"

RESPOND based on their feedback:

IF they are correcting or adding ingredients:
- Acknowledge their corrections
- Update the ingredient list
- Ask if there are other changes needed
- Once satisfied, ask: "Should I proceed to calculate the nutritional information?"

IF they seem satisfied with the ingredient list:
- Present the **Final Ingredient List** with confirmed portions
- Ask: "Should I proceed to calculate the nutritional information for these items?"

MAINTAIN the structured format with **bold headers** and bullet points.

User feedback: "$userResponse"

Continue the structured analysis:"""
        }
        
        val response = callAI(prompt, includeImage = isMultimodal)
        processAnalysisResult(response)
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
    
    fun clearChatAndGoHome() {
        // Clear all conversation state
        _messages.value = emptyList()
        _isLoading.value = false
        _currentMealNutrition.value = emptyList()
        _showHealthConnectDialog.value = false
        _hasImage.value = false
        
        // Clear image state
        currentImagePath = null
        currentImageBitmap = null
        awaitingUserResponse = false
        conversationContext.clear()
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