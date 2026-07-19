package dev.unknownuser.ananda.liquidbounce

import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.event.KeyDown
import dev.unknownuser.ananda.event.PointerDown
import dev.unknownuser.ananda.event.or
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.stateOf
import kotlin.math.abs
import kotlin.math.exp

/**
 * LiquidBounce clickgui `setting/common/Switch.svelte` — 28×16 track with accent knob.
 */
class LbSwitch(
    val checked: State<Boolean> = stateOf(false),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 28f,
    height: Float = 16f,
    private val onChange: (Boolean) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate: () -> Unit = { requestRender() }
    private var checkedAmount = if (checked.value) 1f else 0f

    init {
        focusable = true
        checked.subscribe(invalidate)
        onDispose { checked.unsubscribe(invalidate) }
        on(PointerDown) {
            toggle()
            requestFocus()
            it.consume()
        }
        on(KeyDown.Enter or KeyDown.Space) {
            toggle()
            it.consume()
        }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        val target = if (checked.value) 1f else 0f
        checkedAmount = lbAnimate(checkedAmount, target, context.time.deltaSeconds, 18f)

        val trackOff = LbPalette.withAlpha(LbPalette.ClickGuiTextDimmed, 61)
        val trackOn = LbPalette.withAlpha(LbPalette.Accent, 71)
        val knobOff = LbPalette.ClickGuiTextDimmed
        val knobOn = LbPalette.Accent
        val track = lbLerpColor(trackOff, trackOn, checkedAmount)
        val knob = lbLerpColor(knobOff, knobOn, checkedAmount)
        val knobTravel = measuredWidth - 12f - 4f
        val knobX = 2f + 5f + checkedAmount * knobTravel

        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(0f, 0f, measuredWidth, measuredHeight, measuredHeight / 2f, track)
            context.backend.drawCircle(knobX, measuredHeight / 2f, 5f, knob)
        }
        if (abs(checkedAmount - target) > 0.001f) requestRender()
    }

    private fun toggle() {
        if (disabled) return
        checked.value = !checked.value
        onChange(checked.value)
    }
}

private fun lbAnimate(current: Float, target: Float, deltaSeconds: Float, speed: Float): Float {
    val step = 1f - exp(-speed * deltaSeconds.coerceIn(0f, 0.1f))
    return current + (target - current) * step
}

private fun lbLerpColor(from: java.awt.Color, to: java.awt.Color, progress: Float): java.awt.Color {
    val t = progress.coerceIn(0f, 1f)
    return java.awt.Color(
        (from.red + (to.red - from.red) * t).toInt().coerceIn(0, 255),
        (from.green + (to.green - from.green) * t).toInt().coerceIn(0, 255),
        (from.blue + (to.blue - from.blue) * t).toInt().coerceIn(0, 255),
        (from.alpha + (to.alpha - from.alpha) * t).toInt().coerceIn(0, 255)
    )
}