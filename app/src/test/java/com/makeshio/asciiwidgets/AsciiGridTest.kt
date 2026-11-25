package com.makeshio.asciiwidgets

import com.makeshio.asciiwidgets.data.DefaultGlyphs
import com.makeshio.asciiwidgets.render.AsciiGrid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AsciiGridTest {
    @Test
    fun clock1245UsesFigletDigits() {
        val line = AsciiGrid.renderLine("12:45", DefaultGlyphs.symbols, 30)
        assertEquals(6, line.size)
        assertTrue(line[0].startsWith("  #   #####"))
        assertTrue(line[0].contains("#   #"))
        assertEquals(30, line[0].length)
    }

    @Test
    fun composePlacesTwoLinesAndFrame() {
        val grid = AsciiGrid.compose(
            textLines = listOf("12:45", "26.11"),
            glyphs = DefaultGlyphs.symbols,
            columns = 30,
            rows = 12,
            frame = true,
        )
        assertEquals(12, grid.size)
        assertTrue(grid.all { it.length == 30 })
        assertTrue(grid.first().all { it == '.' })
    }
}
