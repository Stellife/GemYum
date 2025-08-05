package com.stel.gemmunch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.stel.gemmunch.model.ChatMessage
import com.stel.gemmunch.viewmodels.QuickAction
import com.stel.gemmunch.viewmodels.TextOnlyMealViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextOnlyMealScreen(
    navController: NavController,
    viewModel: TextOnlyMealViewModel
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val availableActions by viewModel.availableActions.collectAsStateWithLifecycle()
    val currentMealNutrition by viewModel.currentMealNutrition.collectAsStateWithLifecycle()
    val showHealthConnectDialog by viewModel.showHealthConnectDialog.collectAsStateWithLifecycle()
    val showResetDialog by viewModel.showResetDialog.collectAsStateWithLifecycle()
    
    var inputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new messages arrive or keyboard appears
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    // Remove Scaffold - let GemMunchAppScaffold handle navigation
    Box(modifier = Modifier.fillMaxSize()) {
        // Use imePadding to handle keyboard properly
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding() // This handles keyboard adjustments
        ) {

            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 80.dp // Extra bottom padding to ensure last message is visible above input
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        isFromUser = message.isFromUser
                    )
                }
                
                // Loading indicator
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Analyzing...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Action Buttons
            if (availableActions.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableActions) { action ->
                        QuickActionButton(
                            action = action,
                            onClick = { viewModel.executeQuickAction(action) },
                            enabled = !isLoading
                        )
                    }
                }
            }

            // Nutrition Summary (if available)
            if (currentMealNutrition.isNotEmpty()) {
                NutritionSummaryCard(
                    nutritionItems = currentMealNutrition,
                    onSaveToHealth = { viewModel.requestHealthConnectSave() }
                )
            }

            // Input Area
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { 
                            inputText = it
                            // Auto-scroll when user starts typing
                            if (messages.isNotEmpty()) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Describe your meal...") },
                        placeholder = { 
                            Text("e.g., I had grilled chicken with rice and broccoli") 
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !isLoading) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        maxLines = 3,
                        enabled = !isLoading
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isLoading) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                                keyboardController?.hide()
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message"
                            )
                        }
                    }
                }
            }
        }

        // Health Connect Dialog (inside Box for proper positioning)
    if (showHealthConnectDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissHealthConnectDialog() },
            title = { Text("Save to Health App") },
            text = { 
                Text("Would you like to save this meal's nutrition data to your health tracking app?") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // TODO: Implement Health Connect save
                        viewModel.dismissHealthConnectDialog()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissHealthConnectDialog() }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissResetDialog() },
            title = { Text("Reset Conversation") },
            text = { Text("This will erase and reset the entire conversation. Are you sure?") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        viewModel.dismissResetDialog()
                        viewModel.resetConversation()
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissResetDialog() }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    } // Close Box
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isFromUser: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isFromUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isFromUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun QuickActionButton(
    action: QuickAction,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    FilterChip(
        onClick = onClick,
        label = { 
            Text(
                action.label,
                style = MaterialTheme.typography.labelMedium
            ) 
        },
        selected = false,
        enabled = enabled,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun NutritionSummaryCard(
    nutritionItems: List<com.stel.gemmunch.agent.AnalyzedFoodItem>,
    onSaveToHealth: () -> Unit
) {
    val totalCalories = nutritionItems.sumOf { it.calories }
    val totalProtein = nutritionItems.sumOf { it.protein ?: 0.0 }
    val totalCarbs = nutritionItems.sumOf { it.totalCarbs ?: 0.0 }
    val totalFat = nutritionItems.sumOf { it.totalFat ?: 0.0 }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "📊 Nutrition Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutritionMetric("🔥", totalCalories.toString(), "cal")
                NutritionMetric("🥩", String.format("%.1f", totalProtein), "g protein")
                NutritionMetric("🍞", String.format("%.1f", totalCarbs), "g carbs")
                NutritionMetric("🥑", String.format("%.1f", totalFat), "g fat")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onSaveToHealth,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("💾 Save to Health App")
            }
        }
    }
}

@Composable
fun NutritionMetric(
    emoji: String,
    value: String,
    unit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            emoji,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            unit,
            style = MaterialTheme.typography.labelSmall
        )
    }
}