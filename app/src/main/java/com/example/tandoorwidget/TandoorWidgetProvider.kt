package com.example.tandoorwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.tandoorwidget.R
import android.view.View
import android.util.Log

class TandoorWidgetProvider : AppWidgetProvider() {
    private val TAG = "TandoorWidgetProvider"

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called with ${appWidgetIds.size} widget(s): ${appWidgetIds.joinToString()}")
        sendLogBroadcast(context, appWidgetIds, "=== Widget onUpdate called ===")
        
        for (appWidgetId in appWidgetIds) {
            val sharedPrefs = context.getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            val tandoorUrl = sharedPrefs.getString("tandoor_url_$appWidgetId", null)
            val apiKey = sharedPrefs.getString("api_key_$appWidgetId", null)
            
            Log.d(TAG, "Widget $appWidgetId - URL configured: ${tandoorUrl != null}, API key configured: ${apiKey != null}")
            
            if (tandoorUrl == null || apiKey == null) {
                Log.w(TAG, "Widget $appWidgetId - Missing configuration (URL: ${tandoorUrl != null}, Key: ${apiKey != null})")
                sendLogBroadcast(context, appWidgetId, "Missing configuration - URL: ${tandoorUrl != null}, API Key: ${apiKey != null}")
                
                // Show a helpful message instead of opening config activity
                updateConfigNeededView(context, appWidgetId)
            } else {
                Log.d(TAG, "Widget $appWidgetId - Configuration found, updating widget")
                sendLogBroadcast(context, appWidgetId, "Configuration found - updating widget...")
                sendLogBroadcast(context, appWidgetId, "URL: $tandoorUrl")
                sendLogBroadcast(context, appWidgetId, "API Key: ***${apiKey.length} chars***")
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Log.d(TAG, "onDeleted called for widgets: ${appWidgetIds.joinToString()}")
        for (appWidgetId in appWidgetIds) {
            val sharedPrefs = context.getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                remove("tandoor_url_$appWidgetId")
                remove("api_key_$appWidgetId")
                apply()
            }
        }
    }
    
    private fun sendLogBroadcast(context: Context, appWidgetId: Int, message: String) {
        Log.d(TAG, "Widget $appWidgetId: $message")
        val intent = Intent("com.example.tandoorwidget.ACTION_WIDGET_LOG")
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        intent.putExtra("log_message", message)
        context.sendBroadcast(intent)
    }
    
    private fun sendLogBroadcast(context: Context, appWidgetIds: IntArray, message: String) {
        Log.d(TAG, message)
        for (appWidgetId in appWidgetIds) {
            val intent = Intent("com.example.tandoorwidget.ACTION_WIDGET_LOG")
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            intent.putExtra("log_message", message)
            context.sendBroadcast(intent)
        }
    }
    
    private fun updateConfigNeededView(context: Context, appWidgetId: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.tandoor_widget)
        views.setViewVisibility(R.id.error_view, View.VISIBLE)
        views.setTextViewText(R.id.error_view, "⚙️ Configuration needed\n\nTap title to configure Tandoor URL and API key")
        
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
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
        sendLogBroadcast(context, appWidgetId, "Showing 'Configuration needed' message")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        Log.d(TAG, "onReceive called with action: $action")
        
        if ("com.example.tandoorwidget.ACTION_WIDGET_ERROR" == action) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val errorMessage = intent.getStringExtra("error_message")
            Log.e(TAG, "Widget $appWidgetId error: $errorMessage")
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateErrorView(context, appWidgetId, errorMessage)
            }
        } else if ("com.example.tandoorwidget.ACTION_REFRESH_WIDGET" == action) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            Log.d(TAG, "Refresh requested for widget $appWidgetId")
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                sendLogBroadcast(context, appWidgetId, "Refresh button pressed - requesting data update...")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.calendar_view)
            }
        } else if ("com.example.tandoorwidget.ACTION_WIDGET_LOG" == action) {
            // Just pass through log messages - they're handled by ConfigActivity
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val logMessage = intent.getStringExtra("log_message")
            Log.d(TAG, "Widget $appWidgetId log: $logMessage")
        }
    }

    // Kept for potential future debugging needs, though currently disabled from widget UI
    // The debug_view TextView is set to visibility="gone" in the layout
    private fun updateDebugView(context: Context, appWidgetId: Int, message: String?) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.tandoor_widget)
        views.setTextViewText(R.id.debug_view, message ?: "...")
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
    }

    private fun updateErrorView(context: Context, appWidgetId: Int, errorMessage: String?) {
        Log.e(TAG, "Showing error view for widget $appWidgetId: $errorMessage")
        sendLogBroadcast(context, appWidgetId, "ERROR: $errorMessage")
        
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.tandoor_widget)
        views.setViewVisibility(R.id.error_view, View.VISIBLE)
        
        // Provide context-specific error messages
        val displayMessage = when {
            errorMessage == null -> "Failed to load data.\n\nCheck debug logs in config screen."
            errorMessage.contains("Missing configuration", ignoreCase = true) -> 
                "⚙️ Configuration needed\n\nTap title to configure"
            errorMessage.contains("API Error", ignoreCase = true) -> 
                "API Error\n\n$errorMessage\n\nCheck URL and API key"
            errorMessage.contains("Exception", ignoreCase = true) -> 
                "Connection Error\n\n$errorMessage\n\nCheck network and URL"
            else -> "$errorMessage\n\nCheck debug logs"
        }
        
        views.setTextViewText(R.id.error_view, displayMessage)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        Log.d(TAG, "updateAppWidget called for widget $appWidgetId")
        sendLogBroadcast(context, appWidgetId, "Building widget RemoteViews...")
        
        try {
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
            
            sendLogBroadcast(context, appWidgetId, "RemoteAdapter set to TandoorWidgetService")

            // Set up PendingIntent template for clickable meal items
            // This template will be filled in by individual items in the RemoteViews
            val clickIntent = Intent(context, RecipeActionActivity::class.java)
            clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val clickPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.calendar_view, clickPendingIntent)

            sendLogBroadcast(context, appWidgetId, "Updating AppWidget with RemoteViews...")
            appWidgetManager.updateAppWidget(appWidgetId, views)
            
            // Trigger initial data load
            sendLogBroadcast(context, appWidgetId, "Triggering data refresh (notifyAppWidgetViewDataChanged)...")
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.calendar_view)
            
            Log.d(TAG, "Widget $appWidgetId updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widget $appWidgetId", e)
            sendLogBroadcast(context, appWidgetId, "ERROR building widget: ${e.javaClass.simpleName} - ${e.message}")
            updateErrorView(context, appWidgetId, "Failed to build widget: ${e.message}")
        }
    }
}
