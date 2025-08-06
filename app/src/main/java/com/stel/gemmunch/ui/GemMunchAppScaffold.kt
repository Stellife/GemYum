package com.stel.gemmunch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.stel.gemmunch.ModelStatus
import com.stel.gemmunch.ui.components.ModelStatusIndicator
import com.stel.gemmunch.ui.dialogs.AiDetailsDialog
import com.stel.gemmunch.ui.dialogs.SettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GemYumAppScaffold(
    navController: NavController,
    mainViewModel: MainViewModel,
    analyzeAndChatViewModel: com.stel.gemmunch.viewmodels.EnhancedChatViewModel,
    textOnlyMealViewModel: com.stel.gemmunch.viewmodels.TextOnlyMealViewModel,
    currentRoute: String?,
    currentChatMode: String? = null,
    showBackButton: Boolean = true,
    title: @Composable () -> Unit = { 
        Text(
            text = "GemYum",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    },
    content: @Composable (PaddingValues) -> Unit
) {
    val modelStatus by mainViewModel.appContainer.modelStatus.collectAsStateWithLifecycle()
    val accelerationStats by mainViewModel.appContainer.accelerationStats.collectAsStateWithLifecycle()
    val initMetrics = mainViewModel.initMetrics
    val initMetricsUpdates = mainViewModel.initMetricsUpdates
    
    // Chat inference states for both ViewModels
    val deepChatIsLoading by analyzeAndChatViewModel.isLoading.collectAsStateWithLifecycle()
    val textOnlyIsLoading by textOnlyMealViewModel.isLoading.collectAsStateWithLifecycle()
    val deepChatHasImage by analyzeAndChatViewModel.hasImage.collectAsStateWithLifecycle()
    val textOnlyMessages by textOnlyMealViewModel.messages.collectAsStateWithLifecycle()
    
    var showAiDetailsDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Show custom title
                        title()
                        
                        // Show model status below title
                        ModelStatusIndicator(
                            modelStatus = modelStatus,
                            onClick = { showAiDetailsDialog = true }
                        )
                    }
                },
                navigationIcon = {
                    if (showBackButton && currentRoute != "home" && navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    when (currentChatMode) {
                        "true" -> {
                            // Deep Chat mode (vision + text)
                            if (deepChatIsLoading) {
                                // Show stop button when inference is running
                                IconButton(onClick = { 
                                    analyzeAndChatViewModel.stopGeneration()
                                }) {
                                    Icon(
                                        Icons.Filled.Cancel,
                                        contentDescription = "Stop Generation",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else if (deepChatHasImage) {
                                // Show reset button when not loading and has image
                                IconButton(onClick = { 
                                    analyzeAndChatViewModel.showResetDialog()
                                }) {
                                    Icon(
                                        Icons.Filled.Refresh,
                                        contentDescription = "Reset Conversation",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        "false" -> {
                            // Text Only mode
                            if (textOnlyIsLoading) {
                                // Show stop button when inference is running (though Text Only may not need this)
                                IconButton(onClick = { 
                                    // Text Only doesn't have stop generation, so we'll just show disabled button
                                }) {
                                    Icon(
                                        Icons.Filled.Cancel,
                                        contentDescription = "Stop Generation",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                    )
                                }
                            } else if (textOnlyMessages.isNotEmpty()) {
                                // Show reset button when there are messages
                                IconButton(onClick = { 
                                    textOnlyMealViewModel.showResetDialog()
                                }) {
                                    Icon(
                                        Icons.Filled.Refresh,
                                        contentDescription = "Reset Conversation",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    // Only show settings icon on home screen
                    if (currentRoute == "home") {
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        content(paddingValues)
    }
    
    // Global AI Details Dialog
    if (showAiDetailsDialog) {
        AiDetailsDialog(
            onDismiss = { showAiDetailsDialog = false },
            accelerationStats = accelerationStats,
            initializationMetrics = initMetrics,
            metricsFlow = initMetricsUpdates
        )
    }
    
    // Global Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false },
            mainViewModel = mainViewModel
        )
    }
}