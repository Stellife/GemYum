# GemMunch Automated Testing Suite

This directory contains a comprehensive automated testing suite for the GemMunch Android app. The testing suite can automatically test food recognition, nutrition analysis, and glycemic index calculations using real device interactions.

## 🚀 Quick Start

1. **Setup Environment:**
   ```bash
   python setup_testing_environment.py
   ```

2. **Run Tests:**
   ```bash
   python run_tests.py
   ```

## 📁 Files Overview

### Core Testing Scripts

- **`run_tests.py`** - Main test runner script with auto-detection
- **`enhanced_automated_testing.py`** - Advanced testing with UI automation
- **`automated_testing.py`** - Basic testing using ADB commands
- **`setup_testing_environment.py`** - Environment setup and verification

### Helper Modules

- **`ui_automator_helper.py`** - UI automation utilities using uiautomator2

## 🛠️ Prerequisites

### Required Software

1. **Android SDK Platform Tools** (for ADB)
   - Download from: https://developer.android.com/studio/releases/platform-tools
   - Ensure `adb` is in your system PATH

2. **Python 3.7+** with pip

3. **scrcpy** (optional, for screen recording)
   - Download from: https://github.com/Genymobile/scrcpy
   - Required for video recording of tests

### Required Python Packages

The setup script will install these automatically:
- `uiautomator2` - Advanced UI automation
- `Pillow` - Image processing
- `requests` - HTTP requests for downloading test data

### Android Device Setup

1. **Enable Developer Options:**
   - Go to Settings > About Phone
   - Tap "Build Number" 7 times

2. **Enable USB Debugging:**
   - Go to Settings > Developer Options
   - Enable "USB Debugging"

3. **Connect Device:**
   - Connect via USB cable, or
   - Connect via WiFi ADB (requires initial USB setup)

4. **Install GemMunch App:**
   - Install the GemMunch APK on your testing device

## 📋 Usage Examples

### Basic Usage

```bash
# Setup environment and run tests
python run_tests.py

# Run only setup check
python run_tests.py --setup-only

# Run basic tests (no UI Automator)
python run_tests.py --basic

# Run enhanced tests with UI Automator
python run_tests.py --enhanced
```

### Advanced Options

```bash
# Specify device ID (useful with multiple devices)
python run_tests.py --device-id emulator-5554

# Custom output directory
python run_tests.py --output-dir my_test_results

# Disable screen recording
python run_tests.py --no-recording

# Custom analysis timeout
python run_tests.py --timeout 180

# Skip environment setup check
python run_tests.py --skip-setup
```

### Direct Script Usage

```bash
# Run enhanced tests directly
python enhanced_automated_testing.py --device-id emulator-5554

# Run basic tests directly
python automated_testing.py --no-recording --timeout 90
```

## 🧪 Test Categories

The testing suite automatically tests all images in the `test_images` directory:

### Food Categories
- **Breakfast:** eggs_bacon.jpg, pancakes.jpg
- **Burgers:** classic_burger.jpg, double_burger.jpg
- **Pizza:** margherita_pizza.jpg, pepperoni_pizza.jpg
- **Salads:** caesar_salad.jpg, greek_salad.jpg
- **Tacos:** hard_shell_tacos_3.jpg, street_tacos.jpg
- **Restaurant Meals:** chipotle_bowl.jpg, pho_bowl.jpg, sushi_platter.jpg

### Test Scenarios
- **Challenging Cases:** dark_lighting.jpg, messy_plate.jpg, partial_food.jpg
- **Complex Meals:** bento_box.jpg, thanksgiving_plate.jpg

## 📊 Test Results

### Output Files

Tests generate comprehensive results in the `test_results` directory:

- **`test_report_YYYYMMDD_HHMMSS.json`** - Detailed JSON report
- **`test_summary_YYYYMMDD_HHMMSS.txt`** - Human-readable summary
- **`validation_report_YYYYMMDD_HHMMSS.txt`** - Validation against expected values
- **Screenshots** - Before/after/error screenshots for each test
- **Recordings** - MP4 video recordings of each test (if enabled)

### Metrics Tracked

- **Analysis Time:** How long each food analysis takes
- **Success Rate:** Percentage of successful analyses
- **Nutrition Accuracy:** Comparison against expected values
- **Food Identification:** Accuracy of food recognition
- **Glycemic Index Validation:** GI value accuracy testing

## 🔧 Troubleshooting

### Common Issues

1. **"ADB not found"**
   - Install Android SDK Platform Tools
   - Add ADB to system PATH

2. **"No devices connected"**
   - Enable USB debugging on device
   - Check USB cable connection
   - Try `adb devices` to verify connection

3. **"UI Automator connection failed"**
   - Install uiautomator2: `pip install uiautomator2`
   - Ensure device is properly connected
   - Try running tests with `--no-ui-automator`

4. **"GemMunch app not found"**
   - Install the GemMunch APK on your test device
   - Verify package name matches `com.stel.gemmunch`

5. **Screen recording fails**
   - Install scrcpy from official releases
   - Add scrcpy to system PATH
   - Use `--no-recording` to disable recording

### Device Compatibility

- **Minimum Android Version:** Android 7.0 (API 24)
- **Recommended:** Android 9.0+ for best UI Automator support
- **Physical Devices:** Recommended for realistic testing
- **Emulators:** Supported but may have different performance characteristics

## 🔍 Advanced Configuration

### Custom Test Scenarios

Edit `../test_images/curated_tests/test_scenarios.json` to define expected results for validation:

```json
{
  "glycemic_index_tests": {
    "high_gi": [
      {
        "name": "pancakes.jpg",
        "expected_gi": 67
      }
    ]
  }
}
```

### Environment Variables

Set these environment variables for additional configuration:

- `ANDROID_HOME` - Android SDK location
- `ADB_SERVER_SOCKET` - Custom ADB server socket
- `GEMMUNCH_PACKAGE` - Override app package name

## 📈 Performance Optimization

### Test Speed

- Use `--no-recording` to speed up tests
- Reduce `--timeout` for faster failing tests
- Use physical devices instead of emulators
- Close unnecessary apps on test device

### Reliability

- Ensure stable USB connection
- Use consistent device orientation
- Clear device storage if needed
- Restart ADB server if connection issues occur

## 🤝 Contributing

### Adding New Tests

1. Add test images to appropriate category in `test_images/`
2. Update `test_scenarios.json` with expected results
3. Run validation tests to verify accuracy

### Improving Automation

- UI element selectors in `ui_automator_helper.py`
- Analysis completion detection logic
- Result extraction patterns

### Reporting Issues

Include this information when reporting issues:
- Device model and Android version
- Python version and installed packages
- Complete error output
- Test images that failed

## 📄 License

This testing suite is part of the GemMunch project and follows the same licensing terms.