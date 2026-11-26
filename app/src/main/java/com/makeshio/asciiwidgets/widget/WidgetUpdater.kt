package com.makeshio.asciiwidgets.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.SystemClock
import android.widget.RemoteViews
import com.makeshio.asciiwidgets.R
import com.makeshio.asciiwidgets.data.AppDatabase
import com.makeshio.asciiwidgets.data.ArtPieces
import com.makeshio.asciiwidgets.data.EffectiveSettings
import com.makeshio.asciiwidgets.data.Repository
import com.makeshio.asciiwidgets.data.SettingsEntity
import com.makeshio.asciiwidgets.data.StyleMap
import com.makeshio.asciiwidgets.data.StyleRenderData
import com.makeshio.asciiwidgets.data.WidgetConfigEntity
import com.makeshio.asciiwidgets.data.WidgetKind
import com.makeshio.asciiwidgets.render.ArtRenderer
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
    val columns: Int,
    val rows: Int,
    val frame: Boolean,
)

object WidgetUpdater {
    /** Данные обновляются раз в минуту, между тиками только переключаются готовые кадры. */
    private val snapshots = ConcurrentHashMap<Int, WidgetSnapshot>()

    fun phaseNow(): Int = (SystemClock.elapsedRealtime() / 1000L).toInt() and 1

    /** У арт-виджета четыре заранее сохранённых кадра, смена раз в секунду. */
    fun artPhaseNow(): Int = (SystemClock.elapsedRealtime() / 1000L).toInt() and 3

    fun forget(appWidgetId: Int) {
        snapshots.remove(appWidgetId)
    }

    suspend fun update(
        context: Context,
        appWidgetId: Int,
        kindHint: WidgetKind? = null,
        animated: Boolean = false,
        phase: Int = 0,
        templateHint: String? = null,
    ) {
        val database = AppDatabase.get(context)
        Repository.ensureSeeded(database)
        val config = database.widgetConfigDao().get(appWidgetId)
        val kind = kindHint ?: WidgetKind.from(config?.kind)
        val settings = EffectiveSettings.merge(database.settingsDao().get() ?: SettingsEntity(), config)
        if (animated && !settings.animationEnabled) return

        val (widthPx, heightPx) = dimensions(context, appWidgetId)
        val styles = Repository.styleMap(database)
        if (kind == WidgetKind.ART) {
            // Глобальная анимация относится к мигающим индикаторам; сцены включаются только явно.
            if (animated && config?.animationEnabled != true) return
            updateArt(context, appWidgetId, config, styles, widthPx, heightPx, animated)
            return
        }

        val snapshot = if (animated) {
            snapshots[appWidgetId]
        } else {
            loadSnapshot(context, database, appWidgetId, kind, settings, templateHint).also {
                snapshots[appWidgetId] = it
            }
        } ?: loadSnapshot(context, database, appWidgetId, kind, settings, templateHint).also {
            snapshots[appWidgetId] = it
        }

        val mainColor = Color.parseColor(settings.textColor)
        val dimColor = Color.parseColor(settings.dimColor)
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

        applyBitmaps(
            context,
            appWidgetId,
            bitmapA,
            bitmapB,
            showSecond = settings.animationEnabled && phase % 2 == 1,
        )
    }

    private suspend fun updateArt(
        context: Context,
        appWidgetId: Int,
        config: WidgetConfigEntity?,
        styles: Map<String, StyleRenderData>,
        widthPx: Int,
        heightPx: Int,
        animated: Boolean,
    ) {
        val pieces = withContext(Dispatchers.IO) { ArtPieces.load(context) }
        val piece = pieces.firstOrNull { it.id == config?.artPieceId }
            ?: ArtPieces.default(pieces)
            ?: return
        val styleId = config?.artLookId?.takeIf { it in StyleMap.LOOK_IDS }
            ?: StyleMap.DEFAULT_ART_LOOK
        val style = styles[styleId] ?: styles[StyleMap.DEFAULT_ART_LOOK] ?: return
        val dimColor = Color.parseColor(piece.dimColor)
        val mainColor = Color.parseColor(piece.mainColor)
        val accentColor = Color.parseColor(piece.accentColor)
        val index = if (animated) artPhaseNow() else 0
        val bitmapA = ArtRenderer.render(
            frame = piece.frame(index),
            look = style,
            widthPx = widthPx,
            heightPx = heightPx,
            dimColor = dimColor,
            mainColor = mainColor,
            accentColor = accentColor,
        )
        val bitmapB = ArtRenderer.render(
            frame = piece.frame(index + 1),
            look = style,
            widthPx = widthPx,
            heightPx = heightPx,
            dimColor = dimColor,
            mainColor = mainColor,
            accentColor = accentColor,
        )
        applyBitmaps(context, appWidgetId, bitmapA, bitmapB, showSecond = animated)
    }

    private fun applyBitmaps(
        context: Context,
        appWidgetId: Int,
        bitmapA: android.graphics.Bitmap,
        bitmapB: android.graphics.Bitmap,
        showSecond: Boolean,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_frame)
        views.setImageViewBitmap(R.id.widget_image_a, bitmapA)
        views.setImageViewBitmap(R.id.widget_image_b, bitmapB)
        views.setInt(R.id.widget_flipper, "setDisplayedChild", if (showSecond) 1 else 0)
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
        val manager = AppWidgetManager.getInstance(context)
        val installed = providerClasses(kind).flatMap { provider ->
            manager.getAppWidgetIds(ComponentName(context, provider)).toList()
        }
        val database = AppDatabase.get(context)
        Repository.ensureSeeded(database)
        database.widgetConfigDao().all()
            .filter { WidgetKind.from(it.kind) == kind }
            .map { it.appWidgetId }
            .distinct()
            .plus(installed)
            .distinct()
            .forEach { id -> update(context, id, kind, animated, phase) }
    }

    val allProviders: List<Class<out AppWidgetProvider>> = listOf(
        ClockWidgetProvider::class.java,
        ClockCompactWidgetProvider::class.java,
        ClockLargeWidgetProvider::class.java,
        WeatherWidgetProvider::class.java,
        HourlyWeatherWidgetProvider::class.java,
        BatteryWidgetProvider::class.java,
        MusicWidgetProvider::class.java,
        ArtWidgetProvider::class.java,
    )

    fun providerClasses(kind: WidgetKind): List<Class<out AppWidgetProvider>> =
        allProviders.filter { kindForProvider(it.name) == kind }

    fun kindForProvider(className: String?): WidgetKind = when {
        className.orEmpty().contains("Art") -> WidgetKind.ART
        className.orEmpty().contains("Clock") -> WidgetKind.CLOCK
        className.orEmpty().contains("Weather") -> WidgetKind.WEATHER
        className.orEmpty().contains("Battery") -> WidgetKind.BATTERY
        className.orEmpty().contains("Music") -> WidgetKind.MUSIC
        else -> WidgetKind.CLOCK
    }

    fun templateForProvider(className: String?): String = when {
        className.orEmpty().contains("ClockCompact") -> "builtin_clock_compact"
        className.orEmpty().contains("ClockLarge") -> "builtin_clock_large"
        className.orEmpty().contains("HourlyWeather") -> "builtin_weather_hourly"
        else -> when (kindForProvider(className)) {
            WidgetKind.CLOCK -> "builtin_clock"
            WidgetKind.WEATHER -> "builtin_weather"
            WidgetKind.BATTERY -> "builtin_battery"
            WidgetKind.MUSIC -> "builtin_music"
            WidgetKind.ART -> "builtin_art"
        }
    }

    private suspend fun loadSnapshot(
        context: Context,
        database: AppDatabase,
        appWidgetId: Int,
        kindHint: WidgetKind?,
        settings: EffectiveSettings,
        templateHint: String?,
    ): WidgetSnapshot {
        val config = database.widgetConfigDao().get(appWidgetId)
        val kind = kindHint ?: WidgetKind.from(config?.kind)
        val template = Repository.templateOr(
            database,
            config?.templateId ?: templateHint,
            kind,
        )
        val hourly = template.id == "builtin_weather_hourly"
        val lines = withContext(Dispatchers.IO) {
            when (kind) {
                WidgetKind.CLOCK -> WidgetData.clock(settings)
                WidgetKind.WEATHER -> WidgetData.weather(context, settings, hourly)
                WidgetKind.BATTERY -> WidgetData.battery(context)
                WidgetKind.MUSIC -> WidgetData.music(context, settings.musicSource)
                WidgetKind.ART -> emptyList()
            }
        }
        return WidgetSnapshot(kind, lines, template.cols, template.rows, settings.frame)
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
        WidgetKind.ART -> emptySet()
    }

    private fun dimensions(context: Context, appWidgetId: Int): Pair<Int, Int> {
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
        return (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250) * density).toInt() to
            (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110) * density).toInt()
    }
}
