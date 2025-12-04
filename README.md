# Tandoor Widget

An Android widget for displaying and managing your Tandoor meal plans.

## Features

- 📅 Display your weekly meal plan (Saturday to Friday)
- 🔄 Real-time synchronization with your Tandoor instance
- 📊 Multi-day recipe support - extend recipes across multiple days
- ✏️ Edit meal plans directly from the widget:
  - Move recipes between days
  - Extend recipes to span multiple days
  - Quick access to edit any meal plan entry
- 🐛 Comprehensive debugging and logging features

For detailed information about the multi-day meal feature, see [MULTI_DAY_MEALS_GUIDE.md](MULTI_DAY_MEALS_GUIDE.md).

## Build Configuration

This project requires the Android SDK to be configured.

### Fixing "SDK location not found"

You need to tell Gradle where your Android SDK is located. You can do this in one of two ways:

1.  **Create `local.properties` file:**
    *   Rename `local.properties.example` to `local.properties`.
    *   Open `local.properties` and verify the `sdk.dir` path matches your Android SDK location.
    *   For Windows users, the default path is usually `C:\Users\<YourUsername>\AppData\Local\Android\Sdk`.

2.  **Set Environment Variable:**
    *   Set the `ANDROID_HOME` environment variable to your Android SDK location.

## Debugging the Widget

The widget includes comprehensive debugging features to help diagnose issues. For detailed troubleshooting, see [WIDGET_DEBUG_GUIDE.md](WIDGET_DEBUG_GUIDE.md).

### Quick Debug Steps

1. **Open the widget configuration** (long-press widget > Configure or tap widget title)
2. **Check the Debug Logs section** at the bottom - it shows real-time events
3. **Use "Test API" button** to verify your connection works
4. **Click "Save"** and watch the logs to see what happens during widget update

### Debug Features

1. **Debug Log Viewer**: The configuration activity includes a real-time log viewer that shows:
   - Configuration being saved
   - Widget update process (onUpdate, RemoteViews creation, data refresh)
   - API request details (URL, query parameters, date range)
   - Response status codes and messages
   - Parsed meal plans with their dates
   - Date matching results
   - Any errors or exceptions with specific causes

2. **Test API Button**: Use to verify your Tandoor URL and API key are correct before saving

3. **Android Logcat**: All debug information is also logged to Android's Logcat with these tags:
   - `TandoorWidgetProvider` - Widget lifecycle and update events
   - `TandoorWidget` - Widget service and data loading
   - `TandoorApiClient` - HTTP request/response details

### Common Issues

If the widget shows an error message:

- **"Configuration needed"**: No URL or API key saved. Tap widget title to configure.
- **"API Error"**: Check your API key and URL in the config screen. Use "Test API" to diagnose.
- **"Connection Error"**: Network issue or invalid URL. Check network and verify URL format.
- **Empty dates with no meals**: Check Tandoor has meals planned for the current week (Saturday-Friday).

For detailed troubleshooting steps and log examples, see [WIDGET_DEBUG_GUIDE.md](WIDGET_DEBUG_GUIDE.md).

## CI/CD - Automatic Release

This project is configured to automatically build and release signed APK files via GitHub Actions when you push a version tag.

### Configuring GitHub Secrets

Before creating releases, you need to configure the signing secrets in your GitHub repository.

#### 1. Generate a Keystore (if you don't have one)

```bash
keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias release
```

Follow the prompts to set:
- Keystore password (you'll need this as `KEYSTORE_PASSWORD`)
- Key password (you'll need this as `KEY_PASSWORD`)
- Your name and organization details

#### 2. Encode the Keystore to Base64

On Linux/Mac:
```bash
base64 -i release-keystore.jks -o keystore-base64.txt
```

On Windows (PowerShell):
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release-keystore.jks")) | Out-File keystore-base64.txt
```

#### 3. Configure Secrets in GitHub

Go to your repository: **Settings > Secrets and variables > Actions > New repository secret**

Add these secrets:
- `KEYSTORE_BASE64`: Content of the `keystore-base64.txt` file
- `KEYSTORE_PASSWORD`: Password you used when creating the keystore
- `KEY_ALIAS`: `release` (or the alias you specified when creating the keystore)
- `KEY_PASSWORD`: Key password you used when creating the keystore

### Creating a Release

Once secrets are configured, creating a release is simple:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The GitHub Actions workflow will automatically:
1. Build the signed APK with your keystore
2. Create a GitHub release with the version tag
3. Attach the signed APK to the release
4. Generate release notes automatically

### Version Naming

Use semantic versioning for tags: `v<major>.<minor>.<patch>`

Examples:
- `v1.0.0` - First release
- `v1.1.0` - New features added
- `v1.1.1` - Bug fixes
