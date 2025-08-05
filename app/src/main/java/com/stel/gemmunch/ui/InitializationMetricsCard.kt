package com.stel.gemmunch.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stel.gemmunch.data.InitializationMetrics
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitializationMetricsCard(
    metricsFlow: StateFlow<List<InitializationMetrics.MetricUpdate>>,
    initializationMetrics: InitializationMetrics,
    modifier: Modifier = Modifier
) {
    val metrics by metricsFlow.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(true) }
    
    // Group metrics by phase
    val phaseGroups = remember(metrics) {
        metrics.groupBy { it.phaseName }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (expanded) 200.dp else 0.dp), // Minimum height when expanded
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = "Initialization Metrics",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Initialization Metrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            
            // Content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Total initialization time at the top
                    item {
                        val totalDuration = initializationMetrics.getTotalDurationMs()
                        // Check if main initialization phases are complete (excluding SessionPrewarming)
                        val mainPhases = initializationMetrics.phases.values
                            .filter { it.name != "SessionPrewarming" }
                        val isComplete = mainPhases.isNotEmpty() && mainPhases.all { it.endTime != null }
                        
                        // Exclude SessionPrewarming from total if it exists
                        val initDuration: Long = if (isComplete) {
                            val latestInitTime = initializationMetrics.phases.values
                                .filter { it.name != "SessionPrewarming" }
                                .mapNotNull { it.endTime }
                                .maxByOrNull { it.toEpochMilli() }
                            
                            latestInitTime?.let { 
                                it.toEpochMilli() - initializationMetrics.startTime.toEpochMilli() 
                            } ?: totalDuration
                        } else {
                            totalDuration
                        }
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total Initialization Time",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isComplete) {
                                    Text(
                                        InitializationMetrics.formatDuration(initDuration),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Initializing...",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    phaseGroups
                        .filter { (phaseName, _) -> phaseName != "SessionPrewarming" }
                        .forEach { (phaseName, phaseMetrics) ->
                            item {
                                PhaseCard(phaseName, phaseMetrics)
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun PhaseCard(
    phaseName: String,
    phaseMetrics: List<InitializationMetrics.MetricUpdate>
) {
    val phaseEnd = phaseMetrics.find { it.type == InitializationMetrics.UpdateType.PHASE_END }
    val isComplete = phaseEnd != null
    val phaseDuration = phaseEnd?.durationMs
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Phase header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isComplete) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Complete",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(
                        phaseName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                phaseDuration?.let {
                    Text(
                        InitializationMetrics.formatDuration(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Subphases
            val subphases = phaseMetrics
                .filter { it.type == InitializationMetrics.UpdateType.SUBPHASE_END }
                .sortedBy { it.timestamp }
            
            // Get active subphases (started but not ended)
            val activeSubphases = phaseMetrics
                .filter { it.type == InitializationMetrics.UpdateType.SUBPHASE_START }
                .filter { start ->
                    !subphases.any { end -> end.subPhaseName == start.subPhaseName }
                }
                
            if (subphases.isNotEmpty() || activeSubphases.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                
                // Show completed subphases
                subphases.forEach { subphase ->
                    SubPhaseRow(subphase)
                }
                
                // Show active/loading subphases
                activeSubphases.forEach { activeSubphase ->
                    ActiveSubPhaseRow(activeSubphase)
                }
                
                // Show overhead if significant
                phaseDuration?.let { totalDuration ->
                    val subPhasesTotal = subphases.sumOf { it.durationMs ?: 0 }
                    val overhead = totalDuration - subPhasesTotal
                    if (overhead > 10) { // Only show if overhead is > 10ms
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 28.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "• [Overhead/Other]",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                            Text(
                                InitializationMetrics.formatDuration(overhead),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubPhaseRow(
    metric: InitializationMetrics.MetricUpdate
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "• ${metric.subPhaseName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            metric.details?.let { details ->
                Text(
                    "($details)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
        metric.durationMs?.let {
            Text(
                InitializationMetrics.formatDuration(it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActiveSubPhaseRow(
    metric: InitializationMetrics.MetricUpdate
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "• ${metric.subPhaseName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            metric.details?.let { details ->
                Text(
                    "($details)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp
            )
            Text(
                "loading",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}