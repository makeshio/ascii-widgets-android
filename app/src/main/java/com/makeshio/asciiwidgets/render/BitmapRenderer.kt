package com.makeshio.asciiwidgets.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
}
