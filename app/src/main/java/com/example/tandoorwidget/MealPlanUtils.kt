package com.example.tandoorwidget

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Utility object for safely handling meal plan data, especially when recipe is null (placeholder entries).
 */
object MealPlanUtils {
    private const val TAG = "MealPlanUtils"

    /**
     * Get the display name for a meal plan entry.
     * If recipe is null (placeholder entry), returns the title.
     * Otherwise returns the recipe name.
     *
     * @param recipe The recipe object (can be null for placeholders)
     * @param title The meal plan title (fallback when recipe is null)
     * @return The display name to show to the user
     */
    fun getDisplayName(recipe: Recipe?, title: String): String {
        return if (recipe != null) {
            recipe.name
        } else {
            Log.d(TAG, "Placeholder entry detected (recipe == null), using title: '$title'")
            title
        }
    }

    /**
     * Get the recipe URL for a meal plan entry.
     * Returns null if recipe is null (placeholder entry) or if recipe ID is invalid.
     *
     * @param recipe The recipe object (can be null for placeholders)
     * @return The recipe ID for URL construction, or null if not available
     */
    fun getRecipeUrl(recipe: Recipe?): Int? {
        return if (recipe != null && recipe.id > 0) {
            recipe.id
        } else {
            if (recipe == null) {
                Log.d(TAG, "Placeholder entry (recipe == null), no URL available")
            }
            null
        }
    }

    /**
     * Build the complete recipe URL for a meal plan entry.
     * Returns the base Tandoor URL if recipe is null or invalid.
     *
     * @param tandoorUrl The base Tandoor URL (e.g., "https://tandoor.example.com")
     * @param recipe The recipe object (can be null for placeholders)
     * @return The complete recipe URL with trailing slash, or base URL if recipe is invalid
     */
    fun buildRecipeUrl(tandoorUrl: String, recipe: Recipe?): String {
        val recipeId = getRecipeUrl(recipe)
        return if (recipeId != null) {
            // Remove any trailing slashes from base URL to avoid creating URLs like
            // "https://example.com//recipe/123/" or "https://example.com///recipe/123/"
            val baseUrl = tandoorUrl.trimEnd('/')
            "$baseUrl/recipe/$recipeId/"
        } else {
            Log.d(TAG, "No valid recipe ID, returning base URL")
            tandoorUrl
        }
    }

    /**
     * Safely parse a date string in ISO 8601 format.
     * Extracts the date portion (YYYY-MM-DD) from timestamps like "2025-12-01T22:24:35.522000+01:00"
     *
     * @param rawDate The raw date string from API
     * @return The parsed date string in YYYY-MM-DD format, or the original string if parsing fails
     */
    fun safeParseDate(rawDate: String): String {
        return try {
            if (rawDate.length >= 10) {
                rawDate.substring(0, 10)
            } else {
                Log.w(TAG, "Date string too short: '$rawDate'")
                rawDate
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse date: '$rawDate'", e)
            rawDate
        }
    }

    /**
     * Format a date string for display.
     * Converts "YYYY-MM-DD" to "EEE dd/MM" format.
     *
     * @param dateString Date in YYYY-MM-DD format
     * @param inputFormat SimpleDateFormat for parsing
     * @param outputFormat SimpleDateFormat for formatting
     * @return Formatted date string, or original string if parsing fails
     */
    fun formatDateForDisplay(
        dateString: String,
        inputFormat: SimpleDateFormat,
        outputFormat: SimpleDateFormat
    ): String {
        return try {
            val parsedDate = inputFormat.parse(dateString)
            if (parsedDate != null) {
                outputFormat.format(parsedDate)
            } else {
                Log.e(TAG, "Failed to parse date: $dateString")
                dateString
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception parsing date: $dateString", e)
            dateString
        }
    }
    
    /**
     * Check if a meal plan spans multiple days.
     * 
     * @param mealPlan The meal plan to check
     * @return True if the meal spans multiple days, false otherwise
     */
    fun isMultiDayMeal(mealPlan: MealPlan): Boolean {
        if (mealPlan.to_date == null) return false
        
        val fromDate = safeParseDate(mealPlan.from_date)
        val toDate = safeParseDate(mealPlan.to_date)
        
        return fromDate != toDate
    }
    
    /**
     * Get the number of days a meal spans.
     * 
     * @param mealPlan The meal plan to check
     * @param dateFormat SimpleDateFormat for parsing dates
     * @return Number of days the meal spans (minimum 1)
     */
    fun getMealSpanDays(mealPlan: MealPlan, dateFormat: SimpleDateFormat): Int {
        if (mealPlan.to_date == null) return 1
        
        return try {
            val fromDate = safeParseDate(mealPlan.from_date)
            val toDate = safeParseDate(mealPlan.to_date)
            
            val from = dateFormat.parse(fromDate)
            val to = dateFormat.parse(toDate)
            
            if (from != null && to != null) {
                val diff = to.time - from.time
                val days = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
                if (days > 0) days else 1
            } else {
                1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate meal span days", e)
            1
        }
    }
    
    /**
     * Check if a meal plan should be displayed on a specific date.
     * For multi-day meals, checks if the date falls within the range.
     * 
     * @param mealPlan The meal plan to check
     * @param date The date to check (in YYYY-MM-DD format)
     * @param dateFormat SimpleDateFormat for parsing dates
     * @return True if the meal should be displayed on this date
     */
    fun mealAppliesToDate(mealPlan: MealPlan, date: String, dateFormat: SimpleDateFormat): Boolean {
        val fromDate = safeParseDate(mealPlan.from_date)
        
        // If no to_date, only show on from_date
        if (mealPlan.to_date == null) {
            return fromDate == date
        }
        
        val toDate = safeParseDate(mealPlan.to_date)
        
        return try {
            val targetDate = dateFormat.parse(date)
            val from = dateFormat.parse(fromDate)
            val to = dateFormat.parse(toDate)
            
            if (targetDate != null && from != null && to != null) {
                targetDate >= from && targetDate <= to
            } else {
                // Fallback to simple string comparison
                date >= fromDate && date <= toDate
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if meal applies to date", e)
            // Fallback to simple string comparison
            date >= fromDate && date <= toDate
        }
    }
}
