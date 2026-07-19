package dev.unknownuser.ananda.shapes

import dev.unknownuser.ananda.backend.RenderContext
import java.awt.Color

class Circle(
    override var x: Float = 0f,
    override var y: Float = 0f,
    var radius: Float = 0f,
    override var fillColor: Color? = Color.WHITE,
    override var strokeColor: Color? = null,
    override var strokeWidth: Float = 0f
) : Shape(x, y, true, fillColor, strokeColor, strokeWidth) {
    override fun draw(context: RenderContext) {
        if (!visible) return
        context.backend.drawCircle(x, y, radius, fillColor, stroke())
    }

    fun center(x: Number, y: Number) = apply {
        this.x = x.toFloat()
        this.y = y.toFloat()
    }

    fun radius(radius: Number) = apply {
        this.radius = radius.toFloat()
    }
}
