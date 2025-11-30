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
    private val dailyMeals = mutableListOf<Pair<String, MealPlan?>>()
    private val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    private val TAG = "TandoorWidget"
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.US)

    override fun onCreate() {
        // Not needed for this implementation
    }

    override fun onDataSetChanged() {
        val sharedPrefs = context.getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val apiKey = sharedPrefs.getString("api_key_$appWidgetId", "") ?: ""
        val tandoorUrl = sharedPrefs.getString("tandoor_url_$appWidgetId", "") ?: ""
        val apiService = ApiClient.getApiService(tandoorUrl)
        val authorization = "Token $apiKey"

        val calendar = Calendar.getInstance()
        
        // Find the last Saturday
        calendar.add(Calendar.DAY_OF_WEEK, -(calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SATURDAY))
        
        val dates = (0..6).map {
            val date = sdf.format(calendar.time)
            calendar.add(Calendar.DAY_OF_WEEK, 1)
            date
        }
        
        val fromDate = dates.first()
        val toDate = dates.last()

        try {
            val response = apiService.getMealPlan(authorization, fromDate, toDate).execute()
            if (response.isSuccessful) {
                val mealPlansByDate = response.body()?.results?.associateBy { it.from_date.substring(0, 10) } ?: emptyMap()
                dailyMeals.clear()
                dailyMeals.addAll(dates.map { date ->
                    Pair(date, mealPlansByDate[date])
                })
            } else {
                sendErrorBroadcast("API call failed with response code: ${response.code()}")
            }
        } catch (e: Exception) {
            sendErrorBroadcast("Exception during API call", e)
        }
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
        return dailyMeals.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_day_item)
        val (date, mealPlan) = dailyMeals[position]
        
        val calendar = Calendar.getInstance()
        calendar.time = sdf.parse(date)
        val dayOfWeek = dayOfWeekFormat.format(calendar.time).toUpperCase(Locale.US)

        remoteViews.setTextViewText(R.id.day_of_week, dayOfWeek)
        remoteViews.setTextViewText(R.id.meal, mealPlan?.recipe?.name ?: "---")
        
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
