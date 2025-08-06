# GemYum - On-Device AI Nutrition Tracking

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" alt="GemYum Logo" width="128" height="128">
</p>

<p align="center">
  <strong>The world's first 100% on-device AI nutrition tracker powered by Google's Gemma 3n</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#demo">Demo</a> •
  <a href="#installation">Installation</a> •
  <a href="#building">Building</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#contributing">Contributing</a> •
  <a href="#license">License</a>
</p>

---

## 🏆 Kaggle Gemma 3n Hackathon Submission

This project was built for the [Google Gemma 3n Hackathon](https://www.kaggle.com/competitions/google-gemma-3n-hackathon) to showcase the power of on-device AI for privacy-preserving nutrition tracking.

## ✨ Features

### 🚀 Three Analysis Modes

1. **Quick Snap Insight** - Point, shoot, and get instant nutrition analysis in 0.8 seconds
2. **Deep Chat** - Have multimodal conversations about your meals with streaming AI responses
3. **Text-Only** - Describe your meal in text for fast analysis without photos

### 🔒 100% Privacy-First

- **No Cloud Required** - All AI inference happens on your device
- **No Data Collection** - Your nutrition data never leaves your phone
- **Works Offline** - Full functionality in airplane mode
- **No Subscriptions** - Free forever, no hidden costs

### ⚡ Blazing Fast Performance

- **0.8 second inference** on Pixel 9 Pro (NPU accelerated)
- **10-20x faster** than cloud-based solutions
- **Session pre-warming** for instant responses
- **Smart caching** for repeated queries

### 📊 Comprehensive Nutrition Database

- **700,000+ foods** from USDA and other sources
- **2,000+ restaurant items** with exact nutrition
- **600+ foods** with glycemic index data
- **226 nutrients** tracked per food item

### 🎯 Key Innovations

- **First nutrition app** with 100% on-device multimodal AI
- **NPU/GPU acceleration** with automatic hardware detection
- **RAG without embeddings** using SQL-based retrieval
- **Token-by-token streaming** for responsive chat
- **Silicon Valley Easter Egg** - Yes, it knows if it's a hotdog! 🌭

## 📱 Demo

### Quick Snap in Action
<img src="docs/images/quicksnap_demo.gif" alt="Quick Snap Demo" width="300">

### Performance Metrics
| Device | First Launch | Normal Launch | Inference Time |
|--------|--------------|---------------|----------------|
| Pixel 9 Pro | 2-3 min | 10-15 sec | **0.8 sec** |
| Pixel 7/8 | 2-3 min | 15-30 sec | 1-2 sec |
| Samsung S24 | 2-3 min | 20-40 sec | 2-5 sec |
| CPU Only | 3-4 min | 30-60 sec | 10-30 sec |

## 📋 Requirements

### Device Requirements
- Android 10+ (API level 29 or higher)
- Minimum 8GB RAM (12GB+ recommended for best performance)
- 10GB free storage space
- NPU/GPU recommended (but not required)

### Development Requirements
- Android Studio Ladybug or newer
- JDK 17+
- Android SDK 31+
- Kotlin 2.2.0+

## 🚀 Installation

### Option 1: Install Pre-built APK

1. Download the latest release from [Releases](https://github.com/yourusername/GemYum/releases)
2. Enable "Install from Unknown Sources" on your Android device
3. Install the APK
4. Launch GemYum and follow the setup wizard

### Option 2: Build from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/GemYum.git
cd GemYum

# Copy the configuration template
cp gradle.properties.template gradle.properties

# (Optional) Add your API keys to gradle.properties
# Note: The app works without these keys!

# Build the APK
./gradlew assembleRelease

# Install on connected device
adb install app/build/outputs/apk/release/app-release.apk
```

## 🔧 Configuration

### API Keys (Optional)

The app works completely without API keys, but if you want to contribute or update data:

1. **Hugging Face Token** (for model downloads)
   - Get from: https://huggingface.co/settings/tokens
   - Add to `gradle.properties`: `HF_TOKEN=your_token`

2. **USDA API Key** (for nutrition data updates)
   - Get from: https://fdc.nal.usda.gov/api-guide.html
   - Add to `gradle.properties`: `USDA_API_KEY=your_key`

### Model Installation

The app requires Gemma 3n models (7.4GB total):

#### Automatic Download (Requires Internet)
1. Launch the app
2. Tap "Download Models"
3. Wait for download to complete

#### Manual Installation (Offline)
```bash
# Download models
wget https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task
wget https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task

# Push to device
adb push gemma-3n-E2B-it-int4.task /data/local/tmp/
adb push gemma-3n-E4B-it-int4.task /data/local/tmp/

# Launch app - it will detect and use the models
```

## 🏗️ Architecture

### Technology Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **AI Framework:** MediaPipe LLM Inference API
- **Models:** Gemma 3n (2B/3N) with INT4 quantization
- **Database:** SQLite with FTS5 for fast search
- **Architecture:** MVVM with Clean Architecture principles

### Key Components

```
app/
├── src/main/java/com/stel/gemmunch/
│   ├── ui/                    # Compose UI screens
│   ├── viewmodels/            # ViewModels for each screen
│   ├── agent/                 # AI inference and analysis
│   ├── data/                  # Data models and repositories
│   ├── utils/                 # Utility classes
│   └── AppContainer.kt        # Dependency injection
└── src/main/assets/
    └── databases/
        └── nutrients.db        # Pre-built nutrition database
```

### Core Innovations

1. **Session Pre-warming** - Background session creation for instant responses
2. **Hardware Acceleration** - Automatic NPU/GPU/CPU fallback chain
3. **Token Streaming** - Real-time UI updates during generation
4. **RAG without Embeddings** - SQL-based retrieval for nutrition data
5. **Smart Quantization** - INT4 models with minimal accuracy loss

## 🧪 Testing

```bash
# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest

# Run lint checks
./gradlew lint
```

## 📚 Documentation

- [Architecture Overview](docs/ARCHITECTURE.md)
- [Model Integration Guide](docs/MODEL_INTEGRATION.md)
- [Database Schema](docs/DATABASE_SCHEMA.md)
- [API Documentation](docs/API.md)
- [Contributing Guide](CONTRIBUTING.md)

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Quick Start for Contributors

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Code Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add comments for complex logic
- Write unit tests for new features

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

### Third-Party Licenses

- **Gemma 3n Models** - [Gemma Terms of Use](https://ai.google.dev/gemma/terms)
- **MediaPipe** - Apache License 2.0
- **USDA Food Database** - Public Domain
- **Android Jetpack** - Apache License 2.0

## 🙏 Acknowledgments

- **Google** for Gemma 3n and MediaPipe
- **Kaggle** for hosting the hackathon
- **USDA** for the comprehensive food database
- **Open Food Facts** for additional nutrition data
- **Silicon Valley** for the hotdog inspiration 🌭

## 📮 Contact

- **GitHub Issues:** [Report bugs or request features](https://github.com/yourusername/GemYum/issues)
- **Discussions:** [Join the conversation](https://github.com/yourusername/GemYum/discussions)

## 🌟 Star History

[![Star History Chart](https://api.star-history.com/svg?repos=yourusername/GemYum&type=Date)](https://star-history.com/#yourusername/GemYum&Date)

---

<p align="center">
  Built with ❤️ for the Kaggle Gemma 3n Hackathon 2025
</p>

<p align="center">
  <strong>Your nutrition data belongs to you.</strong>
</p>