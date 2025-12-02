# Debugging API Issues - Tandoor Widget

## Problem Description

The widget displays dates with "---" even though there are planned meals in Tandoor. This indicates that:
1. The widget is successfully loading and displaying date labels
2. The API call is being made (no connection errors)
3. The meal plans are not being matched to the displayed dates

## Root Cause Analysis

Based on the API response example provided:

```json
{
    "count": 31,
    "results": [
        {
            "from_date": "2025-09-17T00:00:00+02:00",
            "recipe": { "name": "Orzo crémeux..." },
            ...
        }
    ]
}
```

### Key Observations

1. **Date Format in API Response**: The API returns dates with full ISO 8601 timestamp including timezone offset
   - Format: `YYYY-MM-DDTHH:MM:SS+TZ:TZ`
   - Example: `2025-09-17T00:00:00+02:00`

2. **Date Parsing in Widget**: The widget extracts the date using `.substring(0, 10)`
   - This extracts: `2025-09-17` from `2025-09-17T00:00:00+02:00`
   - This should work correctly ✓

3. **Date Range Request**: The widget requests:
   - `from_date=2025-11-29` (example for current week starting Saturday)
   - `to_date=2025-12-05` (example for Friday)

### Potential Issues

#### Issue #1: API Not Filtering by Date Range
The API response example shows meals from September even though the current date is December. This suggests:
- The API might be returning ALL meal plans regardless of date filters
- The query parameters might not be reaching the API correctly
- The API might require a different date format

**How to verify**: Use the "Test API" button and check if:
- The request includes `?from_date=YYYY-MM-DD&to_date=YYYY-MM-DD`
- The response contains meals from the requested date range only
- If the response contains meals from other dates, the API filtering is not working

#### Issue #2: Date Matching Logic
The widget matches meals by date using:
```kotlin
val mealPlansByDate = mealPlans?.associateBy { it.from_date.substring(0, 10) }
```

This creates a map where:
- Key: `"2025-09-17"` (from the API response)
- Value: The meal plan object

Then matches against:
- `"2025-11-29"`, `"2025-11-30"`, etc. (current week dates)

If the API returns September dates but the widget is looking for December dates, there will be no matches.

## Debugging Steps

### Step 1: Use the Test API Feature

1. Open the widget configuration
2. Enter your Tandoor URL and API key
3. Click "Test API" button
4. Check the logs for:
   ```
   Request URL: https://your-tandoor.com/api/meal-plan/
   Query params: from_date=2025-12-01&to_date=2025-12-07
   Response code: 200
   Received X meal plans
   ```

### Step 2: Verify Date Range

Check if the meals returned are from the requested date range:
```
1. Recipe Name
   from_date: 2025-12-01T00:00:00+02:00
   Parsed date: 2025-12-01
```

If the parsed dates don't match the query dates, the API is not filtering correctly.

### Step 3: Check Android Logcat

If you have access to Android Studio or `adb logcat`, filter by:
- Tag: `TandoorWidget` - Widget logs
- Tag: `TandoorApiClient` - HTTP request/response logs

Look for the actual HTTP request being made and the full response body.

### Step 4: Verify Tandoor API Directly

Test the API using curl or a browser:

```bash
curl -H "Authorization: Bearer YOUR_API_KEY" \
  "https://your-tandoor.com/api/meal-plan/?from_date=2025-12-01&to_date=2025-12-07"
```

Check if:
1. The API returns only meals in the specified date range
2. The date format in the response matches expectations
3. The API is accessible and the token is valid

## Expected Behavior

When working correctly, you should see logs like:

```
=== Starting data refresh ===
Base URL: https://your-tandoor.com/
Current date: 2025-12-01, Day: 1
Week start (Saturday): 2025-11-29
Week dates: 2025-11-29, 2025-11-30, 2025-12-01, 2025-12-02, 2025-12-03, 2025-12-04, 2025-12-05
API Request: GET api/meal-plan/?from_date=2025-11-29&to_date=2025-12-05
Response code: 200
Success: Received 5 meal plans
Meal #1: 'Recipe Name' - Raw date: '2025-11-29T18:00:00+01:00' -> Parsed: '2025-11-29'
✓ Matched date '2025-11-29' to meal: Recipe Name
✓ Matched date '2025-12-01' to meal: Another Recipe
✗ No match for date '2025-11-30'
=== Data refresh complete: 2 meals matched ===
```

## Solutions

### If API is not filtering by date:

The issue is likely in how Retrofit is constructing the query parameters. The current implementation uses:
```kotlin
@GET("api/meal-plan/")
fun getMealPlan(
    @Header("Authorization") authorization: String,
    @Query("from_date") fromDate: String,
    @Query("to_date") toDate: String
): Call<MealPlanResponse>
```

This should produce: `api/meal-plan/?from_date=2025-12-01&to_date=2025-12-07`

**Verify**: Check the HTTP logging interceptor output for the actual URL being requested.

### If dates are not matching:

Check if the API returns dates in a different format or timezone. The widget assumes:
- API date format: `YYYY-MM-DDTHH:MM:SS+TZ:TZ`
- First 10 characters: `YYYY-MM-DD`

If your API returns dates in a different format, you may need to adjust the parsing logic.

## Additional Debug Information

The debug features added include:

1. **HTTP Logging Interceptor**: Logs all HTTP requests and responses to Logcat
2. **Broadcast Logs**: Sends log messages to the configuration activity in real-time
3. **Date Matching Debug**: Shows which dates matched and which didn't
4. **Test API Button**: Tests the connection without affecting the widget

All logs are also written to Android's Logcat with appropriate tags for filtering.
