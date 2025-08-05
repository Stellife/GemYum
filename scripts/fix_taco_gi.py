#!/usr/bin/env python3
"""Fix glycemic index for taco entries in the database."""

import sqlite3
import logging

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def fix_taco_gi():
    """Update taco entries with proper glycemic index values."""
    conn = sqlite3.connect('nutrients.db')
    cursor = conn.cursor()
    
    try:
        # Update the generic taco entry
        cursor.execute("""
            UPDATE foods 
            SET glycemic_index = 52,
                glycemic_load = ROUND(52.0 * total_carbohydrate_g / 100.0, 1)
            WHERE name = 'Tacos (beef, hard shell)'
        """)
        
        rows_updated = cursor.rowcount
        logger.info(f"Updated {rows_updated} 'Tacos (beef, hard shell)' entries")
        
        # Check the update
        cursor.execute("""
            SELECT name, glycemic_index, glycemic_load, total_carbohydrate_g
            FROM foods 
            WHERE name = 'Tacos (beef, hard shell)'
        """)
        
        result = cursor.fetchone()
        if result:
            logger.info(f"Updated entry: {result[0]} - GI: {result[1]}, GL: {result[2]}, Carbs: {result[3]}g")
        
        # Also check all taco entries
        cursor.execute("""
            SELECT name, glycemic_index, glycemic_load, restaurant_name
            FROM foods 
            WHERE LOWER(name) LIKE '%taco%'
            ORDER BY restaurant_name NULLS FIRST, name
            LIMIT 20
        """)
        
        logger.info("\nAll taco entries:")
        for row in cursor.fetchall():
            restaurant = row[3] or "Generic"
            logger.info(f"  {row[0]} ({restaurant}): GI={row[1]}, GL={row[2]}")
        
        conn.commit()
        logger.info("\nDatabase updated successfully!")
        
    except Exception as e:
        logger.error(f"Error updating database: {e}")
        conn.rollback()
    finally:
        conn.close()

if __name__ == "__main__":
    fix_taco_gi()