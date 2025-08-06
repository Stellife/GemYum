#!/usr/bin/env python3
"""
GemMunch Testing Environment Setup Script

This script sets up the testing environment for automated GemMunch testing:
- Checks and installs required dependencies
- Verifies Android development tools
- Tests device connectivity
- Sets up test directories
- Downloads missing test images (if needed)

Usage:
    python setup_testing_environment.py
"""

import subprocess
import sys
import os
import shutil
from pathlib import Path
from typing import List, Tuple, Optional
import platform

class TestingEnvironmentSetup:
    """Setup and verify testing environment"""
    
    def __init__(self):
        self.platform = platform.system().lower()
        self.script_dir = Path(__file__).parent
        self.test_images_dir = Path("../test_images")
        self.requirements_checked = False
    
    def check_python_version(self) -> bool:
        """Check Python version compatibility"""
        print("🐍 Checking Python version...")
        
        version = sys.version_info
        if version.major >= 3 and version.minor >= 7:
            print(f"✅ Python {version.major}.{version.minor}.{version.micro} is compatible")
            return True
        else:
            print(f"❌ Python {version.major}.{version.minor}.{version.micro} is too old. Need Python 3.7+")
            return False
    
    def install_python_dependencies(self) -> bool:
        """Install required Python packages"""
        print("📦 Installing Python dependencies...")
        
        dependencies = [
            "uiautomator2",
            "Pillow",  # For image processing
            "requests",  # For downloading test images
        ]
        
        for package in dependencies:
            try:
                print(f"  Installing {package}...")
                subprocess.check_call([
                    sys.executable, "-m", "pip", "install", package
                ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                print(f"  ✅ {package} installed")
            except subprocess.CalledProcessError as e:
                print(f"  ❌ Failed to install {package}: {e}")
                return False
        
        return True
    
    def check_adb_installation(self) -> bool:
        """Check if ADB is installed and accessible"""
        print("🔧 Checking ADB installation...")
        
        try:
            result = subprocess.run(["adb", "version"], 
                                  capture_output=True, text=True)
            if result.returncode == 0:
                version_line = result.stdout.split('\n')[0]
                print(f"✅ ADB found: {version_line}")
                return True
            else:
                print("❌ ADB not working properly")
                return False
        except FileNotFoundError:
            print("❌ ADB not found in PATH")
            print("   Please install Android SDK Platform Tools")
            print("   Or add ADB to your system PATH")
            return False
    
    def check_scrcpy_installation(self) -> bool:
        """Check if scrcpy is installed for screen recording"""
        print("📹 Checking scrcpy installation...")
        
        try:
            result = subprocess.run(["scrcpy", "--version"], 
                                  capture_output=True, text=True)
            if result.returncode == 0:
                version = result.stdout.strip().split('\n')[0]
                print(f"✅ scrcpy found: {version}")
                return True
            else:
                print("❌ scrcpy not working properly")
                return False
        except FileNotFoundError:
            print("⚠️ scrcpy not found (screen recording will be disabled)")
            print("   Install scrcpy from: https://github.com/Genymobile/scrcpy")
            return False
    
    def check_device_connectivity(self) -> Tuple[bool, List[str]]:
        """Check Android device connectivity"""
        print("📱 Checking device connectivity...")
        
        try:
            result = subprocess.run(["adb", "devices"], 
                                  capture_output=True, text=True)
            if result.returncode != 0:
                print("❌ Failed to list devices")
                return False, []
            
            devices = []
            for line in result.stdout.strip().split('\n')[1:]:
                if '\tdevice' in line:
                    device_id = line.split('\t')[0]
                    devices.append(device_id)
            
            if devices:
                print(f"✅ Found {len(devices)} connected device(s):")
                for device in devices:
                    print(f"   📱 {device}")
                return True, devices
            else:
                print("⚠️ No devices connected")
                print("   Please connect an Android device via USB or WiFi")
                return False, []
                
        except Exception as e:
            print(f"❌ Error checking devices: {e}")
            return False, []
    
    def setup_test_directories(self) -> bool:
        """Create necessary test directories"""
        print("📁 Setting up test directories...")
        
        directories = [
            Path("test_results"),
            Path("test_results/screenshots"),
            Path("test_results/recordings"),
            Path("test_results/reports")
        ]
        
        for directory in directories:
            try:
                directory.mkdir(exist_ok=True)
                print(f"  ✅ Created/verified: {directory}")
            except Exception as e:
                print(f"  ❌ Failed to create {directory}: {e}")
                return False
        
        return True
    
    def verify_test_images(self) -> bool:
        """Verify test images are available"""
        print("🖼️ Verifying test images...")
        
        if not self.test_images_dir.exists():
            print(f"❌ Test images directory not found: {self.test_images_dir}")
            return False
        
        # Count images by category
        categories = {}
        total_images = 0
        
        for category_dir in self.test_images_dir.iterdir():
            if category_dir.is_dir() and not category_dir.name.startswith('.'):
                image_count = len(list(category_dir.glob("*.jpg")) + 
                                list(category_dir.glob("*.png")))
                if image_count > 0:
                    categories[category_dir.name] = image_count
                    total_images += image_count
        
        if total_images == 0:
            print("❌ No test images found")
            return False
        
        print(f"✅ Found {total_images} test images in {len(categories)} categories:")
        for category, count in categories.items():
            print(f"   📁 {category}: {count} images")
        
        return True
    
    def test_uiautomator_connection(self, device_id: Optional[str] = None) -> bool:
        """Test UI Automator connection"""
        print("🤖 Testing UI Automator connection...")
        
        try:
            import uiautomator2 as u2
            
            if device_id:
                device = u2.connect(device_id)
            else:
                device = u2.connect()
            
            # Test basic functionality
            device_info = device.device_info
            print(f"✅ UI Automator connected to: {device_info.get('udid', 'Unknown')}")
            print(f"   Model: {device_info.get('brand', 'Unknown')} {device_info.get('model', 'Unknown')}")
            print(f"   Android: {device_info.get('version', 'Unknown')}")
            
            return True
            
        except ImportError:
            print("❌ uiautomator2 not installed")
            return False
        except Exception as e:
            print(f"❌ UI Automator connection failed: {e}")
            return False
    
    def check_gemmunch_installation(self, device_id: Optional[str] = None) -> bool:
        """Check if GemMunch app is installed on device"""
        print("📱 Checking GemMunch app installation...")
        
        try:
            cmd = ["adb"]
            if device_id:
                cmd.extend(["-s", device_id])
            cmd.extend(["shell", "pm", "list", "packages", "com.stel.gemmunch"])
            
            result = subprocess.run(cmd, capture_output=True, text=True)
            
            if "com.stel.gemmunch" in result.stdout:
                print("✅ GemMunch app is installed")
                return True
            else:
                print("❌ GemMunch app not found")
                print("   Please install the GemMunch APK on your device")
                return False
                
        except Exception as e:
            print(f"❌ Error checking app installation: {e}")
            return False
    
    def generate_setup_report(self, checks: dict) -> None:
        """Generate setup verification report"""
        report_file = Path("test_results/setup_report.txt")
        
        with open(report_file, 'w') as f:
            f.write("GemMunch Testing Environment Setup Report\n")
            f.write("=" * 50 + "\n\n")
            
            f.write(f"Platform: {platform.system()} {platform.release()}\n")
            f.write(f"Python: {sys.version}\n\n")
            
            f.write("Environment Checks:\n")
            f.write("-" * 20 + "\n")
            
            for check_name, (status, details) in checks.items():
                status_icon = "✅" if status else "❌"
                f.write(f"{status_icon} {check_name}: {'PASS' if status else 'FAIL'}\n")
                if details:
                    f.write(f"   {details}\n")
            
            f.write(f"\nReport generated: {Path(__file__).name}\n")
        
        print(f"📄 Setup report saved: {report_file}")
    
    def run_full_setup(self) -> bool:
        """Run complete environment setup and verification"""
        print("🚀 GemMunch Testing Environment Setup")
        print("=" * 50)
        
        checks = {}
        overall_success = True
        
        # Core requirements
        checks["Python Version"] = (self.check_python_version(), None)
        if not checks["Python Version"][0]:
            overall_success = False
        
        checks["ADB Installation"] = (self.check_adb_installation(), None)
        if not checks["ADB Installation"][0]:
            overall_success = False
        
        checks["Python Dependencies"] = (self.install_python_dependencies(), None)
        if not checks["Python Dependencies"][0]:
            overall_success = False
        
        # Optional but recommended
        checks["scrcpy Installation"] = (self.check_scrcpy_installation(), None)
        
        # Device connectivity
        device_connected, devices = self.check_device_connectivity()
        checks["Device Connectivity"] = (device_connected, f"{len(devices)} devices" if devices else None)
        
        # Test directories
        checks["Test Directories"] = (self.setup_test_directories(), None)
        
        # Test images
        checks["Test Images"] = (self.verify_test_images(), None)
        
        # Device-specific checks (if device available)
        if devices:
            device_id = devices[0] if len(devices) == 1 else None
            
            checks["UI Automator"] = (self.test_uiautomator_connection(device_id), None)
            checks["GemMunch App"] = (self.check_gemmunch_installation(device_id), None)
        
        # Generate report
        self.generate_setup_report(checks)
        
        # Summary
        print(f"\n{'='*50}")
        print("📊 SETUP SUMMARY")
        print(f"{'='*50}")
        
        passed = sum(1 for status, _ in checks.values() if status)
        total = len(checks)
        
        print(f"Checks Passed: {passed}/{total}")
        
        if overall_success and passed >= total - 2:  # Allow 2 optional checks to fail
            print("✅ Environment setup completed successfully!")
            print("🎯 Ready to run automated tests")
            return True
        else:
            print("❌ Environment setup incomplete")
            print("🔧 Please resolve the failing checks before running tests")
            return False

def main():
    """Main setup function"""
    setup = TestingEnvironmentSetup()
    success = setup.run_full_setup()
    
    if success:
        print(f"\n🚀 To run tests, use:")
        print(f"   python enhanced_automated_testing.py")
        print(f"   python automated_testing.py  # (basic version)")
    else:
        sys.exit(1)

if __name__ == "__main__":
    main()