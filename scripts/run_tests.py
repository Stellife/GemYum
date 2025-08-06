#!/usr/bin/env python3
"""
GemMunch Test Runner

Simple script to run GemMunch automated tests with common configurations.

Usage:
    python run_tests.py [options]
"""

import sys
import subprocess
import argparse
from pathlib import Path

def run_setup_check():
    """Run environment setup check"""
    print("🔧 Running environment setup check...")
    result = subprocess.run([sys.executable, "setup_testing_environment.py"])
    return result.returncode == 0

def run_basic_tests(args):
    """Run basic automated tests"""
    cmd = [sys.executable, "automated_testing.py"]
    
    if args.device_id:
        cmd.extend(["--device-id", args.device_id])
    
    if args.output_dir:
        cmd.extend(["--output-dir", args.output_dir])
    
    if args.no_recording:
        cmd.append("--no-recording")
    
    if args.timeout:
        cmd.extend(["--timeout", str(args.timeout)])
    
    print("🚀 Running basic automated tests...")
    return subprocess.run(cmd)

def run_enhanced_tests(args):
    """Run enhanced automated tests with UI Automator"""
    cmd = [sys.executable, "enhanced_automated_testing.py"]
    
    if args.device_id:
        cmd.extend(["--device-id", args.device_id])
    
    if args.output_dir:
        cmd.extend(["--output-dir", args.output_dir])
    
    if args.no_recording:
        cmd.append("--no-recording")
    
    if args.no_ui_automator:
        cmd.append("--no-ui-automator")
    
    if args.timeout:
        cmd.extend(["--timeout", str(args.timeout)])
    
    print("🚀 Running enhanced automated tests...")
    return subprocess.run(cmd)

def main():
    parser = argparse.ArgumentParser(description="GemMunch Test Runner")
    
    # Test type selection
    test_group = parser.add_mutually_exclusive_group()
    test_group.add_argument("--basic", action="store_true", 
                           help="Run basic tests only")
    test_group.add_argument("--enhanced", action="store_true", 
                           help="Run enhanced tests with UI Automator")
    test_group.add_argument("--setup-only", action="store_true", 
                           help="Run setup check only")
    
    # Common options
    parser.add_argument("--device-id", help="Specific Android device ID")
    parser.add_argument("--output-dir", default="test_results", 
                       help="Directory for test results")
    parser.add_argument("--no-recording", action="store_true", 
                       help="Disable screen recording")
    parser.add_argument("--no-ui-automator", action="store_true", 
                       help="Disable UI Automator (enhanced tests only)")
    parser.add_argument("--timeout", type=int, default=120, 
                       help="Analysis timeout in seconds")
    parser.add_argument("--skip-setup", action="store_true", 
                       help="Skip environment setup check")
    
    args = parser.parse_args()
    
    print("🧪 GemMunch Test Runner")
    print("=" * 30)
    
    # Run setup check unless skipped
    if not args.skip_setup and not args.setup_only:
        if not run_setup_check():
            print("❌ Setup check failed. Use --skip-setup to override.")
            sys.exit(1)
    
    # Handle setup-only mode
    if args.setup_only:
        run_setup_check()
        return
    
    # Determine which tests to run
    if args.basic:
        result = run_basic_tests(args)
    elif args.enhanced:
        result = run_enhanced_tests(args)
    else:
        # Default: try enhanced tests, fallback to basic
        print("🎯 Auto-selecting test type...")
        try:
            import uiautomator2
            print("✅ UI Automator available - running enhanced tests")
            result = run_enhanced_tests(args)
        except ImportError:
            print("⚠️ UI Automator not available - running basic tests")
            result = run_basic_tests(args)
    
    # Exit with same code as test script
    sys.exit(result.returncode)

if __name__ == "__main__":
    main()