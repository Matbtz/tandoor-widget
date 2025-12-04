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
        sendLogBroadcast(context, *appWidgetIds, message = "=== Widget onUpdate called ===")
        
        for (appWidgetId in appWidgetIds) {
            val isConfigured = Constants.isWidgetConfigured(context, appWidgetId)
            
            Log.d(TAG, "Widget $appWidgetId - Configured: $isConfigured")
            
            if (!isConfigured) {
                val (url, key) = Constants.getWidgetConfig(context, appWidgetId)
                Log.w(TAG, "Widget $appWidgetId - Missing configuration (URL: ${url != null}, Key: ${key != null})")
                sendLogBroadcast(context, appWidgetId, message = "Missing configuration - URL: ${url != null}, API Key: ${key != null}")
                
                // Show a helpful message instead of opening config activity
                updateConfigNeededView(context, appWidgetId)
            } else {
                val (url, key) = Constants.getWidgetConfig(context, appWidgetId)
                Log.d(TAG, "Widget $appWidgetId - Configuration found, updating widget")
                sendLogBroadcast(context, appWidgetId, message = "Configuration found - updating widget...")
                sendLogBroadcast(context, appWidgetId, message = "URL: $url")
                sendLogBroadcast(context, appWidgetId, message = "API Key: ***${key?.length ?: 0} chars***")
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
    
    private fun sendLogBroadcast(context: Context, vararg appWidgetIds: Int, message: String) {
        if (appWidgetIds.isEmpty()) {
            Log.d(TAG, message)
            return
        }
        
        for (appWidgetId in appWidgetIds) {
            Log.d(TAG, "Widget $appWidgetId: $message")
            val intent = Intent(Constants.ACTION_WIDGET_LOG)
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
        setupConfigClickIntent(context, views, appWidgetId)
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
        sendLogBroadcast(context, appWidgetId, message = "Showing 'Configuration needed' message")
    }
    
    private fun setupConfigClickIntent(context: Context, views: RemoteViews, appWidgetId: Int) {
        val configIntent = Intent(context, ConfigActivity::class.java)
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        val configPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            configIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, configPendingIntent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        Log.d(TAG, "onReceive called with action: $action")
        
        if (Constants.ACTION_WIDGET_ERROR == action) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val errorMessage = intent.getStringExtra("error_message")
            val errorType = intent.getStringExtra("error_type")
            Log.e(TAG, "Widget $appWidgetId error: $errorMessage (type: $errorType)")
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateErrorView(context, appWidgetId, errorMessage, errorType)
            }
        } else if (Constants.ACTION_REFRESH_WIDGET == action) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            Log.d(TAG, "Refresh requested for widget $appWidgetId")
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                sendLogBroadcast(context, appWidgetId, message = "Refresh button pressed - requesting data update...")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.calendar_view)
            }
        } else if (Constants.ACTION_WIDGET_LOG == action) {
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

    private fun updateErrorView(context: Context, appWidgetId: Int, errorMessage: String?, errorType: String? = null) {
        Log.e(TAG, "Showing error view for widget $appWidgetId: $errorMessage (type: $errorType)")
        sendLogBroadcast(context, appWidgetId, message = "ERROR: $errorMessage")
        
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.tandoor_widget)
        views.setViewVisibility(R.id.error_view, View.VISIBLE)
        
        // Provide context-specific error messages based on error type
        val displayMessage = when (errorType) {
            WidgetErrorType.MISSING_CONFIGURATION.name -> 
                "⚙️ Configuration needed\n\nTap title to configure"
            WidgetErrorType.API_ERROR.name -> 
                "API Error\n\n${errorMessage ?: "Unknown API error"}\n\nCheck URL and API key"
            WidgetErrorType.NETWORK_ERROR.name -> 
                "Connection Error\n\n${errorMessage ?: "Network unreachable"}\n\nCheck network and URL"
            WidgetErrorType.PARSE_ERROR.name ->
                "Parse Error\n\n${errorMessage ?: "Invalid response format"}\n\nCheck Tandoor version"
            else -> 
                "${errorMessage ?: "Unknown error"}\n\nCheck debug logs in config screen"
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
        sendLogBroadcast(context, appWidgetId, message = "Building widget RemoteViews...")
        
        try {
            val views = RemoteViews(context.packageName, R.layout.tandoor_widget)
            views.setViewVisibility(R.id.error_view, View.GONE)

            // Set up the refresh button
            val intent = Intent(context, TandoorWidgetProvider::class.java)
            intent.action = Constants.ACTION_REFRESH_WIDGET
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.refresh_button, pendingIntent)

            // Set up config click on title
            setupConfigClickIntent(context, views, appWidgetId)

            // Set up the remote adapter
            val serviceIntent = Intent(context, TandoorWidgetService::class.java)
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            views.setRemoteAdapter(R.id.calendar_view, serviceIntent)
            
            sendLogBroadcast(context, appWidgetId, message = "RemoteAdapter set to TandoorWidgetService")

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

            sendLogBroadcast(context, appWidgetId, message = "Updating AppWidget with RemoteViews...")
            appWidgetManager.updateAppWidget(appWidgetId, views)
            
            // Trigger initial data load
            sendLogBroadcast(context, appWidgetId, message = "Triggering data refresh (notifyAppWidgetViewDataChanged)...")
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.calendar_view)
            
            Log.d(TAG, "Widget $appWidgetId updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widget $appWidgetId", e)
            sendLogBroadcast(context, appWidgetId, message = "ERROR building widget: ${e.javaClass.simpleName} - ${e.message}")
            updateErrorView(context, appWidgetId, "Failed to build widget: ${e.message}")
        }
    }
}
