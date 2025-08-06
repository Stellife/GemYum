#!/usr/bin/env python3
"""
add_chipotle_data.py

Adds Chipotle menu items to the GemMunch nutrition database.
This script handles unit conversions and ensures data consistency.
"""

import sqlite3
import json
from pathlib import Path
from datetime import datetime

# Chipotle nutrition data from official website
CHIPOTLE_DATA = [
    {
        "item_name": "Flour Tortilla (burrito)",
        "portion": "1 ea",
        "calories": 320,
        "calories_from_fat": 80,
        "total_fat_g": 9,
        "saturated_fat_g": 0.5,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 600,
        "carbohydrates_g": 50,
        "dietary_fiber_g": 3,
        "sugar_g": 0,
        "protein_g": 8,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Flour Tortilla (taco)",
        "portion": "1 ea",
        "calories": 80,
        "calories_from_fat": 25,
        "total_fat_g": 2.5,
        "saturated_fat_g": 0,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 160,
        "carbohydrates_g": 13,
        "dietary_fiber_g": 0.5,
        "sugar_g": 0,
        "protein_g": 2,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Crispy Corn Tortilla",
        "portion": "1 ea",
        "calories": 70,
        "calories_from_fat": 25,
        "total_fat_g": 3,
        "saturated_fat_g": 0,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 0,
        "carbohydrates_g": 10,
        "dietary_fiber_g": 1,
        "sugar_g": 0,
        "protein_g": 1,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Cilantro-Lime Brown Rice",
        "portion": "4 oz",
        "calories": 210,
        "calories_from_fat": 50,
        "total_fat_g": 6,
        "saturated_fat_g": 0,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 190,
        "carbohydrates_g": 36,
        "dietary_fiber_g": 2,
        "sugar_g": 0,
        "protein_g": 4,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Cilantro-Lime White Rice",
        "portion": "4 oz",
        "calories": 210,
        "calories_from_fat": 35,
        "total_fat_g": 4,
        "saturated_fat_g": 1,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 350,
        "carbohydrates_g": 40,
        "dietary_fiber_g": 1,
        "sugar_g": 0,
        "protein_g": 4,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Black Beans",
        "portion": "4 oz",
        "calories": 130,
        "calories_from_fat": 15,
        "total_fat_g": 1.5,
        "saturated_fat_g": 0,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 210,
        "carbohydrates_g": 22,
        "dietary_fiber_g": 7,
        "sugar_g": 2,
        "protein_g": 8,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Pinto Beans",
        "portion": "4 oz",
        "calories": 130,
        "calories_from_fat": 10,
        "total_fat_g": 1.5,
        "saturated_fat_g": 0,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 210,
        "carbohydrates_g": 21,
        "dietary_fiber_g": 8,
        "sugar_g": 1,
        "protein_g": 8,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Fajita Vegetables",
        "portion": "2 oz",
        "calories": 20,
        "calories_from_fat": 0,
        "total_fat_g": 0,
        "saturated_fat_g": 0,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 150,
        "carbohydrates_g": 5,
        "dietary_fiber_g": 1,
        "sugar_g": 2,
        "protein_g": 1,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Barbacoa",
        "portion": "4 oz",
        "calories": 170,
        "calories_from_fat": 60,
        "total_fat_g": 7,
        "saturated_fat_g": 2.5,
        "trans_fat_g": 0,
        "cholesterol_mg": 65,
        "sodium_mg": 530,
        "carbohydrates_g": 2,
        "dietary_fiber_g": 1,
        "sugar_g": 0,
        "protein_g": 24,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Chicken",
        "portion": "4 oz",
        "calories": 180,
        "calories_from_fat": 60,
        "total_fat_g": 7,
        "saturated_fat_g": 3,
        "trans_fat_g": 0,
        "cholesterol_mg": 125,
        "sodium_mg": 310,
        "carbohydrates_g": 0,
        "dietary_fiber_g": 0,
        "sugar_g": 0,
        "protein_g": 32,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Carnitas",
        "portion": "4 oz",
        "calories": 210,
        "calories_from_fat": 120,
        "total_fat_g": 12,
        "saturated_fat_g": 7,
        "trans_fat_g": 0,
        "cholesterol_mg": 65,
        "sodium_mg": 450,
        "carbohydrates_g": 0,
        "dietary_fiber_g": 0,
        "sugar_g": 0,
        "protein_g": 23,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Steak",
        "portion": "4 oz",
        "calories": 150,
        "calories_from_fat": 60,
        "total_fat_g": 6,
        "saturated_fat_g": 2.5,
        "trans_fat_g": 0,
        "cholesterol_mg": 80,
        "sodium_mg": 330,
        "carbohydrates_g": 1,
        "dietary_fiber_g": 1,
        "sugar_g": 0,
        "protein_g": 21,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Sofritas",
        "portion": "4 oz",
        "calories": 150,
        "calories_from_fat": 80,
        "total_fat_g": 10,
        "saturated_fat_g": 1.5,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 560,
        "carbohydrates_g": 9,
        "dietary_fiber_g": 3,
        "sugar_g": 5,
        "protein_g": 8,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Fresh Tomato Salsa",
        "portion": "4 oz",
        "calories": 25,
        "calories_from_fat": 0,
        "total_fat_g": 0,
        "saturated_fat_g": 0,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 550,
        "carbohydrates_g": 4,
        "dietary_fiber_g": 1,
        "sugar_g": 1,
        "protein_g": 0,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Roasted Chili-Corn Salsa",
        "portion": "4 oz",
        "calories": 80,
        "calories_from_fat": 15,
        "total_fat_g": 1.5,
        "saturated_fat_g": 0,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 330,
        "carbohydrates_g": 16,
        "dietary_fiber_g": 3,
        "sugar_g": 4,
        "protein_g": 3,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Tomatillo-Green Chili Salsa",
        "portion": "2 fl oz",
        "calories": 15,
        "calories_from_fat": 5,
        "total_fat_g": 0,
        "saturated_fat_g": 0,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 260,
        "carbohydrates_g": 4,
        "dietary_fiber_g": 0,
        "sugar_g": 2,
        "protein_g": 0,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Tomatillo-Red Chili Salsa",
        "portion": "2 fl oz",
        "calories": 30,
        "calories_from_fat": 5,
        "total_fat_g": 0,
        "saturated_fat_g": 0,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 500,
        "carbohydrates_g": 4,
        "dietary_fiber_g": 1,
        "sugar_g": 0,
        "protein_g": 0,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Cheese",
        "portion": "1 oz",
        "calories": 110,
        "calories_from_fat": 70,
        "total_fat_g": 8,
        "saturated_fat_g": 5,
        "trans_fat_g": 0,
        "cholesterol_mg": 30,
        "sodium_mg": 190,
        "carbohydrates_g": 1,
        "dietary_fiber_g": 0,
        "sugar_g": 0,
        "protein_g": 6,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Sour Cream",
        "portion": "2 oz",
        "calories": 110,
        "calories_from_fat": 90,
        "total_fat_g": 9,
        "saturated_fat_g": 7,
        "trans_fat_g": 0,
        "cholesterol_mg": 40,
        "sodium_mg": 30,
        "carbohydrates_g": 2,
        "dietary_fiber_g": 0,
        "sugar_g": 2,
        "protein_g": 2,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Guacamole (topping/side)",
        "portion": "4 oz",
        "calories": 230,
        "calories_from_fat": 190,
        "total_fat_g": 22,
        "saturated_fat_g": 3.5,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 370,
        "carbohydrates_g": 8,
        "dietary_fiber_g": 6,
        "sugar_g": 1,
        "protein_g": 2,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Guacamole (large)",
        "portion": "8 oz",
        "calories": 460,
        "calories_from_fat": 380,
        "total_fat_g": 44,
        "saturated_fat_g": 7,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 740,
        "carbohydrates_g": 16,
        "dietary_fiber_g": 12,
        "sugar_g": 2,
        "protein_g": 4,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Queso Blanco (entreé)",
        "portion": "2 oz",
        "calories": 120,
        "calories_from_fat": 80,
        "total_fat_g": 9,
        "saturated_fat_g": 6,
        "trans_fat_g": 0,
        "cholesterol_mg": 30,
        "sodium_mg": 250,
        "carbohydrates_g": 4,
        "dietary_fiber_g": 0,
        "sugar_g": 1,
        "protein_g": 5,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Queso Blanco (side)",
        "portion": "4 oz",
        "calories": 240,
        "calories_from_fat": 170,
        "total_fat_g": 18,
        "saturated_fat_g": 12,
        "trans_fat_g": 1,
        "cholesterol_mg": 60,
        "sodium_mg": 490,
        "carbohydrates_g": 7,
        "dietary_fiber_g": 0,
        "sugar_g": 2,
        "protein_g": 10,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Queso Blanco (large)",
        "portion": "8 oz",
        "calories": 480,
        "calories_from_fat": 330,
        "total_fat_g": 37,
        "saturated_fat_g": 23,
        "trans_fat_g": 1.5,
        "cholesterol_mg": 120,
        "sodium_mg": 980,
        "carbohydrates_g": 14,
        "dietary_fiber_g": 0.5,
        "sugar_g": 5,
        "protein_g": 20,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Supergreens Salad Mix",
        "portion": "3 oz",
        "calories": 15,
        "calories_from_fat": 0,
        "total_fat_g": 0,
        "saturated_fat_g": 0,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 15,
        "carbohydrates_g": 3,
        "dietary_fiber_g": 2,
        "sugar_g": 1,
        "protein_g": 1,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Romaine Lettuce (tacos)",
        "portion": "1 oz",
        "calories": 5,
        "calories_from_fat": 0,
        "total_fat_g": 0,
        "saturated_fat_g": 0,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 0,
        "carbohydrates_g": 1,
        "dietary_fiber_g": 1,
        "sugar_g": 0,
        "protein_g": 0,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Chips (regular)",
        "portion": "4 oz",
        "calories": 540,
        "calories_from_fat": 230,
        "total_fat_g": 25,
        "saturated_fat_g": 3.5,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 390,
        "carbohydrates_g": 73,
        "dietary_fiber_g": 7,
        "sugar_g": 1,
        "protein_g": 7,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Chips (large)",
        "portion": "6 oz",
        "calories": 810,
        "calories_from_fat": 350,
        "total_fat_g": 38,
        "saturated_fat_g": 5,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 590,
        "carbohydrates_g": 110,
        "dietary_fiber_g": 11,
        "sugar_g": 2,
        "protein_g": 11,
        "tags": ["Chipotle"]
    },
    {
        "item_name": "Chipotle-Honey Vinaigrette",
        "portion": "2 fl oz",
        "calories": 220,
        "calories_from_fat": 140,
        "total_fat_g": 16,
        "saturated_fat_g": 2.5,
        "trans_fat_g": 0,
        "cholesterol_mg": 0,
        "sodium_mg": 850,
        "carbohydrates_g": 18,
        "dietary_fiber_g": 1,
        "sugar_g": 12,
        "protein_g": 1,
        "tags": ["Chipotle"]
    }
]

def convert_portion_to_grams(portion_str):
    """Convert Chipotle portion sizes to grams for database storage."""
    portion_str = portion_str.lower().strip()
    
    # Conversion factors
    conversions = {
        "1 ea": 50,      # 1 each (generic item weight)
        "1 oz": 28.35,   # 1 ounce
        "2 oz": 56.7,    # 2 ounces  
        "3 oz": 85.05,   # 3 ounces
        "4 oz": 113.4,   # 4 ounces
        "6 oz": 170.1,   # 6 ounces
        "8 oz": 226.8,   # 8 ounces
        "2 fl oz": 60,   # 2 fluid ounces (assume ~60g for sauces)
    }
    
    return conversions.get(portion_str, 100.0)  # Default to 100g if unknown

def convert_to_per_100g(value, serving_size_grams):
    """Convert nutrition values from per serving to per 100g."""
    if serving_size_grams == 0:
        return 0
    return (value * 100.0) / serving_size_grams

def add_chipotle_to_database(db_path):
    """Add Chipotle nutrition data to the existing foods database."""
    
    print(f"🌮 Adding Chipotle menu items to nutrition database: {db_path}")
    
    # Connect to database
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    # Check if foods table exists and get schema
    cursor.execute("SELECT sql FROM sqlite_master WHERE type='table' AND name='foods'")
    table_info = cursor.fetchone()
    
    if not table_info:
        print("❌ Error: 'foods' table not found in database!")
        return False
    
    print(f"📊 Found foods table with schema")
    
    # Get current timestamp
    current_timestamp = datetime.now().isoformat()
    
    added_count = 0
    updated_count = 0
    
    for item in CHIPOTLE_DATA:
        try:
            # Convert portion to grams
            serving_grams = convert_portion_to_grams(item["portion"])
            
            # Create search terms
            search_terms = f"chipotle,{item['item_name'].lower()},{','.join(item['tags']).lower()}"
            
            # Check if item already exists
            cursor.execute("SELECT name FROM foods WHERE name = ? AND restaurant_name = 'Chipotle'", (item["item_name"],))
            exists = cursor.fetchone()
            
            if exists:
                # Update existing entry
                cursor.execute("""
                    UPDATE foods SET
                        serving_size = ?,
                        serving_size_grams = ?,
                        calories = ?,
                        calories_from_fat = ?,
                        total_fat_g = ?,
                        saturated_fat_g = ?,
                        trans_fat_g = ?,
                        cholesterol_mg = ?,
                        sodium_mg = ?,
                        total_carbohydrate_g = ?,
                        dietary_fiber_g = ?,
                        sugars_g = ?,
                        protein_g = ?,
                        search_terms = ?,
                        updated_at = ?
                    WHERE name = ? AND restaurant_name = 'Chipotle'
                """, (
                    item["portion"], serving_grams, item["calories"], item["calories_from_fat"],
                    item["total_fat_g"], item["saturated_fat_g"], item["trans_fat_g"],
                    item["cholesterol_mg"], item["sodium_mg"], item["carbohydrates_g"],
                    item["dietary_fiber_g"], item["sugar_g"], item["protein_g"],
                    search_terms, current_timestamp, item["item_name"]
                ))
                updated_count += 1
                print(f"  ✅ Updated: {item['item_name']} ({item['calories']} cal per {item['portion']})")
                
            else:
                # Insert new entry
                cursor.execute("""
                    INSERT INTO foods (
                        name, restaurant_name, serving_size, serving_size_grams,
                        calories, calories_from_fat, total_fat_g, saturated_fat_g,
                        trans_fat_g, cholesterol_mg, sodium_mg, total_carbohydrate_g,
                        dietary_fiber_g, sugars_g, protein_g, data_source, 
                        category, search_terms, created_at, updated_at
                    ) VALUES (?, 'Chipotle', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Restaurant', 'Mexican', ?, ?, ?)
                """, (
                    item["item_name"], item["portion"], serving_grams, item["calories"],
                    item["calories_from_fat"], item["total_fat_g"], item["saturated_fat_g"],
                    item["trans_fat_g"], item["cholesterol_mg"], item["sodium_mg"],
                    item["carbohydrates_g"], item["dietary_fiber_g"], item["sugar_g"],
                    item["protein_g"], search_terms, current_timestamp, current_timestamp
                ))
                added_count += 1
                print(f"  ✅ Added: {item['item_name']} ({item['calories']} cal per {item['portion']})")
                
        except Exception as e:
            print(f"  ❌ Error processing {item['item_name']}: {str(e)}")
            continue
    
    # Commit changes
    conn.commit()
    
    # Verify additions
    cursor.execute("SELECT COUNT(*) FROM foods WHERE restaurant_name = 'Chipotle'")
    chipotle_count = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(*) FROM foods")
    total_count = cursor.fetchone()[0]
    
    conn.close()
    
    print(f"\n🎉 Successfully processed Chipotle data!")
    print(f"   📊 Added: {added_count} new items")
    print(f"   🔄 Updated: {updated_count} existing items") 
    print(f"   🌮 Total Chipotle items in DB: {chipotle_count}")
    print(f"   📈 Total database entries: {total_count}")
    
    return True

def main():
    """Main function to add Chipotle data to nutrition database."""
    
    # Look for the nutrition database in common locations
    possible_db_paths = [
        Path(__file__).parent.parent / "app" / "src" / "main" / "assets" / "nutrients.db",
        Path(__file__).parent.parent / "app" / "build" / "intermediates" / "assets" / "debug" / "mergeDebugAssets" / "nutrients.db",
        Path(__file__).parent / "nutrients.db",
        Path(__file__).parent.parent / "nutrients.db",
        Path.cwd() / "nutrients.db"
    ]
    
    db_path = None
    for path in possible_db_paths:
        if path.exists():
            db_path = path
            break
    
    if not db_path:
        print("❌ Could not find nutrients.db database file!")
        print("   Please ensure the database exists in one of these locations:")
        for path in possible_db_paths:
            print(f"   - {path}")
        return False
    
    print(f"📁 Using database: {db_path}")
    
    # Add Chipotle data
    success = add_chipotle_to_database(db_path)
    
    if success:
        print("\n🚀 Chipotle menu items have been successfully added to your GemMunch nutrition database!")
        print("   Your AI food recognition should now be able to identify Chipotle items.")
    else:
        print("\n❌ Failed to add Chipotle data to the database.")
    
    return success

if __name__ == "__main__":
    main()