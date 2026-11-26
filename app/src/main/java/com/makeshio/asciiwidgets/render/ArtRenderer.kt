package com.makeshio.asciiwidgets.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.makeshio.asciiwidgets.data.StyleRenderData
import kotlin.math.min
import kotlin.math.roundToInt

/** Рисует кадр ASCII-сцены, перекрашенный в выбранный «лук». */
object ArtRenderer {
    fun render(
        frame: List<String>,
        look: StyleRenderData,
        widthPx: Int,
        heightPx: Int,
        mainColor: Int,
        dimColor: Int,
        border: Boolean = false,
    ): Bitmap {
        val width = widthPx.coerceAtLeast(1)
        val height = heightPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val rows = frame.size.coerceAtLeast(1)
        val columns = frame.firstOrNull()?.length?.coerceAtLeast(1) ?: 1
        val cellWidth = width / columns.toFloat()
        val cellHeight = height / rows.toFloat()
        val ramp = normalized(look.chars)

        frame.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, char ->
                val darkness = darknessOf(char)
                if (darkness <= 0f) return@forEachIndexed
                paint.color = if (darkness >= 0.45f) mainColor else dimColor
                val cx = (columnIndex + 0.5f) * cellWidth
                val cy = (rowIndex + 0.5f) * cellHeight
                if (look.shape == "TEXT") {
                    val glyph = ramp[(darkness * (ramp.length - 1)).roundToInt()
                        .coerceIn(ramp.indices)]
                    if (glyph != ' ') {
                        paint.typeface = Typeface.MONOSPACE
                        paint.textAlign = Paint.Align.CENTER
                        paint.textSize = min(cellWidth, cellHeight) * 1.08f
                        canvas.drawText(
                            glyph.toString(),
                            cx,
                            cy - (paint.descent() + paint.ascent()) / 2f,
                            paint,
                        )
                    }
                } else {
                    val size = min(cellWidth, cellHeight) * look.fill * darkness
                    if (look.shape == "DOT") {
                        canvas.drawCircle(cx, cy, size / 2f, paint)
                    } else {
                        canvas.drawRect(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f, paint)
                    }
                }
            }
        }

        if (border) {
            paint.color = dimColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRect(1f, 1f, width - 1f, height - 1f, paint)
        }
        return bitmap
    }

    private fun normalized(chars: String): String =
        if (chars.isEmpty() || chars.first() != ' ') " $chars" else chars

    private fun darknessOf(char: Char): Float = when (char) {
        ' ' -> 0f
        '░' -> 0.25f
        '▒' -> 0.5f
        '▓' -> 0.75f
        '█' -> 1f
        in " .·" -> 0.12f
        in ":-'=~" -> 0.28f
        in "+*oc" -> 0.45f
        in "xX%#8&@$" -> 0.8f
        in "0123456789" -> 0.6f
        else -> 0.55f
    }
}
