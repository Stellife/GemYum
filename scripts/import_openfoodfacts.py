#!/usr/bin/env python3
"""
Import nutrition data from OpenFoodFacts
OpenFoodFacts is a free, open, collaborative database of food products from around the world
"""

import sqlite3
import requests
import logging
import time
from typing import Dict, List, Optional
import json

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class OpenFoodFactsImporter:
    def __init__(self, db_path: str = "nutrients.db"):
        self.db_path = db_path
        self.conn = None
        self.cursor = None
        self.api_base = "https://world.openfoodfacts.org/api/v2"
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'GemMunch/1.0 (Nutrition App)'
        })
        
    def __enter__(self):
        self.conn = sqlite3.connect(self.db_path)
        self.cursor = self.conn.cursor()
        return self
        
    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.conn:
            self.conn.close()
            
    def import_popular_products(self, limit: int = 500):
        """Import popular products from OpenFoodFacts"""
        logger.info(f"Importing up to {limit} popular products from OpenFoodFacts...")
        
        # Categories to focus on for demo
        categories = [
            "beverages",
            "breakfast-cereals",
            "chocolates",
            "cookies",
            "dairy",
            "frozen-foods",
            "fruits",
            "meats",
            "snacks",
            "sodas",
            "yogurts",
            "breads",
            "cheeses"
        ]
        
        total_imported = 0
        
        for category in categories:
            if total_imported >= limit:
                break
                
            logger.info(f"Fetching products from category: {category}")
            
            try:
                # Search for products in this category with nutrition data
                params = {
                    'categories_tags': category,
                    'fields': 'code,product_name,brands,serving_size,nutriments,nutrition_grades',
                    'page_size': 50,
                    'page': 1,
                    'sort_by': 'unique_scans_n',  # Sort by popularity
                    'json': 1
                }
                
                response = self.session.get(
                    f"{self.api_base}/search",
                    params=params,
                    timeout=10
                )
                response.raise_for_status()
                data = response.json()
                
                products = data.get('products', [])
                logger.info(f"Found {len(products)} products in {category}")
                
                for product in products:
                    if total_imported >= limit:
                        break
                        
                    if self._import_product(product, category):
                        total_imported += 1
                        
                    if total_imported % 50 == 0:
                        logger.info(f"Imported {total_imported} products so far...")
                        self.conn.commit()
                        
                time.sleep(0.5)  # Be nice to their API
                
            except Exception as e:
                logger.error(f"Error fetching category {category}: {e}")
                continue
                
        self.conn.commit()
        logger.info(f"Successfully imported {total_imported} products from OpenFoodFacts")
        
    def import_by_barcode(self, barcodes: List[str]):
        """Import specific products by barcode"""
        imported = 0
        
        for barcode in barcodes:
            try:
                response = self.session.get(
                    f"{self.api_base}/product/{barcode}.json",
                    timeout=10
                )
                
                if response.status_code == 200:
                    data = response.json()
                    if 'product' in data and self._import_product(data['product']):
                        imported += 1
                        
                time.sleep(0.2)  # Rate limiting
                
            except Exception as e:
                logger.error(f"Error importing barcode {barcode}: {e}")
                
        self.conn.commit()
        return imported
        
    def _import_product(self, product: Dict, category: str = None) -> bool:
        """Import a single product from OpenFoodFacts data"""
        try:
            # Extract basic info
            name = product.get('product_name', '').strip()
            if not name:
                return False
                
            brands = product.get('brands', '').strip()
            barcode = product.get('code', '')
            serving_size = product.get('serving_size', '')
            
            # Extract nutriments
            nutriments = product.get('nutriments', {})
            
            # Map OpenFoodFacts fields to our schema
            food_data = {
                'name': name,
                'brand_name': brands if brands else None,
                'barcode': barcode if barcode else None,
                'serving_size': serving_size if serving_size else None,
                'category': category,
                'data_source': 'OpenFoodFacts',
                
                # Energy
                'calories': nutriments.get('energy-kcal_100g'),
                
                # Macronutrients (convert from per 100g)
                'total_fat_g': nutriments.get('fat_100g'),
                'saturated_fat_g': nutriments.get('saturated-fat_100g'),
                'trans_fat_g': nutriments.get('trans-fat_100g'),
                'cholesterol_mg': nutriments.get('cholesterol_100g'),
                'sodium_mg': nutriments.get('sodium_100g'),
                'total_carbohydrate_g': nutriments.get('carbohydrates_100g'),
                'dietary_fiber_g': nutriments.get('fiber_100g'),
                'sugars_g': nutriments.get('sugars_100g'),
                'protein_g': nutriments.get('proteins_100g'),
                
                # Vitamins
                'vitamin_a_mcg_rae': nutriments.get('vitamin-a_100g'),
                'vitamin_c_mg': nutriments.get('vitamin-c_100g'),
                'vitamin_d_mcg': nutriments.get('vitamin-d_100g'),
                'vitamin_e_mg': nutriments.get('vitamin-e_100g'),
                'vitamin_k_mcg': nutriments.get('vitamin-k_100g'),
                'thiamin_mg': nutriments.get('vitamin-b1_100g'),
                'riboflavin_mg': nutriments.get('vitamin-b2_100g'),
                'niacin_mg': nutriments.get('vitamin-pp_100g'),
                'vitamin_b6_mg': nutriments.get('vitamin-b6_100g'),
                'folate_mcg': nutriments.get('vitamin-b9_100g'),
                'vitamin_b12_mcg': nutriments.get('vitamin-b12_100g'),
                
                # Minerals
                'calcium_mg': nutriments.get('calcium_100g'),
                'iron_mg': nutriments.get('iron_100g'),
                'magnesium_mg': nutriments.get('magnesium_100g'),
                'phosphorus_mg': nutriments.get('phosphorus_100g'),
                'potassium_mg': nutriments.get('potassium_100g'),
                'zinc_mg': nutriments.get('zinc_100g'),
                
                # Other
                'caffeine_mg': nutriments.get('caffeine_100g'),
                'alcohol_g': nutriments.get('alcohol_100g'),
            }
            
            # Remove None values
            food_data = {k: v for k, v in food_data.items() if v is not None}
            
            # Only import if we have at least basic nutritional data
            if 'calories' in food_data or 'protein_g' in food_data:
                self._insert_food(**food_data)
                return True
                
        except Exception as e:
            logger.error(f"Error importing product: {e}")
            
        return False
        
    def _insert_food(self, **kwargs):
        """Insert a food item into the database"""
        # Generate search terms
        search_terms = []
        if kwargs.get('name'):
            search_terms.extend(kwargs['name'].lower().split())
        if kwargs.get('brand_name'):
            search_terms.extend(kwargs['brand_name'].lower().split())
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
            
    def import_common_barcodes(self):
        """Import products with common barcodes found in US supermarkets"""
        common_barcodes = [
            # Popular beverages
            "012000001772",  # Pepsi
            "049000006346",  # Coca-Cola
            "012000809965",  # Mountain Dew
            "070847811169",  # Gatorade
            
            # Popular snacks
            "028400064057",  # Lay's Classic
            "028400040402",  # Doritos Nacho Cheese
            "030100106012",  # Oreo Cookies
            "038000845512",  # Pringles Original
            
            # Breakfast items
            "016000275270",  # Cheerios
            "038000201103",  # Frosted Flakes
            "030000066102",  # Quaker Oats
            
            # Popular chocolates/candy
            "040000017702",  # M&M's
            "034000002009",  # Snickers
            "034000232901",  # Kit Kat
            "034000440016",  # Reese's
            
            # Yogurt/Dairy
            "053600000277",  # Yoplait
            "036632039323",  # Chobani Greek Yogurt
            
            # Energy/Protein bars
            "038000357213",  # Nature Valley
            "750049000454",  # KIND bars
            "638102202116",  # CLIF bars
        ]
        
        logger.info(f"Importing {len(common_barcodes)} common US products by barcode...")
        imported = self.import_by_barcode(common_barcodes)
        logger.info(f"Successfully imported {imported} products by barcode")


def main():
    """Run the OpenFoodFacts importer"""
    with OpenFoodFactsImporter() as importer:
        # Import popular products
        importer.import_popular_products(limit=300)
        
        # Import specific common US barcodes
        importer.import_common_barcodes()
        
    logger.info("OpenFoodFacts import complete!")


if __name__ == "__main__":
    main()