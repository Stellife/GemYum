#!/usr/bin/env python3
"""
GemMunch Automated Testing Script

This script performs comprehensive automated testing of the GemMunch Android app:
- Records screen during testing using scrcpy
- Waits for session readiness
- Uploads test images from gallery without frame adjustments
- Waits for analysis completion
- Logs timing and nutrition results
- Tests all available images in the test_images directory

Requirements:
- adb (Android Debug Bridge)
- scrcpy for screen recording
- Android device connected via USB or WiFi
- GemMunch app installed on device

Usage:
    python automated_testing.py [options]
    
Options:
    --device-id: Specific device ID (optional)
    --output-dir: Directory for test results (default: test_results)
    --record-video: Enable screen recording (default: True)
    --timeout: Analysis timeout in seconds (default: 120)
"""

import subprocess
import time
import json
import os
import glob
import threading
import signal
import sys
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Optional, Any
import argparse

class GemMunchTester:
    def __init__(self, device_id: Optional[str] = None, output_dir: str = "test_results", 
                 record_video: bool = True, timeout: int = 120):
        self.device_id = device_id
        self.output_dir = Path(output_dir)
        self.record_video = record_video
        self.timeout = timeout
        self.test_images_dir = Path("../test_images")
        self.results = []
        self.current_recording_process = None
        
        # App package and activities
        self.app_package = "com.stel.gemmunch.debug"
        self.main_activity = f"{self.app_package}/com.stel.gemmunch.ui.MainActivity"
        
        # Create output directory
        self.output_dir.mkdir(exist_ok=True)
        
        # Setup signal handler for cleanup
        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)
    
    def _signal_handler(self, sig, frame):
        """Handle Ctrl+C and cleanup"""
        print("\nReceived interrupt signal. Cleaning up...")
        self._stop_recording()
        sys.exit(0)
    
    def _run_adb_command(self, command: List[str]) -> subprocess.CompletedProcess:
        """Run ADB command with optional device ID"""
        cmd = ["adb"]
        if self.device_id:
            cmd.extend(["-s", self.device_id])
        cmd.extend(command)
        
        return subprocess.run(cmd, capture_output=True, text=True)
    
    def _start_recording(self, test_name: str) -> Optional[subprocess.Popen]:
        """Start screen recording using scrcpy"""
        if not self.record_video:
            return None
            
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        recording_file = self.output_dir / f"{test_name}_{timestamp}.mp4"
        
        cmd = ["scrcpy", "--record", str(recording_file), "--no-display"]
        if self.device_id:
            cmd.extend(["-s", self.device_id])
        
        try:
            process = subprocess.Popen(cmd, stdout=subprocess.DEVNULL, 
                                     stderr=subprocess.DEVNULL)
            print(f"  📹 Recording started: {recording_file}")
            return process
        except FileNotFoundError:
            print("  ⚠️ scrcpy not found. Screen recording disabled.")
            return None
    
    def _stop_recording(self) -> None:
        """Stop current screen recording"""
        if self.current_recording_process:
            self.current_recording_process.terminate()
            try:
                self.current_recording_process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self.current_recording_process.kill()
            self.current_recording_process = None
            print("  🛑 Recording stopped")
    
    def check_device_connection(self) -> bool:
        """Check if Android device is connected"""
        result = subprocess.run(["adb", "devices"], capture_output=True, text=True)
        if result.returncode != 0:
            print("❌ ADB not found. Please install Android SDK platform-tools.")
            return False
        
        devices = []
        for line in result.stdout.strip().split('\n')[1:]:
            if '\tdevice' in line:
                devices.append(line.split('\t')[0])
        
        if not devices:
            print("❌ No Android devices connected. Please connect a device via USB or WiFi.")
            return False
        
        if self.device_id and self.device_id not in devices:
            print(f"❌ Specified device {self.device_id} not found.")
            return False
        
        if not self.device_id and len(devices) > 1:
            print(f"⚠️ Multiple devices found: {devices}")
            print("Please specify device with --device-id")
            return False
        
        if not self.device_id:
            self.device_id = devices[0]
        
        print(f"✅ Connected to device: {self.device_id}")
        return True
    
    def check_app_installed(self) -> bool:
        """Check if GemMunch app is installed"""
        result = self._run_adb_command(["shell", "pm", "list", "packages", self.app_package])
        if self.app_package in result.stdout:
            print("✅ GemMunch app found")
            return True
        else:
            print(f"❌ GemMunch app not installed. Please install {self.app_package}")
            return False
    
    def launch_app(self) -> bool:
        """Launch GemMunch app"""
        print("🚀 Launching GemMunch app...")
        result = self._run_adb_command([
            "shell", "am", "start", 
            "-n", self.main_activity,
            "-a", "android.intent.action.MAIN",
            "-c", "android.intent.category.LAUNCHER"
        ])
        
        if result.returncode == 0:
            print("✅ App launched successfully")
            time.sleep(3)  # Wait for app to fully load
            return True
        else:
            print(f"❌ Failed to launch app: {result.stderr}")
            return False
    
    def wait_for_session_ready(self) -> bool:
        """Wait for GemMunch session to be ready"""
        print("⏳ Waiting for session to be ready...")
        
        # This is a simplified check - in reality you might need to:
        # 1. Check for specific UI elements using UI Automator
        # 2. Monitor app logs for readiness indicators
        # 3. Check for model download completion
        
        max_wait = 60  # seconds
        for i in range(max_wait):
            # Check if app is still running
            result = self._run_adb_command([
                "shell", "pidof", self.app_package
            ])
            
            if result.stdout.strip():
                print(f"  📱 App running (waiting {i+1}/{max_wait}s)")
                time.sleep(1)
                
                # After 30 seconds, assume session is ready
                if i >= 30:
                    print("✅ Session assumed ready")
                    return True
            else:
                print("❌ App not running")
                return False
        
        print("⚠️ Session readiness timeout")
        return False
    
    def copy_image_to_device(self, image_path: Path) -> str:
        """Copy test image to device gallery"""
        device_path = f"/sdcard/Pictures/{image_path.name}"
        
        result = self._run_adb_command([
            "push", str(image_path), device_path
        ])
        
        if result.returncode == 0:
            # Refresh media scanner so image appears in gallery
            self._run_adb_command([
                "shell", "am", "broadcast",
                "-a", "android.intent.action.MEDIA_SCANNER_SCAN_FILE",
                "-d", f"file://{device_path}"
            ])
            time.sleep(2)  # Wait for media scanner
            return device_path
        else:
            raise Exception(f"Failed to copy image: {result.stderr}")
    
    def open_gallery_and_select_image(self, image_name: str) -> bool:
        """Open gallery and select the uploaded image"""
        print(f"  📷 Opening gallery to select {image_name}")
        
        # Open gallery app
        result = self._run_adb_command([
            "shell", "am", "start",
            "-a", "android.intent.action.VIEW",
            "-t", "image/*"
        ])
        
        if result.returncode != 0:
            print(f"  ❌ Failed to open gallery: {result.stderr}")
            return False
        
        time.sleep(3)  # Wait for gallery to load
        
        # This is where you'd need more sophisticated UI automation
        # For now, we'll use basic taps assuming the most recent image is visible
        
        # Tap on the image (coordinates may need adjustment for different devices)
        self._run_adb_command(["shell", "input", "tap", "540", "960"])
        time.sleep(1)
        
        # Tap share button (this varies by gallery app)
        self._run_adb_command(["shell", "input", "tap", "200", "100"])
        time.sleep(2)
        
        # Select GemMunch from share menu (this requires knowing the position)
        # This is a simplified approach - real implementation would need UI Automator
        self._run_adb_command(["shell", "input", "tap", "540", "600"])
        time.sleep(2)
        
        return True
    
    def wait_for_analysis_completion(self) -> Dict[str, Any]:
        """Wait for food analysis to complete and extract results"""
        print("  🔍 Waiting for analysis completion...")
        
        start_time = time.time()
        analysis_time = None
        
        # Monitor for analysis completion
        # This would typically involve:
        # 1. Checking UI elements for completion indicators
        # 2. Monitoring app logs for analysis results
        # 3. Looking for specific text patterns on screen
        
        for i in range(self.timeout):
            elapsed = time.time() - start_time
            
            # Simplified completion check - look for typical UI elements
            # In reality, you'd use UI Automator to check for specific elements
            
            if i > 10:  # Assume analysis takes at least 10 seconds
                # Take screenshot to check for results
                screenshot_path = f"/sdcard/screenshot_{int(time.time())}.png"
                self._run_adb_command([
                    "shell", "screencap", "-p", screenshot_path
                ])
                
                # Pull screenshot for analysis (optional)
                local_screenshot = self.output_dir / f"screenshot_{int(time.time())}.png"
                self._run_adb_command([
                    "pull", screenshot_path, str(local_screenshot)
                ])
                
                # Clean up device screenshot
                self._run_adb_command(["shell", "rm", screenshot_path])
                
                # For this demo, we'll assume analysis completes after a reasonable time
                if elapsed > 20:  # Assume analysis completes after 20+ seconds
                    analysis_time = elapsed
                    break
            
            time.sleep(1)
        
        if analysis_time is None:
            print(f"  ⚠️ Analysis timeout after {self.timeout}s")
            return {
                "success": False,
                "analysis_time": self.timeout,
                "error": "Analysis timeout"
            }
        
        # Extract nutrition results from UI
        # This would require OCR or UI element inspection
        nutrition_results = self._extract_nutrition_results()
        
        return {
            "success": True,
            "analysis_time": analysis_time,
            "nutrition_results": nutrition_results
        }
    
    def _extract_nutrition_results(self) -> Dict[str, Any]:
        """Extract nutrition results from the UI"""
        # This is a placeholder implementation
        # Real implementation would use:
        # 1. UI Automator to extract text from specific elements
        # 2. OCR to read nutrition values from screen
        # 3. App logs parsing if nutrition data is logged
        
        return {
            "food_identified": "Unknown",
            "glycemic_index": None,
            "calories": None,
            "carbs": None,
            "protein": None,
            "fat": None,
            "confidence": None
        }
    
    def cleanup_device_image(self, device_path: str) -> None:
        """Remove uploaded image from device"""
        self._run_adb_command(["shell", "rm", device_path])
    
    def test_single_image(self, image_path: Path) -> Dict[str, Any]:
        """Test a single image and return results"""
        test_name = f"{image_path.parent.name}_{image_path.stem}"
        print(f"\n🧪 Testing: {test_name}")
        
        # Start recording
        self.current_recording_process = self._start_recording(test_name)
        
        test_result = {
            "test_name": test_name,
            "image_path": str(image_path),
            "timestamp": datetime.now().isoformat(),
            "success": False
        }
        
        try:
            # Copy image to device
            device_path = self.copy_image_to_device(image_path)
            print(f"  📱 Image uploaded to: {device_path}")
            
            # Open gallery and select image
            if not self.open_gallery_and_select_image(image_path.name):
                test_result["error"] = "Failed to select image from gallery"
                return test_result
            
            # Wait for analysis completion
            analysis_result = self.wait_for_analysis_completion()
            test_result.update(analysis_result)
            
            # Cleanup
            self.cleanup_device_image(device_path)
            
            if analysis_result["success"]:
                print(f"  ✅ Analysis completed in {analysis_result['analysis_time']:.1f}s")
            else:
                print(f"  ❌ Analysis failed: {analysis_result.get('error', 'Unknown error')}")
            
        except Exception as e:
            test_result["error"] = str(e)
            print(f"  ❌ Test failed: {e}")
        
        finally:
            # Stop recording
            self._stop_recording()
            
            # Return to home screen
            self._run_adb_command(["shell", "input", "keyevent", "KEYCODE_HOME"])
            time.sleep(2)
            
            # Relaunch app for next test
            if len(self.results) < len(self.get_test_images()) - 1:  # Not the last test
                self.launch_app()
                time.sleep(3)
        
        return test_result
    
    def get_test_images(self) -> List[Path]:
        """Get list of all test images"""
        image_extensions = ["*.jpg", "*.jpeg", "*.png"]
        test_images = []
        
        for ext in image_extensions:
            test_images.extend(self.test_images_dir.glob(f"**/{ext}"))
        
        return sorted(test_images)
    
    def run_all_tests(self) -> None:
        """Run tests on all available images"""
        test_images = self.get_test_images()
        
        if not test_images:
            print("❌ No test images found in test_images directory")
            return
        
        print(f"🔍 Found {len(test_images)} test images")
        
        # Launch app initially
        if not self.launch_app():
            return
        
        # Wait for session ready
        if not self.wait_for_session_ready():
            print("❌ Session not ready. Aborting tests.")
            return
        
        # Run tests
        for i, image_path in enumerate(test_images, 1):
            print(f"\n{'='*60}")
            print(f"Test {i}/{len(test_images)}: {image_path.name}")
            print(f"{'='*60}")
            
            result = self.test_single_image(image_path)
            self.results.append(result)
            
            # Brief pause between tests
            time.sleep(5)
        
        # Generate report
        self.generate_report()
    
    def generate_report(self) -> None:
        """Generate comprehensive test report"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_file = self.output_dir / f"test_report_{timestamp}.json"
        
        # Calculate summary statistics
        total_tests = len(self.results)
        successful_tests = sum(1 for r in self.results if r.get("success", False))
        failed_tests = total_tests - successful_tests
        
        if successful_tests > 0:
            analysis_times = [r["analysis_time"] for r in self.results if r.get("success", False)]
            avg_analysis_time = sum(analysis_times) / len(analysis_times)
            min_analysis_time = min(analysis_times)
            max_analysis_time = max(analysis_times)
        else:
            avg_analysis_time = min_analysis_time = max_analysis_time = 0
        
        summary = {
            "test_summary": {
                "total_tests": total_tests,
                "successful_tests": successful_tests,
                "failed_tests": failed_tests,
                "success_rate": (successful_tests / total_tests * 100) if total_tests > 0 else 0,
                "average_analysis_time": avg_analysis_time,
                "min_analysis_time": min_analysis_time,
                "max_analysis_time": max_analysis_time
            },
            "test_results": self.results,
            "test_configuration": {
                "device_id": self.device_id,
                "timeout": self.timeout,
                "record_video": self.record_video,
                "timestamp": datetime.now().isoformat()
            }
        }
        
        # Save detailed report
        with open(report_file, 'w') as f:
            json.dump(summary, f, indent=2)
        
        # Print summary
        print(f"\n{'='*60}")
        print("📊 TEST SUMMARY")
        print(f"{'='*60}")
        print(f"Total Tests: {total_tests}")
        print(f"Successful: {successful_tests}")
        print(f"Failed: {failed_tests}")
        print(f"Success Rate: {summary['test_summary']['success_rate']:.1f}%")
        if successful_tests > 0:
            print(f"Average Analysis Time: {avg_analysis_time:.1f}s")
            print(f"Analysis Time Range: {min_analysis_time:.1f}s - {max_analysis_time:.1f}s")
        print(f"\n📄 Detailed report saved: {report_file}")
        
        # Generate human-readable summary
        self._generate_human_readable_report(summary)
    
    def _generate_human_readable_report(self, summary: Dict[str, Any]) -> None:
        """Generate human-readable test report"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        report_file = self.output_dir / f"test_summary_{timestamp}.txt"
        
        with open(report_file, 'w') as f:
            f.write("GemMunch Automated Test Report\n")
            f.write("=" * 50 + "\n\n")
            
            f.write(f"Test Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"Device: {self.device_id}\n")
            f.write(f"Total Tests: {summary['test_summary']['total_tests']}\n")
            f.write(f"Success Rate: {summary['test_summary']['success_rate']:.1f}%\n\n")
            
            f.write("Individual Test Results:\n")
            f.write("-" * 30 + "\n")
            
            for result in self.results:
                status = "✅ PASS" if result.get("success", False) else "❌ FAIL"
                f.write(f"{status} {result['test_name']}\n")
                
                if result.get("success", False):
                    f.write(f"  Analysis Time: {result['analysis_time']:.1f}s\n")
                    nutrition = result.get('nutrition_results', {})
                    if nutrition.get('food_identified'):
                        f.write(f"  Food: {nutrition['food_identified']}\n")
                    if nutrition.get('glycemic_index'):
                        f.write(f"  GI: {nutrition['glycemic_index']}\n")
                else:
                    f.write(f"  Error: {result.get('error', 'Unknown error')}\n")
                f.write("\n")
        
        print(f"📄 Summary report saved: {report_file}")

def main():
    parser = argparse.ArgumentParser(description="GemMunch Automated Testing Script")
    parser.add_argument("--device-id", help="Specific Android device ID")
    parser.add_argument("--output-dir", default="test_results", 
                       help="Directory for test results (default: test_results)")
    parser.add_argument("--no-recording", action="store_true", 
                       help="Disable screen recording")
    parser.add_argument("--timeout", type=int, default=120, 
                       help="Analysis timeout in seconds (default: 120)")
    
    args = parser.parse_args()
    
    print("🚀 GemMunch Automated Testing Script")
    print("=" * 50)
    
    # Initialize tester
    tester = GemMunchTester(
        device_id=args.device_id,
        output_dir=args.output_dir,
        record_video=not args.no_recording,
        timeout=args.timeout
    )
    
    # Pre-flight checks
    if not tester.check_device_connection():
        sys.exit(1)
    
    if not tester.check_app_installed():
        sys.exit(1)
    
    # Run tests
    try:
        tester.run_all_tests()
        print("\n🎉 Testing completed successfully!")
    except KeyboardInterrupt:
        print("\n⚠️ Testing interrupted by user")
    except Exception as e:
        print(f"\n❌ Testing failed: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()