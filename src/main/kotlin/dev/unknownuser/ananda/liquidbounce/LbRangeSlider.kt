package dev.unknownuser.ananda.liquidbounce

import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.layout.Insets
import dev.unknownuser.ananda.event.PointerDown
import dev.unknownuser.ananda.event.PointerMove
import dev.unknownuser.ananda.event.PointerUp
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.stateOf
import kotlin.math.abs
import kotlin.math.exp

/**
 * Range slider backing LiquidBounce `FloatSetting.svelte` nouislider track.
 */
class LbRangeSlider(
    val value: State<Float> = stateOf(0f),
    val rangeMin: Float = 0f,
    val rangeMax: Float = 1f,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 0f,
    height: Float = 14f,
    private val onChange: (Float) -> Unit = {}
) : Component(x, y, width, height) {
    private var animatedNormalized = normalized(value.value)
    private var dragging = false
    private var pressStartX = 0f
    private var pendingClick = false

    init {
        focusable = true
        padding = Insets(right = 10f)
        value.subscribe { requestRender() }
        on(PointerDown) {
            val localX = sceneToLocal(it.x, it.y).first
            pressStartX = localX
            dragging = isOnKnob(localX)
            pendingClick = !dragging
            if (dragging) updateFromPointer(localX)
            requestFocus()
            it.consume()
        }
        on(PointerMove) {
            if (interaction.pressed) {
                val localX = sceneToLocal(it.x, it.y).first
                if (dragging || abs(localX - pressStartX) >= DragThreshold) {
                    dragging = true
                    pendingClick = false
                    updateFromPointer(localX)
                }
                it.consume()
            }
        }
        on(PointerUp) {
            if (pendingClick && !dragging) updateFromPointer(sceneToLocal(it.x, it.y).first)
            pendingClick = false
            dragging = false
            if (interaction.pressed) it.consume()
        }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        val target = normalized(value.value)
        animatedNormalized = if (dragging) target else lbAnimateSlider(animatedNormalized, target, context.time.deltaSeconds, 20f)
        val trackY = measuredHeight / 2f
        val trackWidth = measuredWidth - padding.right
        val fillWidth = (trackWidth * animatedNormalized).coerceAtLeast(3f)
        val knobX = trackWidth * animatedNormalized
        val trackColor = LbPalette.withAlpha(LbPalette.ClickGuiTextDimmed, 61)
        val fillColor = LbPalette.withAlpha(LbPalette.Accent, 200)
        val knobColor = LbPalette.ClickGuiTextDimmed

        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(0f, trackY - 7f, trackWidth, 14f, 7f, trackColor)
            context.backend.drawRoundedRect(0f, trackY - 7f, fillWidth, 14f, 7f, fillColor)
            context.backend.drawCircle(knobX, trackY, 5f, knobColor, Stroke(LbPalette.withAlpha(java.awt.Color.WHITE, 90), 1f))
        }
        if (abs(animatedNormalized - target) > 0.001f) requestRender()
    }

    private fun normalized(raw: Float): Float {
        if (rangeMax <= rangeMin) return 0f
        return ((raw - rangeMin) / (rangeMax - rangeMin)).coerceIn(0f, 1f)
    }

    private fun fromNormalized(normalized: Float): Float =
        rangeMin + normalized.coerceIn(0f, 1f) * (rangeMax - rangeMin)

    private fun updateFromPointer(localX: Float) {
        val trackWidth = (measuredWidth - padding.right).coerceAtLeast(1f)
        val next = fromNormalized((localX / trackWidth).coerceIn(0f, 1f))
        value.value = next
        onChange(next)
        animatedNormalized = normalized(next)
    }

    private fun isOnKnob(localX: Float): Boolean {
        val trackWidth = (measuredWidth - padding.right).coerceAtLeast(1f)
        val knobX = normalized(value.value) * trackWidth
        return abs(localX - knobX) <= KnobHitSlop
    }

    companion object {
        private const val DragThreshold = 2f
        private const val KnobHitSlop = 10f
    }
}

private fun lbAnimateSlider(current: Float, target: Float, deltaSeconds: Float, speed: Float): Float {
    val step = 1f - exp(-speed * deltaSeconds.coerceIn(0f, 0.1f))
    return current + (target - current) * step
}
