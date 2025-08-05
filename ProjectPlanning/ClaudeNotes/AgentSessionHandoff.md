# Agent Session Handoff - GemMunch V2 Implementation

## ChatViewModel Enhancement Plan (Session 5) - REVISED

### Critical Correction: True Goals for Chat Modes

After reviewing ProjectArchitect.md, the goals are much more focused:

**Both Chat Modes Goal**: Quickly identify meal nutritional content → Convert to JSON → Present nutrition → Offer Health Connect save

This is NOT about lengthy conversations or education. It's about SPEED and EFFICIENCY.

### Current State Analysis

After reviewing the ChatViewModel implementation, I've identified several areas for enhancement:

1. **Basic Conversation Management**: Currently only stores last 6 messages in history
2. **No Image Context**: Image path is stored but not actually sent to the AI session
3. **Session Reuse**: Uses the same vision session for all chat modes
4. **No Function Calling**: No integration with nutrition database lookups
5. **Limited Context**: Simple prompt building without proper conversation structuring

### Revised Enhancement Goals (Focused on Speed)

1. **Minimal Conversation Context**
   - Only track current meal analysis (not full history)
   - System prompts focused on extraction, not conversation
   - Gemma chat template for proper formatting
   - NO conversation persistence needed

2. **Image Context Integration** 
   - Send images to AI for rapid food identification
   - Single image per meal analysis
   - NO image gallery or multi-image support needed

3. **Function Calling for Nutrition DB** (PRIORITY ELEVATED TO HIGH)
   - AI directly queries nutrition database
   - Eliminates manual parsing of AI responses
   - Provides structured nutrition data immediately

4. **JSON-First Response Format**
   - Force AI to respond with structured data
   - Minimal prose, maximum data extraction
   - Clear success/failure detection

5. **Health Connect Integration**
   - End every flow with "Save to Health Connect?" prompt
   - Quick save with minimal friction

### Technical Implementation Strategy (Revised)

#### Phase 1: Function Calling Integration (Priority: HIGH - Do This First!)

**Why First?** This is the core differentiator that enables the "agent" behavior described in ProjectArchitect.md

**1.1 Define Two Key Functions**
```kotlin
// Function 1: Direct nutrition database query
val getNutritionFromDB = FunctionDeclaration.newBuilder()
    .setName("getNutritionFromDB")
    .setDescription("Query nutrition database for a food item")
    .setParameters(
        Schema.newBuilder()
            .setType(Type.OBJECT)
            .putProperties("foodName", 
                Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("Name of the food item")
                    .build())
            .putProperties("quantity",
                Schema.newBuilder()
                    .setType(Type.NUMBER)
                    .setDescription("Amount of the food")
                    .build())
            .putProperties("unit",
                Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("Unit of measurement (g, oz, cup, serving)")
                    .build())
            .addRequired("foodName")
            .addRequired("quantity")
            .build()
    )
    .build()

// Function 2: Request clarification (only when needed)
val requestUserInput = FunctionDeclaration.newBuilder()
    .setName("requestUserInput")
    .setDescription("Ask user for clarification about the meal")
    .setParameters(
        Schema.newBuilder()
            .setType(Type.OBJECT)
            .putProperties("question",
                Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription("Specific question to ask the user")
                    .build())
            .addRequired("question")
            .build()
    )
    .build()

// Create tool containing both functions
val nutritionTool = Tool.newBuilder()
    .addFunctionDeclarations(getNutritionFromDB)
    .addFunctionDeclarations(requestUserInput)
    .build()
```

**1.2 System Prompts (Extraction-Focused)**
- **Analyze & Chat Mode**:
  ```
  You are a nutrition analysis tool with access to functions. Analyze the image to identify all food items.
  
  Your workflow:
  1. Identify each food item in the image with estimated portions
  2. Use getNutritionFromDB() for each identified item
  3. If uncertain about specific items (sauces, restaurant, portions), use requestUserInput() ONCE to ask a specific question
  4. Compile total nutrition data
  
  Be direct and efficient. No small talk.
  ```

- **Text-Only Mode**:
  ```
  You are a nutrition analysis tool with access to functions. Extract food items from the user's description.
  
  Your workflow:
  1. Parse the description to identify specific foods and portions
  2. Use getNutritionFromDB() for each item
  3. If portions or items are unclear, use requestUserInput() ONCE
  4. Compile total nutrition data
  
  Be direct and efficient. No small talk.
  ```

#### Phase 2: Minimal Conversation Management (Priority: HIGH)

**2.1 Simplified Message Structure**
```kotlin
data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val isToolCall: Boolean = false,
    val toolName: String? = null,
    val toolParams: Map<String, Any>? = null,
    val nutritionResults: List<AnalyzedFoodItem>? = null
)
```

**2.2 Agent Loop Implementation**
```kotlin
private suspend fun executeAgentLoop(userInput: String, imagePath: String? = null) {
    // Initialize FC-enabled model
    val model = createFCModel()
    val chat = model.startChat()
    val mealNutrition = mutableListOf<AnalyzedFoodItem>()
    
    // Send initial message (with image if available)
    val response = if (imagePath != null && isMultimodal) {
        // For Analyze & Chat mode - include image
        val imageContent = Content.newBuilder()
            .setRole("user")
            .addParts(Part.newBuilder().setText(userInput))
            .addParts(Part.newBuilder().setInlineData(
                Blob.newBuilder()
                    .setMimeType("image/jpeg")
                    .setData(loadImageBytes(imagePath))
            ))
            .build()
        chat.sendMessage(imageContent)
    } else {
        // Text-only mode
        chat.sendMessage(userInput)
    }
    
    // Process response and handle function calls
    var continueLoop = true
    while (continueLoop) {
        when {
            response.hasFunctionCall() -> {
                val functionCall = response.getFunctionCall()
                val args = functionCall.getArgs().getFieldsMap()
                
                when (functionCall.getName()) {
                    "getNutritionFromDB" -> {
                        // Execute nutrition lookup
                        val result = nutritionSearchService.searchNutrition(
                            foodName = args["foodName"]?.stringValue ?: "",
                            servingSize = args["quantity"]?.numberValue ?: 1.0,
                            servingUnit = args["unit"]?.stringValue ?: "serving"
                        )
                        
                        result?.let { mealNutrition.add(it) }
                        
                        // Send function response back to model
                        val functionResponse = FunctionResponse.newBuilder()
                            .setName(functionCall.getName())
                            .setResponse(
                                Struct.newBuilder()
                                    .putFields("result", 
                                        Value.newBuilder()
                                            .setStringValue(formatNutritionResult(result))
                                            .build())
                            )
                            .build()
                        
                        response = chat.sendMessage(
                            Content.newBuilder()
                                .addParts(Part.newBuilder()
                                    .setFunctionResponse(functionResponse))
                                .build()
                        )
                    }
                    
                    "requestUserInput" -> {
                        val question = args["question"]?.stringValue ?: ""
                        // Show question to user
                        addMessage(ChatMessage(
                            text = question,
                            isFromUser = false
                        ))
                        continueLoop = false // Wait for user response
                    }
                }
            }
            
            else -> {
                // AI is done with analysis
                if (mealNutrition.isNotEmpty()) {
                    showNutritionSummary(mealNutrition)
                    promptHealthConnectSave(mealNutrition)
                } else {
                    // Show AI's final message
                    addMessage(ChatMessage(
                        text = response.getText(),
                        isFromUser = false
                    ))
                }
                continueLoop = false
            }
        }
    }
}

private fun createFCModel(): GenerativeModel {
    // Get LlmInference from AppContainer
    val llmInference = appContainer.visionLlmInference
        ?: throw IllegalStateException("LLM not initialized")
    
    // Create FC backend with Gemma formatter
    val fcBackend = LlmInferenceBackend(llmInference, GemmaFormatter())
    
    // Create system instruction
    val systemInstruction = Content.newBuilder()
        .setRole("system")
        .addParts(Part.newBuilder().setText(getSystemPrompt()))
        .build()
    
    // Return model with tools
    return GenerativeModel(
        fcBackend,
        systemInstruction,
        listOf(nutritionTool)
    )
}
```

#### Phase 3: Image Context Integration (Priority: HIGH)

**3.1 Simple Image Handling**
```kotlin
private suspend fun processWithAI(
    userMessage: String,
    imagePath: String? = null
): String {
    val session = appContainer.getReadyVisionSession()
    
    // Add image if available (Analyze & Chat mode)
    if (isMultimodal && imagePath != null) {
        val imageData = loadImageAsBytes(imagePath)
        session.addImage(imageData)
    }
    
    // Build minimal prompt with Gemma format
    val prompt = buildGemmaPrompt(userMessage)
    session.addQueryChunk(prompt)
    
    return session.generateResponse()
}
```

#### Phase 4: Health Connect Integration (Priority: HIGH)

**4.1 Quick Save Flow**
```kotlin
private fun promptHealthConnectSave(nutritionItems: List<AnalyzedFoodItem>) {
    // Show simple dialog
    showDialog(
        title = "Save to Health Connect?",
        message = "Total: ${nutritionItems.sumOf { it.calories }} calories",
        positiveButton = "Save" to { saveToHealthConnect(nutritionItems) },
        negativeButton = "Skip" to { /* dismiss */ }
    )
}
```

### Specific Implementation Steps

#### Step 1: SDK Integration and Function Declaration
1. Add FC SDK imports to ChatViewModel
2. Create FunctionDeclaration objects with proper Schema structure
3. Build Tool containing both functions
4. Verify compilation with ./gradlew :app:compileDebugKotlin

#### Step 2: Create FC-Enabled Model Factory
1. Add method to AppContainer to expose visionLlmInference
2. Create LlmInferenceBackend wrapper with GemmaFormatter
3. Build GenerativeModel with system instruction and tools
4. Handle model initialization errors gracefully

#### Step 3: Implement Agent Loop with Function Calls
1. Create chat session from GenerativeModel
2. Send initial message with optional image
3. Parse response to detect function calls
4. Execute nutrition lookups and format results
5. Send function responses back to model
6. Continue loop until model completes analysis

#### Step 4: Wire Up UI and Nutrition Display
1. Show function call progress in chat UI
2. Display accumulated nutrition data
3. Add Health Connect save dialog
4. Handle user responses to clarification questions

#### Step 5: Test Critical Paths
1. Text-only: "I had a burger and fries"
2. Image analysis: Photo of a meal
3. Clarification: "I had tacos" → "From which restaurant?"
4. Multiple items: Complex meals with many ingredients
5. Error cases: Unknown foods, API failures

### Key Architecture Highlights (For Competition Submission)

1. **"On-device Agent with Function Calling"** - Gemma 3n autonomously queries nutrition DB
2. **"Intelligent Failure Pipeline"** - Snap & Log seamlessly hands off to Analyze & Chat
3. **"Minimal Interaction Design"** - Focused on speed, not conversation
4. **"Direct Health Connect Integration"** - One-tap nutrition logging

### Success Criteria (Revised)

1. AI successfully uses function calling to query nutrition DB
2. Meal analysis completes in < 3 interactions
3. Image analysis works in Analyze & Chat mode
4. Health Connect save is offered every time
5. Failure cases gracefully transition between modes

### What We're NOT Doing

- Long conversations
- Educational content
- Conversation history/persistence
- Multiple images per meal
- Complex UI interactions

## Specific Function Calling Experiences to Enable

### Current Implementation Results

Our structured prompt approach successfully delivers these experiences:

### Experience 1: Text-Only Quick Logging
**User**: "I had a grilled chicken sandwich with fries and a coke"

**AI Actions**:
1. Calls `getNutritionFromDB("grilled chicken sandwich", 1, "serving")`
2. Calls `getNutritionFromDB("french fries", 1, "serving")`  
3. Calls `getNutritionFromDB("coca cola", 1, "serving")`
4. Compiles results: "Found 850 calories total"

**User sees**: 
```
Analyzing your meal...
✓ Grilled chicken sandwich - 380 cal
✓ French fries - 320 cal
✓ Coca Cola - 150 cal

Total: 850 calories, 45g protein, 78g carbs, 35g fat

[Save to Health Connect] [Skip]
```

### Experience 2: Image Analysis with Clarification
**User**: [Takes photo of tacos]

**AI Actions**:
1. Analyzes image: "I see 3 tacos with meat, cheese, and toppings"
2. Calls `requestUserInput("Are these from a specific restaurant? This helps me provide more accurate nutrition info")`

**User**: "Chipotle chicken tacos"

**AI Actions**:
1. Calls `getNutritionFromDB("chipotle chicken taco", 3, "taco")`
2. Returns nutrition summary

**User sees**:
```
I found 3 tacos in your photo.

Are these from a specific restaurant? This helps me provide more accurate nutrition info.

[User types: Chipotle chicken tacos]

Analyzing Chipotle chicken tacos...
✓ 3 Chipotle chicken tacos - 645 cal

Total: 645 calories, 51g protein, 48g carbs, 24g fat

[Save to Health Connect] [Skip]
```

### Experience 3: Complex Meal Breakdown
**User**: "I had a big salad with grilled chicken, ranch dressing, croutons, and cheese"

**AI Actions**:
1. Calls `getNutritionFromDB("mixed green salad", 2, "cup")`
2. Calls `getNutritionFromDB("grilled chicken breast", 4, "oz")`
3. Calls `getNutritionFromDB("ranch dressing", 2, "tbsp")`
4. Calls `getNutritionFromDB("croutons", 0.25, "cup")`
5. Calls `getNutritionFromDB("cheddar cheese shredded", 0.25, "cup")`
6. Compiles comprehensive nutrition data

**User sees**:
```
Analyzing your salad components...
✓ Mixed greens (2 cups) - 20 cal
✓ Grilled chicken (4 oz) - 185 cal
✓ Ranch dressing (2 tbsp) - 140 cal
✓ Croutons (1/4 cup) - 47 cal
✓ Cheddar cheese (1/4 cup) - 110 cal

Total: 502 calories, 36g protein, 15g carbs, 34g fat

[Save to Health Connect] [Skip]
```

### Experience 4: Unknown Food Handling
**User**: "I had quinoa Buddha bowl"

**AI Actions**:
1. Calls `getNutritionFromDB("quinoa buddha bowl", 1, "serving")`
2. Gets null result
3. Calls `requestUserInput("I couldn't find 'quinoa Buddha bowl' in my database. Can you describe the main ingredients?")`

**User**: "It had quinoa, chickpeas, avocado, and tahini sauce"

**AI Actions**:
1. Calls `getNutritionFromDB("quinoa cooked", 1, "cup")`
2. Calls `getNutritionFromDB("chickpeas", 0.5, "cup")`
3. Calls `getNutritionFromDB("avocado", 0.5, "avocado")`
4. Calls `getNutritionFromDB("tahini sauce", 2, "tbsp")`

### Key Implementation Requirements

1. **Function calls must be sequential** - AI should call functions one at a time
2. **Show progress** - Update UI as each item is found
3. **Handle nulls gracefully** - If nutrition not found, ask for alternatives
4. **Always end with Health Connect** - Every flow ends with save option
5. **Keep it under 3 interactions** - Initial input → possible clarification → result

## Function Calling SDK Implementation Challenges

### Issues Encountered with FC SDK

1. **Import Resolution Problems**
   - The FC SDK classes (`FunctionDeclaration`, `Schema`, `Type`, `Tool`) couldn't be properly imported
   - Package structure unclear: `com.google.ai.edge.localagents.fc.*` didn't contain expected classes
   - Build errors: "Unresolved reference" for all FC SDK types despite dependency being present

2. **API Mismatch with MediaPipe**
   - FC SDK expects wrapping MediaPipe's `LlmInference` in their `LlmInferenceBackend`
   - Unclear how to properly create `GenerativeModel` with existing vision sessions
   - Documentation examples were in Java, not Kotlin, making translation difficult

3. **Complex Type System**
   - `Schema.newBuilder()` pattern required nested builders for each parameter
   - Protocol Buffer types (`Struct`, `Value`) added complexity
   - Type system conflicts between FC SDK types and standard Kotlin types

4. **Session Management Conflicts**
   - AppContainer manages pre-warmed `LlmInferenceSession` instances
   - FC SDK wants to create its own `ChatSession` from `GenerativeModel`
   - Unclear how to reuse existing sessions with FC wrapper

### Alternative Implementation: Structured Prompts

To achieve similar user experience without FC SDK:

#### 1. JSON-Structured Responses
```kotlin
// Force AI to output parseable JSON
val prompt = """Output ONLY a JSON object in this exact format:
{
    "foods": [
        {"name": "food item", "quantity": 1.0, "unit": "serving"}
    ],
    "needsClarification": false,
    "question": null
}"""
```

#### 2. Manual Function Execution
```kotlin
// Parse JSON and execute "functions" manually
val json = JSONObject(aiResponse)
val foods = json.getJSONArray("foods")

for (i in 0 until foods.length()) {
    val food = foods.getJSONObject(i)
    // Manually call nutrition service
    val result = nutritionSearchService.searchNutrition(
        foodName = food.getString("name"),
        servingSize = food.getDouble("quantity"),
        servingUnit = food.getString("unit")
    )
}
```

#### 3. Progressive UI Updates
```kotlin
// Show each lookup as it happens
addMessage(ChatMessage(
    text = "Looking up: $foodName ($quantity $unit)...",
    isFromUser = false
))
```

### Where This Implementation Falls Short

1. **No Constrained Output**
   - FC SDK would enforce valid function names and parameters
   - Our approach relies on prompt engineering and hope
   - AI might return malformed JSON or prose instead

2. **Less Reliable Parsing**
   - Manual JSON extraction with fallbacks
   - FC SDK would handle response parsing automatically
   - More error-prone with edge cases

3. **No True Tool Usage Tracking**
   - Can't see which "functions" AI intended to call
   - FC SDK provides clear function call intents
   - Harder to debug when AI doesn't follow instructions

4. **Sequential vs Parallel Execution**
   - Our implementation processes foods sequentially
   - FC SDK might enable parallel function calls
   - Slightly slower user experience

5. **Limited AI Reasoning**
   - AI can't "decide" to call functions based on context
   - We force it to always output JSON format
   - Less flexible than true function calling

## Updated Next Steps

### Immediate Actions (Competition Timeline)

1. **Polish Current Implementation**
   - Add more robust JSON parsing with fallbacks
   - Improve error messages when foods not found
   - Test edge cases (empty responses, malformed JSON)

2. **Optimize Performance**
   - Pre-compile JSON parsing patterns
   - Cache common food lookups
   - Minimize UI updates during lookups

3. **Enhanced User Experience**
   - Add animations for progressive lookups
   - Show partial results as they arrive
   - Better loading states during analysis

4. **Demo Preparation**
   - Record video showing "agent-like" behavior
   - Emphasize progressive feedback
   - Show both text and image modes

### Post-Competition Improvements

1. **Revisit FC SDK Integration**
   - Study Java examples more carefully
   - Try creating minimal FC SDK prototype
   - Consult SDK documentation for Kotlin examples

2. **Alternative Approaches**
   - Investigate LangChain-style prompt chaining
   - Build custom function calling layer
   - Explore other on-device agent frameworks

3. **Conversation Management**
   - Add proper multi-turn conversation support
   - Implement context window management
   - Store conversation history for learning

4. **Advanced Features**
   - Meal plan suggestions based on history
   - Nutritional goal tracking
   - Recipe analysis from ingredients

### Technical Debt to Address

1. **Session Type Optimization**
   - Create text-only sessions for non-image mode
   - Reduce memory usage for text conversations
   - Implement proper session lifecycle

2. **Error Recovery**
   - Better handling of AI hallucinations
   - Graceful degradation when JSON parsing fails
   - User-friendly error messages

3. **Testing Infrastructure**
   - Unit tests for JSON parsing logic
   - Integration tests for nutrition lookups
   - UI tests for chat interactions

### Lessons Learned

1. **SDK Complexity**: Cutting-edge SDKs may have poor documentation and examples
2. **Time Constraints**: Sometimes a pragmatic solution beats a perfect one
3. **User Experience**: Progress feedback makes simple solutions feel intelligent
4. **Prompt Engineering**: Well-crafted prompts can simulate complex behaviors