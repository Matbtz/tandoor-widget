# Release Notes - Version 1.1

## Fix: Refresh Button Not Updating Widget

### Problem
The refresh button on the widget was displaying "Requesting refresh..." but never showed the API call results. The "Test API" button in the configuration screen worked correctly and showed meal data within seconds, but the widget refresh button appeared to do nothing.

### Root Cause
The widget's refresh mechanism was working correctly at a technical level:
1. The refresh button triggered `ACTION_REFRESH_WIDGET` broadcast
2. This called `notifyAppWidgetViewDataChanged()` which initiated the API call
3. The API service made the call and sent log broadcasts (`ACTION_WIDGET_LOG`, `ACTION_WIDGET_ERROR`)

However, these custom broadcast actions were **not registered in AndroidManifest.xml**, so the widget provider never received the log messages. The debug view got stuck showing "Requesting refresh..." because no subsequent updates were received.

### Fix Applied
Added the custom broadcast actions to the `TandoorWidgetProvider` receiver's intent-filter in AndroidManifest.xml:
```xml
<intent-filter>
    <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    <action android:name="com.example.tandoorwidget.ACTION_REFRESH_WIDGET" />
    <action android:name="com.example.tandoorwidget.ACTION_WIDGET_LOG" />
    <action android:name="com.example.tandoorwidget.ACTION_WIDGET_ERROR" />
</intent-filter>
```

### What's New
- ✅ Refresh button now properly displays API call progress and results
- ✅ Debug view shows real-time updates during refresh:
  - "=== Starting data refresh ==="
  - API request details
  - Response status
  - Number of meals matched
  - "=== Data refresh complete: X meals matched ==="
- ✅ Error messages are properly displayed if the API call fails

### Version Changes
- Version code: 1 → 2
- Version name: "1.0" → "1.1"

### Testing
After installing this version:
1. Add the widget to your home screen (or re-configure existing widget)
2. Click the refresh button (circular arrow icon)
3. You should see the debug text update through several stages:
   - "Requesting refresh..."
   - "=== Starting data refresh ==="
   - API request details
   - "Success: Received X meal plans"
   - "=== Data refresh complete: X meals matched ==="
4. The meal list should update to show current week's meals

### How to Create This Release

To trigger the GitHub Actions workflow to build and publish the APK:

```bash
# Make sure your changes are merged to master first
git checkout master
git merge copilot/fix-widget-refresh-button

# Create and push the release tag
git tag -a v1.1 -m "Fix refresh button not updating widget - see RELEASE_NOTES_v1.1.md"
git push origin v1.1
```

The GitHub Actions workflow will automatically:
1. Build the signed APK with the configured keystore
2. Create a GitHub release with tag v1.1
3. Attach the signed APK to the release
4. Generate release notes

### Notes
- The GitHub repository must have the signing secrets configured (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD)
- See README.md for details on configuring signing secrets
