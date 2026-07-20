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

enum class MaterialButtonStyle { Filled, Tonal, Elevated, Outlined, Text }

class MaterialButton(
    var text: String,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 120f,
    height: Float = 40f,
    var variant: MaterialButtonStyle = MaterialButtonStyle.Filled,
    private val onClick: () -> Unit = {}
) : Component(x, y, width, height) {
    private var hover = 0f
    private var pressed = 0f
    private var focused = 0f

    init {
        focusable = true
        minSize(64f, 40f)
        on(PointerDown) {
            setInteraction { copy(pressed = true) }
            requestFocus()
            it.consume()
        }
        on(PointerUp) {
            if (interaction.pressed && !disabled) onClick()
            setInteraction { copy(pressed = false) }
            it.consume()
        }
        on(KeyDown.Enter or KeyDown.Space) {
            if (!disabled) onClick()
            it.consume()
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        hover = animate(hover, if (interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 16f)
        pressed = animate(pressed, if (interaction.pressed && !disabled) 1f else 0f, context.time.deltaSeconds, 22f)
        focused = animate(focused, if (interaction.focused && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        val stateAlpha = 0.08f * hover + 0.10f * pressed + 0.10f * focused
        val (container, content) = when (variant) {
            MaterialButtonStyle.Filled -> p.primary to p.onPrimary
            MaterialButtonStyle.Tonal -> p.secondaryContainer to p.onSecondaryContainer
            MaterialButtonStyle.Elevated -> p.surfaceContainerHigh to p.primary
            MaterialButtonStyle.Outlined, MaterialButtonStyle.Text -> Color(0, 0, 0, 0) to p.primary
        }
        backgroundColor = overlay(container, content, stateAlpha, if (disabled) 0.38f else 1f)
        cornerRadius = measuredHeight.coerceAtLeast(height) / 2f
        borderColor = if (variant == MaterialButtonStyle.Outlined) p.outline.withAlpha(if (disabled) 30 else 255) else null
        borderWidth = 1f
        elevationShadow = if (variant == MaterialButtonStyle.Elevated && !disabled) Shadow(Color(0, 0, 0, 65), 5f, offsetY = 2f) else null
        super.draw(context)
        context.backend.translated(x, y) {
            val color = content.withAlpha(if (disabled) 97 else 255)
            val style = TextStyle(context.theme.typography.bodySize, color, context.theme.typography.fontFamily)
            val textWidth = context.backend.measureText(text, style).first
            context.backend.drawText(text, (measuredWidth - textWidth) / 2f, textBaseline(measuredHeight, style.size), style)
        }
        if (isAnimating(hover, pressed, focused)) requestRender()
    }
}

enum class MaterialCardStyle { Filled, Elevated, Outlined }

class MaterialCard(
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 240f,
    height: Float = 120f,
    var variant: MaterialCardStyle = MaterialCardStyle.Filled,
    var interactive: Boolean = false,
    private val onClick: () -> Unit = {}
) : Component(x, y, width, height) {
    private var hover = 0f

    init {
        minSize(48f, 48f)
        on(PointerDown) {
            if (interactive && !disabled) {
                setInteraction { copy(pressed = true) }
                it.consume()
            }
        }
        on(PointerUp) {
            if (interactive && interaction.pressed && !disabled) onClick()
            setInteraction { copy(pressed = false) }
            if (interactive) it.consume()
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        hover = animate(hover, if (interactive && interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        val base = when (variant) {
            MaterialCardStyle.Filled -> p.surfaceContainerHighest
            MaterialCardStyle.Elevated -> p.surfaceContainerLow
            MaterialCardStyle.Outlined -> p.surface
        }
        backgroundColor = overlay(base, p.onSurface, hover * 0.08f, if (disabled) 0.38f else 1f)
        cornerRadius = MaterialShapes.Medium
        borderColor = if (variant == MaterialCardStyle.Outlined) p.outlineVariant.withAlpha(150) else null
        borderWidth = 1f
        elevationShadow = if (variant == MaterialCardStyle.Elevated && !disabled) Shadow(Color(0, 0, 0, 28), 5f, offsetY = 1f) else null
        super.draw(context)
        if (hover in 0.001f..0.999f) requestRender()
    }
}

class MaterialSwitch(
    val checked: State<Boolean> = stateOf(false),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 52f,
    height: Float = 32f
) : Component(x, y, width, height) {
    private var progress = if (checked.value) 1f else 0f
    private val invalidate = { requestRender() }

    init {
        focusable = true
        minSize(52f, 32f)
        checked.subscribe(invalidate)
        onDispose { checked.unsubscribe(invalidate) }
        on(PointerDown) { toggle(); requestFocus(); it.consume() }
        on(KeyDown.Enter or KeyDown.Space) { toggle(); it.consume() }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        val target = if (checked.value) 1f else 0f
        progress = animate(progress, target, context.time.deltaSeconds, 18f)
        val track = if (disabled) p.onSurface.withAlpha(31) else lerp(p.surfaceContainerHighest, p.primary, progress)
        val stroke = if (!checked.value) Stroke(p.outline, 2f) else null
        val thumbRadius = 8f + progress * 4f
        val thumbX = 16f + progress * (measuredWidth.coerceAtLeast(width) - 32f)
        val thumb = if (disabled) p.onSurface.withAlpha(97) else lerp(p.outline, p.onPrimary, progress)
        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(0f, 0f, measuredWidth, measuredHeight, measuredHeight / 2f, track, stroke)
            context.backend.drawCircle(thumbX, measuredHeight / 2f, thumbRadius, thumb)
        }
        if (abs(progress - target) > 0.001f) requestRender()
    }

    private fun toggle() { if (!disabled) checked.value = !checked.value }
}

