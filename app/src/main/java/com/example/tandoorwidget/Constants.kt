package com.example.tandoorwidget

import android.content.Context
import android.content.SharedPreferences

object Constants {
    const val SHARED_PREFS_NAME = "TandoorWidgetPrefs"
    
    // Broadcast action constants
    const val ACTION_WIDGET_LOG = "com.example.tandoorwidget.ACTION_WIDGET_LOG"
    const val ACTION_WIDGET_ERROR = "com.example.tandoorwidget.ACTION_WIDGET_ERROR"
    const val ACTION_REFRESH_WIDGET = "com.example.tandoorwidget.ACTION_REFRESH_WIDGET"
    
    /**
     * Check if widget configuration is complete
     */
    fun isWidgetConfigured(context: Context, appWidgetId: Int): Boolean {
        val sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val tandoorUrl = sharedPrefs.getString("tandoor_url_$appWidgetId", null)
        val apiKey = sharedPrefs.getString("api_key_$appWidgetId", null)
        return !tandoorUrl.isNullOrBlank() && !apiKey.isNullOrBlank()
    }
    
    /**
     * Get widget configuration
     */
    fun getWidgetConfig(context: Context, appWidgetId: Int): Pair<String?, String?> {
        val sharedPrefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val tandoorUrl = sharedPrefs.getString("tandoor_url_$appWidgetId", null)
        val apiKey = sharedPrefs.getString("api_key_$appWidgetId", null)
        return Pair(tandoorUrl, apiKey)
    }
    
    /**
     * Build a configuration error message based on what's missing
     */
    fun buildConfigErrorMessage(hasUrl: Boolean, hasApiKey: Boolean): String {
        return when {
            !hasUrl && !hasApiKey -> "Missing configuration - No URL or API key configured"
            !hasUrl -> "Missing configuration - No Tandoor URL configured"
            !hasApiKey -> "Missing configuration - No API key configured"
            else -> "Configuration error"
        }
    }
}

enum class WidgetErrorType {
    MISSING_CONFIGURATION,
    API_ERROR,
    NETWORK_ERROR,
    PARSE_ERROR,
    UNKNOWN_ERROR
}
