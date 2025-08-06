# GemMunch RAG Implementation Plan
## Retrieval-Augmented Generation for Enhanced Food Recognition

### Executive Summary
Implement RAG to improve GemMunch's food recognition accuracy by combining visual embeddings, contextual retrieval, and Gemma's generation capabilities. This will enable better food identification, portion estimation, and nutrition lookup - especially for ambiguous or complex dishes.

---

## 1. Current Architecture Analysis

### Existing Components
- **PhotoMealExtractor**: Direct Gemma inference on images
- **EnhancedNutrientDbHelper**: SQLite-based nutrition lookup
- **Local Database**: 716 foods with nutrition data
- **Gemma Model**: On-device vision-language model

### Current Limitations
1. **No Context**: Gemma analyzes images in isolation
2. **Binary Matching**: Exact string match or fuzzy search in DB
3. **No Learning**: System doesn't improve from past recognitions
4. **Limited Portions**: Generic portion sizes (e.g., "1 taco" = 75g)
5. **Missing Foods**: Falls back to USDA API or returns 0 calories

---

## 2. Proposed RAG Architecture

### Core Components

```
┌─────────────────┐     ┌──────────────┐     ┌─────────────┐
│   Image Input   │────▶│   Embedder   │────▶│ Vector Store│
└─────────────────┘     └──────────────┘     └─────────────┘
                               │                      │
                               ▼                      ▼
                        ┌──────────────┐     ┌──────────────┐
                        │  Gemma LLM   │◀────│   Retriever  │
                        └──────────────┘     └──────────────┘
                               │
                               ▼
                        ┌──────────────┐
                        │   Enhanced   │
                        │   Response   │
                        └──────────────┘
```

### Key Additions
1. **Image Embedder**: MediaPipe's MobileNet/EfficientNet for food features
2. **Vector Database**: SQLite with vector similarity extensions
3. **Retrieval Pipeline**: Multi-stage retrieval (visual → semantic → nutritional)
4. **Context Injector**: Augments Gemma prompts with retrieved knowledge

---

## 3. Implementation Phases

### Phase 1: Foundation (Week 1)
**Goal**: Set up embedding infrastructure

#### Tasks:
1. Add MediaPipe image embedding model
2. Extend SQLite schema for vector storage
3. Create embedding generation pipeline
4. Build similarity search functions

#### Code Structure:
```kotlin
// New package structure
com.stel.gemmunch.rag/
├── embeddings/
│   ├── FoodImageEmbedder.kt
│   ├── TextEmbedder.kt
│   └── EmbeddingCache.kt
├── retrieval/
│   ├── VectorStore.kt
│   ├── SimilaritySearch.kt
│   └── RetrievalPipeline.kt
└── augmentation/
    ├── ContextBuilder.kt
    └── RAGPhotoExtractor.kt
```

### Phase 2: Data Preparation (Week 2)
**Goal**: Build comprehensive food embedding database

#### Tasks:
1. Generate embeddings for existing 716 foods
2. Collect reference images for common foods
3. Create portion size visual references
4. Build food category hierarchies

#### Embedding Schema:
```sql
-- Enhanced database schema
CREATE TABLE food_embeddings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    food_id INTEGER NOT NULL,
    embedding_type TEXT NOT NULL, -- 'visual', 'textual', 'nutritional'
    embedding BLOB NOT NULL,       -- 512-dim float array
    metadata TEXT,                 -- JSON metadata
    confidence REAL DEFAULT 1.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (food_id) REFERENCES foods(id)
);

CREATE TABLE food_visual_features (
    food_id INTEGER PRIMARY KEY,
    primary_colors TEXT,           -- JSON array of dominant colors
    texture_descriptor TEXT,       -- 'smooth', 'chunky', 'layered', etc.
    shape_descriptor TEXT,         -- 'round', 'rectangular', 'irregular'
    typical_presentation TEXT,     -- 'plated', 'bowl', 'wrapped', 'handheld'
    portion_visual_cues TEXT,      -- JSON of visual size references
    FOREIGN KEY (food_id) REFERENCES foods(id)
);

-- Similarity index for fast retrieval
CREATE INDEX idx_embeddings_type ON food_embeddings(embedding_type);
```

### Phase 3: Retrieval Implementation (Week 3)
**Goal**: Build multi-modal retrieval system

#### Components:

```kotlin
class MultiModalRetriever(
    private val vectorStore: VectorStore,
    private val dbHelper: EnhancedNutrientDbHelper
) {
    suspend fun retrieve(
        imageEmbedding: FloatArray,
        textContext: String? = null,
        topK: Int = 5
    ): RetrievalResult {
        // Stage 1: Visual similarity
        val visualMatches = vectorStore.searchSimilar(
            embedding = imageEmbedding,
            type = "visual",
            limit = topK * 2
        )
        
        // Stage 2: Semantic filtering (if context provided)
        val semanticMatches = textContext?.let {
            filterBySemanticSimilarity(visualMatches, it)
        } ?: visualMatches
        
        // Stage 3: Nutritional clustering
        val nutritionalGroups = clusterByNutrition(semanticMatches)
        
        return RetrievalResult(
            topMatches = semanticMatches.take(topK),
            nutritionalVariants = nutritionalGroups,
            confidenceScore = calculateConfidence(semanticMatches)
        )
    }
}
```

### Phase 4: Gemma Integration (Week 4)
**Goal**: Enhance Gemma with retrieval context

#### Enhanced Prompt Strategy:
```kotlin
class RAGPromptBuilder {
    fun buildEnhancedPrompt(
        retrievalResult: RetrievalResult,
        userContext: String? = null
    ): String {
        return """
        You are analyzing a food image with the following context:
        
        SIMILAR FOODS IDENTIFIED:
        ${formatSimilarFoods(retrievalResult.topMatches)}
        
        TYPICAL PORTIONS:
        ${formatPortionInfo(retrievalResult.topMatches)}
        
        NUTRITIONAL RANGE:
        ${formatNutritionRange(retrievalResult.nutritionalVariants)}
        
        ${userContext?.let { "USER CONTEXT: $it" } ?: ""}
        
        Based on this context, identify the specific food items in the image.
        Be specific about portions based on the visual cues and similar foods.
        
        Output JSON format:
        [{"food": "name", "quantity": number, "unit": "unit", "confidence": 0.0-1.0}]
        """
    }
}
```

---

## 4. Technical Implementation Details

### A. Embedding Generation

```kotlin
class FoodImageEmbedder(private val context: Context) {
    private lateinit var embedder: ImageEmbedder
    
    suspend fun initialize() = withContext(Dispatchers.IO) {
        val options = ImageEmbedder.ImageEmbedderOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("mobilenet_v3_embedder.tflite")
                    .setDelegate(Delegate.GPU) // Use GPU acceleration
                    .build()
            )
            .setQuantize(true) // Quantize embeddings for storage
            .setL2Normalize(true) // Normalize for cosine similarity
            .build()
            
        embedder = ImageEmbedder.createFromOptions(context, options)
    }
    
    fun generateEmbedding(bitmap: Bitmap): FloatArray {
        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = embedder.embed(mpImage)
        return result.embeddingResult().embeddings(0).floatArray()
    }
}
```

### B. Vector Similarity Search

```kotlin
class VectorStore(private val db: SQLiteDatabase) {
    
    fun searchSimilar(
        embedding: FloatArray,
        type: String = "visual",
        limit: Int = 5
    ): List<SimilarFood> {
        // Convert embedding to byte array for storage
        val embeddingBytes = embedding.toByteArray()
        
        // Cosine similarity using SQL (simplified - real impl would use extension)
        val query = """
            SELECT 
                f.name,
                f.calories,
                f.glycemic_index,
                fe.metadata,
                cosine_similarity(fe.embedding, ?) as similarity
            FROM food_embeddings fe
            JOIN foods f ON fe.food_id = f.id
            WHERE fe.embedding_type = ?
            ORDER BY similarity DESC
            LIMIT ?
        """
        
        val cursor = db.rawQuery(query, arrayOf(embeddingBytes, type, limit))
        return cursor.use { parseSimilarFoods(it) }
    }
    
    private fun cosine_similarity(a: FloatArray, b: FloatArray): Float {
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        return dotProduct / (sqrt(normA) * sqrt(normB))
    }
}
```

### C. Confidence Scoring

```kotlin
class ConfidenceCalculator {
    fun calculate(
        visualSimilarity: Float,
        hasTextualMatch: Boolean,
        nutritionalConsistency: Float
    ): Float {
        val weights = mapOf(
            "visual" to 0.5f,
            "textual" to 0.3f,
            "nutritional" to 0.2f
        )
        
        var score = visualSimilarity * weights["visual"]!!
        
        if (hasTextualMatch) {
            score += weights["textual"]!!
        }
        
        score += nutritionalConsistency * weights["nutritional"]!!
        
        return score.coerceIn(0f, 1f)
    }
}
```

---

## 5. Data Collection Strategy

### Reference Image Collection
1. **Common Foods**: 100 high-quality reference images
2. **Multiple Angles**: 3-5 views per food item
3. **Portion Variations**: Small/medium/large examples
4. **Presentation Styles**: Restaurant vs. home-cooked

### Embedding Pre-computation
```python
# Python script for batch embedding generation
import sqlite3
import numpy as np
from mediapipe.tasks import python
from mediapipe.tasks.python import vision

def generate_food_embeddings():
    # Initialize embedder
    base_options = python.BaseOptions(
        model_asset_path='mobilenet_v3.tflite'
    )
    options = vision.ImageEmbedderOptions(
        base_options=base_options,
        quantize=True,
        l2_normalize=True
    )
    embedder = vision.ImageEmbedder.create_from_options(options)
    
    # Process each food with reference image
    conn = sqlite3.connect('nutrients.db')
    cursor = conn.cursor()
    
    foods = cursor.execute("SELECT id, name FROM foods").fetchall()
    
    for food_id, food_name in foods:
        # Generate text embedding from name
        text_embedding = generate_text_embedding(food_name)
        store_embedding(food_id, text_embedding, 'textual')
        
        # If reference image exists
        ref_image_path = f"reference_images/{food_id}.jpg"
        if os.path.exists(ref_image_path):
            image = mp.Image.create_from_file(ref_image_path)
            result = embedder.embed(image)
            visual_embedding = result.embeddings[0].embedding
            store_embedding(food_id, visual_embedding, 'visual')
```

---

## 6. Performance Optimizations

### Caching Strategy
```kotlin
class RAGCache {
    private val embeddingCache = LRUCache<String, FloatArray>(50)
    private val retrievalCache = LRUCache<String, RetrievalResult>(20)
    
    fun getCachedEmbedding(imageHash: String): FloatArray? {
        return embeddingCache[imageHash]
    }
    
    fun cacheRetrieval(key: String, result: RetrievalResult) {
        retrievalCache.put(key, result)
    }
}
```

### Batch Processing
```kotlin
class BatchEmbeddingProcessor {
    suspend fun processBatch(images: List<Bitmap>): List<FloatArray> {
        return withContext(Dispatchers.Default) {
            images.chunked(4).flatMap { batch ->
                batch.map { async { generateEmbedding(it) } }
                    .awaitAll()
            }
        }
    }
}
```

---

## 7. Testing & Validation

### Metrics to Track
1. **Recognition Accuracy**: % of correctly identified foods
2. **Portion Estimation**: Error rate in quantity estimates
3. **Retrieval Relevance**: Precision@K for similar foods
4. **Inference Speed**: Time from image to results
5. **Memory Usage**: RAM footprint with embeddings

### A/B Testing Plan
- **Control**: Current direct Gemma inference
- **Treatment**: RAG-enhanced inference
- **Metrics**: Accuracy, user satisfaction, processing time

---

## 8. Migration Path

### Step 1: Parallel Implementation
- Keep existing PhotoMealExtractor
- Add RAGPhotoExtractor as alternative
- Toggle via feature flag

### Step 2: Gradual Rollout
```kotlin
class PhotoExtractorFactory {
    fun getExtractor(useRAG: Boolean): PhotoExtractor {
        return if (useRAG && isRAGReady()) {
            RAGPhotoExtractor()
        } else {
            PhotoMealExtractor() // Fallback to original
        }
    }
}
```

### Step 3: Full Migration
- Monitor metrics for 2 weeks
- If RAG performs better, make default
- Keep fallback for edge cases

---

## 9. Future Enhancements

### Advanced Features
1. **User Personalization**: Learn from user's eating patterns
2. **Restaurant Matching**: Match to specific restaurant items
3. **Recipe Detection**: Identify ingredients in complex dishes
4. **Meal Planning**: Suggest similar healthy alternatives
5. **Continuous Learning**: Update embeddings from user feedback

### Integration Opportunities
1. **Health Connect**: Better meal categorization
2. **Barcode Scanner**: Combine with product databases
3. **Voice Input**: Multi-modal queries
4. **Meal History**: Temporal patterns in eating

---

## 10. Resource Requirements

### Models
- Image Embedder: ~20MB (MobileNet V3)
- Text Embedder: ~15MB (Universal Sentence Encoder Lite)
- Storage: ~50MB for embeddings of 1000 foods

### Performance
- Embedding Generation: ~50ms per image
- Similarity Search: ~10ms for top-5
- Total RAG Overhead: ~100ms

### Development Time
- Phase 1-2: 2 weeks
- Phase 3-4: 2 weeks
- Testing & Optimization: 1 week
- **Total: 5 weeks**

---

## Next Steps

1. **Immediate Actions**:
   - Download MobileNet V3 embedder model
   - Create vector extension for SQLite
   - Build prototype with 10 foods

2. **Proof of Concept**:
   - Test with common foods (pizza, burger, salad)
   - Measure accuracy improvement
   - Validate performance impact

3. **Decision Point**:
   - If POC shows >20% accuracy improvement
   - And <200ms latency increase
   - Proceed with full implementation

---

## Conclusion

RAG implementation will transform GemMunch from a simple image-to-nutrition app into an intelligent food recognition system that learns and improves. The phased approach ensures we can validate benefits before full commitment while maintaining the current user experience.