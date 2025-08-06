#!/usr/bin/env python3
"""
UI Automator Helper for GemMunch Testing

This module provides enhanced UI automation capabilities using Android UI Automator.
It can identify UI elements, extract text from the screen, and perform more precise
interactions with the GemMunch app.

Requirements:
- Android UI Automator
- Python uiautomator2 library (pip install uiautomator2)
"""

import time
import re
from typing import Dict, List, Optional, Any, Tuple
import json
import subprocess

try:
    import uiautomator2 as u2
    UI_AUTOMATOR_AVAILABLE = True
except ImportError:
    UI_AUTOMATOR_AVAILABLE = False
    print("⚠️ uiautomator2 not available. Install with: pip install uiautomator2")

class GemMunchUIAutomator:
    """Enhanced UI automation for GemMunch app testing"""
    
    def __init__(self, device_id: Optional[str] = None):
        self.device_id = device_id
        self.device = None
        
        if UI_AUTOMATOR_AVAILABLE:
            try:
                if device_id:
                    self.device = u2.connect(device_id)
                else:
                    self.device = u2.connect()
                print(f"✅ UI Automator connected to: {self.device.device_info['udid']}")
            except Exception as e:
                print(f"❌ Failed to connect UI Automator: {e}")
                self.device = None
    
    def is_available(self) -> bool:
        """Check if UI Automator is available and connected"""
        return self.device is not None
    
    def wait_for_element(self, selector: Dict[str, Any], timeout: int = 10) -> bool:
        """Wait for UI element to appear"""
        if not self.device:
            return False
        
        try:
            return self.device(**selector).wait(timeout=timeout)
        except Exception:
            return False
    
    def find_element_with_text(self, text: str, exact: bool = False) -> Optional[Any]:
        """Find UI element containing specific text"""
        if not self.device:
            return None
        
        try:
            if exact:
                return self.device(text=text)
            else:
                return self.device(textContains=text)
        except Exception:
            return None
    
    def extract_screen_text(self) -> List[str]:
        """Extract all text from current screen"""
        if not self.device:
            return []
        
        try:
            # Get UI hierarchy
            xml = self.device.dump_hierarchy()
            
            # Extract text from XML (simplified parsing)
            text_pattern = r'text="([^"]*)"'
            texts = re.findall(text_pattern, xml)
            
            # Filter out empty texts
            return [t for t in texts if t.strip()]
        except Exception as e:
            print(f"Failed to extract screen text: {e}")
            return []
    
    def wait_for_session_ready(self, timeout: int = 60) -> bool:
        """Wait for GemMunch session to be ready using UI elements"""
        if not self.device:
            return False
        
        print("🔍 Checking for session readiness using UI elements...")
        
        # Look for common UI elements that indicate the app is ready
        ready_indicators = [
            {"text": "Snap & Log"},
            {"text": "Analyze & Chat"},
            {"textContains": "Ready"},
            {"textContains": "Camera"},
            {"resourceId": "com.stel.gemmunch:id/camera_button"},
            {"description": "Camera"}
        ]
        
        start_time = time.time()
        while time.time() - start_time < timeout:
            for indicator in ready_indicators:
                if self.wait_for_element(indicator, timeout=2):
                    print(f"✅ Found ready indicator: {indicator}")
                    return True
            
            time.sleep(2)
            print(f"  ⏳ Still waiting... ({int(time.time() - start_time)}s)")
        
        print("⚠️ Session readiness timeout - assuming ready")
        return False
    
    def open_gallery_and_select_image(self, image_name: str) -> bool:
        """Open gallery and select specific image using UI automation"""
        if not self.device:
            return False
        
        print(f"📱 Opening gallery to select {image_name}")
        
        try:
            # Open gallery using intent
            self.device.app_start("com.google.android.apps.photos")  # Google Photos
            time.sleep(3)
            
            # Alternative gallery apps to try
            gallery_apps = [
                "com.google.android.apps.photos",
                "com.android.gallery3d", 
                "com.sec.android.gallery3d",  # Samsung Gallery
                "com.miui.gallery"  # MIUI Gallery
            ]
            
            # Try to find and tap on the image
            # Look for the image by name or recent images
            if self.device(textContains=image_name).exists(timeout=5):
                self.device(textContains=image_name).click()
                time.sleep(2)
            else:
                # Tap on the most recent image (usually first in gallery)
                if self.device(className="android.widget.ImageView").exists(timeout=5):
                    images = self.device(className="android.widget.ImageView")
                    if images.count > 0:
                        images[0].click()
                        time.sleep(2)
            
            # Look for share button
            share_buttons = [
                {"description": "Share"},
                {"text": "Share"},
                {"resourceId": "com.google.android.apps.photos:id/share_button"}
            ]
            
            for share_btn in share_buttons:
                if self.device(**share_btn).exists(timeout=3):
                    self.device(**share_btn).click()
                    time.sleep(2)
                    break
            
            # Select GemMunch from share menu
            if self.device(text="GemMunch").exists(timeout=5):
                self.device(text="GemMunch").click()
                time.sleep(2)
                return True
            elif self.device(textContains="GemMunch").exists(timeout=5):
                self.device(textContains="GemMunch").click()
                time.sleep(2)
                return True
            
            print("⚠️ Could not find GemMunch in share menu")
            return False
            
        except Exception as e:
            print(f"❌ Gallery automation failed: {e}")
            return False
    
    def wait_for_analysis_completion(self, timeout: int = 120) -> Dict[str, Any]:
        """Wait for analysis completion and extract results using UI elements"""
        if not self.device:
            return {"success": False, "error": "UI Automator not available"}
        
        print("🔍 Monitoring UI for analysis completion...")
        
        start_time = time.time()
        
        # UI elements that indicate analysis is in progress
        progress_indicators = [
            {"textContains": "Analyzing"},
            {"textContains": "Processing"},
            {"textContains": "Loading"},
            {"description": "Progress"},
            {"className": "android.widget.ProgressBar"}
        ]
        
        # UI elements that indicate analysis is complete
        completion_indicators = [
            {"textContains": "calories"},
            {"textContains": "Glycemic Index"},
            {"textContains": "carbs"},
            {"textContains": "protein"},
            {"textContains": "nutrition"},
            {"text": "Done"},
            {"text": "Complete"}
        ]
        
        analysis_started = False
        
        while time.time() - start_time < timeout:
            elapsed = time.time() - start_time
            
            # Check if analysis has started
            if not analysis_started:
                for indicator in progress_indicators:
                    if self.device(**indicator).exists(timeout=1):
                        analysis_started = True
                        print(f"  📊 Analysis started ({elapsed:.1f}s)")
                        break
            
            # Check if analysis is complete
            for indicator in completion_indicators:
                if self.device(**indicator).exists(timeout=1):
                    analysis_time = time.time() - start_time
                    print(f"  ✅ Analysis completed ({analysis_time:.1f}s)")
                    
                    # Extract nutrition results
                    nutrition_results = self.extract_nutrition_results()
                    
                    return {
                        "success": True,
                        "analysis_time": analysis_time,
                        "nutrition_results": nutrition_results
                    }
            
            # Update progress
            if int(elapsed) % 10 == 0:
                print(f"  ⏳ Still analyzing... ({elapsed:.1f}s)")
            
            time.sleep(1)
        
        return {
            "success": False,
            "analysis_time": timeout,
            "error": "Analysis timeout",
            "nutrition_results": {}
        }
    
    def extract_nutrition_results(self) -> Dict[str, Any]:
        """Extract nutrition information from the UI"""
        if not self.device:
            return {}
        
        print("📊 Extracting nutrition results from UI...")
        
        # Get all text from screen
        screen_texts = self.extract_screen_text()
        
        results = {
            "food_identified": None,
            "glycemic_index": None,
            "calories": None,
            "carbs": None,
            "protein": None,
            "fat": None,
            "confidence": None
        }
        
        # Parse nutrition information from text
        for text in screen_texts:
            text_lower = text.lower()
            
            # Extract glycemic index
            gi_match = re.search(r'gi[:\s]*(\d+)', text_lower)
            if gi_match:
                results["glycemic_index"] = int(gi_match.group(1))
            
            # Extract calories
            cal_match = re.search(r'(\d+)\s*cal', text_lower)
            if cal_match:
                results["calories"] = int(cal_match.group(1))
            
            # Extract carbs
            carb_match = re.search(r'(\d+(?:\.\d+)?)\s*g?\s*carb', text_lower)
            if carb_match:
                results["carbs"] = float(carb_match.group(1))
            
            # Extract protein
            protein_match = re.search(r'(\d+(?:\.\d+)?)\s*g?\s*protein', text_lower)
            if protein_match:
                results["protein"] = float(protein_match.group(1))
            
            # Extract fat
            fat_match = re.search(r'(\d+(?:\.\d+)?)\s*g?\s*fat', text_lower)
            if fat_match:
                results["fat"] = float(fat_match.group(1))
            
            # Extract confidence
            conf_match = re.search(r'(\d+)%\s*confidence', text_lower)
            if conf_match:
                results["confidence"] = int(conf_match.group(1))
            
            # Try to identify food name (look for common food terms)
            food_keywords = ['taco', 'burger', 'pizza', 'salad', 'sandwich', 'bowl', 'plate']
            for keyword in food_keywords:
                if keyword in text_lower and not results["food_identified"]:
                    results["food_identified"] = text
                    break
        
        # Log extracted results
        print("📊 Extracted nutrition data:")
        for key, value in results.items():
            if value is not None:
                print(f"  {key}: {value}")
        
        return results
    
    def take_screenshot(self, filename: str) -> bool:
        """Take screenshot for debugging"""
        if not self.device:
            return False
        
        try:
            self.device.screenshot(filename)
            return True
        except Exception as e:
            print(f"Failed to take screenshot: {e}")
            return False
    
    def get_current_app(self) -> str:
        """Get current app package name"""
        if not self.device:
            return ""
        
        try:
            return self.device.app_current()['package']
        except Exception:
            return ""
    
    def is_gemmunch_active(self) -> bool:
        """Check if GemMunch app is currently active"""
        return self.get_current_app() == "com.stel.gemmunch"
    
    def return_to_gemmunch(self) -> bool:
        """Return to GemMunch app if not active"""
        if self.is_gemmunch_active():
            return True
        
        try:
            self.device.app_start("com.stel.gemmunch")
            time.sleep(2)
            return self.is_gemmunch_active()
        except Exception as e:
            print(f"Failed to return to GemMunch: {e}")
            return False

# Example usage and testing
def test_ui_automator():
    """Test UI Automator functionality"""
    if not UI_AUTOMATOR_AVAILABLE:
        print("❌ UI Automator not available for testing")
        return
    
    print("🧪 Testing UI Automator functionality...")
    
    automator = GemMunchUIAutomator()
    
    if not automator.is_available():
        print("❌ UI Automator not connected")
        return
    
    # Test basic functionality
    print("📱 Current app:", automator.get_current_app())
    
    # Take screenshot
    if automator.take_screenshot("test_screenshot.png"):
        print("📸 Screenshot saved")
    
    # Extract screen text
    texts = automator.extract_screen_text()
    print(f"📝 Found {len(texts)} text elements on screen")
    
    print("✅ UI Automator test completed")

if __name__ == "__main__":
    test_ui_automator()