package com.stel.gemmunch.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.stel.gemmunch.agent.AnalyzedFoodItem
import com.stel.gemmunch.agent.MealAnalysis
import com.stel.gemmunch.agent.MultiDownloadState
import com.stel.gemmunch.utils.VisionModelPreferencesManager
import com.stel.gemmunch.utils.MediaQualityPreferencesManager
import com.stel.gemmunch.utils.ImageReasoningPreferencesManager
import com.stel.gemmunch.data.models.*
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
    val uiState by foodViewModel.uiState.collectAsStateWithLifecycle()
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // Feedback state for each item
    var itemFeedbacks by remember { mutableStateOf<Map<Int, ItemFeedback>>(emptyMap()) }
    
    // Meal timing state (shared across all items)
    var mealTiming by remember { mutableStateOf(MealTiming()) }
    
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
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let { 
                isFromGallery = true
                // Generate unique ID for this photo session
                photoUniqueId = "photo_${System.currentTimeMillis()}_${uri.hashCode()}"
                photoTimestamp = extractExifTimestamp(context, uri)
                // Update ViewModel
                foodViewModel.isFromGallery = true
                foodViewModel.photoUniqueId = photoUniqueId
                foodViewModel.photoTimestamp = photoTimestamp
                Log.d(TAG, "Setting photoUniqueId for gallery: $photoUniqueId, photoTimestamp: $photoTimestamp")
                navController.navigate("imageCrop/${Uri.encode(it.toString())}") 
            }
        }
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("GemMunch") }) },
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
                    // Show initialization metrics at the top
                    item {
                        InitializationMetricsCard(
                            metricsFlow = mainViewModel.initMetricsUpdates
                        )
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
                    
                    item {
                        VisionModelSelectionCard(
                            modelFiles = modelFiles,
                            onSwitchModel = foodViewModel::switchVisionModel
                        )
                    }
                    
                    item {
                        MediaQualitySelectionCard()
                    }
                    
                    item {
                        ImageReasoningModeCard()
                    }
                    
                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                    
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
                            enabled = isAiReady
                        ) {
                            Icon(Icons.Default.CameraAlt, null)
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Take Photo")
                        }
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            enabled = isAiReady
                        ) {
                            Icon(Icons.Default.PhotoLibrary, null)
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("From Gallery")
                        }
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
                        CircularProgressIndicator()
                    }
                    item {
                        Text("Analyzing your meal...")
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
                                        OutlinedButton(onClick = foodViewModel::reset) { 
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
                            Button(onClick = foodViewModel::reset) { Text("Try Again") }
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
                        
                        // Total calories and buttons
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(Modifier.height(16.dp))
                                val totalCalories = state.editableItems.sumOf { it.calories }
                                Text("Estimated Total: $totalCalories Calories", style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(16.dp))
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    OutlinedButton(onClick = foodViewModel::reset) { Text("Cancel") }
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
                                                            analyzedBitmap = state.analyzedBitmap
                                                        )
                                                        val documentId = feedbackService.storeFeedback(feedbackDoc)
                                                        if (documentId != null) {
                                                            Log.i("FeedbackScreen", "Stored feedback for ${item.foodName}: score=${feedback.overallScore}")
                                                            
                                                            // Write to Health Connect if requested
                                                            if (feedback.healthConnectWriteChoice == HealthConnectWriteChoice.WRITE_COMPUTED_VALUES ||
                                                                feedback.healthConnectWriteChoice == HealthConnectWriteChoice.WRITE_USER_VALUES) {
                                                                
                                                                val healthConnect = mainViewModel.appContainer.healthConnectManager
                                                                if (healthConnect.isHealthConnectAvailable() && 
                                                                    mainUiState.healthConnectPermissionsGranted) {
                                                                    
                                                                    val itemsToWrite = if (feedback.healthConnectWriteChoice == HealthConnectWriteChoice.WRITE_USER_VALUES && 
                                                                        feedback.providingCorrections && 
                                                                        feedback.correctedValues.isNotEmpty()) {
                                                                        // Create a modified item with user-provided values
                                                                        listOf(item.copy(
                                                                            calories = feedback.correctedValues["Calories"]?.toIntOrNull() ?: item.calories,
                                                                            protein = feedback.correctedValues["Protein"]?.toDoubleOrNull() ?: item.protein,
                                                                            totalFat = feedback.correctedValues["Total Fat"]?.toDoubleOrNull() ?: item.totalFat,
                                                                            totalCarbs = feedback.correctedValues["Carbohydrates"]?.toDoubleOrNull() ?: item.totalCarbs,
                                                                            sodium = feedback.correctedValues["Sodium"]?.toDoubleOrNull() ?: item.sodium
                                                                        ))
                                                                    } else {
                                                                        listOf(item)
                                                                    }
                                                                    
                                                                    val success = healthConnect.writeNutritionRecords(
                                                                        items = itemsToWrite,
                                                                        mealDateTime = mealDateTime ?: Instant.now()
                                                                    )
                                                                    
                                                                    if (success) {
                                                                        Log.i("FeedbackScreen", "Successfully wrote ${item.foodName} to Health Connect")
                                                                    } else {
                                                                        Log.e("FeedbackScreen", "Failed to write ${item.foodName} to Health Connect")
                                                                    }
                                                                } else {
                                                                    Log.w("FeedbackScreen", "Health Connect not available or permissions not granted")
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                
                                                // Save the meal
                                                foodViewModel.saveMeal(context)
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
                        Button(onClick = foodViewModel::reset) { Text("Try Again") }
                    }
                }
            }
        }
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
    analyzedBitmap: Bitmap?
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
        healthConnectWriteIntention = itemFeedback.healthConnectWriteChoice,
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
        photoUniqueId = photoUniqueId,
        wasWrittenToHealthConnect = itemFeedback.healthConnectWriteChoice != null && 
            itemFeedback.healthConnectWriteChoice != HealthConnectWriteChoice.DO_NOT_WRITE
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
    val selectedModel = VisionModelPreferencesManager.getSelectedVisionModel()
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
        healthConnectWriteIntention = HealthConnectWriteChoice.DO_NOT_WRITE, // Don't write errors
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