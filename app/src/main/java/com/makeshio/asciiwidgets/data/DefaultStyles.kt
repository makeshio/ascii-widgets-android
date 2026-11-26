package com.makeshio.asciiwidgets.data

object DefaultStyles {
    private const val ASCII_RAMP =
        " .'^`,:;-~+=*?I!i|/()[]{}tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@\$"
    private const val DENSE_RAMP =
        " .'^`,:;Il!i><~+_-?][}{1)(|/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@\$"

    val styles: List<StyleEntity> = listOf(
        StyleEntity("classic", "Классический", "RECT", 0.92f),
        StyleEntity("slim", "Тонкий", "RECT", 0.55f),
        StyleEntity("dot", "Точечный", "DOT", 0.30f),
        // «Луки» из ASCII Magic: рисуются настоящими моноширинными символами.
        StyleEntity("dots", "Точки ASCII", "TEXT", 1f, " .·:;+*oO0@"),
        StyleEntity("ascii", "Обычный ASCII", "TEXT", 1f, ASCII_RAMP),
        StyleEntity("dense", "Плотный ASCII", "TEXT", 1f, DENSE_RAMP),
        StyleEntity("blocks", "Блоки", "TEXT", 1f, " ▁▂▃▄▅▆▇█░▒▓█"),
        StyleEntity("binary", "Двоичный", "TEXT", 1f, "  00001111"),
        StyleEntity("numbers", "Цифры", "TEXT", 1f, " 9876543210"),
    )

    fun glyphsFor(styleId: String): List<StyleGlyphEntity> =
        DefaultGlyphs.symbols.map { (symbol, rows) ->
            StyleGlyphEntity(styleId, symbol, rows.joinToString("\n"))
        }
}
