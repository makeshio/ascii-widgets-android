package com.makeshio.asciiwidgets.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.makeshio.asciiwidgets.data.DefaultGlyphs
import com.makeshio.asciiwidgets.data.DefaultStyles
import com.makeshio.asciiwidgets.data.DefaultTemplates
import com.makeshio.asciiwidgets.data.EffectiveSettings
import com.makeshio.asciiwidgets.data.StyleMap
import com.makeshio.asciiwidgets.data.StyleRenderData
import com.makeshio.asciiwidgets.data.WidgetKind
import com.makeshio.asciiwidgets.render.AsciiGrid
import com.makeshio.asciiwidgets.render.BitmapRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Статичное превью без сети и без обращения к базе — для списков и конфигуратора. */
@Composable
fun WidgetPreview(
    kind: WidgetKind,
    templateId: String?,
    settings: EffectiveSettings,
    modifier: Modifier = Modifier,
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, kind, templateId, settings) {
        value = withContext(Dispatchers.Default) {
            buildPreview(kind, templateId, settings)
        }
    }
    bitmap?.let { image ->
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    }
}

private fun buildPreview(
    kind: WidgetKind,
    templateId: String?,
    settings: EffectiveSettings,
): Bitmap {
    val template = DefaultTemplates.byId(templateId.orEmpty())
        ?: DefaultTemplates.defaultTemplate(kind)
    val hourly = template.id == "builtin_weather_hourly"
    val lines = when (kind) {
        WidgetKind.CLOCK -> previewClockLines(template.id)
        WidgetKind.WEATHER -> if (hourly) listOf("18°C", "19°20°21°") else listOf("18°C", "21/14")
        WidgetKind.BATTERY -> if (template.rows <= 6) listOf("87%") else listOf("87%", "W B")
        WidgetKind.MUSIC -> listOf("MY MUSIC".take(if (template.cols >= 54) 12 else 7))
    }

    val styles: Map<String, StyleRenderData> = DefaultStyles.styles.associate { style ->
        style.id to StyleRenderData(
            shape = style.shape,
            fill = style.fill,
            glyphs = DefaultGlyphs.symbols.mapValues { entry -> entry.value },
        )
    }
    val mapping = StyleMap.decode(settings.styleMap)
    val grid = AsciiGrid.composeStyled(
        textLines = lines,
        styles = styles,
        styleFor = { char -> StyleMap.styleFor(char, mapping) },
        columns = template.cols,
        rows = template.rows,
        frame = settings.frame,
    )
    val width = 480
    val height = (480 * template.rows / template.cols.toFloat()).toInt().coerceAtLeast(48)
    val main = safeColor(settings.textColor, Color.WHITE)
    val dim = safeColor(settings.dimColor, Color.GRAY)
    return BitmapRenderer.renderStyled(grid, styles, width, height, main, dim)
}

private fun previewClockLines(templateId: String): List<String> = when (templateId) {
    "builtin_clock_compact" -> listOf("12:45")
    "builtin_clock_large" -> listOf("12:45", "26.11", "WED")
    else -> listOf("12:45", "26.11")
}

private fun safeColor(value: String, fallback: Int): Int = try {
    Color.parseColor(value)
} catch (_: IllegalArgumentException) {
    fallback
}
