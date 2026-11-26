package com.makeshio.asciiwidgets.data

data class StyleRenderData(
    val shape: String,
    val fill: Float,
    val glyphs: Map<String, List<String>>,
    val chars: String = "",
)

object Repository {
    suspend fun ensureSeeded(db: AppDatabase) {
        // Досыпаем недостающие встроенные шаблоны, не перезаписывая пользовательские правки.
        DefaultTemplates.templates.forEach { template ->
            if (db.templateDao().byId(template.id) == null) {
                db.templateDao().upsertTemplate(template)
                db.templateDao().insertGlyphs(DefaultTemplates.glyphsFor(template.id))
            }
        }
        DefaultStyles.styles.forEach { style ->
            if (db.styleDao().byId(style.id) == null) {
                db.styleDao().insertStyles(listOf(style))
                db.styleDao().insertGlyphs(DefaultStyles.glyphsFor(style.id))
            }
        }
        DefaultPresets.presets.forEach { preset ->
            if (db.presetDao().byId(preset.id) == null) {
                db.presetDao().insert(listOf(preset))
            }
        }
        if (db.settingsDao().get() == null) {
            db.settingsDao().save(SettingsEntity())
        }
    }

    suspend fun effective(db: AppDatabase, appWidgetId: Int): EffectiveSettings {
        ensureSeeded(db)
        val global = db.settingsDao().get() ?: SettingsEntity()
        return EffectiveSettings.merge(global, db.widgetConfigDao().get(appWidgetId))
    }

    suspend fun styleMap(db: AppDatabase): Map<String, StyleRenderData> {
        val entities = db.styleDao().allOnce()
        return entities.associate { style ->
            style.id to StyleRenderData(
                shape = style.shape,
                fill = style.fill,
                chars = style.chars,
                glyphs = db.styleDao().glyphs(style.id).associate { glyph ->
                    glyph.symbol to glyph.rows.split("\n")
                },
            )
        }
    }

    suspend fun glyphMap(db: AppDatabase, templateId: String): Map<String, List<String>> =
        db.templateDao().glyphsOnce(templateId).associate { entity ->
            entity.symbol to entity.rows.split("\n")
        }

    suspend fun templateOr(db: AppDatabase, id: String?, kind: WidgetKind): TemplateEntity =
        (id?.let { db.templateDao().byId(it) } ?: db.templateDao().byId(DefaultTemplates.defaultTemplate(kind).id))
            ?: DefaultTemplates.defaultTemplate(kind)
}
