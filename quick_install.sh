#!/bin/bash
# Quick installation script for GemYum (downloads models directly)

echo "GemYum Quick Installer"
echo "====================="
echo
echo "This script will:"
echo "1. Install GemYum APK"
echo "2. Download models from Hugging Face (7.4GB)"
echo "3. Push models to device"
echo

# Check for ADB
if ! command -v adb &> /dev/null; then
    echo "Error: ADB not found. Please install Android SDK Platform Tools."
    exit 1
fi

# Check device
if ! adb devices | grep -q "device$"; then
    echo "Error: No Android device found. Please connect your device with USB Debugging enabled."
    exit 1
fi

# Install APK
echo "Installing GemYum..."
if [ -f "GemYum-v1.0-release.apk" ]; then
    adb install -r GemYum-v1.0-release.apk
elif [ -f "GemYum-v1.0-sideload.apk" ]; then
    adb install -r GemYum-v1.0-sideload.apk
else
    echo "Error: No APK found in current directory"
    exit 1
fi

# Download models if not present
echo
echo "Checking for models..."
if [ ! -f "gemma-3n-E2B-it-int4.task" ]; then
    echo "Downloading E2B model (3GB)..."
    wget https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task
fi

if [ ! -f "gemma-3n-E4B-it-int4.task" ]; then
    echo "Downloading E4B model (4.4GB)..."
    wget https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task
fi

# Push to device
echo
echo "Pushing models to device..."
adb push gemma-3n-E2B-it-int4.task /data/local/tmp/
adb push gemma-3n-E4B-it-int4.task /data/local/tmp/

echo
echo "Installation complete!"
echo "Launch GemYum from your device."
echo "First launch will take 2-3 minutes to initialize."