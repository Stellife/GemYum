#!/usr/bin/env python3

import sqlite3
import os

# Create a minimal working database
db_path = "../app/src/main/assets/databases/nutrients.db"
os.makedirs(os.path.dirname(db_path), exist_ok=True)

# Remove existing database
if os.path.exists(db_path):
    os.remove(db_path)

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

# Create minimal schema
cursor.execute('''
CREATE TABLE IF NOT EXISTS foods (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    brand TEXT,
    calories REAL DEFAULT 0,
    protein REAL DEFAULT 0,
    total_fat REAL DEFAULT 0,
    saturated_fat REAL DEFAULT 0,
    cholesterol REAL DEFAULT 0,
    sodium REAL DEFAULT 0,
    total_carbs REAL DEFAULT 0,
    dietary_fiber REAL DEFAULT 0,
    sugars REAL DEFAULT 0,
    glycemic_index INTEGER,
    glycemic_load REAL,
    serving_size TEXT DEFAULT '100g',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
''')

# Add some basic foods for testing
foods = [
    ("Taco", None, 210, 9, 10, 4, 25, 570, 21, 3, 2, 52, 11, "1 taco"),
    ("Chicken breast", None, 165, 31, 3.6, 1, 85, 74, 0, 0, 0, 0, 0, "100g"),
    ("Rice, white", None, 130, 2.7, 0.3, 0.1, 0, 1, 28, 0.4, 0.1, 73, 20, "100g"),
    ("Apple", None, 52, 0.3, 0.2, 0, 0, 1, 14, 2.4, 10, 36, 5, "1 medium"),
    ("Pizza, cheese", None, 266, 11, 10, 4.5, 22, 598, 33, 2, 3.8, 60, 20, "1 slice"),
    ("Hamburger", None, 540, 25, 27, 10, 80, 950, 45, 2, 9, 61, 27, "1 burger"),
    ("Salad, caesar", None, 190, 4, 16, 3, 10, 360, 9, 2, 2, 0, 0, "1 cup"),
    ("French fries", None, 365, 4, 17, 3, 0, 246, 48, 4, 0.3, 75, 36, "100g"),
    ("Chipotle bowl", "Chipotle", 650, 32, 22, 7, 95, 1350, 71, 11, 4, 55, 39, "1 bowl"),
    ("Big Mac", "McDonald's", 563, 26, 33, 11, 79, 1010, 45, 3, 9, 61, 27, "1 burger")
]

for food in foods:
    cursor.execute('''
        INSERT INTO foods (name, brand, calories, protein, total_fat, saturated_fat, 
                          cholesterol, sodium, total_carbs, dietary_fiber, sugars, 
                          glycemic_index, glycemic_load, serving_size)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ''', food)

# Create FTS table for search
cursor.execute('''
CREATE VIRTUAL TABLE foods_fts USING fts5(
    name, 
    brand,
    content=foods,
    content_rowid=id
)
''')

# Populate FTS
cursor.execute('''
INSERT INTO foods_fts (rowid, name, brand)
SELECT id, name, brand FROM foods
''')

conn.commit()
conn.close()

print(f"✅ Created minimal database at {db_path}")
print(f"   Size: {os.path.getsize(db_path) / 1024:.1f} KB")
print(f"   Foods: {len(foods)} items")