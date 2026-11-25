package com.makeshio.asciiwidgets.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.SystemClock
import android.widget.RemoteViews
import com.makeshio.asciiwidgets.R
import com.makeshio.asciiwidgets.data.AppDatabase
import com.makeshio.asciiwidgets.data.Repository
import com.makeshio.asciiwidgets.data.SettingsEntity
import com.makeshio.asciiwidgets.data.StyleMap
import com.makeshio.asciiwidgets.data.WidgetKind
import com.makeshio.asciiwidgets.render.AsciiGrid
import com.makeshio.asciiwidgets.render.BitmapRenderer
import com.makeshio.asciiwidgets.sources.WidgetData
import com.makeshio.asciiwidgets.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private data class WidgetSnapshot(
    val kind: WidgetKind,
    val lines: List<String>,
    val glyphs: Map<String, List<String>>,
    val columns: Int,
    val rows: Int,
    val frame: Boolean,
)

object WidgetUpdater {
    /** Данные обновляются раз в минуту, между тиками только переключаются готовые кадры. */
    private val snapshots = ConcurrentHashMap<Int, WidgetSnapshot>()

    fun phaseNow(): Int = (SystemClock.elapsedRealtime() / 1000L).toInt() and 1

    fun forget(appWidgetId: Int) {
        snapshots.remove(appWidgetId)
    }

    suspend fun update(
        context: Context,
        appWidgetId: Int,
        kindHint: WidgetKind? = null,
        animated: Boolean = false,
        phase: Int = 0,
    ) {
        val database = AppDatabase.get(context)
        Repository.ensureSeeded(database)
        val settings = database.settingsDao().get() ?: SettingsEntity()
        if (animated && !settings.animationEnabled) return

        val snapshot = if (animated) {
            snapshots[appWidgetId]
        } else {
            loadSnapshot(context, database, appWidgetId, kindHint, settings).also {
                snapshots[appWidgetId] = it
            }
        } ?: loadSnapshot(context, database, appWidgetId, kindHint, settings).also {
            snapshots[appWidgetId] = it
        }

        val mainColor = Color.parseColor(settings.textColor)
        val dimColor = Color.parseColor(settings.dimColor)
        val (widthPx, heightPx) = dimensions(context, appWidgetId, snapshot.kind)
        val styles = Repository.styleMap(database)
        val styleMap = StyleMap.decode(settings.styleMap)
        val styleFor: (Char) -> String = { char -> StyleMap.styleFor(char, styleMap) }

        val normalGrid = AsciiGrid.composeStyled(
            textLines = snapshot.lines,
            styles = styles,
            styleFor = styleFor,
            columns = snapshot.columns,
            rows = snapshot.rows,
            frame = snapshot.frame,
        )
        val bitmapA = BitmapRenderer.renderStyled(
            normalGrid, styles, widthPx, heightPx, mainColor, dimColor,
        )

        val blinkSymbols = if (settings.animationEnabled) blinkSymbols(snapshot) else emptySet()
        val bitmapB = if (blinkSymbols.isEmpty()) {
            bitmapA
        } else {
            val dimmedGrid = AsciiGrid.composeStyled(
                textLines = snapshot.lines,
                styles = styles,
                styleFor = styleFor,
                columns = snapshot.columns,
                rows = snapshot.rows,
                frame = snapshot.frame,
                dimSymbols = blinkSymbols,
            )
            BitmapRenderer.renderStyled(
                dimmedGrid, styles, widthPx, heightPx, mainColor, dimColor,
            )
        }

        val views = RemoteViews(context.packageName, R.layout.widget_frame)
        views.setImageViewBitmap(R.id.widget_image_a, bitmapA)
        views.setImageViewBitmap(R.id.widget_image_b, bitmapB)
        views.setInt(
            R.id.widget_flipper,
            "setDisplayedChild",
            if (settings.animationEnabled && phase % 2 == 1) 1 else 0,
        )
        val click = PendingIntent.getActivity(
            context,
            appWidgetId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_flipper, click)
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
    }

    suspend fun updateAll(
        context: Context,
        kind: WidgetKind,
        animated: Boolean = false,
        phase: Int = 0,
    ) {
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, providerClass(kind)))
        val database = AppDatabase.get(context)
        Repository.ensureSeeded(database)
        database.widgetConfigDao().all()
            .filter { WidgetKind.from(it.kind) == kind }
            .map { it.appWidgetId }
            .distinct()
            .plus(ids.toList())
            .distinct()
            .forEach { id -> update(context, id, kind, animated, phase) }
    }

    fun providerClass(kind: WidgetKind): Class<out android.appwidget.AppWidgetProvider> = when (kind) {
        WidgetKind.CLOCK -> ClockWidgetProvider::class.java
        WidgetKind.WEATHER -> WeatherWidgetProvider::class.java
        WidgetKind.BATTERY -> BatteryWidgetProvider::class.java
        WidgetKind.MUSIC -> MusicWidgetProvider::class.java
    }

    private suspend fun loadSnapshot(
        context: Context,
        database: AppDatabase,
        appWidgetId: Int,
        kindHint: WidgetKind?,
        settings: SettingsEntity,
    ): WidgetSnapshot {
        val config = database.widgetConfigDao().get(appWidgetId)
        val kind = kindHint ?: WidgetKind.from(config?.kind)
        val template = Repository.templateOr(database, config?.templateId, kind)
        val glyphs = Repository.glyphMap(database, template.id)
        val lines = withContext(Dispatchers.IO) {
            when (kind) {
                WidgetKind.CLOCK -> WidgetData.clock(settings)
                WidgetKind.WEATHER -> WidgetData.weather(context, settings)
                WidgetKind.BATTERY -> WidgetData.battery(context)
                WidgetKind.MUSIC -> WidgetData.music(context)
            }
        }
        return WidgetSnapshot(kind, lines, glyphs, template.cols, template.rows, settings.frame)
    }

    private fun blinkSymbols(snapshot: WidgetSnapshot): Set<String> = when (snapshot.kind) {
        WidgetKind.CLOCK -> setOf(":")
        WidgetKind.WEATHER -> setOf("°")
        WidgetKind.BATTERY -> setOf("C", "W", "B")
        WidgetKind.MUSIC -> snapshot.lines.firstOrNull()
            ?.firstOrNull()
            ?.uppercaseChar()
            ?.toString()
            ?.let { setOf(it) }
            ?: emptySet()
    }

    private fun dimensions(context: Context, appWidgetId: Int, kind: WidgetKind): Pair<Int, Int> {
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
        return (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, defaultWidth(kind)) * density).toInt() to
            (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, defaultHeight(kind)) * density).toInt()
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
