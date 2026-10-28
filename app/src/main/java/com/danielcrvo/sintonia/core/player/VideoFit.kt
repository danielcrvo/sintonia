package com.danielcrvo.sintonia.core.player

enum class VideoFit(val ratio: Float?) {
    Fit(null),

    Zoom(null),

    Stretch(null),

    Wide(16f / 9f),

    Classic(4f / 3f),
    ;

    fun next(): VideoFit = entries[(ordinal + 1) % entries.size]
}
