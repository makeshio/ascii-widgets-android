package com.makeshio.asciiwidgets.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.makeshio.asciiwidgets.data.AppDatabase
import com.makeshio.asciiwidgets.data.Repository
import com.makeshio.asciiwidgets.data.WidgetKind
import com.makeshio.asciiwidgets.widget.UpdateScheduler
import com.makeshio.asciiwidgets.widget.WidgetConfigureActivity
import com.makeshio.asciiwidgets.widget.WidgetUpdater
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* результат читается через системные настройки виджета */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UpdateScheduler.schedule(this)
        val database = AppDatabase.get(this)
        lifecycleScope.launch { Repository.ensureSeeded(database) }

        setContent {
            AsciiTheme {
                MainScreen(
                    database = database,
                    onRequestLocation = {
                        locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    onRequestExactAlarm = { UpdateScheduler.requestExactAlarm(this) },
                    onAddWidget = { provider, templateId -> pinWidget(provider, templateId) },
                    onEditWidget = { appWidgetId -> openEditor(appWidgetId) },
                    onResetWidget = { appWidgetId ->
                        lifecycleScope.launch {
                            database.widgetConfigDao().delete(appWidgetId)
                            WidgetUpdater.forget(appWidgetId)
                            WidgetKind.entries.forEach { WidgetUpdater.updateAll(this@MainActivity, it) }
                        }
                    },
                )
            }
        }
    }

    private fun pinWidget(provider: Class<out AppWidgetProvider>, templateId: String) {
        val manager = AppWidgetManager.getInstance(this)
        if (!manager.isRequestPinAppWidgetSupported) {
            Toast.makeText(
                this,
                "Лаунчер не разрешает добавление из приложения. Добавьте виджет долгим нажатием по рабочему столу.",
                Toast.LENGTH_LONG,
            ).show()
            return
        }
        val extras = Bundle().apply { putString(EXTRA_TEMPLATE_ID, templateId) }
        val success = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        manager.requestPinAppWidget(ComponentName(this, provider), extras, success)
    }

    private fun openEditor(appWidgetId: Int) {
        startActivity(
            Intent(this, WidgetConfigureActivity::class.java)
                .putExtra(EXTRA_EDIT_WIDGET_ID, appWidgetId),
        )
    }

    companion object {
        const val EXTRA_TEMPLATE_ID = "com.makeshio.asciiwidgets.extra.TEMPLATE_ID"
        const val EXTRA_EDIT_WIDGET_ID = "com.makeshio.asciiwidgets.extra.EDIT_WIDGET_ID"
    }
}
