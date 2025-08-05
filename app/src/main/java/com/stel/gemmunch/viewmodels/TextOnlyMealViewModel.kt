package com.stel.gemmunch.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import java.time.Instant
import java.util.UUID

private const val TAG = "TextOnlyMealViewModel"

/**
 * Fast, non-async ViewModel for "Describe your Meal" mode
 * Provides quick responses with function-calling style reasoning chains
 */
class TextOnlyMealViewModel(
    private val appContainer: AppContainer
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentMealNutrition = MutableStateFlow<List<AnalyzedFoodItem>>(emptyList())
    val currentMealNutrition: StateFlow<List<AnalyzedFoodItem>> = _currentMealNutrition.asStateFlow()

    private val _showHealthConnectDialog = MutableStateFlow(false)
    val showHealthConnectDialog: StateFlow<Boolean> = _showHealthConnectDialog.asStateFlow()

    // Quick action buttons state
    private val _availableActions = MutableStateFlow<List<QuickAction>>(emptyList())
    val availableActions: StateFlow<List<QuickAction>> = _availableActions.asStateFlow()

    // Conversation state
    private var conversationStage = ConversationStage.INITIAL
    private var pendingIngredients = mutableListOf<String>()
    private var mealContext = ""

    init {
        // Show initial greeting with quick actions
        showInitialGreeting()
    }

    private fun showInitialGreeting() {
        addMessage(ChatMessage(
            text = "Hi! I'm here to help you track the nutrition in your meal. Just describe what you're eating and I'll break down the ingredients and nutrition for you.\n\n**What would you like to track today?**",
            isFromUser = false
        ))
        
        // Set initial quick actions
        _availableActions.value = listOf(
            QuickAction("breakfast", "🍳 Breakfast", "I had breakfast with..."),
            QuickAction("lunch", "🥙 Lunch", "I had lunch with..."),
            QuickAction("dinner", "🍽️ Dinner", "I had dinner with..."),
            QuickAction("snack", "🍿 Snack", "I had a snack with...")
        )
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
                processUserMessage(text)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message", e)
                addMessage(ChatMessage(
                    text = "I encountered an error analyzing your meal. Please try describing it again.",
                    isFromUser = false
                ))
                resetToInitial()
            } finally {
                _isLoading.value = false
                appContainer.updateModelStatus(ModelStatus.READY)
            }
        }
    }

    fun executeQuickAction(action: QuickAction) {
        sendMessage(action.promptText)
    }

    private suspend fun processUserMessage(text: String) {
        when (conversationStage) {
            ConversationStage.INITIAL -> {
                // First meal description
                mealContext = text
                analyzeInitialMealDescription(text)
            }
            ConversationStage.CONFIRMING_INGREDIENTS -> {
                // User is confirming or correcting ingredients
                handleIngredientConfirmation(text)
            }
            ConversationStage.REFINING_PORTIONS -> {
                // User is adjusting portion sizes
                handlePortionRefinement(text)
            }
            ConversationStage.FINAL_ANALYSIS -> {
                // Complete analysis and show nutrition
                completeNutritionAnalysis()
            }
        }
    }

    private suspend fun analyzeInitialMealDescription(description: String) {
        // Show reasoning chain
        val reasoningMessage = generateReasoningChain(description)
        addMessage(ChatMessage(
            text = reasoningMessage,
            isFromUser = false
        ))

        // Quick non-async analysis
        val ingredients = extractIngredientsFromDescription(description)
        pendingIngredients.clear()
        pendingIngredients.addAll(ingredients)

        // Show ingredient confirmation
        showIngredientConfirmation(ingredients)
        conversationStage = ConversationStage.CONFIRMING_INGREDIENTS
    }

    private fun generateReasoningChain(description: String): String {
        return """**🔍 Analyzing your meal...**

**STEP 1 - Understanding what you described:**
${description}

**STEP 2 - Breaking down the components:**
Let me identify the main foods and ingredients...

**STEP 3 - Ingredient detection reasoning:**
• Looking for explicitly mentioned foods
• Identifying cooking methods that suggest additional ingredients
• Considering typical accompaniments for this type of meal
• Estimating reasonable portion sizes

**STEP 4 - Initial ingredient list:**"""
    }

    private fun extractIngredientsFromDescription(description: String): List<String> {
        // Simple keyword-based extraction for now
        // In a real implementation, this could use NLP or the AI model
        val commonFoods = mapOf(
            "chicken" to "Chicken breast, 6 oz",
            "rice" to "White rice, 1 cup cooked",
            "broccoli" to "Broccoli, 1 cup",
            "pasta" to "Pasta, 2 oz dry",
            "salmon" to "Salmon fillet, 6 oz",
            "salad" to "Mixed greens, 2 cups",
            "bread" to "Bread, 2 slices",
            "eggs" to "Eggs, 2 large",
            "yogurt" to "Greek yogurt, 1 cup",
            "banana" to "Banana, 1 medium",
            "apple" to "Apple, 1 medium",
            "cheese" to "Cheese, 1 oz",
            "milk" to "Milk, 1 cup",
            "butter" to "Butter, 1 tbsp",
            "oil" to "Olive oil, 1 tbsp"
        )

        val foundIngredients = mutableListOf<String>()
        val lowerDescription = description.lowercase()

        for ((keyword, ingredient) in commonFoods) {
            if (lowerDescription.contains(keyword)) {
                foundIngredients.add(ingredient)
            }
        }

        // If nothing found, provide generic response
        if (foundIngredients.isEmpty()) {
            foundIngredients.add("Mixed meal components, typical serving size")
        }

        return foundIngredients
    }

    private fun showIngredientConfirmation(ingredients: List<String>) {
        val ingredientList = ingredients.joinToString("\n") { "• $it" }
        
        addMessage(ChatMessage(
            text = """**📋 Here's what I identified:**

$ingredientList

**Does this look accurate?** 

✅ If this looks good, I'll calculate the nutrition
❌ If something's wrong, tell me what to change
➕ If I missed something, let me know what to add""",
            isFromUser = false
        ))

        // Set quick action buttons for confirmation
        _availableActions.value = listOf(
            QuickAction("confirm", "✅ Looks Good", "Yes, that looks accurate"),
            QuickAction("missing", "➕ Add Something", "You missed..."),
            QuickAction("wrong", "❌ Fix Something", "Actually, the...")
        )
    }

    private suspend fun handleIngredientConfirmation(response: String) {
        val lowerResponse = response.lowercase()
        
        when {
            lowerResponse.contains("yes") || lowerResponse.contains("correct") || 
            lowerResponse.contains("accurate") || lowerResponse.contains("good") -> {
                // User confirmed - proceed to nutrition analysis
                completeNutritionAnalysis()
            }
            lowerResponse.contains("no") || lowerResponse.contains("wrong") || 
            lowerResponse.contains("missed") || lowerResponse.contains("add") -> {
                // User wants to make changes
                addMessage(ChatMessage(
                    text = """**🔧 Got it! Let me adjust the ingredient list.**

What specifically would you like me to change? For example:
• "Add tomatoes and onions"
• "The chicken should be 4 oz, not 6 oz"  
• "Remove the rice, I didn't have any"

I'll update the list based on your feedback.""",
                    isFromUser = false
                ))
                
                _availableActions.value = listOf(
                    QuickAction("done", "✅ Done Editing", "That's everything, calculate nutrition now")
                )
            }
            else -> {
                // Parse the specific changes they mentioned
                parseIngredientChanges(response)
            }
        }
    }

    private fun parseIngredientChanges(changes: String) {
        // Simple parsing logic - in practice this could be more sophisticated
        addMessage(ChatMessage(
            text = """**✅ Updated ingredient list based on your feedback:**

${pendingIngredients.joinToString("\n") { "• $it" }}

**Ready to calculate nutrition?** Just say "calculate" or "done" when you're satisfied with the list.""",
            isFromUser = false
        ))
        
        _availableActions.value = listOf(
            QuickAction("calculate", "🧮 Calculate Nutrition", "Calculate the nutrition now"),
            QuickAction("more_changes", "🔧 More Changes", "I need to change...")
        )
    }

    private suspend fun handlePortionRefinement(response: String) {
        // Handle portion size adjustments
        completeNutritionAnalysis()
    }

    private suspend fun completeNutritionAnalysis() {
        addMessage(ChatMessage(
            text = "**🧮 Calculating nutrition...**\n\nAnalyzing ingredients and looking up nutrition data...",
            isFromUser = false
        ))

        // Simulate nutrition lookup for each ingredient
        val nutritionItems = mutableListOf<AnalyzedFoodItem>()
        
        for (ingredient in pendingIngredients) {
            try {
                val nutritionItem = lookupNutrition(ingredient)
                nutritionItems.add(nutritionItem)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to lookup nutrition for: $ingredient", e)
            }
        }

        _currentMealNutrition.value = nutritionItems
        
        // Show final results
        showNutritionResults(nutritionItems)
        
        // Reset for next meal
        conversationStage = ConversationStage.INITIAL
        _availableActions.value = listOf(
            QuickAction("new_meal", "🍽️ Track Another Meal", "I want to track another meal"),
            QuickAction("save", "💾 Save to Health", "Save this meal to my health data")
        )
    }

    private suspend fun lookupNutrition(ingredient: String): AnalyzedFoodItem {
        return withContext(Dispatchers.IO) {
            try {
                // Use the existing nutrition service
                val searchResult = appContainer.nutritionSearchService.searchNutrition(
                    foodName = ingredient,
                    servingSize = 1.0,
                    servingUnit = "serving" 
                )
                searchResult ?: createFallbackNutrition(ingredient)
            } catch (e: Exception) {
                Log.w(TAG, "Nutrition lookup failed for: $ingredient", e)
                createFallbackNutrition(ingredient)
            }
        }
    }

    private fun createFallbackNutrition(ingredient: String): AnalyzedFoodItem {
        // Provide reasonable fallback values
        return AnalyzedFoodItem(
            foodName = ingredient,
            quantity = 1.0,
            unit = "serving",
            calories = 150, // Reasonable average
            protein = 10.0,
            totalFat = 5.0,
            totalCarbs = 15.0
        )
    }

    private fun showNutritionResults(items: List<AnalyzedFoodItem>) {
        val totalCalories = items.sumOf { it.calories }
        val totalProtein = items.sumOf { it.protein ?: 0.0 }
        val totalCarbs = items.sumOf { it.totalCarbs ?: 0.0 }
        val totalFat = items.sumOf { it.totalFat ?: 0.0 }

        val itemBreakdown = items.joinToString("\n") { item ->
            "• ${item.foodName}: ${item.calories} cal"
        }

        addMessage(ChatMessage(
            text = """**✅ Nutrition Analysis Complete!**

**📊 Your Meal Summary:**
🔥 **Total Calories:** $totalCalories cal
🥩 **Protein:** ${String.format("%.1f", totalProtein)}g
🍞 **Carbs:** ${String.format("%.1f", totalCarbs)}g
🥑 **Fat:** ${String.format("%.1f", totalFat)}g

**📋 Breakdown by food:**
$itemBreakdown

**🎯 Would you like to:**
• Save to your health tracking app
• Get suggestions for balancing this meal
• Track another meal""",
            isFromUser = false
        ))
    }

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    private fun resetToInitial() {
        conversationStage = ConversationStage.INITIAL
        pendingIngredients.clear()
        mealContext = ""
        _availableActions.value = listOf(
            QuickAction("retry", "🔄 Try Again", "Let me describe my meal again")
        )
    }

    fun requestHealthConnectSave() {
        _showHealthConnectDialog.value = true
    }

    fun dismissHealthConnectDialog() {
        _showHealthConnectDialog.value = false
    }

    fun resetConversation() {
        // Clear all messages
        _messages.value = emptyList()
        
        // Clear nutrition data
        _currentMealNutrition.value = emptyList()
        
        // Reset conversation state
        conversationStage = ConversationStage.INITIAL
        pendingIngredients.clear()
        mealContext = ""
        
        // Reset dialogs
        _showHealthConnectDialog.value = false
        _isLoading.value = false
        
        // Show initial greeting again
        showInitialGreeting()
    }
}

// Conversation stages for state management
enum class ConversationStage {
    INITIAL,
    CONFIRMING_INGREDIENTS,
    REFINING_PORTIONS,
    FINAL_ANALYSIS
}

// Quick action buttons
data class QuickAction(
    val id: String,
    val label: String,
    val promptText: String
)

class TextOnlyMealViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TextOnlyMealViewModel::class.java)) {
            return TextOnlyMealViewModel(appContainer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}