package com.stel.gemmunch.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QuickSnapOptionsScreen(
    onTakePhoto: () -> Unit,
    onSelectFromGallery: (Uri) -> Unit,
    onYoloCamera: () -> Unit,
    onYoloGallery: (Uri) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onSelectFromGallery(it) }
    }

    // Remove Scaffold - let GemMunchAppScaffold handle navigation
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choose your Quick Snap mode",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Text(
                text = "Standard mode allows cropping and editing before analysis",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Standard Camera Option
            OptionCard(
                title = "📸 Take Photo",
                description = "Capture with camera, crop & edit before analysis",
                icon = Icons.Outlined.CameraAlt,
                onClick = {
                    if (cameraPermissionState.status.isGranted) {
                        onTakePhoto()
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                },
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Standard Gallery Option
            OptionCard(
                title = "📱 Upload Image",
                description = "Select from gallery, crop & edit before analysis",
                icon = Icons.Outlined.PhotoLibrary,
                onClick = {
                    galleryLauncher.launch("image/*")
                },
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            // YOLO Mode Section
            Text(
                text = "⚡ YOLO Mode - Instant Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "High-resolution instant analysis - no cropping, just results!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // YOLO Camera Option
            OptionCard(
                title = "⚡ YOLO Camera",
                description = "Instant analysis from camera (3072x4080 capable!)",
                icon = Icons.Outlined.CameraAlt,
                onClick = {
                    if (cameraPermissionState.status.isGranted) {
                        onYoloCamera()
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                },
                isYolo = true,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // YOLO Gallery Option
            OptionCard(
                title = "⚡ YOLO Gallery",
                description = "Instant analysis from any resolution image",
                icon = Icons.Outlined.PhotoLibrary,
                onClick = {
                    galleryLauncher.launch("image/*")
                },
                isYolo = true,
                onYoloGallery = onYoloGallery
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Both options will allow you to crop and adjust your image before analysis",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
    
    // Handle camera permission result
    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted) {
            // Permission was just granted, but don't auto-navigate
            // Let user click the button again
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isYolo: Boolean = false,
    onYoloGallery: ((Uri) -> Unit)? = null
) {
    // For YOLO Gallery, we need special handling
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            if (isYolo && onYoloGallery != null) {
                onYoloGallery(it)
            }
        }
    }
    
    val actualOnClick = if (isYolo && title.contains("Gallery") && onYoloGallery != null) {
        { galleryLauncher.launch("image/*") }
    } else {
        onClick
    }
    Card(
        onClick = actualOnClick,
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isYolo) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isYolo) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isYolo) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isYolo) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}