#!/bin/bash

echo "📥 Downloading Gemma 3n models for GemYum..."
echo "========================================="

# Model URLs from ModelRegistry.kt
E2B_URL="https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task"
E4B_URL="https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task"

ASSETS_DIR="../app/src/main/assets/models"

# Create directory if it doesn't exist
mkdir -p "$ASSETS_DIR"

echo "Downloading E2B model (Fast)..."
curl -L --progress-bar -o "$ASSETS_DIR/gemma-3n-E2B-it-int4.task" "$E2B_URL"

echo "Downloading E4B model (Accurate)..."
curl -L --progress-bar -o "$ASSETS_DIR/gemma-3n-E4B-it-int4.task" "$E4B_URL"

echo "✅ Models downloaded to assets!"
ls -lh "$ASSETS_DIR/"