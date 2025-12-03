package com.example.tandoorwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.tandoorwidget.R
import android.view.View

class TandoorWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val sharedPrefs = context.getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            val tandoorUrl = sharedPrefs.getString("tandoor_url_$appWidgetId", null)
            if (tandoorUrl == null) {
                val intent = Intent(context, ConfigActivity::class.java)
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } else {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val sharedPrefs = context.getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                remove("tandoor_url_$appWidgetId")
                remove("api_key_$appWidgetId")
                apply()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if ("com.example.tandoorwidget.ACTION_WIDGET_ERROR" == intent.action) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val errorMessage = intent.getStringExtra("error_message")
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateErrorView(context, appWidgetId, errorMessage)
            }
        } else if ("com.example.tandoorwidget.ACTION_REFRESH_WIDGET" == intent.action) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateDebugView(context, appWidgetId, "Requesting refresh...")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.calendar_view)
            }
        } else if ("com.example.tandoorwidget.ACTION_WIDGET_LOG" == intent.action) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val logMessage = intent.getStringExtra("log_message")
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateDebugView(context, appWidgetId, logMessage)
            }
        }
    }

    private fun updateDebugView(context: Context, appWidgetId: Int, message: String?) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.tandoor_widget)
        views.setTextViewText(R.id.debug_view, message ?: "...")
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
    }

    private fun updateErrorView(context: Context, appWidgetId: Int, errorMessage: String?) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.tandoor_widget)
        views.setViewVisibility(R.id.error_view, View.VISIBLE)
        views.setTextViewText(R.id.error_view, errorMessage ?: "Failed to load data.")
        // Also update debug view
        views.setTextViewText(R.id.debug_view, "Error: $errorMessage")
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.tandoor_widget)
        views.setViewVisibility(R.id.error_view, View.GONE)

        // Set up the refresh button
        val intent = Intent(context, TandoorWidgetProvider::class.java)
        intent.action = "com.example.tandoorwidget.ACTION_REFRESH_WIDGET"
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.refresh_button, pendingIntent)

        // Set up config click on title
        val configIntent = Intent(context, ConfigActivity::class.java)
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        val configPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            configIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, configPendingIntent)

        // Set up the remote adapter
        val serviceIntent = Intent(context, TandoorWidgetService::class.java)
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        views.setRemoteAdapter(R.id.calendar_view, serviceIntent)

        // Set up PendingIntent template for clickable meal items
        // This template will be filled in by individual items in the RemoteViews
        val clickIntent = Intent(context, MealPlanEditActivity::class.java)
        clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        val clickPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.calendar_view, clickPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
        
        // Trigger initial data load
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.calendar_view)
    }
}
