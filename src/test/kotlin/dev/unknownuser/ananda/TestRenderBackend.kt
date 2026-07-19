package dev.unknownuser.ananda

import dev.unknownuser.ananda.backend.FramebufferRegion
import dev.unknownuser.ananda.backend.CornerRadii
import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.RenderBackend
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextStyle
import dev.unknownuser.ananda.backend.TextureRegion
import java.awt.Color

internal fun testMeasureTextWidth(text: String, style: TextStyle): Float =
    text.length * style.size * 0.55f + style.letterSpacing * (text.codePointCount(0, text.length) - 1).coerceAtLeast(0)

class TestRenderBackend : RenderBackend {
    val texts = mutableListOf<TextDraw>()
    val lines = mutableListOf<LineDraw>()
    val rects = mutableListOf<RectDraw>()
    val roundedRects = mutableListOf<RoundedRectDraw>()
    val shadows = mutableListOf<ShadowDraw>()
    val gradients = mutableListOf<GradientDraw>()
    val frostedRects = mutableListOf<FrostedDraw>()
    val textures = mutableListOf<TextureDraw>()
    val framebuffers = mutableListOf<FramebufferDraw>()

    private var translateX = 0f
    private var translateY = 0f

    override fun clear(argb: Int) = Unit

    override fun drawRect(x: Float, y: Float, width: Float, height: Float, fill: Color?, stroke: Stroke?) {
        rects += RectDraw(translateX + x, translateY + y, width, height, fill, stroke)
    }

    override fun drawRoundedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        fill: Color?,
        stroke: Stroke?
    ) {
        roundedRects += RoundedRectDraw(translateX + x, translateY + y, width, height, CornerRadii.all(radius), fill, stroke)
    }

    override fun drawRoundedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radii: CornerRadii,
        fill: Color?,
        stroke: Stroke?
    ) {
        roundedRects += RoundedRectDraw(translateX + x, translateY + y, width, height, radii, fill, stroke)
    }

    override fun drawCircle(x: Float, y: Float, radius: Float, fill: Color?, stroke: Stroke?) = Unit

    override fun measureText(text: String, style: TextStyle): Pair<Float, Float> =
        testMeasureTextWidth(text, style) to style.size * 1.4f

    override fun drawRoundedGradientStops(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        stops: List<Pair<Color, Float?>>,
        direction: GradientDirection,
        stroke: Stroke?
    ) {
        gradients += GradientDraw(translateX + x, translateY + y, width, height, radius, stops, direction, stroke)
        super.drawRoundedGradientStops(x, y, width, height, radius, stops, direction, stroke)
    }

    override fun drawFrostedRoundedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        blurRadius: Float,
        tint: Color,
        stroke: Stroke?
    ) {
        frostedRects += FrostedDraw(translateX + x, translateY + y, width, height, radius, blurRadius, tint, stroke)
        super.drawFrostedRoundedRect(x, y, width, height, radius, blurRadius, tint, stroke)
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, stroke: Stroke) {
        lines += LineDraw(translateX + x1, translateY + y1, translateX + x2, translateY + y2, stroke)
    }

    override fun drawText(text: String, x: Float, y: Float, style: TextStyle) {
        texts += TextDraw(text, translateX + x, translateY + y, style)
    }

    override fun drawTexture(texture: TextureRegion, x: Float, y: Float, width: Float, height: Float, alpha: Float) {
        textures += TextureDraw(texture, translateX + x, translateY + y, width, height, alpha)
    }

    override fun drawFramebuffer(framebuffer: FramebufferRegion, x: Float, y: Float, width: Float, height: Float, alpha: Float) {
        framebuffers += FramebufferDraw(framebuffer, translateX + x, translateY + y, width, height, alpha)
    }

    override fun drawShadowedRoundedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        shadow: Shadow,
        fill: Color?,
        stroke: Stroke?
    ) {
        shadows += ShadowDraw(translateX + x, translateY + y, width, height, radius, shadow, fill, stroke)
        super.drawShadowedRoundedRect(x, y, width, height, radius, shadow, fill, stroke)
    }

    override fun translated(x: Float, y: Float, block: () -> Unit) {
        val previousX = translateX
        val previousY = translateY
        translateX += x
        translateY += y
        try {
            block()
        } finally {
            translateX = previousX
            translateY = previousY
        }
    }
}

data class TextDraw(
    val text: String,
    val x: Float,
    val y: Float,
    val style: TextStyle
)

data class LineDraw(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val stroke: Stroke
)

data class RectDraw(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val fill: Color?,
    val stroke: Stroke?
)

data class RoundedRectDraw(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val radii: CornerRadii,
    val fill: Color?,
    val stroke: Stroke?
)

data class TextureDraw(
    val texture: TextureRegion,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val alpha: Float
)

data class FramebufferDraw(
    val framebuffer: FramebufferRegion,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val alpha: Float
)

data class ShadowDraw(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val radius: Float,
    val shadow: Shadow,
    val fill: Color?,
    val stroke: Stroke?
)

data class GradientDraw(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val radius: Float,
    val stops: List<Pair<Color, Float?>>,
    val direction: GradientDirection,
    val stroke: Stroke?
)

data class FrostedDraw(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val radius: Float,
    val blurRadius: Float,
    val tint: Color,
    val stroke: Stroke?
)