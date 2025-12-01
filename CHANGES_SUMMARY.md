# Summary of Changes - API Debugging Features

## Problem Statement

The widget displays dates with "---" even though there are planned meals in Tandoor. The API call appears to not be working correctly, returning meals from wrong date ranges or not matching meals to dates.

## Changes Made

### 1. Enhanced Logging in TandoorWidgetService.kt

Added comprehensive debug logging throughout the meal plan fetching process:

- **Current date and week calculation**: Logs the current date, day of week, and the calculated Saturday start date
- **Week date range**: Logs all 7 dates (Saturday through Friday) that the widget will display
- **API request details**: Logs the exact API endpoint, query parameters, and sanitized authorization token
- **Response details**: Logs HTTP response code and message
- **Meal plan parsing**: For each meal received, logs:
  - Recipe name
  - Raw `from_date` value from API
  - Parsed date (first 10 characters)
- **Date matching**: For each date in the week, logs whether a meal was matched or not
- **Summary**: Final count of how many meals were successfully matched

### 2. HTTP Logging Interceptor (ApiClient.kt)

Added OkHttp logging interceptor that logs:
- Complete HTTP request URL with query parameters
- Request headers (including Authorization)
- Response status code
- Full response body

This is logged to Android Logcat with tag `TandoorApiClient` for easy filtering.

**Dependency added**: `com.squareup.okhttp3:logging-interceptor:4.9.0`

### 3. Real-time Debug Log Viewer (ConfigActivity.kt)

Enhanced the configuration activity with:

**UI Changes**:
- Added scrollable debug log viewer with monospace font
- Added "Test API" button to test connection without saving
- Added "Clear Logs" button to reset the log view
- Changed "Save" button layout to horizontal with "Test API"

**Functionality**:
- BroadcastReceiver that listens for log and error messages from the widget service
- Real-time display of all widget logs in the configuration activity
- Logs persist in the activity until cleared or refreshed

**Test API Feature**:
- Tests the API connection using current configuration without saving
- Shows the exact request being made
- Displays first few meal plans returned
- Helps verify date parsing and API response format
- Useful for troubleshooting before committing configuration changes

### 4. Updated Layout (activity_config.xml)

Added UI elements:
- TextView for "Debug Logs" section header
- ScrollView containing debug log TextView
- "Test API" button alongside "Save" button
- "Clear Logs" button below the log viewer

### 5. Documentation

**README.md**:
- Added "Debugging the Widget" section
- Documented all debug features
- Added "How to Debug API Issues" guide
- Listed common issues and solutions

**DEBUGGING_API_ISSUES.md** (new file):
- Detailed root cause analysis
- Step-by-step debugging procedures
- Expected behavior documentation
- Solutions for common problems
- Example log output

## How to Use the Debug Features

### For End Users:

1. **Open widget configuration** (long-press widget → Configure)
2. **Enter Tandoor URL and API key**
3. **Click "Test API"** to verify connection
4. **Review debug logs** in the scrollable text area
5. **Click "Save"** if test succeeds, and watch logs refresh

### For Developers:

1. **Enable ADB logging**:
   ```bash
   adb logcat -s TandoorWidget:D TandoorApiClient:D
   ```

2. **Look for these log patterns**:
   ```
   TandoorWidget: API Request: GET api/meal-plan/?from_date=...
   TandoorApiClient: --> GET https://...
   TandoorApiClient: <-- 200 OK
   TandoorWidget: Meal #1: Recipe - Parsed date: 2025-12-01
   TandoorWidget: ✓ Matched date '2025-12-01' to meal
   ```

## Key Debugging Information Provided

The enhanced logging helps identify:

1. **Date calculation issues**: See if the widget is calculating the correct week dates
2. **API URL construction**: Verify the correct endpoint and query parameters
3. **Authorization**: Confirm the API key is being sent (first 8 chars shown for security)
4. **Response parsing**: See the exact date format returned by API
5. **Date matching**: Identify which dates match and which don't
6. **API filtering**: Determine if the API is respecting the date range filters

## Expected Outcomes

After these changes:

1. ✅ Users can test API connection before saving configuration
2. ✅ All API interactions are visible in real-time
3. ✅ Date parsing and matching is fully transparent
4. ✅ Easier to identify if the issue is:
   - Network/connection problems
   - Authentication issues
   - Date format mismatches
   - API not filtering by date correctly
   - Widget date calculation errors

## Next Steps for Issue Resolution

Based on the debug output, users/developers can:

1. Verify if the API returns meals from the correct date range
2. Check if date parsing is working correctly
3. Confirm the widget is calculating dates properly
4. Identify if it's a Tandoor API configuration issue
5. Report detailed logs for further investigation

## Testing Recommendations

To verify these changes work correctly:

1. ✅ Build the app successfully (verify no syntax errors)
2. ⏳ Install on Android device/emulator
3. ⏳ Configure widget with Tandoor credentials
4. ⏳ Click "Test API" and verify logs appear
5. ⏳ Save configuration and verify widget updates
6. ⏳ Check if meals are displayed correctly or logs show the issue

## Potential Issues Identified

Based on the problem statement showing September meals when testing in December:

**Most Likely Issue**: The Tandoor API is not filtering meals by the `from_date` and `to_date` query parameters. The API returns all meals regardless of the date range requested.

**How to Verify**: Check the debug logs for:
- Request: `from_date=2025-12-01&to_date=2025-12-07`
- Response: Meals with dates from September (or other months)

**Solution**: This would need to be fixed on the Tandoor API side, or the widget needs to implement client-side filtering of the results.
