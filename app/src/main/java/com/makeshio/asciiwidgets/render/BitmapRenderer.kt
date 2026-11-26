package com.makeshio.asciiwidgets.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.makeshio.asciiwidgets.data.StyleRenderData
import kotlin.math.min
import kotlin.math.roundToInt

/** Рисует символьную сетку прямоугольниками — как KWGT-пресеты, без зависимости от шрифта. */
object BitmapRenderer {
    fun render(
        grid: List<String>,
        widthPx: Int,
        heightPx: Int,
        mainColor: Int,
        dimColor: Int,
    ): Bitmap {
        val width = widthPx.coerceAtLeast(1)
        val height = heightPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        val rows = grid.size.coerceAtLeast(1)
        val columns = grid.firstOrNull()?.length?.coerceAtLeast(1) ?: 1
        val cellWidth = width / columns.toFloat()
        val cellHeight = height / rows.toFloat()
        val inset = min(cellWidth, cellHeight) * 0.08f

        grid.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, cell ->
                if (cell == '#' || cell == '.') {
                    paint.color = if (cell == '#') mainColor else dimColor
                    canvas.drawRect(
                        columnIndex * cellWidth + inset,
                        rowIndex * cellHeight + inset,
                        (columnIndex + 1) * cellWidth - inset,
                        (rowIndex + 1) * cellHeight - inset,
                        paint,
                    )
                }
            }
        }
        return bitmap
    }

    /** Каждая ячейка рисуется стилем, выбранным для её символа. */
    fun renderStyled(
        grid: StyledGrid,
        styles: Map<String, StyleRenderData>,
        widthPx: Int,
        heightPx: Int,
        mainColor: Int,
        dimColor: Int,
    ): Bitmap {
        val width = widthPx.coerceAtLeast(1)
        val height = heightPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        val rows = grid.chars.size.coerceAtLeast(1)
        val columns = grid.chars.firstOrNull()?.length?.coerceAtLeast(1) ?: 1
        val cellWidth = width / columns.toFloat()
        val cellHeight = height / rows.toFloat()

        grid.chars.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, cell ->
                if (cell != '#' && cell != '.') return@forEachIndexed
                val styleId = grid.styleIds.getOrNull(rowIndex)?.getOrNull(columnIndex).orEmpty()
                val style = styles[styleId] ?: styles.values.firstOrNull() ?: return@forEachIndexed
                paint.color = if (cell == '#') mainColor else dimColor

                val size = min(cellWidth, cellHeight) * style.fill.coerceIn(0.1f, 1f)
                val cx = (columnIndex + 0.5f) * cellWidth
                val cy = (rowIndex + 0.5f) * cellHeight
                when (style.shape) {
                    "TEXT" -> {
                        val glyph = rampChar(style.chars, cell)
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
                    }
                    "DOT" -> canvas.drawCircle(cx, cy, size / 2f, paint)
                    else -> canvas.drawRect(
                        cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f, paint,
                    )
                }
            }
        }
        return bitmap
    }

    /** '#', '.' и пробел из сетки лукa → символ из набора от светлого к тёмному. */
    internal fun rampChar(chars: String, cell: Char): Char {
        val ramp = if (chars.isEmpty() || chars.first() != ' ') " $chars" else chars
        val darkness = when (cell) {
            '#' -> 1f
            '.' -> 0.45f
            else -> 0f
        }
        val index = (darkness * (ramp.length - 1)).roundToInt().coerceIn(ramp.indices)
        return ramp[index]
    }
}
