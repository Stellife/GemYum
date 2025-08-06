# GemYum Developer Setup Guide

## Prerequisites

### Required Software
- **Android Studio** Ladybug (2024.2.1) or newer
- **JDK 17** or newer
- **Git** for version control
- **Android SDK** API level 31+

### Hardware Requirements
- **Development Machine**: 8GB+ RAM recommended
- **Android Device/Emulator**: 
  - Minimum API 29 (Android 10)
  - 8GB+ RAM
  - 10GB free storage

## Step 1: Clone the Repository

```bash
# Clone the repository
git clone https://github.com/yourusername/GemYum.git
cd GemYum

# Check out the main branch
git checkout main
```

## Step 2: Configure Environment

### Create Local Properties

```bash
# Copy the template
cp gradle.properties.template gradle.properties

# Edit gradle.properties and add your keys (optional)
nano gradle.properties
```

### Optional API Keys

The app works without these, but if you want full functionality:

1. **Hugging Face Token** (for model downloads)
   - Sign up at [huggingface.co](https://huggingface.co)
   - Generate token at [Settings → Access Tokens](https://huggingface.co/settings/tokens)
   - Add to gradle.properties: `HF_TOKEN=your_token_here`

2. **USDA API Key** (for nutrition updates)
   - Register at [FDC API](https://fdc.nal.usda.gov/api-guide.html)
   - Add to gradle.properties: `USDA_API_KEY=your_key_here`

## Step 3: Open in Android Studio

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the cloned GemYum directory
4. Click "OK"
5. Wait for Gradle sync to complete

## Step 4: Download AI Models

### Option A: Automatic Download (7.4GB)

```bash
# Create a script to download models
./scripts/download_gemma_models.sh
```

### Option B: Manual Download

```bash
# Download E2B model (3GB)
wget https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task

# Download E4B model (4.4GB) - Better accuracy
wget https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task

# Move to project
mkdir -p app/src/main/assets/models
mv *.task app/src/main/assets/models/
```

## Step 5: Build the Project

### Debug Build

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

### Release Build

```bash
# Build release APK
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

## Step 6: Run Tests

```bash
# Unit tests
./gradlew test

# Instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint checks
./gradlew lint

# All checks
./gradlew check
```

## Step 7: Setup Emulator (Optional)

### Create AVD for Testing

1. In Android Studio: Tools → AVD Manager
2. Create Virtual Device
3. Choose device: Pixel 6 or newer
4. System Image: API 31+ (Android 12+)
5. Advanced Settings:
   - RAM: 4GB minimum
   - Internal Storage: 16GB
   - SD Card: 8GB

### Emulator Performance Tips

```bash
# Enable hardware acceleration (Mac/Linux)
emulator -avd Pixel_6_API_31 -gpu host -accel on

# Increase RAM
emulator -avd Pixel_6_API_31 -memory 4096
```

## Development Workflow

### 1. Create Feature Branch

```bash
git checkout -b feature/your-feature-name
```

### 2. Make Changes

Follow the code style:
- Kotlin conventions
- Meaningful variable names
- Add comments for complex logic

### 3. Test Your Changes

```bash
# Run affected tests
./gradlew test

# Check code style
./gradlew ktlintCheck
```

### 4. Commit Changes

```bash
git add .
git commit -m "feat: add your feature description"
```

### 5. Push and Create PR

```bash
git push origin feature/your-feature-name
# Create PR on GitHub
```

## Common Issues & Solutions

### Issue: Gradle Sync Failed

```bash
# Clear Gradle cache
./gradlew clean
rm -rf ~/.gradle/caches/
./gradlew build --refresh-dependencies
```

### Issue: Out of Memory During Build

```gradle
// In gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m
```

### Issue: Model Files Too Large for Git

```bash
# Use Git LFS for large files
git lfs track "*.task"
git add .gitattributes
git commit -m "Track model files with LFS"
```

### Issue: Emulator Too Slow

- Enable hardware acceleration
- Use physical device for testing
- Reduce emulator resolution

## IDE Configuration

### Recommended Plugins

1. **Kotlin** - Built-in
2. **Compose Preview** - Built-in
3. **ADB Idea** - Quick ADB commands
4. **Key Promoter X** - Learn shortcuts

### Code Style Settings

1. Settings → Editor → Code Style → Kotlin
2. Set from: Kotlin style guide
3. Enable: Optimize imports on the fly

### Memory Settings

```
# In Android Studio: Help → Edit Custom VM Options
-Xms2g
-Xmx4g
-XX:ReservedCodeCacheSize=512m
```

## Debugging Tips

### 1. Enable Debug Logging

```kotlin
// In AppContainer.kt
private const val DEBUG = true

if (DEBUG) {
    Log.d(TAG, "Debug info here")
}
```

### 2. Use Layout Inspector

- Tools → Layout Inspector
- Select running app
- Inspect Compose hierarchy

### 3. Profile Performance

- Run → Profile 'app'
- Monitor CPU, Memory, Network
- Identify bottlenecks

### 4. Database Inspector

- View → Tool Windows → App Inspection
- Database Inspector tab
- Query nutrition database directly

## Useful Commands

```bash
# Clean everything
./gradlew clean

# Build without tests (faster)
./gradlew assemble -x test

# Install specific variant
./gradlew installDebug
./gradlew installRelease

# Generate APK and immediately install
./gradlew assembleDebug installDebug

# View dependencies
./gradlew app:dependencies

# Update dependencies
./gradlew dependencyUpdates
```

## Project Structure

```
GemYum/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/stel/gemmunch/
│   │   │   │   ├── ui/          # Compose UI
│   │   │   │   ├── viewmodels/  # ViewModels
│   │   │   │   ├── data/        # Data layer
│   │   │   │   ├── agent/       # AI logic
│   │   │   │   └── utils/       # Utilities
│   │   │   ├── res/             # Resources
│   │   │   └── AndroidManifest.xml
│   │   └── test/                # Unit tests
│   └── build.gradle.kts
├── gradle/
├── scripts/                     # Helper scripts
├── docs/                        # Documentation
└── README.md
```

## Next Steps

1. Read [ARCHITECTURE.md](ARCHITECTURE.md) to understand the codebase
2. Check [CONTRIBUTING.md](../CONTRIBUTING.md) for contribution guidelines
3. Join discussions on GitHub
4. Start with a small feature or bug fix

## Need Help?

- GitHub Issues: Report bugs
- Discussions: Ask questions
- Wiki: Find detailed guides

Happy coding! 🚀