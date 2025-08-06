# Public Repository Setup Plan for GemYum

## Repository Structure

```
stel/GemYum/
├── README.md                    # Main documentation (see below)
├── LICENSE                      # Apache 2.0
├── CONTRIBUTING.md             # Contribution guidelines
├── CODE_OF_CONDUCT.md          # Community standards
├── .gitignore                  # Comprehensive ignore file
├── .env.example                # Template for local setup
│
├── app/                        # Android app source
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/stel/gemyum/   # Source code
│   │   │   ├── res/                    # Resources
│   │   │   └── AndroidManifest.xml
│   │   └── test/                       # Unit tests
│   └── build.gradle.kts
│
├── models/                     # Model management
│   ├── README.md              # Model setup instructions
│   ├── download_models.sh     # Auto-download script
│   └── .gitkeep              # Placeholder
│
├── scripts/                   # Build and setup scripts
│   ├── setup.sh              # One-click setup
│   ├── build_nutrients_db.py # Database builder
│   ├── create_test_apk.sh   # Build test version
│   └── README.md            # Script documentation
│
├── docs/                     # Documentation
│   ├── SETUP.md             # Detailed setup guide
│   ├── ARCHITECTURE.md      # Technical architecture
│   ├── TROUBLESHOOTING.md   # Common issues
│   ├── API.md               # Code documentation
│   └── images/              # Screenshots and diagrams
│
├── releases/                # Release artifacts
│   └── .gitkeep            # Will contain APKs via GitHub Releases
│
├── gradle/                  # Gradle wrapper
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
└── build.gradle.kts
```

## Files to EXCLUDE (Security Critical)

```gitignore
# NEVER commit these
*.keystore
*.jks
local.properties
secrets.properties
*.env
!.env.example

# API Keys (should never exist in code)
**/ApiKeys.kt
**/Secrets.kt

# Models (too large, download separately)
*.tflite
*.bin
*.onnx
models/gemma*
models/*/

# Databases (build locally)
*.db
*.sqlite
!*.db.example

# Personal data
app/src/main/assets/nutrients.db
scripts/nutrition_data/
user_feedback/
test_photos/

# IDE
.idea/
*.iml
.gradle/
/local.properties
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
```

## Essential Files to Include

### 1. README.md (Main Entry Point)

```markdown
# 🍕 GemYum - On-Device AI Nutrition Tracker

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Model](https://img.shields.io/badge/Model-Gemma_3n-red.svg)](https://ai.google.dev/gemma)

GemYum is the world's first nutrition tracker that runs entirely on-device using Google's Gemma 3n model. No internet, no cloud, complete privacy.

## ✨ Features

- **100% On-Device**: All AI processing happens on your phone
- **Instant Analysis**: 0.8-2 second food recognition
- **Privacy First**: Your photos never leave your device
- **Offline Capable**: Works without internet
- **900K+ Foods**: Comprehensive nutrition database
- **Glycemic Tracking**: Unique GI/GL support for diabetics

## 📱 Demo

[Video Link] | [Try APK](releases/)

## 🚀 Quick Start

### Option 1: Install Pre-built APK (Easiest)

1. Download the latest APK from [Releases](https://github.com/stel/GemYum/releases)
2. Enable "Install from Unknown Sources" on your Android device
3. Install and enjoy!

### Option 2: Build from Source

```bash
# Clone the repository
git clone https://github.com/stel/GemYum.git
cd GemYum

# Run setup script (downloads models, builds database)
./scripts/setup.sh

# Build and install
./gradlew installDebug
```

## 📋 Requirements

- Android 7.0+ (API 24+)
- 2GB storage space (for models)
- 4GB+ RAM recommended
- GPU/NPU for best performance

## 🛠️ Development Setup

See [SETUP.md](docs/SETUP.md) for detailed instructions.

### Prerequisites

- Android Studio Arctic Fox or newer
- JDK 17+
- Android SDK 34
- Python 3.8+ (for database scripts)

### Building the Nutrition Database

```bash
cd scripts
python build_nutrients_db.py
# This will create a 45MB database from public sources
```

### Downloading Models

Models are not included in the repository due to size. Download them:

```bash
cd models
./download_models.sh
# Downloads Gemma 3n models (1.4GB)
```

## 🏗️ Architecture

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **AI**: MediaPipe AI Edge + Gemma 3n
- **Database**: SQLite with FTS5
- **DI**: Manual (no Dagger/Hilt for simplicity)

See [ARCHITECTURE.md](docs/ARCHITECTURE.md) for details.

## 🤝 Contributing

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## 📄 License

This project is licensed under Apache 2.0 - see [LICENSE](LICENSE) file.

### Third-party Licenses

- Gemma 3n: [Gemma Terms of Use](https://ai.google.dev/gemma/terms)
- MediaPipe: Apache 2.0
- Nutrition data: Public domain / USDA

## 🙏 Acknowledgments

- Google DeepMind for Gemma 3n
- MediaPipe team for AI Edge SDK
- USDA for nutrition database
- Open Food Facts for additional data

## 📧 Contact

- **Kaggle Competition**: [Link to submission]
- **Issues**: [GitHub Issues](https://github.com/stel/GemYum/issues)
- **Discussions**: [GitHub Discussions](https://github.com/stel/GemYum/discussions)

---

Built with ❤️ for the Google Gemma 3n Hackathon 2024
```

### 2. .env.example

```bash
# Copy this to .env and fill in your values

# Build Configuration
BUILD_TYPE=debug
ENABLE_PROGUARD=false

# Model Configuration
MODEL_PATH=/path/to/models
USE_BUNDLED_MODELS=false

# Database
DB_PATH=/path/to/nutrients.db
BUILD_DB_FROM_SOURCE=true

# Testing
TEST_MODE=false
MOCK_AI_RESPONSES=false

# Optional: Crash Reporting (if you add it)
# CRASHLYTICS_API_KEY=your_key_here
```

### 3. docs/SETUP.md

```markdown
# Detailed Setup Guide

## Prerequisites

### System Requirements
- macOS, Linux, or Windows 10+
- 8GB RAM minimum (16GB recommended)
- 10GB free disk space

### Software Requirements
1. **Android Studio Arctic Fox** or newer
   - Download: https://developer.android.com/studio
   
2. **JDK 17**
   ```bash
   # macOS
   brew install openjdk@17
   
   # Ubuntu
   sudo apt install openjdk-17-jdk
   ```

3. **Python 3.8+**
   ```bash
   python --version  # Should be 3.8 or higher
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
# Edit .env with your configuration
```

### 3. Download AI Models

The Gemma 3n models (1.4GB) are required but not included in the repo.

#### Automatic Download
```bash
cd models
./download_models.sh
```

#### Manual Download
1. Visit [Kaggle Gemma Page](https://www.kaggle.com/models/google/gemma)
2. Download `gemma-3n-it-gpu-int4.tflite`
3. Place in `models/` directory

### 4. Build Nutrition Database

```bash
cd scripts
pip install -r requirements.txt
python build_nutrients_db.py
```

This creates a 45MB SQLite database from public nutrition sources.

### 5. Open in Android Studio

1. Open Android Studio
2. Select "Open Existing Project"
3. Navigate to the GemYum folder
4. Wait for Gradle sync

### 6. Configure Device/Emulator

#### Physical Device (Recommended)
1. Enable Developer Options
2. Enable USB Debugging
3. Connect via USB

#### Emulator (Limited)
- **Note**: NPU acceleration not available
- Performance will be significantly slower
- Minimum: Pixel 6 API 31 with 4GB RAM

### 7. Build and Run

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Or use Android Studio's Run button
```

## Troubleshooting

### Common Issues

#### "Model file not found"
- Ensure models are in `app/src/main/assets/models/`
- Run `./models/download_models.sh`

#### "Database not found"
- Build database: `python scripts/build_nutrients_db.py`
- Check path in `.env`

#### "Out of memory" during build
```bash
# Increase Gradle heap
echo "org.gradle.jvmargs=-Xmx4g" >> gradle.properties
```

#### Slow inference on emulator
- Emulators don't support NPU/GPU acceleration
- Use a physical device for testing

## Next Steps

- Read [ARCHITECTURE.md](ARCHITECTURE.md) to understand the codebase
- Check [CONTRIBUTING.md](../CONTRIBUTING.md) to start contributing
- Join discussions on [GitHub Discussions](https://github.com/stel/GemYum/discussions)
```

### 4. scripts/setup.sh

```bash
#!/bin/bash

echo "🚀 GemYum Setup Script"
echo "======================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check prerequisites
check_command() {
    if ! command -v $1 &> /dev/null; then
        echo -e "${RED}❌ $1 is not installed${NC}"
        exit 1
    else
        echo -e "${GREEN}✓ $1 found${NC}"
    fi
}

echo "Checking prerequisites..."
check_command git
check_command python3
check_command java

# Create necessary directories
echo -e "\n${YELLOW}Creating directories...${NC}"
mkdir -p models
mkdir -p app/src/main/assets/models
mkdir -p app/src/main/assets/databases

# Download models
echo -e "\n${YELLOW}Downloading AI models...${NC}"
if [ ! -f "models/gemma-3n.tflite" ]; then
    echo "Downloading Gemma 3n model (1.4GB)..."
    # Using wget with progress bar
    wget --show-progress -q -O models/gemma-3n.tflite \
        "https://huggingface.co/google/gemma-3n/resolve/main/gemma-3n.tflite"
    
    # Copy to assets
    cp models/gemma-3n.tflite app/src/main/assets/models/
    echo -e "${GREEN}✓ Models downloaded${NC}"
else
    echo -e "${GREEN}✓ Models already exist${NC}"
fi

# Build nutrition database
echo -e "\n${YELLOW}Building nutrition database...${NC}"
cd scripts
if [ ! -f "nutrients.db" ]; then
    pip3 install -r requirements.txt
    python3 build_nutrients_db.py
    cp nutrients.db ../app/src/main/assets/databases/
    echo -e "${GREEN}✓ Database built${NC}"
else
    echo -e "${GREEN}✓ Database already exists${NC}"
fi
cd ..

# Create local.properties if it doesn't exist
if [ ! -f "local.properties" ]; then
    echo -e "\n${YELLOW}Creating local.properties...${NC}"
    echo "sdk.dir=$ANDROID_HOME" > local.properties
    echo -e "${GREEN}✓ local.properties created${NC}"
fi

# Final build
echo -e "\n${YELLOW}Building APK...${NC}"
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo -e "\n${GREEN}✨ Setup complete!${NC}"
    echo -e "${GREEN}APK location: app/build/outputs/apk/debug/app-debug.apk${NC}"
    echo -e "\nNext steps:"
    echo "1. Connect your Android device"
    echo "2. Run: ./gradlew installDebug"
else
    echo -e "\n${RED}❌ Build failed. Check the errors above.${NC}"
    exit 1
fi
```

### 5. CONTRIBUTING.md

```markdown
# Contributing to GemYum

We love your input! We want to make contributing to GemYum as easy and transparent as possible.

## Development Process

1. Fork the repo and create your branch from `main`
2. If you've added code, add tests
3. If you've changed APIs, update the documentation
4. Ensure the test suite passes
5. Make sure your code follows the existing style
6. Issue a pull request!

## Code Style

- Kotlin coding conventions
- Max line length: 120 characters
- Use meaningful variable names
- Comment complex logic

## Pull Request Process

1. Update README.md with details of changes if needed
2. Update docs/ if you change functionality
3. The PR will be merged once you have approval

## Any contributions you make will be under the Apache 2.0 License

When you submit code changes, your submissions are understood to be under the same [Apache 2.0 License](LICENSE) that covers the project.

## Report bugs using GitHub Issues

We use GitHub issues to track public bugs. Report a bug by [opening a new issue](https://github.com/stel/GemYum/issues/new).

## Write bug reports with detail, background, and sample code

**Great Bug Reports** tend to have:

- A quick summary and/or background
- Steps to reproduce
  - Be specific!
  - Give sample code if you can
- What you expected would happen
- What actually happens
- Notes (possibly including why you think this might be happening)

## License

By contributing, you agree that your contributions will be licensed under Apache 2.0.
```

## Security Considerations

### What NOT to Include

1. **Never commit**:
   - API keys (even defunct ones)
   - Signing keys/keystores
   - User data or photos
   - Personal information
   - Proprietary datasets

2. **Use placeholders for**:
   - Model download URLs (until public)
   - Database sources
   - Third-party service endpoints

### Sensitive File Handling

```kotlin
// ApiKeys.kt - DO NOT COMMIT
object ApiKeys {
    // This file should be created locally
    // Copy from ApiKeys.kt.example
    const val SOME_SERVICE = "your_key_here"
}

// ApiKeys.kt.example - COMMIT THIS
object ApiKeys {
    // Copy this file to ApiKeys.kt and add your keys
    const val SOME_SERVICE = "ADD_YOUR_KEY_HERE"
}
```

## Repository Settings on GitHub

1. **Description**: "On-device AI nutrition tracker using Gemma 3n. Private, fast, offline-capable."

2. **Topics**: 
   - `android`
   - `kotlin`
   - `gemma`
   - `mediapipe`
   - `on-device-ai`
   - `nutrition`
   - `food-tracker`
   - `jetpack-compose`

3. **Settings**:
   - ✅ Issues enabled
   - ✅ Projects enabled
   - ✅ Wiki enabled
   - ✅ Discussions enabled
   - ⬜ Sponsorships (optional)

4. **Branch Protection** (main):
   - ⬜ Require PR reviews (for now)
   - ✅ Dismiss stale reviews
   - ✅ Include administrators

5. **Secrets** (if using Actions):
   - `RELEASE_KEYSTORE_BASE64`
   - `RELEASE_KEYSTORE_PASSWORD`
   - `RELEASE_KEY_ALIAS`
   - `RELEASE_KEY_PASSWORD`

## Initial Commit Strategy

```bash
# First, clean sensitive data
./scripts/clean_for_public.sh

# Initialize new repo
git init
git remote add origin https://github.com/stel/GemYum.git

# Initial commit
git add .
git commit -m "Initial commit: GemYum - On-device AI nutrition tracker

- Core Android app with Jetpack Compose UI
- MediaPipe AI Edge integration
- Gemma 3n model support
- Nutrition database builder
- Setup and build scripts

Built for Google Gemma 3n Hackathon 2024"

git push -u origin main

# Create initial release
git tag v0.1.0
git push origin v0.1.0
```

## Launch Checklist

- [ ] Remove all sensitive data
- [ ] Test clean clone and build
- [ ] Verify .gitignore is comprehensive
- [ ] Add LICENSE file
- [ ] Complete README with screenshots
- [ ] Test all setup scripts
- [ ] Create initial GitHub Release with APK
- [ ] Add demo video link
- [ ] Enable GitHub Pages for docs (optional)
- [ ] Submit to Kaggle competition

This strategy ensures a clean, professional public repository that others can actually use while protecting sensitive information.