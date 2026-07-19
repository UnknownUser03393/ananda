package dev.unknownuser.ananda.component

import dev.unknownuser.ananda.backend.ChartPoint
import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.layout.Constraints
import java.awt.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ElevatedPanel(
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 0f,
    height: Float = 0f,
    var radius: Float = 18f,
    var colors: List<Color> = listOf(Color(38, 43, 54, 230), Color(25, 28, 36, 230)),
    var gradientDirection: GradientDirection = GradientDirection.DiagonalDown,
    var shadow: Shadow? = Shadow(Color(0, 0, 0, 120), blurRadius = 18f, offsetY = 8f),
    var outline: Stroke? = Stroke(Color(255, 255, 255, 34), 1f),
    var outlineColors: List<Color>? = null,
    var outlineGradientDirection: GradientDirection = GradientDirection.DiagonalDown
) : Component(x, y, width, height) {
    override fun draw(context: RenderContext) {
        if (!visible) return
        val childContext = theme?.let { context.withTheme(it) } ?: context
        style?.padding?.let { padding = it }
        measure(Constraints(width.coerceAtLeast(0f), height.coerceAtLeast(0f)))
        layout.layout(this, Constraints(measuredWidth, measuredHeight))

        childContext.backend.translated(x, y) {
            shadow?.let {
                childContext.backend.drawShadowedRoundedRect(
                    0f,
                    0f,
                    measuredWidth,
                    measuredHeight,
                    radius,
                    it
                )
            }
            childContext.backend.drawRoundedGradientRect(
                0f,
                0f,
                measuredWidth,
                measuredHeight,
                radius,
                colors,
                gradientDirection,
                outline.takeIf { outlineColors == null }
            )
            val gradientOutline = outlineColors
            val outlineWidth = outline?.width ?: 1f
            if (gradientOutline != null) {
                childContext.backend.drawRoundedGradientOutline(
                    0f,
                    0f,
                    measuredWidth,
                    measuredHeight,
                    radius,
                    outlineWidth,
                    gradientOutline,
                    outlineGradientDirection
                )
            }
            children.forEach { it.draw(childContext) }
        }
    }
}

class GlowOrb(
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 120f,
    height: Float = 120f,
    var colors: List<Color> = listOf(Color(234, 179, 8, 110), Color(234, 179, 8, 0)),
    var shadow: Shadow? = null
) : Component(x, y, width, height) {
    override fun draw(context: RenderContext) {
        if (!visible) return
        measure(Constraints(width.coerceAtLeast(0f), height.coerceAtLeast(0f)))
        val radius = min(measuredWidth, measuredHeight) / 2f
        context.backend.translated(x, y) {
            shadow?.let {
                context.backend.drawShadowedCircle(measuredWidth / 2f, measuredHeight / 2f, radius, it)
            }
            context.backend.drawRadialGradientCircle(
                measuredWidth / 2f,
                measuredHeight / 2f,
                radius,
                colors
            )
        }
    }
}

class GlowLine(
    x: Float = 0f,
    y: Float = 0f,
    var x2: Float = 0f,
    var y2: Float = 0f,
    var color: Color = Color(125, 211, 252),
    var strokeWidth: Float = 2f,
    var glowColor: Color = Color(125, 211, 252, 90),
    var glowWidth: Float = 10f
) : Component(x, y, abs(x2 - x), abs(y2 - y)) {
    override fun draw(context: RenderContext) {
        if (!visible) return
        context.backend.drawGlowingLine(x, y, x2, y2, Stroke(glowColor, strokeWidth.coerceAtLeast(1f)), glowWidth)
        context.backend.drawLine(x, y, x2, y2, Stroke(color, strokeWidth))
    }
}

class GlowFrame(
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 120f,
    height: Float = 48f,
    var radius: Float = 14f,
    var glow: Shadow = Shadow(Color(94, 234, 212, 82), blurRadius = 16f, spread = 2f),
    var outlineWidth: Float = 1.2f,
    var outlineColors: List<Color> = listOf(Color(94, 234, 212, 190), Color(255, 222, 126, 150)),
    var outlineGradientDirection: GradientDirection = GradientDirection.Horizontal
) : Component(x, y, width, height) {
    override fun draw(context: RenderContext) {
        if (!visible) return
        measure(Constraints(width.coerceAtLeast(0f), height.coerceAtLeast(0f)))
        context.backend.translated(x, y) {
            if (glow.blurRadius > 0f && glow.color.alpha > 0) {
                context.backend.drawGlowingRoundedGradientOutline(
                    glow.offsetX - glow.spread,
                    glow.offsetY - glow.spread,
                    measuredWidth + glow.spread * 2f,
                    measuredHeight + glow.spread * 2f,
                    radius + glow.spread,
                    outlineWidth,
                    outlineColors.withGlowAlpha(glow.color.alpha),
                    glow.blurRadius,
                    outlineGradientDirection
                )
            }
            context.backend.drawRoundedGradientOutline(
                0f,
                0f,
                measuredWidth,
                measuredHeight,
                radius,
                outlineWidth,
                outlineColors,
                outlineGradientDirection
            )
        }
    }
}

private fun List<Color>.withGlowAlpha(alpha: Int): List<Color> =
    map { color ->
        Color(
            color.red,
            color.green,
            color.blue,
            (color.alpha * alpha / 255f).toInt().coerceIn(0, 255)
        )
    }

class CurveChart(
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 240f,
    height: Float = 120f,
    var values: List<Float> = emptyList(),
    var minValue: Float? = null,
    var maxValue: Float? = null,
    var smooth: Boolean = true,
    var showGrid: Boolean = true,
    var gridLines: Int = 4,
    var lineStroke: Stroke = Stroke(Color(94, 234, 212), 2f),
    var glowStroke: Stroke? = Stroke(Color(94, 234, 212, 56), 2f),
    var glowRadius: Float = 10f,
    var interpolationSteps: Int = 8,
    var pointColor: Color? = Color.WHITE,
    var chartPadding: Float = 12f
) : Component(x, y, width, height) {
    override fun draw(context: RenderContext) {
        if (!visible) return
        measure(Constraints(width.coerceAtLeast(0f), height.coerceAtLeast(0f)))
        val points = chartPoints()
        val linePoints = interpolatedPoints(points)
        context.backend.translated(x, y) {
            drawGrid(context)
            context.backend.clipped(0f, 0f, measuredWidth, measuredHeight) {
                val glow = glowStroke
                if (glow != null) {
                    context.backend.drawGlowingPolyline(linePoints, glow, glowRadius, smooth = false)
                }
                context.backend.drawPolyline(linePoints, lineStroke, smooth = false)
                if (points.size == 1) {
                    pointColor?.let { context.backend.drawCircle(points.first().x, points.first().y, lineStroke.width + 2f, it) }
                } else {
                    pointColor?.let { color ->
                        points.lastOrNull()?.let { point ->
                            context.backend.drawCircle(point.x, point.y, max(lineStroke.width + 1.5f, 3f), color)
                        }
                    }
                }
            }
        }
    }

    private fun drawGrid(context: RenderContext) {
        if (!showGrid || gridLines <= 0) return
        val theme = context.theme
        val gridStroke = Stroke(theme.palette.border.withAlpha(58), 1f)
        val innerLeft = chartPadding
        val innerRight = measuredWidth - chartPadding
        val innerTop = chartPadding
        val innerBottom = measuredHeight - chartPadding
        repeat(gridLines + 1) { index ->
            val t = index / gridLines.toFloat()
            val y = innerTop + (innerBottom - innerTop) * t
            context.backend.drawLine(innerLeft, y, innerRight, y, gridStroke)
        }
    }

    private fun chartPoints(): List<ChartPoint> {
        val samples = values.takeIf { it.isNotEmpty() } ?: return emptyList()
        val resolvedMin = minValue ?: samples.minOrNull() ?: 0f
        val resolvedMax = maxValue ?: samples.maxOrNull() ?: 1f
        val range = (resolvedMax - resolvedMin).takeIf { abs(it) > 0.0001f } ?: 1f
        val innerWidth = (measuredWidth - chartPadding * 2f).coerceAtLeast(1f)
        val innerHeight = (measuredHeight - chartPadding * 2f).coerceAtLeast(1f)
        val denominator = (samples.size - 1).coerceAtLeast(1).toFloat()
        return samples.mapIndexed { index, value ->
            val x = chartPadding + innerWidth * (index / denominator)
            val normalized = ((value - resolvedMin) / range).coerceIn(0f, 1f)
            ChartPoint(x, chartPadding + innerHeight * (1f - normalized))
        }
    }

    private fun interpolatedPoints(points: List<ChartPoint>): List<ChartPoint> {
        if (!smooth || points.size < 3 || interpolationSteps <= 1) return points
        val steps = interpolationSteps.coerceIn(2, 24)
        val result = ArrayList<ChartPoint>((points.size - 1) * steps + 1)
        for (index in 0 until points.lastIndex) {
            val p0 = points.getOrElse(index - 1) { points[index] }
            val p1 = points[index]
            val p2 = points[index + 1]
            val p3 = points.getOrElse(index + 2) { p2 }
            repeat(steps) { step ->
                val t = step / steps.toFloat()
                result += ChartPoint(catmullRom(p0.x, p1.x, p2.x, p3.x, t), catmullRom(p0.y, p1.y, p2.y, p3.y, t))
            }
        }
        result += points.last()
        return result
    }

    private fun catmullRom(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
        val t2 = t * t
        val t3 = t2 * t
        return 0.5f * (
            2f * p1 +
                (-p0 + p2) * t +
                (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
                (-p0 + 3f * p1 - 3f * p2 + p3) * t3
            )
    }
}

private fun Color.withAlpha(alpha: Int): Color =
    Color(red, green, blue, alpha.coerceIn(0, 255))
