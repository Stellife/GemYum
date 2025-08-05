# GemMunch - Comprehensive Developer Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [Dependencies & AI-Edge Stack](#dependencies--ai-edge-stack)
3. [Architecture Overview](#architecture-overview)
4. [Key Files & Components](#key-files--components)
5. [Three-Path UI System](#three-path-ui-system)
6. [Data Flow & Processing](#data-flow--processing)
7. [AI Integration Details](#ai-integration-details)
8. [Project Status & Roadmap](#project-status--roadmap)
9. [Getting Started Guide](#getting-started-guide)
10. [Tips, Tricks & Warnings](#tips-tricks--warnings)

## Project Overview

GemMunch is a sophisticated nutrition tracking Android application that leverages **Google's Gemma 3n AI model** running entirely on-device for food image recognition. The app integrates with **Android Health Connect** and provides three distinct user interaction modes optimized for different use cases.

### Core Features
- **On-device AI food recognition** using MediaPipe and Gemma 3n models
- **Three-path UI system** for different user workflows
- **Comprehensive nutrition database** with USDA API fallback
- **Android Health Connect integration** for automatic meal logging
- **Real-time performance optimization** with hardware acceleration
- **Conversational AI interface** for complex meal analysis with **true token-by-token streaming**

### Target Audience
- **Kaggle Competition Entry** (AI-Edge focus)
- **Health-conscious users** requiring accurate nutrition tracking
- **Developers interested in on-device AI implementation**

## Dependencies & AI-Edge Stack

### Core AI Dependencies
```toml
# From libs.versions.toml
googleAiEdgeAicore = "0.0.1-exp02"          # Gemini Nano integration
mediapipeTasksGenai = "0.10.25"             # LLM inference with vision
localagentsFc = "0.1.0"                     # Function Calling SDK (unused)
localagentsRag = "0.2.0"                    # RAG SDK (unused)
```

### Key Library Documentation Sources
1. **MediaPipe GenAI**: https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android
2. **AI-Edge Gallery**: https://github.com/google-ai-edge/gallery/tree/main/Android
3. **Gemma 3n Documentation**: https://ai.google.dev/gemma/docs/gemma-3n
4. **Android Health Connect**: https://developer.android.com/health-and-fitness/guides/health-connect

### AI-Edge Integration Strategy
- **MediaPipe tasks-genai**: Primary multimodal inference engine
- **Google Play Services TensorFlow Lite**: Hardware acceleration layer
- **LiteRT omitted**: Avoided due to dependency conflicts and MediaPipe superiority
- **Gemma 3n .task models**: Optimized for mobile deployment

### Hardware Acceleration Stack
```kotlin
// Golden Path: PlayServicesAccelerationService
val validatedConfig = playServicesAcceleration.getValidatedAccelerationConfig()
// Fallback: Manual GPU detection
val acceleration = playServicesAcceleration.findOptimalAcceleration()
```

## Architecture Overview

### High-Level Architecture Pattern
**Layered MVVM with Custom Dependency Injection**
```
+---------------------------------------+
|           UI Layer                    |
|  +-----+ +-----+ +-----+             |
|  |Home | |Chat | |Cam  |             |
|  |     | |     | |     |             |
|  +-----+ +-----+ +-----+             |
+---------------------------------------+
|          ViewModel Layer              |  
|  +--------------+ +--------------+   |
|  |EnhancedChat  | |FoodCapture   |   |
|  |ViewModel     | |ViewModel     |   |
|  +--------------+ +--------------+   |
+---------------------------------------+
|         Service Layer                 |
|  +-------------+ +-----------------+ |
|  |PhotoMeal    | |NutritionSearch  | |
|  |Extractor    | |Service          | |
|  +-------------+ +-----------------+ |
+---------------------------------------+
|          Data Layer                   |
|  +-------------+ +-----------------+ |
|  |Enhanced     | |HealthConnect    | |
|  |NutrientDb   | |Manager          | |
|  +-------------+ +-----------------+ |
+---------------------------------------+
|          AI Layer                     |
|  +-------------+ +-----------------+ |
|  |AppContainer | |MediaPipe        | |
|  |(DI)         | |LlmInference     | |
|  +-------------+ +-----------------+ |
+---------------------------------------+
```

### Core Design Principles
1. **Session Pre-warming**: AI sessions prepared before user interaction
2. **Graceful Degradation**: Multiple fallback paths for reliability
3. **Resource Efficiency**: Proper MediaPipe lifecycle management
4. **User Experience First**: Responsive UI with real-time progress
5. **On-device Privacy**: All AI processing stays local

## Key Files & Components

### ARCHITECTURE FOUNDATION

#### `AppContainer.kt` - **Dependency Injection Hub**
```kotlin
interface AppContainer {
    val photoMealExtractor: PhotoMealExtractor?
    val visionLlmInference: LlmInference?
    suspend fun getReadyVisionSession(): LlmInferenceSession
    fun startContinuousPrewarming()
}
```
**Purpose**: Manages AI model lifecycle, session pooling, and hardware acceleration detection.
**Dependencies**: MediaPipe, PlayServicesAccelerationService, model management
**Key Features**: Golden Path acceleration, session pre-warming, resource cleanup

#### `PhotoMealExtractor.kt` - **Core AI Processing Engine**
```kotlin
suspend fun extract(
    bitmap: Bitmap, 
    userContext: String? = null,
    appMode: AppMode = AppMode.SNAP_AND_LOG,
    onProgress: ((AnalysisProgress) -> Unit)? = null
): MealAnalysis
```
**Purpose**: Orchestrates complete food analysis pipeline from image to nutrition data.
**Dependencies**: MediaPipe sessions, nutrition database, prompt engineering
**Key Features**: Context-aware prompting, performance monitoring, error recovery

### UI LAYER

#### `HomeScreen.kt` - **Three-Path Mode Selection**
```kotlin
ModeCard("Quick Snap", "camera/singleshot")     // SNAP_AND_LOG
ModeCard("Analyze & Discuss", "chat/true")      // ANALYZE_AND_CHAT  
ModeCard("Describe Your Meal", "chat/false")    // TEXT_ONLY
```
**Purpose**: User interaction mode selection with session pre-warming.
**Navigation**: Jetpack Compose Navigation with mode-specific destinations

#### `EnhancedChatScreen.kt` - **Conversational AI Interface**
```kotlin
@Composable
fun EnhancedChatScreen(
    navController: NavController,
    withCamera: Boolean,
    viewModel: EnhancedChatViewModel
)
```
**Purpose**: Handles both multimodal (image+text) and text-only conversations.
**Features**: **True token-by-token streaming responses**, Health Connect integration, nutrition summaries, animated streaming indicators

#### `CameraFoodCaptureScreen.kt` - **Primary Analysis Interface**
```kotlin
sealed interface FoodCaptureState {
    data object Idle
    data class Loading(val progress: AnalysisProgress)
    data class Success(val originalAnalysis: MealAnalysis)
    data class Error(val message: String)
}
```
**Purpose**: Camera capture, real-time analysis progress, editable results display.

### VIEWMODEL LAYER

#### `EnhancedChatViewModel.kt` - **Streaming Conversation Management**
```kotlin
private suspend fun generateStreamingResponse(session: LlmInferenceSession): String {
    return suspendCancellableCoroutine { continuation ->
        session.generateResponseAsync { partialResult, done ->
            responseBuilder.append(partialResult)
            
            // Real-time UI updates
            viewModelScope.launch {
                updateStreamingMessage(currentMessageId!!, responseBuilder.toString(), !done)
                if (done) {
                    continuation.resume(responseBuilder.toString()) {}
                }
            }
        }
    }
}
```
**Purpose**: Implements agent-like behavior with **real-time streaming responses** and structured prompts.
**Features**: Token-by-token streaming, JSON parsing, nutrition lookup, clarification handling, animated typing indicators

#### `FoodCaptureViewModel.kt` - **Single-Shot Analysis Management**
```kotlin
fun analyzeMealPhoto(bitmap: Bitmap) {
    val extractor = appContainer.photoMealExtractor
    extractor.extract(bitmap, userContext, appMode) { progress ->
        _uiState.value = FoodCaptureState.Loading(progress)
    }
}
```
**Purpose**: Manages SNAP_AND_LOG workflow with real-time progress tracking.

### DATA LAYER

#### `EnhancedNutrientDbHelper.kt` - **Hybrid Nutrition Database**
```kotlin
class EnhancedNutrientDbHelper(
    private val context: Context,
    private val usdaApiService: UsdaApiService
) {
    suspend fun lookup(
        food: String, 
        qty: Double, 
        unit: String, 
        onUsdaFallback: ((String) -> Unit)? = null
    ): NutrientInfo
}
```
**Purpose**: Three-tier nutrition lookup (Local SQLite -> USDA API -> Defaults).
**Features**: Restaurant-specific data, unit conversion, glycemic index support, real-time USDA fallback notifications

#### `HealthConnectManager.kt` - **Android Health Platform Integration**
```kotlin
suspend fun writeNutritionRecords(
    items: List<AnalyzedFoodItem>,
    mealDateTime: Instant,
    mealName: String? = null
): Boolean
```
**Purpose**: Seamless integration with Android Health Connect for meal logging.
**Features**: Automatic meal type detection, nutrition record formatting

#### `NutritionSearchService.kt` - **Unified Nutrition Interface**
```kotlin
suspend fun searchNutrition(
    foodName: String,
    servingSize: Double,
    servingUnit: String = "serving"
): AnalyzedFoodItem?
```
**Purpose**: Abstraction layer over nutrition sources with intelligent fallback.

#### `UsdaApiService.kt` - **Enhanced USDA API Integration**
```kotlin
class UsdaApiService {
    suspend fun searchAndGetBestCalories(foodName: String): Int?
    suspend fun searchAndGetFullNutrition(foodName: String): UsdaNutritionData?
    
    private fun findBestFoodMatch(searchTerm: String, results: List<UsdaFoodSummary>): UsdaFoodSummary?
    private suspend fun tryAlternativeSearches(originalTerm: String): UsdaFoodSummary?
    private fun generateAlternativeSearchTerms(foodName: String): List<String>
}
```
**Purpose**: Intelligent USDA API integration with advanced food matching algorithms.
**Features**: Custom scoring system, alternative search strategies, minimum quality thresholds, food-specific search term generation

### UTILITY & SERVICE LAYER

#### `SessionManager.kt` - **AI Session Optimization**
```kotlin
suspend fun prewarmForDestination(destination: String) {
    when (destination) {
        "chat/text" -> prewarmTextSession()
        "chat/multimodal" -> prewarmMultimodalSession()
        "camera/singleshot" -> prewarmSingleShotSession()
    }
}
```
**Purpose**: Navigation-aware session pre-warming for optimal performance.

#### `PlayServicesAccelerationService.kt` - **Hardware Acceleration**
```kotlin
fun getValidatedAccelerationConfig(modelPath: String): AccelerationConfigWrapper?
fun findOptimalAcceleration(modelPath: String): AccelerationResult
```
**Purpose**: Golden Path acceleration detection with Google Play Services integration.
**Features**: NPU/GPU detection, performance benchmarking, fallback handling

### AI MODEL MANAGEMENT

#### `ModelRegistry.kt` - **Centralized Model Configuration**
```kotlin
object ModelRegistry {
    private val allModels = listOf(
        ModelAsset("GEMMA_3N_E2B_MODEL", "Gemma 3n E2B (Fast)", "*.task"),
        ModelAsset("GEMMA_3N_E4B_MODEL", "Gemma 3n E4B (Accurate)", "*.task")
    )
}
```
**Purpose**: Defines available AI models with HuggingFace download URLs.
**Models**: Gemma 3n E2B (2B params, fast) vs E4B (4B params, accurate)

#### `ModelDownloader.kt` - **Progressive Model Downloads**
```kotlin
fun downloadAllModels(context: Context, models: List<ModelAsset>): Flow<MultiDownloadState>
```
**Purpose**: Background model downloads with progress tracking and resume capability.

## Three-Path UI System

### Path A: "Quick Snap" (SNAP_AND_LOG Mode)
**Goal**: Lightning-fast nutrition logging with minimal user interaction

**Enhanced Flow** (Recently Updated):
1. **Options Screen**: Choose between "Take Photo" or "Upload Image"
2. **Camera Path**: Camera preview -> Capture -> Analysis
3. **Gallery Path**: Gallery picker -> Image crop -> Analysis
4. **AI Analysis**: Strict JSON prompt for deterministic output
5. **Results Display**: Direct nutrition lookup with editable results
6. **Clean Exit**: All actions (Cancel/Save/Try Again) return to home
7. **Failure Case**: Auto-escalate to Path B with "I need help with this one"

**Prompt Strategy**:
```kotlin
val strictJsonPromptText = """
You are a food recognition API. Return ONLY valid JSON.
Format: [{"food": "item name", "quantity": number, "unit": "unit", "confidence": number}]
If uncertain, return: []
"""
```

### Path B: "Analyze & Chat" (ANALYZE_AND_CHAT Mode)
**Goal**: Conversational analysis with clarification capability

**Flow**:
1. Camera capture -> Initial AI analysis with reasoning
2. AI can request clarification via structured JSON
3. User provides context -> Re-analysis with context
4. Iterative refinement until confident
5. Nutrition summary -> Health Connect option

**Agent Simulation**:
```kotlin
// Simulated Function Calling through structured prompts
{
    "foods": [...],
    "needsClarification": true,
    "question": "Is this from a specific restaurant?"
}
```

### Path C: "Text-Only" (TEXT_ONLY Mode)
**Goal**: No-camera nutrition tracking for accessibility

**Flow**:
1. Natural language meal description
2. Text parsing for food identification
3. Same nutrition lookup pipeline as other paths
4. Conversational refinement if needed
5. Health Connect integration

**Reuse Strategy**: Same `EnhancedChatViewModel` without camera components

## Data Flow & Processing

### Complete Analysis Pipeline
```
+-------------+    +-------------+    +-------------+
| Image/Text  |--->| AI Analysis |--->| JSON Parse  |
| Input       |    | (MediaPipe) |    | & Validate  |
+-------------+    +-------------+    +-------------+
                           |                   |
                           v                   v
+-------------+    +-------------+    +-------------+
| Health      |<---| Nutrition   |<---| Database    |
| Connect     |    | Summary     |    | Lookup      |
+-------------+    +-------------+    +-------------+
                           ^                   |
                           |                   v
+-------------+    +-------------+    +-------------+
| User        |--->| Editable    |    | Enhanced    |
| Refinement  |    | Results     |    | USDA API    |
+-------------+    +-------------+    +-------------+
```

### Enhanced USDA API Search Pipeline (New Implementation)
```
+-------------+    +-------------+    +-------------+
| Food Name   |--->| Initial     |--->| Custom      |
| "Pad Thai"  |    | USDA Search |    | Scoring     |
+-------------+    +-------------+    +-------------+
                          |                   |
                          v                   v
+-------------+    +-------------+    +-------------+
| Alternative |<---| Quality     |--->| Accept/     |
| Search      |    | Threshold   |    | Reject      |
+-------------+    +-------------+    +-------------+
       |                                     |
       v                                     v
+-------------+    +-------------+    +-------------+
| "thai       |--->| Better      |--->| Final       |
| noodles"    |    | Match Found |    | Result      |
+-------------+    +-------------+    +-------------+
```

### Session Lifecycle Management
```
1. Pre-warming:   Session created in background
2. Consumption:   Ready session retrieved instantly  
3. Analysis:      Image + prompt -> AI inference
4. Cleanup:       Proper resource disposal
5. Regeneration:  New session pre-warmed automatically
```

### Error Handling & Fallbacks
```
AI Analysis Failure -> Structured Error Response -> User Feedback Collection
|
Acceleration Issues -> CPU Fallback -> Performance Monitoring
|  
Network Failure -> Local Database Only -> Graceful Degradation
|
Health Connect Unavailable -> Local Storage -> User Notification
```

## AI Integration Details

### MediaPipe Integration Pattern
```kotlin
// Session Creation (Expensive - Pre-warmed)
val llmInference = LlmInference.createFromOptions(context, options)

// Session Usage (Fast - Reused)
val session = LlmInferenceSession.createFromOptions(llmInference, sessionOptions)
session.addImage(mpImage)
session.addQueryChunk(prompt)

// ❌ Old blocking approach:
// val response = session.generateResponse()

// ✅ New streaming approach:
session.generateResponseAsync { partialResult, done ->
    // Handle token-by-token updates
}
session.close() // Important: Proper cleanup
```

### Real-Time Streaming Implementation (Recently Added! ✨)

#### Problem: Non-Streaming Chat Experience
```kotlin
// ❌ Before: Blocking response that felt unnatural
val response = session.generateResponse() // User waits for complete response
addMessage(ChatMessage(text = response, isFromUser = false))
```

#### Solution: True Token-by-Token Streaming
```kotlin
// ✅ After: Real-time streaming like modern AI chats
private suspend fun generateStreamingResponse(session: LlmInferenceSession): String {
    return suspendCancellableCoroutine { continuation ->
        val responseBuilder = StringBuilder()
        var currentMessageId: String? = null
        
        session.generateResponseAsync { partialResult, done ->
            responseBuilder.append(partialResult)
            
            // Real-time UI updates for each token
            viewModelScope.launch {
                if (currentMessageId == null) {
                    // Create initial streaming message
                    currentMessageId = UUID.randomUUID().toString()
                    val streamingMessage = ChatMessage(
                        id = currentMessageId!!,
                        text = partialResult,
                        isFromUser = false,
                        isStreaming = true  // ← Enables animated cursor
                    )
                    addMessage(streamingMessage)
                } else {
                    // Update existing message with accumulated response
                    updateStreamingMessage(currentMessageId!!, responseBuilder.toString(), !done)
                }
                
                if (done) {
                    // Mark message as complete, remove cursor
                    updateStreamingMessage(currentMessageId!!, responseBuilder.toString(), false)
                    continuation.resume(responseBuilder.toString()) {}
                }
            }
        }
    }
}
```

#### Enhanced ChatMessage Data Model
```kotlin
data class ChatMessage(
    val id: String = Instant.now().toEpochMilli().toString(),
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Instant = Instant.now(),
    val imagePath: String? = null,
    val isStreaming: Boolean = false  // ← New field for streaming state
)
```

#### Animated Streaming Indicator UI
```kotlin
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
        text = "▋",  // Blinking cursor
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
    )
}

@Composable 
fun ChatMessageBubble(message: ChatMessage) {
    // ... existing bubble code ...
    
    // Show animated cursor for streaming messages
    if (!message.isFromUser && message.isStreaming) {
        Spacer(modifier = Modifier.width(4.dp))
        StreamingIndicator()
    }
}
```

#### Real-Time Message State Management
```kotlin
private fun updateStreamingMessage(messageId: String, newText: String, isStillStreaming: Boolean) {
    _messages.value = _messages.value.map { message ->
        if (message.id == messageId) {
            message.copy(text = newText, isStreaming = isStillStreaming)
        } else {
            message
        }
    }
}
```

#### User Experience Transformation
```
❌ Before (Blocking):
User: "What's in this meal?"
[Long pause...]
AI: [Complete response appears all at once]

✅ After (Streaming):
User: "What's in this meal?"
AI: "I can see this appears to be▋"
AI: "I can see this appears to be a delicious▋" 
AI: "I can see this appears to be a delicious bowl with▋"
AI: "I can see this appears to be a delicious bowl with rice and vegetables."
```

#### Technical Benefits
- **Modern UX**: Feels like ChatGPT, Claude, or other streaming AI interfaces
- **Perceived Performance**: Users see immediate response even during 12+ second inference
- **Real-time Feedback**: Users know the AI is actively working
- **Proper Resource Management**: No blocking UI threads during generation
- **State Management**: Clean message lifecycle from streaming → complete

### Prompt Engineering Strategies

#### 1. Strict JSON Mode (SNAP_AND_LOG)
```kotlin
private val strictJsonPromptText = """
You are a food recognition API. Your only job is to analyze the image and return a valid JSON array.
Do not add explanation or conversational text.
Output format: [{"food": "item name", "quantity": number, "unit": "unit", "confidence": number}]
If you cannot identify food with high confidence, return: []
"""
```

#### 2. Reasoning Mode (Debug/Learning)
```kotlin
private val reasoningPromptText = """
Analyze the food in this image step by step. Show your reasoning, then provide the JSON.

Step 1: Describe the main visual elements
Step 2: Identify each food component  
Step 3: Provide final answer as JSON array

[Reasoning display] -> [JSON extraction]
"""
```

#### 3. Context-Enhanced Mode
```kotlin
private fun buildContextHint(userContext: String): String {
    when {
        lowerContext.contains("chipotle") -> 
            "This meal is from Chipotle. Look for barbacoa, carnitas, cilantro-lime rice..."
        lowerContext.contains("vegetarian") -> 
            "This meal is vegetarian - no meat products."
    }
}
```

### USDA API Enhancement Strategy (Recently Implemented)

#### Problem: Poor Food Matching Accuracy
```
User Log Example (Before Enhancement):
Search: "Pad Thai" -> Found: "SMART SOUP, Thai Coconut Curry"
USDA Score: 331.8 (misleading high score)
Result: Completely wrong food type (soup vs noodles)
Calories: 36 kcal/100g (way too low for Pad Thai)
```

#### Solution: Advanced Scoring Algorithm
```kotlin
private fun findBestFoodMatch(searchTerm: String, results: List<UsdaFoodSummary>): UsdaFoodSummary? {
    val scoredResults = results.map { food ->
        var customScore = 0.0
        val description = food.description.lowercase()
        
        // Exact match bonus (1000 points)
        if (description == searchTerm.lowercase()) customScore += 1000.0
        else if (description.contains(searchTerm.lowercase())) customScore += 500.0
        
        // Word coverage scoring (300 points max)
        val matchingWords = searchWords.count { word -> description.contains(word) }
        customScore += (matchingWords.toDouble() / searchWords.size) * 300.0
        
        // Penalty for unwanted food types (-200 points)
        val unwantedTerms = listOf("soup", "sauce", "dressing", "supplement")
        if (unwantedTerms.any { description.contains(it) }) customScore -= 200.0
        
        // Penalty for overly processed foods (-50 points)
        if (description.length > 50) customScore -= 50.0
        
        Pair(food, customScore)
    }
    
    // Quality threshold: Only accept scores >= 50.0
    val bestResult = scoredResults.maxByOrNull { it.second }
    return if (bestResult?.second >= 50.0) bestResult.first else null
}
```

#### Alternative Search Strategies
```kotlin
private fun generateAlternativeSearchTerms(foodName: String): List<String> {
    return when {
        foodName.contains("pad thai") -> listOf("thai noodles", "rice noodles", "stir fry noodles")
        foodName.contains("burger") -> listOf("hamburger", "beef patty", "ground beef")
        foodName.contains("taco") -> listOf("mexican food", "tortilla", "ground beef")
        // ... more mappings
    }
}
```

#### Real-World Performance Improvement
```
After Enhancement:
Search: "Pad Thai"
1. Initial: "pad thai" -> "SMART SOUP, Thai Coconut Curry" (score: -16.8) -> REJECTED
2. Alternative: "thai noodles" -> Found better options
3. Alternative: "rice noodles" -> "Rice noodles, cooked" (score: 58.4) -> ACCEPTED
Result: 108 kcal/100g (reasonable for rice noodles)
User gets: 130 kcal for 1 serving (properly scaled)
```

#### Live Progress Updates Integration
```kotlin
// In EnhancedNutrientDbHelper.kt
val nutrients = enhancedNutrientDbHelper.lookup(
    food = item.food, 
    qty = item.quantity, 
    unit = item.unit,
    onUsdaFallback = { foodName ->
        val usdaProgress = "Local miss for '${foodName}' - trying USDA API..."
        progress = progress.updateStep("Looking Up Nutrition", AnalysisStep.StepStatus.IN_PROGRESS, details = usdaProgress)
        onProgress?.invoke(progress)
    }
)
```

#### Current Limitations & Future Improvements
**Current Issues**:
- Still not perfect - "Rice noodles, cooked" vs actual "Pad Thai" with sauce/protein
- Limited alternative search terms (only covers common foods)
- No learning from user corrections yet
- Scoring algorithm parameters are hardcoded

**Planned Improvements**:
1. **Food Composition Analysis**: Break down compound foods (Pad Thai = rice noodles + sauce + protein)
2. **Machine Learning Scoring**: Train model on user feedback for better matching
3. **Restaurant-Specific Mappings**: Direct USDA searches for known restaurant items
4. **Nutritional Profile Validation**: Reject results with unrealistic calorie ranges
5. **User Feedback Loop**: Learn from "This is wrong" corrections

### Hardware Acceleration Strategy

#### Golden Path: PlayServicesAccelerationService
```kotlin
val validatedConfig = playServicesAcceleration.getValidatedAccelerationConfig(modelPath)
if (validatedConfig != null) {
    // Use AccelerationService for optimal NPU/GPU selection
    val options = LlmInference.LlmInferenceOptions.builder()
        .setPreferredBackend(LlmInference.Backend.GPU)
        .build()
}
```

#### Fallback Path: Manual Detection
```kotlin
val accelerationResult = playServicesAcceleration.findOptimalAcceleration(modelPath)
val preferredBackend = when {
    accelerationResult.gpuAvailable -> LlmInference.Backend.GPU
    else -> LlmInference.Backend.CPU
}
```

### Performance Optimization Techniques

#### Session Pre-warming
```kotlin
// Background pre-warming eliminates cold start delays
private suspend fun prewarmSessionAsync() {
    val session = LlmInferenceSession.createFromOptions(inference, options)
    prewarmedSession = session // Ready for instant use
}
```

#### Memory Management
```kotlin
// Proper MediaPipe resource cleanup
try {
    session.close()
    appContainer.onSessionClosed() // Triggers new session pre-warming
} catch (e: Exception) {
    Log.e(TAG, "Failed to close session", e)
}
```

## Project Status & Roadmap

### ✅ **Completed Features**

#### Core AI Integration
- [x] **MediaPipe GenAI integration** with Gemma 3n models
- [x] **Hardware acceleration detection** (Golden Path + Fallback)
- [x] **Session pre-warming system** for responsive UX
- [x] **Multi-model support** (E2B fast, E4B accurate)
- [x] **Progressive model downloads** with resume capability
- [x] **Context-aware prompt engineering** with restaurant hints

#### Three-Path UI System  
- [x] **Home screen mode selection** with clear user paths
- [x] **Quick Snap mode** (SNAP_AND_LOG) with strict JSON prompts
- [x] **Enhanced Chat interface** for conversational analysis
- [x] **Text-only mode** for accessibility and no-camera scenarios
- [x] **Real-time progress tracking** during AI analysis
- [x] **Editable results interface** with add/remove/modify capabilities

#### Data Management
- [x] **Hybrid nutrition database** (Local SQLite + USDA API)
- [x] **Comprehensive food data** including restaurant chains (Chipotle)
- [x] **Intelligent serving size conversion** and unit standardization
- [x] **Advanced nutrition metrics** (glycemic index, dietary fiber)
- [x] **Android Health Connect integration** with automatic meal logging
- [x] **Enhanced USDA API search accuracy** with intelligent food matching
- [x] **Live progress updates** during nutrition lookup with USDA fallback notifications
- [x] **Advanced scoring algorithm** to reject poor food matches and find better alternatives

#### User Experience
- [x] **Material 3 design system** with consistent theming
- [x] **Comprehensive error handling** with user-friendly messages
- [x] **Performance monitoring** with detailed acceleration statistics
- [x] **User feedback collection** system for continuous improvement
- [x] **Settings management** for model selection and preferences

### 🚧 **Currently In Development**

#### Enhanced USDA API Integration (Recently Completed)
- [x] **Advanced food matching algorithm** with custom scoring system
  - **Status**: ✅ COMPLETED - Successfully implemented
  - **Impact**: Rejects poor matches like "Thai Coconut Curry Soup" for "Pad Thai" searches
  - **Performance**: Now finds "Rice noodles, cooked" (108 kcal/100g) instead of soup (36 kcal/100g)
  - **Features**: Alternative search terms, quality thresholds, real-time progress updates

- [x] **Live nutrition lookup progress notifications**
  - **Status**: ✅ COMPLETED - Working in production
  - **Features**: "Local miss for 'Pad Thai' - trying USDA API..." real-time updates
  - **Integration**: Seamlessly integrated with existing AnalysisProgress system

#### Enhanced AI Agent Capabilities
- [ ] **Real Function Calling SDK integration** (when AI-Edge FC SDK matures)
  - **Priority**: High
  - **Next Steps**: 
    1. Monitor AI-Edge Function Calling SDK release updates
    2. Implement `getNutritionFromDB()` and `requestUserInput()` functions
    3. Replace simulated function calling with real SDK integration
  - **Milestone**: Q2 2025 (estimated based on Google AI-Edge roadmap)

#### Advanced Conversation Management
- [ ] **Multi-turn conversation persistence** 
  - **Priority**: Medium
  - **Next Steps**:
    1. Implement conversation history storage
    2. Add conversation context management across sessions
    3. Enable resume conversations after app restart
  - **Milestone**: Next major release

#### Improved Error Recovery
- [ ] **Smart failure-to-success pipeline** 
  - **Priority**: High
  - **Next Steps**:
    1. Implement automatic escalation from SNAP_AND_LOG to ANALYZE_AND_CHAT
    2. Add "I need help with this" dialog with seamless transition
    3. Context preservation across mode switches
  - **Milestone**: 2 weeks

### 📋 **Feature Queue (Priority Order)**

#### High Priority (Next Sprint)
1. **USDA API Food Composition Analysis** (Follow-up to recent improvements)
   - Break down compound foods (Pad Thai = rice noodles + sauce + protein + vegetables)
   - Implement nutritional profile validation (reject results with unrealistic calorie ranges)
   - Add machine learning scoring based on user feedback patterns
   - **Current Issue**: Still getting "Rice noodles, cooked" instead of complete "Pad Thai" nutrition

2. **Automatic Mode Escalation**
   - SNAP_AND_LOG failures -> ANALYZE_AND_CHAT with preserved image  
   - Smart detection of when clarification is needed
   - Seamless UX transition with explanatory messaging

3. **Enhanced Nutrition Database Expansion**
   - Additional restaurant chain integrations (McDonald's, Subway, etc.)
   - Improved portion size estimation algorithms
   - User feedback loop for "This is wrong" corrections
   - **New**: Database caching issue fix (SQLite FTS5 module error)

3. **Performance Optimizations**
   - Implement true text-only sessions (when MediaPipe supports it)
   - Advanced session pooling with different session types
   - Background model switching without UI interruption

#### Medium Priority (Next Release)
4. **Advanced Health Integration**
   - Health Connect read capabilities for nutrition trends
   - Meal timing optimization based on user patterns
   - Integration with fitness tracking data

5. **User Experience Enhancements**
   - Voice input for hands-free meal logging
   - Batch photo analysis for multiple meals
   - Smart meal suggestions based on nutrition goals

6. **Developer Experience**
   - Comprehensive testing suite for AI analysis accuracy
   - Model performance benchmarking dashboard  
   - A/B testing framework for prompt optimization

#### Low Priority (Future Releases)
7. **Advanced AI Features**
   - RAG integration for personalized nutrition recommendations
   - Multi-language support for international foods
   - Custom model fine-tuning for user-specific preferences

8. **Platform Expansion**
   - iOS version with shared AI processing logic
   - Web companion app for detailed nutrition analysis
   - Integration with smart kitchen appliances

### 🎯 **Kaggle Competition Readiness**

#### Submission Requirements Status
- [x] **On-device AI demonstration** - Gemma 3n running locally
- [x] **Edge optimization showcase** - Hardware acceleration implementation  
- [x] **Real-world application** - Practical nutrition tracking use case
- [x] **Performance metrics** - Detailed acceleration and inference statistics
- [x] **Code documentation** - Comprehensive architecture documentation
- [x] **Demo video preparation** - All three paths working smoothly

#### Competition Highlights
- **"Golden Path" acceleration** - Showcases optimal hardware utilization
- **Multi-modal AI integration** - Vision + text processing capabilities
- **Intelligent failure handling** - Graceful degradation and recovery
- **Production-ready architecture** - Scalable, maintainable codebase

## Getting Started Guide

### 🏃‍♂️ **Quick Start for Fresh Claude Instances**

#### 1. Project Context Understanding
```bash
# First, read the project architecture document
read: /ProjectPlanning/ProjectArchitect.md

# Then read this comprehensive README
read: /ProjectPlanning/CLAUDE-README.md

# Key entry points to examine:
- app/src/main/java/com/stel/gemmunch/AppContainer.kt
- app/src/main/java/com/stel/gemmunch/agent/PhotoMealExtractor.kt  
- app/src/main/java/com/stel/gemmunch/ui/screens/HomeScreen.kt
```

#### 2. AI Integration Understanding
```kotlin
// Core AI flow - examine this pattern:
1. AppContainer.initialize() -> Sets up MediaPipe + models
2. AppContainer.getReadyVisionSession() -> Returns pre-warmed session
3. PhotoMealExtractor.extract() -> Complete analysis pipeline
4. Session cleanup -> Triggers new session pre-warming
```

#### 3. Three-Path Navigation
```kotlin
// Navigation structure:
HomeScreen -> Mode Selection
├── "camera/singleshot" -> CameraFoodCaptureScreen (SNAP_AND_LOG)
├── "chat/true" -> EnhancedChatScreen (ANALYZE_AND_CHAT)  
└── "chat/false" -> EnhancedChatScreen (TEXT_ONLY)
```

### 🛠️ **Development Environment Setup**

#### Required Dependencies
```toml
# Ensure these versions in libs.versions.toml:
mediapipeTasksGenai = "0.10.25"      # Core AI inference
googleAiEdgeAicore = "0.0.1-exp02"   # Gemini Nano support
```

#### Build Configuration
```kotlin
// Key build.gradle.kts settings:
compileSdk = 36
minSdk = 35  // Required for Health Connect
targetSdk = 36

// Essential features:
buildFeatures {
    compose = true
    buildConfig = true  // For API keys
}
```

#### API Keys Setup
```properties
# gradle.properties (create if missing):
HF_TOKEN=your_huggingface_token_here
USDA_API_KEY=your_usda_api_key_here
```

### 🧪 **Testing & Validation**

#### Model Verification
```kotlin
// Check model availability:
1. Examine ModelRegistry.getAllModels()
2. Verify HuggingFace URLs are accessible
3. Test model download in SetupScreen
4. Confirm MediaPipe integration in AppContainer
```

#### AI Analysis Testing
```kotlin
// Test the complete pipeline:
1. Capture test image -> CameraFoodCaptureScreen
2. Monitor analysis progress -> AnalysisProgress logs
3. Verify JSON parsing -> PhotoMealExtractor logs  
4. Check nutrition lookup -> EnhancedNutrientDbHelper
5. Validate Health Connect -> HealthConnectManager
```

## Tips, Tricks & Warnings

### ⚠️ **Critical Warnings**

#### MediaPipe Session Management
```kotlin
// ❌ NEVER do this - causes memory leaks:
val session = llmInference.createSession()
// Use session...
// Forget to close session

// ✅ ALWAYS do this - proper resource management:
val session = appContainer.getReadyVisionSession()
try {
    // Use session for analysis
} finally {
    session.close()
    appContainer.onSessionClosed() // Triggers new pre-warming
}
```

#### Model File Handling
```kotlin
// ❌ Don't assume model files exist:
val modelFile = File(modelPath) 
val inference = LlmInference.createFromFile(modelFile) // May crash

// ✅ Always validate model availability:
val modelFile = modelFiles[selectedModel] ?: throw IllegalStateException()
if (!modelFile.exists()) { /* Handle missing model */ }
```

#### JSON Response Parsing
```kotlin
// ❌ Fragile parsing - AI may return descriptive text:
val foods = JSONArray(aiResponse) // Will crash on non-JSON

// ✅ Robust parsing with extraction:
val jsonPart = extractJsonFromResponse(aiResponse)
val foods = JSONObject(jsonPart).getJSONArray("foods")
```

### 💡 **Performance Tips**

#### Session Pre-warming Strategy
```kotlin
// Pre-warm sessions based on user navigation:
sessionManager.prewarmForDestination("camera/singleshot")

// Monitor session status for optimal UX:
when (modelStatus) {
    ModelStatus.PREPARING_SESSION -> showPreparingIndicator()
    ModelStatus.READY -> enableAnalysisButton()
    ModelStatus.RUNNING_INFERENCE -> showProgressIndicator()
}
```

#### Memory Management
```kotlin
// Clear unnecessary bitmap references:
analyzedBitmap?.recycle()
analyzedBitmap = null

// Use appropriate bitmap scaling:
val scaledBitmap = Bitmap.createScaledBitmap(original, 1024, 1024, true)
```

#### Acceleration Optimization
```kotlin
// Monitor acceleration statistics:
val stats = appContainer.accelerationStats.value
if (stats?.confidence < 0.8) {
    // Consider model switching or CPU fallback
}

// Log performance for optimization:
Log.i(TAG, "Inference time: ${metrics.llmInference}ms")
```

### 🎯 **Development Best Practices**

#### Prompt Engineering
```kotlin
// Be specific about output format:
"Output ONLY a JSON array with this exact format: [...]"

// Provide concrete examples:
"Example: [{\"food\": \"taco\", \"quantity\": 3, \"unit\": \"item\"}]"

// Handle edge cases explicitly:
"If you cannot identify any food, return: []"
```

#### Error Handling Philosophy
```kotlin
// Always fail gracefully:
try {
    val result = performAIAnalysis()
    return Success(result)
} catch (e: Exception) {
    return Success(ErrorResult(userFriendlyMessage))
    // Note: Still return Success so user can provide feedback
}
```

#### UI State Management
```kotlin
// Use sealed interfaces for clear state representation:
sealed interface FoodCaptureState {
    data object Idle : FoodCaptureState
    data class Loading(val progress: AnalysisProgress) : FoodCaptureState
    data class Success(val analysis: MealAnalysis) : FoodCaptureState
}
```

### 🔧 **Debugging Techniques**

#### AI Analysis Debugging
```kotlin
// Enable detailed logging:
Log.d(TAG, "Raw AI Response: $llmResponse")
Log.d(TAG, "Parsed JSON: $cleanJson")
Log.d(TAG, "Identified foods: $foodItems")

// Monitor performance metrics:
Log.i(TAG, "=== Performance Breakdown ===")
Log.i(TAG, "LLM Inference: ${timings["LLM Inference"]}ms")
Log.i(TAG, "Total Time: ${totalTime}ms")
```

#### USDA API Enhanced Debugging (New)
```kotlin
// Monitor USDA search quality:
Log.d(TAG, "Found ${allResults.size} USDA results for '$foodName':")
allResults.take(3).forEach { food ->
    Log.d(TAG, "  - '${food.description}' (score: ${food.score}, type: ${food.dataType})")
}

// Track custom scoring decisions:
Log.d(TAG, "Top candidates for '$searchTerm':")
topCandidates.forEach { (food, score) ->
    Log.d(TAG, "  - '${food.description}' (custom score: %.1f, USDA score: %.1f)".format(score, food.score))
}

// Monitor quality threshold decisions:
Log.w(TAG, "Best match score (${bestResult?.second ?: "null"}) below minimum threshold ($minimumAcceptableScore). Rejecting poor match.")

// Track alternative search attempts:
Log.d(TAG, "Trying alternative search: '$altTerm' for original term '$originalTerm'")
Log.i(TAG, "Alternative search '$altTerm' found suitable match: '${bestMatch.description}'")
```

#### Real-World USDA API Log Examples
```kotlin
// Successful enhancement example:
D  Found 1 USDA results for 'pad thai':
D    - 'SMART SOUP, Thai Coconut Curry' (score: 331.79303, type: SR Legacy)
D  Top candidates for 'pad thai':
D    - 'SMART SOUP, Thai Coconut Curry' (custom score: -16.8, USDA score: 331.8)
W  Best match score (-16.8) below minimum threshold (50.0). Rejecting poor match.
D  Trying alternative search: 'rice noodles' for original term 'pad thai'
D  Top candidates for 'pad thai':
D    - 'Rice noodles, cooked' (custom score: 58.4, USDA score: 583.8)
D  Accepting match: 'Rice noodles, cooked' with score 58.383044
I  Alternative search 'rice noodles' found suitable match: 'Rice noodles, cooked'
I  USDA lookup successful for 'pad thai': 108 kcal/100g
I  USDA HIT: 'Pad Thai' -> 130 kcal (cached for future)
```

#### Session State Monitoring
```kotlin
// Track session lifecycle:
Log.d(TAG, "⚡ Using pre-warmed session #$sessionNum")
Log.d(TAG, "🔄 Creating session on-demand...")
Log.d(TAG, "✅ Session closed successfully")
Log.d(TAG, "🚀 Started continuous session pre-warming")
```

#### Hardware Acceleration Debugging
```kotlin
// Monitor acceleration path:
when {
    validatedConfig != null -> Log.i(TAG, "🚀 Using AccelerationService (Golden Path)")
    accelerationResult.gpuAvailable -> Log.i(TAG, "⚡ Using manual GPU detection")
    else -> Log.w(TAG, "🐌 Falling back to CPU")
}
```

### 📱 **Device Compatibility**

#### Pixel Device Optimization
```kotlin
// Detect Tensor chip for NPU optimization:
private fun isPixelDevice(): Boolean {
    val model = Build.MODEL.lowercase()
    return model.contains("pixel") && (
        model.contains("6") || model.contains("7") || 
        model.contains("8") || model.contains("9")
    )
}
```

#### Memory Requirements
```kotlin
// Minimum system requirements:
- Android API 35+ (for Health Connect)
- 4GB RAM (for Gemma 3n models)
- 2GB storage (for model files)
- OpenGL ES 3.1+ (for GPU acceleration)
```

#### Performance Expectations
```kotlin
// Typical inference times:
- Pixel with NPU: 2-8 seconds
- High-end GPU: 5-15 seconds  
- Mid-range GPU: 10-25 seconds
- CPU fallback: 30-60 seconds
```

---

## 🎉 **Ready to Contribute!**

With this comprehensive documentation, any fresh Claude instance should be able to:

1. **Understand the complete architecture** - From UI to AI to data layers
2. **Navigate the codebase confidently** - Know exactly where to find components
3. **Continue development momentum** - Pick up any feature or bug fix
4. **Maintain code quality** - Follow established patterns and best practices
5. **Debug effectively** - Use proper logging and monitoring techniques

**Remember**: This is a Kaggle competition entry showcasing **on-device AI excellence**. Every feature should demonstrate the power and efficiency of running Gemma 3n locally with optimal hardware acceleration.

---

## 🎉 **Recent Major Enhancements Summary**

### USDA API Search Accuracy Revolution (Just Completed! ✨)

**The Problem We Solved:**
```
❌ Before: "Pad Thai" → "SMART SOUP, Thai Coconut Curry" (36 kcal/100g)
   - Wrong food type (soup vs noodles)
   - Completely inaccurate nutrition data
   - User frustration with poor results
```

**The Solution We Implemented:**
```
✅ After: "Pad Thai" → "Rice noodles, cooked" (108 kcal/100g)
   - Custom scoring algorithm rejects poor matches
   - Alternative search strategies find better options
   - Live progress updates keep users informed
   - Significantly improved accuracy
```

**Technical Achievements:**
- **Advanced Scoring System**: Custom algorithm that evaluates food matches based on relevance, not just USDA's raw scores
- **Quality Thresholds**: Automatic rejection of matches below acceptable standards (50+ points)
- **Alternative Search Strategies**: Intelligent fallback searches when initial attempts fail
- **Real-time Progress Updates**: Users see exactly what's happening during nutrition lookup
- **Food-Specific Intelligence**: Context-aware search terms (Pad Thai → thai noodles → rice noodles)

**Impact on User Experience:**
- **Immediate**: No more completely wrong food results
- **Long-term**: Building foundation for machine learning improvements
- **Performance**: Maintains fast response times with multiple API calls when needed
- **Transparency**: Users understand when and why USDA API is being used

**Still Room for Improvement:**
- Working on compound food analysis (Pad Thai = noodles + sauce + protein)
- Planning user feedback integration for continuous learning
- Expanding alternative search term database
- Implementing nutritional profile validation

This enhancement represents a significant step forward in making GemMunch a truly reliable nutrition tracking application. The foundation is now in place for even more sophisticated food matching capabilities.

**Happy coding! 🚀**