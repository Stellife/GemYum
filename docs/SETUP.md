# Detailed Setup Guide for GemYum

## Prerequisites

### System Requirements
- macOS, Linux, or Windows 10+
- 8GB RAM minimum (16GB recommended for development)
- 10GB free disk space
- Android device with Android 7.0+ (API 24+)

### Software Requirements

1. **Android Studio Arctic Fox** or newer
   - Download: https://developer.android.com/studio
   
2. **JDK 17**
   ```bash
   # macOS
   brew install openjdk@17
   
   # Ubuntu/Debian
   sudo apt install openjdk-17-jdk
   
   # Windows
   # Download from https://adoptium.net/
   ```

3. **Python 3.8+** (for building nutrition database)
   ```bash
   python --version  # Should show 3.8 or higher
   ```

## Step-by-Step Setup

### 1. Clone the Repository

```bash
git clone https://github.com/stel/GemYum.git
cd GemYum
```

### 2. Configure Environment

```bash
cp .env.example .env
# Edit .env with your preferred text editor if needed
```

### 3. Download Gemma 3n Models

The app requires Gemma 3n models (approximately 1.4GB). You have two options:

#### Option A: Automatic Download (Recommended)
```bash
cd models
./download_models.sh
```

#### Option B: Manual Download
1. Visit the [Gemma model page](https://www.kaggle.com/models/google/gemma)
2. Download `gemma-3n-it-gpu-int4.tflite` 
3. Place in the `models/` directory
4. Copy to assets: `cp models/*.tflite app/src/main/assets/models/`

### 4. Build the Nutrition Database

The app uses a comprehensive nutrition database that must be built from public sources:

```bash
cd scripts

# Install Python dependencies
pip install -r requirements.txt

# Build the database (this will take 5-10 minutes)
python build_nutrients_db.py

# The script will automatically copy the database to the correct location
```

This creates a 45MB SQLite database with:
- 900,000+ food items
- Restaurant-specific portions (Chipotle, McDonald's, etc.)
- Glycemic index data
- USDA nutrition information

### 5. Open in Android Studio

1. Launch Android Studio
2. Select "Open Existing Project"
3. Navigate to the GemYum folder
4. Click "Open"
5. Wait for Gradle sync to complete (this may take a few minutes)

### 6. Configure Your Device

#### Option A: Physical Device (Recommended for best performance)
1. On your Android device:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times to enable Developer Options
   - Go to Settings → Developer Options
   - Enable "USB Debugging"
2. Connect your device via USB
3. Trust the computer when prompted on your device

#### Option B: Emulator (Limited performance)
1. In Android Studio, open AVD Manager
2. Create a new Virtual Device:
   - Choose Pixel 6 or newer
   - Select API 31 or higher
   - Allocate at least 4GB RAM
   
**Note**: Emulators don't support NPU/GPU acceleration, so inference will be significantly slower (30+ seconds vs 0.8 seconds on real hardware).

### 7. Build and Run

#### From Android Studio
1. Select your device from the device dropdown
2. Click the green "Run" button (▶️)
3. Wait for the build to complete and app to install

#### From Command Line
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Or use adb directly
adb install app/build/outputs/apk/debug/app-debug.apk
```

## First Launch

On first launch, the app will:
1. Check for downloaded models
2. Initialize the AI components (30-60 seconds on first run)
3. Pre-warm an inference session for instant photo analysis

You'll see initialization progress in the UI. Subsequent launches will be much faster (5-10 seconds).

## Troubleshooting

### "Model file not found"
- Ensure models are in `app/src/main/assets/models/`
- Run `ls app/src/main/assets/models/` to verify
- Re-run `./models/download_models.sh`

### "Database not found"
- Build the database: `cd scripts && python build_nutrients_db.py`
- Verify it exists: `ls app/src/main/assets/databases/nutrients.db`

### "Out of memory" during build
```bash
# Increase Gradle heap size
echo "org.gradle.jvmargs=-Xmx4g" >> gradle.properties
```

### Slow inference on device
- Ensure you're using a physical device, not an emulator
- Check that your device has a GPU (most 2020+ phones do)
- Try closing other apps to free up memory

### App crashes on launch
1. Check logcat: `adb logcat | grep -E "GemYum|AndroidRuntime"`
2. Common causes:
   - Missing model files
   - Insufficient memory (need 2GB free)
   - Old Android version (need API 24+)

## Performance Tips

### For Best Performance
- Use a Pixel 6+ (Tensor chip) or flagship Samsung/OnePlus device
- Keep at least 2GB RAM free
- Close other camera apps before using GemYum
- Use good lighting for photos

### Expected Performance
| Device | Inference Time |
|--------|---------------|
| Pixel 9 Pro | 0.8 seconds |
| Pixel 7 | 1.2 seconds |
| Samsung S24 | 1.5 seconds |
| Mid-range (2020+) | 3-5 seconds |
| Emulator | 30+ seconds |

## Next Steps

1. **Test the app**: Take a photo of food and see the instant analysis!
2. **Read the architecture**: Check [ARCHITECTURE.md](ARCHITECTURE.md) to understand how it works
3. **Contribute**: See [CONTRIBUTING.md](../CONTRIBUTING.md) to help improve GemYum
4. **Report issues**: Use [GitHub Issues](https://github.com/stel/GemYum/issues)

## Need Help?

- 📖 [FAQ](FAQ.md) - Common questions answered
- 💬 [Discussions](https://github.com/stel/GemYum/discussions) - Ask the community
- 🐛 [Issues](https://github.com/stel/GemYum/issues) - Report bugs
- 📧 Email - gemyum@stel.com