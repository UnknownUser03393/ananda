package dev.unknownuser.ananda.shapes

import dev.unknownuser.ananda.backend.RenderContext
import java.awt.Color

class Rectangle(
    override var x: Float = 0f,
    override var y: Float = 0f,
    var width: Float = 0f,
    var height: Float = 0f,
    override var fillColor: Color? = null,
    override var strokeColor: Color? = Color.WHITE,
    override var strokeWidth: Float = 1f
) : Shape(x, y, true, fillColor, strokeColor, strokeWidth) {
    override fun draw(context: RenderContext) {
        if (!visible) return
        context.backend.drawRect(x, y, width, height, fillColor, stroke())
    }

    fun at(x: Number, y: Number) = apply {
        this.x = x.toFloat()
        this.y = y.toFloat()
    }

    fun size(width: Number, height: Number) = apply {
        this.width = width.toFloat()
        this.height = height.toFloat()
    }

    fun wh(width: Number, height: Number) = size(width, height)
}
