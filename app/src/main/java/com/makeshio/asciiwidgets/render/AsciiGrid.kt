package com.makeshio.asciiwidgets.render

import com.makeshio.asciiwidgets.data.StyleRenderData

/** Символьная сетка: '#' — основной цвет, '.' — приглушённый, ' ' — пусто. */
data class StyledGrid(val chars: List<String>, val styleIds: List<List<String>>)

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

    fun renderStyledLine(
        text: String,
        styles: Map<String, StyleRenderData>,
        styleFor: (Char) -> String,
        columns: Int,
        dimSymbols: Set<String> = emptySet(),
    ): StyledGrid {
        val chars = MutableList(GLYPH_ROWS) { StringBuilder() }
        val ids = MutableList(GLYPH_ROWS) { mutableListOf<String>() }
        val blank = List(GLYPH_ROWS) { ".".repeat(GLYPH_COLS) }

        text.uppercase().forEach { char ->
            val key = char.toString()
            val styleId = styleFor(char)
            val glyph = styles[styleId]?.glyphs?.get(key)
                ?: styles.values.firstOrNull()?.glyphs?.get(key)
                ?: blank
            val dim = key in dimSymbols
            repeat(GLYPH_ROWS) { row ->
                val source = glyph.getOrNull(row) ?: ".".repeat(GLYPH_COLS)
                source.forEach { cell ->
                    chars[row].append(if (cell == '#') if (dim) '.' else '#' else ' ')
                    ids[row].add(styleId)
                }
                chars[row].append(' ')
                ids[row].add(styleId)
            }
        }

        return StyledGrid(
            chars.map { it.toString().take(columns).padEnd(columns, ' ') },
            ids.map { it.take(columns) },
        )
    }

    fun composeStyled(
        textLines: List<String>,
        styles: Map<String, StyleRenderData>,
        styleFor: (Char) -> String,
        columns: Int,
        rows: Int,
        frame: Boolean = false,
        dimSymbols: Set<String> = emptySet(),
    ): StyledGrid {
        val chars = Array(rows) { CharArray(columns) { ' ' } }
        val ids = Array(rows) { Array(columns) { "" } }
        textLines.take(rows / GLYPH_ROWS).forEachIndexed { index, text ->
            val rendered = renderStyledLine(text, styles, styleFor, columns, dimSymbols)
            repeat(GLYPH_ROWS) { row ->
                val target = index * GLYPH_ROWS + row
                if (target >= rows) return@repeat
                rendered.chars[row].forEachIndexed { column, cell ->
                    if (column < columns && cell != ' ') {
                        chars[target][column] = cell
                        ids[target][column] = rendered.styleIds[row][column]
                    }
                }
            }
        }
        if (frame) {
            chars.firstOrNull()?.fill('.')
            chars.lastOrNull()?.fill('.')
            chars.forEachIndexed { rowIndex, row ->
                row.firstOrNull()?.let { row[0] = '.' }
                row.lastOrNull()?.let { row[columns - 1] = '.' }
                ids[rowIndex][0] = "classic"
                ids[rowIndex][columns - 1] = "classic"
            }
        }
        return StyledGrid(chars.map { String(it) }, ids.map { it.toList() })
    }
}
