# GemYum Releases

## v1.4.0 - Latest Release (August 2025)

### Downloads
- **[GemYum v1.4.0 APK](https://drive.google.com/file/d/16-xZpzdZA3NEv48slvQVASs6c6MYEBCF/view?usp=drive_link)** (262MB)
  - Requires WiFi/Internet for initial model download
  - Models are downloaded from Kaggle on first launch
  
- **[Offline Installation Package](https://drive.google.com/drive/folders/1jvMmeec--PYCZfIY3sXKF5LM9YHlM5w-?usp=drive_link)** (~6GB)
  - Pre-bundled with Gemma 3n models
  - No internet connection required
  - Ideal for devices without network access

### What's New in v1.4.0
- ✅ Enhanced nutrition database with 716 foods
- ✅ Comprehensive glycemic index data (61% coverage)
- ✅ Improved model download experience
- ✅ Better error handling and user feedback
- ✅ Optimized for Google - The Gemma 3n Impact Challenge (8/6/25)

### AI Models
The app uses Google's Gemma 3n models for on-device inference:

1. **Gemma 3n E2B (Required)**
   - Model: [gemma-3n-E2B-it-int4](https://www.kaggle.com/models/google/gemma-3n/tfLite/gemma-3n-E2B-it-int4)
   - Size: ~2GB
   - Purpose: Primary food recognition and analysis

2. **Gemma 3n E4B (Optional)**
   - Model: [gemma-3n-E4B-it-int4](https://www.kaggle.com/models/google/gemma-3n/tfLite/gemma-3n-E4B-it-int4)
   - Size: ~4GB
   - Purpose: Enhanced accuracy for complex meals

### Installation Instructions

#### Standard Installation (Internet Required)
1. Download the [v1.4.0 APK](https://drive.google.com/file/d/16-xZpzdZA3NEv48slvQVASs6c6MYEBCF/view?usp=drive_link)
2. Enable "Install from Unknown Sources" in Android settings
3. Install the APK
4. Launch the app and connect to WiFi
5. Follow the setup wizard to download models
6. Models are cached locally for offline use

#### Offline Installation (No Internet)
1. Download the [Offline Package](https://drive.google.com/drive/folders/1jvMmeec--PYCZfIY3sXKF5LM9YHlM5w-?usp=drive_link)
2. Extract the package
3. Follow the included installation guide
4. Models are pre-bundled - works immediately

### System Requirements
- Android 10+ (API 29+)
- 6GB+ RAM (8GB recommended)
- 5GB free storage
- GPU/NPU acceleration supported

### Known Issues
- Initial model download requires stable WiFi connection
- E4B model initialization may take 2-3 minutes on first launch
- Some devices may experience slower inference without GPU support

### Support
For issues or questions, please open an issue on [GitHub](https://github.com/Stellife/GemYum/issues)

---

## Previous Releases

### v1.3.x Series
- Internal testing builds
- Iterative improvements to model loading
- Database enhancements

### v1.0 - Initial Release
- Core functionality implementation
- Basic food recognition
- Initial nutrition database