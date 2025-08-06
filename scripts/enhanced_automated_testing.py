#!/usr/bin/env python3
"""
Enhanced GemMunch Automated Testing Script

This is an improved version of the automated testing script that incorporates
sophisticated UI automation capabilities for more reliable and accurate testing.

Features:
- Enhanced UI element detection using UI Automator
- More accurate nutrition data extraction
- Better error handling and recovery
- Detailed performance metrics
- Support for different testing scenarios

Requirements:
- All requirements from automated_testing.py
- uiautomator2 (pip install uiautomator2)
"""

import sys
import os
from pathlib import Path

# Add the scripts directory to Python path
sys.path.append(str(Path(__file__).parent))

from automated_testing import GemMunchTester
from ui_automator_helper import GemMunchUIAutomator, UI_AUTOMATOR_AVAILABLE
import time
import json
from datetime import datetime
from typing import Dict, Any, List, Optional

class EnhancedGemMunchTester(GemMunchTester):
    """Enhanced tester with UI Automator integration"""
    
    def __init__(self, device_id: Optional[str] = None, output_dir: str = "test_results", 
                 record_video: bool = True, timeout: int = 120, use_ui_automator: bool = True):
        super().__init__(device_id, output_dir, record_video, timeout)
        
        self.use_ui_automator = use_ui_automator and UI_AUTOMATOR_AVAILABLE
        self.ui_automator = None
        
        if self.use_ui_automator:
            self.ui_automator = GemMunchUIAutomator(device_id)
            if not self.ui_automator.is_available():
                print("⚠️ UI Automator not available, falling back to basic automation")
                self.use_ui_automator = False
        else:
            print("⚠️ UI Automator disabled or not available")
    
    def wait_for_session_ready(self) -> bool:
        """Enhanced session readiness check using UI Automator"""
        if self.use_ui_automator:
            return self.ui_automator.wait_for_session_ready(timeout=60)
        else:
            return super().wait_for_session_ready()
    
    def open_gallery_and_select_image(self, image_name: str) -> bool:
        """Enhanced gallery interaction using UI Automator"""
        if self.use_ui_automator:
            return self.ui_automator.open_gallery_and_select_image(image_name)
        else:
            return super().open_gallery_and_select_image(image_name)
    
    def wait_for_analysis_completion(self) -> Dict[str, Any]:
        """Enhanced analysis monitoring using UI Automator"""
        if self.use_ui_automator:
            return self.ui_automator.wait_for_analysis_completion(timeout=self.timeout)
        else:
            return super().wait_for_analysis_completion()
    
    def test_single_image(self, image_path: Path) -> Dict[str, Any]:
        """Enhanced single image testing with better error recovery"""
        test_name = f"{image_path.parent.name}_{image_path.stem}"
        print(f"\n🧪 Testing: {test_name}")
        
        # Start recording
        self.current_recording_process = self._start_recording(test_name)
        
        test_result = {
            "test_name": test_name,
            "image_path": str(image_path),
            "timestamp": datetime.now().isoformat(),
            "success": False,
            "ui_automator_used": self.use_ui_automator
        }
        
        try:
            # Ensure we're in GemMunch app
            if self.use_ui_automator and not self.ui_automator.is_gemmunch_active():
                print("  📱 Returning to GemMunch app")
                if not self.ui_automator.return_to_gemmunch():
                    test_result["error"] = "Failed to return to GemMunch app"
                    return test_result
            
            # Take screenshot before test
            if self.use_ui_automator:
                screenshot_path = self.output_dir / f"{test_name}_before.png"
                self.ui_automator.take_screenshot(str(screenshot_path))
            
            # Copy image to device
            device_path = self.copy_image_to_device(image_path)
            print(f"  📱 Image uploaded to: {device_path}")
            
            # Open gallery and select image
            if not self.open_gallery_and_select_image(image_path.name):
                test_result["error"] = "Failed to select image from gallery"
                return test_result
            
            # Take screenshot after image selection
            if self.use_ui_automator:
                screenshot_path = self.output_dir / f"{test_name}_selected.png"
                self.ui_automator.take_screenshot(str(screenshot_path))
            
            # Wait for analysis completion
            analysis_result = self.wait_for_analysis_completion()
            test_result.update(analysis_result)
            
            # Take screenshot after analysis
            if self.use_ui_automator and analysis_result.get("success", False):
                screenshot_path = self.output_dir / f"{test_name}_results.png"
                self.ui_automator.take_screenshot(str(screenshot_path))
            
            # Validate results against expected values
            self._validate_test_results(test_result, image_path)
            
            # Cleanup
            self.cleanup_device_image(device_path)
            
            if analysis_result["success"]:
                print(f"  ✅ Analysis completed in {analysis_result['analysis_time']:.1f}s")
                nutrition = analysis_result.get('nutrition_results', {})
                if nutrition:
                    print(f"  📊 Results: {self._format_nutrition_summary(nutrition)}")
            else:
                print(f"  ❌ Analysis failed: {analysis_result.get('error', 'Unknown error')}")
            
        except Exception as e:
            test_result["error"] = str(e)
            print(f"  ❌ Test failed: {e}")
            
            # Take error screenshot
            if self.use_ui_automator:
                screenshot_path = self.output_dir / f"{test_name}_error.png"
                self.ui_automator.take_screenshot(str(screenshot_path))
        
        finally:
            # Stop recording
            self._stop_recording()
            
            # Return to home screen and prepare for next test
            self._prepare_for_next_test()
        
        return test_result
    
    def _format_nutrition_summary(self, nutrition: Dict[str, Any]) -> str:
        """Format nutrition results for display"""
        parts = []
        
        if nutrition.get('food_identified'):
            parts.append(f"Food: {nutrition['food_identified']}")
        
        if nutrition.get('glycemic_index'):
            parts.append(f"GI: {nutrition['glycemic_index']}")
        
        if nutrition.get('calories'):
            parts.append(f"Cal: {nutrition['calories']}")
        
        if nutrition.get('carbs'):
            parts.append(f"Carbs: {nutrition['carbs']}g")
        
        return ", ".join(parts) if parts else "No nutrition data extracted"
    
    def _validate_test_results(self, test_result: Dict[str, Any], image_path: Path) -> None:
        """Validate test results against expected values"""
        # Load expected results from test scenarios
        expected_values = self._get_expected_values(image_path)
        
        if not expected_values:
            return
        
        validation_results = {}
        nutrition = test_result.get('nutrition_results', {})
        
        # Validate glycemic index
        if expected_values.get('expected_gi') and nutrition.get('glycemic_index'):
            expected_gi = expected_values['expected_gi']
            actual_gi = nutrition['glycemic_index']
            gi_diff = abs(expected_gi - actual_gi)
            
            validation_results['gi_validation'] = {
                'expected': expected_gi,
                'actual': actual_gi,
                'difference': gi_diff,
                'within_tolerance': gi_diff <= 10  # Allow 10 point tolerance
            }
        
        # Validate food identification
        if expected_values.get('expected_foods'):
            identified_food = nutrition.get('food_identified', '').lower()
            expected_foods = [f.lower() for f in expected_values['expected_foods']]
            
            food_match = any(food in identified_food for food in expected_foods)
            validation_results['food_validation'] = {
                'expected_foods': expected_values['expected_foods'],
                'identified': nutrition.get('food_identified'),
                'match_found': food_match
            }
        
        test_result['validation_results'] = validation_results
    
    def _get_expected_values(self, image_path: Path) -> Dict[str, Any]:
        """Get expected values for test image from test scenarios"""
        try:
            scenarios_file = Path("../test_images/curated_tests/test_scenarios.json")
            if not scenarios_file.exists():
                return {}
            
            with open(scenarios_file, 'r') as f:
                scenarios = json.load(f)
            
            image_name = image_path.name
            
            # Search through all scenario categories
            for category, items in scenarios.items():
                if isinstance(items, dict):
                    for subcategory, subitems in items.items():
                        if isinstance(subitems, list):
                            for item in subitems:
                                if item.get('name') == image_name:
                                    return {
                                        'expected_gi': item.get('expected_gi'),
                                        'expected_foods': item.get('items', [])
                                    }
                elif isinstance(items, list):
                    for item in items:
                        if item.get('name') == image_name:
                            return {
                                'expected_gi': item.get('expected_gi'),
                                'expected_foods': item.get('items', [])
                            }
            
            # If not found in scenarios, try to infer from path
            category = image_path.parent.name
            expected_foods = []
            
            if 'taco' in category:
                expected_foods = ['taco']
            elif 'burger' in category:
                expected_foods = ['burger', 'hamburger']
            elif 'pizza' in category:
                expected_foods = ['pizza']
            elif 'salad' in category:
                expected_foods = ['salad']
            
            return {'expected_foods': expected_foods} if expected_foods else {}
            
        except Exception as e:
            print(f"⚠️ Could not load expected values: {e}")
            return {}
    
    def _prepare_for_next_test(self) -> None:
        """Prepare device for next test"""
        # Return to home screen
        self._run_adb_command(["shell", "input", "keyevent", "KEYCODE_HOME"])
        time.sleep(2)
        
        # Clear recent apps (optional)
        self._run_adb_command(["shell", "input", "keyevent", "KEYCODE_APP_SWITCH"])
        time.sleep(1)
        self._run_adb_command(["shell", "input", "keyevent", "KEYCODE_BACK"])
        time.sleep(1)
        
        # Relaunch GemMunch app
        if self.use_ui_automator:
            self.ui_automator.return_to_gemmunch()
        else:
            self.launch_app()
        
        time.sleep(3)
    
    def generate_enhanced_report(self) -> None:
        """Generate enhanced report with validation results"""
        super().generate_report()
        
        # Generate validation summary
        self._generate_validation_report()
    
    def _generate_validation_report(self) -> None:
        """Generate validation-specific report"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        validation_file = self.output_dir / f"validation_report_{timestamp}.txt"
        
        with open(validation_file, 'w') as f:
            f.write("GemMunch Test Validation Report\n")
            f.write("=" * 50 + "\n\n")
            
            gi_validations = []
            food_validations = []
            
            for result in self.results:
                validation = result.get('validation_results', {})
                
                if 'gi_validation' in validation:
                    gi_validations.append(validation['gi_validation'])
                
                if 'food_validation' in validation:
                    food_validations.append(validation['food_validation'])
            
            # GI Validation Summary
            if gi_validations:
                f.write("Glycemic Index Validation:\n")
                f.write("-" * 30 + "\n")
                
                within_tolerance = sum(1 for v in gi_validations if v['within_tolerance'])
                total_gi_tests = len(gi_validations)
                
                f.write(f"Total GI Tests: {total_gi_tests}\n")
                f.write(f"Within Tolerance: {within_tolerance}\n")
                f.write(f"GI Accuracy: {within_tolerance/total_gi_tests*100:.1f}%\n\n")
                
                for i, validation in enumerate(gi_validations, 1):
                    status = "✅" if validation['within_tolerance'] else "❌"
                    f.write(f"{status} Test {i}: Expected {validation['expected']}, "
                           f"Got {validation['actual']} (diff: {validation['difference']})\n")
                f.write("\n")
            
            # Food Identification Summary
            if food_validations:
                f.write("Food Identification Validation:\n")
                f.write("-" * 35 + "\n")
                
                correct_identifications = sum(1 for v in food_validations if v['match_found'])
                total_food_tests = len(food_validations)
                
                f.write(f"Total Food Tests: {total_food_tests}\n")
                f.write(f"Correct Identifications: {correct_identifications}\n")
                f.write(f"Food ID Accuracy: {correct_identifications/total_food_tests*100:.1f}%\n\n")
                
                for i, validation in enumerate(food_validations, 1):
                    status = "✅" if validation['match_found'] else "❌"
                    f.write(f"{status} Test {i}: Expected {validation['expected_foods']}, "
                           f"Got '{validation['identified']}'\n")
        
        print(f"📊 Validation report saved: {validation_file}")

def main():
    """Enhanced main function with UI Automator support"""
    import argparse
    
    parser = argparse.ArgumentParser(description="Enhanced GemMunch Automated Testing Script")
    parser.add_argument("--device-id", help="Specific Android device ID")
    parser.add_argument("--output-dir", default="test_results", 
                       help="Directory for test results (default: test_results)")
    parser.add_argument("--no-recording", action="store_true", 
                       help="Disable screen recording")
    parser.add_argument("--no-ui-automator", action="store_true", 
                       help="Disable UI Automator (use basic automation)")
    parser.add_argument("--timeout", type=int, default=120, 
                       help="Analysis timeout in seconds (default: 120)")
    
    args = parser.parse_args()
    
    print("🚀 Enhanced GemMunch Automated Testing Script")
    print("=" * 55)
    
    if UI_AUTOMATOR_AVAILABLE and not args.no_ui_automator:
        print("✅ UI Automator available - using enhanced automation")
    else:
        print("⚠️ Using basic automation (UI Automator disabled or unavailable)")
    
    # Initialize enhanced tester
    tester = EnhancedGemMunchTester(
        device_id=args.device_id,
        output_dir=args.output_dir,
        record_video=not args.no_recording,
        timeout=args.timeout,
        use_ui_automator=not args.no_ui_automator
    )
    
    # Pre-flight checks
    if not tester.check_device_connection():
        sys.exit(1)
    
    if not tester.check_app_installed():
        sys.exit(1)
    
    # Run tests
    try:
        tester.run_all_tests()
        tester.generate_enhanced_report()
        print("\n🎉 Enhanced testing completed successfully!")
    except KeyboardInterrupt:
        print("\n⚠️ Testing interrupted by user")
    except Exception as e:
        print(f"\n❌ Testing failed: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()