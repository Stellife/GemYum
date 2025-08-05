#!/bin/bash

# Script to create the ultimate nutrients database
# Ensure Python 3 is installed and required packages

echo "🍔 GemMunch Ultimate Nutrients Database Builder"
echo "=============================================="

# Check if Python 3 is installed
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 is required but not installed."
    exit 1
fi

# Navigate to scripts directory
cd "$(dirname "$0")"

# Install required packages if needed
echo "📦 Checking dependencies..."
pip3 install requests sqlite3 2>/dev/null || true

# Run the database builder
echo "🏗️ Building comprehensive nutrients database..."
echo "This will create a database with:"
echo "  - USDA FoodData Central foods"
echo "  - Fast food restaurant menus (20+ chains)"
echo "  - Glycemic index data"
echo "  - Complete macro/micronutrient profiles"
echo ""

# Set API key if provided
if [ -n "$USDA_API_KEY" ]; then
    echo "✅ Using provided USDA API key"
else
    echo "⚠️  No USDA API key provided. Using DEMO_KEY (limited data)"
    echo "   Get a free key at: https://fdc.nal.usda.gov/api-key-signup.html"
    echo "   Then run: export USDA_API_KEY='your-key-here'"
fi

# Run the builder
python3 build_ultimate_nutrients_db.py

# Check if database was created
if [ -f "nutrients.db" ]; then
    SIZE=$(du -h nutrients.db | cut -f1)
    echo ""
    echo "✅ Database created successfully!"
    echo "📊 Size: $SIZE"
    echo "📍 Location: $(pwd)/nutrients.db"
    
    # Copy to app assets
    APP_ASSETS="../app/src/main/assets"
    if [ -d "$APP_ASSETS" ]; then
        echo ""
        echo "📱 Copying to app assets..."
        cp nutrients.db "$APP_ASSETS/"
        echo "✅ Database copied to $APP_ASSETS/"
    else
        echo ""
        echo "⚠️  App assets directory not found at $APP_ASSETS"
        echo "   Please manually copy nutrients.db to your app's assets folder"
    fi
else
    echo "❌ Database creation failed!"
    exit 1
fi

echo ""
echo "🎉 Done!"