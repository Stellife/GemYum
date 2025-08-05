package com.stel.gemmunch.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stel.gemmunch.ModelStatus

@Composable
fun ModelStatusIndicator(
    modelStatus: ModelStatus,
    onClick: () -> Unit
) {
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
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = "AI Status:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = icon,
            contentDescription = statusText,
            tint = statusColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium,
            color = statusColor
        )
    }
}