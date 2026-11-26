package com.makeshio.asciiwidgets.data

object DefaultTemplates {
    val templates: List<TemplateEntity> = listOf(
        TemplateEntity("builtin_clock", "Часы — классика 4×2", WidgetKind.CLOCK.name, 30, 12, true),
        TemplateEntity("builtin_clock_compact", "Часы — компакт 4×1", WidgetKind.CLOCK.name, 30, 6, true),
        TemplateEntity("builtin_clock_large", "Часы — крупные 4×3", WidgetKind.CLOCK.name, 30, 18, true),
        TemplateEntity("builtin_weather", "Погода 2×2", WidgetKind.WEATHER.name, 30, 12, true),
        TemplateEntity("builtin_weather_hourly", "Погода — по часам 4×2", WidgetKind.WEATHER.name, 54, 12, true),
        TemplateEntity("builtin_battery", "Батарея и статусы 2×2", WidgetKind.BATTERY.name, 30, 12, true),
        TemplateEntity("builtin_battery_min", "Батарея — компакт", WidgetKind.BATTERY.name, 30, 6, true),
        TemplateEntity("builtin_music", "Музыка 4×1", WidgetKind.MUSIC.name, 42, 6, true),
        TemplateEntity("builtin_music_wide", "Музыка — широкий заголовок", WidgetKind.MUSIC.name, 54, 6, true),
    )

    fun glyphsFor(templateId: String): List<GlyphEntity> =
        DefaultGlyphs.symbols.keys.map { symbol ->
            GlyphEntity(templateId, symbol, DefaultGlyphs.storedRows(symbol))
        }

    fun defaultTemplate(kind: WidgetKind): TemplateEntity =
        templates.first { it.kind == kind.name }

    fun byId(id: String): TemplateEntity? = templates.firstOrNull { it.id == id }
}
