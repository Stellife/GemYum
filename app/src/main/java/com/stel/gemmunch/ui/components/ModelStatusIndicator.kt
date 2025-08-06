package com.stel.gemmunch.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stel.gemmunch.ModelStatus
import com.stel.gemmunch.utils.VisionModelPreferencesManager

@Composable
fun ModelStatusIndicator(
    modelStatus: ModelStatus,
    onClick: () -> Unit
) {
    // Animated loading effects for loading states
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    
    // Rotation animation for loading icons
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Pulsing alpha animation for loading text
    val pulsingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsing"
    )
    
    val statusText = when (modelStatus) {
        ModelStatus.INITIALIZING -> "Initializing"
        ModelStatus.PREPARING_SESSION -> "Preparing Session"
        ModelStatus.READY -> "Ready"
        ModelStatus.RUNNING_INFERENCE -> "Running Multi-modal Inference"
        ModelStatus.CLEANUP -> "Cleanup"
    }
    
    val statusColor = when (modelStatus) {
        ModelStatus.INITIALIZING -> MaterialTheme.colorScheme.onSurfaceVariant
        ModelStatus.PREPARING_SESSION -> MaterialTheme.colorScheme.primary
        ModelStatus.READY -> MaterialTheme.colorScheme.primary
        ModelStatus.RUNNING_INFERENCE -> MaterialTheme.colorScheme.secondary
        ModelStatus.CLEANUP -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val icon = when (modelStatus) {
        ModelStatus.INITIALIZING -> Icons.Default.HourglassEmpty
        ModelStatus.PREPARING_SESSION -> Icons.Default.Sync
        ModelStatus.READY -> Icons.Default.CheckCircle
        ModelStatus.RUNNING_INFERENCE -> Icons.Default.AutoAwesome
        ModelStatus.CLEANUP -> Icons.Default.CleaningServices
    }
    
    // Determine if this status should be animated
    val shouldAnimate = modelStatus in setOf(
        ModelStatus.INITIALIZING, 
        ModelStatus.PREPARING_SESSION,
        ModelStatus.RUNNING_INFERENCE
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp)
    ) {
        // Active Model information
        val selectedVisionModel = VisionModelPreferencesManager.getSelectedVisionModel()
        val modelDisplayName = VisionModelPreferencesManager.getVisionModelDisplayName(selectedVisionModel)
        
        Text(
            text = "Active Model: $modelDisplayName",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // AI Status row
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI Status:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            
            // Animated icon for loading states
            Icon(
                imageVector = icon,
                contentDescription = statusText,
                tint = statusColor,
                modifier = Modifier
                    .size(20.dp)
                    .then(
                        if (shouldAnimate) {
                            Modifier.rotate(rotationAngle)
                        } else {
                            Modifier
                        }
                    )
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Animated text for loading states
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
                modifier = if (shouldAnimate) {
                    Modifier.graphicsLayer { alpha = pulsingAlpha }
                } else {
                    Modifier
                }
            )
        }
    }
}