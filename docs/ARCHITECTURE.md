# GemYum Architecture Overview

## System Architecture

GemYum is built with a layered architecture that emphasizes separation of concerns, testability, and maintainability.

```
┌─────────────────────────────────────────┐
│           Presentation Layer            │
│         (Jetpack Compose UI)            │
├─────────────────────────────────────────┤
│          ViewModel Layer                │
│    (State Management & Business Logic)  │
├─────────────────────────────────────────┤
│           Domain Layer                  │
│    (Use Cases & Business Rules)         │
├─────────────────────────────────────────┤
│            Data Layer                   │
│  (Repositories, APIs, Local Storage)    │
├─────────────────────────────────────────┤
│         Infrastructure Layer            │
│    (AI Models, Database, Hardware)      │
└─────────────────────────────────────────┘
```

## Core Components

### 1. AppContainer (Dependency Injection)

The `AppContainer` serves as our manual dependency injection container, providing singleton instances of core services:

```kotlin
interface AppContainer {
    val photoMealExtractor: PhotoMealExtractor
    val nutritionSearchService: NutritionSearchService
    val healthConnectManager: HealthConnectManager
    val modelStatus: StateFlow<ModelStatus>
    
    fun initialize(modelFiles: Map<String, File>)
    suspend fun getReadySession(): LlmInferenceSession
}
```

### 2. AI Integration Layer

#### Model Management
- **LlmInference**: MediaPipe's inference engine
- **LlmInferenceSession**: Stateful session for conversations
- **SessionManager**: Pools and pre-warms sessions for instant response

#### Key Innovations:
- **Session Pre-warming**: Background session creation eliminates cold starts
- **Hardware Acceleration**: Automatic NPU/GPU/CPU fallback chain
- **Memory Management**: Aggressive cleanup and recycling

### 3. UI Layer (Jetpack Compose)

#### Screen Structure:
```
MainActivity
├── HomeScreen (Mode Selection)
├── CameraFoodCaptureScreen (Quick Snap)
├── EnhancedChatScreen (Deep Chat)
├── TextOnlyMealScreen (Text Input)
└── NutrientDBScreen (Database Search)
```

#### State Management:
- ViewModels manage UI state
- StateFlow for reactive updates
- Coroutines for async operations

### 4. Data Layer

#### Local Storage:
- **SQLite Database**: 700,000+ foods with FTS5 search
- **SharedPreferences**: User settings and preferences
- **File Storage**: Cached images and model files

#### Data Flow:
```
User Input → ViewModel → AI Model → Analysis → Database Lookup → UI Update
```

## AI Pipeline

### 1. Image Analysis Flow
```
Camera/Gallery
    ↓
Image Preprocessing (Bitmap scaling)
    ↓
MediaPipe Image Creation
    ↓
Gemma 3n Vision Model
    ↓
JSON Extraction
    ↓
Nutrition Database Lookup
    ↓
Results Display
```

### 2. Chat Flow
```
User Message + Optional Image
    ↓
Prompt Engineering
    ↓
Session Management
    ↓
Token-by-Token Streaming
    ↓
UI Updates (Real-time)
    ↓
Response Completion
```

### 3. Text-Only Flow
```
Text Input
    ↓
RAG Retrieval (SQL-based)
    ↓
Context Enhancement
    ↓
Rule-based Analysis
    ↓
Nutrition Calculation
```

## Performance Optimizations

### 1. Model Loading
- Models cached in app's private storage
- Memory-mapped for fast access
- Lazy initialization on first use

### 2. Inference Optimization
- INT4 quantization reduces model size by 75%
- NPU acceleration provides 10-20x speedup
- Batch processing for multiple foods

### 3. UI Responsiveness
- Streaming responses prevent UI blocking
- Background processing with coroutines
- Predictive pre-loading of common screens

## Security Considerations

### 1. Data Privacy
- No network calls for inference
- No telemetry or analytics
- All data stored locally

### 2. Model Security
- Models validated with checksums
- Secure storage in app's private directory
- No model updates without user consent

### 3. Input Validation
- Image size limits prevent memory issues
- Text input sanitization
- Timeout protection for long operations

## Hardware Abstraction

### Acceleration Detection:
```kotlin
class PlayServicesAccelerationService {
    fun detectBestHardware(): HardwareType {
        return when {
            hasNPU() -> HardwareType.NPU
            hasGPU() -> HardwareType.GPU
            else -> HardwareType.CPU
        }
    }
}
```

### Fallback Chain:
1. Try NPU (fastest)
2. Fall back to GPU
3. Fall back to CPU (always works)

## Database Architecture

### Schema Overview:
```sql
CREATE TABLE foods (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    brand TEXT,
    calories REAL,
    protein REAL,
    carbs REAL,
    fat REAL,
    -- 220+ more nutrient columns
);

CREATE VIRTUAL TABLE foods_fts USING fts5(
    name, brand, content=foods
);
```

### Query Optimization:
- FTS5 for instant text search
- Indexed columns for common queries
- Prepared statements for performance

## Testing Strategy

### 1. Unit Tests
- ViewModels tested with mock dependencies
- Business logic tested in isolation
- Database queries tested with in-memory DB

### 2. Integration Tests
- UI flows tested with Compose test rules
- AI pipeline tested with sample images
- End-to-end scenarios validated

### 3. Performance Tests
- Inference time benchmarking
- Memory usage monitoring
- Database query profiling

## Future Architecture Considerations

### 1. Modularity
- Extract AI logic to separate module
- Create nutrition-database library
- Build reusable UI components library

### 2. Scalability
- Support for multiple AI models
- Plugin architecture for new features
- Cloud sync option (opt-in only)

### 3. Cross-Platform
- Kotlin Multiplatform for iOS
- Shared business logic
- Platform-specific UI

## Key Design Decisions

1. **Manual DI over Dagger/Hilt**: Simpler, faster compile times, easier to understand
2. **Coroutines over RxJava**: Modern, lightweight, better Compose integration
3. **StateFlow over LiveData**: Kotlin-first, better testing, type safety
4. **Compose over XML**: Modern UI, better performance, easier maintenance
5. **On-device over Cloud**: Privacy, speed, offline capability

## Performance Metrics

| Component | Target | Actual |
|-----------|--------|--------|
| App Startup | <3s | 2.5s |
| Model Init (first) | <3min | 2-3min |
| Model Init (cached) | <30s | 10-30s |
| Inference Time | <1s | 0.8s |
| Memory Usage | <2GB | 1.5GB |
| Database Query | <500ms | 200ms |

## Conclusion

GemYum's architecture prioritizes:
- **Privacy**: On-device processing
- **Performance**: Hardware acceleration
- **Reliability**: Offline capability
- **Maintainability**: Clean architecture
- **User Experience**: Responsive UI

This architecture enables us to deliver a production-ready app that respects user privacy while providing cutting-edge AI capabilities.