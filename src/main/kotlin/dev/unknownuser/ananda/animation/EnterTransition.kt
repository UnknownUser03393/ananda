package dev.unknownuser.ananda.animation

data class EnterTransition(
    val slideX: Float = 0f,
    val slideY: Float = 12f,
    val initialAlpha: Float = 0f,
    val initialScale: Float = 0.98f,
    val durationSeconds: Float = 0.28f,
    val delaySeconds: Float = 0f,
    val easing: Easing = Easings.EaseOutCubic
)

data class ExitTransition(
    val slideX: Float = 0f,
    val slideY: Float = -8f,
    val finalAlpha: Float = 0f,
    val finalScale: Float = 0.98f,
    val durationSeconds: Float = 0.2f,
    val delaySeconds: Float = 0f,
    val easing: Easing = Easings.EaseIn
)

internal data class EnterFrame(
    val translateX: Float = 0f,
    val translateY: Float = 0f,
    val alpha: Float = 1f,
    val scale: Float = 1f,
    val running: Boolean = false
)
