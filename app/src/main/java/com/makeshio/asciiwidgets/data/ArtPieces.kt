package com.makeshio.asciiwidgets.data

import android.content.Context

data class ArtPiece(
    val id: String,
    val name: String,
    val category: String,
    val frames: List<List<String>>,
    /** Три цвета сцены: тусклый, основной и акцент для ярких фрагментов. */
    val dimColor: String = ArtPieces.FALLBACK_DIM,
    val mainColor: String = ArtPieces.FALLBACK_MAIN,
    val accentColor: String = ArtPieces.FALLBACK_ACCENT,
) {
    fun frame(index: Int): List<String> =
        frames[index.coerceIn(frames.indices)]
}

/**
 * Статичные кадры сцен из ASCII Magic (MIT), сохранённые в assets/art.
 * Формат файла: `Название|категория|dim|main|accent`, затем кадры 50×32,
 * разделённые `===`. Цвета подобраны из палитры каждой сцены.
 */
object ArtPieces {
    const val COLS = 50
    const val ROWS = 32
    const val DEFAULT_ID = "sleeping-cat"
    const val FALLBACK_DIM = "#FF707070"
    const val FALLBACK_MAIN = "#FFFFFFFF"
    const val FALLBACK_ACCENT = "#FFD8E8FF"
    private const val DIR = "art"
    @Volatile private var cached: List<ArtPiece>? = null

    fun load(context: Context): List<ArtPiece> = cached ?: synchronized(this) {
        cached ?: run {
            val files = context.assets.list(DIR).orEmpty().filter { it.endsWith(".txt") }
            files.mapNotNull { file ->
                runCatching {
                    val id = file.removeSuffix(".txt")
                    val content = context.assets.open("$DIR/$file").bufferedReader().use { it.readText() }
                    parse(id, content)
                }.getOrNull()
            }.sortedBy { it.name }.also { cached = it }
        }
    }

    fun parse(id: String, content: String): ArtPiece? {
        val header = content.substringBefore('\n').trim()
        val body = content.substringAfter('\n', "")
        val parts = header.split('|').map { it.trim() }
        if (parts.size < 2 || parts[0].isBlank()) return null

        val frames = body.split("===").mapNotNull { block ->
            val lines = block.trim('\n', '\r').lines().map { line ->
                line.take(COLS).padEnd(COLS, ' ')
            }.take(ROWS).toMutableList()
            while (lines.size < ROWS) lines.add(" ".repeat(COLS))
            lines.ifEmpty { null }
        }
        if (frames.isEmpty()) return null
        return ArtPiece(
            id = id,
            name = parts[0],
            category = parts[1],
            frames = frames,
            dimColor = argb(parts.getOrNull(2), FALLBACK_DIM),
            mainColor = argb(parts.getOrNull(3), FALLBACK_MAIN),
            accentColor = argb(parts.getOrNull(4), FALLBACK_ACCENT),
        )
    }

    private fun argb(raw: String?, fallback: String): String {
        val color = raw.orEmpty()
        if (color.isEmpty()) return fallback
        return if (color.length == 7 && color.startsWith("#")) "#FF${color.drop(1)}" else color
    }

    fun default(pieces: List<ArtPiece>): ArtPiece? =
        pieces.firstOrNull { it.id == DEFAULT_ID } ?: pieces.firstOrNull()
}
