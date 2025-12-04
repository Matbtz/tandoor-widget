package com.example.tandoorwidget

object Constants {
    const val SHARED_PREFS_NAME = "TandoorWidgetPrefs"
}

enum class WidgetErrorType {
    MISSING_CONFIGURATION,
    API_ERROR,
    NETWORK_ERROR,
    PARSE_ERROR,
    UNKNOWN_ERROR
}
