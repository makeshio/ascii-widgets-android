package com.makeshio.asciiwidgets.data

object DefaultTemplates {
    val templates: List<TemplateEntity> = listOf(
        TemplateEntity("builtin_clock", "Классические часы", WidgetKind.CLOCK.name, 30, 12, true),
        TemplateEntity("builtin_weather", "Погода", WidgetKind.WEATHER.name, 30, 12, true),
        TemplateEntity("builtin_battery", "Батарея и статусы", WidgetKind.BATTERY.name, 30, 12, true),
        TemplateEntity("builtin_music", "Музыка", WidgetKind.MUSIC.name, 42, 6, true),
    )

    fun glyphsFor(templateId: String): List<GlyphEntity> =
        DefaultGlyphs.symbols.keys.map { symbol ->
            GlyphEntity(templateId, symbol, DefaultGlyphs.storedRows(symbol))
        }

    fun defaultTemplate(kind: WidgetKind): TemplateEntity =
        templates.first { it.kind == kind.name }
}
