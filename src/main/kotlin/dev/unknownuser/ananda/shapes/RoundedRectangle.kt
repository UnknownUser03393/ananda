package dev.unknownuser.ananda.shapes

import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.SkiaRenderBackend
import org.jetbrains.skia.PathBuilder
import java.awt.Color
import kotlin.math.min

enum class CornerContinuity {
    Position,
    Tangent,
    Curvature
}

class RoundedRectangle(
    override var x: Float = 0f,
    override var y: Float = 0f,
    var width: Float = 0f,
    var height: Float = 0f,
    var radius: Float = 0f,
    var continuity: CornerContinuity = CornerContinuity.Tangent,
    override var fillColor: Color? = Color.WHITE,
    override var strokeColor: Color? = null,
    override var strokeWidth: Float = 0f
) : Shape(x, y, true, fillColor, strokeColor, strokeWidth) {
    override fun draw(context: RenderContext) {
        if (!visible) return
        require(radius <= min(width, height) / 2f) {
            "Radius must be <= min(width, height) / 2, actual: $radius"
        }
        when (continuity) {
            CornerContinuity.Position, CornerContinuity.Tangent -> drawRRect(context)
            CornerContinuity.Curvature -> drawCurvaturePath(context)
        }
    }

    private fun drawRRect(context: RenderContext) {
        context.backend.drawRoundedRect(x, y, width, height, radius, fillColor, stroke())
    }

    private fun drawCurvaturePath(context: RenderContext) {
        val skia = context.backend as? SkiaRenderBackend
        if (skia == null) {
            drawRRect(context)
            return
        }
        val r = radius
        val c = r * 0.55228475f
        val path = PathBuilder().apply {
            moveTo(x + r, y)
            lineTo(x + width - r, y)
            cubicTo(x + width - r + c, y, x + width, y + r - c, x + width, y + r)
            lineTo(x + width, y + height - r)
            cubicTo(x + width, y + height - r + c, x + width - r + c, y + height, x + width - r, y + height)
            lineTo(x + r, y + height)
            cubicTo(x + r - c, y + height, x, y + height - r + c, x, y + height - r)
            lineTo(x, y + r)
            cubicTo(x, y + r - c, x + r - c, y, x + r, y)
            closePath()
        }.detach()
        skia.drawPath(path, fillColor, stroke())
        path.close()
    }

    fun at(x: Number, y: Number) = apply {
        this.x = x.toFloat()
        this.y = y.toFloat()
    }

    fun size(width: Number, height: Number) = apply {
        this.width = width.toFloat()
        this.height = height.toFloat()
    }

    fun radii(radius: Number) = apply {
        this.radius = radius.toFloat()
    }
}
