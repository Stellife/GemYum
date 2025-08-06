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
import com.stel.gemmunch.data.InitializationMetrics
import com.stel.gemmunch.data.models.AccelerationStats
import com.stel.gemmunch.ui.AccelerationStatsCard
import com.stel.gemmunch.ui.InitializationMetricsCard
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AiDetailsDialog(
    onDismiss: () -> Unit,
    accelerationStats: AccelerationStats?,
    initializationMetrics: InitializationMetrics,
    metricsFlow: StateFlow<List<InitializationMetrics.MetricUpdate>>
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false, // Allow custom width
            decorFitsSystemWindows = true // Helps with pointer bounds
        )
    ) {
        // Add a Box to handle click outside and prevent pointer bounds issues
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
                    .fillMaxWidth(0.9f) // 90% of screen width
                    .wrapContentHeight() // Dynamic height based on content
                    .heightIn(min = 400.dp, max = LocalConfiguration.current.screenHeightDp.dp * 0.85f) // Min height for loading state
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Prevent clicks from passing through
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
                    .padding(20.dp) // Adequate padding inside
            ) {
                // Title Row (non-scrollable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI System Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp) // Larger close button
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
                    verticalArrangement = Arrangement.spacedBy(20.dp) // More space between cards
                ) {
                    // Acceleration Stats
                    if (accelerationStats != null) {
                        AccelerationStatsCard(
                            accelerationStats = accelerationStats,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Initialization Metrics
                    InitializationMetricsCard(
                        metricsFlow = metricsFlow,
                        initializationMetrics = initializationMetrics,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Add bottom padding for better scrolling experience
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
    }
}