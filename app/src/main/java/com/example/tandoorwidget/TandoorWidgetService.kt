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
    private val flattenedMeals = mutableListOf<Triple<String, String, MealPlan?>>() // date, dayDisplay, meal
    private val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    private val TAG = "TandoorWidget"
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dayDisplayFormat = SimpleDateFormat("EEE dd/MM", Locale.US)
    private val MAX_RECIPE_NAME_LENGTH = 15

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
                flattenedMeals.add(Triple(date, dayDisplay, null))
            } else {
                // Show each meal as a separate row with the same day label
                meals.forEach { meal ->
                    flattenedMeals.add(Triple(date, dayDisplay, meal))
                }
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

                // Debug: Log each meal plan's date
                mealPlans?.forEachIndexed { index, meal ->
                    val rawDate = meal.from_date
                    val parsedDate = MealPlanUtils.safeParseDate(rawDate)
                    // Sanitize display name for logging (truncate and remove newlines)
                    val displayName = MealPlanUtils.getDisplayName(meal.recipe, meal.title).replace("\n", " ").take(50)
                    sendLogBroadcast("Meal #${index + 1}: '${displayName}' - Raw date: '$rawDate' -> Parsed: '$parsedDate'")
                }

                val mealPlansByDate = mealPlans?.groupBy { MealPlanUtils.safeParseDate(it.from_date) } ?: emptyMap()
                
                sendLogBroadcast("Meal plans by date map keys: ${mealPlansByDate.keys.joinToString(", ")}")
                
                dailyMeals.clear()
                dailyMeals.addAll(dates.map { date ->
                    val meals = mealPlansByDate[date] ?: emptyList()
                    if (meals.isNotEmpty()) {
                        val displayNames = meals.joinToString(", ") { 
                            MealPlanUtils.getDisplayName(it.recipe, it.title).replace("\n", " ").take(30) 
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
        val (date, dayDisplay, mealPlan) = flattenedMeals[position]

        remoteViews.setTextViewText(R.id.day_of_week, dayDisplay)

        if (mealPlan != null) {
            // Get display name using utility (handles null recipe)
            val displayName = MealPlanUtils.getDisplayName(mealPlan.recipe, mealPlan.title)
            val truncatedName = displayName.take(MAX_RECIPE_NAME_LENGTH).let { 
                if (displayName.length > MAX_RECIPE_NAME_LENGTH) "$it..." else it 
            }
            val mealText = "${mealPlan.meal_type_name}: $truncatedName"
            remoteViews.setTextViewText(R.id.meal, mealText)
            
            // Only set up click intent if recipe is not null
            val recipeId = MealPlanUtils.getRecipeUrl(mealPlan.recipe)
            if (recipeId != null) {
                val sharedPrefs = context.getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
                val tandoorUrl = sharedPrefs.getString("tandoor_url_$appWidgetId", "")
                
                // Validate URL before creating intent
                if (!tandoorUrl.isNullOrEmpty() && 
                    (tandoorUrl.startsWith("http://") || tandoorUrl.startsWith("https://"))) {
                    try {
                        val recipeUrl = "$tandoorUrl/recipe/$recipeId/"
                        val uri = android.net.Uri.parse(recipeUrl)
                        
                        // Ensure URI is valid
                        if (uri != null && uri.scheme != null) {
                            val fillInIntent = Intent(Intent.ACTION_VIEW)
                            fillInIntent.data = uri
                            remoteViews.setOnClickFillInIntent(R.id.meal, fillInIntent)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create recipe URL for meal ${mealPlan.id}", e)
                    }
                }
            }
        } else {
            remoteViews.setTextViewText(R.id.meal, "---")
        }
        
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
