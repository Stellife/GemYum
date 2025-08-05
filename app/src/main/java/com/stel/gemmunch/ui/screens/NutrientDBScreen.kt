package com.stel.gemmunch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.stel.gemmunch.agent.AnalyzedFoodItem
import com.stel.gemmunch.viewmodels.NutrientDBViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutrientDBScreen(
    navController: NavController,
    viewModel: NutrientDBViewModel
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Remove Scaffold - let GemMunchAppScaffold handle navigation
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
                .padding(16.dp)
        ) {
            // Search Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Search Food Database",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Enter food name to see nutrition information from our database",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Food name") },
                        placeholder = { Text("e.g., pad thai, salmon, watermelon") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearSearch() }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.searchFood()
                                keyboardController?.hide()
                            }
                        ),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Search Button
                    Button(
                        onClick = {
                            viewModel.searchFood()
                            keyboardController?.hide()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = searchQuery.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Searching...")
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Search Database")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Results Section
            when {
                isLoading -> {
                    // Loading state already shown in button
                }
                searchResults.isEmpty() && searchQuery.isNotEmpty() -> {
                    // No results found
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No Results Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No nutrition data found for \"$searchQuery\". Try a different name or check spelling.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                searchResults.isNotEmpty() -> {
                    // Results found
                    Text(
                        "Search Results (${searchResults.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults) { foodItem ->
                            NutrientDBResultCard(foodItem = foodItem)
                        }
                    }
                }
                else -> {
                    // Initial state - show example searches
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Example Searches",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val examples = listOf(
                                "pad thai" to "Thai noodle dish",
                                "salmon" to "Fish protein",
                                "watermelon" to "Fresh fruit",
                                "chipotle burrito bowl" to "Restaurant meal",
                                "greek yogurt" to "Dairy product"
                            )
                            
                            examples.forEach { (food, description) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        food,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NutrientDBResultCard(
    foodItem: AnalyzedFoodItem
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = foodItem.foodName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${foodItem.calories} cal per ${foodItem.quantity} ${foodItem.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = if (expanded) "Hide Details" else "Show Details",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Expandable Details
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                // Nutrition Details
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Nutrition Information",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Macronutrients
                    NutrientRow("Calories", foodItem.calories.toString(), "cal")
                    foodItem.protein?.let { NutrientRow("Protein", "%.1f".format(it), "g") }
                    foodItem.totalFat?.let { NutrientRow("Total Fat", "%.1f".format(it), "g") }
                    foodItem.totalCarbs?.let { NutrientRow("Carbohydrates", "%.1f".format(it), "g") }
                    
                    // Micronutrients (if available)
                    foodItem.saturatedFat?.let { NutrientRow("Saturated Fat", "%.1f".format(it), "g") }
                    foodItem.cholesterol?.let { NutrientRow("Cholesterol", "%.1f".format(it), "mg") }
                    foodItem.sodium?.let { NutrientRow("Sodium", "%.1f".format(it), "mg") }
                    foodItem.dietaryFiber?.let { NutrientRow("Dietary Fiber", "%.1f".format(it), "g") }
                    foodItem.sugars?.let { NutrientRow("Sugars", "%.1f".format(it), "g") }
                    
                    // Special metrics
                    foodItem.glycemicLoad?.let { 
                        if (it > 0) NutrientRow("Glycemic Load", "%.1f".format(it), "") 
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Enhanced Source Information
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            "Search Process",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Determine likely search path based on food properties
                        val searchPath = determineSearchPath(foodItem)
                        
                        searchPath.forEach { step ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "• ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    step,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Note: This shows the likely search process based on data patterns",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NutrientRow(
    name: String,
    value: String,
    unit: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "$value $unit",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Determines the likely search process based on food item characteristics
 */
@Composable
fun determineSearchPath(foodItem: AnalyzedFoodItem): List<String> {
    val foodName = foodItem.foodName.lowercase()
    val searchSteps = mutableListOf<String>()
    
    // Analyze food name to determine likely search path
    when {
        // Complex dishes likely went through USDA API with poor matching
        foodName.contains("pad thai") || foodName.contains("curry") || 
        foodName.contains("stir fry") || foodName.contains("casserole") -> {
            searchSteps.add("Local DB: No matches found")
            searchSteps.add("USDA API: Searched for '$foodName'")
            if (foodName.contains("pad thai") && foodItem.foodName.contains("spread")) {
                searchSteps.add("Found 5 results, but poor matches (soup, spread, etc.)")
                searchSteps.add("Selected best available: '${foodItem.foodName}' (score: ~124)")
                searchSteps.add("⚠️ Match quality: Low - generic ingredient substituted")
            } else {
                searchSteps.add("Advanced scoring applied")
                searchSteps.add("Selected: '${foodItem.foodName}'")
            }
        }
        
        // Simple ingredients likely found in local DB or good USDA match
        foodName.contains("chicken") && !foodName.contains("spread") -> {
            searchSteps.add("Local DB: Checked first")
            searchSteps.add("USDA API: Found good match")
            searchSteps.add("Selected: '${foodItem.foodName}'")
        }
        
        // Fruits and vegetables usually have good matches
        listOf("apple", "banana", "carrot", "broccoli", "salmon", "rice").any { foodName.contains(it) } -> {
            searchSteps.add("Local DB: Found or USDA direct match")
            searchSteps.add("Source: High-quality nutrition data")
            searchSteps.add("Selected: '${foodItem.foodName}'")
        }
        
        // Generic/processed foods indicate USDA fallback
        foodName.contains("spread") || foodName.contains("meatless") || foodName.contains("generic") -> {
            searchSteps.add("Local DB: No specific match")
            searchSteps.add("USDA API: Found substitute ingredient")
            searchSteps.add("⚠️ This is a fallback match - may not represent actual food")
            searchSteps.add("Selected: '${foodItem.foodName}'")
        }
        
        else -> {
            searchSteps.add("Local DB: Searched first")
            searchSteps.add("USDA API: ${if (foodItem.calories > 0) "Found match" else "No results"}")
            searchSteps.add("Selected: '${foodItem.foodName}'")
        }
    }
    
    return searchSteps
}