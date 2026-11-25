package com.makeshio.asciiwidgets.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.makeshio.asciiwidgets.data.StyleRenderData
import kotlin.math.min

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
                if (style.shape == "DOT") {
                    canvas.drawCircle(cx, cy, size / 2f, paint)
                } else {
                    canvas.drawRect(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f, paint)
                }
            }
        }
        return bitmap
    }
}
