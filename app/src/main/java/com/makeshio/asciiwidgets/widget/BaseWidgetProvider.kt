package com.makeshio.asciiwidgets.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.makeshio.asciiwidgets.data.AppDatabase
import com.makeshio.asciiwidgets.data.WidgetKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

abstract class BaseWidgetProvider(private val kind: WidgetKind) : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        updateIds(context, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            AppDatabase.get(context).widgetConfigDao().deleteAll(appWidgetIds)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == UpdateScheduler.ACTION_TICK) {
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, javaClass))
            updateIds(context, ids)
            return
        }
        super.onReceive(context, intent)
    }

    private fun updateIds(context: Context, ids: IntArray) {
        if (ids.isEmpty()) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ids.forEach { WidgetUpdater.update(context, it, kind) }
            } catch (throwable: Throwable) {
                Log.e("AsciiWidgets", "Widget update failed", throwable)
            } finally {
                pending.finish()
            }
        }
    }
}

class ClockWidgetProvider : BaseWidgetProvider(WidgetKind.CLOCK)
class WeatherWidgetProvider : BaseWidgetProvider(WidgetKind.WEATHER)
class BatteryWidgetProvider : BaseWidgetProvider(WidgetKind.BATTERY)
class MusicWidgetProvider : BaseWidgetProvider(WidgetKind.MUSIC)
