package com.stel.gemmunch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun GemMunchAppScaffold(
    navController: NavController,
    mainViewModel: MainViewModel,
    currentRoute: String?,
    showBackButton: Boolean = true,
    title: @Composable () -> Unit = { 
        Text(
            text = "GemMunch",
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
    
    var showAiDetailsDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    ModelStatusIndicator(
                        modelStatus = modelStatus,
                        onClick = { showAiDetailsDialog = true }
                    )
                },
                navigationIcon = {
                    if (showBackButton && currentRoute != "home" && navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    } else {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            title()
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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