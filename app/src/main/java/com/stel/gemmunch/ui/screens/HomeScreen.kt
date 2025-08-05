package com.stel.gemmunch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.LaunchedEffect
import com.stel.gemmunch.ui.LocalSessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val sessionManager = LocalSessionManager.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            // Header
            Text(
                "Choose Your Analysis Mode",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Mode Cards
            ModeCard(
                title = "Quick Snap",
                description = "Instant nutrition facts from a photo",
                estimatedTime = "~5 seconds",
                icon = Icons.Outlined.CameraAlt,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = {
                    coroutineScope.launch {
                        sessionManager.prewarmForDestination("camera/singleshot")
                    }
                    navController.navigate("camera/singleshot")
                }
            )
            
            ModeCard(
                title = "Analyze & Discuss",
                description = "Chat about your meal with photo analysis",
                estimatedTime = "Conversational",
                icon = Icons.Outlined.QuestionAnswer,
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                onClick = {
                    coroutineScope.launch {
                        sessionManager.prewarmForDestination("chat/multimodal")
                    }
                    navController.navigate("chat/true")
                }
            )
            
            ModeCard(
                title = "Describe Your Meal",
                description = "Text-based nutrition tracking",
                estimatedTime = "Conversational",
                icon = Icons.Outlined.TextFields,
                backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                onClick = {
                    coroutineScope.launch {
                        sessionManager.prewarmForDestination("chat/text")
                    }
                    navController.navigate("chat/false")
                }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Tips Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Tip: Use Quick Snap for fast logging, or chat modes for detailed analysis",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeCard(
    title: String,
    description: String,
    estimatedTime: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    estimatedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}