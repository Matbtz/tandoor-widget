package com.example.tandoorwidget

import android.content.Context
import android.content.SharedPreferences

object Constants {
    const val SHARED_PREFS_NAME = "TandoorWidgetPrefs"
    
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
}

enum class WidgetErrorType {
    MISSING_CONFIGURATION,
    API_ERROR,
    NETWORK_ERROR,
    PARSE_ERROR,
    UNKNOWN_ERROR
}
