package com.makeshio.asciiwidgets.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.makeshio.asciiwidgets.data.StyleRenderData
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Рисует кадр ASCII-сцены на тёмной подложке терминала.
 * Символы остаются настоящим ASCII: прямоугольников и блоков среди глифов нет.
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
        title: String = "",
    ): Bitmap {
        val width = widthPx.coerceAtLeast(1)
        val height = heightPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val panel = RectF(0f, 0f, width.toFloat(), height.toFloat())

        val topBar = height * 0.085f
        val bottomBar = height * 0.075f
        val contentTop = topBar
        val contentHeight = (height - topBar - bottomBar).coerceAtLeast(1f)

        // Подложка терминала: почти чёрная, с лёгким оттенком палитры сцены.
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = darkPanel(dimColor, 0.04f)
        }
        canvas.drawRoundRect(panel, 10f, 10f, backgroundPaint)

        canvas.save()
        canvas.clipRect(panel)
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = darkPanel(mainColor, 0.10f)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), topBar, barPaint)

        // Тонкие горизонтальные линии сканирования, как у старого терминала.
        val scanPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = withAlpha(mainColor, 14)
        }
        var y = contentTop + 3f
        while (y < height - bottomBar) {
            canvas.drawLine(0f, y, width.toFloat(), y, scanPaint)
            y += 4f
        }

        val separatorPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = withAlpha(dimColor, 90)
        }
        canvas.drawLine(0f, topBar, width.toFloat(), topBar, separatorPaint)
        canvas.drawLine(0f, height - bottomBar, width.toFloat(), height - bottomBar, separatorPaint)
        canvas.restore()

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = withAlpha(dimColor, 150)
        }
        canvas.drawRoundRect(panel, 10f, 10f, borderPaint)

        val rows = frame.size.coerceAtLeast(1)
        val columns = frame.firstOrNull()?.length?.coerceAtLeast(1) ?: 1
        val cellWidth = width / columns.toFloat()
        val cellHeight = contentHeight / rows
        val ramp = normalized(look.chars.ifEmpty { FALLBACK_RAMP })

        val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
            textSize = min(cellWidth, cellHeight) * 1.06f
        }
        frame.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, char ->
                val darkness = darknessOf(char)
                if (darkness <= 0f) return@forEachIndexed
                val glyph = ramp[(darkness * (ramp.length - 1)).roundToInt()
                    .coerceIn(ramp.indices)]
                if (glyph == ' ') return@forEachIndexed

                glyphPaint.color = when {
                    darkness >= 0.8f -> accentColor
                    darkness >= 0.45f -> mainColor
                    else -> dimColor
                }
                val cx = (columnIndex + 0.5f) * cellWidth
                val cy = contentTop + (rowIndex + 0.5f) * cellHeight
                canvas.drawText(
                    glyph.toString(),
                    cx,
                    cy - (glyphPaint.descent() + glyphPaint.ascent()) / 2f,
                    glyphPaint,
                )
            }
        }

        val chromePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            typeface = Typeface.MONOSPACE
        }
        val dotRadius = (topBar * 0.14f).coerceAtMost(5f).coerceAtLeast(1.5f)
        val dotY = topBar / 2f
        val dotColors = listOf(
            withAlpha(accentColor, 235),
            withAlpha(mainColor, 200),
            withAlpha(dimColor, 160),
        )
        dotColors.forEachIndexed { index, color ->
            chromePaint.color = color
            canvas.drawCircle(12f + index * dotRadius * 3.2f, dotY, dotRadius, chromePaint)
        }

        chromePaint.textAlign = Paint.Align.LEFT
        chromePaint.color = withAlpha(mainColor, 220)
        chromePaint.textSize = (topBar * 0.42f).coerceAtLeast(8f)
        val label = "root@ascii:~$ ${title.take(22).uppercase()}"
        canvas.drawText(
            label,
            12f + dotRadius * 5.6f,
            topBar / 2f - (chromePaint.descent() + chromePaint.ascent()) / 2f,
            chromePaint,
        )

        chromePaint.color = withAlpha(dimColor, 210)
        chromePaint.textSize = (bottomBar * 0.46f).coerceAtLeast(8f)
        canvas.drawText(
            "> ready_",
            12f,
            height - bottomBar / 2f - (chromePaint.descent() + chromePaint.ascent()) / 2f,
            chromePaint,
        )
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

    private fun darkPanel(color: Int, boost: Float): Int = Color.rgb(
        (6 + Color.red(color) * boost).toInt().coerceIn(0, 255),
        (10 + Color.green(color) * boost).toInt().coerceIn(0, 255),
        (9 + Color.blue(color) * boost).toInt().coerceIn(0, 255),
    )

    private fun withAlpha(color: Int, alpha: Int): Int = Color.argb(
        alpha.coerceIn(0, 255),
        Color.red(color),
        Color.green(color),
        Color.blue(color),
    )
}
