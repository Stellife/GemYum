package com.stel.gemmunch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stel.gemmunch.agent.MultiDownloadState

@Composable
fun SetupScreen(
    downloadState: MultiDownloadState,
    onDownloadClick: () -> Unit,
    onBypassSetup: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to GemMunch", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "To get started, we need to download the on-device AI models for food analysis.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("AI Model Setup", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                when (downloadState) {
                    is MultiDownloadState.Checking -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Verifying model files...")
                    }
                    is MultiDownloadState.AllComplete -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, "Success", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Models Downloaded!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "The AI models are being initialized in the background. This may take up to a minute on first launch.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "You can start using the app while this completes.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is MultiDownloadState.Failed -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Warning, 
                                contentDescription = "Failed", 
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Setup Failed", 
                                color = MaterialTheme.colorScheme.error, 
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                downloadState.reason, 
                                style = MaterialTheme.typography.bodyMedium, 
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onDownloadClick) { 
                                Text("Retry Setup") 
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onBypassSetup) {
                                Text(
                                    "Continue without AI (Advanced)",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is MultiDownloadState.InProgress -> {
                        LinearProgressIndicator(
                            progress = { downloadState.progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val sizeText = if (downloadState.totalMB > 1024) {
                            val downloadedGB = downloadState.downloadedMB / 1024.0
                            val totalGB = downloadState.totalMB / 1024.0
                            String.format("%.1f/%.1f GB", downloadedGB, totalGB)
                        } else {
                            "${downloadState.downloadedMB}/${downloadState.totalMB} MB"
                        }
                        Text("Downloading: ${downloadState.currentFileName} (${downloadState.progress}% - $sizeText)")
                    }
                    is MultiDownloadState.Idle -> {
                        Button(onClick = onDownloadClick) { Text("Download Models (≈4 GB)") }
                    }
                }
            }
        }
    }
}