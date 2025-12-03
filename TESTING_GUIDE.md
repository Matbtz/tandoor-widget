# Testing Guide for Widget Improvements

## Overview
This document provides guidance for testing the widget improvements that were implemented.

## Changes Summary

### 1. Calendar Icon Removed
**What changed**: The 📅 emoji prefix has been removed from multi-day meal cards.

**How to test**:
- Create a multi-day meal in Tandoor (e.g., meal from Saturday to Monday)
- Add the widget to your home screen
- Verify the meal card does NOT show a 📅 emoji prefix
- The meal should display like: "Recipe Name (Sat-Mon)" instead of "📅 Recipe Name"

### 2. Multi-Day Meal Consolidation
**What changed**: Multi-day meals now appear only once on their start date with a date range indicator.

**How to test**:
- Create a multi-day meal spanning Saturday to Monday
- Add the widget to your home screen
- **Saturday**: Should show "Recipe Name (Sat-Mon)" ✓
- **Sunday**: Should NOT show the meal ✓
- **Monday**: Should NOT show the meal ✓
- The meal appears only on the first day with a date range suffix

### 3. Combined Lunch and Dinner Rows
**What changed**: Lunch and dinner meals are now combined on the same row for each day.

**How to test**:
- Create both lunch and dinner meals for the same day (e.g., Saturday)
  - Lunch: "Salad"
  - Dinner: "Pasta"
- Add the widget to your home screen
- **Expected**: One row for Saturday showing both "Salad" and "Pasta"
- **Meal order**: Lunch meals should appear before dinner meals
- **Before**: Would have shown two separate rows (one for lunch, one for dinner)
- **After**: Shows one row with both meals

## Test Scenarios

### Scenario 1: Single Day Meals
- **Setup**: Add a lunch meal for Saturday only
- **Expected**: Shows "Meal Name" without any prefix or suffix
- **Verify**: No calendar emoji, no date range

### Scenario 2: Multi-Day Single Meal
- **Setup**: Add a lunch meal from Saturday to Monday
- **Expected**: 
  - Saturday shows "Meal Name (Sat-Mon)"
  - Sunday shows no meals
  - Monday shows no meals

### Scenario 3: Multiple Meals Same Day
- **Setup**: 
  - Saturday lunch: "Salad"
  - Saturday dinner: "Steak"
- **Expected**: 
  - One row for Saturday
  - Shows "Salad" then "Steak"
  - Lunch appears before dinner

### Scenario 4: Mixed Single and Multi-Day
- **Setup**:
  - Saturday lunch: "Salad" (single day)
  - Saturday-Monday dinner: "Pasta" (multi-day)
  - Sunday lunch: "Sandwich" (single day)
- **Expected**:
  - Saturday: Shows "Salad" and "Pasta (Sat-Mon)" on one row
  - Sunday: Shows "Sandwich" only (not the pasta)
  - Monday: No meals (not the pasta)

### Scenario 5: Empty Days
- **Setup**: No meals for a particular day
- **Expected**: Day header shows with no meal cards

### Scenario 6: Click Functionality
- **Setup**: Add any meal
- **Test**: Click on the meal card
- **Expected**: Opens the meal edit activity with correct meal details

## Unit Test Coverage

The following unit tests were added to verify the date range formatting:

1. ✅ `formatDateRangeSpan_withValidDates_returnsFormattedRange`
   - Tests formatting "2025-12-07" to "2025-12-09" returns "Sat-Mon"

2. ✅ `formatDateRangeSpan_withSameDayRange_returnsFormattedRange`
   - Tests formatting same day returns "Sat-Sat"

3. ✅ `formatDateRangeSpan_withInvalidFromDate_returnsEmptyString`
   - Tests error handling for invalid start date

4. ✅ `formatDateRangeSpan_withInvalidToDate_returnsEmptyString`
   - Tests error handling for invalid end date

## Build and Run

To build and test the app:

```bash
# Build the app
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Install on device
./gradlew installDebug
```

## Regression Testing

Verify these existing features still work:

1. ✅ Widget displays week from Saturday to Friday
2. ✅ Widget refreshes when manually triggered
3. ✅ Widget shows correct meal type names
4. ✅ Widget handles API errors gracefully
5. ✅ Widget handles missing recipe data (placeholders)
6. ✅ Widget truncates long recipe names correctly
7. ✅ Widget click intents open edit activity

## Notes

- The widget layout supports up to 5 recipe cards per day
- If a day has more than 5 meals (lunch + dinner combined), only the first 5 will be displayed
- Date range format uses abbreviated day names (Mon, Tue, Wed, etc.)
- All logging for debugging multi-day meals has been preserved

