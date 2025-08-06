#!/bin/bash

echo "📦 Creating OBB Expansion File with Models..."
echo "============================================"

# Create OBB directory structure
OBB_DIR="obb_package"
rm -rf $OBB_DIR
mkdir -p $OBB_DIR

# Copy models to OBB
echo "📋 Copying models..."
cp ~/Downloads/gemma-3n-E2B-it-int4.task $OBB_DIR/
cp ~/Downloads/gemma-3n-E4B-it-int4.task $OBB_DIR/

# Create OBB file (just a renamed ZIP)
echo "🗜️ Creating OBB file..."
cd $OBB_DIR
zip -0 ../main.1.com.stel.gemyum.obb *.task
cd ..

echo "✅ OBB file created: main.1.com.stel.gemyum.obb"
ls -lh main.1.com.stel.gemyum.obb

echo ""
echo "To use:"
echo "1. Install the APK: adb install GemYum-v1.0-release.apk"
echo "2. Push OBB to device: adb push main.1.com.stel.gemyum.obb /sdcard/Android/obb/com.stel.gemyum/"
echo ""
echo "The app will find the models in the OBB file."