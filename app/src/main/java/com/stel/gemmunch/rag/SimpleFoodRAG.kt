package com.stel.gemmunch.rag

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.stel.gemmunch.data.models.NutrientInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

private const val TAG = "SimpleFoodRAG"

/**
 * Simple RAG implementation for GemMunch that enhances food recognition
 * using contextual retrieval from the existing database.
 * 
 * This demonstrates AI Edge's capability to combine:
 * 1. Local knowledge retrieval
 * 2. Context-aware inference
 * 3. On-device similarity matching
 */
class SimpleFoodRAG(private val context: Context) {
    
    data class FoodContext(
        val name: String,
        val calories: Int,
        val glycemicIndex: Int?,
        val visualHints: String,
        val typicalPortions: String,
        val similarity: Float
    )
    
    data class RAGContext(
        val similarFoods: List<FoodContext>,
        val categoryInsights: String,
        val portionGuidance: String,
        val nutritionRange: String
    )
    
    /**
     * Retrieves similar foods from the database based on text similarity
     * This simulates embedding-based retrieval using simple text matching
     */
    suspend fun retrieveSimilarFoods(
        identifiedFood: String,
        limit: Int = 5
    ): List<FoodContext> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FoodContext>()
        
        // Open the nutrients database
        val dbPath = context.getDatabasePath("nutrients.db")
        if (!dbPath.exists()) {
            Log.w(TAG, "Database not found")
            return@withContext emptyList()
        }
        
        val db = SQLiteDatabase.openDatabase(dbPath.path, null, SQLiteDatabase.OPEN_READONLY)
        
        try {
            // First, try exact match
            var cursor = db.rawQuery(
                """
                SELECT name, calories, glycemic_index, serving_size_grams
                FROM foods 
                WHERE LOWER(name) = LOWER(?)
                LIMIT 1
                """,
                arrayOf(identifiedFood)
            )
            
            if (cursor.moveToFirst()) {
                results.add(
                    FoodContext(
                        name = cursor.getString(0),
                        calories = cursor.getInt(1),
                        glycemicIndex = if (cursor.isNull(2)) null else cursor.getInt(2),
                        visualHints = generateVisualHints(cursor.getString(0)),
                        typicalPortions = generatePortionInfo(cursor.getDoubleOrNull(3)),
                        similarity = 1.0f
                    )
                )
            }
            cursor.close()
            
            // Then find similar foods using LIKE
            val searchTerms = identifiedFood.split(" ")
            val likePattern = searchTerms.joinToString("%") { it }
            
            cursor = db.rawQuery(
                """
                SELECT name, calories, glycemic_index, serving_size_grams,
                       LENGTH(name) as name_length
                FROM foods 
                WHERE LOWER(name) LIKE LOWER(?)
                  AND LOWER(name) != LOWER(?)
                ORDER BY name_length ASC
                LIMIT ?
                """,
                arrayOf("%$likePattern%", identifiedFood, limit.toString())
            )
            
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)
                results.add(
                    FoodContext(
                        name = name,
                        calories = cursor.getInt(1),
                        glycemicIndex = if (cursor.isNull(2)) null else cursor.getInt(2),
                        visualHints = generateVisualHints(name),
                        typicalPortions = generatePortionInfo(cursor.getDoubleOrNull(3)),
                        similarity = calculateSimpleSimilarity(identifiedFood, name)
                    )
                )
            }
            cursor.close()
            
            // If still not enough results, try category matching
            if (results.size < limit) {
                val category = detectFoodCategory(identifiedFood)
                cursor = db.rawQuery(
                    """
                    SELECT name, calories, glycemic_index, serving_size_grams
                    FROM foods 
                    WHERE LOWER(name) LIKE LOWER(?)
                      AND name NOT IN (${results.joinToString(",") { "?" }})
                    ORDER BY calories DESC
                    LIMIT ?
                    """,
                    arrayOf("%$category%") + results.map { it.name } + arrayOf((limit - results.size).toString())
                )
                
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0)
                    results.add(
                        FoodContext(
                            name = name,
                            calories = cursor.getInt(1),
                            glycemicIndex = if (cursor.isNull(2)) null else cursor.getInt(2),
                            visualHints = generateVisualHints(name),
                            typicalPortions = generatePortionInfo(cursor.getDoubleOrNull(3)),
                            similarity = 0.5f // Lower similarity for category matches
                        )
                    )
                }
                cursor.close()
            }
            
        } finally {
            db.close()
        }
        
        Log.i(TAG, "Retrieved ${results.size} similar foods for '$identifiedFood'")
        results.sortedByDescending { it.similarity }.take(limit)
    }
    
    /**
     * Builds comprehensive context from retrieved foods
     */
    fun buildRAGContext(similarFoods: List<FoodContext>): RAGContext {
        val avgCalories = similarFoods.map { it.calories }.average().toInt()
        val minCalories = similarFoods.minOf { it.calories }
        val maxCalories = similarFoods.maxOf { it.calories }
        
        val avgGI = similarFoods.mapNotNull { it.glycemicIndex }.takeIf { it.isNotEmpty() }?.average()?.toInt()
        
        val categoryInsights = when {
            similarFoods.any { it.name.contains("pizza", true) } -> 
                "Pizza-like dish: typically 250-400 calories per slice, high in carbs and fats"
            similarFoods.any { it.name.contains("burger", true) } ->
                "Burger-style item: usually 400-800 calories, high protein and fat content"
            similarFoods.any { it.name.contains("salad", true) } ->
                "Salad dish: varies widely 150-600 calories depending on dressing and toppings"
            similarFoods.any { it.name.contains("taco", true) } ->
                "Taco/Mexican food: typically 150-300 calories per item, moderate carbs and protein"
            else -> "Mixed food category with varied nutritional profiles"
        }
        
        val portionGuidance = buildPortionGuidance(similarFoods)
        
        val nutritionRange = buildString {
            append("Based on similar foods:\n")
            append("• Calories: $minCalories-$maxCalories (avg: $avgCalories)\n")
            avgGI?.let { append("• Glycemic Index: ~$it\n") }
            append("• Confidence: ${(similarFoods.firstOrNull()?.similarity ?: 0f) * 100}%")
        }
        
        return RAGContext(
            similarFoods = similarFoods,
            categoryInsights = categoryInsights,
            portionGuidance = portionGuidance,
            nutritionRange = nutritionRange
        )
    }
    
    /**
     * Generates an enhanced prompt for Gemma using RAG context
     */
    fun generateRAGEnhancedPrompt(
        basePrompt: String,
        ragContext: RAGContext,
        userContext: String? = null
    ): String {
        return """
        $basePrompt
        
        === CONTEXT FROM KNOWLEDGE BASE ===
        Similar foods in our database:
        ${ragContext.similarFoods.take(3).joinToString("\n") { 
            "• ${it.name}: ${it.calories} cal${it.glycemicIndex?.let { gi -> ", GI: $gi" } ?: ""}"
        }}
        
        Category Insights: ${ragContext.categoryInsights}
        
        Portion Guidance: ${ragContext.portionGuidance}
        
        ${ragContext.nutritionRange}
        
        ${userContext?.let { "User Context: $it" } ?: ""}
        
        Based on this context, provide a more accurate analysis of the food in the image.
        Consider the typical portions and calorie ranges from similar foods.
        """.trimIndent()
    }
    
    // Helper functions
    private fun generateVisualHints(foodName: String): String {
        return when {
            foodName.contains("pizza", true) -> "triangular slice, melted cheese visible, flat bread base"
            foodName.contains("burger", true) -> "circular bun, layered ingredients, handheld size"
            foodName.contains("taco", true) -> "folded tortilla, visible fillings, V-shaped profile"
            foodName.contains("salad", true) -> "mixed greens, bowl presentation, various colors"
            else -> "standard food presentation"
        }
    }
    
    private fun generatePortionInfo(servingGrams: Double?): String {
        return servingGrams?.let {
            when {
                it < 100 -> "small portion (~${it.toInt()}g)"
                it < 200 -> "medium portion (~${it.toInt()}g)"
                else -> "large portion (~${it.toInt()}g)"
            }
        } ?: "standard serving"
    }
    
    private fun calculateSimpleSimilarity(query: String, target: String): Float {
        val queryWords = query.lowercase().split(" ").toSet()
        val targetWords = target.lowercase().split(" ").toSet()
        
        val intersection = queryWords.intersect(targetWords).size
        val union = queryWords.union(targetWords).size
        
        return if (union > 0) intersection.toFloat() / union else 0f
    }
    
    private fun detectFoodCategory(foodName: String): String {
        return when {
            foodName.contains("pizza", true) -> "pizza"
            foodName.contains("burger", true) -> "burger"
            foodName.contains("taco", true) || foodName.contains("burrito", true) -> "mexican"
            foodName.contains("salad", true) -> "salad"
            foodName.contains("sandwich", true) -> "sandwich"
            foodName.contains("pasta", true) -> "pasta"
            foodName.contains("chicken", true) -> "chicken"
            foodName.contains("beef", true) -> "beef"
            else -> "food"
        }
    }
    
    private fun buildPortionGuidance(foods: List<FoodContext>): String {
        val portions = foods.mapNotNull { food ->
            when {
                food.name.contains("slice", true) -> "1 slice"
                food.name.contains("piece", true) -> "1 piece"
                food.name.contains("cup", true) -> "1 cup"
                food.name.contains("bowl", true) -> "1 bowl"
                food.name.contains("plate", true) -> "1 plate"
                else -> null
            }
        }.distinct()
        
        return if (portions.isNotEmpty()) {
            "Common portions: ${portions.joinToString(", ")}"
        } else {
            "Portion sizes vary - consider visual cues"
        }
    }
    
    private fun android.database.Cursor.getDoubleOrNull(index: Int): Double? {
        return if (isNull(index)) null else getDouble(index)
    }
}

/**
 * Data class for demonstrating RAG results in UI
 */
data class RAGAnalysisResult(
    val originalAnalysis: String,
    val ragContext: SimpleFoodRAG.RAGContext,
    val enhancedAnalysis: String,
    val confidenceImprovement: Float
)