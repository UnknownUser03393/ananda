package dev.unknownuser.ananda.shapes

import dev.unknownuser.ananda.backend.RenderContext
import java.awt.Color

class Line(
    override var x: Float = 0f,
    override var y: Float = 0f,
    var x2: Float = 0f,
    var y2: Float = 0f,
    override var strokeColor: Color? = Color.WHITE,
    override var strokeWidth: Float = 1f
) : Shape(x, y, true, null, strokeColor, strokeWidth) {
    override fun draw(context: RenderContext) {
        if (!visible) return
        stroke()?.let { context.backend.drawLine(x, y, x2, y2, it) }
    }

    fun from(x: Number, y: Number) = apply {
        this.x = x.toFloat()
        this.y = y.toFloat()
    }

    fun target(x: Number, y: Number) = apply {
        this.x2 = x.toFloat()
        this.y2 = y.toFloat()
    }
}
