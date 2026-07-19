package dev.unknownuser.ananda.shapes

import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.draw.Drawable
import java.awt.Color

abstract class Shape(
    open var x: Float = 0f,
    open var y: Float = 0f,
    open var visible: Boolean = true,
    open var fillColor: Color? = Color.WHITE,
    open var strokeColor: Color? = null,
    open var strokeWidth: Float = 0f
) : Drawable {
    protected fun stroke(): Stroke? =
        strokeColor?.takeIf { strokeWidth > 0f }?.let { color -> Stroke(color, strokeWidth) }
}
