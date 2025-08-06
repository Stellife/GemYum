#!/bin/bash

# Script to prepare the repository for public release
# This removes sensitive files and cleans up development artifacts

echo "🧹 Preparing repository for public release..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Files and directories to remove (sensitive or unnecessary)
FILES_TO_REMOVE=(
    # Test results and screenshots
    "scripts/test_results"
    "scripts/ui_dump*.xml"
    "test_images/current_screen.png"
    "test_images/manual_test_logs.txt"
    
    # Python cache
    "scripts/__pycache__"
    "**/__pycache__"
    "**/*.pyc"
    
    # Development notes that might contain sensitive info
    "ProjectPlanning/ClaudeNotes"
    
    # IDE files
    ".idea"
    "*.iml"
    
    # Build artifacts
    "build"
    "app/build"
    ".gradle"
    
    # Local configuration
    "local.properties"
    
    # Signing keys (should never be committed anyway)
    "*.keystore"
    "*.jks"
    
    # Database files (users should build their own)
    "app/src/main/assets/nutrients.db"
    "scripts/nutrients.db"
    "scripts/*.db"
    "scripts/*.db-journal"
    
    # Backup files
    "**/*~"
    "**/*.bak"
    "**/*.backup"
)

# Create a .gitignore if it doesn't exist
echo -e "${YELLOW}Creating comprehensive .gitignore...${NC}"
cat > .gitignore << 'EOF'
# Built application files
*.apk
*.aar
*.ap_
*.aab

# Files for the ART/Dalvik VM
*.dex

# Java class files
*.class

# Generated files
bin/
gen/
out/
release/

# Gradle files
.gradle/
build/

# Local configuration file
local.properties

# Proguard folder
proguard/

# Log Files
*.log

# Android Studio Navigation editor temp files
.navigation/

# Android Studio captures folder
captures/

# IntelliJ
*.iml
.idea/
*.iws

# Keystore files
*.jks
*.keystore

# External native build folder
.externalNativeBuild
.cxx/

# Google Services (if using Firebase)
google-services.json

# Freeline
freeline.py
freeline/
freeline_project_description.json

# fastlane
fastlane/report.xml
fastlane/Preview.html
fastlane/screenshots
fastlane/test_output
fastlane/readme.md

# Python
__pycache__/
*.py[cod]
*$py.class
*.so
.Python
env/
venv/
.venv

# Database files
*.db
*.sqlite
*.db-journal

# API Keys and secrets
secrets.properties
apikeys.properties
*.env
!.env.example

# MacOS
.DS_Store

# Test artifacts
test_results/
test_images/current_screen.png
test_images/manual_test_logs.txt
ui_dump*.xml

# Temporary files
*.tmp
*.temp
*~
*.swp
*.bak
*.backup

# Model files (too large)
*.tflite
*.bin
*.onnx
models/gemma*
models/*/

# Nutrition database (build locally)
nutrients.db
app/src/main/assets/nutrients.db
scripts/nutrition_data/
EOF

echo -e "${GREEN}✓ .gitignore created${NC}"

# Remove sensitive files
echo -e "${YELLOW}Removing sensitive and unnecessary files...${NC}"
for file in "${FILES_TO_REMOVE[@]}"; do
    if [ -e "$file" ] || compgen -G "$file" > /dev/null 2>&1; then
        rm -rf $file 2>/dev/null
        echo -e "${GREEN}✓ Removed: $file${NC}"
    fi
done

# Create essential directories with .gitkeep
echo -e "${YELLOW}Creating directory structure...${NC}"
mkdir -p models
mkdir -p app/src/main/assets/models
mkdir -p app/src/main/assets/databases
mkdir -p releases

# Add .gitkeep files
touch models/.gitkeep
touch app/src/main/assets/models/.gitkeep
touch app/src/main/assets/databases/.gitkeep
touch releases/.gitkeep

# Create .env.example
echo -e "${YELLOW}Creating .env.example...${NC}"
cat > .env.example << 'EOF'
# Copy this to .env and configure for your environment

# Build Configuration
BUILD_TYPE=debug
ENABLE_PROGUARD=false

# Model Configuration
MODEL_PATH=models/
USE_BUNDLED_MODELS=false

# Database
DB_PATH=app/src/main/assets/databases/nutrients.db
BUILD_DB_FROM_SOURCE=true

# Optional: Add your own API keys here
# USDA_API_KEY=your_key_here
EOF

echo -e "${GREEN}✓ .env.example created${NC}"

# Create a clean README
echo -e "${YELLOW}Creating clean README...${NC}"
if [ -f "README_PUBLIC.md" ]; then
    mv README_PUBLIC.md README.md
    echo -e "${GREEN}✓ README.md updated${NC}"
fi

# Create LICENSE file (Apache 2.0)
echo -e "${YELLOW}Creating LICENSE file...${NC}"
cat > LICENSE << 'EOF'
Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/

Copyright 2024 Stel

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
EOF

echo -e "${GREEN}✓ LICENSE created${NC}"

# Remove any API keys or sensitive strings from code
echo -e "${YELLOW}Checking for potential API keys or sensitive data...${NC}"

# Check for common API key patterns
if grep -r "api[_-]key\|apikey\|secret\|password\|token" --include="*.kt" --include="*.java" --include="*.xml" --exclude-dir=".git" . 2>/dev/null | grep -v "// " | grep -v "BuildConfig" | grep -v "getString"; then
    echo -e "${RED}⚠️  Warning: Potential sensitive data found. Please review the above matches.${NC}"
else
    echo -e "${GREEN}✓ No obvious API keys found${NC}"
fi

# Final cleanup
echo -e "${YELLOW}Final cleanup...${NC}"

# Remove empty directories
find . -type d -empty -delete 2>/dev/null

# Summary
echo -e "\n${GREEN}✨ Repository prepared for public release!${NC}"
echo -e "\n${YELLOW}Next steps:${NC}"
echo "1. Review the changes with: git status"
echo "2. Build the nutrition database: cd scripts && python build_nutrients_db.py"
echo "3. Download models: cd models && ./download_models.sh"
echo "4. Test the build: ./gradlew assembleDebug"
echo "5. Commit the cleaned repository: git add -A && git commit -m 'Prepare for public release'"
echo "6. Push to public repository: git push origin public-release"

echo -e "\n${YELLOW}Important:${NC}"
echo "- The nutrition database has been removed. Users must build it themselves."
echo "- Model files are not included. Users must download them."
echo "- Test images and results have been removed."
echo "- All Python cache files have been cleaned."