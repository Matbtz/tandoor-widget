# Multi-Day Meal Plan Feature Guide

## Overview

This guide explains the new multi-day meal plan feature that allows recipes to span multiple days in the Tandoor widget.

## Features

### 1. Multi-Day Recipe Display

Recipes can now span multiple days in your meal plan. When a recipe has both a `from_date` and a `to_date`, it will appear on all days within that range.

**Visual Indicator**: Multi-day recipes are marked with a 📅 calendar emoji prefix to distinguish them from single-day recipes.

### 2. Moving Recipes Between Days

You can move a recipe from one day to another by:

1. **Tap on any recipe card** in the widget
2. A dialog will open showing the meal details
3. Select the new date from the "Move To Date" dropdown
4. Click "Save Changes"

The widget will automatically refresh to show the recipe on its new date.

### 3. Extending Recipes Across Multiple Days

You can extend a recipe to span multiple days:

1. **Tap on the recipe card** you want to extend
2. In the dialog, select an end date from the "Extend To Date" dropdown
3. To make a multi-day meal single-day again, select "None (single day)"
4. Click "Save Changes"

The recipe will now appear on all days between the start and end dates with the 📅 indicator.

## API Changes

### Updated Data Model

The `MealPlan` data class now includes an optional `to_date` field:

```kotlin
data class MealPlan(
    val id: Int,
    val title: String,
    val recipe: Recipe?,
    val from_date: String,
    val to_date: String? = null,  // New field for multi-day meals
    val meal_type: MealType,
    val meal_type_name: String
)
```

### New API Endpoint

The app now supports updating meal plans via the Tandoor API:

```kotlin
@PATCH("api/meal-plan/{id}/")
fun updateMealPlan(
    @Header("Authorization") authorization: String,
    @Path("id") mealPlanId: Int,
    @Body updates: MealPlanUpdate
): Call<MealPlan>
```

The `MealPlanUpdate` data class allows partial updates:

```kotlin
data class MealPlanUpdate(
    val from_date: String? = null,
    val to_date: String? = null
)
```

## How It Works

### Widget Display Logic

The widget now uses the `MealPlanUtils.mealAppliesToDate()` function to determine which meals should be displayed on each date. This function:

1. For single-day meals (no `to_date`): Only shows on the `from_date`
2. For multi-day meals: Shows on all dates where `from_date <= date <= to_date`

### Edit Activity

When you tap a recipe card, the `MealPlanEditActivity` opens with:

- **Meal Name**: Shows which recipe you're editing
- **Move To Date**: A date picker with all days in the current week (Saturday to Friday)
- **Extend To Date**: An optional end date to create multi-day meals
- **Validation**: Ensures the end date is not before the start date

When you save changes:

1. The activity sends a PATCH request to the Tandoor API
2. On success, it triggers a widget refresh
3. The widget reloads data and updates the display
4. You'll see a success or error message

## Technical Details

### Utility Functions

The `MealPlanUtils` object includes several new helper functions:

- `isMultiDayMeal(mealPlan)`: Returns true if the meal spans multiple days
- `getMealSpanDays(mealPlan, dateFormat)`: Returns the number of days the meal spans
- `mealAppliesToDate(mealPlan, date, dateFormat)`: Returns true if the meal should be shown on the given date

### Testing

Comprehensive unit tests have been added for all multi-day meal functionality in `MealPlanUtilsTest.kt`:

- Tests for single-day meals
- Tests for multi-day meals
- Tests for date range calculations
- Tests for edge cases (null dates, same start/end dates)

## Android Widget Limitations

Due to Android widget RemoteViews limitations, advanced interactions like native drag-and-drop are not possible directly in the widget. Instead, we use the standard Android pattern of opening a companion Activity for complex operations. This is the recommended approach for widget interactions that go beyond simple clicks.

## Troubleshooting

### Recipe doesn't appear on multiple days

- Check that the `to_date` field is set in the Tandoor API response
- Verify the date range is within the current week (Saturday to Friday)
- Try refreshing the widget using the refresh button

### Edit dialog doesn't open when tapping a recipe

- Ensure the widget is properly configured with valid API credentials
- Check that the meal plan has a valid ID
- Look for error messages in the Android logcat

### Changes don't save

- Verify your Tandoor API key has write permissions
- Check your internet connection
- Look at the error message displayed in the toast notification
- Check the Tandoor server logs for API errors

## Future Enhancements

Possible future improvements:

- Visual spanning indicators (card stretches across days)
- Batch move operations
- Copy/duplicate meals to multiple days
- Custom date range filters
- Undo/redo functionality
