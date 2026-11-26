package com.makeshio.asciiwidgets.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Callback для requestPinAppWidget: лаунчер сам не открывает configure activity,
 * поэтому после закрепления виджета открываем её сами с полученным appWidgetId.
 */
class WidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val configure = Intent(context, WidgetConfigureActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .putExtra(
                com.makeshio.asciiwidgets.ui.MainActivity.EXTRA_TEMPLATE_ID,
                intent.getStringExtra(com.makeshio.asciiwidgets.ui.MainActivity.EXTRA_TEMPLATE_ID),
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(configure)
    }
}
