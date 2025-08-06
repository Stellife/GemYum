# GemMunch Testing Instructions

## Quick Start Testing

1. **Basic Recognition Test**
   - Use images from `test_images/tacos/`
   - Verify: Identifies as "taco", shows GI=52

2. **Restaurant Test**
   - Use images from `test_images/restaurant_meals/`
   - Test with context: "Chipotle bowl"
   - Verify: Identifies components correctly

3. **Glycemic Index Tests**
   - High GI: `test_images/breakfast/pancakes.jpg` (should show GI ~67)
   - Medium GI: `test_images/burgers/` (should show GI=66)
   - Low GI: `test_images/salads/` (should show GI=15)

## Comprehensive Testing Checklist

### ✅ Single Food Items
- [ ] Taco (GI: 52)
- [ ] Burger (GI: 66)
- [ ] Pizza (GI: 60)
- [ ] Salad (GI: 15)
- [ ] Apple (GI: 36)

### ✅ Restaurant Meals
- [ ] Chipotle Bowl
- [ ] McDonald's Big Mac
- [ ] Taco Bell items
- [ ] Vietnamese Pho (GI: 40)

### ✅ Complex Scenarios
- [ ] Multiple items on one plate
- [ ] Partially eaten food
- [ ] Poor lighting conditions
- [ ] Blurry images

### ✅ Quantity Detection
- [ ] Single item
- [ ] Multiple items (e.g., 3 tacos)
- [ ] Large quantities

## Performance Metrics to Track

1. **Recognition Accuracy**
   - Food correctly identified: ___/%
   - Quantity accuracy: ___/%

2. **Glycemic Index Accuracy**
   - GI values match database: ___/%
   - GL calculations correct: ___/%

3. **Processing Speed**
   - Average inference time: ___s
   - NPU/GPU utilization: Yes/No

## Edge Cases to Test

1. **Ambiguous Foods**
   - Burrito vs wrapped sandwich
   - Different types of pasta
   - Similar looking items

2. **Challenging Images**
   - Extreme close-ups
   - Far away shots
   - Multiple overlapping items
   - Food in containers

3. **Context Testing**
   - With restaurant context
   - Without context
   - Misleading context

## Bug Report Template

**Image**: [filename]
**Expected**: [what should be detected]
**Actual**: [what was detected]
**GI Expected**: [number]
**GI Actual**: [number]
**Notes**: [any additional observations]

---

Remember to test both "Snap & Log" and "Analyze & Chat" modes!
