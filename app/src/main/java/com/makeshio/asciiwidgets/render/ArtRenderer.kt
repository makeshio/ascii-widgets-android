package com.makeshio.asciiwidgets.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.makeshio.asciiwidgets.data.StyleRenderData
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Рисует кадр ASCII-сцены только моноширинными символами.
 * Прямоугольники, точки и рамки здесь не используются намеренно.
 */
object ArtRenderer {
    private const val MASTER_RAMP =
        " .'^`,:;Il!i><~+_-?][}{1)(|/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@\$"
    private const val PARTIAL_RAMP = " ▁▂▃▄▅▆▇█"
    private const val SHADE_RAMP = " ░▒▓█"
    private const val FALLBACK_RAMP = MASTER_RAMP

    fun render(
        frame: List<String>,
        look: StyleRenderData,
        widthPx: Int,
        heightPx: Int,
        dimColor: Int,
        mainColor: Int,
        accentColor: Int,
    ): Bitmap {
        val width = widthPx.coerceAtLeast(1)
        val height = heightPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
        val rows = frame.size.coerceAtLeast(1)
        val columns = frame.firstOrNull()?.length?.coerceAtLeast(1) ?: 1
        val cellWidth = width / columns.toFloat()
        val cellHeight = height / rows.toFloat()
        val ramp = normalized(look.chars.ifEmpty { FALLBACK_RAMP })
        paint.textSize = min(cellWidth, cellHeight) * 1.06f

        frame.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, char ->
                val darkness = darknessOf(char)
                if (darkness <= 0f) return@forEachIndexed
                val glyph = ramp[(darkness * (ramp.length - 1)).roundToInt()
                    .coerceIn(ramp.indices)]
                if (glyph == ' ') return@forEachIndexed

                paint.color = when {
                    darkness >= 0.8f -> accentColor
                    darkness >= 0.45f -> mainColor
                    else -> dimColor
                }
                val cx = (columnIndex + 0.5f) * cellWidth
                val cy = (rowIndex + 0.5f) * cellHeight
                canvas.drawText(
                    glyph.toString(),
                    cx,
                    cy - (paint.descent() + paint.ascent()) / 2f,
                    paint,
                )
            }
        }
        return bitmap
    }

    private fun normalized(chars: String): String =
        if (chars.isEmpty() || chars.first() != ' ') " $chars" else chars

    private fun darknessOf(char: Char): Float =
        ratio(char, MASTER_RAMP)
            ?: ratio(char, PARTIAL_RAMP)
            ?: ratio(char, SHADE_RAMP)
            ?: when {
                char.isDigit() -> 0.5f + (char - '0') / 10f * 0.3f
                char.isLetter() -> 0.58f
                else -> 0.45f
            }

    private fun ratio(char: Char, ramp: String): Float? {
        val index = ramp.indexOf(char)
        if (index < 0) return null
        return index / (ramp.length - 1).toFloat()
    }
}
