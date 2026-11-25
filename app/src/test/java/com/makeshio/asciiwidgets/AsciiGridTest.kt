package com.makeshio.asciiwidgets

import com.makeshio.asciiwidgets.data.DefaultGlyphs
import com.makeshio.asciiwidgets.data.DefaultStyles
import com.makeshio.asciiwidgets.data.StyleMap
import com.makeshio.asciiwidgets.data.StyleRenderData
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
    fun dimSymbolsReduceBrightnessOfMatchingGlyphs() {
        val normal = AsciiGrid.renderLine("12:45", DefaultGlyphs.symbols, 30)
        val dimmed = AsciiGrid.renderLine("12:45", DefaultGlyphs.symbols, 30, setOf(":"))
        val normalCells = normal.sumOf { it.count { cell -> cell == '#' } }
        val dimmedCells = dimmed.sumOf { it.count { cell -> cell == '#' } }
        assertTrue(dimmedCells < normalCells)
    }

    @Test
    fun allDefaultGlyphsAreSixRows() {
        assertTrue(DefaultGlyphs.symbols.values.all { it.size == 6 })
    }

    @Test
    fun styleMapRoundTripAndResolution() {
        val encoded = StyleMap.encode(
            mapOf("digits" to "dot", "symbols" to "slim", "letters" to "classic"),
        )
        val decoded = StyleMap.decode(encoded)
        assertEquals("dot", decoded["digits"])
        assertEquals("slim", decoded["symbols"])
        assertEquals("classic", StyleMap.styleFor('A', decoded))
        assertEquals("dot", StyleMap.styleFor('7', decoded))
        assertEquals("slim", StyleMap.styleFor(':', decoded))
    }

    @Test
    fun styledGridAssignsDifferentStylesToDigitsAndSymbols() {
        val styles = DefaultStyles.styles.associate { style ->
            style.id to StyleRenderData(
                shape = style.shape,
                fill = style.fill,
                glyphs = DefaultStyles.glyphsFor(style.id).associate { glyph ->
                    glyph.symbol to glyph.rows.split("\n")
                },
            )
        }
        val mapping = StyleMap.decode(StyleMap.DEFAULT)
        val grid = AsciiGrid.composeStyled(
            textLines = listOf("12:45"),
            styles = styles,
            styleFor = { StyleMap.styleFor(it, mapping) },
            columns = 30,
            rows = 6,
        )
        assertTrue(grid.styleIds[0].contains("classic"))
        assertTrue(grid.styleIds[0].contains("dot"))
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
