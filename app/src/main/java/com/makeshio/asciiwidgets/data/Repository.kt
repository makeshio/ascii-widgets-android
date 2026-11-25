package com.makeshio.asciiwidgets.data

data class StyleRenderData(
    val shape: String,
    val fill: Float,
    val glyphs: Map<String, List<String>>,
)

object Repository {
    suspend fun ensureSeeded(db: AppDatabase) {
        if (db.templateDao().count() == 0) {
            DefaultTemplates.templates.forEach { template ->
                db.templateDao().upsertTemplate(template)
                db.templateDao().insertGlyphs(DefaultTemplates.glyphsFor(template.id))
            }
        }
        if (db.styleDao().count() == 0) {
            DefaultStyles.styles.forEach { style ->
                db.styleDao().insertStyles(listOf(style))
                db.styleDao().insertGlyphs(DefaultStyles.glyphsFor(style.id))
            }
        }
        if (db.settingsDao().get() == null) {
            db.settingsDao().save(SettingsEntity())
        }
    }

    suspend fun styleMap(db: AppDatabase): Map<String, StyleRenderData> {
        val entities = db.styleDao().allOnce()
        return entities.associate { style ->
            style.id to StyleRenderData(
                shape = style.shape,
                fill = style.fill,
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
