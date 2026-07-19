package dev.unknownuser.ananda.style

import dev.unknownuser.ananda.backend.CornerRadii
import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.Stroke
import java.awt.Color

data class GradientStop(
    val color: Color,
    val position: Float? = null
)

sealed interface BackgroundLayer {
    data class Solid(val color: Color) : BackgroundLayer

    data class Gradient(
        val stops: List<GradientStop>,
        val direction: GradientDirection = GradientDirection.Vertical
    ) : BackgroundLayer
}

fun gradientLayer(colors: List<Color>, direction: GradientDirection = GradientDirection.Vertical): BackgroundLayer.Gradient =
    BackgroundLayer.Gradient(colors.map { GradientStop(it) }, direction)

data class BorderSides(
    val top: Stroke? = null,
    val right: Stroke? = null,
    val bottom: Stroke? = null,
    val left: Stroke? = null
) {
    fun isEmpty(): Boolean = top == null && right == null && bottom == null && left == null
}

enum class PseudoSlot {
    Before,
    After
}

enum class PseudoVisibility {
    Always,
    Hovered,
    Pressed,
    Selected,
    Enabled,
    Disabled
}

data class TransformOffset(
    val x: Float = 0f,
    val y: Float = 0f,
    val xPercent: Float? = null,
    val yPercent: Float? = null
) {
    fun resolveX(boundsWidth: Float): Float = x + (xPercent ?: 0f) * boundsWidth
    fun resolveY(boundsHeight: Float): Float = y + (yPercent ?: 0f) * boundsHeight
}

data class PseudoElement(
    val width: Float? = null,
    val height: Float? = null,
    val left: Float? = null,
    val top: Float? = null,
    val right: Float? = null,
    val bottom: Float? = null,
    val background: Color? = null,
    val gradient: List<GradientStop>? = null,
    val gradientDirection: GradientDirection = GradientDirection.Vertical,
    val radius: Float = 0f,
    val cornerRadii: CornerRadii? = null,
    val border: Stroke? = null,
    val shadow: Shadow? = null,
    val opacity: Float = 1f,
    val rotate: Float = 0f,
    val translate: TransformOffset = TransformOffset(),
    val visibility: PseudoVisibility = PseudoVisibility.Always
)

data class StateStyles(
    val hover: Style? = null,
    val pressed: Style? = null,
    val selected: Style? = null,
    val enabled: Style? = null,
    val disabled: Style? = null
)