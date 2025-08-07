#!/bin/bash

echo "======================================"
echo "   GemYum Offline Installer v1.0     "
echo "======================================"
echo

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if ADB is available
if ! command -v adb &> /dev/null; then
    echo -e "${RED}ERROR: ADB not found.${NC}"
    echo "Please install Android SDK Platform Tools:"
    echo "  https://developer.android.com/studio/releases/platform-tools"
    echo
    echo "On macOS with Homebrew:"
    echo "  brew install android-platform-tools"
    exit 1
fi

# Check device connection
echo "Checking for connected Android device..."
if ! adb devices | grep -q "device$"; then
    echo -e "${RED}ERROR: No Android device found.${NC}"
    echo
    echo "Please ensure:"
    echo "  1. Your Android device is connected via USB"
    echo "  2. Developer Options are enabled"
    echo "  3. USB Debugging is turned on"
    echo "  4. You've trusted this computer when prompted"
    echo
    echo "To enable Developer Options:"
    echo "  Settings > About Phone > Tap 'Build Number' 7 times"
    exit 1
fi

DEVICE=$(adb devices | grep device$ | cut -f1)
echo -e "${GREEN}✓ Device found:${NC} $DEVICE"
echo

# Check for required files
if [ ! -f "GemYum-v1.0-sideload.apk" ]; then
    echo -e "${RED}ERROR: GemYum-v1.0-sideload.apk not found.${NC}"
    echo "Please ensure you're running this script from the package directory."
    exit 1
fi

if [ ! -f "models/gemma-3n-E2B-it-int4.task" ] || [ ! -f "models/gemma-3n-E4B-it-int4.task" ]; then
    echo -e "${RED}ERROR: Model files not found in models/ directory.${NC}"
    echo "Expected files:"
    echo "  models/gemma-3n-E2B-it-int4.task (3GB)"
    echo "  models/gemma-3n-E4B-it-int4.task (4.4GB)"
    exit 1
fi

# Install APK
echo "Installing GemYum APK..."
if ! adb install -r GemYum-v1.0-sideload.apk; then
    echo -e "${RED}ERROR: APK installation failed.${NC}"
    echo "Try uninstalling the existing app first:"
    echo "  adb uninstall com.stel.gemyum"
    exit 1
fi
echo -e "${GREEN}✓ APK installed successfully${NC}"
echo

# Copy models
echo "Copying AI models to device..."
echo "This will take several minutes. Please be patient."
echo

echo -e "${YELLOW}[1/2]${NC} Copying E2B model (3GB)..."
if adb push models/gemma-3n-E2B-it-int4.task /data/local/tmp/; then
    echo -e "${GREEN}✓ E2B model copied${NC}"
else
    echo -e "${RED}✗ Failed to copy E2B model${NC}"
fi

echo
echo -e "${YELLOW}[2/2]${NC} Copying E4B model (4.4GB)..."
if adb push models/gemma-3n-E4B-it-int4.task /data/local/tmp/; then
    echo -e "${GREEN}✓ E4B model copied${NC}"
else
    echo -e "${RED}✗ Failed to copy E4B model${NC}"
fi

echo
echo "======================================"
echo -e "${GREEN}Installation complete!${NC}"
echo "======================================"
echo
echo "You can now launch GemYum from your device."
echo "The app will automatically detect the pre-installed models."
echo
echo "First launch may take 2-3 minutes to initialize."
echo "Subsequent launches will be much faster (10-30 seconds)."
echo
echo "Enjoy using GemYum - AI-powered nutrition tracking!"