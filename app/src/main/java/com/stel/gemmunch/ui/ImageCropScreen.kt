package com.stel.gemmunch.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.abs
import com.stel.gemmunch.utils.MediaQualityPreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropScreen(
    imageUri: Uri,
    onCropComplete: (Bitmap) -> Unit,
    onNavigateBack: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var imageAspectRatio by remember { mutableStateOf(1f) }
    var initialScaleApplied by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            val bitmap = if (android.os.Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            } else {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, imageUri)
                android.graphics.ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
            }
            imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            loadedBitmap = bitmap
        }
    }
    
    // Calculate initial scale when both image and view size are available
    LaunchedEffect(loadedBitmap, viewSize) {
        if (loadedBitmap != null && viewSize != IntSize.Zero && !initialScaleApplied) {
            val cropFrameSize = min(viewSize.width, viewSize.height) * 0.85f
            val viewAspectRatio = viewSize.width.toFloat() / viewSize.height.toFloat()
            
            val (imageDisplayWidth, imageDisplayHeight) = if (imageAspectRatio > viewAspectRatio) {
                viewSize.width.toFloat() to (viewSize.width / imageAspectRatio)
            } else {
                (viewSize.height * imageAspectRatio) to viewSize.height.toFloat()
            }
            
            // Calculate minimum scale to ensure crop frame fits within image
            val minScaleX = cropFrameSize / imageDisplayWidth
            val minScaleY = cropFrameSize / imageDisplayHeight
            val minScale = max(minScaleX, minScaleY)
            
            // Apply initial scale if needed
            if (minScale > 1f) {
                scale = minScale * 1.1f // Add 10% padding for better UX
                initialScaleApplied = true
            }
        }
    }

    // Remove Scaffold - let GemMunchAppScaffold handle navigation
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Instructions at the top
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📝 Framing Tips",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Capture food and as many individual items as possible\n• Make sure they're clearly visible in the frame\n• Center the most important items within the square",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Move pinch to zoom instructions to top
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = "📱 Pinch to zoom • Drag to position",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                }
            }
            
            // Image cropping area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (loadedBitmap != null) {
                    Image(
                        bitmap = loadedBitmap!!.asImageBitmap(),
                        contentDescription = "Image to crop",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .onSizeChanged { viewSize = it }
                            .pointerInput(loadedBitmap) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    // Calculate the crop frame size
                                    val cropFrameSize = min(viewSize.width, viewSize.height) * 0.85f
                                    
                                    // Calculate image display dimensions
                                    val viewAspectRatio = viewSize.width.toFloat() / viewSize.height.toFloat()
                                    val (imageDisplayWidth, imageDisplayHeight) = if (imageAspectRatio > viewAspectRatio) {
                                        // Image is wider than view
                                        viewSize.width.toFloat() to (viewSize.width / imageAspectRatio)
                                    } else {
                                        // Image is taller than view
                                        (viewSize.height * imageAspectRatio) to viewSize.height.toFloat()
                                    }
                                    
                                    // Update scale with minimum constraint based on crop frame
                                    val minScaleX = cropFrameSize / imageDisplayWidth
                                    val minScaleY = cropFrameSize / imageDisplayHeight
                                    val minScale = max(minScaleX, minScaleY)
                                    
                                    // Only apply minimum scale if crop frame would exceed image bounds
                                    val effectiveMinScale = if (minScale > 1f) minScale else 1f
                                    
                                    scale = (scale * zoom).coerceIn(effectiveMinScale, 5f)
                                    
                                    // Calculate the visible image bounds after scaling
                                    val scaledImageWidth = imageDisplayWidth * scale
                                    val scaledImageHeight = imageDisplayHeight * scale
                                    
                                    // Calculate maximum allowed offset to keep crop frame within image
                                    val halfCropFrame = cropFrameSize / 2f
                                    val maxOffsetX = max(0f, (scaledImageWidth / 2f) - halfCropFrame)
                                    val maxOffsetY = max(0f, (scaledImageHeight / 2f) - halfCropFrame)
                                    
                                    // Apply pan with proper bounds
                                    offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                    offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                }
                            }
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                    CropFrameOverlay(modifier = Modifier.fillMaxSize())
                } else {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            
            // Giant Analyze Button at bottom (like camera capture button)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        loadedBitmap?.let { bmp ->
                            coroutineScope.launch {
                                val cropped = cropBitmap(bmp, scale, offsetX, offsetY, viewSize)
                                onCropComplete(cropped)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(32.dp),
                    enabled = loadedBitmap != null
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = "Analyze",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Analyze Food",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CropFrameOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val squareSize = size.minDimension * 0.85f
        val offsetX = (size.width - squareSize) / 2
        val offsetY = (size.height - squareSize) / 2

        // Draw dark overlay outside the crop area
        // Top
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset.Zero,
            size = androidx.compose.ui.geometry.Size(size.width, offsetY)
        )
        // Bottom
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, offsetY + squareSize),
            size = androidx.compose.ui.geometry.Size(size.width, size.height - offsetY - squareSize)
        )
        // Left
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(0f, offsetY),
            size = androidx.compose.ui.geometry.Size(offsetX, squareSize)
        )
        // Right
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(offsetX + squareSize, offsetY),
            size = androidx.compose.ui.geometry.Size(size.width - offsetX - squareSize, squareSize)
        )

        // Draw crop frame border
        drawRect(
            color = Color.White,
            topLeft = Offset(offsetX, offsetY),
            size = androidx.compose.ui.geometry.Size(squareSize, squareSize),
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw grid lines
        val gridAlpha = 0.3f
        val third = squareSize / 3
        drawLine(Color.White.copy(alpha = gridAlpha), Offset(offsetX + third, offsetY), Offset(offsetX + third, offsetY + squareSize), strokeWidth = 1.dp.toPx())
        drawLine(Color.White.copy(alpha = gridAlpha), Offset(offsetX + 2 * third, offsetY), Offset(offsetX + 2 * third, offsetY + squareSize), strokeWidth = 1.dp.toPx())
        drawLine(Color.White.copy(alpha = gridAlpha), Offset(offsetX, offsetY + third), Offset(offsetX + squareSize, offsetY + third), strokeWidth = 1.dp.toPx())
        drawLine(Color.White.copy(alpha = gridAlpha), Offset(offsetX, offsetY + 2 * third), Offset(offsetX + squareSize, offsetY + 2 * third), strokeWidth = 1.dp.toPx())
    }
}

private suspend fun cropBitmap(
    bitmap: Bitmap,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    viewSize: IntSize
): Bitmap = withContext(Dispatchers.IO) {
    // Ensure viewSize is valid
    if (viewSize.width <= 0 || viewSize.height <= 0) {
        // Return a center crop of the original bitmap if view size is invalid
        val size = min(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2
        val cropped = Bitmap.createBitmap(bitmap, x, y, size, size)
        val targetResolution = MediaQualityPreferencesManager.getCurrentResolution()
        return@withContext Bitmap.createScaledBitmap(cropped, targetResolution, targetResolution, true)
    }
    
    val viewToBitmapScale = bitmap.width.toFloat() / viewSize.width.toFloat()
    val cropSizeInView = min(viewSize.width, viewSize.height) * 0.85f
    
    // Calculate the desired crop size in bitmap coordinates
    var cropSizeInBitmap = (cropSizeInView * viewToBitmapScale / scale).roundToInt()
    
    // Ensure crop size doesn't exceed bitmap dimensions
    cropSizeInBitmap = min(cropSizeInBitmap, min(bitmap.width, bitmap.height))
    
    // Ensure minimum crop size (at least 1 pixel)
    cropSizeInBitmap = max(1, cropSizeInBitmap)

    val bitmapCenterX = bitmap.width / 2
    val bitmapCenterY = bitmap.height / 2

    val offsetXInBitmap = (offsetX * viewToBitmapScale / scale).roundToInt()
    val offsetYInBitmap = (offsetY * viewToBitmapScale / scale).roundToInt()

    // Calculate crop position with safe bounds
    val maxCropX = max(0, bitmap.width - cropSizeInBitmap)
    val maxCropY = max(0, bitmap.height - cropSizeInBitmap)
    
    val cropX = (bitmapCenterX - cropSizeInBitmap / 2 - offsetXInBitmap)
        .coerceIn(0, maxCropX)
    val cropY = (bitmapCenterY - cropSizeInBitmap / 2 - offsetYInBitmap)
        .coerceIn(0, maxCropY)

    val targetResolution = MediaQualityPreferencesManager.getCurrentResolution()
    try {
        val cropped = Bitmap.createBitmap(bitmap, cropX, cropY, cropSizeInBitmap, cropSizeInBitmap)
        Bitmap.createScaledBitmap(cropped, targetResolution, targetResolution, true)
    } catch (e: Exception) {
        // If cropping fails for any reason, return a center crop as fallback
        val fallbackSize = min(bitmap.width, bitmap.height)
        val fallbackX = (bitmap.width - fallbackSize) / 2
        val fallbackY = (bitmap.height - fallbackSize) / 2
        val fallbackCrop = Bitmap.createBitmap(bitmap, fallbackX, fallbackY, fallbackSize, fallbackSize)
        Bitmap.createScaledBitmap(fallbackCrop, targetResolution, targetResolution, true)
    }
}