package com.stel.gemmunch.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.launch
import java.io.File
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import android.Manifest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CameraFoodCaptureScreen(
    foodViewModel: FoodCaptureViewModel,
    mainViewModel: MainViewModel,
    navController: NavController,
    isAiReady: Boolean,
    initializationProgress: String?
) {
    val uiState by foodViewModel.uiState.collectAsStateWithLifecycle()
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // Camera permission state
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var shouldNavigateToCamera by remember { mutableStateOf(false) }
    
    // Navigate to camera when permission is granted after request
    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted && shouldNavigateToCamera) {
            shouldNavigateToCamera = false
            navController.navigate("cameraPreview")
        }
    }

    val modelFiles = remember(mainUiState.downloadState) {
        (mainUiState.downloadState as? MultiDownloadState.AllComplete)?.files ?: emptyMap()
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
            uri?.let { navController.navigate("imageCrop/${Uri.encode(it.toString())}") }
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
            
            when (val state = uiState) {
                is FoodCaptureState.Idle -> {
                    // Show initialization metrics at the top
                    item {
                        InitializationMetricsCard(
                            metricsFlow = mainViewModel.initMetricsUpdates
                        )
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
                    if (state.editableItems.isEmpty()) {
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
                        
                        // Food items
                        itemsIndexed(state.editableItems, key = { _, item -> item.hashCode() }) { index, item ->
                            EditableFoodItem(
                                item = item,
                                onItemChanged = { updatedItem -> foodViewModel.updateItem(index, updatedItem) },
                                onItemDeleted = { foodViewModel.deleteItem(index) }
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
                                        onClick = { foodViewModel.saveMeal(context) }, 
                                        enabled = state.editableItems.isNotEmpty()
                                    ) { 
                                        Text("Save Meal") 
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
                    modifier = Modifier.fillMaxWidth().menuAnchor()
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
                    modifier = Modifier.fillMaxWidth().menuAnchor()
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
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
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