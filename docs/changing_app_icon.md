# How to Change GemMunch App Icon

## Method 1: Using Android Studio's Image Asset Studio (Recommended)

1. **Open Android Studio**
2. **Right-click** on `app/src/main/res` in the Project view
3. Select **New > Image Asset**
4. In the **Icon Type** dropdown, ensure "Launcher Icons (Adaptive and Legacy)" is selected
5. Configure your icon:
   - **Foreground Layer**: Upload your logo image (PNG/SVG recommended)
   - **Background Layer**: Choose a color or image
   - **Options**: Adjust scaling, trim, and safe zone
6. Preview the icon across different devices
7. Click **Next** and then **Finish**

## Method 2: Manual Replacement

### File Structure
```
app/src/main/res/
├── mipmap-mdpi/
│   ├── ic_launcher.webp (48x48)
│   └── ic_launcher_round.webp (48x48)
├── mipmap-hdpi/
│   ├── ic_launcher.webp (72x72)
│   └── ic_launcher_round.webp (72x72)
├── mipmap-xhdpi/
│   ├── ic_launcher.webp (96x96)
│   └── ic_launcher_round.webp (96x96)
├── mipmap-xxhdpi/
│   ├── ic_launcher.webp (144x144)
│   └── ic_launcher_round.webp (144x144)
├── mipmap-xxxhdpi/
│   ├── ic_launcher.webp (192x192)
│   └── ic_launcher_round.webp (192x192)
├── mipmap-anydpi-v26/
│   ├── ic_launcher.xml
│   └── ic_launcher_round.xml
└── drawable/
    ├── ic_launcher_background.xml
    └── ic_launcher_foreground.xml
```

### Steps:
1. Create your logo in the required sizes
2. Convert to WebP format (or use PNG)
3. Replace files in each mipmap folder
4. For adaptive icons (Android 8.0+), update:
   - `drawable/ic_launcher_foreground.xml`
   - `drawable/ic_launcher_background.xml`

## Method 3: Using Online Tools

1. **Launcher Icon Generator**: https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html
2. **App Icon Generator**: https://appicon.co/
3. Upload your logo, configure settings, download the generated assets
4. Extract and copy to your project's res folders

## Important Notes

- **Adaptive Icons**: Android 8.0+ uses adaptive icons with separate foreground and background layers
- **Round Icons**: Used on devices that display circular app icons
- **Safe Zone**: Keep important logo elements within the center 66% of the icon
- **WebP vs PNG**: WebP is smaller but PNG has wider compatibility

## Testing Your New Icon

1. Clean and rebuild the project:
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   ```

2. Install on device:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. Check:
   - App drawer
   - Home screen
   - Recent apps
   - Settings > Apps

## Design Guidelines

- **Simple and recognizable**: Works well at small sizes
- **Use bold shapes**: Thin lines may disappear at small sizes
- **Consider the background**: Test on various wallpapers
- **Follow Material Design**: https://material.io/design/iconography/