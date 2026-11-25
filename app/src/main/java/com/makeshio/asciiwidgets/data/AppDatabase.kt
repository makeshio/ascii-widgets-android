package com.makeshio.asciiwidgets.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TemplateEntity::class,
        GlyphEntity::class,
        WidgetConfigEntity::class,
        SettingsEntity::class,
        StyleEntity::class,
        StyleGlyphEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao
    abstract fun widgetConfigDao(): WidgetConfigDao
    abstract fun settingsDao(): SettingsDao
    abstract fun styleDao(): StyleDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN animationEnabled INTEGER NOT NULL DEFAULT 1",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS styles (" +
                        "id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, " +
                        "shape TEXT NOT NULL, fill REAL NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS style_glyphs (" +
                        "styleId TEXT NOT NULL, symbol TEXT NOT NULL, rows TEXT NOT NULL, " +
                        "PRIMARY KEY(styleId, symbol))",
                )
                db.execSQL(
                    "ALTER TABLE settings ADD COLUMN styleMap TEXT NOT NULL " +
                        "DEFAULT 'digits=classic;symbols=dot;letters=slim'",
                )
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "ascii_widgets.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
    }
}
