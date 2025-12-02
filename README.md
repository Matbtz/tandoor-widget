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

The widget includes comprehensive debugging features to help diagnose API connection issues.

### Debug Features

1. **Debug Log Viewer**: The configuration activity now includes a real-time log viewer that shows:
   - API request details (URL, query parameters, date range)
   - Response status codes and messages
   - Parsed meal plans with their dates
   - Date matching results
   - Any errors or exceptions

2. **Test API Button**: Use the "Test API" button in the configuration screen to:
   - Verify your Tandoor URL and API key are correct
   - Check if the API is reachable
   - See what meal plans are returned for the current week
   - Debug date parsing and matching issues

3. **Android Logcat**: All debug information is also logged to Android's Logcat with these tags:
   - `TandoorWidget` - Widget service logs
   - `TandoorApiClient` - HTTP request/response details

### How to Debug API Issues

If you see "---" for meals even though you have planned meals:

1. Open the widget configuration (long-press widget > Configure)
2. Click the "Test API" button to verify:
   - The API connection works
   - The date range matches your planned meals
   - Meal dates are being parsed correctly
3. Click "Save" to refresh the widget and watch the debug logs
4. Check the logs for:
   - "Response code: 200" indicates successful API call
   - "Received X meal plans" shows how many meals were returned
   - "Parsed date" shows the date extracted from each meal
   - "Matched date" or "No match" indicates if meals matched the week dates

### Common Issues

- **Wrong date format**: The API returns dates like `2025-12-01T00:00:00+02:00`. The widget extracts just the date part (`2025-12-01`) for matching.
- **Date range mismatch**: The widget shows Saturday-Friday. Ensure your meals are planned within this range.
- **API filtering**: Check if the Tandoor API is correctly filtering meals by the requested date range.
