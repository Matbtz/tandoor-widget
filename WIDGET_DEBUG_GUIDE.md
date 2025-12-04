# Widget Debugging Guide

This guide explains how to debug the Tandoor Widget when it's not loading data correctly.

## Widget Lifecycle Overview

Understanding the widget lifecycle helps diagnose issues:

1. **Widget Added to Home Screen**
   - Android calls `TandoorWidgetProvider.onUpdate()`
   - If no configuration exists, widget shows "Configuration needed" message
   - If configuration exists, widget proceeds to step 2

2. **Widget Configuration**
   - User opens ConfigActivity (by tapping widget title or adding new widget)
   - User enters Tandoor URL and API key
   - User taps "Save"
   - ConfigActivity saves to SharedPreferences
   - ConfigActivity calls `TandoorWidgetProvider.onUpdate()` to refresh widget

3. **Widget Update Process**
   - `TandoorWidgetProvider.onUpdate()`:
     - Checks if configuration exists in SharedPreferences
     - If missing: shows "Configuration needed" message
     - If present: calls `updateAppWidget()`
   - `updateAppWidget()`:
     - Creates RemoteViews for the widget UI
     - Sets up click handlers (refresh button, config button)
     - Sets up RemoteAdapter (TandoorWidgetService)
     - Calls `notifyAppWidgetViewDataChanged()` to trigger data load

4. **Data Loading Process**
   - `TandoorWidgetService.onGetViewFactory()` creates a RemoteViewsFactory
   - `RemoteViewsFactory.onCreate()` initializes the week date structure
   - `RemoteViewsFactory.onDataSetChanged()` is called:
     - Reads configuration from SharedPreferences
     - If missing: logs error and returns (widget shows empty dates)
     - If present: makes API call to Tandoor
     - Parses meal plans and maps them to dates
     - Updates internal data structures
   - `RemoteViewsFactory.getViewAt()` is called for each visible row:
     - Creates RemoteViews for each day's meal cards

5. **Error Handling**
   - Configuration errors: Show "Configuration needed" message
   - Network errors: Broadcast error to widget, show error message
   - Parse errors: Broadcast error to widget, show error message

## Using the Debug Log

The app includes a comprehensive debug log that shows what's happening at each step.

### Enabling Debug Logs

Debug logs are always enabled and visible in the ConfigActivity:

1. Open the ConfigActivity:
   - Long-press the widget → Configure
   - Or tap the widget title
2. Scroll down to see the "Debug Logs" section
3. Logs appear automatically as the widget processes events

### What to Look For in Logs

When you tap "Save", you should see:

```
=== SAVING CONFIGURATION ===
Widget ID: 123
Tandoor URL: https://your-tandoor-url.com/
API Key: ***32 characters***

Configuration saved to SharedPreferences
Triggering widget update...

=== Widget onUpdate called ===
Widget 123 - Configuration found, updating widget...
URL: https://your-tandoor-url.com/
API Key: ***32 chars***
Building widget RemoteViews...
RemoteAdapter set to TandoorWidgetService
Updating AppWidget with RemoteViews...
Triggering data refresh (notifyAppWidgetViewDataChanged)...

RemoteViewsFactory created for widget 123
Initialized week view: 2025-12-07 to 2025-12-13

=== Starting data refresh for widget 123 ===
Configuration loaded:
  Base URL: https://your-tandoor-url.com/
  API Key: ***32 characters***
Current date: 2025-12-10, Day: 3
Week start (Saturday): 2025-12-07
Week dates: 2025-12-07, 2025-12-08, 2025-12-09, 2025-12-10, 2025-12-11, 2025-12-12, 2025-12-13
API Request: GET api/meal-plan/?from_date=2025-12-07&to_date=2025-12-13
Authorization: Bearer ***32 characters***
Response code: 200
Response message: OK
Success: Received 5 meal plans
Meal #1: 'Spaghetti Bolognese' - Date: 2025-12-07 - URL: 'https://your-tandoor-url.com/recipe/42/'
...
✓ Matched date '2025-12-07' to 1 meal(s): Spaghetti Bolognese
✗ No match for date '2025-12-08'
...
=== Data refresh complete: 5 meals matched ===
```

### Common Issues and Log Patterns

#### Issue: Widget shows "Configuration needed"

**Log pattern:**
```
Widget 123 - Missing configuration (URL: false, Key: false)
Missing configuration - URL: false, API Key: false
```

**Solution:** Open ConfigActivity and save URL and API key.

---

#### Issue: Widget shows empty dates (no meals)

**Log pattern:**
```
Response code: 200
Success: Received 0 meal plans
```

**Possible causes:**
- No meals planned in Tandoor for the current week (Sat-Fri)
- Date range issue in Tandoor
- API filtering not working correctly

**Solution:** Check Tandoor web interface to verify meals are planned for the correct dates.

---

#### Issue: Widget shows API error

**Log pattern:**
```
Response code: 401
API Error 401: {"detail": "Invalid token"}
```

**Possible causes:**
- Incorrect API key
- Expired API token
- Wrong API key format

**Solution:** Generate a new API key in Tandoor and update in ConfigActivity.

---

#### Issue: Widget shows connection error

**Log pattern:**
```
Exception during API call: UnknownHostException - Unable to resolve host
```

**Possible causes:**
- Incorrect URL
- Network connectivity issue
- Tandoor server is down
- SSL/TLS certificate issue

**Solution:** 
- Verify URL is correct (should end with `/`)
- Check network connectivity
- Test URL in browser
- Use "Test API" button to diagnose

---

#### Issue: Meals not appearing on correct dates

**Log pattern:**
```
Meal #1: 'Recipe Name' - Date: 2025-12-01 - URL: '...'
✗ No match for date '2025-12-01'
```

**Possible causes:**
- Meal date is outside the current week (Sat-Fri)
- Date parsing issue
- Time zone mismatch

**Solution:** Check the parsed dates in the logs match your expected dates.

---

## Manual Testing Checklist

Use this checklist to test the widget:

### Fresh Install Test
- [ ] Uninstall app completely
- [ ] Install new APK
- [ ] Add widget to home screen
- [ ] Widget shows "Configuration needed" message
- [ ] Tap widget title to open ConfigActivity
- [ ] Enter URL and API key
- [ ] Tap "Save"
- [ ] Check debug logs show configuration saved
- [ ] Check debug logs show widget update process
- [ ] Check debug logs show API call success
- [ ] Widget shows meals on home screen

### Reconfiguration Test
- [ ] Open ConfigActivity from existing widget
- [ ] Verify existing URL and key are pre-filled
- [ ] Modify URL or key
- [ ] Tap "Save"
- [ ] Check debug logs show new configuration
- [ ] Widget updates with new data

### Refresh Test
- [ ] Tap refresh button on widget
- [ ] Check debug logs show "Refresh button pressed"
- [ ] Check debug logs show data refresh
- [ ] Widget updates with latest data

### Error Handling Test
- [ ] Remove API key from configuration
- [ ] Tap "Save"
- [ ] Widget shows "Configuration needed" message
- [ ] Enter invalid API key
- [ ] Tap "Save"
- [ ] Widget shows API error with details
- [ ] Enter invalid URL
- [ ] Tap "Save"
- [ ] Widget shows connection error

## Troubleshooting Tips

1. **Always check debug logs first** - They show exactly what's happening at each step

2. **Use "Test API" button** - Tests the same code path as the widget but with immediate feedback

3. **Compare Test API logs with widget logs** - If Test API works but widget doesn't, the issue is in the widget update mechanism

4. **Check SharedPreferences** - Use adb or Android Studio to verify configuration is actually saved:
   ```bash
   adb shell run-as com.example.tandoorwidget cat /data/data/com.example.tandoorwidget/shared_prefs/TandoorWidgetPrefs.xml
   ```

5. **Check Android logcat** - More detailed logs are available via `adb logcat`:
   ```bash
   adb logcat -s TandoorWidget TandoorWidgetProvider TandoorApiClient
   ```

6. **Force widget update** - Remove and re-add widget to force a fresh update cycle

7. **Clear app data** - If all else fails, clear app data and start fresh

## Expected Behavior Summary

✅ **With valid configuration:**
- Widget loads and displays meals
- Refresh button updates data
- Tapping meals opens action dialog
- Debug logs show successful API calls

✅ **Without configuration:**
- Widget shows "Configuration needed" message
- Tapping title opens ConfigActivity
- Debug logs show "Missing configuration"

✅ **With invalid configuration:**
- Widget shows specific error message (API error, connection error)
- Debug logs show detailed error information
- User can fix configuration and retry

## Getting Help

If you're still having issues:

1. Copy debug logs using "Copy Logs" button in ConfigActivity
2. Check Android logcat for additional details
3. Create a GitHub issue with:
   - Debug logs from ConfigActivity
   - Android logcat output
   - Screenshots of widget and ConfigActivity
   - Your Tandoor version and URL structure
