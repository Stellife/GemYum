#!/bin/bash

echo "📥 GemYum Model Downloader"
echo "========================="

# Check if we're in the models directory
if [ ! -f ".gitkeep" ]; then
    echo "❌ Error: Please run this script from the models/ directory"
    exit 1
fi

# Model information
MODEL_NAME="gemma-3n-it-gpu-int4.tflite"
MODEL_URL="https://huggingface.co/google/gemma-3n/resolve/main/gemma-3n-it-gpu-int4.tflite"
MODEL_SIZE="1.4GB"
ASSETS_DIR="../app/src/main/assets/models"

echo ""
echo "This script will download the Gemma 3n model required for GemYum."
echo "Model: $MODEL_NAME"
echo "Size: Approximately $MODEL_SIZE"
echo ""
read -p "Do you want to continue? (y/n) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Download cancelled."
    exit 0
fi

# Check if model already exists
if [ -f "$MODEL_NAME" ]; then
    echo "✅ Model already exists in models/"
    echo "Checking assets directory..."
else
    echo "⏬ Downloading model..."
    echo "This may take several minutes depending on your connection speed."
    
    # Try wget first, fall back to curl
    if command -v wget &> /dev/null; then
        wget --show-progress -O "$MODEL_NAME" "$MODEL_URL"
    elif command -v curl &> /dev/null; then
        curl -L --progress-bar -o "$MODEL_NAME" "$MODEL_URL"
    else
        echo "❌ Error: Neither wget nor curl is installed."
        echo "Please install one of them and try again."
        exit 1
    fi
    
    if [ $? -ne 0 ]; then
        echo "❌ Download failed. Please check your internet connection and try again."
        rm -f "$MODEL_NAME"
        exit 1
    fi
    
    echo "✅ Model downloaded successfully!"
fi

# Create assets directory if it doesn't exist
mkdir -p "$ASSETS_DIR"

# Copy to assets
if [ -f "$ASSETS_DIR/$MODEL_NAME" ]; then
    echo "✅ Model already exists in assets"
else
    echo "📋 Copying model to assets directory..."
    cp "$MODEL_NAME" "$ASSETS_DIR/"
    
    if [ $? -eq 0 ]; then
        echo "✅ Model copied to assets successfully!"
    else
        echo "❌ Failed to copy model to assets. Please copy manually:"
        echo "   cp $MODEL_NAME $ASSETS_DIR/"
    fi
fi

echo ""
echo "✨ Setup complete!"
echo ""
echo "The Gemma 3n model is ready to use."
echo "You can now build and run the app."
echo ""
echo "Note: The model file is large (1.4GB). Keep it in this directory"
echo "for future builds, as it's excluded from git."