package com.makeshio.asciiwidgets

import com.makeshio.asciiwidgets.data.ArtPieces
import com.makeshio.asciiwidgets.data.DefaultPresets
import com.makeshio.asciiwidgets.data.DefaultStyles
import com.makeshio.asciiwidgets.data.DefaultTemplates
import com.makeshio.asciiwidgets.data.EffectiveSettings
import com.makeshio.asciiwidgets.data.SettingsEntity
import com.makeshio.asciiwidgets.data.StyleMap
import com.makeshio.asciiwidgets.data.WidgetConfigEntity
import com.makeshio.asciiwidgets.data.WidgetKind
import com.makeshio.asciiwidgets.sources.WidgetData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewFeaturesTest {
    private val global = SettingsEntity(
        textColor = "#FFFFFFFF",
        dimColor = "#FF808080",
        use24Hour = true,
        tempUnit = "C",
        city = "Москва",
        autoLocation = false,
        frame = false,
        animationEnabled = true,
        styleMap = StyleMap.DEFAULT,
        musicSource = "com.global.player",
    )

    @Test
    fun mergeUsesGlobalValuesWithoutConfig() {
        val effective = EffectiveSettings.merge(global, null)
        assertEquals(global.textColor, effective.textColor)
        assertEquals(global.styleMap, effective.styleMap)
        assertEquals(global.musicSource, effective.musicSource)
        assertEquals(global.frame, effective.frame)
    }

    @Test
    fun mergeOverridesOnlySpecifiedWidgetValues() {
        val config = WidgetConfigEntity(
            appWidgetId = 7,
            kind = WidgetKind.MUSIC.name,
            templateId = "builtin_music",
            textColor = "#FFFF0000",
            frame = true,
            musicSource = "com.spotify.music",
        )
        val effective = EffectiveSettings.merge(global, config)
        assertEquals("#FFFF0000", effective.textColor)
        assertTrue(effective.frame)
        assertEquals("com.spotify.music", effective.musicSource)
        assertEquals(global.dimColor, effective.dimColor)
        assertEquals(global.styleMap, effective.styleMap)
        assertEquals(global.city, effective.city)
    }

    @Test
    fun hourlyTempsAreFormattedCompactly() {
        assertEquals("19°20°21°", WidgetData.formatHourlyTemps(listOf(19, 20, 21)))
        assertEquals("-1°20°", WidgetData.formatHourlyTemps(listOf(-1, 20)))
        assertEquals("", WidgetData.formatHourlyTemps(emptyList()))
    }

    @Test
    fun presetsUseOnlyKnownStyles() {
        val knownStyleIds = DefaultStyles.styles.map { it.id }.toSet()
        assertTrue(DefaultPresets.presets.size >= 5)
        DefaultPresets.presets.forEach { preset ->
            val mapping = StyleMap.decode(preset.styleMap)
            assertEquals(setOf("digits", "symbols", "letters"), mapping.keys)
            assertTrue(mapping.values.all { it in knownStyleIds })
        }
    }

    @Test
    fun everyKindHasEnoughTemplates() {
        WidgetKind.entries.forEach { kind ->
            val count = DefaultTemplates.templates.count { it.kind == kind.name }
            val minimum = if (kind == WidgetKind.ART) 1 else 2
            assertTrue("$kind has only $count template(s)", count >= minimum)
        }
    }

    @Test
    fun lookStylesUseTextRamps() {
        val byId = DefaultStyles.styles.associateBy { it.id }
        StyleMap.LOOK_IDS.forEach { id ->
            val style = byId.getValue(id)
            assertEquals("TEXT", style.shape)
            assertTrue("$id has no ramp", style.chars.length >= 2)
            assertTrue("$id has no glyphs", DefaultStyles.glyphsFor(id).isNotEmpty())
        }
        assertTrue("ASCII ramp too short", byId.getValue("ascii").chars.length >= 30)
        assertTrue("Dense ramp too short", byId.getValue("dense").chars.length >= 30)
    }

    @Test
    fun artPiecesParseFramesColorsAndPadRows() {
        val content =
            "Тестовая сцена|weather|#FF112233|#FF445566|#FF778899\nabc\nxyz\n===\n123\n"
        val piece = ArtPieces.parse("test", content)!!
        assertEquals("Тестовая сцена", piece.name)
        assertEquals("weather", piece.category)
        assertEquals("#FF112233", piece.dimColor)
        assertEquals("#FF445566", piece.mainColor)
        assertEquals("#FF778899", piece.accentColor)
        assertEquals(2, piece.frames.size)
        assertEquals(ArtPieces.ROWS, piece.frame(0).size)
        assertEquals(ArtPieces.COLS, piece.frame(0).first().length)
        assertEquals(piece.frame(1).first(), piece.frame(9).first())
    }
}
