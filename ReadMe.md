# GemYum - On-Device AI Nutrition Tracking

[![Kaggle Competition](https://img.shields.io/badge/Kaggle-Gemma%20Sprint-blue)](https://www.kaggle.com/competitions/gemma-sprint-2025)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-API%2024%2B-brightgreen)](https://developer.android.com/)

## 🏆 Kaggle Gemma Sprint 2025 Submission

GemYum is a privacy-first Android application that uses Google's Gemma 3n model for on-device food recognition and nutrition tracking. Built for the Kaggle Gemma Sprint AI competition, it demonstrates practical edge AI applications in health and wellness.

## ✨ Key Features

- **🤖 On-Device AI**: Runs Gemma 3n model locally - no cloud required
- **📸 Instant Food Recognition**: Point camera at food for instant nutrition data
- **📊 Comprehensive Database**: 700+ foods with glycemic index data
- **🔒 Privacy First**: All processing happens on your device
- **📱 Offline Capable**: Works without internet connection
- **💚 Health Connect Integration**: Syncs with Android Health platform

## 📦 Downloads

- **[Latest Release (v1.4.0)](https://drive.google.com/file/d/16-xZpzdZA3NEv48slvQVASs6c6MYEBCF/view?usp=drive_link)** - Standard APK (requires internet for models)
- **[Offline Package](https://drive.google.com/drive/folders/1jvMmeec--PYCZfIY3sXKF5LM9YHlM5w-?usp=drive_link)** - Pre-bundled with AI models
- **[All Releases](RELEASES.md)** - Version history and changelogs

## 🚀 Quick Start

### Prerequisites
- Android device (API 24+) with 6GB+ RAM
- 5GB free storage for AI models
- WiFi/Internet connection for initial model download
- Android Studio (for building from source)

### Installation

#### Option 1: Download Pre-built APK (Recommended)

1. **Download GemYum v1.4.0**
   - [Download APK from Google Drive](https://drive.google.com/file/d/16-xZpzdZA3NEv48slvQVASs6c6MYEBCF/view?usp=drive_link)
   - Enable "Install from Unknown Sources" in Android settings
   - Install the APK on your device

2. **Download AI Models (Required)**
   - Launch the app after installation
   - Connect to WiFi/Internet for model download
   - The app will download Gemma 3n models (~3-4GB):
     - [E2B Model](https://www.kaggle.com/models/google/gemma-3n/tfLite/gemma-3n-E2B-it-int4) - 2GB (Required)
     - [E4B Model](https://www.kaggle.com/models/google/gemma-3n/tfLite/gemma-3n-E4B-it-int4) - 4GB (Optional, better accuracy)
   - Models are downloaded once and work offline thereafter

#### Option 2: Offline Installation (No Internet Required)
For devices without internet access, use the pre-bundled APK with models:
- [Offline Installation Package](https://drive.google.com/drive/folders/1jvMmeec--PYCZfIY3sXKF5LM9YHlM5w-?usp=drive_link)
- Contains APK with embedded AI models (larger file size ~6GB)
- No internet connection required after installation

### Building from Source

```bash
# Clone repository
git clone https://github.com/Stellife/GemYum.git
cd GemYum

# Build APK
./gradlew assembleRelease

# Install on device
adb install app/build/outputs/apk/release/app-release.apk
```

## 📱 Usage

1. **Setup**: Download AI models on first launch
2. **Camera Mode**: Point at food for instant recognition
3. **Search**: Type food names for nutrition lookup
4. **Track**: View daily nutrition summaries
5. **Health Sync**: Export to Health Connect

## 🏗️ Technical Architecture

- **AI Model**: Gemma 3n (3B parameters) quantized for mobile
- **Framework**: TensorFlow Lite with GPU acceleration
- **Database**: SQLite with 700+ foods and glycemic index
- **UI**: Jetpack Compose with Material 3
- **Platform**: Android (Kotlin)

## 📊 Model Performance

- **Inference Time**: ~2-3 seconds per image
- **Accuracy**: 85%+ on common foods
- **Memory Usage**: ~4GB during inference
- **Battery Impact**: Minimal with GPU acceleration

## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md) for details.

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Google for the Gemma model and TensorFlow Lite
- Kaggle for hosting the competition
- The Android ML community for inspiration

## 📧 Contact

For questions about this Kaggle submission, please open an issue on GitHub.

---

**Competition Entry**: This project is submitted to the [Kaggle Gemma Sprint 2025](https://www.kaggle.com/competitions/gemma-sprint-2025) competition.
