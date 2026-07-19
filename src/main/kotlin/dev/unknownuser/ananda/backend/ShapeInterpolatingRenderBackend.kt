package dev.unknownuser.ananda.backend

import java.awt.Color
import kotlin.math.abs
import kotlin.math.exp

interface ShapeInterpolationScope {
    fun beginFrame(delegate: RenderBackend, deltaSeconds: Float)
    fun endFrame(): Boolean
    fun pushInterpolationKey(key: Any)
    fun popInterpolationKey()
}

data class InterpolationPartKey(val owner: Any, val part: Any)

fun RenderBackend.withInterpolationKey(key: Any, block: () -> Unit) {
    val scope = this as? ShapeInterpolationScope
    scope?.pushInterpolationKey(key)
    try {
        block()
    } finally {
        scope?.popInterpolationKey()
    }
}

fun RenderContext.interpolationPart(owner: Any, part: Any, block: () -> Unit) =
    backend.withInterpolationKey(InterpolationPartKey(owner, part), block)

class ShapeInterpolatingRenderBackend(
    private var delegate: RenderBackend
) : RenderBackend, ShapeInterpolationScope {
    private data class RectState(
        var x: Float,
        var y: Float,
        var width: Float,
        var height: Float,
        var radius: Float = 0f,
        var fill: Color? = null,
        var stroke: Stroke? = null,
        var colors: List<Color> = emptyList(),
        var strokeWidth: Float = 0f
    )

    private data class CircleState(
        var x: Float,
        var y: Float,
        var radius: Float,
        var fill: Color? = null,
        var stroke: Stroke? = null
    )

    private data class LineState(
        var x1: Float,
        var y1: Float,
        var x2: Float,
        var y2: Float,
        var stroke: Stroke
    )

    private val rects = linkedMapOf<String, RectState>()
    private val circles = linkedMapOf<String, CircleState>()
    private val lines = linkedMapOf<String, LineState>()
    private val keyStack = arrayListOf<Any>()
    private val drawCounters = hashMapOf<String, Int>()
    private val accessed = hashSetOf<String>()
    private var frame = 0L
    private var deltaSeconds = 0f
    private var active = false

    override fun beginFrame(delegate: RenderBackend, deltaSeconds: Float) {
        this.delegate = delegate
        this.deltaSeconds = deltaSeconds.coerceIn(0f, 0.1f)
        drawCounters.clear()
        accessed.clear()
        active = false
    }

    override fun endFrame(): Boolean {
        frame += 1
        if (frame % 120L == 0L) {
            rects.keys.removeAll { it !in accessed }
            circles.keys.removeAll { it !in accessed }
            lines.keys.removeAll { it !in accessed }
        }
        keyStack.clear()
        return active
    }

    override fun pushInterpolationKey(key: Any) {
        keyStack += key
    }

    override fun popInterpolationKey() {
        if (keyStack.isNotEmpty()) keyStack.removeAt(keyStack.lastIndex)
    }

    override fun clear(argb: Int) = delegate.clear(argb)

    override fun drawRect(x: Float, y: Float, width: Float, height: Float, fill: Color?, stroke: Stroke?) {
        val state = rectState("rect", x, y, width, height, 0f, fill, stroke)
        delegate.drawRect(state.x, state.y, state.width, state.height, state.fill, state.stroke)
    }

    override fun drawRoundedRect(x: Float, y: Float, width: Float, height: Float, radius: Float, fill: Color?, stroke: Stroke?) {
        val state = rectState("round", x, y, width, height, radius, fill, stroke)
        delegate.drawRoundedRect(state.x, state.y, state.width, state.height, state.radius, state.fill, state.stroke)
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
        delegate.drawRoundedRect(x, y, width, height, radii, fill, stroke)
    }

    override fun drawCircle(x: Float, y: Float, radius: Float, fill: Color?, stroke: Stroke?) {
        val key = nextKey("circle")
        val state = circles.getOrPut(key) { CircleState(x, y, radius, fill, stroke) }
        state.x = smooth(state.x, x)
        state.y = smooth(state.y, y)
        state.radius = smooth(state.radius, radius)
        state.fill = lerpNullable(state.fill, fill)
        state.stroke = lerpNullable(state.stroke, stroke)
        markActive(abs(state.x - x), abs(state.y - y), abs(state.radius - radius), colorDistance(state.fill, fill), strokeDistance(state.stroke, stroke))
        delegate.drawCircle(state.x, state.y, state.radius, state.fill, state.stroke)
    }

    override fun drawRoundedGradientRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        colors: List<Color>,
        direction: GradientDirection,
        stroke: Stroke?
    ) {
        if (colors.isEmpty()) return
        val key = nextKey("gradientRound")
        val state = rects.getOrPut(key) { RectState(x, y, width, height, radius, colors = colors, stroke = stroke) }
        val oldColors = state.colors
        state.x = smooth(state.x, x)
        state.y = smooth(state.y, y)
        state.width = smooth(state.width, width)
        state.height = smooth(state.height, height)
        state.radius = smooth(state.radius, radius)
        state.colors = lerpColors(state.colors, colors)
        state.stroke = lerpNullable(state.stroke, stroke)
        markActive(abs(state.x - x), abs(state.y - y), abs(state.width - width), abs(state.height - height), abs(state.radius - radius), colorsDistance(oldColors, colors), strokeDistance(state.stroke, stroke))
        delegate.drawRoundedGradientRect(state.x, state.y, state.width, state.height, state.radius, state.colors, direction, state.stroke)
    }

    override fun drawRoundedGradientOutline(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        strokeWidth: Float,
        colors: List<Color>,
        direction: GradientDirection
    ) {
        if (colors.isEmpty() || strokeWidth <= 0f) return
        val key = nextKey("gradientOutline")
        val state = rects.getOrPut(key) { RectState(x, y, width, height, radius, colors = colors, strokeWidth = strokeWidth) }
        val oldColors = state.colors
        state.x = smooth(state.x, x)
        state.y = smooth(state.y, y)
        state.width = smooth(state.width, width)
        state.height = smooth(state.height, height)
        state.radius = smooth(state.radius, radius)
        state.colors = lerpColors(state.colors, colors)
        state.strokeWidth = smooth(state.strokeWidth, strokeWidth)
        markActive(abs(state.x - x), abs(state.y - y), abs(state.width - width), abs(state.height - height), abs(state.radius - radius), abs(state.strokeWidth - strokeWidth), colorsDistance(oldColors, colors))
        delegate.drawRoundedGradientOutline(state.x, state.y, state.width, state.height, state.radius, state.strokeWidth, state.colors, direction)
    }

    override fun drawRadialGradientCircle(x: Float, y: Float, radius: Float, colors: List<Color>, stroke: Stroke?) {
        val fill = colors.firstOrNull()
        val state = circles.getOrPut(nextKey("radialCircle")) { CircleState(x, y, radius, fill, stroke) }
        state.x = smooth(state.x, x)
        state.y = smooth(state.y, y)
        state.radius = smooth(state.radius, radius)
        state.fill = lerpNullable(state.fill, fill)
        state.stroke = lerpNullable(state.stroke, stroke)
        markActive(abs(state.x - x), abs(state.y - y), abs(state.radius - radius), colorDistance(state.fill, fill), strokeDistance(state.stroke, stroke))
        delegate.drawRadialGradientCircle(state.x, state.y, state.radius, listOfNotNull(state.fill), state.stroke)
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, stroke: Stroke) {
        val key = nextKey("line")
        val state = lines.getOrPut(key) { LineState(x1, y1, x2, y2, stroke) }
        state.x1 = smooth(state.x1, x1)
        state.y1 = smooth(state.y1, y1)
        state.x2 = smooth(state.x2, x2)
        state.y2 = smooth(state.y2, y2)
        state.stroke = lerp(state.stroke, stroke)
        markActive(abs(state.x1 - x1), abs(state.y1 - y1), abs(state.x2 - x2), abs(state.y2 - y2), strokeDistance(state.stroke, stroke))
        delegate.drawLine(state.x1, state.y1, state.x2, state.y2, state.stroke)
    }

    override fun drawText(text: String, x: Float, y: Float, style: TextStyle) =
        delegate.drawText(text, x, y, style)

    override fun measureText(text: String, style: TextStyle): Pair<Float, Float> =
        delegate.measureText(text, style)

    override fun drawTexture(texture: TextureRegion, x: Float, y: Float, width: Float, height: Float, alpha: Float) =
        delegate.drawTexture(texture, x, y, width, height, alpha)

    override fun drawFramebuffer(framebuffer: FramebufferRegion, x: Float, y: Float, width: Float, height: Float, alpha: Float) =
        delegate.drawFramebuffer(framebuffer, x, y, width, height, alpha)

    override fun clipped(x: Float, y: Float, width: Float, height: Float, block: () -> Unit) =
        delegate.clipped(x, y, width, height, block)

    override fun translated(x: Float, y: Float, block: () -> Unit) =
        delegate.translated(x, y, block)

    override fun scaled(scaleX: Float, scaleY: Float, block: () -> Unit) =
        delegate.scaled(scaleX, scaleY, block)

    override fun rotated(degrees: Float, pivotX: Float, pivotY: Float, block: () -> Unit) =
        delegate.rotated(degrees, pivotX, pivotY, block)

    override fun withAlpha(alpha: Float, block: () -> Unit) =
        delegate.withAlpha(alpha, block)

    private fun rectState(type: String, x: Float, y: Float, width: Float, height: Float, radius: Float, fill: Color?, stroke: Stroke?): RectState {
        val key = nextKey(type)
        val state = rects.getOrPut(key) { RectState(x, y, width, height, radius, fill, stroke) }
        state.x = smooth(state.x, x)
        state.y = smooth(state.y, y)
        state.width = smooth(state.width, width)
        state.height = smooth(state.height, height)
        state.radius = smooth(state.radius, radius)
        state.fill = lerpNullable(state.fill, fill)
        state.stroke = lerpNullable(state.stroke, stroke)
        markActive(abs(state.x - x), abs(state.y - y), abs(state.width - width), abs(state.height - height), abs(state.radius - radius), colorDistance(state.fill, fill), strokeDistance(state.stroke, stroke))
        return state
    }

    private fun nextKey(type: String): String {
        val scope = if (keyStack.isEmpty()) "root" else keyStack.joinToString("/")
        val prefix = "$scope:$type"
        val index = drawCounters.getOrDefault(prefix, 0)
        drawCounters[prefix] = index + 1
        val key = "$prefix:$index"
        accessed += key
        return key
    }

    private fun smooth(current: Float, target: Float): Float =
        current + (target - current) * interpolationAmount()

    private fun interpolationAmount(): Float =
        (1f - exp(-DefaultSpeed * deltaSeconds)).coerceIn(0f, 1f)

    private fun lerpNullable(from: Color?, to: Color?): Color? =
        when {
            from == null && to == null -> null
            from == null -> withAlpha(to ?: return null, 0).let { lerp(it, to, interpolationAmount()) }
            to == null -> lerp(from, withAlpha(from, 0), interpolationAmount())
            else -> lerp(from, to, interpolationAmount())
        }

    private fun lerpNullable(from: Stroke?, to: Stroke?): Stroke? =
        when {
            from == null && to == null -> null
            from == null -> to?.let { lerp(Stroke(withAlpha(it.color, 0), it.width), it) }
            to == null -> from.let { lerp(it, Stroke(withAlpha(it.color, 0), it.width)) }
            else -> lerp(from, to)
        }

    private fun lerp(from: Stroke, to: Stroke): Stroke =
        Stroke(lerp(from.color, to.color, interpolationAmount()), smooth(from.width, to.width))

    private fun lerpColors(from: List<Color>, to: List<Color>): List<Color> {
        if (from.isEmpty()) return to.map { withAlpha(it, 0) }.zip(to) { a, b -> lerp(a, b, interpolationAmount()) }
        return to.mapIndexed { index, color ->
            lerp(from.getOrElse(index) { from.last() }, color, interpolationAmount())
        }
    }

    private fun lerp(from: Color, to: Color, amount: Float): Color =
        Color(
            (from.red + (to.red - from.red) * amount).toInt().coerceIn(0, 255),
            (from.green + (to.green - from.green) * amount).toInt().coerceIn(0, 255),
            (from.blue + (to.blue - from.blue) * amount).toInt().coerceIn(0, 255),
            (from.alpha + (to.alpha - from.alpha) * amount).toInt().coerceIn(0, 255)
        )

    private fun withAlpha(color: Color, alpha: Int): Color =
        Color(color.red, color.green, color.blue, alpha.coerceIn(0, 255))

    private fun colorDistance(from: Color?, to: Color?): Float {
        if (from == null && to == null) return 0f
        val a = from ?: withAlpha(to ?: return 0f, 0)
        val b = to ?: withAlpha(a, 0)
        return maxOf(abs(a.red - b.red), abs(a.green - b.green), abs(a.blue - b.blue), abs(a.alpha - b.alpha)).toFloat()
    }

    private fun colorsDistance(from: List<Color>, to: List<Color>): Float =
        to.indices.maxOfOrNull { colorDistance(from.getOrNull(it), to[it]) } ?: 0f

    private fun strokeDistance(from: Stroke?, to: Stroke?): Float {
        if (from == null && to == null) return 0f
        val a = from ?: Stroke(withAlpha(to?.color ?: return 0f, 0), to.width)
        val b = to ?: Stroke(withAlpha(a.color, 0), a.width)
        return maxOf(colorDistance(a.color, b.color), abs(a.width - b.width))
    }

    private fun markActive(vararg distances: Float) {
        if (distances.any { it > 0.5f }) active = true
    }

    private companion object {
        const val DefaultSpeed = 18f
    }
}
