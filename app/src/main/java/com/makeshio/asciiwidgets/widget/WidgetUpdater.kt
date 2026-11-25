package com.makeshio.asciiwidgets.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.makeshio.asciiwidgets.R
import com.makeshio.asciiwidgets.data.AppDatabase
import com.makeshio.asciiwidgets.data.Repository
import com.makeshio.asciiwidgets.data.SettingsEntity
import com.makeshio.asciiwidgets.data.WidgetKind
import com.makeshio.asciiwidgets.render.AsciiGrid
import com.makeshio.asciiwidgets.render.BitmapRenderer
import com.makeshio.asciiwidgets.sources.WidgetData
import com.makeshio.asciiwidgets.ui.MainActivity

object WidgetUpdater {
    suspend fun update(context: Context, appWidgetId: Int, kindHint: WidgetKind? = null) {
        val database = AppDatabase.get(context)
        Repository.ensureSeeded(database)

        val config = database.widgetConfigDao().get(appWidgetId)
        val kind = kindHint ?: WidgetKind.from(config?.kind)
        val template = Repository.templateOr(database, config?.templateId, kind)
        val glyphs = Repository.glyphMap(database, template.id)
        val settings = database.settingsDao().get() ?: SettingsEntity()

        val lines = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            when (kind) {
                WidgetKind.CLOCK -> WidgetData.clock(settings)
                WidgetKind.WEATHER -> WidgetData.weather(context, settings)
                WidgetKind.BATTERY -> WidgetData.battery(context)
                WidgetKind.MUSIC -> WidgetData.music(context)
            }
        }

        val grid = AsciiGrid.compose(
            textLines = lines,
            glyphs = glyphs,
            columns = template.cols,
            rows = template.rows,
            frame = settings.frame,
        )

        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
        val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, defaultWidth(kind))
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, defaultHeight(kind))
        val bitmap = BitmapRenderer.render(
            grid = grid,
            widthPx = (widthDp * density).toInt(),
            heightPx = (heightDp * density).toInt(),
            mainColor = Color.parseColor(settings.textColor),
            dimColor = Color.parseColor(settings.dimColor),
        )

        val views = RemoteViews(context.packageName, R.layout.widget_frame)
        views.setImageViewBitmap(R.id.widget_image, bitmap)
        val click = PendingIntent.getActivity(
            context,
            appWidgetId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_image, click)
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
    }

    suspend fun updateAll(context: Context, kind: WidgetKind) {
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(android.content.ComponentName(context, providerClass(kind)))
        val database = AppDatabase.get(context)
        Repository.ensureSeeded(database)
        database.widgetConfigDao().all()
            .filter { WidgetKind.from(it.kind) == kind }
            .map { it.appWidgetId }
            .distinct()
            .plus(ids.toList())
            .distinct()
            .forEach { id -> update(context, id, kind) }
    }

    fun providerClass(kind: WidgetKind): Class<out android.appwidget.AppWidgetProvider> = when (kind) {
        WidgetKind.CLOCK -> ClockWidgetProvider::class.java
        WidgetKind.WEATHER -> WeatherWidgetProvider::class.java
        WidgetKind.BATTERY -> BatteryWidgetProvider::class.java
        WidgetKind.MUSIC -> MusicWidgetProvider::class.java
    }

    private fun defaultWidth(kind: WidgetKind) = when (kind) {
        WidgetKind.CLOCK, WidgetKind.MUSIC -> 250
        else -> 110
    }

    private fun defaultHeight(kind: WidgetKind) = when (kind) {
        WidgetKind.MUSIC -> 60
        WidgetKind.CLOCK -> 110
        else -> 110
    }
}
