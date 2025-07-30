package com.stel.gemmunch.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.stel.gemmunch.GemMunchApplication
import com.stel.gemmunch.agent.MultiDownloadState
import com.stel.gemmunch.data.HealthConnectManager
import com.stel.gemmunch.ui.theme.GemMunchTheme

import com.stel.gemmunch.ui.*

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

                    // The main app router. Show setup screen only if models aren't downloaded yet
                    val modelsDownloaded = uiState.downloadState is MultiDownloadState.AllComplete
                    
                    if (modelsDownloaded) {
                        // Models are downloaded, show main app (AI might still be initializing)
                        GemMunchApp(
                            mainViewModel = mainViewModel,
                            foodCaptureViewModel = foodCaptureViewModel,
                            isAiReady = uiState.isAiReady,
                            initializationProgress = uiState.initializationProgress,
                            onRequestHealthConnectPermissions = {
                                healthConnectPermissionLauncher.launch(
                                    HealthConnectManager.NUTRITION_PERMISSIONS
                                )
                            }
                        )
                    } else {
                        // Models not downloaded yet, show setup screen
                        SetupScreen(
                            downloadState = uiState.downloadState,
                            onDownloadClick = { mainViewModel.startModelDownload() },
                            onBypassSetup = { mainViewModel.bypassSetup() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GemMunchApp(
    mainViewModel: MainViewModel,
    foodCaptureViewModel: FoodCaptureViewModel,
    isAiReady: Boolean,
    initializationProgress: String?,
    onRequestHealthConnectPermissions: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            CameraFoodCaptureScreen(
                foodViewModel = foodCaptureViewModel,
                mainViewModel = mainViewModel,
                navController = navController,
                isAiReady = isAiReady,
                initializationProgress = initializationProgress,
                onRequestHealthConnectPermissions = onRequestHealthConnectPermissions
            )
        }
        composable("cameraPreview") {
            CameraPreviewScreen(
                onImageCaptured = { bitmap ->
                    foodCaptureViewModel.analyzeMealPhoto(bitmap)
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("imageCrop/{imageUri}") { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            val imageUri = Uri.parse(Uri.decode(encodedUri))
            ImageCropScreen(
                imageUri = imageUri,
                onCropComplete = { bitmap ->
                    foodCaptureViewModel.analyzeMealPhoto(bitmap)
                    // Pop back to the main screen after analysis
                    navController.popBackStack("main", inclusive = false)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}