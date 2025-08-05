package com.stel.gemmunch.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.stel.gemmunch.agent.AnalyzedFoodItem
import com.stel.gemmunch.agent.MealAnalysis
import com.stel.gemmunch.agent.MultiDownloadState
import com.stel.gemmunch.model.AppMode
import com.stel.gemmunch.utils.VisionModelPreferencesManager
import com.stel.gemmunch.utils.MediaQualityPreferencesManager
import com.stel.gemmunch.utils.ImageReasoningPreferencesManager
import com.stel.gemmunch.data.models.*
import com.stel.gemmunch.ui.components.ManualNutritionEntry
import com.stel.gemmunch.ui.components.AnalysisProgressDisplay
import com.stel.gemmunch.data.models.AnalysisProgress
import com.stel.gemmunch.data.models.AnalysisStep
import com.stel.gemmunch.ModelStatus
import com.stel.gemmunch.data.InitializationMetrics
import com.stel.gemmunch.data.models.AccelerationStats
import kotlinx.coroutines.flow.StateFlow
import java.time.ZoneId
import java.time.Instant
import java.time.LocalDateTime
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import android.Manifest
import android.util.Log
import android.widget.Toast

private const val TAG = "CameraFoodCaptureScreen"

// Data class to track Health Connect selections
data class HealthConnectItemSelection(
    val itemId: String, // Unique identifier for the item
    val itemType: String, // "ai" or "manual"
    val itemIndex: Int, // Index in the original list
    val foodName: String,
    val calories: Int,
    val isSelected: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CameraFoodCaptureScreen(
    foodViewModel: FoodCaptureViewModel,
    mainViewModel: MainViewModel,
    navController: NavController,
    isAiReady: Boolean,
    initializationProgress: String?,
    onRequestHealthConnectPermissions: () -> Unit
) {
    // Pre-warm session when screen is displayed
    LaunchedEffect(isAiReady) {
        if (isAiReady) {
            mainViewModel.appContainer.prewarmSessionOnDemand()
        }
    }
    val uiState by foodViewModel.uiState.collectAsStateWithLifecycle()
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val modelStatus by mainViewModel.appContainer.modelStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // Feedback state for each item
    var itemFeedbacks by remember { mutableStateOf<Map<Int, ItemFeedback>>(emptyMap()) }
    
    // Meal timing state (shared across all items)
    var mealTiming by remember { mutableStateOf(MealTiming()) }
    
    // Health Connect state
    var writeToHealthConnect by remember { mutableStateOf(false) }
    var healthConnectItemSelections by remember { mutableStateOf<List<HealthConnectItemSelection>>(emptyList()) }
    
    // AI Details Dialog state
    var showTransitionDialog by remember { mutableStateOf(false) }
    
    // Settings Dialog state
    
    // Use ViewModel to track photo metadata (persists across navigation)
    var isFromGallery by remember { mutableStateOf(foodViewModel.isFromGallery) }
    var photoUniqueId by remember { mutableStateOf(foodViewModel.photoUniqueId) }
    var photoTimestamp by remember { mutableStateOf(foodViewModel.photoTimestamp) }
    
    // Log loaded values from ViewModel
    LaunchedEffect(Unit) {
        Log.d(TAG, "Loaded from ViewModel - photoUniqueId: ${foodViewModel.photoUniqueId}, isFromGallery: ${foodViewModel.isFromGallery}")
    }
    
    // Camera permission state
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var shouldNavigateToCamera by remember { mutableStateOf(false) }
    
    // Navigate to camera when permission is granted after request
    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted && shouldNavigateToCamera) {
            shouldNavigateToCamera = false
            isFromGallery = false
            photoUniqueId = "photo_${System.currentTimeMillis()}_camera"
            photoTimestamp = Instant.now()
            // Update ViewModel
            foodViewModel.isFromGallery = false
            foodViewModel.photoUniqueId = photoUniqueId
            foodViewModel.photoTimestamp = photoTimestamp
            Log.d(TAG, "Setting photoUniqueId for camera: $photoUniqueId")
            navController.navigate("cameraPreview")
        }
    }

    val modelFiles = remember(mainUiState.downloadState) {
        (mainUiState.downloadState as? MultiDownloadState.AllComplete)?.files ?: emptyMap()
    }
    
    // Initialize feedback for new items
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is FoodCaptureState.Success) {
            // Initialize feedback for any new items
            state.editableItems.forEachIndexed { index, _ ->
                if (!itemFeedbacks.containsKey(index)) {
                    itemFeedbacks = itemFeedbacks + (index to ItemFeedback(itemIndex = index))
                }
            }
            // Remove feedback for deleted items
            itemFeedbacks = itemFeedbacks.filterKeys { it < state.editableItems.size }
        }
    }

    LaunchedEffect(uiState) {
        val currentState = uiState
        if (currentState is FoodCaptureState.Success && currentState.lastDeletedItem != null) {
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "${currentState.lastDeletedItem.second.foodName} removed.",
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    foodViewModel.undoDeleteItem()
                }
            }
        }
        
        // Check for empty result in SNAP_AND_LOG mode
        if (currentState is FoodCaptureState.Success && 
            currentState.originalAnalysis.isEmptyResult && 
            currentState.appMode == AppMode.SNAP_AND_LOG) {
            showTransitionDialog = true
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { 
                isFromGallery = true
                // Generate unique ID based on URI hash (consistent for same photo)
                photoUniqueId = "gallery_photo_${uri.hashCode()}"
                photoTimestamp = extractExifTimestamp(context, uri)
                // Update ViewModel
                foodViewModel.isFromGallery = true
                foodViewModel.photoUniqueId = photoUniqueId
                foodViewModel.photoTimestamp = photoTimestamp
                Log.d(TAG, "Setting photoUniqueId for gallery: $photoUniqueId (uri: $uri, hash: ${uri.hashCode()}), photoTimestamp: $photoTimestamp")
                navController.navigate("imageCrop/${Uri.encode(it.toString())}") 
            }
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Show initialization banner if AI is not ready
            if (!isAiReady && initializationProgress != null) {
                item {
                    Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = initializationProgress,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "This may take up to a minute on first launch",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                }
            }
            
            // Show Health Connect banner if available but not connected
            if (mainUiState.healthConnectAvailable && !mainUiState.healthConnectPermissionsGranted) {
                item {
                    HealthConnectBanner(
                        isAvailable = mainUiState.healthConnectAvailable,
                        hasPermissions = mainUiState.healthConnectPermissionsGranted,
                        onRequestPermissions = onRequestHealthConnectPermissions
                    )
                }
            }
            
            
            when (val state = uiState) {
                is FoodCaptureState.Idle -> {
                    
                    // Camera and Gallery buttons at the top
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { 
                                        if (cameraPermissionState.status.isGranted) {
                                            isFromGallery = false
                                            photoUniqueId = "photo_${System.currentTimeMillis()}_camera"
                                            photoTimestamp = Instant.now()
                                            // Update ViewModel
                                            foodViewModel.isFromGallery = false
                                            foodViewModel.photoUniqueId = photoUniqueId
                                            foodViewModel.photoTimestamp = photoTimestamp
                                            Log.d(TAG, "Setting photoUniqueId for camera button: $photoUniqueId")
                                            navController.navigate("cameraPreview")
                                        } else {
                                            shouldNavigateToCamera = true
                                            cameraPermissionState.launchPermissionRequest()
                                        }
                                    },
                                    enabled = isAiReady,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CameraAlt, null)
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text("Take Photo")
                                }
                                OutlinedButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    enabled = isAiReady,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, null)
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text("From Gallery")
                                }
                            }
                            
                            // Show initialization message if AI is not ready
                            if (!isAiReady) {
                                Text(
                                    text = "Initializing AI... Please wait",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    
                    // App Mode Selection
                    item {
                        val currentAppMode by foodViewModel.currentAppMode.collectAsStateWithLifecycle()
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Choose Analysis Mode",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = currentAppMode == AppMode.SNAP_AND_LOG,
                                    onClick = { foodViewModel.setAppMode(AppMode.SNAP_AND_LOG) },
                                    label = { Text("Snap & Log", style = MaterialTheme.typography.bodySmall) },
                                    leadingIcon = if (currentAppMode == AppMode.SNAP_AND_LOG) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                FilterChip(
                                    selected = currentAppMode == AppMode.ANALYZE_AND_CHAT,
                                    onClick = { foodViewModel.setAppMode(AppMode.ANALYZE_AND_CHAT) },
                                    label = { Text("Analyze & Chat", style = MaterialTheme.typography.bodySmall) },
                                    leadingIcon = if (currentAppMode == AppMode.ANALYZE_AND_CHAT) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                FilterChip(
                                    selected = currentAppMode == AppMode.TEXT_ONLY,
                                    onClick = { foodViewModel.setAppMode(AppMode.TEXT_ONLY) },
                                    label = { Text("Text Only", style = MaterialTheme.typography.bodySmall) },
                                    leadingIcon = if (currentAppMode == AppMode.TEXT_ONLY) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            // Mode description
                            val modeDescription = when (currentAppMode) {
                                AppMode.SNAP_AND_LOG -> "Quick photo analysis with instant nutrition info"
                                AppMode.ANALYZE_AND_CHAT -> "Discuss ingredients and get detailed analysis"
                                AppMode.TEXT_ONLY -> "Describe your meal without a photo"
                            }
                            
                            Text(
                                text = modeDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                            )
                        }
                    }
                    
                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                    
                    // Show Health Connect banner if permissions not granted
                    if (mainUiState.healthConnectAvailable && !mainUiState.healthConnectPermissionsGranted) {
                        item {
                            HealthConnectBanner(
                                isAvailable = mainUiState.healthConnectAvailable,
                                hasPermissions = mainUiState.healthConnectPermissionsGranted,
                                onRequestPermissions = {
                                    // TODO: Implement permission request
                                    Log.d(TAG, "Health Connect permission request triggered")
                                }
                            )
                        }
                    }
                    
                    // Show permission rationale if needed
                    if (cameraPermissionState.status.shouldShowRationale) {
                        item {
                            Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "Camera permission is required to take food photos for analysis.",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        }
                    }
                }
                is FoodCaptureState.Loading -> {
                    item {
                        val selectedModel = VisionModelPreferencesManager.getSelectedVisionModel()
                        val modelDisplayName = VisionModelPreferencesManager.getVisionModelDisplayName(selectedModel)
                        val reasoningMode = ImageReasoningPreferencesManager.getSelectedMode()
                        
                        AnalysisProgressDisplay(
                            progress = state.progress,
                            modelName = modelDisplayName,
                            reasoningMode = reasoningMode.displayName,
                            imageSize = "512x512", // MediaPipe resizes to 512x512
                            contextLength = 0, // No context text anymore
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is FoodCaptureState.SwitchingModel -> {
                    item {
                        CircularProgressIndicator()
                    }
                    item {
                        Text("Switching AI model...")
                    }
                }
                is FoodCaptureState.Success -> {
                    // YOLO Mode indicator - show if high resolution was processed
                    val isYoloMode = state.analyzedBitmap?.let { bitmap ->
                        bitmap.width > 768 || bitmap.height > 768
                    } ?: false
                    
                    if (isYoloMode) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚡",
                                        style = MaterialTheme.typography.headlineMedium,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "YOLO Mode Complete!",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Analyzed ${state.analyzedBitmap?.width}x${state.analyzedBitmap?.height} image in ~19s",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Check if this is an error analysis
                    if (state.originalAnalysis.isError) {
                        // Show error card with details
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = "Error",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Analysis Failed",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Text(
                                        state.originalAnalysis.errorMessage ?: "Failed to analyze the image",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    
                                    // Show error type for debugging
                                    state.originalAnalysis.errorType?.let { errorType ->
                                        Text(
                                            "Error type: $errorType",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    
                                    // Show truncated raw response if available
                                    state.originalAnalysis.rawAiResponse?.let { raw ->
                                        Text(
                                            "AI Response (truncated):",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            raw.take(200) + if (raw.length > 200) "..." else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Still show performance metrics if available
                        state.originalAnalysis.performanceMetrics?.let { metrics ->
                            item {
                                PerformanceMetricsCard(metrics)
                            }
                        }
                        
                        // Allow user to provide feedback about the error
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Help us improve by providing feedback about this error",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        OutlinedButton(onClick = { 
                                            foodViewModel.reset()
                                            navController.navigate("home") {
                                                popUpTo("home") { inclusive = true }
                                            }
                                        }) { 
                                            Text("Try Again") 
                                        }
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    // Save error feedback
                                                    val feedbackService = mainViewModel.appContainer.feedbackStorageService
                                                    val errorFeedback = buildErrorFeedbackDocument(
                                                        mealAnalysis = state.originalAnalysis,
                                                        mealTiming = mealTiming,
                                                        isFromGallery = isFromGallery,
                                                        photoUniqueId = photoUniqueId,
                                                        photoTimestamp = photoTimestamp,
                                                        analyzedBitmap = state.analyzedBitmap
                                                    )
                                                    val documentId = feedbackService.storeFeedback(errorFeedback)
                                                    if (documentId != null) {
                                                        Log.i("FeedbackScreen", "Stored error feedback: ${state.originalAnalysis.errorType}")
                                                        snackbarHostState.showSnackbar("Error feedback saved. Thank you!")
                                                    }
                                                    foodViewModel.reset()
                                                }
                                            }
                                        ) {
                                            Text("Save Error Feedback")
                                        }
                                    }
                                }
                            }
                        }
                        
                    } else if (state.editableItems.isEmpty()) {
                        item {
                            Text("Could not identify food in the photo.", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        }
                        item {
                            Button(onClick = { 
                                foodViewModel.reset()
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }) { Text("Try Again") }
                        }
                    } else {
                        // Performance metrics card
                        state.originalAnalysis.performanceMetrics?.let { metrics ->
                            item {
                                PerformanceMetricsCard(metrics)
                            }
                        }
                        
                        // Model name and header
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(state.originalAnalysis.modelName, style = MaterialTheme.typography.headlineSmall)
                                Text("Identified Items:", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        
                        // Food items with inline feedback
                        itemsIndexed(state.editableItems, key = { _, item -> item.hashCode() }) { index, item ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                EditableFoodItem(
                                    item = item,
                                    onItemChanged = { updatedItem -> foodViewModel.updateItem(index, updatedItem) },
                                    onItemDeleted = { 
                                        foodViewModel.deleteItem(index)
                                        // Also remove the feedback for this item
                                        itemFeedbacks = itemFeedbacks - index
                                    }
                                )
                                
                                // Inline feedback card for this item
                                InlineFeedbackCard(
                                    item = item,
                                    itemIndex = index,
                                    feedback = itemFeedbacks[index] ?: ItemFeedback(itemIndex = index),
                                    nutritionSearchService = mainViewModel.appContainer.nutritionSearchService,
                                    onFeedbackUpdate = { updatedFeedback ->
                                        itemFeedbacks = itemFeedbacks + (index to updatedFeedback)
                                    }
                                )
                            }
                        }
                        
                        // Meal timing card (appears once, before save buttons)
                        item {
                            MealTimingCard(
                                isFromGallery = isFromGallery,
                                photoTimestamp = photoTimestamp,
                                mealTiming = mealTiming,
                                onTimingUpdate = { mealTiming = it }
                            )
                        }
                        
                        // Health Connect Section (single, consolidated)
                        if (mainUiState.healthConnectPermissionsGranted) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Header with toggle
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Health Connect",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Switch(
                                                checked = writeToHealthConnect,
                                                onCheckedChange = { checked ->
                                                    writeToHealthConnect = checked
                                                    if (checked && healthConnectItemSelections.isEmpty()) {
                                                        // Initialize selections when first enabled
                                                        val selections = mutableListOf<HealthConnectItemSelection>()
                                                        
                                                        // Add AI items
                                                        state.editableItems.forEachIndexed { index, item ->
                                                            selections.add(
                                                                HealthConnectItemSelection(
                                                                    itemId = "ai_$index",
                                                                    itemType = "ai",
                                                                    itemIndex = index,
                                                                    foodName = item.foodName,
                                                                    calories = item.calories,
                                                                    isSelected = true
                                                                )
                                                            )
                                                        }
                                                        
                                                        // Add manual items
                                                        itemFeedbacks.forEach { (feedbackIndex, feedback) ->
                                                            feedback.manualItems.forEachIndexed { manualIndex, manualItem ->
                                                                selections.add(
                                                                    HealthConnectItemSelection(
                                                                        itemId = "manual_${feedbackIndex}_$manualIndex",
                                                                        itemType = "manual",
                                                                        itemIndex = manualIndex,
                                                                        foodName = manualItem.foodName,
                                                                        calories = manualItem.calories,
                                                                        isSelected = true
                                                                    )
                                                                )
                                                            }
                                                        }
                                                        
                                                        healthConnectItemSelections = selections
                                                    }
                                                }
                                            )
                                        }
                                        
                                        if (writeToHealthConnect && healthConnectItemSelections.isNotEmpty()) {
                                            Text(
                                                "Items to include:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            
                                            // Item selection checkboxes
                                            healthConnectItemSelections.forEach { selection ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = selection.isSelected,
                                                        onCheckedChange = { checked ->
                                                            healthConnectItemSelections = healthConnectItemSelections.map {
                                                                if (it.itemId == selection.itemId) {
                                                                    it.copy(isSelected = checked)
                                                                } else it
                                                            }
                                                        }
                                                    )
                                                    Text(
                                                        "${selection.foodName} (${selection.calories} cal) - ${if (selection.itemType == "ai") "AI detected" else "User added"}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        modifier = Modifier.padding(start = 8.dp)
                                                    )
                                                }
                                            }
                                            
                                            // Total calories for selected items
                                            val selectedCalories = healthConnectItemSelections
                                                .filter { it.isSelected }
                                                .sumOf { it.calories }
                                            
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                            
                                            Text(
                                                "Total to write: $selectedCalories calories",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Total calories and buttons
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(Modifier.height(16.dp))
                                val aiCalories = state.editableItems.sumOf { it.calories }
                                val manualCalories = itemFeedbacks.values
                                    .flatMap { it.manualItems }
                                    .sumOf { it.calories }
                                val totalCalories = aiCalories + manualCalories
                                Text("Estimated Total: $totalCalories Calories", style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(16.dp))
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    OutlinedButton(onClick = { 
                                        foodViewModel.reset()
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }) { Text("Cancel") }
                                    Button(
                                        onClick = { 
                                            coroutineScope.launch {
                                                // Save feedback for all items
                                                val feedbackService = mainViewModel.appContainer.feedbackStorageService
                                                
                                                state.editableItems.forEachIndexed { index, item ->
                                                    val feedback = itemFeedbacks[index]
                                                    if (feedback != null) {
                                                        // Debug logging
                                                        Log.d(TAG, "Building feedback document with photoUniqueId: $photoUniqueId")
                                                        
                                                        // Calculate meal time first
                                                        val mealDateTime = when (mealTiming.option) {
                                                            MealTimingOption.USE_PHOTO_TIME -> {
                                                                when {
                                                                    !isFromGallery -> Instant.now()
                                                                    photoTimestamp != null -> photoTimestamp
                                                                    else -> Instant.now()
                                                                }
                                                            }
                                                            MealTimingOption.CUSTOM_TIME -> {
                                                                mealTiming.customDateTime?.atZone(ZoneId.systemDefault())?.toInstant() ?: Instant.now()
                                                            }
                                                        }
                                                        
                                                        // Convert to full feedback document
                                                        val feedbackDoc = buildFeedbackDocument(
                                                            mealAnalysis = state.originalAnalysis,
                                                            item = item,
                                                            itemFeedback = feedback,
                                                            totalItems = state.editableItems.size,
                                                            mealTiming = mealTiming,
                                                            isFromGallery = isFromGallery,
                                                            photoUniqueId = photoUniqueId,
                                                            photoTimestamp = photoTimestamp,
                                                            analyzedBitmap = state.analyzedBitmap,
                                                            writeToHealthConnect = writeToHealthConnect,
                                                            wasWrittenToHealthConnect = false // Will be updated after HC write
                                                        )
                                                        val documentId = feedbackService.storeFeedback(feedbackDoc)
                                                        if (documentId != null) {
                                                            Log.i("FeedbackScreen", "Stored feedback for ${item.foodName}: score=${feedback.overallScore}")
                                                            
                                                            // Note: Health Connect write is now handled after all items are saved
                                                        }
                                                    }
                                                }
                                                
                                                // Write to Health Connect if enabled (consolidated single entry)
                                                if (writeToHealthConnect && healthConnectItemSelections.any { it.isSelected }) {
                                                    val healthConnect = mainViewModel.appContainer.healthConnectManager
                                                    if (healthConnect.isHealthConnectAvailable() && mainUiState.healthConnectPermissionsGranted) {
                                                        val selectedItems = mutableListOf<AnalyzedFoodItem>()
                                                        
                                                        // Collect selected AI items with user corrections applied
                                                        healthConnectItemSelections
                                                            .filter { it.isSelected && it.itemType == "ai" }
                                                            .forEach { selection ->
                                                                val item = state.editableItems[selection.itemIndex]
                                                                val feedback = itemFeedbacks[selection.itemIndex]
                                                                
                                                                // Apply user corrections if provided
                                                                val correctedItem = if (feedback != null && 
                                                                    feedback.providingCorrections && 
                                                                    feedback.correctedValues.isNotEmpty()) {
                                                                    item.copy(
                                                                        calories = feedback.correctedValues["Calories"]?.toIntOrNull() ?: item.calories,
                                                                        protein = feedback.correctedValues["Protein"]?.toDoubleOrNull() ?: item.protein,
                                                                        totalFat = feedback.correctedValues["Total Fat"]?.toDoubleOrNull() ?: item.totalFat,
                                                                        totalCarbs = feedback.correctedValues["Carbohydrates"]?.toDoubleOrNull() ?: item.totalCarbs,
                                                                        sodium = feedback.correctedValues["Sodium"]?.toDoubleOrNull() ?: item.sodium
                                                                    )
                                                                } else {
                                                                    item
                                                                }
                                                                selectedItems.add(correctedItem)
                                                            }
                                                        
                                                        // Collect selected manual items
                                                        itemFeedbacks.forEach { (feedbackIndex, feedback) ->
                                                            feedback.manualItems.forEachIndexed { manualIndex, manualItem ->
                                                                val itemId = "manual_${feedbackIndex}_$manualIndex"
                                                                if (healthConnectItemSelections.any { it.itemId == itemId && it.isSelected }) {
                                                                    selectedItems.add(manualItem)
                                                                }
                                                            }
                                                        }
                                                        
                                                        // Combine all selected items into a single entry
                                                        if (selectedItems.isNotEmpty()) {
                                                            val itemsToWrite = if (selectedItems.size > 1) {
                                                                val nutritionSearchService = mainViewModel.appContainer.nutritionSearchService
                                                                listOf(nutritionSearchService.combineNutritionData(selectedItems))
                                                            } else {
                                                                selectedItems
                                                            }
                                                            
                                                            val mealDateTime = when (mealTiming.option) {
                                                                MealTimingOption.USE_PHOTO_TIME -> {
                                                                    when {
                                                                        !isFromGallery -> Instant.now()
                                                                        photoTimestamp != null -> photoTimestamp
                                                                        else -> Instant.now()
                                                                    }
                                                                }
                                                                MealTimingOption.CUSTOM_TIME -> {
                                                                    mealTiming.customDateTime?.atZone(ZoneId.systemDefault())?.toInstant() ?: Instant.now()
                                                                }
                                                            }
                                                            
                                                            val success = healthConnect.writeNutritionRecords(
                                                                items = itemsToWrite,
                                                                mealDateTime = mealDateTime ?: Instant.now()
                                                            )
                                                            
                                                            if (success) {
                                                                Log.i("FeedbackScreen", "Successfully wrote combined meal to Health Connect")
                                                            } else {
                                                                Log.e("FeedbackScreen", "Failed to write to Health Connect")
                                                            }
                                                        }
                                                    }
                                                }
                                                
                                                // Calculate total calories including manual items
                                                val aiCalories = state.editableItems.sumOf { it.calories }
                                                val manualCalories = itemFeedbacks.values
                                                    .flatMap { it.manualItems }
                                                    .sumOf { it.calories }
                                                val totalCalories = aiCalories + manualCalories
                                                
                                                // Show confirmation with total calories
                                                Toast.makeText(
                                                    context, 
                                                    "Meal with $totalCalories Calories saved!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                
                                                // Reset the view model and navigate to home
                                                foodViewModel.reset()
                                                navController.navigate("home") {
                                                    popUpTo("home") { inclusive = true }
                                                }
                                            }
                                        }, 
                                        enabled = state.editableItems.isNotEmpty()
                                    ) { 
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Save to Records") 
                                    }
                                }
                            }
                        }
                    }
                }
                is FoodCaptureState.Error -> {
                    item {
                        Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                    item {
                        Button(onClick = { 
                            foodViewModel.reset()
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }) { Text("Try Again") }
                    }
                }
            }
        }
    }
    
    // Transition Dialog for empty results
    if (showTransitionDialog) {
        AlertDialog(
            onDismissRequest = { showTransitionDialog = false },
            title = { Text("Need More Details") },
            text = { 
                Text("I'm having trouble identifying the food in this image. Would you like to discuss the ingredients with me?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTransitionDialog = false
                        // Switch to ANALYZE_AND_CHAT mode
                        foodViewModel.setAppMode(AppMode.ANALYZE_AND_CHAT)
                        // The current analysis with the image is already loaded
                    }
                ) {
                    Text("Yes, let's chat")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showTransitionDialog = false
                        foodViewModel.reset()
                    }
                ) {
                    Text("No, try again")
                }
            }
        )
    }
}
// Helper function to build feedback document
private fun buildFeedbackDocument(
    mealAnalysis: MealAnalysis,
    item: AnalyzedFoodItem,
    itemFeedback: ItemFeedback,
    totalItems: Int,
    mealTiming: MealTiming,
    isFromGallery: Boolean,
    photoUniqueId: String?,
    photoTimestamp: Instant?,
    analyzedBitmap: Bitmap?,
    writeToHealthConnect: Boolean = false,
    wasWrittenToHealthConnect: Boolean = false
): MealAnalysisFeedback {
    // Determine meal time and source
    val (mealDateTime, mealDateTimeSource) = when (mealTiming.option) {
        MealTimingOption.USE_PHOTO_TIME -> {
            when {
                !isFromGallery -> Instant.now() to "photo_capture_time"
                photoTimestamp != null -> photoTimestamp to "photo_metadata"
                else -> Instant.now() to "error_loading_time"
            }
        }
        MealTimingOption.CUSTOM_TIME -> {
            val customInstant = mealTiming.customDateTime?.atZone(ZoneId.systemDefault())?.toInstant() ?: Instant.now()
            customInstant to "user_provided"
        }
    }
    return MealAnalysisFeedback(
        insightGeneratedDate = mealAnalysis.generatedAt,
        mealDateTime = mealDateTime,
        mealDateTimeSource = mealDateTimeSource,
        mealDateTimeZone = ZoneId.systemDefault().toString(),
        modelDetails = ModelDetails(
            modelName = mealAnalysis.modelName,
            mediaQualitySize = MediaQualityPreferencesManager.getSelectedQuality().displayName,
            imageAnalysisMode = ImageReasoningPreferencesManager.getSelectedMode().displayName,
            promptText = getPromptTextForMode()
        ),
        performanceMetrics = com.stel.gemmunch.data.models.PerformanceMetrics(
            totalAnalysisTime = mealAnalysis.performanceMetrics?.totalTime ?: 0,
            sessionCreationTime = mealAnalysis.performanceMetrics?.sessionCreation ?: 0,
            textPromptAddTime = mealAnalysis.performanceMetrics?.textPromptAdd ?: 0,
            imageAddTime = mealAnalysis.performanceMetrics?.imageAdd ?: 0,
            llmInferenceTime = mealAnalysis.performanceMetrics?.llmInference ?: 0,
            jsonParsingTime = mealAnalysis.performanceMetrics?.jsonParsing ?: 0,
            nutrientLookupTime = mealAnalysis.performanceMetrics?.nutrientLookup ?: 0
        ),
        modelReturnStatus = if (totalItems > 0) ModelReturnStatus.SUCCESS else ModelReturnStatus.FAILED_NO_ITEMS_IDENTIFIED,
        aiResponseRaw = mealAnalysis.rawAiResponse ?: "No raw response available",
        aiResponsePerItem = listOf(
            FoodItemAnalysis(
                foodName = item.foodName,
                quantity = item.quantity,
                unit = item.unit,
                nutritionalInfo = NutritionalInfo(
                    calories = item.calories,
                    caloriesDV = calculateDailyValue(item.calories.toDouble(), 2000.0),
                    protein = item.protein,
                    proteinDV = item.protein?.let { calculateDailyValue(it, 50.0) },
                    totalFat = item.totalFat,
                    totalFatDV = item.totalFat?.let { calculateDailyValue(it, 78.0) },
                    saturatedFat = item.saturatedFat,
                    saturatedFatDV = item.saturatedFat?.let { calculateDailyValue(it, 20.0) },
                    cholesterol = item.cholesterol,
                    cholesterolDV = item.cholesterol?.let { calculateDailyValue(it, 300.0) },
                    sodium = item.sodium,
                    sodiumDV = item.sodium?.let { calculateDailyValue(it, 2300.0) },
                    totalCarbs = item.totalCarbs,
                    totalCarbsDV = item.totalCarbs?.let { calculateDailyValue(it, 275.0) },
                    dietaryFiber = item.dietaryFiber,
                    dietaryFiberDV = item.dietaryFiber?.let { calculateDailyValue(it, 28.0) },
                    sugars = item.sugars,
                    glycemicIndex = item.glycemicIndex,
                    glycemicLoad = item.glycemicLoad
                )
            )
        ),
        aiResponseTotal = null, // Single item feedback
        humanScore = itemFeedback.overallScore,
        humanReportedErrors = itemFeedback.selectedErrors.toList(),
        humanErrorNotes = itemFeedback.errorDetails.values.joinToString("\n").takeIf { it.isNotBlank() },
        restaurantMealDetails = if (itemFeedback.restaurantOrMfgName.isNotBlank() || itemFeedback.mealDescription.isNotBlank()) {
            RestaurantMealInfo(
                isRestaurant = itemFeedback.restaurantOrMfgName.isNotBlank(),
                restaurantName = itemFeedback.restaurantOrMfgName.takeIf { it.isNotBlank() },
                mealDescription = itemFeedback.mealDescription.takeIf { it.isNotBlank() }
            )
        } else null,
        humanCorrectedNutrition = if (itemFeedback.providingCorrections && itemFeedback.correctedValues.isNotEmpty()) {
            CorrectedNutritionInfo(
                nutritionalValuePerItem = itemFeedback.correctedValues.mapValues { it.value.toDoubleOrNull() ?: 0.0 },
                informationSource = itemFeedback.nutritionSource.takeIf { it.isNotBlank() }
            )
        } else null,
        manuallyAddedItems = itemFeedback.manualItems.map { manualItem ->
            ManualFoodItem(
                foodName = manualItem.foodName,
                quantity = manualItem.quantity,
                unit = manualItem.unit,
                source = "user_search",
                nutritionalInfo = NutritionalInfo(
                    calories = manualItem.calories,
                    caloriesDV = calculateDailyValue(manualItem.calories.toDouble(), 2000.0),
                    protein = manualItem.protein,
                    proteinDV = manualItem.protein?.let { calculateDailyValue(it, 50.0) },
                    totalFat = manualItem.totalFat,
                    totalFatDV = manualItem.totalFat?.let { calculateDailyValue(it, 78.0) },
                    saturatedFat = manualItem.saturatedFat,
                    saturatedFatDV = manualItem.saturatedFat?.let { calculateDailyValue(it, 20.0) },
                    cholesterol = manualItem.cholesterol,
                    cholesterolDV = manualItem.cholesterol?.let { calculateDailyValue(it, 300.0) },
                    sodium = manualItem.sodium,
                    sodiumDV = manualItem.sodium?.let { calculateDailyValue(it, 2300.0) },
                    totalCarbs = manualItem.totalCarbs,
                    totalCarbsDV = manualItem.totalCarbs?.let { calculateDailyValue(it, 275.0) },
                    dietaryFiber = manualItem.dietaryFiber,
                    dietaryFiberDV = manualItem.dietaryFiber?.let { calculateDailyValue(it, 28.0) },
                    sugars = manualItem.sugars,
                    glycemicIndex = manualItem.glycemicIndex,
                    glycemicLoad = manualItem.glycemicLoad
                )
            )
        }.takeIf { it.isNotEmpty() },
        healthConnectWriteIntention = if (writeToHealthConnect) HealthConnectWriteChoice.WRITE_COMPUTED_VALUES else HealthConnectWriteChoice.DO_NOT_WRITE,
        healthConnectDataSources = null, // Now tracked at meal level, not item level
        wasWrittenToHealthConnect = wasWrittenToHealthConnect,
        imageMetadata = ImageMetadata(
            originalImagePath = null, // Not available in current implementation
            imageWidth = analyzedBitmap?.width ?: 0,
            imageHeight = analyzedBitmap?.height ?: 0,
            imageSizeBytes = analyzedBitmap?.allocationByteCount?.toLong(),
            wasCropped = false, // Not tracked in current implementation
            cropCoordinates = null,
            exifDateTime = photoTimestamp,
            imageConfig = analyzedBitmap?.config?.name ?: "UNKNOWN",
            hasAlpha = analyzedBitmap?.hasAlpha() ?: false
        ),
        photoUniqueId = photoUniqueId
    )
}

// Helper function to get prompt text based on reasoning mode
private fun getPromptTextForMode(): String {
    return when (ImageReasoningPreferencesManager.getSelectedMode()) {
        ImageReasoningPreferencesManager.ImageReasoningMode.REASONING -> 
            """Analyze the food in this image step by step. Show your reasoning, then provide the JSON.

Step 1: Describe the main visual elements:
- Shape and structure of the food
- Colors you observe
- Any visible ingredients or toppings
- How the food is arranged or presented

Step 2: Based on your observations, identify the food items.
Think carefully:
- Tacos have folded tortillas with exposed fillings on top
- Burritos are fully wrapped cylinders
- Burgers have round buns with patties between them

Step 3: Provide your final answer as a JSON array at the end.

Example response:
"I see three folded corn tortillas arranged in a standing V-shape. The tortillas appear crispy and golden. Visible fillings include ground meat (appears to be seasoned beef), shredded lettuce, diced tomatoes, and shredded cheese on top. The arrangement and exposed fillings clearly indicate these are hard-shell tacos.

Final answer:
[{"food": "taco", "quantity": 3, "unit": "item", "confidence": 0.9}]"

Your analysis:"""
        ImageReasoningPreferencesManager.ImageReasoningMode.SINGLE_SHOT -> 
            """Analyze the food items in the image carefully and systematically.

Think step by step:
1. What is the main food item visible? Look for shape, color, and structure.
2. Are there any secondary items or condiments?
3. What quantity can you count or estimate?

Common food identifications:
- Tacos: folded corn/flour tortillas with fillings visible on top
- Burritos: fully wrapped cylindrical tortillas
- Burgers: round buns with patties between them
- Pizza: flat round/square base with toppings

Instructions:
- Focus on what you actually see, not what might be common combinations
- If you see folded tortillas with exposed fillings, they are likely tacos
- Count individual items when possible
- Output ONLY a JSON array with your final answer

Example outputs:
[{"food": "taco", "quantity": 3, "unit": "item", "confidence": 0.85}]
[{"food": "burger", "quantity": 1, "unit": "item", "confidence": 0.9}]

Valid units: item, piece, slice, cup, serving, tablespoon, teaspoon, grams, ounces

JSON output:"""
    }
}


// MealAnalysisResultView is no longer needed as its content is directly integrated into LazyColumn items

@Composable
fun EditableFoodItem(
    item: AnalyzedFoodItem,
    onItemChanged: (AnalyzedFoodItem) -> Unit,
    onItemDeleted: () -> Unit
) {
    val showDetails = true // Always show nutritional details
    val isMultipleItems = item.quantity > 1 && item.unit in listOf("item", "piece", "slice", "serving")
    
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            // Header with food name, quantity, and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${item.foodName.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${item.quantity.toInt()} ${item.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onItemDeleted) { 
                    Icon(Icons.Default.Clear, "Remove Item", tint = MaterialTheme.colorScheme.error) 
                }
            }
            
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            
            // Nutritional Information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Nutritional Information", style = MaterialTheme.typography.titleSmall)
                Text(
                    "(Source: USDA Database)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Show per-unit values if multiple items
            if (isMultipleItems) {
                Text(
                    "Per ${item.unit}:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                NutritionalInfoList(item, perUnit = true)
                
                Spacer(Modifier.height(12.dp))
                Text(
                    "Total (${item.quantity.toInt()} ${item.unit}s):",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
            }
            
            NutritionalInfoList(item, perUnit = false)
        }
    }
}

@Composable
fun NutritionalInfoList(
    item: AnalyzedFoodItem,
    perUnit: Boolean = false
) {
    val multiplier = if (perUnit) 1.0 / item.quantity else 1.0
    
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        // Calories first (no daily value percentage)
        NutrientRow(
            label = "Calories",
            value = (item.calories * multiplier).toInt().toString(),
            unit = "",
            dailyValue = null
        )
        
        // Fats
        item.totalFat?.let { fat ->
            NutrientRow(
                label = "Total Fat",
                value = (fat * multiplier).format(1),
                unit = "g",
                dailyValue = calculateDailyValue(fat * multiplier, 78.0) // 78g daily value
            )
        }
        item.saturatedFat?.let { satFat ->
            NutrientRow(
                label = "Saturated Fat",
                value = (satFat * multiplier).format(1),
                unit = "g",
                dailyValue = calculateDailyValue(satFat * multiplier, 20.0), // 20g daily value
                indent = true
            )
        }
        // Trans fat would go here if we had it
        
        // Cholesterol
        item.cholesterol?.let { chol ->
            NutrientRow(
                label = "Cholesterol",
                value = (chol * multiplier).format(0),
                unit = "mg",
                dailyValue = calculateDailyValue(chol * multiplier, 300.0) // 300mg daily value
            )
        }
        
        // Sodium
        item.sodium?.let { sodium ->
            NutrientRow(
                label = "Sodium",
                value = (sodium * multiplier).format(0),
                unit = "mg",
                dailyValue = calculateDailyValue(sodium * multiplier, 2300.0) // 2300mg daily value
            )
        }
        
        // Carbohydrates
        item.totalCarbs?.let { carbs ->
            NutrientRow(
                label = "Total Carbohydrate",
                value = (carbs * multiplier).format(1),
                unit = "g",
                dailyValue = calculateDailyValue(carbs * multiplier, 275.0) // 275g daily value
            )
        }
        item.dietaryFiber?.let { fiber ->
            NutrientRow(
                label = "Dietary Fiber",
                value = (fiber * multiplier).format(1),
                unit = "g",
                dailyValue = calculateDailyValue(fiber * multiplier, 28.0), // 28g daily value
                indent = true
            )
        }
        item.sugars?.let { sugars ->
            NutrientRow(
                label = "Total Sugars",
                value = (sugars * multiplier).format(1),
                unit = "g",
                dailyValue = null, // No daily value for total sugars
                indent = true
            )
        }
        
        // Protein
        item.protein?.let { protein ->
            NutrientRow(
                label = "Protein",
                value = (protein * multiplier).format(1),
                unit = "g",
                dailyValue = calculateDailyValue(protein * multiplier, 50.0) // 50g daily value
            )
        }
        
        // Glycemic Index (no daily value)
        Spacer(Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(Modifier.height(4.dp))
        
        NutrientRow(
            label = "Glycemic Index",
            value = item.glycemicIndex?.toString() ?: "Unknown",
            unit = "",
            dailyValue = null
        )
        
        item.glycemicLoad?.let { gl ->
            NutrientRow(
                label = "Glycemic Load",
                value = (gl * multiplier).format(1),
                unit = "",
                dailyValue = null
            )
        }
    }
}

@Composable
fun NutrientRow(
    label: String, 
    value: String,
    unit: String,
    dailyValue: Int? = null,
    indent: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indent) 16.dp else 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "$value$unit",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            dailyValue?.let { dv ->
                Text(
                    text = "($dv%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun calculateDailyValue(amount: Double, dailyValue: Double): Int {
    return ((amount / dailyValue) * 100).toInt()
}

private fun extractExifTimestamp(context: android.content.Context, uri: Uri): Instant? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            val exif = android.media.ExifInterface(stream)
            
            // Try to get datetime from EXIF
            val dateTimeOriginal = exif.getAttribute(android.media.ExifInterface.TAG_DATETIME_ORIGINAL)
            val dateTime = exif.getAttribute(android.media.ExifInterface.TAG_DATETIME)
            val dateTimeDigitized = exif.getAttribute(android.media.ExifInterface.TAG_DATETIME_DIGITIZED)
            
            val dateString = dateTimeOriginal ?: dateTimeDigitized ?: dateTime
            
            if (dateString != null) {
                // EXIF date format is "yyyy:MM:dd HH:mm:ss"
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
                val localDateTime = java.time.LocalDateTime.parse(dateString, formatter)
                // Convert to Instant assuming local timezone (could be improved with GPS data)
                localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant()
            } else {
                null
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to extract EXIF timestamp from URI", e)
        null
    }
}

// Extension function to format doubles
fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceMetricsCard(metrics: com.stel.gemmunch.agent.PerformanceMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("⚡ Performance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("Total: ${metrics.getFormattedTime(metrics.totalTime)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text("Session: ${metrics.getFormattedTime(metrics.sessionCreation)}, Prompt: ${metrics.getFormattedTime(metrics.textPromptAdd)}, Image: ${metrics.getFormattedTime(metrics.imageAdd)}", style = MaterialTheme.typography.bodySmall)
            Text("Inference: ${metrics.getFormattedTime(metrics.llmInference)}, Parsing: ${metrics.getFormattedTime(metrics.jsonParsing)}, Nutrition: ${metrics.getFormattedTime(metrics.nutrientLookup)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisionModelSelectionCard(
    modelFiles: Map<String, File>,
    onSwitchModel: (String, Map<String, File>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    val availableModels = VisionModelPreferencesManager.getAvailableVisionModels()
    var selectedModel by remember { mutableStateOf(VisionModelPreferencesManager.getSelectedVisionModel()) }
    val selectedDisplayName = VisionModelPreferencesManager.getVisionModelDisplayName(selectedModel)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vision Model", style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset to default",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = selectedDisplayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Dropdown") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    availableModels.forEach { (modelKey, displayName) ->
                        DropdownMenuItem(
                            text = { Text(displayName) },
                            onClick = {
                                expanded = false
                                if (modelKey != selectedModel) {
                                    selectedModel = modelKey
                                    onSwitchModel(modelKey, modelFiles)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Model Selection") },
            text = { Text("Reset to default model (Gemma 3n E4B)?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        selectedModel = "GEMMA_3N_E4B_MODEL"
                        VisionModelPreferencesManager.setSelectedVisionModel("GEMMA_3N_E4B_MODEL")
                        onSwitchModel("GEMMA_3N_E4B_MODEL", modelFiles)
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaQualitySelectionCard() {
    var expanded by remember { mutableStateOf(false) }
    var selectedQuality by remember { mutableStateOf(MediaQualityPreferencesManager.getSelectedQuality()) }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Media Quality", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = selectedQuality.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Dropdown") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    MediaQualityPreferencesManager.MediaQuality.values().forEach { quality ->
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(quality.displayName)
                                    if (quality == selectedQuality) {
                                        Icon(
                                            Icons.Default.Check, 
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            onClick = {
                                expanded = false
                                if (quality != selectedQuality) {
                                    MediaQualityPreferencesManager.setSelectedQuality(quality)
                                    selectedQuality = quality
                                }
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Higher quality may improve accuracy but increases processing time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageReasoningModeCard() {
    var expanded by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(ImageReasoningPreferencesManager.getSelectedMode()) }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Image Analysis Mode", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = selectedMode.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Dropdown") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    supportingText = { Text(selectedMode.description) }
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ImageReasoningPreferencesManager.ImageReasoningMode.values().forEach { mode ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(mode.displayName, fontWeight = FontWeight.Medium)
                                    Text(
                                        mode.description, 
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                expanded = false
                                if (mode != selectedMode) {
                                    ImageReasoningPreferencesManager.setSelectedMode(mode)
                                    selectedMode = mode
                                }
                            },
                            leadingIcon = if (mode == selectedMode) {
                                { Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) }
                            } else null
                        )
                    }
                }
            }
            if (selectedMode == ImageReasoningPreferencesManager.ImageReasoningMode.REASONING) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Shows model's reasoning process in logs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Helper function to build error feedback document
private fun buildErrorFeedbackDocument(
    mealAnalysis: MealAnalysis,
    mealTiming: MealTiming,
    isFromGallery: Boolean,
    photoUniqueId: String?,
    photoTimestamp: Instant?,
    analyzedBitmap: Bitmap?
): MealAnalysisFeedback {
    // Calculate meal time
    val mealDateTime = when (mealTiming.option) {
        MealTimingOption.USE_PHOTO_TIME -> {
            when {
                !isFromGallery -> Instant.now()
                photoTimestamp != null -> photoTimestamp
                else -> Instant.now()
            }
        }
        MealTimingOption.CUSTOM_TIME -> {
            mealTiming.customDateTime?.atZone(ZoneId.systemDefault())?.toInstant() ?: Instant.now()
        }
    }
    
    // Determine the model return status based on error type
    val modelStatus = when (mealAnalysis.errorType) {
        "MULTIPLE_JSON_ARRAYS" -> ModelReturnStatus.FAILED_MULTIPLE_JSON_ARRAYS
        "MALFORMED_JSON" -> ModelReturnStatus.FAILED_MALFORMED_JSON
        "TOKEN_LIMIT_EXCEEDED" -> ModelReturnStatus.FAILED_TOKEN_LIMIT_EXCEEDED
        "SESSION_ERROR" -> ModelReturnStatus.FAILED_SESSION_ERROR
        "INFERENCE_ERROR" -> ModelReturnStatus.FAILED_INFERENCE_ERROR
        else -> ModelReturnStatus.FAILED_NOT_JSON
    }
    
    return MealAnalysisFeedback(
        insightGeneratedDate = mealAnalysis.generatedAt,
        modelDetails = ModelDetails(
            modelName = mealAnalysis.modelName,
            mediaQualitySize = MediaQualityPreferencesManager.getSelectedQuality().displayName,
            imageAnalysisMode = ImageReasoningPreferencesManager.getSelectedMode().displayName,
            promptText = getPromptTextForMode()
        ),
        performanceMetrics = mealAnalysis.performanceMetrics?.let {
            PerformanceMetrics(
                totalAnalysisTime = it.totalTime,
                sessionCreationTime = it.sessionCreation,
                textPromptAddTime = it.textPromptAdd,
                imageAddTime = it.imageAdd,
                llmInferenceTime = it.llmInference,
                jsonParsingTime = it.jsonParsing,
                nutrientLookupTime = it.nutrientLookup
            )
        } ?: PerformanceMetrics(0, 0, 0, 0, 0, 0, 0),
        mealDateTime = mealDateTime,
        mealDateTimeSource = when {
            !isFromGallery -> "photo_capture_time"
            photoTimestamp != null -> "photo_metadata"
            mealTiming.option == MealTimingOption.CUSTOM_TIME -> "user_provided"
            else -> "error_loading_time"
        },
        mealDateTimeZone = ZoneId.systemDefault().id,
        modelReturnStatus = modelStatus,
        aiResponseRaw = mealAnalysis.rawAiResponse ?: "",
        aiResponsePerItem = emptyList(), // No items for errors
        aiResponseTotal = null,
        humanScore = 0, // Automatic 0 score for errors
        humanReportedErrors = listOf(ErrorType.FOOD_COMPLETELY_WRONG), // Default error type
        humanErrorNotes = "AI failed to parse response: ${mealAnalysis.errorMessage}",
        restaurantMealDetails = null,
        humanCorrectedNutrition = null,
        manuallyAddedItems = null, // No manual items for errors
        healthConnectWriteIntention = HealthConnectWriteChoice.DO_NOT_WRITE, // Don't write errors
        healthConnectDataSources = null, // No data sources for errors
        wasWrittenToHealthConnect = false,
        imageMetadata = ImageMetadata(
            originalImagePath = null,
            imageWidth = analyzedBitmap?.width ?: 0,
            imageHeight = analyzedBitmap?.height ?: 0,
            imageSizeBytes = analyzedBitmap?.allocationByteCount?.toLong(),
            wasCropped = false,
            cropCoordinates = null,
            exifDateTime = photoTimestamp,
            imageConfig = analyzedBitmap?.config?.name ?: "UNKNOWN",
            hasAlpha = analyzedBitmap?.hasAlpha() ?: false
        ),
        photoUniqueId = photoUniqueId
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextTextInputCard(
    contextText: String,
    onContextTextChange: (String) -> Unit
) {
    // Helper function to add text without duplicates
    fun addToContext(newItem: String) {
        val cleanText = contextText.trim().removeSuffix(",").trim()
        val newText = when {
            cleanText.isEmpty() -> newItem
            cleanText.contains(newItem, ignoreCase = true) -> cleanText // Don't add duplicate
            else -> "$cleanText, $newItem"
        }
        onContextTextChange(newText)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Additional Context (Optional)",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            
            OutlinedTextField(
                value = contextText,
                onValueChange = onContextTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { 
                    Text("e.g., \"Chipotle crispy tacos, chicken, hot salsa, cheese\"") 
                },
                supportingText = { 
                    Text(
                        text = "Describe the restaurant, meal type, or specific items. Use suggestions below to add details.",
                        style = MaterialTheme.typography.bodySmall
                    ) 
                },
                keyboardOptions = KeyboardOptions.Default,
                maxLines = 4,
                leadingIcon = {
                    Icon(
                        Icons.Default.Notes,
                        contentDescription = "Context notes",
                        tint = MaterialTheme.colorScheme.outline
                    )
                },
                trailingIcon = if (contextText.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { onContextTextChange("") }
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear context",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else null
            )
            
            // Always show suggestions - they help build detailed context
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = "Add context:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(6.dp))
            
            // Restaurant suggestions
            Text(
                text = "Restaurants:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SuggestionChip(
                    onClick = { addToContext("Chipotle") },
                    label = { Text("Chipotle", style = MaterialTheme.typography.labelSmall) }
                )
                SuggestionChip(
                    onClick = { addToContext("Mexican") },
                    label = { Text("Mexican", style = MaterialTheme.typography.labelSmall) }
                )
                SuggestionChip(
                    onClick = { addToContext("fast food") },
                    label = { Text("Fast food", style = MaterialTheme.typography.labelSmall) }
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Food items suggestions
            Text(
                text = "Common items:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SuggestionChip(
                    onClick = { addToContext("crispy tacos") },
                    label = { Text("Crispy tacos", style = MaterialTheme.typography.labelSmall) }
                )
                SuggestionChip(
                    onClick = { addToContext("chicken") },
                    label = { Text("Chicken", style = MaterialTheme.typography.labelSmall) }
                )
                SuggestionChip(
                    onClick = { addToContext("bowl") },
                    label = { Text("Bowl", style = MaterialTheme.typography.labelSmall) }
                )
            }
            
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SuggestionChip(
                    onClick = { addToContext("hot salsa") },
                    label = { Text("Hot salsa", style = MaterialTheme.typography.labelSmall) }
                )
                SuggestionChip(
                    onClick = { addToContext("cheese") },
                    label = { Text("Cheese", style = MaterialTheme.typography.labelSmall) }
                )
                SuggestionChip(
                    onClick = { addToContext("sour cream") },
                    label = { Text("Sour cream", style = MaterialTheme.typography.labelSmall) }
                )
            }
            
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SuggestionChip(
                    onClick = { addToContext("lettuce") },
                    label = { Text("Lettuce", style = MaterialTheme.typography.labelSmall) }
                )
                SuggestionChip(
                    onClick = { addToContext("rice") },
                    label = { Text("Rice", style = MaterialTheme.typography.labelSmall) }
                )
                SuggestionChip(
                    onClick = { addToContext("beans") },
                    label = { Text("Beans", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

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

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    modelFiles: Map<String, File>,
    onModelSwitch: (String, Map<String, File>) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
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
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .heightIn(min = 400.dp, max = LocalConfiguration.current.screenHeightDp.dp * 0.85f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
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
                        .padding(20.dp)
                ) {
                    // Title Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Vision Model Selection
                        VisionModelSelectionCard(
                            modelFiles = modelFiles,
                            onSwitchModel = onModelSwitch
                        )
                        
                        // Media Quality Selection
                        MediaQualitySelectionCard()
                        
                        // Image Analysis Mode
                        ImageReasoningModeCard()
                        
                        // Add bottom padding for better scrolling experience
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
