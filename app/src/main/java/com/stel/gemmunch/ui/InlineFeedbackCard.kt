package com.stel.gemmunch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stel.gemmunch.agent.AnalyzedFoodItem
import com.stel.gemmunch.data.models.ErrorType
import com.stel.gemmunch.data.models.HealthConnectWriteChoice
import com.stel.gemmunch.ui.components.ManualNutritionEntry
import com.stel.gemmunch.data.NutritionSearchService

data class ItemFeedback(
    val itemIndex: Int,
    val overallScore: Int = 3,
    val selectedErrors: Set<ErrorType> = emptySet(),
    val errorDetails: Map<ErrorType, String> = emptyMap(),
    val restaurantOrMfgName: String = "",
    val mealDescription: String = "",
    val providingCorrections: Boolean = false,
    val correctedValues: Map<String, String> = emptyMap(),
    val nutritionSource: String = "",
    val manualItems: List<com.stel.gemmunch.agent.AnalyzedFoodItem> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InlineFeedbackCard(
    item: AnalyzedFoodItem,
    itemIndex: Int,
    feedback: ItemFeedback,
    nutritionSearchService: NutritionSearchService,
    onFeedbackUpdate: (ItemFeedback) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedErrors by remember { mutableStateOf(feedback.selectedErrors) }
    var errorDetails by remember { mutableStateOf(feedback.errorDetails) }
    
    // Score is now calculated automatically based on errors
    val overallScore = calculateQualityScore(selectedErrors)
    
    // Meal details
    var restaurantOrMfgName by remember { mutableStateOf(feedback.restaurantOrMfgName) }
    var mealDescription by remember { mutableStateOf(feedback.mealDescription) }
    
    // Nutritional corrections
    var providingCorrections by remember { mutableStateOf(feedback.providingCorrections) }
    var correctedValues by remember { mutableStateOf(feedback.correctedValues) }
    var nutritionSource by remember { mutableStateOf(feedback.nutritionSource) }
    
    // Manual items
    var manualItems by remember { mutableStateOf(feedback.manualItems) }
    
    // Update parent whenever any value changes
    LaunchedEffect(
        selectedErrors, errorDetails, 
        restaurantOrMfgName, mealDescription, providingCorrections, 
        correctedValues, nutritionSource, manualItems
    ) {
        onFeedbackUpdate(
            ItemFeedback(
                itemIndex = itemIndex,
                overallScore = overallScore, // Now auto-calculated
                selectedErrors = selectedErrors,
                errorDetails = errorDetails,
                restaurantOrMfgName = restaurantOrMfgName,
                mealDescription = mealDescription,
                providingCorrections = providingCorrections,
                correctedValues = correctedValues,
                nutritionSource = nutritionSource,
                manualItems = manualItems
            )
        )
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                "Feedback for ${item.foodName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Analysis Quality Score (Auto-calculated)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Analysis Quality",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    "Score: $overallScore/5",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = getScoreColor(overallScore)
                )
            }
            
            HorizontalDivider()
            
            // Error Selection Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Any Errors?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                ErrorType.values().forEach { errorType ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = errorType in selectedErrors,
                                    onClick = {
                                        selectedErrors = if (errorType in selectedErrors) {
                                            selectedErrors - errorType
                                        } else {
                                            selectedErrors + errorType
                                        }
                                    },
                                    role = Role.Checkbox
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = errorType in selectedErrors,
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(errorType.displayName)
                        }
                        
                        // Individual text box for each selected error
                        if (errorType in selectedErrors) {
                            OutlinedTextField(
                                value = errorDetails[errorType] ?: "",
                                onValueChange = { value ->
                                    errorDetails = errorDetails + (errorType to value)
                                },
                                label = { Text("Describe the error") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 40.dp, top = 4.dp),
                                singleLine = false,
                                minLines = 2
                            )
                        }
                    }
                }
            }
            
            HorizontalDivider()
            
            // Additional Meal Details Section (Optional)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Additional Meal Details (Optional)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                OutlinedTextField(
                    value = restaurantOrMfgName,
                    onValueChange = { restaurantOrMfgName = it },
                    label = { Text("Restaurant or Manufacturer Name") },
                    placeholder = { Text("e.g., Chipotle, Chobani") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = mealDescription,
                    onValueChange = { mealDescription = it },
                    label = { Text("Additional Details") },
                    placeholder = { Text("e.g., chicken burrito bowl with brown rice") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2
                )
            }
            
            HorizontalDivider()
            
            // Nutritional Corrections Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Enter known nutritional info (Calories, Protein, etc)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = providingCorrections,
                        onCheckedChange = { providingCorrections = it }
                    )
                }
                
                if (providingCorrections) {
                    // Show main nutrients for correction
                    listOf(
                        "Calories" to "",
                        "Protein" to "g",
                        "Total Fat" to "g",
                        "Carbohydrates" to "g",
                        "Sodium" to "mg"
                    ).forEach { (nutrient, unit) ->
                        OutlinedTextField(
                            value = correctedValues[nutrient] ?: "",
                            onValueChange = { value ->
                                correctedValues = if (value.isBlank()) {
                                    correctedValues - nutrient
                                } else {
                                    correctedValues + (nutrient to value)
                                }
                            },
                            label = { Text("$nutrient${if (unit.isNotEmpty()) " ($unit)" else ""}") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    OutlinedTextField(
                        value = nutritionSource,
                        onValueChange = { nutritionSource = it },
                        label = { Text("Source of Information") },
                        placeholder = { Text("e.g., Restaurant website, Food package") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            HorizontalDivider()
            
            // Manual Items Section
            ManualNutritionEntry(
                onSearchNutrition = { foodName, servingSize ->
                    nutritionSearchService.searchNutrition(foodName, servingSize)
                },
                onItemsChanged = { items ->
                    manualItems = items
                }
            )
        }
    }
}


private fun getScoreColor(score: Int): Color = when (score) {
    0, 1 -> Color(0xFFD32F2F) // Red
    2 -> Color(0xFFF57C00) // Orange
    3 -> Color(0xFFFBC02D) // Yellow
    4 -> Color(0xFF689F38) // Light Green
    5 -> Color(0xFF388E3C) // Green
    else -> Color.Gray
}

/**
 * Calculates quality score based on error types selected
 * - No errors → score 5
 * - Nutritional information feels wrong → score 4  
 * - Food was slightly wrong (missing ingredients) → score 3
 * - Quantity was wrong → score 2
 * - Incorrect food identified → score 1
 */
private fun calculateQualityScore(selectedErrors: Set<ErrorType>): Int {
    return when {
        selectedErrors.isEmpty() -> 5 // No errors
        selectedErrors.contains(ErrorType.FOOD_COMPLETELY_WRONG) -> 1
        selectedErrors.contains(ErrorType.QUANTITY_WRONG) -> 2
        selectedErrors.contains(ErrorType.FOOD_SLIGHTLY_WRONG) -> 3
        selectedErrors.contains(ErrorType.NUTRITION_WRONG) -> 4
        else -> 3 // Default case
    }
}