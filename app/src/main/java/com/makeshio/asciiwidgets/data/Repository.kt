package com.makeshio.asciiwidgets.data

object Repository {
    suspend fun ensureSeeded(db: AppDatabase) {
        if (db.templateDao().count() == 0) {
            DefaultTemplates.templates.forEach { template ->
                db.templateDao().upsertTemplate(template)
                db.templateDao().insertGlyphs(DefaultTemplates.glyphsFor(template.id))
            }
        }
        if (db.settingsDao().get() == null) {
            db.settingsDao().save(SettingsEntity())
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
