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
    )

    fun default(): PresetEntity = presets.first()
}
