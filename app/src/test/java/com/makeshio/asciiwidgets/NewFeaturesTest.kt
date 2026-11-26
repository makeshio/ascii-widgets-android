package com.makeshio.asciiwidgets

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
    fun everyKindHasAtLeastTwoTemplates() {
        WidgetKind.entries.forEach { kind ->
            val count = DefaultTemplates.templates.count { it.kind == kind.name }
            assertTrue("$kind has only $count template(s)", count >= 2)
        }
    }
}
