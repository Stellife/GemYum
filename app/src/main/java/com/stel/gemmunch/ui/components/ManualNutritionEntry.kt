package com.stel.gemmunch.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stel.gemmunch.agent.AnalyzedFoodItem
import kotlinx.coroutines.launch

/**
 * A composable for manually entering food items and searching for their nutrition data.
 * This is designed to be reusable across different screens.
 */
@Composable
fun ManualNutritionEntry(
    onSearchNutrition: suspend (String, Double) -> AnalyzedFoodItem?,
    onItemsChanged: (List<AnalyzedFoodItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    var manualItems by remember { mutableStateOf(listOf<AnalyzedFoodItem>()) }
    var showAddItem by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var newItemServing by remember { mutableStateOf("1") }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header with Add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Additional Items",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            FilledTonalButton(
                onClick = { showAddItem = !showAddItem },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add item"
                )
                Spacer(Modifier.width(4.dp))
                Text("Add Item")
            }
        }
        
        // Add item form
        AnimatedVisibility(
            visible = showAddItem,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Item name input
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Food item") },
                        placeholder = { Text("e.g., salmon, watermelon") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Serving size input
                    OutlinedTextField(
                        value = newItemServing,
                        onValueChange = { newItemServing = it },
                        label = { Text("Serving size") },
                        placeholder = { Text("e.g., 1.25") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Error message
                    searchError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                showAddItem = false
                                newItemName = ""
                                newItemServing = "1"
                                searchError = null
                            }
                        ) {
                            Text("Cancel")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val serving = newItemServing.toDoubleOrNull() ?: 1.0
                                    if (newItemName.isNotBlank() && serving > 0) {
                                        isSearching = true
                                        searchError = null
                                        
                                        val result = onSearchNutrition(newItemName.trim(), serving)
                                        
                                        if (result != null) {
                                            manualItems = manualItems + result
                                            onItemsChanged(manualItems)
                                            
                                            // Reset form
                                            showAddItem = false
                                            newItemName = ""
                                            newItemServing = "1"
                                        } else {
                                            searchError = "No nutrition data found for \"$newItemName\". Try a different name or check spelling."
                                        }
                                        
                                        isSearching = false
                                    }
                                }
                            },
                            enabled = newItemName.isNotBlank() && !isSearching
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search"
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text("Analyze")
                        }
                    }
                }
            }
        }
        
        // List of manual items
        manualItems.forEachIndexed { index, item ->
            ManualNutritionItemCard(
                item = item,
                onServingSizeChanged = { newServing ->
                    // Recalculate nutrition based on new serving size
                    val scaleFactor = newServing / item.quantity
                    val updatedItem = item.copy(
                        quantity = newServing,
                        calories = (item.calories * scaleFactor).toInt(),
                        protein = item.protein?.let { it * scaleFactor },
                        totalFat = item.totalFat?.let { it * scaleFactor },
                        saturatedFat = item.saturatedFat?.let { it * scaleFactor },
                        cholesterol = item.cholesterol?.let { it * scaleFactor },
                        sodium = item.sodium?.let { it * scaleFactor },
                        totalCarbs = item.totalCarbs?.let { it * scaleFactor },
                        dietaryFiber = item.dietaryFiber?.let { it * scaleFactor },
                        sugars = item.sugars?.let { it * scaleFactor },
                        glycemicLoad = item.glycemicLoad?.let { it * scaleFactor }
                    )
                    
                    manualItems = manualItems.toMutableList().apply { set(index, updatedItem) }
                    onItemsChanged(manualItems)
                },
                onNutrientChanged = { nutrientName, newValue ->
                    // Update specific nutrient value
                    val updatedItem = when (nutrientName) {
                        "calories" -> item.copy(calories = newValue?.toInt() ?: 0)
                        "protein" -> item.copy(protein = newValue)
                        "totalFat" -> item.copy(totalFat = newValue)
                        "saturatedFat" -> item.copy(saturatedFat = newValue)
                        "cholesterol" -> item.copy(cholesterol = newValue)
                        "sodium" -> item.copy(sodium = newValue)
                        "totalCarbs" -> item.copy(totalCarbs = newValue)
                        "dietaryFiber" -> item.copy(dietaryFiber = newValue)
                        "sugars" -> item.copy(sugars = newValue)
                        else -> item
                    }
                    
                    manualItems = manualItems.toMutableList().apply { set(index, updatedItem) }
                    onItemsChanged(manualItems)
                },
                onDelete = {
                    manualItems = manualItems.filterIndexed { i, _ -> i != index }
                    onItemsChanged(manualItems)
                }
            )
        }
    }
}

@Composable
private fun ManualNutritionItemCard(
    item: AnalyzedFoodItem,
    onServingSizeChanged: (Double) -> Unit,
    onNutrientChanged: (String, Double?) -> Unit,
    onDelete: () -> Unit
) {
    var servingText by remember(item.quantity) { mutableStateOf(item.quantity.toString()) }
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.foodName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.calories} cal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand for more nutrition info",
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (expanded) "Hide details" else "Show details",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
                
                // Serving size editor
                OutlinedTextField(
                    value = servingText,
                    onValueChange = { 
                        servingText = it
                        it.toDoubleOrNull()?.let { serving ->
                            if (serving > 0) onServingSizeChanged(serving)
                        }
                    },
                    label = { Text("Servings") },
                    modifier = Modifier.width(100.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete item",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // Expandable nutrition details
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Nutrition editors
                    NutrientEditor("Calories", item.calories.toDouble(), "cal") { 
                        onNutrientChanged("calories", it)
                    }
                    item.protein?.let {
                        NutrientEditor("Protein", it, "g") { value ->
                            onNutrientChanged("protein", value)
                        }
                    }
                    item.totalFat?.let {
                        NutrientEditor("Total Fat", it, "g") { value ->
                            onNutrientChanged("totalFat", value)
                        }
                    }
                    item.totalCarbs?.let {
                        NutrientEditor("Carbs", it, "g") { value ->
                            onNutrientChanged("totalCarbs", value)
                        }
                    }
                    item.sodium?.let {
                        NutrientEditor("Sodium", it, "mg") { value ->
                            onNutrientChanged("sodium", value)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NutrientEditor(
    name: String,
    value: Double,
    unit: String,
    onValueChange: (Double?) -> Unit
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textValue,
                onValueChange = { 
                    textValue = it
                    onValueChange(it.toDoubleOrNull())
                },
                modifier = Modifier.width(80.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            IconButton(
                onClick = { 
                    textValue = ""
                    onValueChange(null)
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear value",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}