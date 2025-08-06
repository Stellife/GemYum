package com.stel.gemmunch.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.stel.gemmunch.agent.AnalyzedFoodItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "RestaurantNutritionService"

/**
 * Service for handling restaurant-specific nutrition lookups.
 * Provides enhanced search capabilities for restaurant menu items.
 */
class RestaurantNutritionService(private val context: Context) {
    
    private var database: SQLiteDatabase? = null
    
    private fun getDatabase(): SQLiteDatabase {
        if (database == null) {
            val dbFile = context.assets.openFd("nutrients.db")
            database = SQLiteDatabase.openDatabase(dbFile.fileDescriptor.toString(), null, SQLiteDatabase.OPEN_READONLY)
        }
        return database!!
    }
    
    /**
     * Searches for restaurant-specific nutrition information.
     * 
     * @param foodName The name of the food item
     * @param quantity The serving quantity
     * @param unit The serving unit
     * @param restaurantHint Optional restaurant context (e.g., "chipotle", "mcdonald's")
     * @return AnalyzedFoodItem with nutrition data, or null if not found
     */
    suspend fun searchRestaurantNutrition(
        foodName: String,
        quantity: Double,
        unit: String,
        restaurantHint: String? = null
    ): AnalyzedFoodItem? = withContext(Dispatchers.IO) {
        
        Log.d(TAG, "Searching restaurant nutrition: $foodName (${quantity} ${unit}) at ${restaurantHint ?: "any restaurant"}")
        
        // Step 1: Try restaurant-specific search if hint provided
        if (!restaurantHint.isNullOrBlank()) {
            val restaurantResult = searchByRestaurant(foodName, quantity, unit, restaurantHint)
            if (restaurantResult != null) {
                Log.d(TAG, "Found restaurant match: ${restaurantResult.foodName}")
                return@withContext restaurantResult
            }
        }
        
        // Step 2: Try fuzzy search across all restaurant items
        val fuzzyResult = searchRestaurantFuzzy(foodName, quantity, unit)
        if (fuzzyResult != null) {
            Log.d(TAG, "Found fuzzy restaurant match: ${fuzzyResult.foodName}")
            return@withContext fuzzyResult
        }
        
        // Step 3: No restaurant match found
        Log.d(TAG, "No restaurant nutrition data found for: $foodName")
        return@withContext null
    }
    
    /**
     * Searches for nutrition data within a specific restaurant.
     */
    private suspend fun searchByRestaurant(
        foodName: String,
        quantity: Double,
        unit: String,
        restaurantName: String
    ): AnalyzedFoodItem? = withContext(Dispatchers.IO) {
        
        val normalizedRestaurant = restaurantName.lowercase().trim()
        val normalizedFood = foodName.lowercase().trim()
        
        // Query for exact restaurant match
        val query = """
            SELECT name, restaurant_name, serving_size, serving_size_grams,
                   calories, total_fat_g, saturated_fat_g, cholesterol_mg,
                   sodium_mg, total_carbohydrate_g, dietary_fiber_g, 
                   sugars_g, protein_g
            FROM foods 
            WHERE LOWER(restaurant_name) = ? 
            AND (LOWER(name) LIKE ? OR LOWER(search_terms) LIKE ?)
            ORDER BY 
                CASE 
                    WHEN LOWER(name) = ? THEN 1
                    WHEN LOWER(name) LIKE ? THEN 2
                    ELSE 3
                END
            LIMIT 5
        """.trimIndent()
        
        val searchPattern = "%$normalizedFood%"
        
        getDatabase().rawQuery(query, arrayOf(
            normalizedRestaurant, searchPattern, searchPattern, 
            normalizedFood, "$normalizedFood%"
        )).use { cursor ->
            
            if (cursor.moveToFirst()) {
                val result = extractFoodItemFromCursor(cursor, quantity, unit)
                Log.d(TAG, "Found ${cursor.getString(1)} item: ${cursor.getString(0)}")
                return@withContext result
            }
        }
        
        return@withContext null
    }
    
    /**
     * Performs fuzzy search across all restaurant items.
     */
    private suspend fun searchRestaurantFuzzy(
        foodName: String,
        quantity: Double,
        unit: String
    ): AnalyzedFoodItem? = withContext(Dispatchers.IO) {
        
        val normalizedFood = foodName.lowercase().trim()
        
        // Query using FTS (Full Text Search) for better matching
        val query = """
            SELECT f.name, f.restaurant_name, f.serving_size, f.serving_size_grams,
                   f.calories, f.total_fat_g, f.saturated_fat_g, f.cholesterol_mg,
                   f.sodium_mg, f.total_carbohydrate_g, f.dietary_fiber_g, 
                   f.sugars_g, f.protein_g
            FROM foods f
            JOIN foods_fts fts ON f.id = fts.rowid
            WHERE f.restaurant_name IS NOT NULL 
            AND foods_fts MATCH ?
            ORDER BY rank
            LIMIT 3
        """.trimIndent()
        
        getDatabase().rawQuery(query, arrayOf(normalizedFood)).use { cursor ->
            
            if (cursor.moveToFirst()) {
                val result = extractFoodItemFromCursor(cursor, quantity, unit)
                Log.d(TAG, "Found fuzzy restaurant match: ${cursor.getString(1)} - ${cursor.getString(0)}")
                return@withContext result
            }
        }
        
        return@withContext null
    }
    
    /**
     * Extracts nutrition data from database cursor and scales it for the requested serving.
     */
    private fun extractFoodItemFromCursor(
        cursor: android.database.Cursor,
        requestedQuantity: Double,
        requestedUnit: String
    ): AnalyzedFoodItem {
        
        val name = cursor.getString(0)  // name
        val restaurantName = cursor.getString(1)  // restaurant_name
        val servingSize = cursor.getString(2)  // serving_size
        val servingSizeGrams = cursor.getDouble(3)  // serving_size_grams
        val calories = cursor.getDouble(4)  // calories
        val totalFat = cursor.getDoubleOrNull(5)  // total_fat_g
        val saturatedFat = cursor.getDoubleOrNull(6)  // saturated_fat_g
        val cholesterol = cursor.getDoubleOrNull(7)  // cholesterol_mg
        val sodium = cursor.getDoubleOrNull(8)  // sodium_mg
        val totalCarbs = cursor.getDoubleOrNull(9)  // total_carbohydrate_g
        val fiber = cursor.getDoubleOrNull(10)  // dietary_fiber_g
        val sugars = cursor.getDoubleOrNull(11)  // sugars_g
        val protein = cursor.getDoubleOrNull(12)  // protein_g
        
        // Calculate scaling factor for the requested serving
        val scaleFactor = calculateServingScaleFactor(
            requestedQuantity, requestedUnit,
            servingSize, servingSizeGrams
        )
        
        return AnalyzedFoodItem(
            foodName = "$name ($restaurantName)",
            quantity = requestedQuantity,
            unit = requestedUnit,
            calories = (calories * scaleFactor).toInt(),
            protein = protein?.times(scaleFactor),
            totalFat = totalFat?.times(scaleFactor),
            saturatedFat = saturatedFat?.times(scaleFactor),
            cholesterol = cholesterol?.times(scaleFactor),
            sodium = sodium?.times(scaleFactor),
            totalCarbs = totalCarbs?.times(scaleFactor),
            dietaryFiber = fiber?.times(scaleFactor),
            sugars = sugars?.times(scaleFactor)
        )
    }
    
    /**
     * Calculates the scaling factor for different serving sizes.
     */
    private fun calculateServingScaleFactor(
        requestedQuantity: Double,
        requestedUnit: String,
        dbServingSize: String,
        dbServingSizeGrams: Double
    ): Double {
        
        // If exact unit match, scale directly
        if (requestedUnit.lowercase() in dbServingSize.lowercase()) {
            return requestedQuantity
        }
        
        // Convert to grams for standardized scaling
        val requestedGrams = convertToGrams(requestedQuantity, requestedUnit)
        
        return if (dbServingSizeGrams > 0) {
            requestedGrams / dbServingSizeGrams
        } else {
            requestedQuantity // Fallback to direct scaling
        }
    }
    
    /**
     * Converts various units to grams for consistent scaling.
     */
    private fun convertToGrams(quantity: Double, unit: String): Double {
        val lowerUnit = unit.lowercase().trim()
        
        return when (lowerUnit) {
            "g", "gram", "grams" -> quantity
            "oz", "ounce", "ounces" -> quantity * 28.35
            "lb", "pound", "pounds" -> quantity * 453.59
            "cup", "cups" -> quantity * 240.0  // Approximate for most foods
            "tbsp", "tablespoon", "tablespoons" -> quantity * 15.0
            "tsp", "teaspoon", "teaspoons" -> quantity * 5.0
            "ml", "milliliter", "milliliters" -> quantity  // Assume density ~1
            "fl oz", "fluid ounce", "fluid ounces" -> quantity * 29.57
            "ea", "each", "item", "piece" -> quantity * 50.0  // Generic item weight
            else -> quantity * 100.0  // Default serving assumption
        }
    }
    
    /**
     * Gets restaurant context from detected food items.
     */
    fun detectRestaurantContext(detectedItems: List<String>): String? {
        val allText = detectedItems.joinToString(" ").lowercase()
        
        return when {
            // Chipotle indicators
            allText.contains("chipotle") || 
            allText.contains("barbacoa") || 
            allText.contains("sofritas") || 
            allText.contains("carnitas") ||
            allText.contains("cilantro-lime") -> "chipotle"
            
            // McDonald's indicators  
            allText.contains("mcdonald") ||
            allText.contains("big mac") ||
            allText.contains("mcchicken") ||
            allText.contains("quarter pounder") -> "mcdonald's"
            
            // Add more restaurant patterns as needed
            else -> null
        }
    }
    
    /**
     * Helper extension for nullable double values from cursor.
     */
    private fun android.database.Cursor.getDoubleOrNull(columnIndex: Int): Double? {
        return if (isNull(columnIndex)) null else getDouble(columnIndex)
    }
    
    fun close() {
        database?.close()
        database = null
    }
}