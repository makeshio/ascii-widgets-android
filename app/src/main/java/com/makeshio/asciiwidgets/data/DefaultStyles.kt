package com.makeshio.asciiwidgets.data

object DefaultStyles {
    val styles: List<StyleEntity> = listOf(
        StyleEntity("classic", "Классический", "RECT", 0.92f),
        StyleEntity("slim", "Тонкий", "RECT", 0.55f),
        StyleEntity("dot", "Точечный", "DOT", 0.30f),
    )

    fun glyphsFor(styleId: String): List<StyleGlyphEntity> =
        DefaultGlyphs.symbols.map { (symbol, rows) ->
            StyleGlyphEntity(styleId, symbol, rows.joinToString("\n"))
        }
}
