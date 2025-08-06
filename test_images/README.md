# GemMunch Test Images

This directory contains test images for the GemMunch food recognition app.

## Categories

1. **tacos/** - Test glycemic index GI=52
   - Hard shell tacos
   - Soft tacos
   - Street tacos

2. **burgers/** - Test glycemic index GI=66
   - Classic burgers
   - Cheeseburgers

3. **pizza/** - Test glycemic index GI=60
   - Various pizza types

4. **salads/** - Test low glycemic index GI=15
   - Caesar salad
   - Greek salad

5. **restaurant_meals/** - Test restaurant identification
   - Chipotle bowls
   - Vietnamese pho (GI=40)
   - Sushi

6. **breakfast/** - Common breakfast items
   - Pancakes
   - Eggs and bacon

7. **complex_meals/** - Test multi-item detection
   - Bento boxes
   - Full dinner plates

8. **challenging_cases/** - Edge cases
   - Poor lighting
   - Messy presentation
   - Partially eaten food

## Testing Guidelines

1. **Single Item Tests**: Start with clear, single-item images
2. **Restaurant Tests**: Test restaurant-specific items (Chipotle, etc.)
3. **GI Verification**: Verify glycemic index values match database
4. **Complex Scenes**: Test multi-item detection
5. **Edge Cases**: Test challenging scenarios

## Expected Results

See `test_images_metadata.json` for expected results for each image.

## Image Sources

Images sourced from Unsplash (free stock photos).
Replace with your own test images for more accurate testing.
