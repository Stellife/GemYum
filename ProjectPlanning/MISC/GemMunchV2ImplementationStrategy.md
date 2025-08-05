# GemMunch V2 Implementation Strategy

## Overview
Complete redesign of the app's navigation and session management to provide a better user experience while leveraging Gemma 3N's advanced features for optimal performance.

## Core Architecture Principles

### 1. Mode-Specific Session Optimization

Based on Gemma 3N documentation analysis:

#### Text-Only Sessions
- **No vision components loaded** → Faster initialization (~2-3s saved)
- **Smaller memory footprint** → More headroom for longer conversations
- **PLE caching for common prompts** → Faster response times

#### Multimodal Sessions  
- **Conditional loading** → Vision encoder loaded only when needed
- **MobileNet-V5 encoder** → Efficient vision processing
- **Shared text components** → Reuse from text-only session

#### Single-Shot Sessions
- **Pre-warmed with strict JSON prompt** → Instant analysis
- **Minimal context window** → Optimized for speed
- **No conversation history** → Lower memory usage

### 2. Smart Pre-warming Strategy

```kotlin
class OptimizedSessionManager(private val appContainer: AppContainer) {
    
    // Different session configurations
    private val textOnlyOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
        .setMaxNumImages(0)
        .setMaxTokens(2048)  // More tokens for conversation
        .setTemperature(0.7f)  // More creative for chat
        .build()
    
    private val multimodalOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
        .setMaxNumImages(1)
        .setMaxTokens(1024)  // Balance between vision and text
        .setTemperature(0.5f)  // More focused
        .setEncoderConfig("mobilenet_v5")  // Efficient encoder
        .build()
    
    private val singleShotOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
        .setMaxNumImages(1)
        .setMaxTokens(256)  // Minimal for JSON output
        .setTemperature(0.1f)  // Deterministic
        .build()
    
    // Pre-warm based on user navigation
    fun prewarmForDestination(destination: String) {
        when (destination) {
            "chat/text" -> prewarmTextSession()
            "chat/multimodal" -> prewarmMultimodalSession()
            "camera/singleshot" -> prewarmSingleShotSession()
        }
    }
}
```

### 3. PLE Caching Implementation

```kotlin
object PromptCache {
    // Cache common prompt prefixes
    private val textAssistantPrefix = """
        You are a helpful nutrition assistant. You help users track their meals 
        and understand nutritional content. Be friendly and conversational.
    """.trimIndent()
    
    private val visionAnalysisPrefix = """
        Analyze the food in this image and identify all visible items.
        Consider portion sizes, cooking methods, and visible ingredients.
    """.trimIndent()
    
    private val jsonOutputPrefix = """
        You are a food recognition API. Return only valid JSON.
        Format: {"foods": [{"name": "item", "quantity": n, "unit": "unit"}]}
    """.trimIndent()
    
    // Pre-compute embeddings for faster inference
    suspend fun initializePLECache(llmInference: LlmInference) {
        // This would use Gemma's PLE caching if available in the SDK
        // For now, we ensure prompts are consistent for internal caching
    }
}
```

## UI/UX Implementation

### 1. Home Screen Design

```kotlin
@Composable
fun HomeScreen(navController: NavController) {
    val sessionManager = LocalSessionManager.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GemMunch AI") },
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                "Choose Your Analysis Mode",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Mode Cards
            ModeCard(
                title = "Quick Snap",
                description = "Instant nutrition facts from a photo",
                estimatedTime = "~5 seconds",
                icon = Icons.Outlined.CameraAlt,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = {
                    sessionManager.prewarmForDestination("camera/singleshot")
                    navController.navigate("camera/singleshot")
                }
            )
            
            ModeCard(
                title = "Analyze & Discuss",
                description = "Chat about your meal with photo analysis",
                estimatedTime = "Conversational",
                icon = Icons.Outlined.QuestionAnswer,
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                onClick = {
                    sessionManager.prewarmForDestination("chat/multimodal")
                    navController.navigate("chat/true")
                }
            )
            
            ModeCard(
                title = "Describe Your Meal",
                description = "Text-based nutrition tracking",
                estimatedTime = "Conversational",
                icon = Icons.Outlined.TextFields,
                backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                onClick = {
                    sessionManager.prewarmForDestination("chat/text")
                    navController.navigate("chat/false")
                }
            )
            
            // Tips Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        }
    }
}

@Composable
fun ModeCard(
    title: String,
    description: String,
    estimatedTime: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
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
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    estimatedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### 2. Enhanced Chat Screen

```kotlin
@Composable
fun ChatScreen(
    navController: NavController,
    withCamera: Boolean,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (withCamera) "Analyze & Discuss" else "Describe Your Meal") 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                onSendMessage = viewModel::sendMessage,
                onCameraClick = if (withCamera) {
                    { navController.navigate("camera/chat") }
                } else null,
                isLoading = isLoading
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            reverseLayout = true,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages.reversed()) { message ->
                ChatMessageBubble(message)
            }
            
            // Welcome message
            if (messages.isEmpty()) {
                item {
                    WelcomeMessage(withCamera)
                }
            }
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
                IconButton(
                    onClick = it,
                    enabled = !isLoading
                ) {
                    Icon(Icons.Filled.CameraAlt, "Take Photo")
                }
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
                        }
                    }
                ),
                maxLines = 4
            )
            
            // Send button
            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSendMessage(text)
                        text = ""
                    }
                },
                enabled = !isLoading && text.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send")
            }
        }
        
        // Loading indicator
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

### 3. Navigation Setup

```kotlin
@Composable
fun GemMunchNavigation() {
    val navController = rememberNavController()
    val sessionManager = remember { OptimizedSessionManager(LocalAppContainer.current) }
    
    CompositionLocalProvider(LocalSessionManager provides sessionManager) {
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") { 
                HomeScreen(navController) 
            }
            
            composable(
                "camera/{mode}",
                arguments = listOf(navArgument("mode") { type = NavType.StringType })
            ) { backStackEntry ->
                val mode = backStackEntry.arguments?.getString("mode") ?: "singleshot"
                CameraScreen(
                    navController = navController,
                    mode = when (mode) {
                        "singleshot" -> CameraMode.SingleShot
                        "chat" -> CameraMode.ForChat
                        else -> CameraMode.SingleShot
                    }
                )
            }
            
            composable(
                "chat/{withCamera}",
                arguments = listOf(navArgument("withCamera") { type = NavType.BoolType })
            ) { backStackEntry ->
                val withCamera = backStackEntry.arguments?.getBoolean("withCamera") ?: false
                ChatScreen(
                    navController = navController,
                    withCamera = withCamera
                )
            }
            
            composable("results") {
                ResultsScreen(navController)
            }
        }
    }
}
```

## Performance Considerations

### 1. Memory Management
- Text-only sessions use ~40% less memory than multimodal
- Aggressive cleanup of unused sessions
- Monitor for OOM conditions

### 2. Latency Optimization
- Pre-warm sessions during UI transitions
- Use PLE caching for common prompts
- Progressive UI updates during streaming

### 3. Battery Usage
- Prefer NPU/GPU when available
- Batch inference requests
- Idle session cleanup

## Testing Strategy

### 1. Mode Transition Tests
- Home → Single Shot → Results
- Home → Chat (with camera) → Camera → Chat
- Home → Chat (text only)
- Background/foreground transitions

### 2. Performance Tests
- Cold start times for each mode
- Memory usage monitoring
- Battery consumption analysis
- Thermal throttling behavior

### 3. User Experience Tests
- Navigation flow clarity
- Response time perception
- Error handling gracefully
- Offline capability

## Implementation Priority

### Phase 1 (4 hours)
1. Create HomeScreen with mode cards
2. Set up navigation structure
3. Basic session management

### Phase 2 (4 hours)
1. Full ChatScreen implementation
2. Camera integration
3. Results screen update

### Phase 3 (2 hours)
1. Session optimization
2. PLE caching setup
3. Performance tuning

### Phase 4 (2 hours)
1. Polish and animations
2. Error handling
3. Final testing

## Success Metrics

1. **Performance**
   - Text-only mode starts in <3 seconds
   - Single shot analysis in <5 seconds
   - Smooth 60fps UI transitions

2. **User Experience**
   - Clear mode differentiation
   - Intuitive navigation
   - Responsive interactions

3. **Technical**
   - Efficient memory usage
   - Proper session cleanup
   - Graceful error handling

## Risk Mitigation

1. **Fallback to Single Session**
   - If multi-session management fails
   - Revert to current architecture

2. **Progressive Enhancement**
   - Basic features work first
   - Advanced optimization optional

3. **Testing on Multiple Devices**
   - Low-end device testing
   - Different Android versions
   - Various screen sizes