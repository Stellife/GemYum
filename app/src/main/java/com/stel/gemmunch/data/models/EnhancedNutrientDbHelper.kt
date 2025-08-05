package com.stel.gemmunch.data.models

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.stel.gemmunch.data.api.UsdaApiService
import com.stel.gemmunch.data.NutrientDatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val TAG = "NutrientDbHelper"
private const val DB_NAME = "nutrients.db"

/** Complete nutritional info. The primary output of this helper. */
data class NutrientInfo(
    val calories: Int,
    val protein: Double? = null,
    val totalFat: Double? = null,
    val saturatedFat: Double? = null,
    val cholesterol: Double? = null,
    val sodium: Double? = null,
    val totalCarbs: Double? = null,
    val dietaryFiber: Double? = null,
    val sugars: Double? = null,
    val glycemicIndex: Int? = null,
    val glycemicLoad: Double? = null
)

/**
 * Manages a hybrid nutrition database that uses a local, pre-packaged SQLite database
 * for fast lookups and falls back to the USDA API for unknown foods, caching the results.
 */
class EnhancedNutrientDbHelper(
    private val context: Context,
    private val usdaApiService: UsdaApiService
) {
    private var db: SQLiteDatabase

    // A map to convert common units from the AI model into a gram equivalent.
    private val unitToGramMap = mapOf(
        "item" to 50.0, "items" to 50.0,
        "piece" to 50.0, "pieces" to 50.0,
        "slice" to 25.0, "slices" to 25.0,
        "serving" to 120.0, "servings" to 120.0,
        "ounce" to 28.0, "ounces" to 28.0, "oz" to 28.0,
        "cup" to 125.0, "cups" to 125.0,
        "tablespoon" to 15.0, "tablespoons" to 15.0, "tbsp" to 15.0,
        "teaspoon" to 5.0, "teaspoons" to 5.0, "tsp" to 5.0,
        "gram" to 1.0, "grams" to 1.0, "g" to 1.0,
        // Specific food items for better accuracy
        "fried egg" to 46.0, "boiled egg" to 50.0, "scrambled egg" to 61.0,
        "banana" to 118.0, "bacon" to 8.0, "taco" to 75.0, "tacos" to 75.0,
        "cheese" to 28.0 // Default to 1 oz for generic cheese
    )

    init {
        // Database initialization is now handled asynchronously
        // This is a temporary placeholder until the database is ready
        db = SQLiteDatabase.create(null)
    }
    
    /**
     * Initialize the database asynchronously.
     * This should be called from AppContainer during initialization.
     */
    suspend fun initialize(databaseManager: NutrientDatabaseManager) {
        val dbFile = databaseManager.ensureDatabase(useLiteVersion = false)
        db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        Log.i(TAG, "Enhanced nutrient database opened from: ${dbFile.path}")
        
        // Log database stats
        val stats = databaseManager.getDatabaseStats()
        Log.i(TAG, "Database stats: $stats")
    }

    /**
     * The main lookup function with a fallback chain: Local DB -> USDA API -> Cache -> Default.
     */
    suspend fun lookup(
        food: String, 
        qty: Double, 
        unit: String, 
        onUsdaFallback: ((String) -> Unit)? = null
    ): NutrientInfo = withContext(Dispatchers.IO) {
        val lowerCaseFood = food.lowercase()
        val grams = convertToGrams(qty, unit.lowercase(), lowerCaseFood)
        if (grams <= 0) return@withContext NutrientInfo(0)

        // Step 1: Try local database first (fastest).
        val localResult = lookupLocal(lowerCaseFood)
        if (localResult != null) {
            val (localNutrients, servingSizeGrams) = localResult
            
            // If database has serving size, nutrients are for that serving
            // Otherwise, assume nutrients are per 100g
            val databaseServingSize = servingSizeGrams ?: 100.0
            val scaleFactor = grams / databaseServingSize
            
            val scaledNutrients = NutrientInfo(
                calories = (localNutrients.calories * scaleFactor).roundToInt(),
                protein = localNutrients.protein?.times(scaleFactor),
                totalFat = localNutrients.totalFat?.times(scaleFactor),
                saturatedFat = localNutrients.saturatedFat?.times(scaleFactor),
                cholesterol = localNutrients.cholesterol?.times(scaleFactor),
                sodium = localNutrients.sodium?.times(scaleFactor),
                totalCarbs = localNutrients.totalCarbs?.times(scaleFactor),
                dietaryFiber = localNutrients.dietaryFiber?.times(scaleFactor),
                sugars = localNutrients.sugars?.times(scaleFactor),
                glycemicIndex = localNutrients.glycemicIndex,
                glycemicLoad = localNutrients.glycemicLoad?.times(scaleFactor)
            )
            Log.i(TAG, "LOCAL HIT: '$food' -> ${scaledNutrients.calories} Calories (base: ${localNutrients.calories} Calories/${databaseServingSize}g, requested: ${grams}g)")
            return@withContext scaledNutrients
        }

        // Step 2: Fallback to USDA API if configured.
        if (usdaApiService.isConfigured()) {
            Log.d(TAG, "LOCAL MISS: Trying USDA API for '$food'")
            // Notify callback about USDA API fallback
            onUsdaFallback?.invoke(food)
            val usdaCalories = usdaApiService.searchAndGetBestCalories(lowerCaseFood)
            if (usdaCalories != null) {
                // Step 3: Cache the new result for future lookups.
                cacheNutrientInfo(lowerCaseFood, usdaCalories)
                val finalCalories = (usdaCalories * grams / 100.0).roundToInt()
                Log.i(TAG, "USDA HIT: '$food' -> $finalCalories kcal (cached for future)")
                return@withContext NutrientInfo(finalCalories)
            }
        }

        // Step 4: No results found anywhere.
        Log.w(TAG, "NO RESULTS: '$food' not found in any source.")
        NutrientInfo(0)
    }

    private fun convertToGrams(qty: Double, lowerCaseUnit: String, lowerCaseFood: String): Double {
        // Prioritize a food-specific conversion first, then a generic unit.
        val conversionFactor = unitToGramMap[lowerCaseFood] ?: unitToGramMap[lowerCaseUnit]
        return if (conversionFactor != null) {
            qty * conversionFactor
        } else {
            Log.w(TAG, "Unknown unit '$lowerCaseUnit' for food '$lowerCaseFood'. Defaulting to 0g.")
            0.0
        }
    }

    private fun lookupLocal(food: String): Pair<NutrientInfo, Double?>? {
        val columns = "calories, protein_g, total_fat_g, saturated_fat_g, cholesterol_mg, sodium_mg, " +
                     "total_carbohydrate_g, dietary_fiber_g, sugars_g, glycemic_index, glycemic_load, serving_size_grams"
        
        try {
            // Query 1: Try for an exact match first.
            var cursor = db.rawQuery(
                "SELECT $columns FROM foods WHERE LOWER(name) = LOWER(?) LIMIT 1",
                arrayOf(food)
            )
            if (cursor.moveToFirst()) {
                val result = extractNutrientInfo(cursor)
                cursor.close()
                return result
            }
            cursor.close()

            // Query 2: Prioritize non-restaurant items for generic food searches
            cursor = db.rawQuery(
                "SELECT $columns FROM foods WHERE restaurant_name IS NULL AND LOWER(name) LIKE LOWER(?) ORDER BY length(name) ASC LIMIT 1",
                arrayOf("%$food%")
            )
            if (cursor.moveToFirst()) {
                val result = extractNutrientInfo(cursor)
                cursor.close()
                return result
            }
            cursor.close()

            // Query 3: Try searching in restaurant items
            cursor = db.rawQuery(
                "SELECT $columns FROM foods WHERE restaurant_name IS NOT NULL AND LOWER(name) LIKE LOWER(?) ORDER BY popularity_score DESC, length(name) ASC LIMIT 1",
                arrayOf("%$food%")
            )
            if (cursor.moveToFirst()) {
                val result = extractNutrientInfo(cursor)
                cursor.close()
                return result
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error in local lookup for '$food'", e)
        }

        return null // No match found
    }
    
    private fun extractNutrientInfo(cursor: android.database.Cursor): Pair<NutrientInfo, Double?> {
        val nutrientInfo = NutrientInfo(
            calories = cursor.getDouble(0).toInt(),
            protein = cursor.getDoubleOrNull(1),
            totalFat = cursor.getDoubleOrNull(2),
            saturatedFat = cursor.getDoubleOrNull(3),
            cholesterol = cursor.getDoubleOrNull(4),
            sodium = cursor.getDoubleOrNull(5),
            totalCarbs = cursor.getDoubleOrNull(6),
            dietaryFiber = cursor.getDoubleOrNull(7),
            sugars = cursor.getDoubleOrNull(8),
            glycemicIndex = cursor.getIntOrNull(9),
            glycemicLoad = cursor.getDoubleOrNull(10)
        )
        val servingSizeGrams = cursor.getDoubleOrNull(11)
        return Pair(nutrientInfo, servingSizeGrams)
    }
    
    private fun android.database.Cursor.getDoubleOrNull(index: Int): Double? {
        return if (isNull(index)) null else getDouble(index)
    }
    
    private fun android.database.Cursor.getIntOrNull(index: Int): Int? {
        return if (isNull(index)) null else getInt(index)
    }

    private fun cacheNutrientInfo(foodName: String, calories: Int) {
        try {
            val values = ContentValues().apply {
                put("name", foodName)
                put("calories", calories.toDouble())
                put("data_source", "USDA API") // Mark the source of the cached data
                put("created_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict("foods", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            Log.d(TAG, "Successfully cached '$foodName' from USDA.")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching nutrition data for '$foodName'", e)
        }
    }

    fun close() {
        db.close()
    }
}