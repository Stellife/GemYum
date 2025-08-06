# Contributing to GemYum

First off, thank you for considering contributing to GemYum! 🎉

GemYum is an open-source project built for the Kaggle Gemma 3n Hackathon, and we welcome contributions from the community to make on-device AI nutrition tracking even better.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [How to Contribute](#how-to-contribute)
- [Development Setup](#development-setup)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Documentation](#documentation)

## 📜 Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code:

- Be respectful and inclusive
- Welcome newcomers and help them get started
- Focus on constructive criticism
- Show empathy towards other community members

## 🚀 Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/GemYum.git
   cd GemYum
   ```
3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/ORIGINAL_OWNER/GemYum.git
   ```
4. **Create a branch** for your changes:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## 🤝 How to Contribute

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates. When creating a bug report, include:

- **Clear title and description**
- **Steps to reproduce**
- **Expected behavior**
- **Actual behavior**
- **Screenshots** (if applicable)
- **Device information** (Android version, device model, RAM)
- **App version**

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, include:

- **Clear title and description**
- **Use case** - Why is this enhancement useful?
- **Possible implementation** - If you have ideas on how to implement it
- **Alternatives considered**

### Areas We Need Help

- 🌍 **Internationalization** - Help translate the app
- 📊 **More Food Data** - Add cuisine-specific databases
- 🎨 **UI/UX Improvements** - Make the app more beautiful
- ⚡ **Performance Optimization** - Make it even faster
- 📱 **Device Compatibility** - Test on more devices
- 📚 **Documentation** - Improve guides and tutorials
- 🧪 **Testing** - Add more test coverage

## 💻 Development Setup

### Prerequisites

- Android Studio Ladybug or newer
- JDK 17+
- Android SDK 31+
- 8GB+ RAM for comfortable development

### Setup Steps

1. **Install Android Studio** from [https://developer.android.com/studio](https://developer.android.com/studio)

2. **Clone and open the project**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/GemYum.git
   cd GemYum
   ```

3. **Copy configuration template**:
   ```bash
   cp gradle.properties.template gradle.properties
   ```

4. **(Optional) Add API keys** to `gradle.properties`:
   - `HF_TOKEN` - For Hugging Face model downloads
   - `USDA_API_KEY` - For nutrition data updates
   
   Note: The app works without these keys!

5. **Sync project** in Android Studio (File → Sync Project with Gradle Files)

6. **Download models** (7.4GB):
   - Option A: Use the in-app downloader
   - Option B: Download manually and place in `/data/local/tmp/`

7. **Run the app** on emulator or physical device

### Project Structure

```
app/src/main/java/com/stel/gemmunch/
├── ui/                 # Jetpack Compose UI
│   ├── screens/       # Main screens
│   ├── components/    # Reusable components
│   └── theme/         # Material3 theming
├── viewmodels/        # MVVM ViewModels
├── agent/             # AI and analysis logic
├── data/              # Data models
├── utils/             # Utility functions
└── AppContainer.kt    # Dependency injection
```

## 🔄 Pull Request Process

1. **Update your fork**:
   ```bash
   git fetch upstream
   git checkout main
   git merge upstream/main
   ```

2. **Create feature branch**:
   ```bash
   git checkout -b feature/your-feature
   ```

3. **Make your changes**:
   - Write clean, readable code
   - Add comments for complex logic
   - Update documentation if needed
   - Add tests for new features

4. **Test thoroughly**:
   ```bash
   ./gradlew test
   ./gradlew lint
   ```

5. **Commit with clear message**:
   ```bash
   git commit -m "feat: add amazing feature
   
   - Detailed description of what changed
   - Why this change was made
   - Any breaking changes or migrations needed"
   ```

6. **Push to your fork**:
   ```bash
   git push origin feature/your-feature
   ```

7. **Create Pull Request**:
   - Go to GitHub and create PR from your fork
   - Fill in the PR template
   - Link related issues
   - Add screenshots/videos if UI changes

8. **Address review feedback**:
   - Make requested changes
   - Push updates to the same branch
   - Respond to all review comments

### PR Requirements

- ✅ All tests pass
- ✅ No lint errors
- ✅ Code follows style guide
- ✅ Documentation updated
- ✅ Commit messages follow convention
- ✅ PR description is complete

## 📝 Coding Standards

### Kotlin Style Guide

We follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) with these additions:

```kotlin
// Good: Descriptive names
fun calculateNutritionForMeal(meal: Meal): NutritionInfo

// Bad: Unclear abbreviations
fun calcNutr(m: Meal): NInfo

// Good: Clear class structure
class MealAnalyzer(
    private val nutritionDb: NutritionDatabase,
    private val aiModel: GemmaModel
) {
    // Public API at top
    fun analyzeMeal(image: Bitmap): AnalysisResult
    
    // Private helpers below
    private fun preprocessImage(image: Bitmap): Tensor
}
```

### Compose Guidelines

```kotlin
// Good: Stateless composables with clear parameters
@Composable
fun FoodCard(
    food: Food,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Implementation
}

// Good: Preview functions for UI components
@Preview
@Composable
fun FoodCardPreview() {
    FoodCard(
        food = sampleFood,
        onClick = {}
    )
}
```

### Comments and Documentation

```kotlin
/**
 * Analyzes a food image using Gemma 3n model.
 * 
 * @param image The bitmap image to analyze
 * @param useGpu Whether to use GPU acceleration
 * @return Analysis result with identified foods and confidence scores
 * @throws ModelNotLoadedException if the model isn't initialized
 */
suspend fun analyzeImage(
    image: Bitmap,
    useGpu: Boolean = true
): AnalysisResult
```

## 🧪 Testing

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest

# All tests
./gradlew check
```

### Writing Tests

```kotlin
// Example unit test
@Test
fun `calculateCalories returns correct total`() {
    val foods = listOf(
        Food("Apple", calories = 95),
        Food("Banana", calories = 105)
    )
    
    val total = NutritionCalculator.calculateTotalCalories(foods)
    
    assertEquals(200, total)
}

// Example UI test
@Test
fun foodCard_displaysCorrectInfo() {
    composeTestRule.setContent {
        FoodCard(
            food = Food("Apple", calories = 95),
            onClick = {}
        )
    }
    
    composeTestRule
        .onNodeWithText("Apple")
        .assertIsDisplayed()
}
```

## 📚 Documentation

### Code Documentation

- Add KDoc comments to all public APIs
- Include examples in complex functions
- Document edge cases and limitations

### README Updates

When adding features, update:
- Feature list if adding major feature
- Installation steps if process changes
- Requirements if dependencies change

### Additional Docs

For major features, add documentation in `docs/`:
- Architecture decisions
- API documentation
- Integration guides

## 🎯 Focus Areas

### Current Priorities

1. **Performance** - Reduce inference time below 0.5 seconds
2. **Accuracy** - Improve food recognition accuracy
3. **Database** - Add more international foods
4. **Accessibility** - Add screen reader support
5. **Testing** - Increase test coverage to 80%+

### Future Ideas

- Voice input for hands-free logging
- Meal planning features
- Recipe import/export
- Wearable device integration
- Multi-language support

## 📮 Questions?

- **Discord**: [Join our community](https://discord.gg/gemyum)
- **GitHub Discussions**: [Ask questions](https://github.com/OWNER/GemYum/discussions)
- **Issues**: [Report bugs](https://github.com/OWNER/GemYum/issues)

## 🙏 Recognition

Contributors will be recognized in:
- README.md contributors section
- In-app credits screen
- Release notes

Thank you for helping make GemYum better! 🚀

---

**Remember**: The goal is to make nutrition tracking accessible to everyone, everywhere, with complete privacy. Every contribution helps achieve this mission!