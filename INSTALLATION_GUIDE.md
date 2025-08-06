# GemYum Installation Guide

## Overview
GemYum is an AI-powered nutrition tracking app that runs entirely on-device using Google's Gemma 3n models. We provide two installation options:

1. **Online Version** - Downloads models from Kaggle (requires internet)
2. **Offline Version** - Pre-packaged with models for immediate use

## Latest Release: v1.4.0
- **[Download APK](https://drive.google.com/file/d/16-xZpzdZA3NEv48slvQVASs6c6MYEBCF/view?usp=drive_link)** - Requires WiFi for model download
- **[Offline Package](https://drive.google.com/drive/folders/1jvMmeec--PYCZfIY3sXKF5LM9YHlM5w-?usp=drive_link)** - Pre-bundled with AI models

## System Requirements

### Device Requirements
- Android 10+ (API level 29 or higher)
- Minimum 8GB RAM (12GB+ recommended)
- 10GB free storage space
- NPU/GPU support recommended for optimal performance

### Tested Devices
- Google Pixel 6/7/8/9 series ✅
- Samsung Galaxy S22/S23/S24 series ✅
- OnePlus 11/12 series ✅

## Installation Options

### Option 1: Online Installation (Recommended)

**Best for:** Users with stable internet connection

1. **Download and Install**
   - Download [GemYum v1.4.0 APK](https://drive.google.com/file/d/16-xZpzdZA3NEv48slvQVASs6c6MYEBCF/view?usp=drive_link)
   - Enable "Install from Unknown Sources" in Android settings
   - Install the APK on your device

2. **Download AI Models**
   - Launch GemYum from your app drawer
   - Connect to WiFi/Internet
   - Tap "Download Models" when prompted
   - The app will download:
     - [Gemma 3n E2B Model](https://www.kaggle.com/models/google/gemma-3n/tfLite/gemma-3n-E2B-it-int4) (~2GB)
     - [Gemma 3n E4B Model](https://www.kaggle.com/models/google/gemma-3n/tfLite/gemma-3n-E4B-it-int4) (~4GB, optional)
   - Models are cached locally and work offline after download

### Option 2: Offline Installation (Sideload Package)

**Best for:** Judges, offline testing, or limited internet access

#### Prerequisites
- Computer with Windows, macOS, or Linux
- USB cable
- Android Debug Bridge (ADB) installed

#### Installing ADB

**Windows:**
1. Download [Platform Tools](https://developer.android.com/studio/releases/platform-tools)
2. Extract to `C:\platform-tools`
3. Add to PATH: System Properties → Environment Variables → Path → Add `C:\platform-tools`

**macOS:**
```bash
brew install android-platform-tools
```

**Linux:**
```bash
sudo apt install android-tools-adb  # Ubuntu/Debian
sudo dnf install android-tools      # Fedora
```

#### Enable USB Debugging on Your Device

1. Go to **Settings → About Phone**
2. Tap **Build Number** 7 times to enable Developer Options
3. Go to **Settings → System → Developer Options**
4. Enable **USB Debugging**
5. Connect your device to your computer via USB
6. Tap **Allow** when prompted to trust the computer

#### Installation Steps

1. **Download the offline package**
   - [GemYum Offline Package](https://drive.google.com/drive/folders/1jvMmeec--PYCZfIY3sXKF5LM9YHlM5w-?usp=drive_link) (≈7.65GB)
   - Contains APK with pre-bundled Gemma 3n models

2. **Extract the package**
   ```
   GemYum-v1.0-offline-package/
   ├── GemYum-v1.0-sideload.apk
   ├── models/
   │   ├── gemma-3n-E2B-it-int4.task
   │   └── gemma-3n-E4B-it-int4.task
   ├── install.sh (Mac/Linux)
   ├── install.bat (Windows)
   └── INSTALLATION_GUIDE.md
   ```

3. **Run the installer**
   
   **Windows:**
   - Double-click `install.bat`
   
   **Mac/Linux:**
   ```bash
   chmod +x install.sh
   ./install.sh
   ```

4. **Wait for installation**
   - APK will be installed
   - Models will be copied (takes 5-10 minutes)
   - You'll see "Installation complete!" when done

5. **Launch GemYum**
   - Open the app from your device
   - First launch takes 2-3 minutes to initialize
   - Subsequent launches are much faster (10-30 seconds)

## Manual Installation (Advanced)

If the installer scripts don't work:

```bash
# Install APK
adb install -r GemYum-v1.0-sideload.apk

# Copy models
adb push models/gemma-3n-E2B-it-int4.task /data/local/tmp/
adb push models/gemma-3n-E4B-it-int4.task /data/local/tmp/

# Launch app
adb shell am start -n com.stel.gemyum/com.stel.gemmunch.ui.MainActivity
```

## Features

### Quick Snap Insight
- Take a photo or select from gallery
- Instant nutritional analysis
- Silicon Valley "Hotdog Not Hotdog" easter egg

### Deep Chat
- Multimodal conversations about your meals
- Upload food images for detailed analysis
- Ask follow-up questions

### Text-Only Mode
- Fast meal logging without photos
- Quick nutrition estimates
- Offline operation

### Nutrient Database
- Search 700,000+ foods
- Restaurant menu items
- Barcode scanning (coming soon)

### Health Connect Integration
- Sync nutrition data with Google Health Connect
- Track your dietary patterns
- Export to other health apps

## Troubleshooting

### "Models not found" error
- Ensure you have 10GB free storage
- Try clearing app data: Settings → Apps → GemYum → Clear Data
- Re-run the installer

### App crashes on launch
- Check device has minimum 8GB RAM
- Close other apps to free memory
- Restart your device

### Slow performance
- First launch is always slow (2-3 minutes)
- Ensure device isn't in battery saver mode
- Models run best on NPU/GPU hardware

### USB Debugging issues
- Ensure cable supports data transfer (not charge-only)
- Try different USB port
- Revoke and re-grant USB debugging permissions

## Performance Tips

- **Best Performance:** Pixel 9 Pro with Tensor G4 (0.8s inference)
- **Good Performance:** Devices with Snapdragon 8 Gen 2+ or equivalent
- **Acceptable Performance:** Mid-range devices with 8GB+ RAM

## Support

For issues or questions:
- GitHub: [GemYum Repository](https://github.com/yourusername/GemYum)
- Email: support@gemyum.app

## License

GemYum is built for the Google Gemma 3n Hackathon 2025.
Models are licensed under Gemma Terms of Use.

---

*Built with ❤️ using Google's Gemma 3n models and MediaPipe*