package com.makeshio.asciiwidgets.data

object DefaultPresets {
    private fun styles(digits: String, symbols: String, letters: String) =
        "digits=$digits;symbols=$symbols;letters=$letters"

    val presets: List<PresetEntity> = listOf(
        PresetEntity(
            "classic",
            "Классика",
            styles("classic", "dot", "slim"),
            "#FFFFFFFF",
            "#FF808080",
            frame = false,
            animationEnabled = true,
        ),
        PresetEntity(
            "dots",
            "Точки",
            styles("dot", "dot", "dot"),
            "#FFFFFFFF",
            "#FF707070",
            frame = false,
            animationEnabled = true,
        ),
        PresetEntity(
            "slim_neon",
            "Тонкий неон",
            styles("slim", "dot", "classic"),
            "#FFD8E8FF",
            "#FF4A6FA5",
            frame = false,
            animationEnabled = true,
        ),
        PresetEntity(
            "minimal",
            "Минимум",
            styles("slim", "slim", "slim"),
            "#FFC0C0C0",
            "#FF606060",
            frame = false,
            animationEnabled = false,
        ),
        PresetEntity(
            "contrast",
            "Контраст",
            styles("classic", "classic", "classic"),
            "#FFFFFFFF",
            "#FF909090",
            frame = true,
            animationEnabled = true,
        ),
        PresetEntity(
            "ascii_dense",
            "Плотный ASCII",
            styles("dense", "ascii", "ascii"),
            "#FFE8FFF4",
            "#FF66887A",
            frame = false,
            animationEnabled = true,
        ),
        PresetEntity(
            "blocks_mono",
            "Блоки",
            styles("blocks", "blocks", "blocks"),
            "#FFB8FFE0",
            "#FF4E7A68",
            frame = false,
            animationEnabled = true,
        ),
        PresetEntity(
            "binary_matrix",
            "Двоичный",
            styles("binary", "dot", "numbers"),
            "#FF9CFFB0",
            "#FF3A6A42",
            frame = true,
            animationEnabled = true,
        ),
    )

    fun default(): PresetEntity = presets.first()
}
