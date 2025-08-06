#!/usr/bin/env python3
"""
Download food images from public datasets for testing.
Uses Food-101 and other open datasets.
"""

import os
import requests
import logging
import zipfile
import tarfile
import shutil
from pathlib import Path
import random

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Datasets configuration
DATASETS = {
    "food101_sample": {
        "description": "Sample from Food-101 dataset",
        "url": "http://data.vision.ee.ethz.ch/cvl/food-101.tar.gz",
        "size_gb": 5.0,  # Full dataset is 5GB
        "categories": ["pizza", "hamburger", "sushi", "ice_cream", "fried_rice", 
                      "chicken_curry", "caesar_salad", "nachos", "tacos", "breakfast_burrito"]
    },
    "recipes5k": {
        "description": "Recipes with multiple ingredients dataset",
        "url": "http://www.ub.edu/cvub/recipes5k/recipes5k.tar",
        "size_gb": 1.7,
        "categories": ["salad", "soup", "meat", "fish", "dessert"]
    }
}

# Mapping to our test categories
CATEGORY_MAPPING = {
    "pizza": "pizza",
    "hamburger": "burgers",
    "tacos": "tacos",
    "caesar_salad": "salads",
    "sushi": "restaurant_meals",
    "breakfast_burrito": "breakfast",
    "nachos": "restaurant_meals",
    "fried_rice": "restaurant_meals"
}

def download_food101_sample():
    """Download sample images from Food-101 dataset."""
    logger.info("Downloading Food-101 sample images...")
    
    # For testing, we'll download individual images from the dataset
    # The full dataset is too large (5GB)
    
    base_url = "https://github.com/stratospark/food-101-keras/raw/master/images"
    test_images_dir = Path("../test_images")
    
    # Sample images to download
    sample_images = {
        "tacos": [
            "tacos_000.jpg",
            "tacos_001.jpg", 
            "tacos_002.jpg"
        ],
        "pizza": [
            "pizza_000.jpg",
            "pizza_001.jpg"
        ],
        "hamburger": [
            "hamburger_000.jpg",
            "hamburger_001.jpg"
        ],
        "caesar_salad": [
            "caesar_salad_000.jpg"
        ]
    }
    
    for food_type, image_files in sample_images.items():
        category = CATEGORY_MAPPING.get(food_type, "complex_meals")
        category_dir = test_images_dir / category / "food101"
        category_dir.mkdir(parents=True, exist_ok=True)
        
        for image_file in image_files:
            url = f"{base_url}/{food_type}/{image_file}"
            output_path = category_dir / f"food101_{image_file}"
            
            if output_path.exists():
                logger.info(f"Skipping {image_file} - already exists")
                continue
            
            try:
                logger.info(f"Downloading {image_file}...")
                response = requests.get(url, timeout=30)
                response.raise_for_status()
                
                with open(output_path, 'wb') as f:
                    f.write(response.content)
                    
                logger.info(f"Saved to {output_path}")
            except Exception as e:
                logger.error(f"Failed to download {image_file}: {e}")

def create_curated_test_set():
    """Create a curated test set with specific scenarios."""
    test_scenarios = {
        "glycemic_index_tests": {
            "high_gi": [
                {"name": "white_bread_sandwich.jpg", "expected_gi": 75},
                {"name": "baked_potato.jpg", "expected_gi": 85},
                {"name": "cornflakes.jpg", "expected_gi": 81}
            ],
            "medium_gi": [
                {"name": "banana.jpg", "expected_gi": 51},
                {"name": "orange_juice.jpg", "expected_gi": 50},
                {"name": "popcorn.jpg", "expected_gi": 65}
            ],
            "low_gi": [
                {"name": "apple.jpg", "expected_gi": 36},
                {"name": "greek_yogurt.jpg", "expected_gi": 11},
                {"name": "chickpeas.jpg", "expected_gi": 28}
            ]
        },
        "restaurant_specific": {
            "chipotle": [
                {"name": "chipotle_bowl_chicken.jpg", "items": ["chicken", "rice", "beans", "salsa"]},
                {"name": "chipotle_burrito.jpg", "items": ["burrito", "carnitas"]},
                {"name": "chipotle_tacos.jpg", "items": ["soft taco", "barbacoa"]}
            ],
            "mcdonalds": [
                {"name": "big_mac.jpg", "expected_gi": 66},
                {"name": "mcnuggets.jpg", "expected_gi": 46},
                {"name": "apple_pie.jpg", "expected_gi": 68}
            ],
            "vietnamese": [
                {"name": "pho_beef.jpg", "expected_gi": 40},
                {"name": "banh_mi.jpg", "expected_gi": 65},
                {"name": "spring_rolls.jpg", "expected_gi": 30}
            ]
        },
        "quantity_tests": {
            "single_items": [
                {"name": "one_taco.jpg", "quantity": 1},
                {"name": "one_apple.jpg", "quantity": 1}
            ],
            "multiple_items": [
                {"name": "three_tacos.jpg", "quantity": 3},
                {"name": "dozen_donuts.jpg", "quantity": 12}
            ]
        }
    }
    
    # Create test scenario directories
    test_dir = Path("../test_images/curated_tests")
    test_dir.mkdir(parents=True, exist_ok=True)
    
    # Save test scenarios metadata
    import json
    with open(test_dir / "test_scenarios.json", 'w') as f:
        json.dump(test_scenarios, f, indent=2)
    
    logger.info(f"Created test scenarios metadata at {test_dir / 'test_scenarios.json'}")

def download_from_pixabay():
    """Download free images from Pixabay API."""
    # Note: Requires Pixabay API key
    logger.info("For Pixabay images, you'll need to:")
    logger.info("1. Sign up at https://pixabay.com/api/")
    logger.info("2. Get your API key")
    logger.info("3. Use the API to search for food images")
    
    # Example API usage (requires API key)
    pixabay_api_template = """
import requests

API_KEY = 'your-pixabay-api-key'
BASE_URL = 'https://pixabay.com/api/'

def search_food_images(query, per_page=10):
    params = {
        'key': API_KEY,
        'q': query,
        'image_type': 'photo',
        'category': 'food',
        'per_page': per_page
    }
    
    response = requests.get(BASE_URL, params=params)
    return response.json()

# Example searches:
# tacos = search_food_images('tacos')
# burgers = search_food_images('hamburger')
# salads = search_food_images('salad healthy')
"""
    
    with open("../test_images/pixabay_example.py", 'w') as f:
        f.write(pixabay_api_template)

def create_test_instructions():
    """Create detailed testing instructions."""
    instructions = """# GemMunch Testing Instructions

## Quick Start Testing

1. **Basic Recognition Test**
   - Use images from `test_images/tacos/`
   - Verify: Identifies as "taco", shows GI=52

2. **Restaurant Test**
   - Use images from `test_images/restaurant_meals/`
   - Test with context: "Chipotle bowl"
   - Verify: Identifies components correctly

3. **Glycemic Index Tests**
   - High GI: `test_images/breakfast/pancakes.jpg` (should show GI ~67)
   - Medium GI: `test_images/burgers/` (should show GI=66)
   - Low GI: `test_images/salads/` (should show GI=15)

## Comprehensive Testing Checklist

### ✅ Single Food Items
- [ ] Taco (GI: 52)
- [ ] Burger (GI: 66)
- [ ] Pizza (GI: 60)
- [ ] Salad (GI: 15)
- [ ] Apple (GI: 36)

### ✅ Restaurant Meals
- [ ] Chipotle Bowl
- [ ] McDonald's Big Mac
- [ ] Taco Bell items
- [ ] Vietnamese Pho (GI: 40)

### ✅ Complex Scenarios
- [ ] Multiple items on one plate
- [ ] Partially eaten food
- [ ] Poor lighting conditions
- [ ] Blurry images

### ✅ Quantity Detection
- [ ] Single item
- [ ] Multiple items (e.g., 3 tacos)
- [ ] Large quantities

## Performance Metrics to Track

1. **Recognition Accuracy**
   - Food correctly identified: ___/%
   - Quantity accuracy: ___/%

2. **Glycemic Index Accuracy**
   - GI values match database: ___/%
   - GL calculations correct: ___/%

3. **Processing Speed**
   - Average inference time: ___s
   - NPU/GPU utilization: Yes/No

## Edge Cases to Test

1. **Ambiguous Foods**
   - Burrito vs wrapped sandwich
   - Different types of pasta
   - Similar looking items

2. **Challenging Images**
   - Extreme close-ups
   - Far away shots
   - Multiple overlapping items
   - Food in containers

3. **Context Testing**
   - With restaurant context
   - Without context
   - Misleading context

## Bug Report Template

**Image**: [filename]
**Expected**: [what should be detected]
**Actual**: [what was detected]
**GI Expected**: [number]
**GI Actual**: [number]
**Notes**: [any additional observations]

---

Remember to test both "Snap & Log" and "Analyze & Chat" modes!
"""
    
    with open("../test_images/TESTING_GUIDE.md", 'w') as f:
        f.write(instructions)
    
    logger.info("Created testing guide at test_images/TESTING_GUIDE.md")

def main():
    """Main function to set up test images."""
    logger.info("Setting up GemMunch test image suite...")
    
    # Create main test directory
    test_dir = Path("../test_images")
    test_dir.mkdir(exist_ok=True)
    
    # Download Food-101 samples
    try:
        download_food101_sample()
    except Exception as e:
        logger.error(f"Failed to download Food-101 samples: {e}")
    
    # Create curated test scenarios
    create_curated_test_set()
    
    # Create Pixabay example
    download_from_pixabay()
    
    # Create testing instructions
    create_test_instructions()
    
    logger.info("\nTest image setup complete!")
    logger.info("Next steps:")
    logger.info("1. Run download_test_images.py for Unsplash images")
    logger.info("2. Add your own test images to appropriate categories")
    logger.info("3. Follow TESTING_GUIDE.md for comprehensive testing")

if __name__ == "__main__":
    main()