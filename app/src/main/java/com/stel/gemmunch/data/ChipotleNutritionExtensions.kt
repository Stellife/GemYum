package com.stel.gemmunch.data

import android.util.Log
import com.stel.gemmunch.data.models.EnhancedNutrientDbHelper
import com.stel.gemmunch.data.models.NutrientInfo

private const val TAG = "ChipotleNutritionExt"

/**
 * Extensions for handling Chipotle-specific nutrition lookups.
 * This improves AI recognition accuracy for Chipotle menu items.
 */
object ChipotleNutritionExtensions {
    
    /**
     * Maps common AI-detected food terms to specific Chipotle menu items.
     */
    private val chipotleFoodMappings = mapOf(
        // Tortillas - AI might detect generic terms
        "burrito tortilla" to "Flour Tortilla (burrito)",
        "flour tortilla large" to "Flour Tortilla (burrito)",
        "taco tortilla" to "Flour Tortilla (taco)", 
        "flour tortilla small" to "Flour Tortilla (taco)",
        "corn tortilla" to "Crispy Corn Tortilla",
        "crispy taco shell" to "Crispy Corn Tortilla",
        
        // Rice variations
        "brown rice" to "Cilantro-Lime Brown Rice",
        "cilantro lime brown rice" to "Cilantro-Lime Brown Rice",
        "white rice" to "Cilantro-Lime White Rice", 
        "cilantro lime white rice" to "Cilantro-Lime White Rice",
        "chipotle rice" to "Cilantro-Lime White Rice",
        
        // Beans
        "black beans" to "Black Beans",
        "pinto beans" to "Pinto Beans",
        "refried beans" to "Pinto Beans", // Close approximation
        
        // Proteins
        "barbacoa" to "Barbacoa",
        "shredded beef" to "Barbacoa",
        "chicken" to "Chicken",
        "grilled chicken" to "Chicken",
        "carnitas" to "Carnitas", 
        "pork" to "Carnitas",
        "steak" to "Steak",
        "beef" to "Steak",
        "sofritas" to "Sofritas",
        "tofu" to "Sofritas",
        
        // Salsas
        "salsa" to "Fresh Tomato Salsa",
        "tomato salsa" to "Fresh Tomato Salsa",
        "pico de gallo" to "Fresh Tomato Salsa",
        "corn salsa" to "Roasted Chili-Corn Salsa",
        "green salsa" to "Tomatillo-Green Chili Salsa",
        "hot salsa" to "Tomatillo-Red Chili Salsa",
        "red salsa" to "Tomatillo-Red Chili Salsa",
        
        // Toppings
        "cheese" to "Cheese",
        "shredded cheese" to "Cheese",
        "sour cream" to "Sour Cream",
        "guacamole" to "Guacamole (topping/side)",
        "avocado" to "Guacamole (topping/side)",
        "queso" to "Queso Blanco (entreé)",
        "queso blanco" to "Queso Blanco (entreé)",
        
        // Vegetables
        "fajita vegetables" to "Fajita Vegetables",
        "peppers and onions" to "Fajita Vegetables",
        "lettuce" to "Romaine Lettuce (tacos)",
        "romaine" to "Romaine Lettuce (tacos)",
        "salad mix" to "Supergreens Salad Mix",
        "greens" to "Supergreens Salad Mix",
        
        // Sides
        "chips" to "Chips (regular)",
        "tortilla chips" to "Chips (regular)",
        "vinaigrette" to "Chipotle-Honey Vinaigrette",
        "dressing" to "Chipotle-Honey Vinaigrette"
    )
    
    /**
     * Enhanced lookup function that tries Chipotle-specific mappings first.
     * 
     * @param helper The EnhancedNutrientDbHelper instance
     * @param foodName The detected food name from AI
     * @param quantity The quantity detected
     * @param unit The unit detected
     * @return NutrientInfo if found, null otherwise
     */
    suspend fun lookupChipotleFood(
        helper: EnhancedNutrientDbHelper,
        foodName: String,
        quantity: Double,
        unit: String
    ): NutrientInfo? {
        val lowerFoodName = foodName.lowercase().trim()
        
        // Step 1: Try exact Chipotle mapping first
        val chipotleMatch = chipotleFoodMappings[lowerFoodName]
        if (chipotleMatch != null) {
            Log.d(TAG, "Found Chipotle mapping: '$foodName' -> '$chipotleMatch'")
            return helper.lookup(chipotleMatch, quantity, unit)
        }
        
        // Step 2: Try partial matches for Chipotle items
        for ((pattern, chipotleItem) in chipotleFoodMappings) {
            if (lowerFoodName.contains(pattern) || pattern.contains(lowerFoodName)) {
                Log.d(TAG, "Found partial Chipotle match: '$foodName' ~= '$pattern' -> '$chipotleItem'")
                return helper.lookup(chipotleItem, quantity, unit)
            }
        }
        
        // Step 3: Look for "chipotle" keyword and try generic matches
        if (lowerFoodName.contains("chipotle")) {
            val cleanedName = lowerFoodName.replace("chipotle", "").trim()
            val chipotleMatch2 = chipotleFoodMappings[cleanedName]
            if (chipotleMatch2 != null) {
                Log.d(TAG, "Found Chipotle keyword match: '$foodName' -> '$chipotleMatch2'")
                return helper.lookup(chipotleMatch2, quantity, unit)
            }
        }
        
        // Step 4: No Chipotle match found, return null
        return null
    }
    
    /**
     * Gets common serving sizes for Chipotle items to improve AI portion estimation.
     */
    fun getChipotleServingSuggestions(foodName: String): List<Pair<Double, String>> {
        val lowerName = foodName.lowercase()
        
        return when {
            lowerName.contains("tortilla") -> listOf(
                1.0 to "piece",
                1.0 to "ea"
            )
            lowerName.contains("rice") || lowerName.contains("beans") -> listOf(
                4.0 to "oz",
                0.5 to "cup",
                1.0 to "serving"
            )
            lowerName.contains("chicken") || lowerName.contains("steak") || 
            lowerName.contains("carnitas") || lowerName.contains("barbacoa") -> listOf(
                4.0 to "oz",
                1.0 to "serving"
            )
            lowerName.contains("salsa") -> listOf(
                2.0 to "fl oz", 
                4.0 to "fl oz",
                1.0 to "tbsp"
            )
            lowerName.contains("cheese") -> listOf(
                1.0 to "oz",
                0.25 to "cup"
            )
            lowerName.contains("guacamole") -> listOf(
                4.0 to "oz",
                2.0 to "oz",
                1.0 to "serving"
            )
            lowerName.contains("chips") -> listOf(
                4.0 to "oz",
                6.0 to "oz",
                1.0 to "bag"
            )
            else -> listOf(1.0 to "serving")
        }
    }
    
    /**
     * Estimates if a detected food item is likely from Chipotle based on context clues.
     */
    fun isLikelyChipotleItem(foodName: String, contextItems: List<String> = emptyList()): Boolean {
        val lowerName = foodName.lowercase()
        val allItems = (listOf(foodName) + contextItems).map { it.lowercase() }
        
        // Direct Chipotle indicators
        if (lowerName.contains("chipotle") || 
            lowerName.contains("cilantro-lime") ||
            lowerName.contains("barbacoa") ||
            lowerName.contains("sofritas") ||
            lowerName.contains("carnitas")) {
            return true
        }
        
        // Context-based detection - if multiple Chipotle-style items detected together
        val chipotleStyleItems = allItems.count { item ->
            chipotleFoodMappings.keys.any { pattern -> 
                item.contains(pattern) || pattern.contains(item)
            }
        }
        
        return chipotleStyleItems >= 2 // At least 2 Chipotle-style items suggest it's Chipotle
    }
    
    /**
     * Provides nutritional insights specific to Chipotle menu combinations.
     */
    fun getChipotleNutritionalTips(detectedItems: List<String>): List<String> {
        val tips = mutableListOf<String>()
        val lowerItems = detectedItems.map { it.lowercase() }
        
        if (lowerItems.any { it.contains("burrito") && it.contains("tortilla") }) {
            tips.add("💡 Chipotle burrito tortillas are 320 calories alone - consider a bowl to save calories")
        }
        
        if (lowerItems.any { it.contains("carnitas") || it.contains("barbacoa") }) {
            tips.add("🥩 Great protein choice! These meats are higher in fat but very flavorful")
        }
        
        if (lowerItems.any { it.contains("brown rice") }) {
            tips.add("🌾 Brown rice adds fiber and nutrients compared to white rice")
        }
        
        if (lowerItems.any { it.contains("black beans") || it.contains("pinto beans") }) {
            tips.add("🫘 Beans add protein and fiber - a nutritious choice!")
        }
        
        if (lowerItems.any { it.contains("guacamole") }) {
            tips.add("🥑 Guacamole adds healthy fats but is calorie-dense (230 cal for 4oz)")
        }
        
        return tips
    }
}