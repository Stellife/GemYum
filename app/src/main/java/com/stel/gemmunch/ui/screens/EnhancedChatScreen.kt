package com.stel.gemmunch.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.stel.gemmunch.model.ChatMessage
import com.stel.gemmunch.viewmodels.EnhancedChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedChatScreen(
    navController: NavController,
    withCamera: Boolean,
    viewModel: EnhancedChatViewModel
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val showHealthConnectDialog by viewModel.showHealthConnectDialog.collectAsStateWithLifecycle()
    val currentMealNutrition by viewModel.currentMealNutrition.collectAsStateWithLifecycle()
    val hasImage by viewModel.hasImage.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Gallery launcher for uploading images
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addImageFromGallery(it) }
    }
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analyze & Discuss") },
                actions = {
                    // Clear/Reset button
                    IconButton(
                        onClick = {
                            viewModel.clearChatAndGoHome()
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Clear & Go Home",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            state = listState,
            reverseLayout = true,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages.reversed()) { message ->
                ChatMessageBubble(message)
            }
            
            // Welcome messages (preloaded AI introduction)
            if (messages.isEmpty()) {
                item {
                    InitialWelcomeMessages(
                        withCamera = withCamera,
                        onTakePhoto = { navController.navigate("camera/chat") },
                        onUploadImage = { galleryLauncher.launch("image/*") },
                        onStartTextChat = { /* Focus text input automatically */ }
                    )
                }
            }
        }
        
            ChatInputBar(
                onSendMessage = { message ->
                    viewModel.sendMessage(message)
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                onCameraClick = if (withCamera && !hasImage) {
                    { navController.navigate("camera/chat") }
                } else null,
                isLoading = isLoading
            )
        }
    }
    
    // Health Connect Save Dialog
    if (showHealthConnectDialog && currentMealNutrition.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissHealthConnectDialog() },
            title = { Text("Save to Health Connect?") },
            text = {
                val totalCalories = currentMealNutrition.sumOf { it.calories }
                Text("Save this meal with $totalCalories calories to Health Connect?")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.saveToHealthConnect() }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissHealthConnectDialog() }
                ) {
                    Text("Skip")
                }
            }
        )
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.isFromUser
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = message.text,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                // Show streaming indicator for AI messages that are still streaming
                if (!isUser && message.isStreaming) {
                    Spacer(modifier = Modifier.width(4.dp))
                    StreamingIndicator()
                }
            }
        }
    }
}

@Composable
fun StreamingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "StreamingIndicator")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StreamingAlpha"
    )
    
    Text(
        text = "▋",
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.size(width = 8.dp, height = 16.dp)
    )
}

@Composable
fun InitialWelcomeMessages(
    withCamera: Boolean,
    onTakePhoto: () -> Unit,
    onUploadImage: () -> Unit,
    onStartTextChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // AI Introduction Message
        ChatMessageBubble(
            ChatMessage(
                id = "welcome",
                text = "Hi! I'm your nutrition conversation partner, powered by Gemma 3n. I'd love to chat with you about the nutritional content of your meals in a natural, flowing conversation.",
                isFromUser = false
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Instructions Message
        ChatMessageBubble(
            ChatMessage(
                id = "instructions",
                text = "You can start our conversation by:",
                isFromUser = false
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Action buttons as clickable message bubbles
        if (withCamera) {
            // Taking a picture option
            ActionMessageBubble(
                text = "📸 Taking a picture",
                onClick = onTakePhoto
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Uploading picture option
            ActionMessageBubble(
                text = "📱 Uploading a picture of your food",
                onClick = onUploadImage
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Text input option
        ActionMessageBubble(
            text = "💬 Describing your meal to me",
            onClick = onStartTextChat
        )
    }
}

@Composable
fun ActionMessageBubble(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            ),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clickable { onClick() },
            shadowElevation = 2.dp
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ChatInputBar(
    onSendMessage: (String) -> Unit,
    onCameraClick: (() -> Unit)?,
    isLoading: Boolean
) {
    var text by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column {
        // Loading indicator
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Surface(
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Camera button (if enabled)
                onCameraClick?.let {
                    FilledTonalIconButton(
                        onClick = it,
                        enabled = !isLoading,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(Icons.Filled.CameraAlt, "Take Photo")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                // Text input
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Describe your meal...") },
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (text.isNotBlank()) {
                                onSendMessage(text)
                                text = ""
                                keyboardController?.hide()
                            }
                        }
                    ),
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Send button
                FilledIconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSendMessage(text)
                            text = ""
                            keyboardController?.hide()
                        }
                    },
                    enabled = !isLoading && text.isNotBlank(),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send")
                }
            }
        }
    }
}