#!/usr/bin/env python3
"""
Import expanded restaurant data into the nutrients database
"""

import sqlite3
import logging
from expanded_restaurant_data import EXPANDED_RESTAURANT_DATA

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def insert_food(cursor, restaurant_name, **kwargs):
    """Insert a food item into the database"""
    # Add restaurant name and data source
    kwargs['restaurant_name'] = restaurant_name
    kwargs['data_source'] = 'Restaurant Menu Data'
    
    # Generate search terms
    search_terms = []
    if kwargs.get('name'):
        search_terms.extend(kwargs['name'].lower().split())
    search_terms.append(restaurant_name.lower())
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
        cursor.execute(
            f"INSERT OR REPLACE INTO foods ({column_names}) VALUES ({placeholders})",
            values
        )
    except Exception as e:
        logger.error(f"Error inserting food {kwargs.get('name', 'Unknown')}: {e}")

def main():
    """Import all expanded restaurant data"""
    conn = sqlite3.connect('nutrients.db')
    cursor = conn.cursor()
    
    total_items = 0
    
    for restaurant, items in EXPANDED_RESTAURANT_DATA.items():
        logger.info(f"Loading {len(items)} items from {restaurant}")
        for item in items:
            insert_food(cursor, restaurant, **item)
            total_items += 1
            
    conn.commit()
    
    # Get updated stats
    cursor.execute("SELECT COUNT(*) FROM foods WHERE restaurant_name IS NOT NULL")
    total_restaurant_foods = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(DISTINCT restaurant_name) FROM foods WHERE restaurant_name IS NOT NULL")
    total_restaurants = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(*) FROM foods")
    total_foods = cursor.fetchone()[0]
    
    logger.info(f"Import complete!")
    logger.info(f"Added {total_items} new restaurant items")
    logger.info(f"Total restaurant foods: {total_restaurant_foods}")
    logger.info(f"Total restaurants: {total_restaurants}")
    logger.info(f"Total foods in database: {total_foods}")
    
    conn.close()

if __name__ == "__main__":
    main()