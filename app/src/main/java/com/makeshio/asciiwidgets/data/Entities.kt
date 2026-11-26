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
    CLOCK, WEATHER, BATTERY, MUSIC, ART;

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
    /** null означает «использовать глобальную настройку». */
    val styleMap: String? = null,
    val textColor: String? = null,
    val dimColor: String? = null,
    val frame: Boolean? = null,
    val animationEnabled: Boolean? = null,
    val presetId: String? = null,
    val musicSource: String? = null,
    /** Идентификатор сцены из assets/art — только для WidgetKind.ART. */
    val artPieceId: String? = null,
    /** Отдельный «лук» арт-виджета; null → Density ASCII. */
    val artLookId: String? = null,
)

@Entity(tableName = "styles")
data class StyleEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** RECT — прямоугольники, DOT — точки, TEXT — символы из chars. */
    val shape: String,
    val fill: Float,
    /** Набор символов от светлого к тёмному для TEXT-стилей. */
    val chars: String = "",
)

@Entity(tableName = "style_glyphs", primaryKeys = ["styleId", "symbol"])
data class StyleGlyphEntity(
    val styleId: String,
    val symbol: String,
    val rows: String,
)

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val styleMap: String,
    val textColor: String,
    val dimColor: String,
    val frame: Boolean,
    val animationEnabled: Boolean,
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
    val styleMap: String = StyleMap.DEFAULT,
    val musicSource: String = "",
)

/** Простой формат: "digits=classic;symbols=dot;letters=slim" — без внешнего JSON-парсера. */
object StyleMap {
    const val DEFAULT = "digits=classic;symbols=dot;letters=slim"
    private const val DIGITS = "digits"
    private const val SYMBOLS = "symbols"
    private const val LETTERS = "letters"

    fun decode(value: String?): Map<String, String> {
        val result = mutableMapOf(
            DIGITS to "classic",
            SYMBOLS to "dot",
            LETTERS to "slim",
        )
        value.orEmpty().split(';').forEach { part ->
            val key = part.substringBefore('=').trim()
            val style = part.substringAfter('=').trim()
            if (key.isNotEmpty() && style.isNotEmpty()) result[key] = style
        }
        return result
    }

    fun encode(map: Map<String, String>): String = listOf(DIGITS, SYMBOLS, LETTERS)
        .joinToString(";") { "$it=${map[it] ?: DEFAULT_STYLES.getValue(it)}" }

    fun styleFor(char: Char, map: Map<String, String>): String = when {
        char.isDigit() -> map[DIGITS] ?: "classic"
        char.isLetter() -> map[LETTERS] ?: "slim"
        else -> map[SYMBOLS] ?: "dot"
    }

    private val DEFAULT_STYLES = mapOf(DIGITS to "classic", SYMBOLS to "dot", LETTERS to "slim")

    /** Стили, пришедшие из «луков» ASCII Magic: рисуются настоящими символами. */
    val LOOK_IDS = listOf("dots", "ascii", "dense", "binary", "numbers")

    /** Арт по умолчанию рисуется символами, а не прямоугольниками. */
    const val DEFAULT_ART_LOOK = "dense"
}

/** Настройки, реально применённые к виджету: глобальные значения + переопределения виджета. */
data class EffectiveSettings(
    val textColor: String,
    val dimColor: String,
    val use24Hour: Boolean,
    val tempUnit: String,
    val city: String,
    val autoLocation: Boolean,
    val frame: Boolean,
    val animationEnabled: Boolean,
    val styleMap: String,
    val musicSource: String,
) {
    companion object {
        fun merge(global: SettingsEntity, config: WidgetConfigEntity?): EffectiveSettings = EffectiveSettings(
            textColor = config?.textColor ?: global.textColor,
            dimColor = config?.dimColor ?: global.dimColor,
            use24Hour = global.use24Hour,
            tempUnit = global.tempUnit,
            city = global.city,
            autoLocation = global.autoLocation,
            frame = config?.frame ?: global.frame,
            animationEnabled = config?.animationEnabled ?: global.animationEnabled,
            styleMap = config?.styleMap ?: global.styleMap,
            musicSource = config?.musicSource ?: global.musicSource,
        )
    }
}

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
interface StyleDao {
    @Query("SELECT * FROM styles ORDER BY name")
    fun all(): Flow<List<StyleEntity>>

    @Query("SELECT * FROM styles")
    suspend fun allOnce(): List<StyleEntity>

    @Query("SELECT * FROM styles WHERE id = :id")
    suspend fun byId(id: String): StyleEntity?

    @Query("SELECT * FROM style_glyphs WHERE styleId = :styleId")
    suspend fun glyphs(styleId: String): List<StyleGlyphEntity>

    @Query("SELECT COUNT(*) FROM styles")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStyles(styles: List<StyleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlyphs(glyphs: List<StyleGlyphEntity>)
}

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY name")
    fun flow(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets")
    suspend fun allOnce(): List<PresetEntity>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun byId(id: String): PresetEntity?

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(presets: List<PresetEntity>)
}

@Dao
interface WidgetConfigDao {
    @Query("SELECT * FROM widget_configs WHERE appWidgetId = :appWidgetId")
    suspend fun get(appWidgetId: Int): WidgetConfigEntity?

    @Query("SELECT * FROM widget_configs")
    fun flow(): Flow<List<WidgetConfigEntity>>

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
