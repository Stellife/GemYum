package com.stel.gemmunch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stel.gemmunch.data.HealthConnectAvailability

/**
 * Shows Health Connect status and allows requesting permissions.
 */
@Composable
fun HealthConnectBanner(
    isAvailable: Boolean,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isAvailable) {
        // Don't show banner if Health Connect is not available
        return
    }
    
    if (hasPermissions) {
        // Already have permissions, no need to show banner
        return
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.HealthAndSafety,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Health Connect",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Grant permission to save meals to Health Connect",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            FilledTonalButton(
                onClick = onRequestPermissions
            ) {
                Text("Enable")
            }
        }
    }
}

/**
 * Shows detailed Health Connect availability status.
 */
@Composable
fun HealthConnectStatusCard(
    availabilityStatus: HealthConnectAvailability,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onInstallHealthConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (availabilityStatus) {
                HealthConnectAvailability.AVAILABLE -> {
                    if (hasPermissions) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer
                }
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = when (availabilityStatus) {
                        HealthConnectAvailability.AVAILABLE -> Icons.Default.HealthAndSafety
                        else -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = when (availabilityStatus) {
                        HealthConnectAvailability.AVAILABLE -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Health Connect",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = when (availabilityStatus) {
                            HealthConnectAvailability.AVAILABLE -> {
                                if (hasPermissions) "Connected and ready"
                                else "Permission required"
                            }
                            HealthConnectAvailability.NOT_INSTALLED -> "Not installed"
                            HealthConnectAvailability.UPDATE_REQUIRED -> "Update required"
                            HealthConnectAvailability.NOT_SUPPORTED -> "Not supported on this device"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            when (availabilityStatus) {
                HealthConnectAvailability.AVAILABLE -> {
                    if (!hasPermissions) {
                        Text(
                            "Grant permission to automatically save your meal data to Health Connect.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        
                        Button(
                            onClick = onRequestPermissions,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
                
                HealthConnectAvailability.NOT_INSTALLED -> {
                    Text(
                        "Health Connect is not installed on your device. Install it to sync nutrition data.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Button(
                        onClick = onInstallHealthConnect,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Install Health Connect")
                    }
                }
                
                HealthConnectAvailability.UPDATE_REQUIRED -> {
                    Text(
                        "Health Connect needs to be updated to work with this app.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Button(
                        onClick = onInstallHealthConnect,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Update Health Connect")
                    }
                }
                
                HealthConnectAvailability.NOT_SUPPORTED -> {
                    Text(
                        "Health Connect is not supported on this device.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}