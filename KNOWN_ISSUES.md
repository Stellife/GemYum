# GemYum - Known Issues & Workarounds

## Current Version: v1.0 (Kaggle Hackathon Submission)

### Model Initialization

**Issue:** First launch takes 2-3 minutes to initialize
- **Status:** Expected behavior
- **Details:** Gemma 3n models (3GB + 4.4GB) require significant initialization time on first load
- **Workaround:** Models are cached after first initialization. Subsequent launches take 10-30 seconds
- **User Impact:** One-time delay per installation

**Issue:** "Unable to open zip archive" error
- **Status:** Fixed in v1.0
- **Details:** Models were corrupted during copy process
- **Workaround:** Ensure models are pushed completely via installer script
- **Resolution:** Added verification and retry logic

### Storage & Installation

**Issue:** Device requires 10GB free storage
- **Status:** By design
- **Details:** 
  - E2B model: 3GB
  - E4B model: 4.4GB
  - App storage: ~500MB
  - Working space: ~2GB
- **Workaround:** Can run with E2B model only (3GB) for space-constrained devices
- **Future:** Implement model compression

**Issue:** Storage permission denied on Android 11+
- **Status:** Partially resolved
- **Details:** Scoped storage restrictions prevent direct /sdcard access
- **Workaround:** Models are placed in /data/local/tmp/ which is world-readable
- **Resolution:** Added permission requests and alternative paths

### Performance

**Issue:** Slow inference on non-NPU devices
- **Status:** Hardware limitation
- **Details:** 
  - NPU/GPU: 0.8-2 seconds per inference
  - CPU only: 10-30 seconds per inference
- **Workaround:** App automatically detects and uses best available hardware
- **Recommendation:** Use Pixel 6+ or devices with Snapdragon 8 Gen 2+

**Issue:** Session creation timeout
- **Status:** Fixed in v1.0
- **Details:** Session creation could hang indefinitely
- **Resolution:** Added 30-second timeout with fallback

### Chat Features

**Issue:** Deep Chat JSON responses may be truncated
- **Status:** Improved in v1.0
- **Details:** Complex meals with many ingredients could exceed token limits
- **Workaround:** JSON completion logic automatically repairs truncated responses
- **Resolution:** Added smart JSON repair and completion

**Issue:** Streaming responses may show incomplete sentences
- **Status:** Expected behavior
- **Details:** Token-by-token streaming shows partial outputs
- **Workaround:** Wait for complete response before acting on content

### Health Connect

**Issue:** Health Connect not available on all devices
- **Status:** Platform limitation
- **Details:** Requires Android 14+ or device with Health Connect app
- **Workaround:** App gracefully degrades - nutrition tracking works without Health Connect
- **Future:** Add export to CSV/JSON as alternative

**Issue:** Permission dialog may not appear
- **Status:** Under investigation
- **Details:** Some devices don't properly launch Health Connect permission screen
- **Workaround:** Grant permissions manually in Settings → Apps → GemYum → Permissions

### Camera & Image Processing

**Issue:** High-resolution images may cause memory issues
- **Status:** Mitigated
- **Details:** Very large photos (>20MP) can cause OutOfMemory errors
- **Workaround:** App automatically downscales images to safe resolution
- **Resolution:** Added bitmap recycling and memory management

**Issue:** Dark photos produce poor results
- **Status:** Model limitation
- **Details:** Gemma 3n vision model trained on well-lit images
- **Workaround:** Ensure good lighting when taking food photos
- **Future:** Add image enhancement preprocessing

### Database

**Issue:** Nutrient database takes time to load
- **Status:** Optimized
- **Details:** 700,000+ food items require indexing
- **Resolution:** Added database indexing and caching
- **Performance:** First search: 2-3s, subsequent: <500ms

**Issue:** Some branded foods missing nutrition data
- **Status:** Data limitation
- **Details:** Not all restaurant items have complete nutrition profiles
- **Workaround:** App falls back to similar generic foods
- **Future:** Crowd-source missing data

### Silicon Valley Easter Egg

**Issue:** "Hotdog Not Hotdog" triggers unexpectedly
- **Status:** Feature, not bug 😄
- **Details:** Any cylindrical food may trigger the easter egg
- **Workaround:** Enjoy the reference!

## Troubleshooting Steps

### App Won't Start
1. Check device has 8GB+ RAM
2. Ensure 10GB free storage
3. Clear app data: Settings → Apps → GemYum → Clear Data
4. Reinstall using installer script

### Models Not Loading
1. Verify models in /data/local/tmp/:
   ```bash
   adb shell ls -la /data/local/tmp/*.task
   ```
2. Re-push models:
   ```bash
   adb push models/*.task /data/local/tmp/
   ```
3. Clear app data and restart

### Slow Performance
1. Close other apps to free RAM
2. Disable battery saver mode
3. Ensure device isn't overheating
4. Check Settings → Model Selection → Use E2B (faster)

### Crashes
1. Check logcat for errors:
   ```bash
   adb logcat | grep -E "FATAL|ERROR.*gemyum"
   ```
2. Common causes:
   - Out of memory: Close other apps
   - Model corruption: Re-install models
   - Permission issues: Grant all requested permissions

## Feedback & Support

Please report issues to the development team with:
1. Device model and Android version
2. Steps to reproduce
3. Screenshots if applicable
4. Logcat output for crashes

## Planned Improvements

### v1.1 (Post-Hackathon)
- [ ] Model quantization for smaller size
- [ ] Batch processing for meal prep
- [ ] Barcode scanning integration
- [ ] Recipe import from URLs
- [ ] Weekly nutrition reports

### v2.0 (Future)
- [ ] Multi-language support
- [ ] Voice input for hands-free logging
- [ ] Restaurant GPS integration
- [ ] Social sharing features
- [ ] Meal planning assistant

---

*Last Updated: August 2025*
*For Kaggle Gemma 3n Hackathon Submission*