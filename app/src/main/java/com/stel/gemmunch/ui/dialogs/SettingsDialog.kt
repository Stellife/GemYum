package com.stel.gemmunch.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stel.gemmunch.ui.MainViewModel
import com.stel.gemmunch.ui.VisionModelSelectionCard
import com.stel.gemmunch.ui.MediaQualitySelectionCard
import com.stel.gemmunch.ui.ImageReasoningModeCard
import com.stel.gemmunch.agent.MultiDownloadState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    mainViewModel: MainViewModel
) {
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val modelFiles = remember(mainUiState.downloadState) {
        (mainUiState.downloadState as? MultiDownloadState.AllComplete)?.files ?: emptyMap()
    }
    val scope = rememberCoroutineScope()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .heightIn(min = 400.dp, max = LocalConfiguration.current.screenHeightDp.dp * 0.85f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Title Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Divider
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 16.dp),
                        thickness = 1.dp
                    )
                    
                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Vision Model Selection
                        VisionModelSelectionCard(
                            modelFiles = modelFiles,
                            onSwitchModel = { newModelKey, modelFiles ->
                                // Switch model through AppContainer
                                scope.launch {
                                    mainViewModel.appContainer.switchVisionModel(newModelKey, modelFiles)
                                }
                            }
                        )
                        
                        // Media Quality Selection
                        MediaQualitySelectionCard()
                        
                        // Image Analysis Mode
                        ImageReasoningModeCard()
                        
                        // Add bottom padding for better scrolling experience
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}