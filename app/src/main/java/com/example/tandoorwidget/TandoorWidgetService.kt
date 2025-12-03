package com.example.tandoorwidget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.view.View
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.appwidget.AppWidgetManager
import android.util.Log

class TandoorWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TandoorWidgetRemoteViewsFactory(this.applicationContext, intent)
    }
}

class TandoorWidgetRemoteViewsFactory(private val context: Context, private val intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    private val dailyMeals = mutableListOf<Pair<String, List<MealPlan>>>()
    // Changed to store grouped meals: date, dayDisplay, meal_type_name, and list of meals for that type
    private val flattenedMeals = mutableListOf<GroupedMeal>() 
    private val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    private val TAG = "TandoorWidget"
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dayDisplayFormat = SimpleDateFormat("EEE dd/MM", Locale.US)
    private val MAX_RECIPE_NAME_LENGTH = 15
    
    // Data class to hold grouped meal information
    data class GroupedMeal(
        val date: String,
        val dayDisplay: String,
        val mealTypeName: String?,
        val meals: List<MealPlan>
    )

    override fun onCreate() {
        // Initialize dates immediately so they show even before API call
        val calendar = Calendar.getInstance()
        // Find the start date (Saturday)
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            calendar.add(Calendar.DATE, -1)
        }
        
        val dates = (0..6).map {
            val date = sdf.format(calendar.time)
            calendar.add(Calendar.DATE, 1)
            date
        }
        
        // Initialize with empty meal lists
        dailyMeals.clear()
        dailyMeals.addAll(dates.map { date -> Pair(date, emptyList()) })
        
        // Build flattened structure for display
        updateFlattenedMeals()
    }
    
    private fun updateFlattenedMeals() {
        flattenedMeals.clear()
        for ((date, meals) in dailyMeals) {
            val dayDisplay = MealPlanUtils.formatDateForDisplay(date, sdf, dayDisplayFormat)
            
            if (meals.isEmpty()) {
                // Show day with no meals
                flattenedMeals.add(GroupedMeal(date, dayDisplay, null, emptyList()))
            } else {
                // Combine all meals for the day in one row
                // Sort by meal type: lunch first, then dinner
                val sortedMeals = meals.sortedBy { meal ->
                    when (meal.meal_type_name.lowercase()) {
                        "lunch" -> 0
                        "dinner" -> 1
                        else -> 2  // Other meal types come last
                    }
                }
                flattenedMeals.add(GroupedMeal(date, dayDisplay, null, sortedMeals))
            }
        }
    }

    override fun onDataSetChanged() {
        sendLogBroadcast("=== Starting data refresh ===")
        val sharedPrefs = context.getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val apiKey = sharedPrefs.getString("api_key_$appWidgetId", "") ?: ""
        val tandoorUrl = sharedPrefs.getString("tandoor_url_$appWidgetId", "") ?: ""

        if (tandoorUrl.isBlank() || apiKey.isBlank()) {
            sendLogBroadcast("Error: Missing URL or API Key")
            return
        }

        sendLogBroadcast("Base URL: $tandoorUrl")

        // Calculate dates first so we can show them even on error
        val calendar = Calendar.getInstance()
        sendLogBroadcast("Current date: ${sdf.format(calendar.time)}, Day: ${calendar.get(Calendar.DAY_OF_WEEK)}")

        // Find the start date (Saturday)
        // If today is Saturday, we want today. If today is Sunday, we want yesterday (Saturday).
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            calendar.add(Calendar.DATE, -1)
        }

        sendLogBroadcast("Week start (Saturday): ${sdf.format(calendar.time)}")

        val dates = (0..6).map {
            val date = sdf.format(calendar.time)
            calendar.add(Calendar.DATE, 1)
            date
        }

        sendLogBroadcast("Week dates: ${dates.joinToString(", ")}")

        try {
            val apiService = ApiClient.getApiService(tandoorUrl)
            val authorization = "Bearer $apiKey"

            val fromDate = dates.first()
            val toDate = dates.last()

            sendLogBroadcast("API Request: GET api/meal-plan/?from_date=$fromDate&to_date=$toDate")
            sendLogBroadcast("Authorization: Bearer ***${apiKey.length} characters***")

            val response = apiService.getMealPlan(authorization, fromDate, toDate).execute()
            
            sendLogBroadcast("Response code: ${response.code()}")
            sendLogBroadcast("Response message: ${response.message()}")
            
            if (response.isSuccessful) {
                val mealPlans = response.body()?.results
                val count = mealPlans?.size ?: 0
                sendLogBroadcast("Success: Received $count meal plans")

                // Debug: Log each meal plan's date and URL
                mealPlans?.forEachIndexed { index, meal ->
                    val rawFromDate = meal.from_date
                    val parsedFromDate = MealPlanUtils.safeParseDate(rawFromDate)
                    val parsedToDate = meal.to_date?.let { MealPlanUtils.safeParseDate(it) }
                    // Sanitize display name for logging (truncate and remove newlines)
                    val displayName = MealPlanUtils.getDisplayName(meal.recipe, meal.title).replace("\n", " ").take(50)
                    val recipeUrl = MealPlanUtils.buildRecipeUrl(tandoorUrl, meal.recipe)
                    val dateRange = if (parsedToDate != null && parsedToDate != parsedFromDate) {
                        "$parsedFromDate to $parsedToDate"
                    } else {
                        parsedFromDate
                    }
                    sendLogBroadcast("Meal #${index + 1}: '${displayName}' - Date: $dateRange - URL: '$recipeUrl'")
                }

                // Build a map of date -> meals
                // Multi-day meals appear only on their start date
                val mealPlansByDate = mutableMapOf<String, MutableList<MealPlan>>()
                mealPlans?.forEach { meal ->
                    val fromDate = MealPlanUtils.safeParseDate(meal.from_date)
                    // Only add meal if its start date is in our date range
                    if (dates.contains(fromDate)) {
                        mealPlansByDate.getOrPut(fromDate) { mutableListOf() }.add(meal)
                    }
                }
                
                sendLogBroadcast("Meal plans by date map keys: ${mealPlansByDate.keys.joinToString(", ")}")
                
                dailyMeals.clear()
                dailyMeals.addAll(dates.map { date ->
                    val meals = mealPlansByDate[date] ?: emptyList()
                    if (meals.isNotEmpty()) {
                        val displayNames = meals.joinToString(", ") { 
                            val name = MealPlanUtils.getDisplayName(it.recipe, it.title).replace("\n", " ").take(30)
                            if (MealPlanUtils.isMultiDayMeal(it)) "[$name]" else name
                        }
                        sendLogBroadcast("✓ Matched date '$date' to ${meals.size} meal(s): $displayNames")
                    } else {
                        sendLogBroadcast("✗ No match for date '$date'")
                    }
                    Pair(date, meals)
                })
                
                updateFlattenedMeals()
                sendLogBroadcast("=== Data refresh complete: ${dailyMeals.count { it.second.isNotEmpty() }} meals matched ===")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                sendErrorBroadcast("API Error ${response.code()}: $errorBody")
                // Still show dates even on error
                dailyMeals.clear()
                dailyMeals.addAll(dates.map { date -> Pair(date, emptyList()) })
                updateFlattenedMeals()
            }
        } catch (e: Exception) {
            sendErrorBroadcast("Exception during API call: ${e.javaClass.simpleName} - ${e.message}", e)
            e.printStackTrace()
            // Still show dates even on exception
            dailyMeals.clear()
            dailyMeals.addAll(dates.map { date -> Pair(date, emptyList()) })
            updateFlattenedMeals()
        }
    }

    private fun sendLogBroadcast(message: String) {
        Log.d(TAG, message)
        val intent = Intent("com.example.tandoorwidget.ACTION_WIDGET_LOG")
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        intent.putExtra("log_message", message)
        context.sendBroadcast(intent)
    }

    private fun sendErrorBroadcast(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
        val intent = Intent("com.example.tandoorwidget.ACTION_WIDGET_ERROR")
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        intent.putExtra("error_message", message)
        context.sendBroadcast(intent)
    }

    override fun onDestroy() {
        // Not needed for this implementation
    }

    override fun getCount(): Int {
        return flattenedMeals.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_day_item)
        val groupedMeal = flattenedMeals[position]

        remoteViews.setTextViewText(R.id.day_of_week, groupedMeal.dayDisplay)

        // Hide all recipe TextViews initially
        val recipeIds = listOf(R.id.recipe_1, R.id.recipe_2, R.id.recipe_3, R.id.recipe_4, R.id.recipe_5)
        recipeIds.forEach { id ->
            remoteViews.setViewVisibility(id, View.GONE)
        }

        if (groupedMeal.meals.isNotEmpty()) {
            val sharedPrefs = context.getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            val tandoorUrl = sharedPrefs.getString("tandoor_url_$appWidgetId", "")
            
            // Show each recipe in its own card (up to 5 recipes)
            groupedMeal.meals.take(5).forEachIndexed { index, meal ->
                val recipeId = recipeIds[index]
                val displayName = MealPlanUtils.getDisplayName(meal.recipe, meal.title)
                
                // For multi-day meals, add date range suffix
                val suffix = if (MealPlanUtils.isMultiDayMeal(meal)) {
                    val fromDate = MealPlanUtils.safeParseDate(meal.from_date)
                    val toDate = meal.to_date?.let { MealPlanUtils.safeParseDate(it) }
                    if (toDate != null) {
                        val span = MealPlanUtils.formatDateRangeSpan(fromDate, toDate, sdf)
                        if (span.isNotEmpty()) " ($span)" else ""
                    } else ""
                } else ""
                
                // Truncate the display name, then add suffix
                val truncatedDisplayName = displayName.take(MAX_RECIPE_NAME_LENGTH).let { 
                    if (displayName.length > MAX_RECIPE_NAME_LENGTH) "$it..." else it 
                }
                val truncatedName = truncatedDisplayName + suffix
                
                // Set text and make visible
                remoteViews.setTextViewText(recipeId, truncatedName)
                remoteViews.setViewVisibility(recipeId, View.VISIBLE)
                
                // Set up click intent to open edit activity for this meal
                try {
                    val fillInIntent = Intent()
                    fillInIntent.putExtra("meal_plan_id", meal.id)
                    fillInIntent.putExtra("meal_name", displayName)
                    fillInIntent.putExtra("from_date", MealPlanUtils.safeParseDate(meal.from_date))
                    fillInIntent.putExtra("to_date", meal.to_date?.let { MealPlanUtils.safeParseDate(it) })
                    remoteViews.setOnClickFillInIntent(recipeId, fillInIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create click intent for meal", e)
                }
            }
        }
        // Note: If no meals, all recipe views remain hidden (GONE), showing just the date
        
        return remoteViews
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}
