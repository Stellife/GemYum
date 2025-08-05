package com.stel.gemmunch.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.stel.gemmunch.GemMunchApplication
import com.stel.gemmunch.agent.MultiDownloadState
import com.stel.gemmunch.data.HealthConnectManager
import com.stel.gemmunch.ui.theme.GemMunchTheme

import com.stel.gemmunch.ui.*
import com.stel.gemmunch.viewmodels.EnhancedChatViewModel
import com.stel.gemmunch.viewmodels.EnhancedChatViewModelFactory
import com.stel.gemmunch.utils.SessionManager
import com.stel.gemmunch.utils.VisionModelPreferencesManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.stel.gemmunch.ui.screens.NutrientDBScreen
import com.stel.gemmunch.viewmodels.NutrientDBViewModel
import com.stel.gemmunch.viewmodels.NutrientDBViewModelFactory

class MainActivity : ComponentActivity() {
    
    // Health Connect permission launcher - will be initialized in onCreate
    private lateinit var healthConnectPermissionLauncher: ActivityResultLauncher<Set<String>>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as GemMunchApplication).container

        val mainViewModel: MainViewModel by viewModels {
            MainViewModelFactory(application, appContainer)
        }
        val foodCaptureViewModel: FoodCaptureViewModel by viewModels {
            FoodCaptureViewModelFactory(appContainer)
        }
        
        // Create Enhanced ChatViewModels for each mode at Activity level
        val analyzeAndChatViewModel: EnhancedChatViewModel by viewModels {
            EnhancedChatViewModelFactory(appContainer, isMultimodal = true)
        }
        
        // Fast text-only meal tracking ViewModel
        val textOnlyMealViewModel: com.stel.gemmunch.viewmodels.TextOnlyMealViewModel by viewModels {
            com.stel.gemmunch.viewmodels.TextOnlyMealViewModelFactory(appContainer)
        }
        
        val nutrientDBViewModel: NutrientDBViewModel by viewModels {
            NutrientDBViewModelFactory(appContainer)
        }
        
        // Initialize Health Connect permission launcher
        healthConnectPermissionLauncher = registerForActivityResult(
            appContainer.healthConnectManager.createPermissionRequestContract()
        ) { grantedPermissions ->
            // After returning from permission screen, refresh the permission status
            mainViewModel.refreshHealthConnectPermissions()
        }

        setContent {
            GemMunchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

                    // Always show main app - NutrientDB works without AI models
                    GemMunchApp(
                        mainViewModel = mainViewModel,
                        foodCaptureViewModel = foodCaptureViewModel,
                        analyzeAndChatViewModel = analyzeAndChatViewModel,
                        textOnlyMealViewModel = textOnlyMealViewModel,
                        nutrientDBViewModel = nutrientDBViewModel,
                        isAiReady = uiState.isAiReady,
                        initializationProgress = uiState.initializationProgress,
                        downloadState = uiState.downloadState,
                        onRequestHealthConnectPermissions = {
                            healthConnectPermissionLauncher.launch(
                                HealthConnectManager.NUTRITION_PERMISSIONS
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GemMunchApp(
    mainViewModel: MainViewModel,
    foodCaptureViewModel: FoodCaptureViewModel,
    analyzeAndChatViewModel: EnhancedChatViewModel,
    textOnlyMealViewModel: com.stel.gemmunch.viewmodels.TextOnlyMealViewModel,
    nutrientDBViewModel: NutrientDBViewModel,
    isAiReady: Boolean,
    initializationProgress: String?,
    downloadState: MultiDownloadState,
    onRequestHealthConnectPermissions: () -> Unit
) {
    val navController = rememberNavController()
    val sessionManager = remember { SessionManager(mainViewModel.appContainer) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // State to track current screen title
    var currentTitle by remember { mutableStateOf<String>("GemMunch") }
    
    // Track current chat mode
    var currentChatMode by remember { mutableStateOf<String?>(null) }
    
    // Update title and chat mode based on navigation changes
    LaunchedEffect(navBackStackEntry) {
        val route = navBackStackEntry?.destination?.route
        val withCameraArg = navBackStackEntry?.arguments?.getString("withCamera")
        
        // Update chat mode for AppScaffold
        currentChatMode = if (route == "chat/{withCamera}") withCameraArg else null
        
        currentTitle = when {
            route?.startsWith("camera/") == true -> "Quick Snap Insight"
            route?.startsWith("imageCrop/") == true -> "Frame food for analysis"
            route == "chat/{withCamera}" -> {
                when (withCameraArg) {
                    "true" -> "Deep Chat: Vision + Text Async chat"
                    "false" -> "Text Only: Reasoning Model"
                    else -> "GemMunch"
                }
            }
            else -> "GemMunch"
        }
    }
    
    CompositionLocalProvider(LocalSessionManager provides sessionManager) {
        GemMunchAppScaffold(
            navController = navController,
            mainViewModel = mainViewModel,
            analyzeAndChatViewModel = analyzeAndChatViewModel,
            textOnlyMealViewModel = textOnlyMealViewModel,
            currentRoute = currentRoute,
            currentChatMode = currentChatMode,
            title = {
                Text(
                    text = currentTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        ) { paddingValues ->
            NavHost(
                navController = navController, 
                startDestination = "home",
                modifier = Modifier.padding(paddingValues)
            ) {
        composable("home") {
            com.stel.gemmunch.ui.screens.HomeScreen(navController = navController)
        }
        composable("camera/{mode}") { backStackEntry ->
            // AI-dependent route - check if models are downloaded
            val modelsDownloaded = downloadState is MultiDownloadState.AllComplete
            if (!modelsDownloaded) {
                SetupScreen(
                    downloadState = downloadState,
                    onDownloadClick = { mainViewModel.startModelDownload() },
                    onBypassSetup = { mainViewModel.bypassSetup() }
                )
            } else {
                val mode = backStackEntry.arguments?.getString("mode") ?: "singleshot"
                when (mode) {
                "singleshot" -> {
                    // Set app mode for single shot
                    LaunchedEffect(Unit) {
                        foodCaptureViewModel.setAppMode(com.stel.gemmunch.model.AppMode.SNAP_AND_LOG)
                    }
                    // Show options screen for Quick Snap mode
                    QuickSnapOptionsScreen(
                        onTakePhoto = {
                            navController.navigate("camera/quicksnap")
                        },
                        onSelectFromGallery = { uri ->
                            val encodedUri = Uri.encode(uri.toString())
                            navController.navigate("imageCrop/$encodedUri")
                        },
                        onYoloCamera = {
                            navController.navigate("camera/yolo")
                        },
                        onYoloGallery = { uri ->
                            val encodedUri = Uri.encode(uri.toString())
                            navController.navigate("yoloAnalysis/$encodedUri")
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                "quicksnap" -> {
                    // Camera preview for Quick Snap
                    CameraPreviewScreen(
                        onImageCaptured = { bitmap ->
                            foodCaptureViewModel.analyzeMealPhoto(bitmap)
                            navController.navigate("analysis")
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                "yolo" -> {
                    // Camera preview for YOLO Mode - instant analysis
                    CameraPreviewScreen(
                        onImageCaptured = { bitmap ->
                            // Set YOLO mode for instant high-res analysis
                            foodCaptureViewModel.setAppMode(com.stel.gemmunch.model.AppMode.SNAP_AND_LOG)
                            foodCaptureViewModel.analyzeMealPhoto(bitmap)
                            navController.navigate("analysis")
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                "chat" -> {
                    CameraPreviewScreen(
                        onImageCaptured = { bitmap ->
                            foodCaptureViewModel.captureForChat(bitmap)
                            navController.navigate("chat/true")
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
            }
        }
        composable("chat/{withCamera}") { backStackEntry ->
            val withCamera = backStackEntry.arguments?.getString("withCamera")?.toBoolean() ?: false
            
            if (withCamera) {
                // AI-dependent route for multimodal chat - check if models are downloaded
                val modelsDownloaded = downloadState is MultiDownloadState.AllComplete
                if (!modelsDownloaded) {
                    SetupScreen(
                        downloadState = downloadState,
                        onDownloadClick = { mainViewModel.startModelDownload() },
                        onBypassSetup = { mainViewModel.bypassSetup() }
                    )
                } else {
                    // For analyze & chat mode, set image context if available
                    foodCaptureViewModel.capturedImageForChat?.let { imagePath ->
                        analyzeAndChatViewModel.addImageToConversation(imagePath)
                    }
                    
                    com.stel.gemmunch.ui.screens.EnhancedChatScreen(
                        navController = navController,
                        withCamera = withCamera,
                        viewModel = analyzeAndChatViewModel
                    )
                }
            } else {
                // Text-only mode - no AI dependency, use fast function-calling style
                com.stel.gemmunch.ui.screens.TextOnlyMealScreen(
                    navController = navController,
                    viewModel = textOnlyMealViewModel
                )
            }
        }
        composable("analysis") {
            // AI-dependent route - check if models are downloaded
            val modelsDownloaded = downloadState is MultiDownloadState.AllComplete
            if (!modelsDownloaded) {
                SetupScreen(
                    downloadState = downloadState,
                    onDownloadClick = { mainViewModel.startModelDownload() },
                    onBypassSetup = { mainViewModel.bypassSetup() }
                )
            } else {
                CameraFoodCaptureScreen(
                    foodViewModel = foodCaptureViewModel,
                    mainViewModel = mainViewModel,
                    navController = navController,
                    isAiReady = isAiReady,
                    initializationProgress = initializationProgress,
                    onRequestHealthConnectPermissions = onRequestHealthConnectPermissions
                )
            }
        }
        composable("cameraPreview") {
            // AI-dependent route - check if models are downloaded
            val modelsDownloaded = downloadState is MultiDownloadState.AllComplete
            if (!modelsDownloaded) {
                SetupScreen(
                    downloadState = downloadState,
                    onDownloadClick = { mainViewModel.startModelDownload() },
                    onBypassSetup = { mainViewModel.bypassSetup() }
                )
            } else {
                CameraPreviewScreen(
                    onImageCaptured = { bitmap ->
                        foodCaptureViewModel.analyzeMealPhoto(bitmap)
                        navController.navigate("analysis") {
                            popUpTo("home") { inclusive = false }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable("imageCrop/{imageUri}") { backStackEntry ->
            // AI-dependent route - check if models are downloaded
            val modelsDownloaded = downloadState is MultiDownloadState.AllComplete
            if (!modelsDownloaded) {
                SetupScreen(
                    downloadState = downloadState,
                    onDownloadClick = { mainViewModel.startModelDownload() },
                    onBypassSetup = { mainViewModel.bypassSetup() }
                )
            } else {
                val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
                val imageUri = Uri.parse(Uri.decode(encodedUri))
                ImageCropScreen(
                    imageUri = imageUri,
                    onCropComplete = { bitmap ->
                        foodCaptureViewModel.analyzeMealPhoto(bitmap)
                        // Navigate to analysis screen after cropping
                        navController.navigate("analysis") {
                            // Clear back stack to prevent going back to crop screen
                            popUpTo("home") { inclusive = false }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        
        // YOLO Mode - Direct analysis without cropping  
        composable("yoloAnalysis/{imageUri}") { backStackEntry ->
            // AI-dependent route - check if models are downloaded
            val modelsDownloaded = downloadState is MultiDownloadState.AllComplete
            if (!modelsDownloaded) {
                SetupScreen(
                    downloadState = downloadState,
                    onDownloadClick = { mainViewModel.startModelDownload() },
                    onBypassSetup = { mainViewModel.bypassSetup() }
                )
            } else {
                val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
                val imageUri = Uri.parse(Uri.decode(encodedUri))
                
                // Load and analyze image directly without cropping
                LaunchedEffect(imageUri) {
                    try {
                        val inputStream = navController.context.contentResolver.openInputStream(imageUri)
                        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                        
                        if (bitmap != null) {
                            // Set YOLO mode for instant high-res analysis
                            foodCaptureViewModel.setAppMode(com.stel.gemmunch.model.AppMode.SNAP_AND_LOG)
                            foodCaptureViewModel.analyzeMealPhoto(bitmap)
                            
                            // Navigate directly to analysis
                            navController.navigate("analysis") {
                                popUpTo("home") { inclusive = false }
                            }
                        }
                    } catch (e: Exception) {
                        // Handle error - could show error screen or go back
                        navController.popBackStack()
                    }
                }
            
            // Show loading screen while processing
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "🤞 Feeling Lucky: Analyzing high-resolution image...",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No cropping needed - instant analysis!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            }
        }
        composable("nutrient-db") {
            NutrientDBScreen(
                navController = navController,
                viewModel = nutrientDBViewModel
            )
        }
            }
        }
    }
}