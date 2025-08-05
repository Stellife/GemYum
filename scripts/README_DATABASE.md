# GemMunch Nutrition Database Setup

## Overview

The GemMunch app uses a comprehensive nutrition database that combines data from multiple sources:
- USDA FoodData Central (Foundation & Branded foods)
- Open Food Facts
- Restaurant menu items
- International dishes
- Manual overrides for accuracy

## Database Features

- **Full-text search** with fuzzy matching
- **Multi-language support** with synonyms
- **Comprehensive nutrition data** (macros, micros, allergens)
- **Multiple deployment options** (embedded, CDN, on-demand)
- **Automatic updates** with versioning

## Building the Database

### Prerequisites
```bash
pip install requests
```

### Build Command
```bash
cd scripts
python build_comprehensive_nutrients_db.py --output-dir ../build/databases
```

This creates:
- `nutrients.db` - Full database (~10-15MB)
- `nutrients.db.gz` - Compressed full database (~3-5MB)
- `nutrients_lite.db` - Lite version with common foods (~2-3MB)
- `nutrients_lite.db.gz` - Compressed lite database (~1MB)
- `nutrients_manifest.json` - Version and hash information

## Deployment Options

### Option 1: CDN/Cloud Storage (Recommended)

1. Upload files to your CDN:
   ```bash
   # Example with AWS S3
   aws s3 cp nutrients.db.gz s3://your-bucket/gemmunch/
   aws s3 cp nutrients_lite.db.gz s3://your-bucket/gemmunch/
   aws s3 cp nutrients_manifest.json s3://your-bucket/gemmunch/
   ```

2. Update CDN URLs in `NutrientDatabaseManager.kt`:
   ```kotlin
   private const val CDN_BASE_URL = "https://your-cdn.com/gemmunch/"
   ```

3. The app will automatically download on first launch.

### Option 2: Git LFS

1. Install Git LFS:
   ```bash
   git lfs install
   ```

2. Track the compressed database:
   ```bash
   git lfs track "app/src/main/assets/nutrients_lite.db.gz"
   git add .gitattributes
   ```

3. Add and commit:
   ```bash
   git add app/src/main/assets/nutrients_lite.db.gz
   git commit -m "Add nutrition database via LFS"
   ```

### Option 3: GitHub Releases

Use the included GitHub Actions workflow which automatically:
1. Builds the database on changes
2. Creates a new release
3. Uploads database files as release assets

### Option 4: Embedded in APK

For the lite version only (to keep APK size reasonable):
1. Copy compressed database to assets:
   ```bash
   cp build/databases/nutrients_lite.db.gz app/src/main/assets/
   ```

2. The app will extract on first launch.

## Updating the Database

### Manual Updates
1. Modify data sources in `build_comprehensive_nutrients_db.py`
2. Rebuild the database
3. Update version in manifest
4. Deploy using chosen method

### Automatic Updates
The app checks for updates weekly and can:
- Notify users of new versions
- Auto-download updates (configurable)
- Verify integrity with SHA256

## Database Schema

Key tables:
- `nutrients` - Main nutrition data
- `nutrients_fts` - Full-text search index
- `serving_sizes` - Common serving conversions
- `food_synonyms` - Multi-language synonyms

Key fields:
- Basic: calories, protein, fat, carbs, fiber, sugar
- Detailed: vitamins, minerals, omega-3/6, glycemic index
- Metadata: source, confidence, allergens, category

## Testing

After deployment, test with:
```kotlin
// In your app
val stats = nutrientDatabaseManager.getDatabaseStats()
Log.d("DB", "Stats: $stats")

// Query examples
"SELECT * FROM nutrients WHERE food_name LIKE '%banana%';"
"SELECT * FROM nutrients_fts WHERE nutrients_fts MATCH 'chicken';"
```

## Troubleshooting

1. **Database not found**: Check CDN URLs and network connectivity
2. **Hash mismatch**: Database corrupted during download, will retry
3. **Old version**: Force update with `ensureDatabase(forceUpdate = true)`
4. **Large APK**: Use lite version or CDN deployment

## Future Improvements

- [ ] Add more restaurant chains
- [ ] Include recipe databases
- [ ] Add barcode scanning support
- [ ] Implement offline-first sync
- [ ] Add user-contributed foods