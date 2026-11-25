package com.makeshio.asciiwidgets.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

enum class WidgetKind {
    CLOCK, WEATHER, BATTERY, MUSIC;

    companion object {
        fun from(value: String?): WidgetKind = entries.firstOrNull { it.name == value } ?: CLOCK
    }
}

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val cols: Int,
    val rows: Int,
    val isBuiltIn: Boolean,
)

@Entity(tableName = "glyphs", primaryKeys = ["templateId", "symbol"])
data class GlyphEntity(
    val templateId: String,
    val symbol: String,
    val rows: String,
)

@Entity(tableName = "widget_configs")
data class WidgetConfigEntity(
    @PrimaryKey val appWidgetId: Int,
    val kind: String,
    val templateId: String,
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val textColor: String = "#FFFFFFFF",
    val dimColor: String = "#FF808080",
    val use24Hour: Boolean = true,
    val tempUnit: String = "C",
    val city: String = "Москва",
    val autoLocation: Boolean = false,
    val frame: Boolean = false,
    val animationEnabled: Boolean = true,
)

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY kind, name")
    fun all(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE kind = :kind ORDER BY name")
    fun byKind(kind: String): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun byId(id: String): TemplateEntity?

    @Query("SELECT * FROM glyphs WHERE templateId = :templateId")
    fun glyphs(templateId: String): Flow<List<GlyphEntity>>

    @Query("SELECT * FROM glyphs WHERE templateId = :templateId")
    suspend fun glyphsOnce(templateId: String): List<GlyphEntity>

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertTemplate(template: TemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlyphs(glyphs: List<GlyphEntity>)

    @Query("DELETE FROM glyphs WHERE templateId = :templateId")
    suspend fun deleteGlyphs(templateId: String)

    @Query("DELETE FROM templates WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteUserTemplate(id: String)
}

@Dao
interface WidgetConfigDao {
    @Query("SELECT * FROM widget_configs WHERE appWidgetId = :appWidgetId")
    suspend fun get(appWidgetId: Int): WidgetConfigEntity?

    @Query("SELECT * FROM widget_configs")
    suspend fun all(): List<WidgetConfigEntity>

    @Upsert
    suspend fun upsert(config: WidgetConfigEntity)

    @Query("DELETE FROM widget_configs WHERE appWidgetId = :appWidgetId")
    suspend fun delete(appWidgetId: Int)

    @Query("DELETE FROM widget_configs WHERE appWidgetId IN (:ids)")
    suspend fun deleteAll(ids: IntArray)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun flow(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun get(): SettingsEntity?

    @Upsert
    suspend fun save(settings: SettingsEntity)
}
