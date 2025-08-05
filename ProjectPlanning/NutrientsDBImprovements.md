# Nutrients Database Improvements

## Overview
This document outlines the comprehensive improvements made to the nutrients.db database, including expansion of food items, restaurant menu coverage, and glycemic index integration.

## Database Expansion Summary

### Initial State
- **Total Foods**: 141 items
- **Restaurants**: 16 chains
- **Glycemic Index Coverage**: 100 items (limited)

### Final State
- **Total Foods**: 716 items (5x increase)
- **Restaurants**: 36 chains (2.25x increase)
- **Glycemic Index Coverage**: 408 items (57% of database)
- **Database Size**: 472KB uncompressed, 133KB compressed

## Data Sources Integration

### 1. USDA FoodData Central (150 items)
- Enabled API integration in `build_ultimate_nutrients_db.py`
- Fetches comprehensive nutritional data including:
  - Foundation foods (whole foods with lab analysis)
  - SR Legacy (standard reference foods)
  - Branded foods (packaged products)
  - Restaurant foods
- API key: Configurable via environment variable

### 2. OpenFoodFacts Integration (310 items)
- Created `import_openfoodfacts.py` script
- Imports popular products by category:
  - Beverages, breakfast cereals, chocolates, cookies
  - Dairy, frozen foods, fruits, meats, snacks
  - Includes barcode data for scanning features
- Free, open-source data with no API restrictions

### 3. Expanded Common Foods (67 items)
- Created `expanded_common_foods_data.py`
- Added visual foods commonly photographed:
  - Fresh fruits and vegetables with detailed nutrients
  - Common proteins (various cooking methods)
  - Popular snacks and packaged foods
  - Prepared meals and dishes

### 4. Restaurant Data Expansion (189 items from 36 chains)

#### Original Chains (expanded menus):
- **Chipotle**: 12 items (added carnitas, sofritas, tacos, quesadilla)
- **In-N-Out**: 10 items (added Animal Style, Protein Style, Flying Dutchman)
- **Five Guys**: 12 items (added bacon burger, BLT, grilled cheese)
- **Chick-fil-A**: 13 items (added spicy sandwich, grilled options, salads)

#### New Restaurant Chains Added:
- **Coffee/Breakfast**: Dunkin', IHOP, Denny's
- **Pizza**: Papa John's, Little Caesars
- **Mexican**: Qdoba, Moe's Southwest Grill
- **Sandwiches**: Jimmy John's, Jersey Mike's
- **Casual Dining**: Olive Garden, Applebee's, Chili's, TGI Friday's
- **Asian**: Panda Express, P.F. Chang's
- **Vietnamese**: Pho 24, Pho Hoa, Lee's Sandwiches
- **Seafood**: Red Lobster

## Glycemic Index Implementation

### Database Schema
Added two fields to the `foods` table:
- `glycemic_index INTEGER` - GI value (0-100+)
- `glycemic_load REAL` - GL calculated as (GI × carbs) / 100

### GI Data Coverage
- Created comprehensive `glycemic_index_data.py` with 150+ food mappings
- Implemented intelligent matching algorithm in `update_glycemic_index.py`
- Coverage breakdown:
  - Low GI (0-55): 220 foods
  - Medium GI (56-69): 91 foods  
  - High GI (70+): 97 foods

### GI Assignment Logic
The update script uses multiple strategies:
1. Direct name matching (e.g., "apple" → GI: 36)
2. Partial word matching (e.g., "apple pie" → finds "apple")
3. Category-based defaults:
   - Proteins (chicken, beef, fish): GI: 0
   - Most vegetables: GI: 15
   - White bread items: GI: 75
   - Restaurant items (burgers: 66, pizza: 60, tacos: 52)

### UI Integration
Fixed glycemic index display in two places:

1. **TextOnlyMealViewModel** (Text-based meal tracking):
   - Shows GI/GL for each food item
   - Displays total meal glycemic load with category

2. **EnhancedChatViewModel** (Quick Snap image analysis):
   - Fixed missing GI display after image analysis
   - Shows per-item GI/GL values
   - Calculates total meal glycemic load

## Build Process

### Scripts Created/Modified
1. `build_ultimate_nutrients_db.py` - Main build script
2. `expanded_common_foods_data.py` - Additional common foods
3. `expanded_restaurant_data.py` - Restaurant menu items
4. `additional_restaurant_data.py` - More restaurant chains
5. `import_openfoodfacts.py` - OpenFoodFacts importer
6. `glycemic_index_data.py` - GI mappings
7. `update_glycemic_index.py` - GI updater
8. `import_expanded_restaurants.py` - Restaurant data importer
9. `import_additional_restaurants.py` - Additional restaurant importer

### Build Commands
```bash
# Main build with USDA data
python3 build_ultimate_nutrients_db.py

# Import OpenFoodFacts data
python3 import_openfoodfacts.py

# Add restaurant data
python3 import_expanded_restaurants.py
python3 import_additional_restaurants.py

# Update glycemic index values
python3 update_glycemic_index.py

# Copy to app
cp nutrients.db ../app/src/main/assets/nutrients.db
```

## Future Improvements

### Recommended Enhancements
1. **Barcode Scanning**: Utilize barcode data from OpenFoodFacts
2. **User Contributions**: Allow users to add/verify nutrition data
3. **Recipe Builder**: Combine ingredients to create custom meals
4. **Regional Foods**: Add international cuisine databases
5. **Cooking Methods**: Adjust nutrition based on preparation
6. **Portion Sizes**: More accurate serving size conversions

### API Key Security for Public Repo
- Remove hardcoded USDA API key before committing
- Add `.env.example` with placeholder
- Document how users can get their own API key
- Ship with 716-item database as "demo version"

## Performance Optimizations
- Added database indexes for faster searches
- Implemented FTS5 for full-text search (Android compatible)
- Popularity scoring for common items
- Compressed database option (133KB vs 472KB)

## Testing Notes
- Verified "tacos" returns GI: 52 in database
- Fixed UI display issues in both meal tracking modes
- Tested with various restaurant items and common foods
- Glycemic load calculations working correctly

## Bug Fixes
### Taco GI Issue (2025-08-05)
- **Issue**: Generic "Tacos (beef, hard shell)" entry had GI: 0 instead of 52
- **Cause**: The update_glycemic_index.py script assigned GI: 0 to beef/meat items
- **Fix**: Created fix_taco_gi.py to specifically update this entry to GI: 52
- **Result**: Now correctly returns GI: 52, GL: 18.7 for generic taco lookups