#!/bin/bash

echo "🚀 Preparing FAT APK with bundled models..."
echo "========================================="

# Check if models exist in Downloads
E2B_MODEL="$HOME/Downloads/gemma-3n-E2B-it-int4.task"
E4B_MODEL="$HOME/Downloads/gemma-3n-E4B-it-int4.task"

if [ ! -f "$E2B_MODEL" ]; then
    echo "❌ E2B model not found at $E2B_MODEL"
    echo "Please download from Hugging Face first"
    exit 1
fi

if [ ! -f "$E4B_MODEL" ]; then
    echo "❌ E4B model not found at $E4B_MODEL"
    echo "Please download from Hugging Face first"
    exit 1
fi

# Check model sizes
E2B_SIZE=$(ls -lh "$E2B_MODEL" | awk '{print $5}')
E4B_SIZE=$(ls -lh "$E4B_MODEL" | awk '{print $5}')

echo "✅ Found models:"
echo "   E2B: $E2B_SIZE"
echo "   E4B: $E4B_SIZE"

# Copy models to assets
ASSETS_DIR="app/src/main/assets"
mkdir -p "$ASSETS_DIR/models"

echo "📋 Copying models to assets..."
cp "$E2B_MODEL" "$ASSETS_DIR/models/"
cp "$E4B_MODEL" "$ASSETS_DIR/models/"

# Also copy them to a special directory for the app to find on first launch
mkdir -p "$ASSETS_DIR/bundled_models"
cp "$E2B_MODEL" "$ASSETS_DIR/bundled_models/"
cp "$E4B_MODEL" "$ASSETS_DIR/bundled_models/"

# Verify copies
echo "✅ Models copied to assets:"
ls -lh "$ASSETS_DIR/models/" | grep ".task"
ls -lh "$ASSETS_DIR/bundled_models/" | grep ".task"

# Build comprehensive nutrition database
echo "🍎 Building nutrition database..."
cd scripts
if [ -f "nutrients.db" ]; then
    rm nutrients.db
fi

# Try to build comprehensive DB, fall back to minimal if it fails
python3 build_comprehensive_nutrients_db.py 2>/dev/null || python3 create_minimal_db.py

# Copy database to assets
cp nutrients.db ../app/src/main/assets/databases/nutrients.db
cd ..

echo "✅ Database prepared: $(ls -lh app/src/main/assets/databases/nutrients.db | awk '{print $5}')"

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean

# Build release APK
echo "🔨 Building FAT release APK..."
./gradlew assembleRelease

if [ $? -eq 0 ]; then
    # Copy with proper name
    cp app/build/outputs/apk/release/app-release.apk GemYum-v1.0-FAT-release.apk
    
    APK_SIZE=$(ls -lh GemYum-v1.0-FAT-release.apk | awk '{print $5}')
    echo ""
    echo "✨ SUCCESS! FAT APK created:"
    echo "   File: GemYum-v1.0-FAT-release.apk"
    echo "   Size: $APK_SIZE"
    echo ""
    echo "This APK contains:"
    echo "  ✅ Gemma 3n E2B model (Fast)"
    echo "  ✅ Gemma 3n E4B model (Accurate)"
    echo "  ✅ Nutrition database"
    echo "  ✅ Works completely offline"
    echo ""
    echo "Install with:"
    echo "  adb install GemYum-v1.0-FAT-release.apk"
else
    echo "❌ Build failed. Check errors above."
    exit 1
fi