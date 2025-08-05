package com.stel.gemmunch.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stel.gemmunch.AppContainer
import com.stel.gemmunch.agent.AnalyzedFoodItem

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "NutrientDBViewModel"

/**
 * ViewModel for the Nutrient Database search screen.
 * Provides nutrition lookup functionality without any state persistence.
 */
class NutrientDBViewModel(
    private val appContainer: AppContainer
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<AnalyzedFoodItem>>(emptyList())
    val searchResults: StateFlow<List<AnalyzedFoodItem>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _hasSearched.value = false
    }

    fun searchFood() {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            _searchResults.value = emptyList()
            
            try {
                Log.d(TAG, "Searching nutrition database for: '$query'")
                
                // Perform nutrition search using existing infrastructure
                val results = searchForMultipleResults(query)
                
                Log.d(TAG, "Found ${results.size} results for '$query'")
                _searchResults.value = results
                
            } catch (e: Exception) {
                Log.e(TAG, "Error searching nutrition database", e)
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
                _hasSearched.value = true // Mark that a search was performed
            }
        }
    }

    /**
     * Search for multiple nutrition results to show variety in database coverage.
     * This attempts different serving sizes and search strategies.
     */
    private suspend fun searchForMultipleResults(query: String): List<AnalyzedFoodItem> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<AnalyzedFoodItem>()
            
            try {
                // Primary search with default serving
                val primaryResult = appContainer.nutritionSearchService.searchNutrition(
                    foodName = query,
                    servingSize = 1.0,
                    servingUnit = "serving"
                )
                
                primaryResult?.let { results.add(it) }
                
                // Try alternative serving sizes to show different perspectives
                if (primaryResult != null) {
                    // 100g serving (common nutrition label standard)
                    val result100g = appContainer.nutritionSearchService.searchNutrition(
                        foodName = query,
                        servingSize = 100.0,
                        servingUnit = "g"
                    )
                    
                    // Only add if significantly different from primary result
                    result100g?.let { result ->
                        if (!isDuplicateResult(result, results)) {
                            results.add(result)
                        }
                    }
                    
                    // Try cup measurement for foods that commonly use it
                    if (isCommonCupFood(query)) {
                        val resultCup = appContainer.nutritionSearchService.searchNutrition(
                            foodName = query,
                            servingSize = 1.0,
                            servingUnit = "cup"
                        )
                        
                        resultCup?.let { result ->
                            if (!isDuplicateResult(result, results)) {
                                results.add(result)
                            }
                        }
                    }
                }
                
                Log.d(TAG, "Generated ${results.size} nutrition variations for '$query'")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in multi-result search for '$query'", e)
            }
            
            results.take(3) // Limit to top 3 results to avoid overwhelming UI
        }
    }

    /**
     * Check if this result is essentially a duplicate of existing results
     */
    private fun isDuplicateResult(newResult: AnalyzedFoodItem, existingResults: List<AnalyzedFoodItem>): Boolean {
        return existingResults.any { existing ->
            // Consider duplicate if calories per gram are very similar (within 10%)
            val newCalPerGram = newResult.calories / (newResult.quantity * getGramConversion(newResult.unit))
            val existingCalPerGram = existing.calories / (existing.quantity * getGramConversion(existing.unit))
            
            val difference = kotlin.math.abs(newCalPerGram - existingCalPerGram) / maxOf(newCalPerGram, existingCalPerGram)
            difference < 0.1 // Less than 10% difference
        }
    }

    /**
     * Get approximate gram conversion for common units
     */
    private fun getGramConversion(unit: String): Double {
        return when (unit.lowercase()) {
            "g", "gram", "grams" -> 1.0
            "kg", "kilogram", "kilograms" -> 1000.0
            "oz", "ounce", "ounces" -> 28.35
            "lb", "pound", "pounds" -> 453.6
            "cup", "cups" -> 240.0 // Approximate for liquids
            "serving" -> 100.0 // Default assumption
            else -> 100.0
        }
    }

    /**
     * Check if a food commonly uses cup measurements
     */
    private fun isCommonCupFood(query: String): Boolean {
        val cupFoods = listOf(
            "rice", "pasta", "noodles", "cereal", "oats", "quinoa",
            "milk", "yogurt", "soup", "broth", "juice",
            "berries", "grapes", "chopped", "diced"
        )
        
        return cupFoods.any { query.lowercase().contains(it) }
    }
}

/**
 * Factory for creating NutrientDBViewModel
 */
class NutrientDBViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NutrientDBViewModel::class.java)) {
            return NutrientDBViewModel(appContainer) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}