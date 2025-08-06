# GemMunch Test Report - Burger Analysis

## Test Details
- **Date**: August 5, 2025
- **Device**: Pixel 9 Pro XL (Android 16)
- **App Version**: com.stel.gemmunch.debug
- **Model**: Gemma 3n E4B

## Test Image
- **File**: classic_burger.jpg
- **Expected GI**: 66

## Results

### ✅ Success: Glycemic Index Correctly Displayed

The app successfully:
1. Analyzed the burger image
2. Retrieved correct glycemic index from database
3. Displayed GI = 66 (matching expected value)

### Performance Metrics
- **Total Analysis Time**: 32.1 seconds
  - Session Setup: 1ms
  - Prompt Processing: 1ms
  - Image Processing: 9ms
  - AI Inference: 30.2s
  - JSON Parsing: 7ms
  - Nutrition Lookup: 1.9s

### Nutritional Information Retrieved
- **Burger (1 item)**:
  - Calories: 110
  - Total Fat: 5.5g
  - Protein: 6.9g
  - Carbohydrates: 8.1g
  - Glycemic Index: 66 ✅
  - Glycemic Load: 5.4

### Issues Noted
- Beef patty (2 pieces) showed 0 calories and "Unknown" GI
- This suggests the database needs entries for individual burger components

## Conclusion

The glycemic index fix is working correctly! The burger was identified and its GI value of 66 was successfully retrieved and displayed from the database.

## Next Steps
1. Test with taco images to verify GI=52
2. Test with other foods from the database
3. Add missing entries for burger components (beef patty, etc.)