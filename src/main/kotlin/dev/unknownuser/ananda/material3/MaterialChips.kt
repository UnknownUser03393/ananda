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

class MaterialChip(
    var text: String,
    val selectedState: State<Boolean>? = null,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 96f,
    height: Float = 32f,
    private val onClick: () -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var selectedProgress = if (selectedState?.value == true) 1f else 0f

    init {
        focusable = true
        minSize(48f, 32f)
        selectedState?.subscribe(invalidate)
        onDispose { selectedState?.unsubscribe(invalidate) }
        on(PointerDown) {
            setInteraction { copy(pressed = true) }
            requestFocus()
            it.consume()
        }
        on(PointerUp) {
            if (interaction.pressed && !disabled) {
                selectedState?.let { it.value = !it.value }
                onClick()
            }
            setInteraction { copy(pressed = false) }
            it.consume()
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        val isSelected = selectedState?.value == true
        selectedProgress = animate(selectedProgress, if (isSelected) 1f else 0f, context.time.deltaSeconds, 18f)
        backgroundColor = lerp(Color(0, 0, 0, 0), p.secondaryContainer, selectedProgress)
        borderColor = p.outline.withAlpha(((1f - selectedProgress) * 255f).toInt().coerceIn(0, 255))
        borderWidth = 1f
        cornerRadius = MaterialShapes.Small
        super.draw(context)
        context.backend.translated(x, y) {
            val color = lerp(p.onSurfaceVariant, p.onSecondaryContainer, selectedProgress)
            val style = TextStyle(context.theme.typography.bodySize, color, context.theme.typography.fontFamily)
            val textWidth = context.backend.measureText(text, style).first
            context.backend.drawText(text, (measuredWidth - textWidth) / 2f, textBaseline(measuredHeight, style.size), style)
        }
        if (abs(selectedProgress - if (isSelected) 1f else 0f) > 0.001f) requestRender()
    }
}

class MaterialAssistChip(
    var text: String,
    var leadingLabel: String = "",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 112f,
    height: Float = 32f,
    private val onClick: () -> Unit = {}
) : Component(x, y, width, height) {
    private var hover = 0f
    private var pressed = 0f
    private var focused = 0f

    init {
        focusable = true
        minSize(52f, 32f)
        on(PointerDown) {
            if (!disabled) {
                setInteraction { copy(pressed = true) }
                requestFocus()
                it.consume()
            }
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
        pressed = animate(pressed, if (interaction.pressed && !disabled) 1f else 0f, context.time.deltaSeconds, 20f)
        focused = animate(focused, if (interaction.focused && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        backgroundColor = overlay(Color(0, 0, 0, 0), p.onSurface, 0.06f * hover + 0.10f * pressed + 0.08f * focused, if (disabled) 0.38f else 1f)
        borderColor = p.outline.withAlpha(if (disabled) 90 else 255)
        borderWidth = 1f
        cornerRadius = MaterialShapes.Small
        super.draw(context)
        context.backend.translated(x, y) {
            val style = TextStyle(context.theme.typography.bodySize, if (disabled) p.onSurface.withAlpha(97) else p.onSurfaceVariant, context.theme.typography.fontFamily)
            var textX = 16f
            if (leadingLabel.isNotBlank()) {
                val iconWidth = context.backend.measureText(leadingLabel, style).first
                context.backend.drawText(leadingLabel, 14f, textBaseline(measuredHeight, style.size), style)
                textX += iconWidth + 8f
            }
            context.backend.drawText(text.takeFitting(context, style, measuredWidth - textX - 16f), textX, textBaseline(measuredHeight, style.size), style)
        }
        if (isAnimating(hover, pressed, focused)) requestRender()
    }
}

class MaterialSuggestionChip(
    var text: String,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 112f,
    height: Float = 32f,
    private val onClick: () -> Unit = {}
) : Component(x, y, width, height) {
    private var hover = 0f
    private var pressed = 0f

    init {
        focusable = true
        minSize(52f, 32f)
        on(PointerDown) {
            if (!disabled) {
                setInteraction { copy(pressed = true) }
                requestFocus()
                it.consume()
            }
        }
        on(PointerUp) {
            if (interaction.pressed && !disabled) onClick()
            setInteraction { copy(pressed = false) }
            it.consume()
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        hover = animate(hover, if (interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 16f)
        pressed = animate(pressed, if (interaction.pressed && !disabled) 1f else 0f, context.time.deltaSeconds, 20f)
        backgroundColor = overlay(Color(0, 0, 0, 0), p.onSurface, 0.06f * hover + 0.10f * pressed, if (disabled) 0.38f else 1f)
        borderColor = p.outline
        borderWidth = 1f
        cornerRadius = MaterialShapes.Small
        super.draw(context)
        context.backend.translated(x, y) {
            val style = TextStyle(context.theme.typography.bodySize, if (disabled) p.onSurface.withAlpha(97) else p.onSurfaceVariant, context.theme.typography.fontFamily)
            context.backend.drawText(text.takeFitting(context, style, measuredWidth - 24f), 12f, textBaseline(measuredHeight, style.size), style)
        }
        if (isAnimating(hover, pressed)) requestRender()
    }
}

class MaterialElevatedAssistChip(
    var text: String,
    var leadingLabel: String = "",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 112f,
    height: Float = 32f,
    private val onClick: () -> Unit = {}
) : Component(x, y, width, height) {
    private var hover = 0f
    private var pressed = 0f

    init {
        focusable = true
        minSize(52f, 32f)
        on(PointerDown) {
            if (!disabled) {
                setInteraction { copy(pressed = true) }
                requestFocus()
                it.consume()
            }
        }
        on(PointerUp) {
            if (interaction.pressed && !disabled) onClick()
            setInteraction { copy(pressed = false) }
            it.consume()
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        hover = animate(hover, if (interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 16f)
        pressed = animate(pressed, if (interaction.pressed && !disabled) 1f else 0f, context.time.deltaSeconds, 20f)
        backgroundColor = overlay(p.surfaceContainer, p.onSurface, 0.06f * hover + 0.10f * pressed, if (disabled) 0.38f else 1f)
        borderColor = p.outlineVariant
        borderWidth = 1f
        cornerRadius = MaterialShapes.Small
        elevationShadow = Shadow(Color(0, 0, 0, 42), 4f, offsetY = 1f)
        super.draw(context)
        context.backend.translated(x, y) {
            val style = TextStyle(context.theme.typography.bodySize, if (disabled) p.onSurface.withAlpha(97) else p.onSurfaceVariant, context.theme.typography.fontFamily)
            var textX = 16f
            if (leadingLabel.isNotBlank()) {
                val iconWidth = context.backend.measureText(leadingLabel, style).first
                context.backend.drawText(leadingLabel, 14f, textBaseline(measuredHeight, style.size), style)
                textX += iconWidth + 8f
            }
            context.backend.drawText(text.takeFitting(context, style, measuredWidth - textX - 16f), textX, textBaseline(measuredHeight, style.size), style)
        }
        if (isAnimating(hover, pressed)) requestRender()
    }
}

class MaterialFilterChip(
    var text: String,
    val selectedState: State<Boolean> = stateOf(false),
    var leadingLabel: String = "",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 112f,
    height: Float = 32f,
    private val onClick: () -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var hover = 0f
    private var pressed = 0f

    init {
        focusable = true
        minSize(52f, 32f)
        selectedState.subscribe(invalidate)
        onDispose { selectedState.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!disabled) {
                setInteraction { copy(pressed = true) }
                requestFocus()
                it.consume()
            }
        }
        on(PointerUp) {
            if (interaction.pressed && !disabled) {
                selectedState.value = !selectedState.value
                onClick()
            }
            setInteraction { copy(pressed = false) }
            it.consume()
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        val selected = selectedState.value
        hover = animate(hover, if (interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 16f)
        pressed = animate(pressed, if (interaction.pressed && !disabled) 1f else 0f, context.time.deltaSeconds, 20f)
        backgroundColor = if (selected) p.secondaryContainer else overlay(Color(0, 0, 0, 0), p.onSurface, 0.06f * hover + 0.10f * pressed, if (disabled) 0.38f else 1f)
        borderColor = if (selected) null else p.outline
        borderWidth = 1f
        cornerRadius = MaterialShapes.Small
        super.draw(context)
        context.backend.translated(x, y) {
            val style = TextStyle(context.theme.typography.bodySize, if (selected) p.onSecondaryContainer else if (disabled) p.onSurface.withAlpha(97) else p.onSurfaceVariant, context.theme.typography.fontFamily)
            var textX = 12f
            if (selected || leadingLabel.isNotBlank()) {
                val icon = if (selected) "✓" else leadingLabel
                val iconWidth = context.backend.measureText(icon, style).first
                context.backend.drawText(icon, 12f, textBaseline(measuredHeight, style.size), style)
                textX += iconWidth + 8f
            }
            context.backend.drawText(text.takeFitting(context, style, measuredWidth - textX - 12f), textX, textBaseline(measuredHeight, style.size), style)
        }
        if (isAnimating(hover, pressed)) requestRender()
    }
}

class MaterialInputChip(
    var text: String,
    var avatarLabel: String = "",
    var trailingLabel: String = "×",
    val selectedState: State<Boolean>? = null,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 124f,
    height: Float = 32f,
    private val onClick: () -> Unit = {},
    private val onRemove: () -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var pressedTrailing = false

    init {
        focusable = true
        minSize(64f, 32f)
        selectedState?.subscribe(invalidate)
        onDispose { selectedState?.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!disabled) {
                setInteraction { copy(pressed = true) }
                requestFocus()
                pressedTrailing = it.x - x >= measuredWidth - 28f
                it.consume()
            }
        }
        on(PointerUp) {
            if (interaction.pressed && !disabled) {
                if (pressedTrailing) {
                    onRemove()
                } else {
                    selectedState?.let { state -> state.value = !(state.value) }
                    onClick()
                }
            }
            pressedTrailing = false
            setInteraction { copy(pressed = false) }
            it.consume()
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        val selected = selectedState?.value == true
        backgroundColor = if (selected) p.secondaryContainer else overlay(Color(0, 0, 0, 0), p.onSurface, if (interaction.pressed) 0.10f else if (interaction.hovered) 0.06f else 0f, if (disabled) 0.38f else 1f)
        borderColor = if (selected) null else p.outline
        borderWidth = 1f
        cornerRadius = MaterialShapes.Small
        super.draw(context)
        context.backend.translated(x, y) {
            val style = TextStyle(context.theme.typography.bodySize, if (selected) p.onSecondaryContainer else if (disabled) p.onSurface.withAlpha(97) else p.onSurfaceVariant, context.theme.typography.fontFamily)
            var textX = 12f
            if (avatarLabel.isNotBlank()) {
                context.backend.drawRoundedRect(10f, 6f, 20f, 20f, 10f, if (selected) p.primary else p.surfaceContainerHighest, null)
                val avatarStyle = TextStyle((style.size - 2f).coerceAtLeast(10f), if (selected) p.onPrimary else p.onSurfaceVariant, style.fontFamily)
                val avatarWidth = context.backend.measureText(avatarLabel, avatarStyle).first
                context.backend.drawText(avatarLabel, 20f - avatarWidth / 2f, textBaseline(32f, avatarStyle.size), avatarStyle)
                textX = 36f
            }
            val trailingWidth = if (trailingLabel.isBlank()) 0f else context.backend.measureText(trailingLabel, style).first + 16f
            context.backend.drawText(text.takeFitting(context, style, measuredWidth - textX - trailingWidth - 8f), textX, textBaseline(measuredHeight, style.size), style)
            if (trailingLabel.isNotBlank()) {
                val trailingTextWidth = context.backend.measureText(trailingLabel, style).first
                context.backend.drawText(trailingLabel, measuredWidth - 14f - trailingTextWidth, textBaseline(measuredHeight, style.size), style)
            }
        }
    }
}

