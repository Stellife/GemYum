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

## Discuss with Model - discuss with the model on ingredients that may or may not be in the photograph
* Implement Chain of Thought Reasoning with this model

## Text only - No Image Inference - Quick text interface and streaming

## Nutrients.db 

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
