#!/usr/bin/env python3
"""
Create a visual collection of all test images with expected GI values.
"""

import os
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json

def create_test_collection():
    test_images_dir = Path("../test_images")
    output_dir = Path("test_results/collections")
    output_dir.mkdir(exist_ok=True)
    
    # Load metadata
    with open(test_images_dir / "test_images_metadata.json", 'r') as f:
        metadata = json.load(f)
    
    # Expected GI values for each category
    expected_gi = {
        "tacos": 52,
        "burgers": 66,
        "pizza": 60,
        "salads": 15,
        "restaurant_meals": "varies",
        "breakfast": "varies",
        "complex_meals": "varies",
        "challenging_cases": "varies"
    }
    
    # Create individual category sheets
    for category, cat_info in metadata['categories'].items():
        if not cat_info['images']:
            continue
            
        print(f"\nProcessing {category}...")
        
        # Create a collection image for this category
        images = []
        max_width = 0
        total_height = 0
        
        for img_info in cat_info['images']:
            img_path = test_images_dir / category / img_info['filename']
            if img_path.exists():
                try:
                    img = Image.open(img_path)
                    # Resize to standard width
                    target_width = 400
                    ratio = target_width / img.width
                    target_height = int(img.height * ratio)
                    img = img.resize((target_width, target_height), Image.Resampling.LANCZOS)
                    
                    # Add label
                    label_height = 60
                    labeled_img = Image.new('RGB', (target_width, target_height + label_height), 'white')
                    labeled_img.paste(img, (0, 0))
                    
                    # Draw text
                    draw = ImageDraw.Draw(labeled_img)
                    try:
                        font = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 16)
                    except:
                        font = ImageFont.load_default()
                    
                    text = f"{img_info['filename']}"
                    gi_text = f"Expected GI: {expected_gi[category]}"
                    
                    draw.text((10, target_height + 5), text, fill='black', font=font)
                    draw.text((10, target_height + 30), gi_text, fill='blue', font=font)
                    
                    images.append(labeled_img)
                    max_width = max(max_width, target_width)
                    total_height += target_height + label_height
                    
                except Exception as e:
                    print(f"Error processing {img_path}: {e}")
        
        if images:
            # Create category collection
            collection = Image.new('RGB', (max_width, total_height + 50), 'white')
            
            # Add category header
            draw = ImageDraw.Draw(collection)
            try:
                header_font = ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", 24)
            except:
                header_font = ImageFont.load_default()
            
            header_text = f"{category.upper()} - {cat_info['description']}"
            draw.text((10, 10), header_text, fill='black', font=header_font)
            
            # Paste images
            y_offset = 50
            for img in images:
                collection.paste(img, (0, y_offset))
                y_offset += img.height
            
            # Save
            output_path = output_dir / f"{category}_collection.png"
            collection.save(output_path)
            print(f"Created: {output_path}")
    
    # Create master index
    print("\n=== TEST IMAGE COLLECTIONS CREATED ===")
    print(f"Location: {output_dir}")
    print("\nCategories:")
    for category in metadata['categories']:
        gi = expected_gi.get(category, "unknown")
        print(f"- {category}: Expected GI = {gi}")

if __name__ == "__main__":
    try:
        create_test_collection()
    except ImportError:
        print("Pillow not installed. Installing...")
        import subprocess
        subprocess.run(["pip3", "install", "Pillow"])
        print("Please run the script again.")