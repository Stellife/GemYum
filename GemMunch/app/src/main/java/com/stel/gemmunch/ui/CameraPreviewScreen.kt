package com.stel.gemmunch.ui

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.stel.gemmunch.utils.MediaQualityPreferencesManager

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraPreviewScreen(
    onImageCaptured: (Bitmap) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture Food Photo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (cameraPermissionState.status.isGranted) {
                CameraPreview(controller = controller, modifier = Modifier.fillMaxSize())
                ViewfinderOverlay(modifier = Modifier.fillMaxSize())

                Button(
                    onClick = {
                        takePhoto(
                            controller = controller,
                            context = context,
                            onPhotoTaken = onImageCaptured
                        )
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).size(80.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Capture", modifier = Modifier.size(32.dp))
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("Grant Camera Permission")
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    controller: LifecycleCameraController,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = {
            PreviewView(it).apply {
                this.controller = controller
                controller.bindToLifecycle(lifecycleOwner)
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val squareSize = size.minDimension * 0.85f
        val offsetX = (size.width - squareSize) / 2
        val offsetY = (size.height - squareSize) / 2

        drawRect(color = Color.Black.copy(alpha = 0.5f))
        drawRect(
            color = Color.Transparent,
            topLeft = Offset(offsetX, offsetY),
            size = Size(squareSize, squareSize),
            blendMode = BlendMode.Clear
        )
        drawRect(
            color = Color.White,
            topLeft = Offset(offsetX, offsetY),
            size = Size(squareSize, squareSize),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

private fun takePhoto(
    controller: LifecycleCameraController,
    context: Context,
    onPhotoTaken: (Bitmap) -> Unit
) {
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                super.onCaptureSuccess(image)
                val matrix = android.graphics.Matrix().apply {
                    postRotate(image.imageInfo.rotationDegrees.toFloat())
                }
                var bitmap = image.toBitmap()
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                // Crop to square
                val dimension = bitmap.width.coerceAtMost(bitmap.height)
                val xOffset = (bitmap.width - dimension) / 2
                val yOffset = (bitmap.height - dimension) / 2
                val croppedBitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, dimension, dimension)

                // Scale to selected quality for the model
                val targetResolution = MediaQualityPreferencesManager.getCurrentResolution()
                val finalBitmap = Bitmap.createScaledBitmap(croppedBitmap, targetResolution, targetResolution, true)

                image.close()
                onPhotoTaken(finalBitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                Log.e("Camera", "Could not take photo: ", exception)
            }
        }
    )
}