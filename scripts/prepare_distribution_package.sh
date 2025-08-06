#!/bin/bash

echo "📦 Creating GemYum Distribution Package"
echo "======================================"

# Create distribution directory
DIST_DIR="GemYum_Distribution"
rm -rf $DIST_DIR
mkdir -p $DIST_DIR

# Copy the base APK
echo "📱 Copying APK..."
cp GemYum-v1.0-release.apk $DIST_DIR/

# Copy models
echo "🤖 Copying models..."
cp ~/Downloads/gemma-3n-E2B-it-int4.task $DIST_DIR/
cp ~/Downloads/gemma-3n-E4B-it-int4.task $DIST_DIR/

# Create installation script
cat > $DIST_DIR/install.sh << 'EOF'
#!/bin/bash

echo "🚀 GemYum Installer"
echo "=================="

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected. Please connect your device and try again."
    exit 1
fi

echo "📱 Installing GemYum APK..."
adb install -r GemYum-v1.0-release.apk

echo "📂 Creating model directory on device..."
adb shell "mkdir -p /sdcard/Download/GemYum"

echo "⬆️ Pushing E4B model (this will take a few minutes)..."
adb push gemma-3n-E4B-it-int4.task /sdcard/Download/GemYum/

echo "⬆️ Pushing E2B model (backup, faster model)..."
adb push gemma-3n-E2B-it-int4.task /sdcard/Download/GemYum/

echo "✅ Installation complete!"
echo ""
echo "Now on your device:"
echo "1. Open GemYum app"
echo "2. Grant storage permissions if asked"
echo "3. The app will detect and copy models from Download folder"
echo "4. After first launch, you can delete files from Download to save space"
EOF

chmod +x $DIST_DIR/install.sh

# Create install.bat for Windows
cat > $DIST_DIR/install.bat << 'EOF'
@echo off
echo 🚀 GemYum Installer for Windows
echo ==============================

adb devices | findstr "device$" >nul
if errorlevel 1 (
    echo ❌ No Android device connected. Please connect your device and try again.
    pause
    exit /b 1
)

echo 📱 Installing GemYum APK...
adb install -r GemYum-v1.0-release.apk

echo 📂 Creating model directory on device...
adb shell mkdir -p /sdcard/Download/GemYum

echo ⬆️ Pushing E4B model (this will take a few minutes)...
adb push gemma-3n-E4B-it-int4.task /sdcard/Download/GemYum/

echo ⬆️ Pushing E2B model (backup, faster model)...
adb push gemma-3n-E2B-it-int4.task /sdcard/Download/GemYum/

echo ✅ Installation complete!
echo.
echo Now on your device:
echo 1. Open GemYum app
echo 2. Grant storage permissions if asked
echo 3. The app will detect and copy models from Download folder
echo 4. After first launch, you can delete files from Download to save space
pause
EOF

# Create README
cat > $DIST_DIR/README.md << 'EOF'
# GemYum Installation Package

## Contents
- `GemYum-v1.0-release.apk` - The main application
- `gemma-3n-E4B-it-int4.task` - Accurate AI model (4.1GB)
- `gemma-3n-E2B-it-int4.task` - Fast AI model (2.9GB)
- `install.sh` - Installation script for Mac/Linux
- `install.bat` - Installation script for Windows

## Requirements
- Android device with Android 12+ (API 31+)
- 8GB free storage on device
- ADB installed on computer
- USB cable

## Installation

### Mac/Linux
```bash
chmod +x install.sh
./install.sh
```

### Windows
Double-click `install.bat` or run in Command Prompt:
```
install.bat
```

### Manual Installation
1. Install APK: `adb install GemYum-v1.0-release.apk`
2. Push models: 
   - `adb push gemma-3n-E4B-it-int4.task /sdcard/Download/GemYum/`
   - `adb push gemma-3n-E2B-it-int4.task /sdcard/Download/GemYum/`
3. Open app and grant permissions

## First Launch
The app will automatically detect and import the models from the Download folder.
This process takes about 2-3 minutes. After import, the app works completely offline.

## Troubleshooting
- If "device not found": Enable USB Debugging in Developer Options
- If "permission denied": Grant storage permissions to GemYum
- If models not detected: Ensure they're in `/sdcard/Download/GemYum/`
EOF

# Calculate package size
TOTAL_SIZE=$(du -sh $DIST_DIR | cut -f1)

echo ""
echo "✅ Distribution package created: $DIST_DIR"
echo "   Total size: $TOTAL_SIZE"
echo ""
echo "Package contents:"
ls -lh $DIST_DIR/
echo ""
echo "To distribute:"
echo "1. ZIP the folder: zip -r GemYum_Complete.zip $DIST_DIR"
echo "2. Share via Google Drive, USB, or cloud storage"
echo "3. Users run the install script for their platform"