package com.makeshio.asciiwidgets.data

/** Маленькие шутки в строке состояния арт-виджета. Только ASCII, без эмодзи. */
object SceneQuips {
    private val PER_SCENE = mapOf(
        "sleeping-cat" to listOf("cat --sleep", "zzz: 8 paws", "purr level: MAX"),
        "matrix-rain" to listOf("wake up, neo", "grep -r redpill .", "follow the rabbit"),
        "moonlit-sea" to listOf("tide --calm", "moon: 93%", "waves: ok"),
        "northern-lights" to listOf("aurora --on", "sky: green", "geomagnetic kp=5"),
        "campfire" to listOf("fire --warm", "+1 marshmallow", "smoke: ok"),
        "snowfall" to listOf("flake -n 1024", "temp: -3C", "silent mode"),
        "rotating-cube" to listOf("spin --cube", "axis: xyz", "fps: 1 (on purpose)"),
        "lit-sphere" to listOf("light --sphere", "normal: ok", "shade: 42%"),
        "black-hole" to listOf("404: light missing", "horizon: stable", "spaghettify --safe"),
        "rocket-launch" to listOf("3..2..1.. ^", "liftoff!", "orbit: circular"),
        "fireworks" to listOf("boom -n 12", "sparks: 99", "sky: confetti"),
        "plasma" to listOf("ionize --all", "temp: 1e6K", "magnetic: stable"),
        "synthwave-grid" to listOf("grid --retro", "1984: online", "neon: on"),
        "led-matrix-scroller" to listOf("scroll --led", "hello, world!", "pixels: 120x20"),
        "rain-shower" to listOf("rain --wet", "umbrella: off", "puddles: 7"),
        "aurora-curtains" to listOf("curtain --open", "sky: violet", "no sunglasses"),
        "tunnel" to listOf("tunnel --depth 9", "light: maybe", "echo: on"),
        "lorenz-attractor" to listOf("butterfly --effect", "chaos: nominal", "delta: 0.001"),
        "game-of-life" to listOf("generation: 128", "cells: alive", "glider detected"),
        "sierpinski" to listOf("triangle -r 8", "fractal: ok", "recursion: deep"),
        "wireframe-globe" to listOf("globe --wire", "lon/lat: ok", "spin: slow"),
        "wave-interference" to listOf("wave --sum", "nodes: 12", "phase: 0.3"),
        "heart-3d" to listOf("heart --beat", "bpm: 72", "love: true"),
        "owl-blink" to listOf("owl --blink", "who goes there?", "night: watch"),
    )
    private val GENERIC = listOf("ready", "scan...", "ok")

    fun command(sceneId: String): String = "./$sceneId.asc"

    fun status(sceneId: String, frame: Int): String {
        val lines = PER_SCENE[sceneId] ?: GENERIC
        return "> " + lines[frame.coerceAtLeast(0) % lines.size]
    }
}
