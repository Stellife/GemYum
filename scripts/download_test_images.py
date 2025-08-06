#!/usr/bin/env python3
"""
Download sample food images for testing GemMunch app.
Includes images for various food categories, especially those in our database.
"""

import os
import requests
import logging
from pathlib import Path
import time
from typing import List, Dict
import json

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Base directory for test images
TEST_IMAGES_DIR = Path("../test_images")

# Food categories with example image URLs
# Note: These are example URLs - you should replace with actual URLs from free sources
FOOD_CATEGORIES = {
    "tacos": {
        "description": "Various taco images to test GI=52",
        "images": [
            {
                "name": "hard_shell_tacos_3.jpg",
                "url": "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=800",
                "description": "3 hard shell tacos with beef"
            },
            {
                "name": "soft_tacos_chicken.jpg", 
                "url": "https://images.unsplash.com/photo-1552332386-9e1d21d1e1b3?w=800",
                "description": "Soft tacos with chicken"
            },
            {
                "name": "street_tacos.jpg",
                "url": "https://images.unsplash.com/photo-1551504734-5ee1c4a1479b?w=800",
                "description": "Street style tacos"
            }
        ]
    },
    "burgers": {
        "description": "Burger images to test GI=66",
        "images": [
            {
                "name": "classic_burger.jpg",
                "url": "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=800",
                "description": "Classic burger with fries"
            },
            {
                "name": "double_burger.jpg",
                "url": "https://images.unsplash.com/photo-1551615593-ef5fe247e8f7?w=800",
                "description": "Double cheeseburger"
            }
        ]
    },
    "pizza": {
        "description": "Pizza images to test GI=60",
        "images": [
            {
                "name": "pepperoni_pizza.jpg",
                "url": "https://images.unsplash.com/photo-1628840042765-356cda07504e?w=800",
                "description": "Pepperoni pizza"
            },
            {
                "name": "margherita_pizza.jpg",
                "url": "https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=800",
                "description": "Margherita pizza"
            }
        ]
    },
    "salads": {
        "description": "Salad images to test low GI=15",
        "images": [
            {
                "name": "caesar_salad.jpg",
                "url": "https://images.unsplash.com/photo-1550304943-4f24f54ddde9?w=800",
                "description": "Caesar salad"
            },
            {
                "name": "greek_salad.jpg",
                "url": "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?w=800",
                "description": "Greek salad"
            }
        ]
    },
    "restaurant_meals": {
        "description": "Restaurant-style meals from our database",
        "images": [
            {
                "name": "chipotle_bowl.jpg",
                "url": "https://images.unsplash.com/photo-1626700051175-6818013e1d4f?w=800",
                "description": "Chipotle-style bowl"
            },
            {
                "name": "pho_bowl.jpg",
                "url": "https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=800",
                "description": "Vietnamese pho (GI=40)"
            },
            {
                "name": "sushi_platter.jpg",
                "url": "https://images.unsplash.com/photo-1579584425555-c3ce17fd4351?w=800",
                "description": "Sushi platter"
            }
        ]
    },
    "breakfast": {
        "description": "Breakfast items",
        "images": [
            {
                "name": "pancakes.jpg",
                "url": "https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=800",
                "description": "Stack of pancakes"
            },
            {
                "name": "eggs_bacon.jpg",
                "url": "https://images.unsplash.com/photo-1525351484163-7529414344d8?w=800",
                "description": "Eggs and bacon"
            }
        ]
    },
    "complex_meals": {
        "description": "Complex meals with multiple components",
        "images": [
            {
                "name": "bento_box.jpg",
                "url": "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=800",
                "description": "Japanese bento box"
            },
            {
                "name": "thanksgiving_plate.jpg",
                "url": "https://images.unsplash.com/photo-1574672280600-4accfa5b6f98?w=800",
                "description": "Full dinner plate"
            }
        ]
    },
    "challenging_cases": {
        "description": "Edge cases and challenging images",
        "images": [
            {
                "name": "messy_plate.jpg",
                "url": "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800",
                "description": "Mixed food items"
            },
            {
                "name": "dark_lighting.jpg",
                "url": "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?w=800",
                "description": "Food in low light"
            },
            {
                "name": "partial_food.jpg",
                "url": "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=800",
                "description": "Partially eaten food"
            }
        ]
    }
}

def create_directories():
    """Create directory structure for test images."""
    for category in FOOD_CATEGORIES.keys():
        category_dir = TEST_IMAGES_DIR / category
        category_dir.mkdir(parents=True, exist_ok=True)
        logger.info(f"Created directory: {category_dir}")

def download_image(url: str, filepath: Path) -> bool:
    """Download a single image from URL."""
    try:
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        }
        response = requests.get(url, headers=headers, timeout=30)
        response.raise_for_status()
        
        with open(filepath, 'wb') as f:
            f.write(response.content)
        
        logger.info(f"Downloaded: {filepath.name}")
        return True
    except Exception as e:
        logger.error(f"Failed to download {url}: {e}")
        return False

def create_test_metadata(category: str, image_info: Dict) -> Dict:
    """Create metadata for test image."""
    return {
        "filename": image_info["name"],
        "category": category,
        "description": image_info["description"],
        "source_url": image_info["url"],
        "expected_results": get_expected_results(category, image_info),
        "download_timestamp": time.strftime("%Y-%m-%d %H:%M:%S")
    }

def get_expected_results(category: str, image_info: Dict) -> Dict:
    """Get expected results based on category and image."""
    # Define expected results for different categories
    expected_results = {
        "tacos": {
            "food_items": ["taco"],
            "glycemic_index": 52,
            "glycemic_load_range": [6.8, 31.2]  # Based on our database
        },
        "burgers": {
            "food_items": ["burger", "hamburger", "cheeseburger"],
            "glycemic_index": 66,
            "glycemic_load_range": [20, 35]
        },
        "pizza": {
            "food_items": ["pizza"],
            "glycemic_index": 60,
            "glycemic_load_range": [12, 25]
        },
        "salads": {
            "food_items": ["salad"],
            "glycemic_index": 15,
            "glycemic_load_range": [2, 8]
        }
    }
    
    return expected_results.get(category, {"food_items": [], "glycemic_index": None})

def download_test_images():
    """Download all test images."""
    create_directories()
    
    metadata = {
        "description": "Test images for GemMunch food recognition app",
        "created": time.strftime("%Y-%m-%d %H:%M:%S"),
        "total_images": sum(len(cat["images"]) for cat in FOOD_CATEGORIES.values()),
        "categories": {}
    }
    
    total_downloaded = 0
    
    for category, category_info in FOOD_CATEGORIES.items():
        logger.info(f"\nDownloading {category} images...")
        logger.info(f"Description: {category_info['description']}")
        
        category_metadata = {
            "description": category_info["description"],
            "images": []
        }
        
        for image_info in category_info["images"]:
            filepath = TEST_IMAGES_DIR / category / image_info["name"]
            
            if filepath.exists():
                logger.info(f"Skipping {image_info['name']} - already exists")
                category_metadata["images"].append(create_test_metadata(category, image_info))
                continue
            
            if download_image(image_info["url"], filepath):
                total_downloaded += 1
                category_metadata["images"].append(create_test_metadata(category, image_info))
                
                # Be nice to the servers
                time.sleep(1)
        
        metadata["categories"][category] = category_metadata
    
    # Save metadata
    metadata_file = TEST_IMAGES_DIR / "test_images_metadata.json"
    with open(metadata_file, 'w') as f:
        json.dump(metadata, f, indent=2)
    
    logger.info(f"\nDownload complete!")
    logger.info(f"Total images downloaded: {total_downloaded}")
    logger.info(f"Metadata saved to: {metadata_file}")
    
    # Create README
    create_readme()

def create_readme():
    """Create README file for test images."""
    readme_content = """# GemMunch Test Images

This directory contains test images for the GemMunch food recognition app.

## Categories

1. **tacos/** - Test glycemic index GI=52
   - Hard shell tacos
   - Soft tacos
   - Street tacos

2. **burgers/** - Test glycemic index GI=66
   - Classic burgers
   - Cheeseburgers

3. **pizza/** - Test glycemic index GI=60
   - Various pizza types

4. **salads/** - Test low glycemic index GI=15
   - Caesar salad
   - Greek salad

5. **restaurant_meals/** - Test restaurant identification
   - Chipotle bowls
   - Vietnamese pho (GI=40)
   - Sushi

6. **breakfast/** - Common breakfast items
   - Pancakes
   - Eggs and bacon

7. **complex_meals/** - Test multi-item detection
   - Bento boxes
   - Full dinner plates

8. **challenging_cases/** - Edge cases
   - Poor lighting
   - Messy presentation
   - Partially eaten food

## Testing Guidelines

1. **Single Item Tests**: Start with clear, single-item images
2. **Restaurant Tests**: Test restaurant-specific items (Chipotle, etc.)
3. **GI Verification**: Verify glycemic index values match database
4. **Complex Scenes**: Test multi-item detection
5. **Edge Cases**: Test challenging scenarios

## Expected Results

See `test_images_metadata.json` for expected results for each image.

## Image Sources

Images sourced from Unsplash (free stock photos).
Replace with your own test images for more accurate testing.
"""
    
    readme_file = TEST_IMAGES_DIR / "README.md"
    with open(readme_file, 'w') as f:
        f.write(readme_content)
    
    logger.info(f"Created README at: {readme_file}")

if __name__ == "__main__":
    logger.info("Starting test image download...")
    logger.info(f"Images will be saved to: {TEST_IMAGES_DIR.absolute()}")
    
    try:
        download_test_images()
    except KeyboardInterrupt:
        logger.info("\nDownload interrupted by user")
    except Exception as e:
        logger.error(f"Unexpected error: {e}")