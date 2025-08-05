#!/usr/bin/env python3
"""
Update foods in the database with glycemic index values and calculate glycemic load
"""

import sqlite3
import logging
from glycemic_index_data import GLYCEMIC_INDEX_MAP, calculate_glycemic_load

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def find_gi_for_food(food_name):
    """
    Find GI value for a food by checking various keywords
    """
    if not food_name:
        return None
        
    food_lower = food_name.lower()
    
    # Direct match
    if food_lower in GLYCEMIC_INDEX_MAP:
        return GLYCEMIC_INDEX_MAP[food_lower]
    
    # Check each word in the food name
    words = food_lower.split()
    for word in words:
        if word in GLYCEMIC_INDEX_MAP:
            return GLYCEMIC_INDEX_MAP[word]
    
    # Check for partial matches
    for gi_food, gi_value in GLYCEMIC_INDEX_MAP.items():
        if gi_food in food_lower:
            return gi_value
            
    # Special cases for restaurant foods
    if any(term in food_lower for term in ['burger', 'cheeseburger', 'hamburger']):
        return 66
    elif 'pizza' in food_lower:
        return 60
    elif any(term in food_lower for term in ['fries', 'french fries']):
        return 75
    elif 'salad' in food_lower and 'pasta' not in food_lower:
        return 15  # Most salads are low GI
    elif 'grilled chicken' in food_lower or 'grilled fish' in food_lower:
        return 0
    elif any(term in food_lower for term in ['donut', 'doughnut']):
        return 76
    elif 'pancake' in food_lower:
        return 67
    elif 'waffle' in food_lower:
        return 76
    elif 'taco' in food_lower:
        return 52
    elif 'burrito' in food_lower:
        return 55
    elif 'sandwich' in food_lower:
        if 'whole wheat' in food_lower or 'whole grain' in food_lower:
            return 54
        else:
            return 70  # Assume white bread
    elif 'wrap' in food_lower:
        return 57  # Tortilla wrap
    elif 'rice' in food_lower:
        if 'brown' in food_lower:
            return 68
        elif 'fried' in food_lower:
            return 75
        else:
            return 73  # White rice
    elif 'pasta' in food_lower or 'spaghetti' in food_lower or 'fettuccine' in food_lower:
        return 49
    elif 'noodle' in food_lower:
        if 'rice' in food_lower:
            return 61
        else:
            return 55
    elif 'bread' in food_lower:
        if any(term in food_lower for term in ['whole wheat', 'whole grain', 'rye']):
            return 51
        else:
            return 75
    elif 'coffee' in food_lower and 'frapp' not in food_lower:
        return 0
    elif 'tea' in food_lower and 'sweet' not in food_lower:
        return 0
    elif any(term in food_lower for term in ['soda', 'cola', 'pepsi', 'sprite']):
        return 70
    elif 'juice' in food_lower:
        return 50
    elif 'smoothie' in food_lower:
        return 55
    elif 'shake' in food_lower:
        return 61
    elif 'ice cream' in food_lower:
        return 61
    elif 'cookie' in food_lower:
        return 77
    elif 'cake' in food_lower:
        return 73
    elif 'muffin' in food_lower:
        return 71
    elif 'bagel' in food_lower:
        return 72
    elif 'croissant' in food_lower:
        return 67
    elif 'chips' in food_lower:
        if 'potato' in food_lower:
            return 56
        elif 'tortilla' in food_lower:
            return 63
        else:
            return 60
    elif 'wings' in food_lower or 'chicken tender' in food_lower:
        if 'fried' in food_lower or 'buffalo' in food_lower:
            return 70
        else:
            return 0
    elif 'soup' in food_lower:
        if 'lentil' in food_lower or 'bean' in food_lower:
            return 30
        else:
            return 40  # Average for soups
    elif 'pho' in food_lower:
        return 40  # Rice noodles in broth
    elif 'banh mi' in food_lower:
        return 75  # French bread
            
    return None

def update_database_gi():
    """Update all foods in database with GI values"""
    conn = sqlite3.connect('nutrients.db')
    cursor = conn.cursor()
    
    # Get all foods without GI values
    cursor.execute("""
        SELECT id, name, total_carbohydrate_g, glycemic_index 
        FROM foods 
        WHERE glycemic_index IS NULL OR glycemic_index = 0
    """)
    
    foods_to_update = cursor.fetchall()
    logger.info(f"Found {len(foods_to_update)} foods without GI values")
    
    updated_count = 0
    
    for food_id, name, carbs, current_gi in foods_to_update:
        gi_value = find_gi_for_food(name)
        
        if gi_value is not None:
            # Calculate glycemic load if we have carb data
            gl_value = None
            if carbs:
                gl_value = calculate_glycemic_load(gi_value, carbs)
            
            # Update the database
            cursor.execute("""
                UPDATE foods 
                SET glycemic_index = ?, glycemic_load = ?
                WHERE id = ?
            """, (gi_value, gl_value, food_id))
            
            updated_count += 1
            
            if updated_count % 50 == 0:
                logger.info(f"Updated {updated_count} foods so far...")
                conn.commit()
    
    # Also update foods that have GI but no GL
    cursor.execute("""
        UPDATE foods 
        SET glycemic_load = ROUND((glycemic_index * total_carbohydrate_g) / 100.0, 1)
        WHERE glycemic_index IS NOT NULL 
        AND glycemic_load IS NULL 
        AND total_carbohydrate_g IS NOT NULL
    """)
    
    conn.commit()
    
    # Get stats
    cursor.execute("SELECT COUNT(*) FROM foods WHERE glycemic_index IS NOT NULL")
    total_with_gi = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(*) FROM foods WHERE glycemic_load IS NOT NULL")
    total_with_gl = cursor.fetchone()[0]
    
    # Get GI distribution
    cursor.execute("""
        SELECT 
            CASE 
                WHEN glycemic_index <= 55 THEN 'Low GI (0-55)'
                WHEN glycemic_index <= 69 THEN 'Medium GI (56-69)'
                ELSE 'High GI (70+)'
            END as gi_category,
            COUNT(*) as count
        FROM foods
        WHERE glycemic_index IS NOT NULL
        GROUP BY gi_category
        ORDER BY gi_category
    """)
    gi_distribution = cursor.fetchall()
    
    logger.info(f"Update complete!")
    logger.info(f"Updated {updated_count} foods with new GI values")
    logger.info(f"Total foods with GI: {total_with_gi}")
    logger.info(f"Total foods with GL: {total_with_gl}")
    logger.info(f"GI Distribution: {gi_distribution}")
    
    # Show some examples
    cursor.execute("""
        SELECT name, glycemic_index, total_carbohydrate_g, glycemic_load
        FROM foods
        WHERE glycemic_index IS NOT NULL
        AND restaurant_name IS NOT NULL
        ORDER BY RANDOM()
        LIMIT 10
    """)
    
    logger.info("\nSample restaurant foods with GI/GL:")
    for row in cursor.fetchall():
        logger.info(f"  {row[0]}: GI={row[1]}, Carbs={row[2]}g, GL={row[3]}")
    
    conn.close()

if __name__ == "__main__":
    update_database_gi()