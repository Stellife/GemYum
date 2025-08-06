#!/usr/bin/env python3
"""
Debug script to investigate the burger calorie issue.
"""

import sqlite3
from pathlib import Path

def investigate_burger_entries():
    db_path = Path("../app/src/main/assets/nutrients.db")
    
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    print("=== INVESTIGATING BURGER DATABASE ISSUE ===\n")
    
    # 1. Check all burger entries
    print("1. All burger entries in database:")
    print("-" * 80)
    cursor.execute("""
        SELECT name, calories, serving_size, glycemic_index, brand_name, restaurant_name
        FROM foods 
        WHERE name LIKE '%burger%' 
        ORDER BY calories ASC
        LIMIT 20
    """)
    
    for row in cursor.fetchall():
        name, cal, serving, gi, brand, restaurant = row
        print(f"{name}: {cal} cal, {serving}, GI={gi}, Brand={brand}, Restaurant={restaurant}")
    
    # 2. Check for entries with exactly 110 calories
    print("\n2. Entries with exactly 110 calories:")
    print("-" * 80)
    cursor.execute("""
        SELECT name, calories, serving_size, glycemic_index
        FROM foods 
        WHERE calories = 110.0
        ORDER BY name
    """)
    
    results = cursor.fetchall()
    if results:
        for row in results:
            print(f"{row[0]}: {row[1]} cal, {row[2]}, GI={row[3]}")
    else:
        print("No entries found with exactly 110 calories")
    
    # 3. Check for beef patty entries
    print("\n3. Beef patty entries:")
    print("-" * 80)
    cursor.execute("""
        SELECT name, calories, serving_size, glycemic_index
        FROM foods 
        WHERE name LIKE '%beef%' AND name LIKE '%patt%'
        ORDER BY name
    """)
    
    results = cursor.fetchall()
    if results:
        for row in results:
            print(f"{row[0]}: {row[1]} cal, {row[2]}, GI={row[3]}")
    else:
        print("No beef patty entries found")
    
    # 4. Check for ground beef
    print("\n4. Ground beef entries:")
    print("-" * 80)
    cursor.execute("""
        SELECT name, calories, serving_size, glycemic_index
        FROM foods 
        WHERE name LIKE '%ground beef%' OR name LIKE '%beef%ground%'
        ORDER BY calories DESC
        LIMIT 10
    """)
    
    for row in cursor.fetchall():
        print(f"{row[0]}: {row[1]} cal, {row[2]}, GI={row[3]}")
    
    # 5. Look for the most reasonable burger match
    print("\n5. Most likely burger matches (300-800 calories):")
    print("-" * 80)
    cursor.execute("""
        SELECT name, calories, serving_size, glycemic_index, restaurant_name
        FROM foods 
        WHERE name LIKE '%burger%' AND calories BETWEEN 300 AND 800
        ORDER BY calories ASC
        LIMIT 10
    """)
    
    for row in cursor.fetchall():
        name, cal, serving, gi, restaurant = row
        rest_info = f" ({restaurant})" if restaurant else ""
        print(f"{name}{rest_info}: {cal} cal, {serving}, GI={gi}")
    
    conn.close()
    
    print("\n=== ANALYSIS ===")
    print("The 110 calories for a burger seems incorrect.")
    print("Most burgers in the database range from 390-840 calories.")
    print("The app might be using a fallback or USDA API result.")

if __name__ == "__main__":
    investigate_burger_entries()