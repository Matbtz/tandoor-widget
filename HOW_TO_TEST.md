# How to Test the API Debugging Features

## Overview

This guide explains how to use the new debugging features to diagnose why the widget shows "---" instead of meal names.

## Quick Start

### For Widget Users

1. **Add/Configure Widget**:
   - Long-press on home screen → Widgets → Find "Tandoor Widget"
   - Or long-press existing widget → Configure

2. **Enter Configuration**:
   - **Tandoor URL**: Your Tandoor instance URL (e.g., `https://recipes.example.com/`)
     - The app will automatically normalize the URL
     - Don't worry about trailing slashes or `/api/meal-plan/` paths
   - **API Key**: Your Tandoor API token from Settings → API Key

3. **Test Connection** (Recommended First):
   - Click "Test API" button
   - Watch the debug logs appear below
   - Look for:
     - `✓ SUCCESS: Received X meal plans` - API is working
     - `✗ ERROR: XXX` - Connection or authentication problem
   - Review the meal plans shown - check if dates match current week

4. **Save Configuration**:
   - Once test succeeds, click "Save"
   - Widget will refresh and logs will show the refresh process
   - Check if meals appear or if logs show matching issues

5. **Review Logs**:
   - Scroll through debug logs to see:
     - What date range is being requested
     - How many meals were returned
     - Which dates matched meals
     - Which dates show "---" and why
   - Click "Done" when finished

### For Developers

#### Using Android Studio

1. **Open Logcat**:
   ```
   View → Tool Windows → Logcat
   ```

2. **Filter Logs**:
   - Filter by tag: `TandoorWidget` or `TandoorApiClient`
   - Or use filter: `tag:TandoorWidget | tag:TandoorApiClient`

3. **Look for Key Log Lines**:
   ```
   TandoorWidget: === Starting data refresh ===
   TandoorWidget: Current date: 2025-12-01, Day: 1
   TandoorWidget: Week start (Saturday): 2025-11-29
   TandoorWidget: Week dates: 2025-11-29, 2025-11-30, ...
   TandoorWidget: API Request: GET api/meal-plan/?from_date=2025-11-29&to_date=2025-12-05
   TandoorApiClient: --> GET https://example.com/api/meal-plan/?from_date=2025-11-29...
   TandoorApiClient: <-- 200 OK
   TandoorWidget: Success: Received 3 meal plans
   TandoorWidget: Meal #1: 'Recipe Name' - Raw date: '2025-11-29T18:00:00+01:00' -> Parsed: '2025-11-29'
   TandoorWidget: ✓ Matched date '2025-11-29' to meal: Recipe Name
   TandoorWidget: ✗ No match for date '2025-11-30'
   TandoorWidget: === Data refresh complete: 1 meals matched ===
   ```

#### Using ADB Command Line

1. **Connect Device**:
   ```bash
   adb devices
   ```

2. **View Logs**:
   ```bash
   adb logcat -s TandoorWidget:D TandoorApiClient:D
   ```

3. **Save Logs to File**:
   ```bash
   adb logcat -s TandoorWidget:D TandoorApiClient:D > widget_debug.log
   ```

4. **Clear and Refresh**:
   ```bash
   adb logcat -c  # Clear logs
   # Then refresh widget
   adb logcat -s TandoorWidget:D TandoorApiClient:D
   ```

## Interpreting the Logs

### Successful Scenario

```
=== Starting data refresh ===
Base URL: https://recipes.example.com/
Current date: 2025-12-01, Day: 1
Week start (Saturday): 2025-11-29
Week dates: 2025-11-29, 2025-11-30, 2025-12-01, 2025-12-02, 2025-12-03, 2025-12-04, 2025-12-05
API Request: GET api/meal-plan/?from_date=2025-11-29&to_date=2025-12-05
Authorization: Token ***32 characters***
Response code: 200
Response message: OK
Success: Received 5 meal plans
Meal #1: 'Pasta Carbonara' - Raw date: '2025-11-29T18:00:00+01:00' -> Parsed: '2025-11-29'
Meal #2: 'Chicken Curry' - Raw date: '2025-12-01T12:00:00+01:00' -> Parsed: '2025-12-01'
✓ Matched date '2025-11-29' to meal: Pasta Carbonara
✗ No match for date '2025-11-30'
✓ Matched date '2025-12-01' to meal: Chicken Curry
=== Data refresh complete: 2 meals matched ===
```

**Interpretation**: 
- ✓ API connection working
- ✓ 5 meals returned
- ✓ Date parsing working correctly
- ✓ 2 out of 7 days have meals
- ℹ Days without meals show "---" (expected)

### Problem: API Returns Wrong Dates

```
=== Starting data refresh ===
...
Week dates: 2025-11-29, 2025-11-30, 2025-12-01, ...
API Request: GET api/meal-plan/?from_date=2025-11-29&to_date=2025-12-05
Response code: 200
Success: Received 31 meal plans
Meal #1: 'Old Recipe' - Raw date: '2025-09-17T00:00:00+02:00' -> Parsed: '2025-09-17'
Meal #2: 'Another Old Recipe' - Raw date: '2025-09-14T12:00:00+02:00' -> Parsed: '2025-09-14'
✗ No match for date '2025-11-29'
✗ No match for date '2025-11-30'
✗ No match for date '2025-12-01'
=== Data refresh complete: 0 meals matched ===
```

**Interpretation**:
- ✓ API connection working
- ✓ API returns data (31 meals)
- ✗ **PROBLEM**: API returns meals from September, not November/December
- ✗ **ROOT CAUSE**: Tandoor API not filtering by date range
- **Solution**: Need to check Tandoor API configuration or implement client-side filtering

### Problem: Authentication Failed

```
=== Starting data refresh ===
...
API Request: GET api/meal-plan/?from_date=2025-11-29&to_date=2025-12-05
Response code: 401
API Error 401: {"detail":"Invalid token."}
```

**Interpretation**:
- ✗ Authentication failed
- **Solution**: Check API key is correct in Settings → API Key in Tandoor

### Problem: Network Error

```
=== Starting data refresh ===
...
Exception during API call: UnknownHostException - Unable to resolve host "recipes.example.com"
```

**Interpretation**:
- ✗ Cannot reach server
- **Solution**: Check URL is correct and server is accessible

### Problem: Date Format Mismatch

```
=== Starting data refresh ===
...
Success: Received 5 meal plans
Meal #1: 'Recipe' - Raw date: '17/09/2025' -> Parsed: '17/09/2025'
✗ No match for date '2025-11-29'
```

**Interpretation**:
- ✗ API returns dates in unexpected format (DD/MM/YYYY instead of ISO 8601)
- **Solution**: Need to update date parsing logic

## Common Problems and Solutions

### Problem: All Days Show "---"

**Debug Steps**:
1. Check logs for "Success: Received X meal plans"
   - If X = 0: No meals planned in Tandoor for this week
   - If X > 0: Continue to step 2

2. Check "Meal #X" log entries
   - Look at "Raw date" and "Parsed" date
   - Compare parsed dates to "Week dates"

3. Check matching results
   - If all show "✗ No match": Date mismatch problem
   - Compare the dates carefully

**Solutions**:
- **If API returns wrong dates**: API filtering issue (see Tandoor API docs)
- **If no meals returned**: Plan meals in Tandoor for the current week (Saturday-Friday)
- **If date format wrong**: Report as a bug with logs

### Problem: Widget Won't Refresh

**Debug Steps**:
1. Check if logs appear at all
   - If no logs: Widget not refreshing, try removing and re-adding
   
2. Check for exceptions in logs
   - Look for "Exception during API call"

**Solutions**:
- Clear app data and reconfigure
- Check internet connection
- Verify Tandoor URL is accessible

### Problem: Some Days Missing

**This is Normal**:
- Days without planned meals show "---"
- Only days with meals in Tandoor will show meal names

**Verify**:
1. Check Tandoor web interface
2. Confirm meals are planned for missing days
3. Check if meal dates fall within the widget's week (Saturday-Friday)

## Testing Checklist

- [ ] Widget installs and appears on home screen
- [ ] Configuration screen opens
- [ ] Can enter Tandoor URL and API key
- [ ] "Test API" button works and shows logs
- [ ] Logs show correct week dates
- [ ] Logs show API request with correct URL
- [ ] Logs show successful response (200)
- [ ] Logs show meal plans with correct dates
- [ ] "Save" button updates widget
- [ ] Widget shows meals for planned days
- [ ] Widget shows "---" for days without meals
- [ ] "Done" button closes configuration
- [ ] "Clear Logs" button clears the log view

## Reporting Issues

When reporting issues, include:

1. **Full logs** from debug log viewer or Logcat
2. **Expected behavior**: What meals should appear
3. **Actual behavior**: What the widget shows
4. **Tandoor version** and API response example
5. **Date range** being requested vs. dates in meal plans

Example:
```
Expected: Show "Pasta Carbonara" for Saturday 2025-11-29
Actual: Shows "---" for all days

Logs show:
- Request: from_date=2025-11-29&to_date=2025-12-05
- Response: 31 meals, all from September 2025
- No dates match current week

API seems to ignore date filter parameters.
```

This detailed report helps identify the root cause quickly.
