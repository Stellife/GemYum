@echo off
setlocal EnableDelayedExpansion

echo ======================================
echo    GemYum Offline Installer v1.0     
echo ======================================
echo.

REM Check if ADB is available
adb version >nul 2>&1
if errorlevel 1 (
    echo ERROR: ADB not found.
    echo.
    echo Please install Android SDK Platform Tools:
    echo   https://developer.android.com/studio/releases/platform-tools
    echo.
    echo 1. Download the Windows platform-tools zip
    echo 2. Extract to C:\platform-tools
    echo 3. Add C:\platform-tools to your PATH
    pause
    exit /b 1
)

REM Check device connection
echo Checking for connected Android device...
adb devices | find "device" >nul
if errorlevel 1 (
    echo ERROR: No Android device found.
    echo.
    echo Please ensure:
    echo   1. Your Android device is connected via USB
    echo   2. Developer Options are enabled
    echo   3. USB Debugging is turned on
    echo   4. You've trusted this computer when prompted
    echo.
    echo To enable Developer Options:
    echo   Settings ^> About Phone ^> Tap 'Build Number' 7 times
    pause
    exit /b 1
)

for /f "tokens=1" %%i in ('adb devices ^| findstr device$') do set DEVICE=%%i
echo Device found: %DEVICE%
echo.

REM Check for required files
if not exist "GemYum-v1.0-sideload.apk" (
    echo ERROR: GemYum-v1.0-sideload.apk not found.
    echo Please ensure you're running this script from the package directory.
    pause
    exit /b 1
)

if not exist "models\gemma-3n-E2B-it-int4.task" (
    echo ERROR: Model files not found in models\ directory.
    echo Expected files:
    echo   models\gemma-3n-E2B-it-int4.task ^(3GB^)
    echo   models\gemma-3n-E4B-it-int4.task ^(4.4GB^)
    pause
    exit /b 1
)

if not exist "models\gemma-3n-E4B-it-int4.task" (
    echo ERROR: Model files not found in models\ directory.
    echo Expected files:
    echo   models\gemma-3n-E2B-it-int4.task ^(3GB^)
    echo   models\gemma-3n-E4B-it-int4.task ^(4.4GB^)
    pause
    exit /b 1
)

REM Install APK
echo Installing GemYum APK...
adb install -r GemYum-v1.0-sideload.apk
if errorlevel 1 (
    echo ERROR: APK installation failed.
    echo Try uninstalling the existing app first:
    echo   adb uninstall com.stel.gemyum
    pause
    exit /b 1
)
echo APK installed successfully
echo.

REM Copy models
echo Copying AI models to device...
echo This will take several minutes. Please be patient.
echo.

echo [1/2] Copying E2B model (3GB)...
adb push models\gemma-3n-E2B-it-int4.task /data/local/tmp/
if errorlevel 1 (
    echo Failed to copy E2B model
) else (
    echo E2B model copied successfully
)

echo.
echo [2/2] Copying E4B model (4.4GB)...
adb push models\gemma-3n-E4B-it-int4.task /data/local/tmp/
if errorlevel 1 (
    echo Failed to copy E4B model
) else (
    echo E4B model copied successfully
)

echo.
echo ======================================
echo Installation complete!
echo ======================================
echo.
echo You can now launch GemYum from your device.
echo The app will automatically detect the pre-installed models.
echo.
echo First launch may take 2-3 minutes to initialize.
echo Subsequent launches will be much faster (10-30 seconds).
echo.
echo Enjoy using GemYum - AI-powered nutrition tracking!
pause