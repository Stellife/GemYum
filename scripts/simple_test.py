#!/usr/bin/env python3
"""
Simple test script for GemMunch that directly tests image analysis
without complex UI navigation through Photos app.
"""

import subprocess
import time
import json
from pathlib import Path
from datetime import datetime

class SimpleGemMunchTester:
    def __init__(self):
        self.device_id = self.get_device_id()
        self.app_package = "com.stel.gemmunch.debug"
        self.results = []
        
    def get_device_id(self):
        """Get connected device ID."""
        result = subprocess.run(['adb', 'devices'], capture_output=True, text=True)
        lines = result.stdout.strip().split('\n')[1:]
        for line in lines:
            if '\tdevice' in line:
                return line.split('\t')[0]
        return None
    
    def launch_app(self):
        """Launch GemMunch app."""
        print("🚀 Launching GemMunch...")
        # Launch directly to camera capture screen if possible
        subprocess.run([
            'adb', '-s', self.device_id, 'shell', 'am', 'start', '-n',
            f'{self.app_package}/com.stel.gemmunch.ui.MainActivity'
        ])
        time.sleep(3)
    
    def take_screenshot(self, name):
        """Take a screenshot."""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"screenshot_{name}_{timestamp}.png"
        subprocess.run([
            'adb', '-s', self.device_id, 'exec-out', 'screencap', '-p'
        ], stdout=open(f"test_results/{filename}", 'wb'))
        return filename
    
    def get_current_activity(self):
        """Get current activity name."""
        result = subprocess.run([
            'adb', '-s', self.device_id, 'shell',
            'dumpsys', 'activity', 'activities', '|', 'grep', 'mResumedActivity'
        ], capture_output=True, text=True)
        return result.stdout.strip()
    
    def tap_screen(self, x, y):
        """Tap at specific coordinates."""
        subprocess.run([
            'adb', '-s', self.device_id, 'shell', 'input', 'tap', str(x), str(y)
        ])
    
    def run_simple_test(self):
        """Run a simple test by launching the app and taking screenshots."""
        print("\n🧪 Starting Simple GemMunch Test")
        print("=" * 50)
        
        # Launch app
        self.launch_app()
        
        # Take initial screenshot
        print("📸 Taking initial screenshot...")
        self.take_screenshot("initial")
        
        # Get current activity
        activity = self.get_current_activity()
        print(f"📱 Current activity: {activity}")
        
        # Wait for user to manually test
        print("\n⚡ App is now open. Please manually:")
        print("1. Navigate to the camera/image capture screen")
        print("2. Select 'Quick Snap' or similar option")
        print("3. Choose an image from the GemMunchTests folder")
        print("4. Wait for analysis to complete")
        print("\nPress Enter when analysis is complete...")
        input()
        
        # Take final screenshot
        print("📸 Taking final screenshot...")
        self.take_screenshot("analysis_complete")
        
        # Get recent logcat for analysis results
        print("📊 Extracting analysis results from logcat...")
        result = subprocess.run([
            'adb', '-s', self.device_id, 'logcat', '-d', '-t', '200',
            'PhotoMealExtractor:I', 'EnhancedNutrientDbHelper:I', '*:S'
        ], capture_output=True, text=True)
        
        # Save logcat
        with open('test_results/analysis_logcat.txt', 'w') as f:
            f.write(result.stdout)
        
        print("\n✅ Test complete!")
        print("📁 Results saved in test_results/")
        print("\nLogcat preview (last 20 lines):")
        print("-" * 50)
        lines = result.stdout.strip().split('\n')[-20:]
        for line in lines:
            print(line)

if __name__ == "__main__":
    # Create results directory
    Path("test_results").mkdir(exist_ok=True)
    
    tester = SimpleGemMunchTester()
    if tester.device_id:
        print(f"✅ Connected to device: {tester.device_id}")
        tester.run_simple_test()
    else:
        print("❌ No device connected. Please connect your Android device.")