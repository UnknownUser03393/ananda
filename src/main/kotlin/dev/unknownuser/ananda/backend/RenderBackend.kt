package dev.unknownuser.ananda.backend

import dev.unknownuser.ananda.theme.Theme
import dev.unknownuser.ananda.theme.DefaultFontFamily
import dev.unknownuser.ananda.time.TimeFrame
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

data class Stroke(
    val color: Color,
    val width: Float = 1f
)

data class TextStyle(
    val size: Float = 18f,
    val color: Color = Color.WHITE,
    val fontFamily: String = DefaultFontFamily,
    val fontWeight: Int = 400,
    val letterSpacing: Float = 0f
)

enum class GradientDirection {
    Horizontal,
    Vertical,
    DiagonalDown,
    DiagonalUp
}

data class Shadow(
    val color: Color = Color(0, 0, 0, 120),
    val blurRadius: Float = 18f,
    val offsetX: Float = 0f,
    val offsetY: Float = 6f,
    val spread: Float = 0f,
    val inset: Boolean = false
)

data class CornerRadii(
    val topLeft: Float = 0f,
    val topRight: Float = topLeft,
    val bottomRight: Float = topLeft,
    val bottomLeft: Float = topLeft
) {
    val isUniform: Boolean
        get() = topLeft == topRight && topLeft == bottomRight && topLeft == bottomLeft

    fun coerceAtLeast(minimum: Float): CornerRadii =
        CornerRadii(
            topLeft.coerceAtLeast(minimum),
            topRight.coerceAtLeast(minimum),
            bottomRight.coerceAtLeast(minimum),
            bottomLeft.coerceAtLeast(minimum)
        )

    companion object {
        fun all(radius: Number) = CornerRadii(radius.toFloat())
    }
}

data class TextureRegion(
    val id: String,
    val u: Float = 0f,
    val v: Float = 0f,
    val width: Float = 1f,
    val height: Float = 1f
)

fun texture(id: String): TextureRegion = TextureRegion(id)

enum class ImageFit {
    Fill,
    Contain,
    Cover
}

data class ImagePosition(
    val pixels: Float? = null,
    val percent: Float? = null
) {
    fun resolve(available: Float, drawSize: Float): Float =
        pixels ?: (available - drawSize) * (percent ?: 0.5f)

    companion object {
        val Start = percent(0f)
        val Center = percent(0.5f)
        val End = percent(1f)

        fun pixels(value: Number) = ImagePosition(pixels = value.toFloat())
        fun percent(value: Number) = ImagePosition(percent = value.toFloat())
    }
}

data class FramebufferRegion(
    val id: String,
    val colorAttachment: Int,
    val width: Int,
    val height: Int,
    val u: Float = 0f,
    val v: Float = 0f,
    val regionWidth: Float = 1f,
    val regionHeight: Float = 1f,
    val flipY: Boolean = true
)

data class ChartPoint(
    val x: Float,
    val y: Float
)

data class RenderContext(
    val backend: RenderBackend,
    val width: Int = 0,
    val height: Int = 0,
    val nanoTime: Long = 0L,
    val theme: Theme = Theme.Default,
    val time: TimeFrame = TimeFrame(),
    val scaleFactor: Float = 1f,
    val uiScale: Float = 1f
) {
    fun withTheme(theme: Theme): RenderContext = copy(theme = theme)
}

interface RenderBackend {
    fun clear(argb: Int)

    fun drawRect(x: Float, y: Float, width: Float, height: Float, fill: Color? = null, stroke: Stroke? = null)

    fun drawRoundedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        fill: Color? = null,
        stroke: Stroke? = null
    )

    fun drawRoundedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radii: CornerRadii,
        fill: Color? = null,
        stroke: Stroke? = null
    ) {
        drawRoundedRect(x, y, width, height, radii.topLeft, fill, stroke)
    }

    fun drawFrostedRoundedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        blurRadius: Float = 20f,
        tint: Color = Color(12, 12, 16, 210),
        stroke: Stroke? = Stroke(Color(255, 255, 255, 42), 0.8f)
    ) {
        drawRoundedRect(x, y, width, height, radius, tint, stroke)
    }

    fun drawCircle(x: Float, y: Float, radius: Float, fill: Color? = null, stroke: Stroke? = null)

    fun drawArc(x: Float, y: Float, radius: Float, startAngle: Float, sweepAngle: Float, stroke: Stroke) {
        val segments = (kotlin.math.abs(sweepAngle) * radius / 3f).toInt().coerceIn(12, 128)
        var previousX = x + cos(startAngle) * radius
        var previousY = y + sin(startAngle) * radius
        for (index in 1..segments) {
            val angle = startAngle + sweepAngle * index / segments
            val nextX = x + cos(angle) * radius
            val nextY = y + sin(angle) * radius
            drawLine(previousX, previousY, nextX, nextY, stroke)
            previousX = nextX
            previousY = nextY
        }
    }

    fun drawRoundedGradientRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        colors: List<Color>,
        direction: GradientDirection = GradientDirection.Horizontal,
        stroke: Stroke? = null
    ) {
        drawRoundedRect(x, y, width, height, radius, colors.firstOrNull(), stroke)
    }

    fun drawRoundedGradientStops(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        stops: List<Pair<Color, Float?>>,
        direction: GradientDirection = GradientDirection.Horizontal,
        stroke: Stroke? = null
    ) {
        drawRoundedGradientRect(x, y, width, height, radius, stops.map { it.first }, direction, stroke)
    }

    fun drawBorderSides(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        borders: dev.unknownuser.ananda.style.BorderSides
    ) {
        borders.top?.let { drawLine(x, y, x + width, y, it) }
        borders.right?.let { drawLine(x + width, y, x + width, y + height, it) }
        borders.bottom?.let { drawLine(x, y + height, x + width, y + height, it) }
        borders.left?.let { drawLine(x, y, x, y + height, it) }
    }

    fun drawRadialGradientCircle(
        x: Float,
        y: Float,
        radius: Float,
        colors: List<Color>,
        stroke: Stroke? = null
    ) {
        drawCircle(x, y, radius, colors.firstOrNull(), stroke)
    }

    fun drawRoundedGradientOutline(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        strokeWidth: Float,
        colors: List<Color>,
        direction: GradientDirection = GradientDirection.DiagonalDown
    ) {
        drawRoundedRect(x, y, width, height, radius, null, Stroke(colors.firstOrNull() ?: return, strokeWidth))
    }

    fun drawGlowingRoundedGradientOutline(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        strokeWidth: Float,
        colors: List<Color>,
        blurRadius: Float,
        direction: GradientDirection = GradientDirection.DiagonalDown
    ) {
        if (blurRadius <= 0f) {
            drawRoundedGradientOutline(x, y, width, height, radius, strokeWidth, colors, direction)
            return
        }
        drawRoundedGradientOutline(
            x,
            y,
            width,
            height,
            radius,
            strokeWidth + blurRadius * 0.45f,
            colors,
            direction
        )
    }

    fun drawShadowedRoundedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        shadow: Shadow,
        fill: Color? = null,
        stroke: Stroke? = null
    ) {
        val shadowFill = shadow.color.takeIf { it.alpha > 0 }
        if (shadowFill != null) {
            drawRoundedRect(
                x + shadow.offsetX - shadow.spread,
                y + shadow.offsetY - shadow.spread,
                width + shadow.spread * 2f,
                height + shadow.spread * 2f,
                radius + shadow.spread,
                shadowFill,
                null
            )
        }
        if (fill != null || stroke != null) {
            drawRoundedRect(x, y, width, height, radius, fill, stroke)
        }
    }

    fun drawShadowedCircle(
        x: Float,
        y: Float,
        radius: Float,
        shadow: Shadow,
        fill: Color? = null,
        stroke: Stroke? = null
    ) {
        val shadowFill = shadow.color.takeIf { it.alpha > 0 }
        if (shadowFill != null) {
            drawCircle(x + shadow.offsetX, y + shadow.offsetY, radius + shadow.spread, shadowFill)
        }
        if (fill != null || stroke != null) {
            drawCircle(x, y, radius, fill, stroke)
        }
    }

    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, stroke: Stroke)

    fun drawGlowingLine(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        stroke: Stroke,
        blurRadius: Float
    ) {
        if (blurRadius <= 0f) {
            drawLine(x1, y1, x2, y2, stroke)
            return
        }
        drawLine(x1, y1, x2, y2, stroke.copy(width = stroke.width + blurRadius * 0.45f))
    }

    fun drawPolyline(points: List<ChartPoint>, stroke: Stroke, smooth: Boolean = false) {
        if (points.size < 2) return
        points.zipWithNext().forEach { (from, to) ->
            drawLine(from.x, from.y, to.x, to.y, stroke)
        }
    }

    fun drawGlowingPolyline(points: List<ChartPoint>, stroke: Stroke, blurRadius: Float, smooth: Boolean = false) {
        if (blurRadius <= 0f) {
            drawPolyline(points, stroke, smooth)
            return
        }
        drawPolyline(points, stroke.copy(width = stroke.width + blurRadius * 0.45f), smooth)
    }

    fun drawText(text: String, x: Float, y: Float, style: TextStyle)

    fun measureText(text: String, style: TextStyle): Pair<Float, Float> =
        (text.length * style.size * 0.55f + style.letterSpacing * (text.codePointCount(0, text.length) - 1).coerceAtLeast(0)) to style.size * 1.4f

    fun drawTexture(texture: TextureRegion, x: Float, y: Float, width: Float, height: Float, alpha: Float = 1f) = Unit

    fun drawFramebuffer(framebuffer: FramebufferRegion, x: Float, y: Float, width: Float, height: Float, alpha: Float = 1f) = Unit

    fun clipped(x: Float, y: Float, width: Float, height: Float, block: () -> Unit) {
        block()
    }

    fun translated(x: Float, y: Float, block: () -> Unit) {
        block()
    }

    fun scaled(scaleX: Float, scaleY: Float = scaleX, block: () -> Unit) {
        block()
    }

    fun rotated(degrees: Float, pivotX: Float = 0f, pivotY: Float = 0f, block: () -> Unit) {
        block()
    }

    fun withAlpha(alpha: Float, block: () -> Unit) {
        block()
    }
}
