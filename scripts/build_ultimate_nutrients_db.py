#!/usr/bin/env python3
"""
build_ultimate_nutrients_db.py
Creates the most comprehensive nutrition database possible with:
- USDA FoodData Central (Foundation + Branded + Restaurant foods)
- Fast food restaurant menu items with full nutritional data
- Glycemic index and glycemic load data
- Complete macronutrient and micronutrient profiles
- International cuisine data
"""

import sqlite3
import json
import requests
import time
import logging
import os
from typing import Dict, List, Optional, Tuple
from datetime import datetime
import gzip
import csv
from collections import defaultdict

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# USDA API Configuration
USDA_API_KEY = os.environ.get("USDA_API_KEY", "uJiMUGGjocfI5P4F5j3g4oT6DCqGl0f0i7kwT1R4")
USDA_API_BASE = "https://api.nal.usda.gov/fdc/v1"

# Database schema with comprehensive nutritional fields
SCHEMA = """
CREATE TABLE IF NOT EXISTS foods (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    brand_name TEXT,
    restaurant_name TEXT,
    serving_size TEXT,
    serving_size_grams REAL,
    
    -- Energy
    calories REAL,
    calories_from_fat REAL,
    
    -- Macronutrients
    total_fat_g REAL,
    saturated_fat_g REAL,
    trans_fat_g REAL,
    polyunsaturated_fat_g REAL,
    monounsaturated_fat_g REAL,
    cholesterol_mg REAL,
    sodium_mg REAL,
    total_carbohydrate_g REAL,
    dietary_fiber_g REAL,
    soluble_fiber_g REAL,
    insoluble_fiber_g REAL,
    sugars_g REAL,
    added_sugars_g REAL,
    sugar_alcohols_g REAL,
    protein_g REAL,
    
    -- Vitamins
    vitamin_a_iu REAL,
    vitamin_a_mcg_rae REAL,
    vitamin_c_mg REAL,
    vitamin_d_mcg REAL,
    vitamin_d_iu REAL,
    vitamin_e_mg REAL,
    vitamin_k_mcg REAL,
    thiamin_mg REAL,
    riboflavin_mg REAL,
    niacin_mg REAL,
    vitamin_b6_mg REAL,
    folate_mcg REAL,
    vitamin_b12_mcg REAL,
    biotin_mcg REAL,
    pantothenic_acid_mg REAL,
    choline_mg REAL,
    
    -- Minerals
    calcium_mg REAL,
    iron_mg REAL,
    magnesium_mg REAL,
    phosphorus_mg REAL,
    potassium_mg REAL,
    zinc_mg REAL,
    copper_mg REAL,
    manganese_mg REAL,
    selenium_mcg REAL,
    iodine_mcg REAL,
    chromium_mcg REAL,
    molybdenum_mcg REAL,
    
    -- Other nutrients
    caffeine_mg REAL,
    alcohol_g REAL,
    water_g REAL,
    omega3_fatty_acids_mg REAL,
    omega6_fatty_acids_mg REAL,
    
    -- Glycemic data
    glycemic_index INTEGER,
    glycemic_load REAL,
    
    -- Metadata
    data_source TEXT,
    fdc_id INTEGER,
    barcode TEXT,
    category TEXT,
    subcategory TEXT,
    is_raw BOOLEAN DEFAULT 0,
    is_cooked BOOLEAN DEFAULT 0,
    cooking_method TEXT,
    
    -- Search optimization
    search_terms TEXT,
    popularity_score INTEGER DEFAULT 0,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_name ON foods(name);
CREATE INDEX IF NOT EXISTS idx_brand ON foods(brand_name);
CREATE INDEX IF NOT EXISTS idx_restaurant ON foods(restaurant_name);
CREATE INDEX IF NOT EXISTS idx_category ON foods(category);
CREATE INDEX IF NOT EXISTS idx_fdc_id ON foods(fdc_id);
CREATE INDEX IF NOT EXISTS idx_barcode ON foods(barcode);

-- Note: FTS5 is not available on Android by default, so we skip the virtual table

-- Triggers removed since FTS5 is not available on Android

-- Restaurant menus table
CREATE TABLE IF NOT EXISTS restaurant_menus (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    restaurant_name TEXT NOT NULL,
    menu_item_name TEXT NOT NULL,
    menu_category TEXT,
    last_updated DATE,
    UNIQUE(restaurant_name, menu_item_name)
);

-- Glycemic index reference table
CREATE TABLE IF NOT EXISTS glycemic_index_ref (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    food_name TEXT NOT NULL,
    glycemic_index INTEGER,
    glycemic_load REAL,
    serving_size_g REAL,
    source TEXT
);
"""

# Nutrient mapping from USDA to our schema
NUTRIENT_MAPPING = {
    1003: 'protein_g',
    1004: 'total_fat_g',
    1005: 'total_carbohydrate_g',
    1008: 'calories',
    1009: 'sugars_g',
    1079: 'dietary_fiber_g',
    1087: 'calcium_mg',
    1089: 'iron_mg',
    1090: 'magnesium_mg',
    1091: 'phosphorus_mg',
    1092: 'potassium_mg',
    1093: 'sodium_mg',
    1095: 'zinc_mg',
    1098: 'copper_mg',
    1101: 'manganese_mg',
    1103: 'selenium_mcg',
    1104: 'vitamin_c_mg',
    1105: 'thiamin_mg',
    1106: 'riboflavin_mg',
    1107: 'niacin_mg',
    1108: 'pantothenic_acid_mg',
    1109: 'vitamin_b6_mg',
    1114: 'vitamin_d_mcg',
    1123: 'vitamin_e_mg',
    1165: 'vitamin_b12_mcg',
    1166: 'choline_mg',
    1175: 'vitamin_k_mcg',
    1177: 'folate_mcg',
    1180: 'vitamin_a_mcg_rae',
    1253: 'cholesterol_mg',
    1257: 'trans_fat_g',
    1258: 'saturated_fat_g',
    1259: 'monounsaturated_fat_g',
    1260: 'polyunsaturated_fat_g',
    1264: 'omega3_fatty_acids_mg',
    1265: 'omega6_fatty_acids_mg',
    1269: 'sugars_g',  # Total sugars
    1272: 'soluble_fiber_g',
    1273: 'insoluble_fiber_g',
    1275: 'added_sugars_g',
    1276: 'sugar_alcohols_g',
    1369: 'caffeine_mg',
}

class NutrientsDatabaseBuilder:
    def __init__(self, db_path: str = "nutrients.db"):
        self.db_path = db_path
        self.conn = None
        self.cursor = None
        self.session = requests.Session()
        
    def __enter__(self):
        self.conn = sqlite3.connect(self.db_path)
        self.cursor = self.conn.cursor()
        self.cursor.executescript(SCHEMA)
        self.conn.commit()
        return self
        
    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.conn:
            self.conn.close()
            
    def fetch_usda_foods(self, data_types: List[str] = ["Foundation", "SR Legacy", "Branded", "Restaurant"]):
        """Fetch comprehensive food data from USDA FoodData Central"""
        logger.info(f"Fetching USDA foods for data types: {data_types}")
        
        for data_type in data_types:
            page_number = 1
            total_pages = 1
            
            while page_number <= total_pages and page_number <= 100:  # Limit pages for API limits
                try:
                    params = {
                        "api_key": USDA_API_KEY,
                        "pageSize": 50,
                        "pageNumber": page_number,
                        "dataType": data_type
                    }
                    
                    response = self.session.get(f"{USDA_API_BASE}/foods/list", params=params)
                    response.raise_for_status()
                    data = response.json()
                    
                    # Handle different response formats
                    if isinstance(data, list):
                        # Direct list response
                        foods = data
                        total_pages = 1
                    else:
                        # Paginated response
                        if page_number == 1:
                            total_pages = data.get("totalPages", 1)
                            logger.info(f"Total pages for {data_type}: {total_pages}")
                        foods = data.get("foods", [])
                    for food in foods:
                        self._fetch_food_details(food["fdcId"])
                    
                    logger.info(f"Processed page {page_number}/{min(total_pages, 100)} for {data_type}")
                    page_number += 1
                    time.sleep(1)  # Rate limiting
                    
                except Exception as e:
                    logger.error(f"Error fetching {data_type} foods page {page_number}: {e}")
                    break
                    
    def _fetch_food_details(self, fdc_id: int):
        """Fetch detailed nutritional information for a specific food"""
        try:
            response = self.session.get(
                f"{USDA_API_BASE}/food/{fdc_id}",
                params={"api_key": USDA_API_KEY}
            )
            response.raise_for_status()
            food_data = response.json()
            
            # Extract basic information
            name = food_data.get("description", "Unknown")
            brand_name = food_data.get("brandName", food_data.get("brandOwner"))
            category = food_data.get("foodCategory", {}).get("description", "")
            
            # Extract nutrients
            nutrients = {}
            for nutrient in food_data.get("foodNutrients", []):
                nutrient_id = nutrient.get("nutrient", {}).get("id")
                if nutrient_id in NUTRIENT_MAPPING:
                    field_name = NUTRIENT_MAPPING[nutrient_id]
                    value = nutrient.get("amount")
                    if value is not None:
                        # Convert omega fatty acids from g to mg
                        if field_name in ['omega3_fatty_acids_mg', 'omega6_fatty_acids_mg']:
                            value = value * 1000
                        nutrients[field_name] = value
            
            # Insert into database
            self._insert_food(
                name=name,
                brand_name=brand_name,
                category=category,
                fdc_id=fdc_id,
                data_source="USDA FoodData Central",
                **nutrients
            )
            
        except Exception as e:
            logger.error(f"Error fetching details for FDC ID {fdc_id}: {e}")
            
    def load_fast_food_data(self):
        """Load comprehensive fast food restaurant data"""
        logger.info("Loading fast food restaurant data...")
        
        # Import the comprehensive fast food data
        try:
            from fast_food_nutrition_data import FAST_FOOD_NUTRITION_DATA
        except ImportError:
            logger.error("Could not import fast_food_nutrition_data.py")
            return
        
        # Insert all fast food data
        total_items = 0
        for restaurant, items in FAST_FOOD_NUTRITION_DATA.items():
            logger.info(f"Loading {len(items)} items from {restaurant}")
            for item in items:
                self._insert_food(
                    restaurant_name=restaurant,
                    data_source="Restaurant Menu Data",
                    **item
                )
                total_items += 1
                
        logger.info(f"Loaded {total_items} fast food items from {len(FAST_FOOD_NUTRITION_DATA)} restaurants")
        self.conn.commit()
                
    def load_glycemic_index_data(self):
        """Load glycemic index data for common foods"""
        logger.info("Loading glycemic index data...")
        
        # Import the comprehensive GI data
        try:
            from fast_food_nutrition_data import GLYCEMIC_INDEX_DATA
        except ImportError:
            logger.error("Could not import GLYCEMIC_INDEX_DATA from fast_food_nutrition_data.py")
            return
        
        # Insert GI reference data
        for item in GLYCEMIC_INDEX_DATA:
            # Calculate glycemic load (GL = GI × carb content / 100)
            # This is simplified - in production, use actual carb content
            estimated_carbs_per_100g = 20  # Simplified estimate
            carbs_in_serving = (item["serving_size_g"] / 100) * estimated_carbs_per_100g
            glycemic_load = (item["glycemic_index"] * carbs_in_serving) / 100
            
            self.cursor.execute("""
                INSERT INTO glycemic_index_ref (food_name, glycemic_index, glycemic_load, serving_size_g, source)
                VALUES (?, ?, ?, ?, ?)
            """, (item["name"], item["glycemic_index"], round(glycemic_load, 1), 
                  item["serving_size_g"], "International GI Database"))
            
        # Update existing foods with GI data where matches exist
        self.cursor.execute("""
            UPDATE foods 
            SET glycemic_index = gi.glycemic_index,
                glycemic_load = gi.glycemic_load
            FROM glycemic_index_ref gi
            WHERE LOWER(foods.name) LIKE '%' || LOWER(gi.food_name) || '%'
        """)
        
    def _insert_food(self, **kwargs):
        """Insert a food item into the database"""
        # Generate search terms
        search_terms = []
        if kwargs.get('name'):
            search_terms.extend(kwargs['name'].lower().split())
        if kwargs.get('brand_name'):
            search_terms.extend(kwargs['brand_name'].lower().split())
        if kwargs.get('restaurant_name'):
            search_terms.extend(kwargs['restaurant_name'].lower().split())
        kwargs['search_terms'] = ' '.join(set(search_terms))
        
        # Prepare the insert statement
        columns = []
        values = []
        for col, val in kwargs.items():
            if val is not None:
                columns.append(col)
                values.append(val)
                
        if not columns:
            return
            
        placeholders = ','.join(['?' for _ in columns])
        column_names = ','.join(columns)
        
        try:
            self.cursor.execute(
                f"INSERT OR REPLACE INTO foods ({column_names}) VALUES ({placeholders})",
                values
            )
        except Exception as e:
            logger.error(f"Error inserting food: {e}")
            
    def optimize_database(self):
        """Optimize the database for mobile use"""
        logger.info("Optimizing database...")
        
        # Remove duplicates
        self.cursor.execute("""
            DELETE FROM foods 
            WHERE id NOT IN (
                SELECT MIN(id) 
                FROM foods 
                GROUP BY name, brand_name, restaurant_name
            )
        """)
        
        # Update popularity scores for common items
        common_foods = [
            'pizza', 'burger', 'fries', 'chicken', 'salad', 'sandwich', 
            'pasta', 'rice', 'bread', 'milk', 'cheese', 'eggs', 'apple',
            'banana', 'coffee', 'water', 'soda', 'juice'
        ]
        
        for food in common_foods:
            self.cursor.execute("""
                UPDATE foods SET popularity_score = popularity_score + 10
                WHERE LOWER(name) LIKE ?
            """, (f'%{food}%',))
            
        # Commit any pending transactions before VACUUM
        self.conn.commit()
        
        # Vacuum to reclaim space (must be done outside of a transaction)
        self.conn.isolation_level = None
        self.conn.execute("VACUUM")
        self.conn.isolation_level = ""
        
        # Analyze for query optimization
        self.conn.execute("ANALYZE")
        
    def get_stats(self):
        """Get database statistics"""
        stats = {}
        
        # Total foods
        self.cursor.execute("SELECT COUNT(*) FROM foods")
        stats['total_foods'] = self.cursor.fetchone()[0]
        
        # Foods by source
        self.cursor.execute("""
            SELECT data_source, COUNT(*) 
            FROM foods 
            GROUP BY data_source
        """)
        stats['by_source'] = dict(self.cursor.fetchall())
        
        # Restaurant foods
        self.cursor.execute("""
            SELECT COUNT(DISTINCT restaurant_name) 
            FROM foods 
            WHERE restaurant_name IS NOT NULL
        """)
        stats['total_restaurants'] = self.cursor.fetchone()[0]
        
        # Foods with GI data
        self.cursor.execute("""
            SELECT COUNT(*) 
            FROM foods 
            WHERE glycemic_index IS NOT NULL
        """)
        stats['foods_with_gi'] = self.cursor.fetchone()[0]
        
        # Database size
        stats['size_mb'] = os.path.getsize(self.db_path) / (1024 * 1024)
        
        return stats


    def load_common_foods_data(self):
        """Load common foods nutritional data"""
        logger.info("Loading common foods data...")
        
        total_items = 0
        
        # Import the original common foods data
        try:
            from common_foods_data import COMMON_FOODS_DATA
            for item in COMMON_FOODS_DATA:
                self._insert_food(**item)
                total_items += 1
            logger.info(f"Loaded {len(COMMON_FOODS_DATA)} items from common_foods_data.py")
        except ImportError:
            logger.error("Could not import common_foods_data.py")
            
        # Import the expanded common foods data
        try:
            from expanded_common_foods_data import EXPANDED_COMMON_FOODS
            for item in EXPANDED_COMMON_FOODS:
                self._insert_food(**item)
                total_items += 1
            logger.info(f"Loaded {len(EXPANDED_COMMON_FOODS)} items from expanded_common_foods_data.py")
        except ImportError:
            logger.error("Could not import expanded_common_foods_data.py")
            
        logger.info(f"Total common food items loaded: {total_items}")
        self.conn.commit()


def main():
    """Build the comprehensive nutrients database"""
    logger.info("Starting ultimate nutrients database build...")
    
    with NutrientsDatabaseBuilder() as builder:
        # 1. Load USDA data (most comprehensive)
        if USDA_API_KEY:
            logger.info("Using USDA API key to fetch comprehensive food data...")
            builder.fetch_usda_foods()
        else:
            logger.warning("No USDA API key provided - skipping USDA data import")
            
        # 2. Load common foods data
        builder.load_common_foods_data()
        
        # 3. Load fast food restaurant data
        builder.load_fast_food_data()
        
        # 4. Load glycemic index data
        builder.load_glycemic_index_data()
        
        # 5. Optimize for mobile use
        builder.optimize_database()
        
        # 6. Get final statistics
        stats = builder.get_stats()
        
        logger.info("Database build complete!")
        logger.info(f"Total foods: {stats['total_foods']:,}")
        logger.info(f"Total restaurants: {stats['total_restaurants']}")
        logger.info(f"Foods with GI data: {stats['foods_with_gi']:,}")
        logger.info(f"Database size: {stats['size_mb']:.1f} MB")
        logger.info(f"By source: {stats['by_source']}")
        
    # Create compressed version
    logger.info("Creating compressed version...")
    with open('nutrients.db', 'rb') as f_in:
        with gzip.open('nutrients.db.gz', 'wb') as f_out:
            f_out.writelines(f_in)
    
    compressed_size = os.path.getsize('nutrients.db.gz') / (1024 * 1024)
    logger.info(f"Compressed size: {compressed_size:.1f} MB")
    

if __name__ == "__main__":
    main()