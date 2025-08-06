#!/usr/bin/env python3
"""
Direct test that navigates to Quick Snap without using Photos app.
"""

import subprocess
import time
import json
from datetime import datetime
from pathlib import Path

def run_command(cmd):
    """Run a command and return output."""
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return result.stdout.strip()

def main():
    print("🧪 GemMunch Direct Test")
    print("=" * 50)
    
    # Get device ID
    device_id = run_command("adb devices | grep device$ | cut -f1").split('\n')[0]
    if not device_id:
        print("❌ No device connected")
        return
    
    print(f"✅ Connected to device: {device_id}")
    
    # Launch GemMunch
    print("🚀 Launching GemMunch...")
    run_command(f"adb -s {device_id} shell am start -n com.stel.gemmunch.debug/com.stel.gemmunch.ui.MainActivity")
    time.sleep(3)
    
    # Take screenshot
    run_command(f"adb -s {device_id} exec-out screencap -p > test_results/home_screen.png")
    print("📸 Screenshot saved: home_screen.png")
    
    # Try to find and tap Quick Snap button
    # First, let's see what's on screen
    print("\n🔍 Analyzing UI elements...")
    ui_dump = run_command(f"adb -s {device_id} shell uiautomator dump /sdcard/ui.xml && adb -s {device_id} pull /sdcard/ui.xml ui_dump.xml 2>/dev/null && cat ui_dump.xml")
    
    # Look for Quick Snap or camera-related elements
    if "Quick Snap" in ui_dump:
        print("✅ Found Quick Snap option")
    elif "camera" in ui_dump.lower():
        print("✅ Found camera-related option")
    else:
        print("⚠️ Could not find Quick Snap automatically")
    
    # Get recent logs
    print("\n📊 Recent GemMunch logs:")
    logs = run_command(f"adb -s {device_id} logcat -d -t 100 | grep -i gemmunch | tail -10")
    print(logs)
    
    print("\n📱 Current state captured. Check test_results/home_screen.png")
    print("\n💡 To proceed with testing:")
    print("1. Manually tap on Quick Snap or camera option")
    print("2. Select an image from device storage (not Photos app)")
    print("3. Run: adb logcat | grep -E '(PhotoMealExtractor|glycemic)' to see results")

if __name__ == "__main__":
    # Create results directory
    Path("test_results").mkdir(exist_ok=True)
    
    # Set PATH for adb
    import os
    os.environ['PATH'] = os.environ['PATH'] + ":" + os.path.expanduser("~/Library/Android/sdk/platform-tools")
    
    main()