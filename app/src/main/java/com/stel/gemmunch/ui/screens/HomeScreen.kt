package com.stel.gemmunch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.content.ComponentName
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.health.connect.client.HealthConnectClient
import com.stel.gemmunch.ui.LocalSessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    isAiReady: Boolean = true, // Default to true for backwards compatibility
    initializationProgress: String? = null
) {
    val sessionManager = LocalSessionManager.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            // Show initialization banner if AI is not ready
            if (!isAiReady && initializationProgress != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "AI Model Initializing",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = initializationProgress,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Text(
                            text = "⚠️ First-time initialization takes 30-60 seconds. Subsequent launches will be much faster.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            // Header
            Text(
                "Gemma 3n Food Analysis Types",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Mode Cards
            ModeCard(
                title = "Quick Snap: Image Analysis",
                description = "Vision-led Inference, no chat interface",
                estimatedTime = "<60 seconds analysis",
                icon = Icons.Outlined.CameraAlt,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                enabled = isAiReady,
                onClick = {
                    coroutineScope.launch {
                        sessionManager.prewarmForDestination("camera/singleshot")
                    }
                    navController.navigate("camera/singleshot")
                }
            )
            
            ModeCard(
                title = "Deep Chat: Multimodal + Token Async",
                description = "Image analysis -> nutrition record",
                estimatedTime = "🧪 Experimental",
                icon = Icons.Outlined.QuestionAnswer,
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                enabled = isAiReady,
                onClick = {
                    coroutineScope.launch {
                        sessionManager.prewarmForDestination("chat/multimodal")
                    }
                    navController.navigate("chat/true")
                }
            )
            
            ModeCard(
                title = "Text-Only Chat: Reasoning Model",
                description = "No vision inference, Function Calling",
                estimatedTime = "🧪 Experimental",
                icon = Icons.Outlined.TextFields,
                backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                enabled = isAiReady,
                onClick = {
                    coroutineScope.launch {
                        sessionManager.prewarmForDestination("chat/text")
                    }
                    navController.navigate("chat/false")
                }
            )
            
            // Spacer for visual separation
            Spacer(modifier = Modifier.height(8.dp))
            
            // Nutrient Database Lookup
            ModeCard(
                title = "Nutrient DB Debug",
                description = "Explore local + API nutrient DB",
                estimatedTime = "Debug tool",
                icon = Icons.Outlined.Search,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                onClick = {
                    navController.navigate("nutrient-db")
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
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Health Connect Button
            Button(
                onClick = {
                    try {
                        // Method 1: Try to launch Health Connect app directly via package manager
                        val healthConnectIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.healthdata")
                        if (healthConnectIntent != null) {
                            Log.d("HomeScreen", "Opening Health Connect app directly")
                            context.startActivity(healthConnectIntent)
                            return@Button
                        }
                        
                        // Method 2: Try Health Connect specific intents
                        val intent = try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                // Android 14+ (API 34+) - Use ACTION_MANAGE_HEALTH_PERMISSIONS
                                Intent("androidx.health.ACTION_MANAGE_HEALTH_PERMISSIONS").apply {
                                    putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                                }
                            } else {
                                // Android 13 and below - Use ACTION_HEALTH_CONNECT_SETTINGS
                                Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                            }
                        } catch (e: Exception) {
                            Log.w("HomeScreen", "Health Connect intents not available", e)
                            null
                        }
                        
                        if (intent != null) {
                            Log.d("HomeScreen", "Trying Health Connect settings intent")
                            context.startActivity(intent)
                            return@Button
                        }
                        
                        // Method 3: Try alternative component name approach
                        val componentIntent = Intent().apply {
                            component = ComponentName(
                                "com.google.android.apps.healthdata", 
                                "com.google.android.apps.healthdata.home.HomeActivity"
                            )
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        
                        Log.d("HomeScreen", "Trying Health Connect component intent")
                        context.startActivity(componentIntent)
                        
                    } catch (e: Exception) {
                        Log.w("HomeScreen", "All Health Connect methods failed, opening Play Store", e)
                        // Fallback: Open Health Connect in Play Store
                        try {
                            val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse("market://details?id=com.google.android.apps.healthdata")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(playStoreIntent)
                        } catch (e2: Exception) {
                            // Final fallback: Show toast message
                            Toast.makeText(
                                context, 
                                "Health Connect not available. Please install from Play Store.", 
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Icon(
                    Icons.Outlined.HealthAndSafety,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Open Health Connect",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
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
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        onClick = if (enabled) onClick else { {} },
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f)
        )
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
                modifier = Modifier.size(48.dp).alpha(if (enabled) 1f else 0.5f),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.6f)
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.6f)
                )
                Text(
                    estimatedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            if (enabled) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}