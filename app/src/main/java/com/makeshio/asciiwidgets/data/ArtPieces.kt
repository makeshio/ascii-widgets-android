package com.makeshio.asciiwidgets.data

import android.content.Context

data class ArtPiece(
    val id: String,
    val name: String,
    val category: String,
    val frames: List<List<String>>,
) {
    fun frame(index: Int): List<String> =
        frames[index.coerceIn(frames.indices)]
}

/**
 * Статичные кадры сцен из ASCII Magic (MIT), сохранённые в assets/art.
 * Формат файла: `Название|категория`, затем кадры 30×18, разделённые `===`.
 */
object ArtPieces {
    const val COLS = 30
    const val ROWS = 18
    const val DEFAULT_ID = "sleeping-cat"
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
        val parts = header.split('|', limit = 2)
        if (parts.size != 2 || parts[0].isBlank()) return null

        val frames = body.split("===").mapNotNull { block ->
            val lines = block.trim('\n', '\r').lines().map { line ->
                line.take(COLS).padEnd(COLS, ' ')
            }.take(ROWS).toMutableList()
            while (lines.size < ROWS) lines.add(" ".repeat(COLS))
            lines.ifEmpty { null }
        }
        if (frames.isEmpty()) return null
        return ArtPiece(id, parts[0], parts[1], frames)
    }

    fun default(pieces: List<ArtPiece>): ArtPiece? =
        pieces.firstOrNull { it.id == DEFAULT_ID } ?: pieces.firstOrNull()
}
