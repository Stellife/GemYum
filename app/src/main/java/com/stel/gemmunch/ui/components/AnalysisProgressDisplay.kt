package com.stel.gemmunch.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stel.gemmunch.data.models.AnalysisProgress
import com.stel.gemmunch.data.models.AnalysisStep

@Composable
fun AnalysisProgressDisplay(
    progress: AnalysisProgress,
    modelName: String = "",
    reasoningMode: String = "",
    imageSize: String = "",
    contextLength: Int = 0,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Overall progress
            Text(
                text = "Analyzing your meal...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Analysis details
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (modelName.isNotEmpty() && reasoningMode.isNotEmpty()) {
                    Text(
                        text = "Model: $modelName, Mode: $reasoningMode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (imageSize.isNotEmpty()) {
                    Text(
                        text = "Image Size: $imageSize",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (contextLength > 0) {
                    Text(
                        text = "Additional Textual Context: $contextLength chars",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Additional Textual Context: None",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress bar
            val animatedProgress by animateFloatAsState(
                targetValue = progress.overallProgress,
                animationSpec = tween(300),
                label = "progress"
            )
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                strokeCap = StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${(progress.overallProgress * 100).toInt()}% Complete",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Individual steps
            progress.steps.forEach { step ->
                AnalysisStepRow(
                    step = step,
                    modifier = Modifier.fillMaxWidth()
                )
                if (step != progress.steps.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Error message if any
            progress.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalysisStepRow(
    step: AnalysisStep,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status icon
        val icon = when (step.status) {
            AnalysisStep.StepStatus.COMPLETED -> Icons.Default.CheckCircle
            AnalysisStep.StepStatus.IN_PROGRESS -> Icons.Default.Refresh
            AnalysisStep.StepStatus.FAILED -> Icons.Default.Error
            AnalysisStep.StepStatus.PENDING -> Icons.Default.RadioButtonUnchecked
        }
        
        val iconColor = when (step.status) {
            AnalysisStep.StepStatus.COMPLETED -> MaterialTheme.colorScheme.primary
            AnalysisStep.StepStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondary
            AnalysisStep.StepStatus.FAILED -> MaterialTheme.colorScheme.error
            AnalysisStep.StepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        
        Icon(
            imageVector = icon,
            contentDescription = step.status.name,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Step name and details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (step.status == AnalysisStep.StepStatus.IN_PROGRESS) 
                    FontWeight.Bold else FontWeight.Normal
            )
            
            step.details?.let { details ->
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Duration
        if (step.durationMs != null && step.durationMs > 0) {
            Text(
                text = formatDuration(step.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (step.status == AnalysisStep.StepStatus.IN_PROGRESS) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    return when {
        durationMs < 1000 -> "${durationMs}ms"
        durationMs < 60000 -> "%.1fs".format(durationMs / 1000.0)
        else -> "${durationMs / 60000}m ${(durationMs % 60000) / 1000}s"
    }
}