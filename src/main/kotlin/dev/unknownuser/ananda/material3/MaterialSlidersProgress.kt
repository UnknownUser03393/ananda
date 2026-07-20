package dev.unknownuser.ananda.material3

import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.RenderBackend
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextStyle
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.event.ImeCommit
import dev.unknownuser.ananda.event.ImeCompose
import dev.unknownuser.ananda.event.Blur
import dev.unknownuser.ananda.event.KeyDown
import dev.unknownuser.ananda.event.KeyEvent
import dev.unknownuser.ananda.event.PointerDown
import dev.unknownuser.ananda.event.PointerMove
import dev.unknownuser.ananda.event.PointerUp
import dev.unknownuser.ananda.event.TextInput
import dev.unknownuser.ananda.event.or
import dev.unknownuser.ananda.layout.Insets
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.stateOf
import java.awt.Color
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.event.InputEvent
import java.awt.event.KeyEvent as AwtKeyEvent
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

class MaterialSlider(
    val value: State<Float> = stateOf(0f),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 220f,
    height: Float = 40f,
    private val valueFormatter: (Float) -> String = { "%.2f".format(it.coerceIn(0f, 1f)) },
    var keyboardStep: Float = 0.05f
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var hover = 0f
    private var pressed = 0f
    private var animatedValue = value.value.coerceIn(0f, 1f)
    private var pressStartX = 0f
    private var dragging = false

    init {
        focusable = true
        minSize(80f, 40f)
        value.subscribe(invalidate)
        onDispose { value.unsubscribe(invalidate) }
        on(PointerDown) {
            val localX = sceneToLocal(it.x, it.y).first
            pressStartX = localX
            dragging = isOnThumb(localX)
            updateFromPointer(localX, immediateVisual = true)
            requestFocus()
            setInteraction { copy(pressed = true) }
            it.consume()
        }
        on(PointerMove) {
            if (interaction.pressed && !disabled) {
                val localX = sceneToLocal(it.x, it.y).first
                if (dragging || abs(localX - pressStartX) >= 2f) {
                    dragging = true
                    updateFromPointer(localX, immediateVisual = true)
                }
                it.consume()
            }
        }
        on(PointerUp) {
            val wasPressed = interaction.pressed
            dragging = false
            setInteraction { copy(pressed = false) }
            if (wasPressed) it.consume()
        }
        on(KeyDown) {
            if (handleKey(it)) {
                requestRender()
                it.consume()
            }
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        hover = animate(hover, if (interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        pressed = animate(pressed, if (interaction.pressed && !disabled) 1f else 0f, context.time.deltaSeconds, 20f)
        val targetValue = value.value.coerceIn(0f, 1f)
        animatedValue = if (dragging) targetValue else animate(animatedValue, targetValue, context.time.deltaSeconds, 20f)
        val normalized = animatedValue.coerceIn(0f, 1f)
        val trackLeft = 10f
        val trackRight = (measuredWidth - 10f).coerceAtLeast(trackLeft + 24f)
        val trackWidth = trackRight - trackLeft
        val centerY = measuredHeight / 2f
        val thumbX = trackLeft + trackWidth * normalized
        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(trackLeft, centerY - 2f, trackWidth, 4f, 2f, p.surfaceContainerHighest, null)
            context.backend.drawRoundedRect(trackLeft, centerY - 2f, (trackWidth * normalized).coerceAtLeast(4f), 4f, 2f, p.primary, null)
            if (hover > 0.001f || pressed > 0.001f || interaction.focused) {
                context.backend.drawCircle(thumbX, centerY, 10f + hover * 2f + pressed * 2f, p.primary.withAlpha((32f + hover * 22f + pressed * 32f).toInt()))
            }
            context.backend.drawCircle(thumbX, centerY, 8f + pressed, if (disabled) p.onSurface.withAlpha(110) else p.primary)
            val valueText = valueFormatter(targetValue)
            val textStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), p.onSurfaceVariant, context.theme.typography.fontFamily)
            val textWidth = context.backend.measureText(valueText, textStyle).first
            context.backend.drawText(valueText, measuredWidth - textWidth, 12f, textStyle)
        }
        if (isAnimating(hover, pressed) || abs(animatedValue - targetValue) > 0.001f) requestRender()
    }

    private fun handleKey(event: KeyEvent): Boolean {
        if (disabled) return false
        return when (event.keyCode) {
            AwtKeyEvent.VK_LEFT, AwtKeyEvent.VK_DOWN -> {
                value.value = (value.value - keyboardStep).coerceIn(0f, 1f)
                true
            }
            AwtKeyEvent.VK_RIGHT, AwtKeyEvent.VK_UP -> {
                value.value = (value.value + keyboardStep).coerceIn(0f, 1f)
                true
            }
            AwtKeyEvent.VK_HOME -> {
                value.value = 0f
                true
            }
            AwtKeyEvent.VK_END -> {
                value.value = 1f
                true
            }
            else -> false
        }
    }

    private fun updateFromPointer(localX: Float, immediateVisual: Boolean) {
        if (disabled) return
        val trackLeft = 10f
        val trackRight = (measuredWidth - 10f).coerceAtLeast(trackLeft + 24f)
        val trackWidth = trackRight - trackLeft
        val next = ((localX - trackLeft) / trackWidth).coerceIn(0f, 1f)
        value.value = next
        if (immediateVisual) animatedValue = next
    }

    private fun isOnThumb(localX: Float): Boolean {
        val trackLeft = 10f
        val trackRight = (measuredWidth - 10f).coerceAtLeast(trackLeft + 24f)
        val thumbX = trackLeft + (trackRight - trackLeft) * value.value.coerceIn(0f, 1f)
        return abs(localX - thumbX) <= 12f
    }
}

class MaterialRangeSlider(
    val startValue: State<Float> = stateOf(0.25f),
    val endValue: State<Float> = stateOf(0.75f),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 240f,
    height: Float = 40f,
    private val valueFormatter: (Float, Float) -> String = { start, end -> "%.2f - %.2f".format(start.coerceIn(0f, 1f), end.coerceIn(0f, 1f)) },
    var keyboardStep: Float = 0.05f
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var hover = 0f
    private var pressed = 0f
    private var animatedStart = startValue.value.coerceIn(0f, 1f)
    private var animatedEnd = endValue.value.coerceIn(0f, 1f)
    private var activeThumb = 1
    private var dragging = false

    init {
        focusable = true
        minSize(96f, 40f)
        startValue.subscribe(invalidate)
        endValue.subscribe(invalidate)
        onDispose {
            startValue.unsubscribe(invalidate)
            endValue.unsubscribe(invalidate)
        }
        on(PointerDown) {
            if (disabled) return@on
            requestFocus()
            val localX = sceneToLocal(it.x, it.y).first
            activeThumb = thumbFor(localX)
            dragging = true
            setInteraction { copy(pressed = true) }
            updateFromPointer(localX, activeThumb, immediateVisual = true)
            it.consume()
        }
        on(PointerMove) {
            if (interaction.pressed && dragging && !disabled) {
                updateFromPointer(sceneToLocal(it.x, it.y).first, activeThumb, immediateVisual = true)
                it.consume()
            }
        }
        on(PointerUp) {
            val wasPressed = interaction.pressed
            dragging = false
            setInteraction { copy(pressed = false) }
            if (wasPressed) it.consume()
        }
        on(KeyDown) {
            if (handleKey(it)) {
                requestRender()
                it.consume()
            }
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        hover = animate(hover, if (interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        pressed = animate(pressed, if (interaction.pressed && !disabled) 1f else 0f, context.time.deltaSeconds, 20f)
        val start = minOf(startValue.value, endValue.value).coerceIn(0f, 1f)
        val end = maxOf(startValue.value, endValue.value).coerceIn(0f, 1f)
        animatedStart = if (dragging && activeThumb == 0) start else animate(animatedStart, start, context.time.deltaSeconds, 20f)
        animatedEnd = if (dragging && activeThumb == 1) end else animate(animatedEnd, end, context.time.deltaSeconds, 20f)
        val trackLeft = 10f
        val trackRight = (measuredWidth - 10f).coerceAtLeast(trackLeft + 24f)
        val trackWidth = trackRight - trackLeft
        val centerY = measuredHeight / 2f
        val startX = trackLeft + trackWidth * animatedStart
        val endX = trackLeft + trackWidth * animatedEnd
        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(trackLeft, centerY - 2f, trackWidth, 4f, 2f, p.surfaceContainerHighest, null)
            context.backend.drawRoundedRect(startX, centerY - 2f, (endX - startX).coerceAtLeast(4f), 4f, 2f, p.primary, null)
            if (hover > 0.001f || pressed > 0.001f || interaction.focused) {
                val haloRadius = 10f + hover * 2f + pressed * 2f
                val haloAlpha = (32f + hover * 22f + pressed * 32f).toInt()
                context.backend.drawCircle(startX, centerY, haloRadius, p.primary.withAlpha(haloAlpha))
                context.backend.drawCircle(endX, centerY, haloRadius, p.primary.withAlpha(haloAlpha))
            }
            context.backend.drawCircle(startX, centerY, 8f + if (activeThumb == 0) pressed else 0f, if (disabled) p.onSurface.withAlpha(110) else p.primary)
            context.backend.drawCircle(endX, centerY, 8f + if (activeThumb == 1) pressed else 0f, if (disabled) p.onSurface.withAlpha(110) else p.primary)
            val label = valueFormatter(start, end)
            val textStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), p.onSurfaceVariant, context.theme.typography.fontFamily)
            val textWidth = context.backend.measureText(label, textStyle).first
            context.backend.drawText(label, measuredWidth - textWidth, 12f, textStyle)
        }
        if (isAnimating(hover, pressed) || abs(animatedStart - start) > 0.001f || abs(animatedEnd - end) > 0.001f) requestRender()
    }

    private fun handleKey(event: KeyEvent): Boolean {
        if (disabled) return false
        return when (event.keyCode) {
            AwtKeyEvent.VK_TAB -> {
                activeThumb = 1 - activeThumb
                true
            }
            AwtKeyEvent.VK_LEFT, AwtKeyEvent.VK_DOWN -> {
                adjustThumb(activeThumb, -keyboardStep)
                true
            }
            AwtKeyEvent.VK_RIGHT, AwtKeyEvent.VK_UP -> {
                adjustThumb(activeThumb, keyboardStep)
                true
            }
            AwtKeyEvent.VK_HOME -> {
                if (activeThumb == 0) startValue.value = 0f else endValue.value = startValue.value.coerceAtLeast(0f)
                true
            }
            AwtKeyEvent.VK_END -> {
                if (activeThumb == 1) endValue.value = 1f else startValue.value = endValue.value.coerceAtMost(1f)
                true
            }
            else -> false
        }
    }

    private fun adjustThumb(index: Int, delta: Float) {
        val start = minOf(startValue.value, endValue.value).coerceIn(0f, 1f)
        val end = maxOf(startValue.value, endValue.value).coerceIn(0f, 1f)
        if (index == 0) {
            startValue.value = (start + delta).coerceIn(0f, end)
            endValue.value = end
        } else {
            startValue.value = start
            endValue.value = (end + delta).coerceIn(start, 1f)
        }
    }

    private fun updateFromPointer(localX: Float, index: Int, immediateVisual: Boolean) {
        val trackLeft = 10f
        val trackRight = (measuredWidth - 10f).coerceAtLeast(trackLeft + 24f)
        val trackWidth = trackRight - trackLeft
        val next = ((localX - trackLeft) / trackWidth).coerceIn(0f, 1f)
        val start = minOf(startValue.value, endValue.value).coerceIn(0f, 1f)
        val end = maxOf(startValue.value, endValue.value).coerceIn(0f, 1f)
        if (index == 0) {
            startValue.value = next.coerceAtMost(end)
            endValue.value = end
        } else {
            startValue.value = start
            endValue.value = next.coerceAtLeast(start)
        }
        if (immediateVisual) {
            animatedStart = minOf(startValue.value, endValue.value)
            animatedEnd = maxOf(startValue.value, endValue.value)
        }
    }

    private fun thumbFor(localX: Float): Int {
        val trackLeft = 10f
        val trackRight = (measuredWidth - 10f).coerceAtLeast(trackLeft + 24f)
        val trackWidth = trackRight - trackLeft
        val startX = trackLeft + trackWidth * minOf(startValue.value, endValue.value).coerceIn(0f, 1f)
        val endX = trackLeft + trackWidth * maxOf(startValue.value, endValue.value).coerceIn(0f, 1f)
        return if (abs(localX - startX) <= abs(localX - endX)) 0 else 1
    }
}

class MaterialLinearProgressIndicator(
    val progress: State<Float> = stateOf(0f),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 180f,
    height: Float = 4f
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var displayProgress = progress.value.coerceIn(0f, 1f)

    init {
        minSize(48f, 4f)
        progress.subscribe(invalidate)
        onDispose { progress.unsubscribe(invalidate) }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        val target = progress.value.coerceIn(0f, 1f)
        displayProgress = animate(displayProgress, target, context.time.deltaSeconds, 18f)
        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(0f, 0f, measuredWidth, measuredHeight, measuredHeight / 2f, p.secondaryContainer, null)
            if (displayProgress > 0f) {
                context.backend.drawRoundedRect(0f, 0f, measuredWidth * displayProgress, measuredHeight, measuredHeight / 2f, p.primary, null)
            }
        }
        if (abs(displayProgress - target) > 0.001f) requestRender()
    }
}

class MaterialCircularProgressIndicator(
    val progress: State<Float> = stateOf(0f),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 40f,
    height: Float = 40f,
    var strokeWidth: Float = 5f
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var displayProgress = progress.value.coerceIn(0f, 1f)

    init {
        minSize(24f, 24f)
        progress.subscribe(invalidate)
        onDispose { progress.unsubscribe(invalidate) }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        val target = progress.value.coerceIn(0f, 1f)
        displayProgress = animate(displayProgress, target, context.time.deltaSeconds, 18f)
        context.backend.translated(x, y) {
            val radius = (minOf(measuredWidth, measuredHeight) - strokeWidth) / 2f
            val centerX = measuredWidth / 2f
            val centerY = measuredHeight / 2f
            drawArcStroke(context, centerX, centerY, radius, -PI.toFloat() / 2f, PI.toFloat() * 2f, Stroke(p.secondaryContainer, strokeWidth))
            if (displayProgress > 0f) {
                drawArcStroke(context, centerX, centerY, radius, -PI.toFloat() / 2f, PI.toFloat() * 2f * displayProgress, Stroke(p.primary, strokeWidth))
            }
        }
        if (abs(displayProgress - target) > 0.001f) requestRender()
    }
}

class MaterialIndeterminateCircularProgressIndicator(
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 40f,
    height: Float = 40f,
    var strokeWidth: Float = 5f,
    var speed: Float = 2.4f
) : Component(x, y, width, height) {
    private var phase = 0f

    init {
        minSize(24f, 24f)
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        phase = (phase + context.time.deltaSeconds * speed).rem((PI.toFloat() * 2f))
        val radius = (minOf(measuredWidth, measuredHeight) - strokeWidth) / 2f
        val centerX = measuredWidth / 2f
        val centerY = measuredHeight / 2f
        val sweep = PI.toFloat() * 1.35f
        context.backend.translated(x, y) {
            drawArcStroke(context, centerX, centerY, radius, 0f, PI.toFloat() * 2f, Stroke(p.secondaryContainer, strokeWidth))
            drawArcStroke(context, centerX, centerY, radius, phase, sweep, Stroke(p.primary, strokeWidth))
        }
        requestRender()
    }
}

class MaterialIndeterminateLinearProgressIndicator(
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 180f,
    height: Float = 4f,
    var segmentWidthFraction: Float = 0.3f,
    var speed: Float = 0.6f
) : Component(x, y, width, height) {
    private var offsetProgress = 0f

    init {
        minSize(48f, 4f)
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        val segmentWidth = measuredWidth * segmentWidthFraction.coerceIn(0.1f, 0.8f)
        val travel = measuredWidth + segmentWidth
        offsetProgress = (offsetProgress + context.time.deltaSeconds * measuredWidth * speed).rem(travel.coerceAtLeast(1f))
        val offset = offsetProgress - segmentWidth
        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(0f, 0f, measuredWidth, measuredHeight, measuredHeight / 2f, p.secondaryContainer, null)
            context.backend.drawRoundedRect(offset, 0f, segmentWidth, measuredHeight, measuredHeight / 2f, p.primary, null)
        }
        requestRender()
    }
}

class MaterialRefreshIndicator(
    val refreshing: State<Boolean> = stateOf(false),
    val pullProgress: State<Float> = stateOf(0f),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 40f,
    height: Float = 40f,
    var indicatorStrokeWidth: Float = 4f
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var spinAngle = 0f
    private var animatedPull = 0f

    init {
        minSize(24f, 24f)
        refreshing.subscribe(invalidate)
        pullProgress.subscribe(invalidate)
        onDispose {
            refreshing.unsubscribe(invalidate)
            pullProgress.unsubscribe(invalidate)
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        val isRefreshing = refreshing.value
        val pull = pullProgress.value.coerceIn(0f, 1f)
        animatedPull = animate(animatedPull, pull, context.time.deltaSeconds, 12f)
        spinAngle = if (isRefreshing) (spinAngle + context.time.deltaSeconds * 270f) % 360f else spinAngle
        if (!isRefreshing && animatedPull < 0.005f) return
        context.backend.translated(x, y) {
            val radius = (minOf(measuredWidth, measuredHeight) - indicatorStrokeWidth) / 2f
            val centerX = measuredWidth / 2f
            val centerY = measuredHeight / 2f
            val startAngleDeg = if (isRefreshing) spinAngle else -90f
            val startAngleRad = Math.toRadians(startAngleDeg.toDouble()).toFloat()
            val sweepRad = if (isRefreshing) {
                (PI * 1.5f).toFloat()
            } else {
                (PI * 2f * animatedPull).toFloat()
            }
            drawArcStroke(context, centerX, centerY, radius, 0f, (PI * 2f).toFloat(), Stroke(p.surfaceContainerHighest, indicatorStrokeWidth))
            if (sweepRad > 0.01f) {
                drawArcStroke(context, centerX, centerY, radius, startAngleRad, sweepRad, Stroke(p.primary, indicatorStrokeWidth))
            }
        }
        if (isRefreshing || isAnimating(animatedPull)) requestRender()
    }
}

