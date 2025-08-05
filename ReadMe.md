# FIRST TIME USER SETUP
* Download the models
* Enable Health Connect Permissions

# USER BOOTUP
* Ai Acceleration - identify HW + see if there's
* Choosing model
* image size/type
* reasoning type/style

# Image Analysis

## Photo
* Camera Capture
* Image Gallery Upload

# MAIN SCREEN:

## Single Shot - Quickly identify a single food item or simple meal


### Additional items lookup flow:
"Additional Items (+add item)"
Step 3: Analysis Trigger
- User clicks "Analyze" button (with search icon)
- Button shows loading spinner during search (isSearching = true)
- Triggers the onSearchNutrition callback with food name and serving size

3. Backend Processing (ManualNutritionEntry.kt:150-172)

val result = onSearchNutrition(newItemName.trim(), serving)

This calls through to:
- NutritionSearchService.searchNutrition(foodName, servingSize, servingUnit)
- Local Database First: Searches enhanced nutrition database
- USDA API Fallback: If not found locally, queries USDA FoodData Central API
- Smart Matching: Uses fuzzy matching and food name normalization

4. Success Path

- If nutrition data found → Creates AnalyzedFoodItem with full nutrition facts
- Adds item to manualItems list
- Calls onItemsChanged(manualItems) to update parent component
- Form resets and collapses
- New item appears as expandable card below

5. Error Handling

- If no data found → Shows error: "No nutrition data found for [food]. Try different name or check spelling"
- User can retry with different spelling/name
- Form stays open for corrections


## Discuss with Model - discuss with the model on ingredients that may or may not be in the photograph
* Implement Chain of Thought Reasoning with this model
* Investigate ASYNC more
* separate items into specific messages, with reply to specific items
* Implement Stop button when ASYNC STREAMING IS HAPPENING

## Text only - No Image Inference - Quick text interface and streaming
* make sure this is not ASYNC

## Nutrients.db 
* Chicken Pad Thai on Google - is 200g (308 calories) but we return 100g (158 calories), why does Google return 200g - how do we know the proper serving size?

## General discussion + RAG NOTES

## History



# BUGS:
* USDA API missed responses



# APK OPTIMIZATIONS:
* No internet:
  * BUNDLING IN MODEL
  * USDA API check
* Memory/battery usage
* * Detect if ML accelerator is in a weird state and if a phone reboot is worth it.

# Preparing Github
* Double Check API Keys


# ASK FOR SHWETAK 
* NPU MODELS
* LITERT
* SENSOR LM


================================================================
HELPFUL LINKS:

LLM Inference
* https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/index
* https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android
* https://github.com/google-ai-edge/ai-edge-apis/tree/main
  https://github.com/google-ai-edge/gallery/tree/main/Android
  Function Calling SDK
* https://ai.google.dev/edge/mediapipe/solutions/genai/function_calling/android
  RAG
* https://ai.google.dev/edge/mediapipe/solutions/genai/rag
* https://ai.google.dev/edge/mediapipe/solutions/genai/rag/android
* https://github.com/google-ai-edge/ai-edge-apis/tree/main/examples/rag
  AI-EDGE MISC - VISION / IMAGE:
* GEMMA 3N - multimodal prompting - https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android#multimodal:
    * MobileNet-V5-encoder https://developers.googleblog.com/en/introducing-gemma-3n-developer-guide/#mobilenet-v5:-new-state-of-the-art-vision-encoder
* VISION - https://ai.google.dev/edge/mediapipe/solutions/vision/image_embedder/android
* SUPPORTED/RECOMMENDED Models - https://ai.google.dev/edge/mediapipe/solutions/vision/image_classifier
