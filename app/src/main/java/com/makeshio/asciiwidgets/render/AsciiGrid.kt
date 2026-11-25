package com.makeshio.asciiwidgets.render

/** Символьная сетка: '#' — основной цвет, '.' — приглушённый, ' ' — пусто. */
object AsciiGrid {
    const val GLYPH_ROWS = 6
    private const val GLYPH_COLS = 5

    fun renderLine(
        text: String,
        glyphs: Map<String, List<String>>,
        columns: Int,
        dimSymbols: Set<String> = emptySet(),
    ): List<String> {
        val lines = MutableList(GLYPH_ROWS) { StringBuilder() }
        val fallback = glyphs[" "] ?: List(GLYPH_ROWS) { ".".repeat(GLYPH_COLS) }

        text.uppercase().forEach { char ->
            val key = char.toString()
            val glyph = glyphs[key] ?: fallback
            val dim = key in dimSymbols
            repeat(GLYPH_ROWS) { row ->
                val source = glyph.getOrNull(row) ?: ".".repeat(GLYPH_COLS)
                source.forEach { cell ->
                    lines[row].append(
                        when (cell) {
                            '#' -> if (dim) '.' else '#'
                            else -> ' '
                        },
                    )
                }
            }
            lines.forEach { it.append(' ') }
        }

        return lines.map { it.toString().take(columns).padEnd(columns, ' ') }
    }

    fun compose(
        textLines: List<String>,
        glyphs: Map<String, List<String>>,
        columns: Int,
        rows: Int,
        frame: Boolean = false,
        dimSymbols: Set<String> = emptySet(),
    ): List<String> {
        val grid = Array(rows) { CharArray(columns) { ' ' } }
        textLines.take(rows / GLYPH_ROWS).forEachIndexed { index, text ->
            renderLine(text, glyphs, columns, dimSymbols).forEachIndexed { row, rendered ->
                val targetRow = index * GLYPH_ROWS + row
                if (targetRow >= rows) return@forEachIndexed
                rendered.forEachIndexed { column, cell ->
                    if (column < columns) grid[targetRow][column] = cell
                }
            }
        }
        if (frame) {
            grid.firstOrNull()?.fill('.')
            grid.lastOrNull()?.fill('.')
            grid.forEach { row ->
                row.firstOrNull()?.let { row[0] = '.' }
                row.lastOrNull()?.let { row[columns - 1] = '.' }
            }
        }
        return grid.map { String(it) }
    }
}
