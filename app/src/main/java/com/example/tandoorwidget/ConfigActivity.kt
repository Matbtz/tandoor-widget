package com.example.tandoorwidget

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.widget.Toast

class ConfigActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        val tandoorUrlEditText = findViewById<EditText>(R.id.tandoor_url)
        val apiKeyEditText = findViewById<EditText>(R.id.api_key)
        val saveButton = findViewById<Button>(R.id.save_button)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val sharedPrefs = getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)

        // Pre-fill existing values if present
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val existingUrl = sharedPrefs.getString("tandoor_url_$appWidgetId", "")
            val existingKey = sharedPrefs.getString("api_key_$appWidgetId", "")
            if (!existingUrl.isNullOrEmpty()) {
                tandoorUrlEditText.setText(existingUrl)
            }
            if (!existingKey.isNullOrEmpty()) {
                apiKeyEditText.setText(existingKey)
            }
        }

        saveButton.setOnClickListener {
            var tandoorUrl = tandoorUrlEditText.text.toString().trim()
            val apiKey = apiKeyEditText.text.toString().trim()

            if (!tandoorUrl.endsWith("/")) {
                tandoorUrl += "/"
            }

            if (tandoorUrl.isEmpty() || apiKey.isEmpty()) {
                Toast.makeText(this, "Please enter both a URL and an API key.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPrefs = getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)

            with(sharedPrefs.edit()) {
                putString("tandoor_url_$appWidgetId", tandoorUrl)
                putString("api_key_$appWidgetId", apiKey)
                apply()
            }

            val appWidgetManager = AppWidgetManager.getInstance(this)
            TandoorWidgetProvider().onUpdate(this, appWidgetManager, intArrayOf(appWidgetId))

            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}
