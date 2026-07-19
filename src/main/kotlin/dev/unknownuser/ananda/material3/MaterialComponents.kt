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
        backgroundColor = if (isSelected) p.secondaryContainer else Color(0, 0, 0, 0)
        borderColor = if (isSelected) null else p.outline
        borderWidth = 1f
        cornerRadius = MaterialShapes.Small
        super.draw(context)
        context.backend.translated(x, y) {
            val color = if (isSelected) p.onSecondaryContainer else p.onSurfaceVariant
            val style = TextStyle(context.theme.typography.bodySize, color, context.theme.typography.fontFamily)
            val textWidth = context.backend.measureText(text, style).first
            context.backend.drawText(text, (measuredWidth - textWidth) / 2f, textBaseline(measuredHeight, style.size), style)
        }
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

enum class MaterialTextFieldStyle { Filled, Outlined }
enum class MaterialTextFieldLineMode { SingleLine, MultiLine }

class MaterialTextField(
    val value: State<String> = stateOf(""),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 280f,
    height: Float = 56f,
    var label: String = "",
    var placeholder: String = "",
    var supportingText: String = "",
    var variant: MaterialTextFieldStyle = MaterialTextFieldStyle.Filled
) : Component(x, y, width, height) {
    private var composingText = ""
    private val invalidate = { requestRender() }
    private var cursor = value.value.length
    private var selectionAnchor: Int? = null
    private var hover = 0f
    private var focused = 0f
    private var labelProgress = if (value.value.isNotEmpty()) 1f else 0f
    private var placeholderProgress = if (label.isBlank() && value.value.isEmpty()) 1f else 0f
    private var selectingWithPointer = false
    private var textBackend: RenderBackend? = null
    private var textStyle: TextStyle? = null

    init {
        focusable = true
        minSize(160f, 56f)
        padding = Insets(16f, 18f, 16f, 12f)
        value.subscribe(invalidate)
        onDispose { value.unsubscribe(invalidate) }
        on(PointerDown) {
            requestFocus()
            val local = sceneToLocal(it.x, it.y)
            cursor = cursorAt(local.first)
            selectionAnchor = cursor
            selectingWithPointer = true
            it.consume()
        }
        on(PointerMove) {
            if (selectingWithPointer && interaction.pressed) {
                cursor = cursorAt(sceneToLocal(it.x, it.y).first)
                requestRender()
                it.consume()
            }
        }
        on(PointerUp) {
            if (selectingWithPointer) {
                cursor = cursorAt(sceneToLocal(it.x, it.y).first)
                selectingWithPointer = false
                it.consume()
            }
        }
        on(Blur) {
            selectionAnchor = null
            selectingWithPointer = false
            requestRender()
        }
        on(TextInput) {
            replaceSelection(it.text)
            it.consume()
        }
        on(ImeCompose) {
            composingText = it.composedText
            requestRender()
        }
        on(ImeCommit) {
            replaceSelection(it.committedText)
            composingText = ""
            it.consume()
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
        val hasInput = value.value.isNotEmpty() || composingText.isNotEmpty()
        hover = animate(hover, if (interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        focused = animate(focused, if (interaction.focused && !disabled) 1f else 0f, context.time.deltaSeconds, 16f)
        labelProgress = animate(
            labelProgress,
            if (label.isNotBlank() && (interaction.focused || hasInput)) 1f else 0f,
            context.time.deltaSeconds,
            18f
        )
        placeholderProgress = animate(
            placeholderProgress,
            if (!hasInput && placeholder.isNotBlank() && (label.isBlank() || interaction.focused)) 1f else 0f,
            context.time.deltaSeconds,
            14f
        )
        val stateAlpha = 0.03f * hover + 0.04f * focused
        val base = if (variant == MaterialTextFieldStyle.Filled) p.surfaceContainerHighest else p.surface
        backgroundColor = overlay(base, p.onSurface, stateAlpha, if (disabled) 0.38f else 1f)
        cornerRadius = MaterialShapes.ExtraSmall
        val restingBorder = when (variant) {
            MaterialTextFieldStyle.Filled -> p.outlineVariant.withAlpha(140)
            MaterialTextFieldStyle.Outlined -> p.outline
        }
        borderColor = lerp(restingBorder, p.primary, focused)
        borderWidth = 1f + focused
        elevationShadow = null
        super.draw(context)
        context.backend.translated(x, y) {
            val bodySize = context.theme.typography.bodySize
            val labelSize = (bodySize - 2f).coerceAtLeast(10f)
            val centeredBaseline = textBaseline(measuredHeight, bodySize)
            val bodyBaseline = if (label.isNotBlank()) {
                centeredBaseline + (40f - centeredBaseline) * labelProgress
            } else {
                centeredBaseline
            }
            if (label.isNotBlank()) {
                val animatedLabelSize = bodySize + (labelSize - bodySize) * labelProgress
                val floatingLabelBaseline = when (variant) {
                    MaterialTextFieldStyle.Filled -> 18f
                    MaterialTextFieldStyle.Outlined -> labelSize * 0.36f + 1f
                }
                val labelBaseline = centeredBaseline + (floatingLabelBaseline - centeredBaseline) * labelProgress
                val labelColor = lerp(p.onSurfaceVariant, p.primary, focused)
                if (variant == MaterialTextFieldStyle.Outlined && labelProgress > 0.001f) {
                    val labelWidth = context.backend.measureText(
                        label,
                        TextStyle(labelSize, labelColor, context.theme.typography.fontFamily)
                    ).first
                    val fullNotchWidth = labelWidth + 10f
                    val notchWidth = fullNotchWidth * labelProgress
                    val notchX = padding.left - 5f + (fullNotchWidth - notchWidth) / 2f
                    context.backend.drawRect(
                        notchX,
                        -2f,
                        notchWidth,
                        4f,
                        backgroundColor,
                        null
                    )
                }
                context.backend.drawText(
                    label,
                    padding.left,
                    labelBaseline,
                    TextStyle(animatedLabelSize, labelColor.withAlpha(if (disabled) 110 else 255), context.theme.typography.fontFamily)
                )
            }
            val bodyStyle = TextStyle(
                bodySize,
                if (disabled) p.onSurface.withAlpha(97) else p.onSurface,
                context.theme.typography.fontFamily
            )
            textBackend = context.backend
            textStyle = bodyStyle
            val textCenterY = bodyBaseline - bodySize * 0.36f
            val caretHeight = (bodySize * 1.25f).coerceAtLeast(16f)
            val caretTop = textCenterY - caretHeight / 2f
            val caretBottom = textCenterY + caretHeight / 2f
            if (interaction.focused) {
                val selection = selectionRange()
                if (selection != null) {
                    val prefix = value.value.substring(0, selection.first)
                    val selected = value.value.substring(0, selection.second)
                    val startX = padding.left + context.backend.measureText(prefix, bodyStyle).first
                    val endX = padding.left + context.backend.measureText(selected, bodyStyle).first
                    context.backend.drawRoundedRect(
                        startX,
                        caretTop,
                        endX - startX,
                        caretHeight,
                        4f,
                        p.primary.withAlpha(72),
                        null
                    )
                }
            }
            if (hasInput) {
                context.backend.drawText(value.value + composingText, padding.left, bodyBaseline, bodyStyle)
            } else if (placeholderProgress > 0.001f) {
                val labelClearance = if (label.isBlank()) 1f else ((labelProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
                val placeholderAlpha = (placeholderProgress * labelClearance * if (disabled) 0.38f else 1f)
                if (placeholderAlpha > 0.001f) {
                    context.backend.drawText(
                        placeholder,
                        padding.left,
                        bodyBaseline,
                        TextStyle(
                            bodySize,
                            p.onSurfaceVariant.withAlpha((255f * placeholderAlpha).toInt().coerceIn(0, 255)),
                            context.theme.typography.fontFamily
                        )
                    )
                }
            }
            if (interaction.focused) {
                val caretSource = value.value.substring(0, cursor.coerceIn(0, value.value.length)) + composingText
                val caretX = padding.left + context.backend.measureText(caretSource, bodyStyle).first + 1f
                context.backend.drawLine(
                    caretX,
                    caretTop,
                    caretX,
                    caretBottom,
                    Stroke(p.primary, 1.5f)
                )
            }
            if (supportingText.isNotBlank()) {
                context.backend.drawText(
                    supportingText,
                    padding.left,
                    measuredHeight + 14f,
                    TextStyle(labelSize, p.onSurfaceVariant.withAlpha(if (disabled) 110 else 255), context.theme.typography.fontFamily)
                )
            }
        }
        if (isAnimating(hover, focused, labelProgress, placeholderProgress)) requestRender()
    }

    private fun handleKey(event: KeyEvent): Boolean {
        val ctrl = event.modifiers and InputEvent.CTRL_DOWN_MASK != 0
        val shift = event.modifiers and InputEvent.SHIFT_DOWN_MASK != 0
        return when (event.keyCode) {
            AwtKeyEvent.VK_A -> if (ctrl) {
                cursor = value.value.length
                selectionAnchor = 0
                true
            } else false
            AwtKeyEvent.VK_C -> if (ctrl) {
                selectedText()?.let { setClipboard(it) }
                true
            } else false
            AwtKeyEvent.VK_X -> if (ctrl) {
                selectedText()?.let {
                    setClipboard(it)
                    replaceSelection("")
                }
                true
            } else false
            AwtKeyEvent.VK_V -> if (ctrl) {
                replaceSelection(getClipboard())
                true
            } else false
            AwtKeyEvent.VK_LEFT -> {
                moveCursor((cursor - 1).coerceAtLeast(0), shift)
                true
            }
            AwtKeyEvent.VK_RIGHT -> {
                moveCursor((cursor + 1).coerceAtMost(value.value.length), shift)
                true
            }
            AwtKeyEvent.VK_HOME -> {
                moveCursor(0, shift)
                true
            }
            AwtKeyEvent.VK_END -> {
                moveCursor(value.value.length, shift)
                true
            }
            AwtKeyEvent.VK_BACK_SPACE -> {
                if (selectionRange() != null) replaceSelection("") else if (cursor > 0) {
                    value.value = value.value.removeRange(cursor - 1, cursor)
                    cursor -= 1
                }
                true
            }
            AwtKeyEvent.VK_DELETE -> {
                if (selectionRange() != null) replaceSelection("") else if (cursor < value.value.length) {
                    value.value = value.value.removeRange(cursor, cursor + 1)
                }
                true
            }
            else -> false
        }
    }

    private fun moveCursor(next: Int, extendingSelection: Boolean) {
        if (extendingSelection) {
            if (selectionAnchor == null) selectionAnchor = cursor
        } else {
            selectionAnchor = null
        }
        cursor = next.coerceIn(0, value.value.length)
    }

    private fun replaceSelection(text: String) {
        val range = selectionRange()
        if (range != null) {
            value.value = value.value.replaceRange(range.first, range.second, text)
            cursor = range.first + text.length
        } else {
            value.value = value.value.substring(0, cursor) + text + value.value.substring(cursor)
            cursor += text.length
        }
        selectionAnchor = null
    }

    private fun selectionRange(): Pair<Int, Int>? {
        val anchor = selectionAnchor ?: return null
        if (anchor == cursor) return null
        val start = minOf(anchor, cursor).coerceIn(0, value.value.length)
        val end = maxOf(anchor, cursor).coerceIn(0, value.value.length)
        return start to end
    }

    private fun selectedText(): String? = selectionRange()?.let { value.value.substring(it.first, it.second) }

    private fun cursorAt(localX: Float): Int {
        val backend = textBackend ?: return value.value.length
        val style = textStyle ?: return value.value.length
        val target = (localX - padding.left).coerceAtLeast(0f)
        var previousWidth = 0f
        for (index in 1..value.value.length) {
            val width = backend.measureText(value.value.substring(0, index), style).first
            if (target < (previousWidth + width) / 2f) return index - 1
            previousWidth = width
        }
        return value.value.length
    }

    private fun setClipboard(text: String) {
        runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) }
    }

    private fun getClipboard(): String =
        runCatching { Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String ?: "" }.getOrDefault("")
}

class MaterialTextArea(
    val value: State<String> = stateOf(""),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 280f,
    height: Float = 120f,
    var label: String = "",
    var placeholder: String = "",
    var supportingText: String = "",
    var variant: MaterialTextFieldStyle = MaterialTextFieldStyle.Filled
) : Component(x, y, width, height) {
    private var composingText = ""
    private val invalidate = { requestRender() }
    private var cursor = value.value.length
    private var selectionAnchor: Int? = null
    private var hover = 0f
    private var focused = 0f

    init {
        focusable = true
        minSize(160f, 96f)
        padding = Insets(16f, 16f, 16f, 16f)
        value.subscribe(invalidate)
        onDispose { value.unsubscribe(invalidate) }
        on(PointerDown) {
            requestFocus()
            it.consume()
        }
        on(TextInput) {
            replaceSelection(it.text)
            it.consume()
        }
        on(ImeCompose) {
            composingText = it.composedText
            requestRender()
        }
        on(ImeCommit) {
            replaceSelection(it.committedText)
            composingText = ""
            it.consume()
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
        focused = animate(focused, if (interaction.focused && !disabled) 1f else 0f, context.time.deltaSeconds, 16f)
        val stateAlpha = 0.03f * hover + 0.04f * focused
        val base = if (variant == MaterialTextFieldStyle.Filled) p.surfaceContainerHighest else p.surface
        backgroundColor = overlay(base, p.onSurface, stateAlpha, if (disabled) 0.38f else 1f)
        cornerRadius = MaterialShapes.ExtraSmall
        borderColor = when (variant) {
            MaterialTextFieldStyle.Filled -> if (interaction.focused) p.primary else p.outlineVariant.withAlpha(140)
            MaterialTextFieldStyle.Outlined -> if (interaction.focused) p.primary else p.outline
        }
        borderWidth = if (interaction.focused) 2f else 1f
        elevationShadow = null
        super.draw(context)
        context.backend.translated(x, y) {
            val bodySize = context.theme.typography.bodySize
            val labelSize = (bodySize - 2f).coerceAtLeast(10f)
            val lineHeight = bodySize + 6f
            val contentTop = if (label.isNotBlank()) 34f else 18f
            val contentBottom = measuredHeight - 14f
            if (label.isNotBlank()) {
                val labelColor = if (interaction.focused) p.primary else p.onSurfaceVariant
                context.backend.drawText(
                    label,
                    padding.left,
                    18f,
                    TextStyle(labelSize, labelColor.withAlpha(if (disabled) 110 else 255), context.theme.typography.fontFamily)
                )
            }
            val hasInput = value.value.isNotEmpty() || composingText.isNotEmpty()
            val displayValue = if (hasInput) {
                value.value.substring(0, cursor.coerceIn(0, value.value.length)) + composingText + value.value.substring(cursor.coerceIn(0, value.value.length))
            } else {
                placeholder
            }
            val textColor = when {
                disabled -> p.onSurface.withAlpha(97)
                hasInput -> p.onSurface
                else -> p.onSurfaceVariant
            }
            val bodyStyle = TextStyle(bodySize, textColor, context.theme.typography.fontFamily)
            val lines = displayValue.split('\n')
            val maxLines = ((contentBottom - contentTop) / lineHeight).toInt().coerceAtLeast(1)
            lines.take(maxLines).forEachIndexed { index, line ->
                val baseline = contentTop + index * lineHeight
                context.backend.drawText(
                    line.takeFitting(context, bodyStyle, measuredWidth - padding.left - padding.right),
                    padding.left,
                    baseline,
                    bodyStyle
                )
            }
            if (interaction.focused) {
                val caretPrefix = value.value.substring(0, cursor.coerceIn(0, value.value.length)) + composingText
                val caretLines = caretPrefix.split('\n')
                val caretLine = (caretLines.size - 1).coerceAtLeast(0)
                if (caretLine < maxLines) {
                    val caretColumnText = caretLines.lastOrNull().orEmpty()
                    val caretX = padding.left + context.backend.measureText(caretColumnText.takeFitting(context, bodyStyle, measuredWidth - padding.left - padding.right), bodyStyle).first + 1f
                    val caretBaseline = contentTop + caretLine * lineHeight
                    context.backend.drawLine(
                        caretX,
                        caretBaseline - bodySize + 2f,
                        caretX,
                        caretBaseline + 3f,
                        Stroke(p.primary, 1.5f)
                    )
                }
            }
            if (supportingText.isNotBlank()) {
                context.backend.drawText(
                    supportingText,
                    padding.left,
                    measuredHeight + 14f,
                    TextStyle(labelSize, p.onSurfaceVariant.withAlpha(if (disabled) 110 else 255), context.theme.typography.fontFamily)
                )
            }
        }
        if (isAnimating(hover, focused)) requestRender()
    }

    private fun handleKey(event: KeyEvent): Boolean {
        val ctrl = event.modifiers and InputEvent.CTRL_DOWN_MASK != 0
        val shift = event.modifiers and InputEvent.SHIFT_DOWN_MASK != 0
        return when (event.keyCode) {
            AwtKeyEvent.VK_A -> if (ctrl) {
                cursor = value.value.length
                selectionAnchor = 0
                true
            } else false
            AwtKeyEvent.VK_C -> if (ctrl) {
                selectedText()?.let { setClipboard(it) }
                true
            } else false
            AwtKeyEvent.VK_X -> if (ctrl) {
                selectedText()?.let {
                    setClipboard(it)
                    replaceSelection("")
                }
                true
            } else false
            AwtKeyEvent.VK_V -> if (ctrl) {
                replaceSelection(getClipboard())
                true
            } else false
            AwtKeyEvent.VK_ENTER -> {
                replaceSelection("\n")
                true
            }
            AwtKeyEvent.VK_LEFT -> {
                moveCursor((cursor - 1).coerceAtLeast(0), shift)
                true
            }
            AwtKeyEvent.VK_RIGHT -> {
                moveCursor((cursor + 1).coerceAtMost(value.value.length), shift)
                true
            }
            AwtKeyEvent.VK_UP -> {
                moveVertical(-1, shift)
                true
            }
            AwtKeyEvent.VK_DOWN -> {
                moveVertical(1, shift)
                true
            }
            AwtKeyEvent.VK_HOME -> {
                moveCursor(lineStart(cursor), shift)
                true
            }
            AwtKeyEvent.VK_END -> {
                moveCursor(lineEnd(cursor), shift)
                true
            }
            AwtKeyEvent.VK_BACK_SPACE -> {
                if (selectionRange() != null) replaceSelection("") else if (cursor > 0) {
                    value.value = value.value.removeRange(cursor - 1, cursor)
                    cursor -= 1
                }
                true
            }
            AwtKeyEvent.VK_DELETE -> {
                if (selectionRange() != null) replaceSelection("") else if (cursor < value.value.length) {
                    value.value = value.value.removeRange(cursor, cursor + 1)
                }
                true
            }
            else -> false
        }
    }

    private fun moveVertical(direction: Int, extendingSelection: Boolean) {
        val column = cursor - lineStart(cursor)
        val currentLineStart = lineStart(cursor)
        val currentLineIndex = value.value.substring(0, currentLineStart.coerceIn(0, value.value.length)).count { it == '\n' }
        val lines = value.value.split('\n')
        val targetLine = (currentLineIndex + direction).coerceIn(0, lines.lastIndex.coerceAtLeast(0))
        var index = 0
        repeat(targetLine) { index += lines[it].length + 1 }
        index += column.coerceAtMost(lines.getOrElse(targetLine) { "" }.length)
        moveCursor(index, extendingSelection)
    }

    private fun lineStart(index: Int): Int {
        val safe = index.coerceIn(0, value.value.length)
        val newline = value.value.lastIndexOf('\n', safe - 1)
        return if (newline == -1) 0 else newline + 1
    }

    private fun lineEnd(index: Int): Int {
        val safe = index.coerceIn(0, value.value.length)
        val newline = value.value.indexOf('\n', safe)
        return if (newline == -1) value.value.length else newline
    }

    private fun moveCursor(next: Int, extendingSelection: Boolean) {
        if (extendingSelection) {
            if (selectionAnchor == null) selectionAnchor = cursor
        } else {
            selectionAnchor = null
        }
        cursor = next.coerceIn(0, value.value.length)
    }

    private fun replaceSelection(text: String) {
        val range = selectionRange()
        if (range != null) {
            value.value = value.value.replaceRange(range.first, range.second, text)
            cursor = range.first + text.length
        } else {
            value.value = value.value.substring(0, cursor) + text + value.value.substring(cursor)
            cursor += text.length
        }
        selectionAnchor = null
    }

    private fun selectionRange(): Pair<Int, Int>? {
        val anchor = selectionAnchor ?: return null
        if (anchor == cursor) return null
        val start = minOf(anchor, cursor).coerceIn(0, value.value.length)
        val end = maxOf(anchor, cursor).coerceIn(0, value.value.length)
        return start to end
    }

    private fun selectedText(): String? = selectionRange()?.let { value.value.substring(it.first, it.second) }

    private fun setClipboard(text: String) {
        runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) }
    }

    private fun getClipboard(): String =
        runCatching { Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String ?: "" }.getOrDefault("")
}

class MaterialCheckbox(
    var text: String = "",
    val checked: State<Boolean> = stateOf(false),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 180f,
    height: Float = 40f
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var hover = 0f
    private var focused = 0f

    init {
        focusable = true
        minSize(48f, 40f)
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
        val p = context.theme.palette
        hover = animate(hover, if (interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        focused = animate(focused, if (interaction.focused && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        val boxSize = 18f
        val boxY = (measuredHeight - boxSize) / 2f
        val boxColor = when {
            disabled && checked.value -> p.onSurface.withAlpha(40)
            checked.value -> p.primary
            else -> overlay(p.surface, p.onSurface, hover * 0.04f, 1f)
        }
        val strokeColor = when {
            checked.value -> p.primary
            interaction.focused -> p.primary
            else -> p.outline
        }
        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(0f, boxY, boxSize, boxSize, 2f, boxColor, Stroke(strokeColor.withAlpha(if (disabled) 90 else 255), 2f))
            if (checked.value) {
                val mark = if (disabled) p.onSurface.withAlpha(150) else p.onPrimary
                context.backend.drawLine(4f, boxY + 9f, 8f, boxY + 13f, Stroke(mark, 2f))
                context.backend.drawLine(8f, boxY + 13f, 14f, boxY + 5f, Stroke(mark, 2f))
            }
            if (text.isNotBlank()) {
                context.backend.drawText(
                    text,
                    28f,
                    textBaseline(measuredHeight, context.theme.typography.bodySize),
                    TextStyle(context.theme.typography.bodySize, if (disabled) p.onSurface.withAlpha(97) else p.onSurface, context.theme.typography.fontFamily)
                )
            }
        }
        if (isAnimating(hover, focused)) requestRender()
    }

    private fun toggle() {
        if (!disabled) checked.value = !checked.value
    }
}

class MaterialRadioButton<T>(
    val selection: State<T>,
    val option: T,
    var text: String = option.toString(),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 180f,
    height: Float = 40f
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var hover = 0f
    private var focused = 0f

    init {
        focusable = true
        minSize(48f, 40f)
        selection.subscribe(invalidate)
        onDispose { selection.unsubscribe(invalidate) }
        on(PointerDown) {
            select()
            requestFocus()
            it.consume()
        }
        on(KeyDown.Enter or KeyDown.Space) {
            select()
            it.consume()
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        hover = animate(hover, if (interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        focused = animate(focused, if (interaction.focused && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        val selectedNow = selection.value == option
        val centerY = measuredHeight / 2f
        val ringColor = when {
            disabled -> p.onSurface.withAlpha(90)
            selectedNow || interaction.focused -> p.primary
            else -> p.outline
        }
        context.backend.translated(x, y) {
            context.backend.drawCircle(10f, centerY, 9f, overlay(p.surface, p.onSurface, hover * 0.03f, 1f), Stroke(ringColor, 2f))
            if (selectedNow) {
                context.backend.drawCircle(10f, centerY, 4.5f, if (disabled) p.onSurface.withAlpha(110) else p.primary)
            }
            if (text.isNotBlank()) {
                context.backend.drawText(
                    text,
                    28f,
                    textBaseline(measuredHeight, context.theme.typography.bodySize),
                    TextStyle(context.theme.typography.bodySize, if (disabled) p.onSurface.withAlpha(97) else p.onSurface, context.theme.typography.fontFamily)
                )
            }
        }
        if (isAnimating(hover, focused)) requestRender()
    }

    private fun select() {
        if (!disabled) selection.value = option
    }
}

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

data class MaterialActionItem(
    val label: String,
    val onClick: () -> Unit = {}
)

class MaterialIconButton(
    var label: String,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 40f,
    height: Float = 40f,
    var filled: Boolean = false,
    private val onClick: () -> Unit = {}
) : Component(x, y, width, height) {
    private var hover = 0f
    private var pressed = 0f
    private var focused = 0f

    init {
        focusable = true
        minSize(40f, 40f)
        on(PointerDown) {
            if (!disabled) {
                setInteraction { copy(pressed = true) }
                requestFocus()
            }
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
        pressed = animate(pressed, if (interaction.pressed && !disabled) 1f else 0f, context.time.deltaSeconds, 20f)
        focused = animate(focused, if (interaction.focused && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        val base = if (filled) p.secondaryContainer else Color(0, 0, 0, 0)
        backgroundColor = overlay(base, p.onSurface, 0.08f * hover + 0.10f * pressed + 0.08f * focused, if (disabled) 0.38f else 1f)
        cornerRadius = measuredHeight / 2f
        borderColor = null
        borderWidth = 0f
        elevationShadow = null
        super.draw(context)
        context.backend.translated(x, y) {
            val style = TextStyle(context.theme.typography.bodySize, if (disabled) p.onSurface.withAlpha(97) else p.onSurfaceVariant, context.theme.typography.fontFamily)
            val textWidth = context.backend.measureText(label, style).first
            context.backend.drawText(label, (measuredWidth - textWidth) / 2f, textBaseline(measuredHeight, style.size), style)
        }
        if (isAnimating(hover, pressed, focused)) requestRender()
    }
}

class MaterialFloatingActionButton(
    var label: String,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 56f,
    height: Float = 56f,
    var expandedText: String = "",
    private val onClick: () -> Unit = {}
) : Component(x, y, width, height) {
    private var hover = 0f
    private var pressed = 0f
    private var focused = 0f

    init {
        focusable = true
        minSize(56f, 56f)
        on(PointerDown) {
            if (!disabled) {
                setInteraction { copy(pressed = true) }
                requestFocus()
            }
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
        pressed = animate(pressed, if (interaction.pressed && !disabled) 1f else 0f, context.time.deltaSeconds, 20f)
        focused = animate(focused, if (interaction.focused && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        backgroundColor = overlay(p.secondaryContainer, p.onSecondaryContainer, 0.08f * hover + 0.10f * pressed + 0.08f * focused, if (disabled) 0.38f else 1f)
        cornerRadius = measuredHeight / 2f
        borderColor = null
        borderWidth = 0f
        elevationShadow = if (disabled) null else Shadow(Color(0, 0, 0, 28), 14f, offsetY = 2f)
        super.draw(context)
        context.backend.translated(x, y) {
            val iconStyle = TextStyle(context.theme.typography.titleSize, if (disabled) p.onSurface.withAlpha(97) else p.onSecondaryContainer, context.theme.typography.fontFamily)
            val iconWidth = context.backend.measureText(label, iconStyle).first
            val iconX = if (expandedText.isBlank()) (measuredWidth - iconWidth) / 2f else 16f
            context.backend.drawText(label, iconX, textBaseline(measuredHeight, iconStyle.size), iconStyle)
            if (expandedText.isNotBlank()) {
                val textStyle = TextStyle(context.theme.typography.bodySize, if (disabled) p.onSurface.withAlpha(97) else p.onSecondaryContainer, context.theme.typography.fontFamily)
                context.backend.drawText(expandedText, 16f + iconWidth + 12f, textBaseline(measuredHeight, textStyle.size), textStyle)
            }
        }
        if (isAnimating(hover, pressed, focused)) requestRender()
    }
}

class MaterialIconToggleButton(
    var label: String,
    val checked: State<Boolean> = stateOf(false),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 40f,
    height: Float = 40f,
    var filledWhenChecked: Boolean = true,
    private val onToggle: (Boolean) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var hover = 0f
    private var pressed = 0f
    private var focused = 0f

    init {
        focusable = true
        minSize(40f, 40f)
        checked.subscribe(invalidate)
        onDispose { checked.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!disabled) {
                setInteraction { copy(pressed = true) }
                requestFocus()
            }
            it.consume()
        }
        on(PointerUp) {
            if (interaction.pressed && !disabled) toggle()
            setInteraction { copy(pressed = false) }
            it.consume()
        }
        on(KeyDown.Enter or KeyDown.Space) {
            if (!disabled) toggle()
            it.consume()
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        val isChecked = checked.value
        hover = animate(hover, if (interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 16f)
        pressed = animate(pressed, if (interaction.pressed && !disabled) 1f else 0f, context.time.deltaSeconds, 20f)
        focused = animate(focused, if (interaction.focused && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        val filled = isChecked && filledWhenChecked
        val base = when {
            filled -> p.secondaryContainer
            isChecked -> overlay(p.surfaceContainerHigh, p.primary, 0.10f, 1f)
            else -> Color(0, 0, 0, 0)
        }
        backgroundColor = overlay(base, if (filled) p.onSecondaryContainer else p.onSurface, 0.08f * hover + 0.10f * pressed + 0.08f * focused, if (disabled) 0.38f else 1f)
        cornerRadius = measuredHeight / 2f
        borderColor = if (!filled && !isChecked) p.outlineVariant else null
        borderWidth = if (!filled && !isChecked) 1f else 0f
        elevationShadow = null
        super.draw(context)
        context.backend.translated(x, y) {
            val color = when {
                disabled -> p.onSurface.withAlpha(97)
                filled -> p.onSecondaryContainer
                isChecked -> p.primary
                else -> p.onSurfaceVariant
            }
            val style = TextStyle(context.theme.typography.bodySize, color, context.theme.typography.fontFamily)
            val textWidth = context.backend.measureText(label, style).first
            context.backend.drawText(label, (measuredWidth - textWidth) / 2f, textBaseline(measuredHeight, style.size), style)
        }
        if (isAnimating(hover, pressed, focused)) requestRender()
    }

    private fun toggle() {
        checked.value = !checked.value
        onToggle(checked.value)
    }
}

class MaterialSmallFloatingActionButton(
    var label: String,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 40f,
    height: Float = 40f,
    private val onClick: () -> Unit = {}
) : Component(x, y, width, height) {
    private var hover = 0f
    private var pressed = 0f
    private var focused = 0f

    init {
        focusable = true
        minSize(40f, 40f)
        on(PointerDown) {
            if (!disabled) {
                setInteraction { copy(pressed = true) }
                requestFocus()
            }
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
        pressed = animate(pressed, if (interaction.pressed && !disabled) 1f else 0f, context.time.deltaSeconds, 20f)
        focused = animate(focused, if (interaction.focused && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        backgroundColor = overlay(p.secondaryContainer, p.onSecondaryContainer, 0.08f * hover + 0.10f * pressed + 0.08f * focused, if (disabled) 0.38f else 1f)
        cornerRadius = 12f
        borderColor = null
        borderWidth = 0f
        elevationShadow = if (disabled) null else Shadow(Color(0, 0, 0, 26), 12f, offsetY = 2f)
        super.draw(context)
        context.backend.translated(x, y) {
            val style = TextStyle((context.theme.typography.titleSize - 2f).coerceAtLeast(12f), if (disabled) p.onSurface.withAlpha(97) else p.onSecondaryContainer, context.theme.typography.fontFamily)
            val textWidth = context.backend.measureText(label, style).first
            context.backend.drawText(label, (measuredWidth - textWidth) / 2f, textBaseline(measuredHeight, style.size), style)
        }
        if (isAnimating(hover, pressed, focused)) requestRender()
    }
}

class MaterialLargeFloatingActionButton(
    var largeLabel: String,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 96f,
    height: Float = 96f,
    private val onClick: () -> Unit = {}
) : Component(x, y, width, height) {
    private var hover = 0f
    private var pressed = 0f
    private var focused = 0f

    init {
        focusable = true
        minSize(96f, 96f)
        on(PointerDown) {
            if (!disabled) {
                setInteraction { copy(pressed = true) }
                requestFocus()
            }
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
        pressed = animate(pressed, if (interaction.pressed && !disabled) 1f else 0f, context.time.deltaSeconds, 20f)
        focused = animate(focused, if (interaction.focused && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        backgroundColor = overlay(p.secondaryContainer, p.onSecondaryContainer, 0.08f * hover + 0.10f * pressed + 0.08f * focused, if (disabled) 0.38f else 1f)
        cornerRadius = 28f
        borderColor = null
        borderWidth = 0f
        elevationShadow = if (disabled) null else Shadow(Color(0, 0, 0, 28), 16f, offsetY = 2f)
        super.draw(context)
        context.backend.translated(x, y) {
            val style = TextStyle(context.theme.typography.titleSize + 6f, if (disabled) p.onSurface.withAlpha(97) else p.onSecondaryContainer, context.theme.typography.fontFamily)
            val textWidth = context.backend.measureText(largeLabel, style).first
            context.backend.drawText(largeLabel, (measuredWidth - textWidth) / 2f, textBaseline(measuredHeight, style.size), style)
        }
        if (isAnimating(hover, pressed, focused)) requestRender()
    }
}

class MaterialExtendedFloatingActionButton(
    var label: String,
    var text: String,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 140f,
    height: Float = 56f,
    private val onClick: () -> Unit = {}
) : Component(x, y, width, height) {
    private var hover = 0f
    private var pressed = 0f
    private var focused = 0f

    init {
        focusable = true
        minSize(80f, 56f)
        on(PointerDown) {
            if (!disabled) {
                setInteraction { copy(pressed = true) }
                requestFocus()
            }
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
        pressed = animate(pressed, if (interaction.pressed && !disabled) 1f else 0f, context.time.deltaSeconds, 20f)
        focused = animate(focused, if (interaction.focused && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        backgroundColor = overlay(p.secondaryContainer, p.onSecondaryContainer, 0.08f * hover + 0.10f * pressed + 0.08f * focused, if (disabled) 0.38f else 1f)
        cornerRadius = measuredHeight / 2f
        borderColor = null
        borderWidth = 0f
        elevationShadow = if (disabled) null else Shadow(Color(0, 0, 0, 28), 14f, offsetY = 2f)
        super.draw(context)
        context.backend.translated(x, y) {
            val iconStyle = TextStyle(context.theme.typography.titleSize, if (disabled) p.onSurface.withAlpha(97) else p.onSecondaryContainer, context.theme.typography.fontFamily)
            val iconWidth = context.backend.measureText(label, iconStyle).first
            context.backend.drawText(label, 16f, textBaseline(measuredHeight, iconStyle.size), iconStyle)
            val textStyle = TextStyle(context.theme.typography.bodySize, if (disabled) p.onSurface.withAlpha(97) else p.onSecondaryContainer, context.theme.typography.fontFamily)
            context.backend.drawText(text, 16f + iconWidth + 12f, textBaseline(measuredHeight, textStyle.size), textStyle)
        }
        if (isAnimating(hover, pressed, focused)) requestRender()
    }
}

data class MaterialSpeedDialAction(
    val label: String,
    val iconLabel: String = "+",
    val supportingText: String = "",
    val onClick: () -> Unit = {}
)

class MaterialSpeedDial(
    val expanded: State<Boolean> = stateOf(false),
    val actions: List<MaterialSpeedDialAction>,
    var fabLabel: String = "+",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 220f,
    height: Float = 220f,
    private val onExpandedChange: (Boolean) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var hoverIndex = Int.MIN_VALUE
    private var pressedIndex = Int.MIN_VALUE
    private var expansion = if (expanded.value) 1f else 0f
    private var focused = 0f

    init {
        focusable = true
        minSize(56f, 56f)
        expanded.subscribe(invalidate)
        onDispose { expanded.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!disabled) {
                requestFocus()
                pressedIndex = actionAt(it.x - x, it.y - y)
            }
            it.consume()
        }
        on(PointerMove) {
            hoverIndex = actionAt(it.x - x, it.y - y)
            requestRender()
        }
        on(PointerUp) {
            if (!disabled) {
                val releasedIndex = actionAt(it.x - x, it.y - y)
                if (releasedIndex == pressedIndex) {
                    when {
                        releasedIndex == -1 -> toggleExpanded()
                        releasedIndex >= 0 && releasedIndex < actions.size -> {
                            actions[releasedIndex].onClick()
                            if (expanded.value) {
                                expanded.value = false
                                onExpandedChange(false)
                            }
                        }
                    }
                }
            }
            pressedIndex = Int.MIN_VALUE
            it.consume()
        }
        on(KeyDown.Enter or KeyDown.Space) {
            if (!disabled) toggleExpanded()
            it.consume()
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        val contentWidth = measuredWidth.coerceAtLeast(width)
        val contentHeight = measuredHeight.coerceAtLeast(height)
        expansion = animate(expansion, if (expanded.value) 1f else 0f, context.time.deltaSeconds, 14f)
        focused = animate(focused, if (interaction.focused && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        backgroundColor = Color(0, 0, 0, 0)
        borderColor = null
        borderWidth = 0f
        elevationShadow = null
        super.draw(context)
        context.backend.translated(x, y) {
            if (expansion > 0.001f) {
                var offsetY = 12f
                actions.forEachIndexed { index, action ->
                    val rowHeight = if (action.supportingText.isBlank()) 40f else 56f
                    val rowY = contentHeight - 56f - offsetY - rowHeight
                    offsetY += rowHeight + 12f
                    val hovered = hoverIndex == index
                    val pressed = pressedIndex == index
                    val labelWidth = 144f
                    val iconSize = 40f
                    val rowX = (contentWidth - iconSize - 12f - labelWidth).coerceAtLeast(0f)
                    val alpha = (expansion * 255f).toInt().coerceIn(0, 255)
                    val labelColor = overlay(p.surfaceContainerHigh, p.onSurface, if (pressed) 0.10f else if (hovered) 0.06f else 0f, 1f).withAlpha(alpha)
                    val iconColor = overlay(p.primaryContainer, p.onPrimaryContainer, if (pressed) 0.10f else if (hovered) 0.06f else 0f, 1f).withAlpha(alpha)
                    context.backend.drawRoundedRect(rowX, rowY, labelWidth, rowHeight, rowHeight / 2f, labelColor, null)
                    context.backend.drawRoundedRect(rowX + labelWidth + 12f, rowY + (rowHeight - iconSize) / 2f, iconSize, iconSize, 20f, iconColor, null)
                    val headlineStyle = TextStyle(context.theme.typography.bodySize, p.onSurface.withAlpha(alpha), context.theme.typography.fontFamily)
                    val supportingStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), p.onSurfaceVariant.withAlpha(alpha), context.theme.typography.fontFamily)
                    if (action.supportingText.isBlank()) {
                        val labelWidthPx = context.backend.measureText(action.label, headlineStyle).first
                        context.backend.drawText(action.label, rowX + (labelWidth - labelWidthPx) / 2f, rowY + textBaseline(rowHeight, headlineStyle.size), headlineStyle)
                    } else {
                        context.backend.drawText(action.label.takeFitting(context, headlineStyle, labelWidth - 24f), rowX + 16f, rowY + 22f, headlineStyle)
                        context.backend.drawText(action.supportingText.takeFitting(context, supportingStyle, labelWidth - 24f), rowX + 16f, rowY + 40f, supportingStyle)
                    }
                    val iconStyle = TextStyle(context.theme.typography.bodySize, p.onPrimaryContainer.withAlpha(alpha), context.theme.typography.fontFamily)
                    val iconWidth = context.backend.measureText(action.iconLabel, iconStyle).first
                    context.backend.drawText(action.iconLabel, rowX + labelWidth + 12f + (iconSize - iconWidth) / 2f, rowY + (rowHeight - iconSize) / 2f + textBaseline(iconSize, iconStyle.size), iconStyle)
                }
            }
            val fabBounds = fabBounds(contentWidth, contentHeight)
            val mainHovered = hoverIndex == -1
            val mainPressed = pressedIndex == -1
            val stateAlpha = 0.08f * if (mainHovered && !disabled) 1f else 0f + 0.10f * if (mainPressed && !disabled) 1f else 0f + 0.08f * focused
            context.backend.drawRoundedRect(fabBounds.x, fabBounds.y, fabBounds.width, fabBounds.height, fabBounds.height / 2f, overlay(p.secondaryContainer, p.onSecondaryContainer, stateAlpha, if (disabled) 0.38f else 1f), null)
            val icon = if (expanded.value) "×" else fabLabel
            val iconStyle = TextStyle(context.theme.typography.titleSize, if (disabled) p.onSurface.withAlpha(97) else p.onSecondaryContainer, context.theme.typography.fontFamily)
            val iconWidth = context.backend.measureText(icon, iconStyle).first
            context.backend.drawText(icon, fabBounds.x + (fabBounds.width - iconWidth) / 2f, fabBounds.y + textBaseline(fabBounds.height, iconStyle.size), iconStyle)
        }
        if (abs(expansion - if (expanded.value) 1f else 0f) > 0.001f || abs(focused - if (interaction.focused && !disabled) 1f else 0f) > 0.001f) requestRender()
    }

    private fun toggleExpanded() {
        val next = !expanded.value
        expanded.value = next
        onExpandedChange(next)
    }

    private fun actionAt(localX: Float, localY: Float): Int {
        val contentWidth = measuredWidth.coerceAtLeast(width)
        val contentHeight = measuredHeight.coerceAtLeast(height)
        val fabBounds = fabBounds(contentWidth, contentHeight)
        if (localX in fabBounds.x..(fabBounds.x + fabBounds.width) && localY in fabBounds.y..(fabBounds.y + fabBounds.height)) return -1
        if (!expanded.value) return Int.MIN_VALUE
        var offsetY = 12f
        actions.forEachIndexed { index, action ->
            val rowHeight = if (action.supportingText.isBlank()) 40f else 56f
            val rowY = contentHeight - 56f - offsetY - rowHeight
            offsetY += rowHeight + 12f
            val labelWidth = 144f
            val iconSize = 40f
            val rowX = (contentWidth - iconSize - 12f - labelWidth).coerceAtLeast(0f)
            if (localX in rowX..(rowX + labelWidth + 12f + iconSize) && localY in rowY..(rowY + rowHeight)) return index
        }
        return Int.MIN_VALUE
    }

    private fun fabBounds(contentWidth: Float, contentHeight: Float): DialogBounds =
        DialogBounds(contentWidth - 56f, contentHeight - 56f, 56f, 56f)
}

class MaterialBottomAppBar(
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 80f,
    var navigationLabel: String = "",
    var actions: List<MaterialActionItem> = emptyList(),
    var fabLabel: String = "",
    var fabCradle: Boolean = true,
    private val onNavigationClick: () -> Unit = {},
    private val onFabClick: () -> Unit = {}
) : Component(x, y, width, height) {
    private var hoverIndex = Int.MIN_VALUE
    private var pressedIndex = Int.MIN_VALUE

    init {
        minSize(160f, 80f)
        on(PointerDown) {
            if (!disabled) {
                pressedIndex = hitActionIndex(it.x - x, it.y - y)
                if (pressedIndex != Int.MIN_VALUE) it.consume()
            }
        }
        on(PointerMove) {
            hoverIndex = hitActionIndex(it.x - x, it.y - y)
            requestRender()
        }
        on(PointerUp) {
            if (!disabled) {
                val releasedIndex = hitActionIndex(it.x - x, it.y - y)
                if (releasedIndex == pressedIndex) {
                    when {
                        releasedIndex == -2 -> onFabClick()
                        releasedIndex == -1 -> onNavigationClick()
                        releasedIndex >= 0 -> actions.getOrNull(releasedIndex)?.onClick?.invoke()
                    }
                }
                if (pressedIndex != Int.MIN_VALUE || releasedIndex != Int.MIN_VALUE) it.consume()
            }
            pressedIndex = Int.MIN_VALUE
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        backgroundColor = p.surfaceContainer
        cornerRadius = 0f
        borderColor = null
        borderWidth = 0f
        elevationShadow = Shadow(Color(0, 0, 0, 16), 12f, offsetY = 1f)
        super.draw(context)
        context.backend.translated(x, y) {
            if (fabCradle && fabLabel.isNotBlank()) {
                context.backend.drawRoundedRect((measuredWidth - 64f) / 2f, -8f, 64f, 64f, 32f, p.background, null)
            }
            if (navigationLabel.isNotBlank()) {
                drawTopBarAction(context, navigationLabel, 12f, 20f, hoverIndex == -1, pressedIndex == -1)
            }
            actions.forEachIndexed { index, item ->
                val actionX = measuredWidth - 12f - actions.size * 48f + index * 48f
                drawTopBarAction(context, item.label, actionX, 20f, hoverIndex == index, pressedIndex == index)
            }
            if (fabLabel.isNotBlank()) {
                val fabX = (measuredWidth - 56f) / 2f
                val fabY = 4f
                context.backend.drawRoundedRect(fabX, fabY, 56f, 56f, 28f, overlay(p.secondaryContainer, p.onSecondaryContainer, if (hoverIndex == -2) 0.08f else 0f, 1f), null)
                val style = TextStyle(context.theme.typography.titleSize, p.onSecondaryContainer, context.theme.typography.fontFamily)
                val textWidth = context.backend.measureText(fabLabel, style).first
                context.backend.drawText(fabLabel, fabX + (56f - textWidth) / 2f, fabY + textBaseline(56f, style.size), style)
            }
        }
    }

    private fun hitActionIndex(localX: Float, localY: Float): Int {
        if (fabLabel.isNotBlank()) {
            val fabX = (measuredWidth - 56f) / 2f
            if (localX in fabX..(fabX + 56f) && localY in 4f..60f) return -2
        }
        if (localY !in 20f..60f) return Int.MIN_VALUE
        if (navigationLabel.isNotBlank() && localX in 12f..52f) return -1
        val startX = measuredWidth - 12f - actions.size * 48f
        actions.forEachIndexed { index, _ ->
            val itemStart = startX + index * 48f
            if (localX in itemStart..(itemStart + 40f)) return index
        }
        return Int.MIN_VALUE
    }
}

enum class MaterialTopAppBarStyle { Small, CenterAligned, Medium, Large }

open class MaterialTopAppBar(
    var title: String,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 320f,
    height: Float = 64f,
    var subtitle: String = "",
    var navigationLabel: String = "",
    var actions: List<MaterialActionItem> = emptyList(),
    var barStyle: MaterialTopAppBarStyle = MaterialTopAppBarStyle.Small,
    private val onNavigationClick: () -> Unit = {}
) : Component(x, y, width, height) {
    private var hoverIndex = Int.MIN_VALUE
    private var pressedIndex = Int.MIN_VALUE

    init {
        minSize(120f, 64f)
        on(PointerDown) {
            if (!disabled) {
                pressedIndex = hitActionIndex(it.x - x, it.y - y)
                if (pressedIndex != Int.MIN_VALUE) it.consume()
            }
        }
        on(PointerMove) {
            hoverIndex = hitActionIndex(it.x - x, it.y - y)
            requestRender()
        }
        on(PointerUp) {
            if (!disabled) {
                val releasedIndex = hitActionIndex(it.x - x, it.y - y)
                if (releasedIndex == pressedIndex) {
                    when {
                        releasedIndex == -1 -> onNavigationClick()
                        releasedIndex >= 0 -> actions.getOrNull(releasedIndex)?.onClick?.invoke()
                    }
                }
                if (pressedIndex != Int.MIN_VALUE || releasedIndex != Int.MIN_VALUE) it.consume()
            }
            pressedIndex = Int.MIN_VALUE
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        backgroundColor = p.surface
        cornerRadius = 0f
        borderColor = null
        borderWidth = 0f
        elevationShadow = Shadow(Color(0, 0, 0, 16), 12f, offsetY = 1f)
        super.draw(context)
        context.backend.translated(x, y) {
            val navWidth = if (navigationLabel.isBlank()) 0f else 48f
            val actionAreaWidth = actions.size * 48f
            val titleX = 16f + navWidth
            val titleWidth = (measuredWidth - titleX - actionAreaWidth - 16f).coerceAtLeast(40f)
            if (navigationLabel.isNotBlank()) {
                drawTopBarAction(context, navigationLabel, 4f, 12f, hoverIndex == -1, pressedIndex == -1)
            }
            val titleStyle = TextStyle(
                when (barStyle) {
                    MaterialTopAppBarStyle.Small, MaterialTopAppBarStyle.CenterAligned -> context.theme.typography.titleSize + 2f
                    MaterialTopAppBarStyle.Medium -> context.theme.typography.titleSize + 8f
                    MaterialTopAppBarStyle.Large -> context.theme.typography.titleSize + 14f
                },
                p.onSurface,
                context.theme.typography.fontFamily
            )
            val subtitleStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), p.onSurfaceVariant, context.theme.typography.fontFamily)
            val topInset = when (barStyle) {
                MaterialTopAppBarStyle.Small, MaterialTopAppBarStyle.CenterAligned -> 0f
                MaterialTopAppBarStyle.Medium -> 16f
                MaterialTopAppBarStyle.Large -> 28f
            }
            val titleY = when {
                subtitle.isNotBlank() -> 28f + topInset
                barStyle == MaterialTopAppBarStyle.Small || barStyle == MaterialTopAppBarStyle.CenterAligned -> textBaseline(measuredHeight, titleStyle.size)
                else -> 52f + topInset
            }
            val titleText = title.takeFitting(context, titleStyle, titleWidth)
            if (barStyle == MaterialTopAppBarStyle.CenterAligned) {
                val centeredWidth = context.backend.measureText(titleText, titleStyle).first
                context.backend.drawText(titleText, (measuredWidth - centeredWidth) / 2f, titleY, titleStyle)
            } else {
                context.backend.drawText(titleText, titleX, titleY, titleStyle)
            }
            if (subtitle.isNotBlank()) {
                val subtitleText = subtitle.takeFitting(context, subtitleStyle, titleWidth)
                val subtitleY = titleY + subtitleStyle.size + 8f
                if (barStyle == MaterialTopAppBarStyle.CenterAligned) {
                    val centeredWidth = context.backend.measureText(subtitleText, subtitleStyle).first
                    context.backend.drawText(subtitleText, (measuredWidth - centeredWidth) / 2f, subtitleY, subtitleStyle)
                } else {
                    context.backend.drawText(subtitleText, titleX, subtitleY, subtitleStyle)
                }
            }
            actions.forEachIndexed { index, item ->
                val xOffset = measuredWidth - 16f - actionAreaWidth + index * 48f
                drawTopBarAction(context, item.label, xOffset, 12f, hoverIndex == index, pressedIndex == index)
            }
        }
    }

    private fun hitActionIndex(localX: Float, localY: Float): Int {
        if (localY !in 12f..52f) return Int.MIN_VALUE
        if (navigationLabel.isNotBlank() && localX in 4f..44f) return -1
        val startX = measuredWidth - 16f - actions.size * 48f
        actions.forEachIndexed { index, _ ->
            val itemStart = startX + index * 48f
            if (localX in itemStart..(itemStart + 40f)) return index
        }
        return Int.MIN_VALUE
    }
}

class MaterialCenterAlignedTopAppBar(
    title: String,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 320f,
    height: Float = 64f,
    subtitle: String = "",
    navigationLabel: String = "",
    actions: List<MaterialActionItem> = emptyList(),
    onNavigationClick: () -> Unit = {}
) : MaterialTopAppBar(title, x, y, width, height, subtitle, navigationLabel, actions, MaterialTopAppBarStyle.CenterAligned, onNavigationClick)

class MaterialMediumTopAppBar(
    title: String,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 320f,
    height: Float = 112f,
    subtitle: String = "",
    navigationLabel: String = "",
    actions: List<MaterialActionItem> = emptyList(),
    onNavigationClick: () -> Unit = {}
) : MaterialTopAppBar(title, x, y, width, height, subtitle, navigationLabel, actions, MaterialTopAppBarStyle.Medium, onNavigationClick)

class MaterialLargeTopAppBar(
    title: String,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 320f,
    height: Float = 128f,
    subtitle: String = "",
    navigationLabel: String = "",
    actions: List<MaterialActionItem> = emptyList(),
    onNavigationClick: () -> Unit = {}
) : MaterialTopAppBar(title, x, y, width, height, subtitle, navigationLabel, actions, MaterialTopAppBarStyle.Large, onNavigationClick)

enum class MaterialTabRowStyle { Primary, Secondary }

open class MaterialTabRow(
    val tabs: List<String>,
    val selectedIndex: State<Int> = stateOf(0),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 280f,
    height: Float = 48f,
    var tabStyle: MaterialTabRowStyle = MaterialTabRowStyle.Primary,
    private val onSelect: (Int) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var hoverIndex = -1

    init {
        minSize(96f, 48f)
        selectedIndex.subscribe(invalidate)
        onDispose { selectedIndex.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!disabled) {
                val index = tabIndexAt(it.x - x)
                if (index >= 0) {
                    selectedIndex.value = index
                    onSelect(index)
                    it.consume()
                }
            }
        }
        on(PointerMove) {
            hoverIndex = tabIndexAt(it.x - x)
            requestRender()
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        val count = tabs.size.coerceAtLeast(1)
        val tabWidth = measuredWidth / count
        val indicatorWidth = (tabWidth - 24f).coerceAtLeast(24f)
        val selected = selectedIndex.value.coerceIn(0, tabs.lastIndex.coerceAtLeast(0))
        context.backend.translated(x, y) {
            if (tabStyle == MaterialTabRowStyle.Primary) {
                context.backend.drawLine(0f, measuredHeight - 1f, measuredWidth, measuredHeight - 1f, Stroke(p.outlineVariant, 1f))
            }
            tabs.forEachIndexed { index, text ->
                val isSelected = index == selected
                val isHovered = index == hoverIndex
                if (tabStyle == MaterialTabRowStyle.Secondary && (isSelected || isHovered)) {
                    context.backend.drawRoundedRect(index * tabWidth + 6f, 6f, tabWidth - 12f, measuredHeight - 12f, (measuredHeight - 12f) / 2f, if (isSelected) p.secondaryContainer else overlay(p.surfaceContainerHigh, p.onSurface, 0.06f, 1f), null)
                }
                val style = TextStyle(
                    context.theme.typography.bodySize,
                    when {
                        isSelected && this@MaterialTabRow.tabStyle == MaterialTabRowStyle.Secondary -> p.onSecondaryContainer
                        isSelected -> p.primary
                        isHovered -> p.onSurface
                        else -> p.onSurfaceVariant
                    },
                    context.theme.typography.fontFamily
                )
                val textWidth = context.backend.measureText(text, style).first
                val textX = index * tabWidth + (tabWidth - textWidth) / 2f
                context.backend.drawText(text, textX, textBaseline(measuredHeight - if (this@MaterialTabRow.tabStyle == MaterialTabRowStyle.Primary) 6f else 2f, style.size), style)
            }
            if (tabStyle == MaterialTabRowStyle.Primary) {
                context.backend.drawRoundedRect(selected * tabWidth + (tabWidth - indicatorWidth) / 2f, measuredHeight - 3f, indicatorWidth, 3f, 1.5f, p.primary, null)
            }
        }
    }

    private fun tabIndexAt(localX: Float): Int {
        if (tabs.isEmpty() || localX < 0f || localX > measuredWidth) return -1
        val tabWidth = measuredWidth / tabs.size
        return (localX / tabWidth).toInt().coerceIn(0, tabs.lastIndex)
    }
}

class MaterialSecondaryTabRow(
    tabs: List<String>,
    selectedIndex: State<Int> = stateOf(0),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 280f,
    height: Float = 48f,
    onSelect: (Int) -> Unit = {}
) : MaterialTabRow(
    tabs = tabs,
    selectedIndex = selectedIndex,
    x = x,
    y = y,
    width = width,
    height = height,
    tabStyle = MaterialTabRowStyle.Secondary,
    onSelect = onSelect
)

class MaterialNavigationBar(
    val destinations: List<String>,
    val selectedIndex: State<Int> = stateOf(0),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 320f,
    height: Float = 80f,
    private val onSelect: (Int) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var hoverIndex = -1

    init {
        minSize(160f, 80f)
        selectedIndex.subscribe(invalidate)
        onDispose { selectedIndex.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!disabled) {
                val index = itemIndexAt(sceneToLocal(it.x, it.y).first)
                if (index >= 0) {
                    selectedIndex.value = index
                    onSelect(index)
                    it.consume()
                }
            }
        }
        on(PointerMove) {
            hoverIndex = itemIndexAt(sceneToLocal(it.x, it.y).first)
            requestRender()
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        backgroundColor = p.surfaceContainer
        cornerRadius = 0f
        borderColor = null
        borderWidth = 0f
        elevationShadow = Shadow(Color(0, 0, 0, 16), 12f, offsetY = 1f)
        super.draw(context)
        if (destinations.isEmpty()) return
        val itemWidth = measuredWidth / destinations.size
        context.backend.translated(x, y) {
            destinations.forEachIndexed { index, label ->
                val selected = selectedIndex.value == index
                val hovered = hoverIndex == index
                val pillWidth = (itemWidth - 24f).coerceAtLeast(28f)
                val pillX = index * itemWidth + (itemWidth - pillWidth) / 2f
                if (selected || hovered) {
                    context.backend.drawRoundedRect(pillX, 12f, pillWidth, 32f, 16f, overlay(p.secondaryContainer, p.onSecondaryContainer, if (selected) 0.14f else 0.06f, 1f), null)
                }
                val style = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), if (selected) p.onSurface else p.onSurfaceVariant, context.theme.typography.fontFamily)
                val textWidth = context.backend.measureText(label, style).first
                context.backend.drawText(label, index * itemWidth + (itemWidth - textWidth) / 2f, 61f, style)
            }
        }
    }

    private fun itemIndexAt(localX: Float): Int {
        if (destinations.isEmpty() || localX < 0f || localX > measuredWidth) return -1
        val itemWidth = measuredWidth / destinations.size
        return (localX / itemWidth).toInt().coerceIn(0, destinations.lastIndex)
    }
}

class MaterialNavigationRail(
    val destinations: List<String>,
    val selectedIndex: State<Int> = stateOf(0),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 96f,
    height: Float = 320f,
    private val onSelect: (Int) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var hoverIndex = -1

    init {
        minSize(80f, 160f)
        selectedIndex.subscribe(invalidate)
        onDispose { selectedIndex.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!disabled) {
                val index = itemIndexAt(sceneToLocal(it.x, it.y).second)
                if (index >= 0) {
                    selectedIndex.value = index
                    onSelect(index)
                    it.consume()
                }
            }
        }
        on(PointerMove) {
            hoverIndex = itemIndexAt(sceneToLocal(it.x, it.y).second)
            requestRender()
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        backgroundColor = p.surface
        cornerRadius = MaterialShapes.Large
        borderColor = null
        borderWidth = 0f
        elevationShadow = null
        super.draw(context)
        if (destinations.isEmpty()) return
        val slotHeight = 56f
        context.backend.translated(x, y) {
            destinations.forEachIndexed { index, label ->
                val top = 16f + index * slotHeight
                val selected = selectedIndex.value == index
                val hovered = hoverIndex == index
                if (selected || hovered) {
                    context.backend.drawRoundedRect(12f, top, measuredWidth - 24f, 40f, 20f, overlay(p.secondaryContainer, p.onSecondaryContainer, if (selected) 0.14f else 0.06f, 1f), null)
                }
                val style = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), if (selected) p.onSurface else p.onSurfaceVariant, context.theme.typography.fontFamily)
                val textWidth = context.backend.measureText(label, style).first
                context.backend.drawText(label, (measuredWidth - textWidth) / 2f, top + 24f, style)
            }
        }
    }

    private fun itemIndexAt(localY: Float): Int {
        if (destinations.isEmpty()) return -1
        destinations.indices.forEach { index ->
            val top = 16f + index * 56f
            if (localY in top..(top + 40f)) return index
        }
        return -1
    }
}

enum class MaterialSnackbarStyle { Snackbar, Banner }

data class MaterialBannerAction(
    val label: String,
    val onClick: () -> Unit = {}
)

open class MaterialSnackbar(
    val message: State<String?> = stateOf(null),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 320f,
    height: Float = 48f,
    var actionLabel: String = "",
    var withDismissAction: Boolean = false,
    var supportingText: String = "",
    var snackbarStyle: MaterialSnackbarStyle = MaterialSnackbarStyle.Snackbar,
    private val onAction: () -> Unit = {},
    private val onDismiss: () -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = {
        visible = message.value != null
        requestRender()
    }
    private var pressedAction = Int.MIN_VALUE
    private var hoverAction = Int.MIN_VALUE

    init {
        minSize(160f, if (snackbarStyle == MaterialSnackbarStyle.Banner) 96f else 48f)
        visible = message.value != null
        message.subscribe(invalidate)
        onDispose { message.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!visible) return@on
            pressedAction = actionAt(it.x - x, it.y - y)
            it.consume()
        }
        on(PointerMove) {
            if (!visible) return@on
            hoverAction = actionAt(it.x - x, it.y - y)
            requestRender()
        }
        on(PointerUp) {
            if (!visible) return@on
            val released = actionAt(it.x - x, it.y - y)
            if (released == pressedAction) {
                when (released) {
                    1 -> onAction()
                    2 -> dismiss()
                }
            }
            pressedAction = Int.MIN_VALUE
            it.consume()
        }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        val p = context.theme.palette
        val isBanner = snackbarStyle == MaterialSnackbarStyle.Banner
        backgroundColor = if (isBanner) p.surfaceContainerHigh else p.inverseSurface
        cornerRadius = if (isBanner) MaterialShapes.Medium else MaterialShapes.Small
        borderColor = if (isBanner) p.outlineVariant else null
        borderWidth = if (isBanner) 1f else 0f
        elevationShadow = if (isBanner) null else Shadow(Color(0, 0, 0, 30), 14f, offsetY = 3f)
        super.draw(context)
        context.backend.translated(x, y) {
            val text = message.value.orEmpty()
            val bodyColor = if (isBanner) p.onSurface else p.inverseOnSurface
            val bodyStyle = TextStyle(context.theme.typography.bodySize, bodyColor, context.theme.typography.fontFamily)
            val supportStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), if (isBanner) p.onSurfaceVariant else p.inverseOnSurface.withAlpha(204), context.theme.typography.fontFamily)
            val actionStyle = TextStyle(context.theme.typography.bodySize, if (isBanner) p.primary else p.primary, context.theme.typography.fontFamily)
            if (!isBanner) {
                val dismissWidth = if (withDismissAction) 28f else 0f
                val actionWidth = if (actionLabel.isBlank()) 0f else context.backend.measureText(actionLabel, actionStyle).first + 24f
                val availableWidth = (measuredWidth - 32f - actionWidth - dismissWidth).coerceAtLeast(48f)
                context.backend.drawText(text.takeFitting(context, bodyStyle, availableWidth), 16f, textBaseline(measuredHeight, bodyStyle.size), bodyStyle)
                if (actionLabel.isNotBlank()) {
                    val textWidth = context.backend.measureText(actionLabel, actionStyle).first
                    val actionX = measuredWidth - 16f - dismissWidth - textWidth
                    if (hoverAction == 1 || pressedAction == 1) {
                        context.backend.drawRoundedRect(actionX - 8f, 8f, textWidth + 16f, measuredHeight - 16f, (measuredHeight - 16f) / 2f, overlay(backgroundColor ?: p.inverseSurface, p.primary, if (pressedAction == 1) 0.18f else 0.10f, 1f), null)
                    }
                    context.backend.drawText(actionLabel, actionX, textBaseline(measuredHeight, actionStyle.size), actionStyle)
                }
                if (withDismissAction) {
                    if (hoverAction == 2 || pressedAction == 2) {
                        context.backend.drawRoundedRect(measuredWidth - 36f, 8f, 24f, measuredHeight - 16f, (measuredHeight - 16f) / 2f, overlay(backgroundColor ?: p.inverseSurface, p.inverseOnSurface, if (pressedAction == 2) 0.16f else 0.08f, 1f), null)
                    }
                    context.backend.drawText("×", measuredWidth - 28f, textBaseline(measuredHeight, actionStyle.size), TextStyle(actionStyle.size, p.inverseOnSurface, actionStyle.fontFamily))
                }
            } else {
                val contentWidth = (measuredWidth - 32f).coerceAtLeast(48f)
                context.backend.drawText(text.takeFitting(context, bodyStyle, contentWidth), 16f, 24f, bodyStyle)
                val support = if (supportingText.isBlank()) emptyList() else supportingText.wrapText(context, supportStyle, contentWidth, 2)
                support.forEachIndexed { index, line ->
                    context.backend.drawText(line, 16f, 44f + index * (supportStyle.size + 4f), supportStyle)
                }
                val actionRowTop = (measuredHeight - 16f).coerceAtLeast(64f)
                if (actionLabel.isNotBlank()) {
                    val textWidth = context.backend.measureText(actionLabel, actionStyle).first
                    val actionX = 16f
                    if (hoverAction == 1 || pressedAction == 1) {
                        context.backend.drawRoundedRect(actionX - 8f, actionRowTop - 18f, textWidth + 16f, 28f, 14f, overlay(backgroundColor ?: p.surfaceContainerHigh, p.primary, if (pressedAction == 1) 0.14f else 0.08f, 1f), null)
                    }
                    context.backend.drawText(actionLabel, actionX, actionRowTop, actionStyle)
                }
                if (withDismissAction) {
                    val dismissStyle = TextStyle(actionStyle.size, p.onSurfaceVariant, actionStyle.fontFamily)
                    val dismissX = measuredWidth - 28f
                    if (hoverAction == 2 || pressedAction == 2) {
                        context.backend.drawRoundedRect(measuredWidth - 40f, actionRowTop - 18f, 24f, 28f, 14f, overlay(backgroundColor ?: p.surfaceContainerHigh, p.onSurface, if (pressedAction == 2) 0.14f else 0.06f, 1f), null)
                    }
                    context.backend.drawText("×", dismissX, actionRowTop, dismissStyle)
                }
            }
        }
    }

    fun dismiss() {
        message.value = null
        onDismiss()
    }

    private fun actionAt(localX: Float, localY: Float): Int {
        if (!visible) return Int.MIN_VALUE
        return if (snackbarStyle == MaterialSnackbarStyle.Banner) {
            val actionTop = (measuredHeight - 48f).coerceAtLeast(36f)
            if (actionLabel.isNotBlank() && localX in 8f..140f && localY in actionTop..(actionTop + 36f)) 1
            else if (withDismissAction && localX in (measuredWidth - 44f)..(measuredWidth - 8f) && localY in actionTop..(actionTop + 36f)) 2
            else Int.MIN_VALUE
        } else {
            val actionStart = measuredWidth - 112f - if (withDismissAction) 28f else 0f
            val actionEnd = measuredWidth - 8f - if (withDismissAction) 28f else 8f
            if (actionLabel.isNotBlank() && localX in actionStart..actionEnd && localY in 0f..measuredHeight) 1
            else if (withDismissAction && localX in (measuredWidth - 40f)..measuredWidth && localY in 0f..measuredHeight) 2
            else Int.MIN_VALUE
        }
    }
}

class MaterialBanner(
    message: State<String?> = stateOf(null),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 320f,
    height: Float = 96f,
    actionLabel: String = "",
    supportingText: String = "",
    withDismissAction: Boolean = true,
    onAction: () -> Unit = {},
    onDismiss: () -> Unit = {}
) : MaterialSnackbar(
    message = message,
    x = x,
    y = y,
    width = width,
    height = height,
    actionLabel = actionLabel,
    withDismissAction = withDismissAction,
    supportingText = supportingText,
    snackbarStyle = MaterialSnackbarStyle.Banner,
    onAction = onAction,
    onDismiss = onDismiss
)

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

data class MaterialMenuItem(
    val label: String,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
    val leadingLabel: String = "",
    val trailingLabel: String = "",
    val supportingText: String = "",
    val keepOpen: Boolean = false,
    val isSection: Boolean = false,
    val isDivider: Boolean = false,
    val onClick: () -> Unit = {}
)

fun MaterialMenuSection(label: String): MaterialMenuItem = MaterialMenuItem(label = label, enabled = false, isSection = true)

fun MaterialMenuDivider(): MaterialMenuItem = MaterialMenuItem(label = "", enabled = false, isDivider = true)

open class MaterialDialog(
    val visibleState: State<Boolean> = stateOf(false),
    var title: String,
    var message: String = "",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 240f,
    var dialogWidth: Float = 280f,
    var dialogHeight: Float = 180f,
    var confirmLabel: String = "OK",
    var dismissLabel: String = "",
    var dismissOnOutsideClick: Boolean = true,
    var dismissOnEscape: Boolean = true,
    private val onConfirm: () -> Unit = {},
    private val onDismiss: () -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = {
        visible = visibleState.value
        requestRender()
    }
    private var hoverAction = Int.MIN_VALUE
    private var pressedAction = Int.MIN_VALUE

    init {
        focusable = true
        visible = visibleState.value
        minSize(160f, 120f)
        visibleState.subscribe(invalidate)
        onDispose { visibleState.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!visible) return@on
            requestFocus()
            pressedAction = actionAt(it.x - x, it.y - y)
            it.consume()
        }
        on(PointerMove) {
            if (!visible) return@on
            hoverAction = actionAt(it.x - x, it.y - y)
            requestRender()
        }
        on(PointerUp) {
            if (!visible) return@on
            val releasedAction = actionAt(it.x - x, it.y - y)
            when {
                releasedAction == 1 && pressedAction == 1 -> confirm()
                releasedAction == 0 && pressedAction == 0 -> dismiss()
                releasedAction == -2 && pressedAction == -2 && dismissOnOutsideClick -> dismiss()
            }
            pressedAction = Int.MIN_VALUE
            it.consume()
        }
        onKey("keyDown") {
            if (!visible) return@onKey
            when (it.keyCode) {
                AwtKeyEvent.VK_ESCAPE -> if (dismissOnEscape) {
                    dismiss()
                    it.consume()
                }
                AwtKeyEvent.VK_ENTER -> {
                    confirm()
                    it.consume()
                }
            }
        }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        val p = context.theme.palette
        val bounds = dialogBounds()
        val titleStyle = TextStyle(context.theme.typography.titleSize + 2f, p.onSurface, context.theme.typography.fontFamily)
        val bodyStyle = TextStyle(context.theme.typography.bodySize, p.onSurfaceVariant, context.theme.typography.fontFamily)
        val actionStyle = TextStyle(context.theme.typography.bodySize, p.primary, context.theme.typography.fontFamily)
        val dismissStyle = TextStyle(context.theme.typography.bodySize, p.onSurfaceVariant, context.theme.typography.fontFamily)
        val contentWidth = (bounds.width - 48f).coerceAtLeast(48f)
        val messageTop = 60f
        val actionsTop = bounds.height - 52f
        val availableBodyHeight = (actionsTop - messageTop - 12f).coerceAtLeast(bodyStyle.size)
        val maxBodyLines = (availableBodyHeight / (bodyStyle.size + 6f)).toInt().coerceAtLeast(1)
        val messageLines = message.wrapText(context, bodyStyle, contentWidth, maxBodyLines)
        context.backend.translated(x, y) {
            context.backend.drawRect(0f, 0f, measuredWidth, measuredHeight, p.scrim.withAlpha(148), null)
            context.backend.drawRoundedRect(bounds.x, bounds.y, bounds.width, bounds.height, MaterialShapes.ExtraLarge, p.surfaceContainerHigh, Stroke(p.outlineVariant, 1f))
            context.backend.drawText(title.takeFitting(context, titleStyle, contentWidth), bounds.x + 24f, bounds.y + 34f, titleStyle)
            messageLines.forEachIndexed { index, line ->
                context.backend.drawText(line, bounds.x + 24f, bounds.y + messageTop + index * (bodyStyle.size + 6f), bodyStyle)
            }
            if (dismissLabel.isNotBlank()) {
                drawDialogAction(context, dismissLabel, dismissStyle, bounds.x + bounds.width - 180f, bounds.y + bounds.height - 40f, 72f, hoverAction == 0, pressedAction == 0)
            }
            drawDialogAction(context, confirmLabel, actionStyle, bounds.x + bounds.width - 92f, bounds.y + bounds.height - 40f, 72f, hoverAction == 1, pressedAction == 1)
        }
    }

    fun confirm() {
        if (!visible) return
        onConfirm()
        visibleState.value = false
    }

    fun dismiss() {
        if (!visible) return
        visibleState.value = false
        onDismiss()
    }

    private fun dialogBounds(): DialogBounds {
        val cardWidth = dialogWidth.coerceAtLeast(180f).coerceAtMost((measuredWidth - 24f).coerceAtLeast(180f))
        val cardHeight = dialogHeight.coerceAtLeast(120f).coerceAtMost((measuredHeight - 24f).coerceAtLeast(120f))
        return DialogBounds(
            (measuredWidth - cardWidth) / 2f,
            (measuredHeight - cardHeight) / 2f,
            cardWidth,
            cardHeight
        )
    }

    private fun actionAt(localX: Float, localY: Float): Int {
        val bounds = dialogBounds()
        if (localX !in bounds.x..(bounds.x + bounds.width) || localY !in bounds.y..(bounds.y + bounds.height)) return -2
        val actionTop = bounds.y + bounds.height - 44f
        if (localY !in actionTop..(actionTop + 32f)) return Int.MIN_VALUE
        val confirmStart = bounds.x + bounds.width - 96f
        if (localX in confirmStart..(confirmStart + 80f)) return 1
        if (dismissLabel.isNotBlank()) {
            val dismissStart = confirmStart - 88f
            if (localX in dismissStart..(dismissStart + 80f)) return 0
        }
        return Int.MIN_VALUE
    }
}

class MaterialAlertDialog(
    visibleState: State<Boolean> = stateOf(false),
    title: String,
    message: String = "",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 240f,
    dialogWidth: Float = 280f,
    dialogHeight: Float = 180f,
    confirmLabel: String = "OK",
    dismissLabel: String = "Cancel",
    dismissOnOutsideClick: Boolean = true,
    dismissOnEscape: Boolean = true,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
) : MaterialDialog(
    visibleState = visibleState,
    title = title,
    message = message,
    x = x,
    y = y,
    width = width,
    height = height,
    dialogWidth = dialogWidth,
    dialogHeight = dialogHeight,
    confirmLabel = confirmLabel,
    dismissLabel = dismissLabel,
    dismissOnOutsideClick = dismissOnOutsideClick,
    dismissOnEscape = dismissOnEscape,
    onConfirm = onConfirm,
    onDismiss = onDismiss
)

class MaterialBasicDialog(
    visibleState: State<Boolean> = stateOf(false),
    title: String,
    message: String = "",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 240f,
    dialogWidth: Float = 280f,
    dialogHeight: Float = 180f,
    dismissOnOutsideClick: Boolean = true,
    dismissOnEscape: Boolean = true,
    onDismiss: () -> Unit = {}
) : MaterialDialog(
    visibleState = visibleState,
    title = title,
    message = message,
    x = x,
    y = y,
    width = width,
    height = height,
    dialogWidth = dialogWidth,
    dialogHeight = dialogHeight,
    confirmLabel = "",
    dismissLabel = "",
    dismissOnOutsideClick = dismissOnOutsideClick,
    dismissOnEscape = dismissOnEscape,
    onConfirm = {},
    onDismiss = onDismiss
)

class MaterialConfirmationDialog(
    visibleState: State<Boolean> = stateOf(false),
    title: String,
    message: String = "",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 240f,
    dialogWidth: Float = 300f,
    dialogHeight: Float = 188f,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel",
    dismissOnOutsideClick: Boolean = true,
    dismissOnEscape: Boolean = true,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
) : MaterialDialog(
    visibleState = visibleState,
    title = title,
    message = message,
    x = x,
    y = y,
    width = width,
    height = height,
    dialogWidth = dialogWidth,
    dialogHeight = dialogHeight,
    confirmLabel = confirmLabel,
    dismissLabel = dismissLabel,
    dismissOnOutsideClick = dismissOnOutsideClick,
    dismissOnEscape = dismissOnEscape,
    onConfirm = onConfirm,
    onDismiss = onDismiss
)

class MaterialDropdownMenu(
    val expanded: State<Boolean> = stateOf(false),
    val items: List<MaterialMenuItem>,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 240f,
    var menuX: Float = 0f,
    var menuY: Float = 0f,
    var menuWidth: Float = 200f,
    var dismissOnOutsideClick: Boolean = true,
    var showDividers: Boolean = false
) : Component(x, y, width, height) {
    private val invalidate = {
        visible = expanded.value
        requestRender()
    }
    private var hoverIndex = Int.MIN_VALUE
    private var pressedIndex = Int.MIN_VALUE

    init {
        focusable = true
        visible = expanded.value
        minSize(120f, 120f)
        expanded.subscribe(invalidate)
        onDispose { expanded.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!visible) return@on
            requestFocus()
            val local = sceneToLocal(it.x, it.y)
            pressedIndex = itemIndexAt(local.first, local.second)
            it.consume()
        }
        on(PointerMove) {
            if (!visible) return@on
            val local = sceneToLocal(it.x, it.y)
            hoverIndex = itemIndexAt(local.first, local.second)
            requestRender()
        }
        on(PointerUp) {
            if (!visible) return@on
            val local = sceneToLocal(it.x, it.y)
            val releasedIndex = itemIndexAt(local.first, local.second)
            when {
                releasedIndex >= 0 && releasedIndex == pressedIndex -> {
                    items.getOrNull(releasedIndex)?.takeIf { menuItem -> menuItem.enabled && !menuItem.isSection && !menuItem.isDivider }?.onClick?.invoke()
                    if (items.getOrNull(releasedIndex)?.keepOpen != true) expanded.value = false
                }
                releasedIndex == -2 && pressedIndex == -2 && dismissOnOutsideClick -> expanded.value = false
            }
            pressedIndex = Int.MIN_VALUE
            it.consume()
        }
        onKey("keyDown") {
            if (!visible) return@onKey
            if (it.keyCode == AwtKeyEvent.VK_ESCAPE) {
                expanded.value = false
                it.consume()
            }
        }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        val p = context.theme.palette
        val bounds = menuBounds()
        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(bounds.x, bounds.y, bounds.width, bounds.height, MaterialShapes.Medium, p.surfaceContainerHigh, Stroke(p.outlineVariant, 1f))
            var cursorY = bounds.y + 8f
            items.forEachIndexed { index, item ->
                when {
                    item.isDivider -> {
                        context.backend.drawLine(bounds.x + 12f, cursorY + 7f, bounds.x + bounds.width - 12f, cursorY + 7f, Stroke(p.outlineVariant, 1f))
                        cursorY += 16f
                    }
                    item.isSection -> {
                        val sectionStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), p.primary, context.theme.typography.fontFamily)
                        context.backend.drawText(item.label.takeFitting(context, sectionStyle, bounds.width - 24f), bounds.x + 12f, cursorY + textBaseline(24f, sectionStyle.size), sectionStyle)
                        cursorY += 28f
                    }
                    else -> {
                        val rowHeight = if (item.supportingText.isBlank()) 40f else 56f
                        val top = cursorY
                        val active = hoverIndex == index || pressedIndex == index
                        if (active) {
                            context.backend.drawRoundedRect(bounds.x + 8f, top, bounds.width - 16f, rowHeight - 8f, MaterialShapes.Small, overlay(p.surfaceContainerHigh, p.onSurface, if (pressedIndex == index) 0.10f else 0.06f, 1f), null)
                        }
                        val color = when {
                            !item.enabled -> p.onSurface.withAlpha(97)
                            item.destructive -> p.error
                            else -> p.onSurface
                        }
                        val leadingOffset = if (item.leadingLabel.isBlank()) 0f else 28f
                        if (item.leadingLabel.isNotBlank()) {
                            val iconStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), color, context.theme.typography.fontFamily)
                            context.backend.drawText(item.leadingLabel, bounds.x + 16f, top + textBaseline(rowHeight - 8f, iconStyle.size), iconStyle)
                        }
                        if (item.trailingLabel.isNotBlank()) {
                            val trailingStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), if (item.enabled) p.onSurfaceVariant else p.onSurface.withAlpha(97), context.theme.typography.fontFamily)
                            val trailingWidth = context.backend.measureText(item.trailingLabel, trailingStyle).first
                            context.backend.drawText(item.trailingLabel, bounds.x + bounds.width - 16f - trailingWidth, top + textBaseline(rowHeight - 8f, trailingStyle.size), trailingStyle)
                        }
                        val trailingReserve = if (item.trailingLabel.isBlank()) 0f else 52f
                        val labelStyle = TextStyle(context.theme.typography.bodySize, color, context.theme.typography.fontFamily)
                        val contentWidth = (bounds.width - 32f - leadingOffset - trailingReserve).coerceAtLeast(48f)
                        val labelX = bounds.x + 16f + leadingOffset
                        val labelY = if (item.supportingText.isBlank()) top + textBaseline(rowHeight - 8f, labelStyle.size) else top + 20f
                        context.backend.drawText(item.label.takeFitting(context, labelStyle, contentWidth), labelX, labelY, labelStyle)
                        if (item.supportingText.isNotBlank()) {
                            val supportingStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), if (item.enabled) p.onSurfaceVariant else p.onSurface.withAlpha(97), context.theme.typography.fontFamily)
                            context.backend.drawText(item.supportingText.takeFitting(context, supportingStyle, contentWidth), labelX, top + 38f, supportingStyle)
                        }
                        cursorY += rowHeight
                        if (showDividers && index < items.lastIndex && !items[index + 1].isDivider && !items[index + 1].isSection) {
                            context.backend.drawLine(bounds.x + 12f, cursorY - 4f, bounds.x + bounds.width - 12f, cursorY - 4f, Stroke(p.outlineVariant, 1f))
                        }
                    }
                }
            }
        }
    }

    private fun menuBounds(): DialogBounds {
        val popupWidth = menuWidth.coerceAtLeast(120f).coerceAtMost((measuredWidth - menuX - 8f).coerceAtLeast(120f))
        val popupHeight = contentHeight().coerceAtLeast(16f).coerceAtMost((measuredHeight - menuY - 8f).coerceAtLeast(16f))
        return DialogBounds(menuX.coerceAtLeast(0f), menuY.coerceAtLeast(0f), popupWidth, popupHeight)
    }

    private fun contentHeight(): Float = items.fold(16f) { total, item ->
        total + when {
            item.isDivider -> 16f
            item.isSection -> 28f
            item.supportingText.isBlank() -> 40f
            else -> 56f
        }
    }

    private fun itemIndexAt(localX: Float, localY: Float): Int {
        val bounds = menuBounds()
        if (localX !in bounds.x..(bounds.x + bounds.width) || localY !in bounds.y..(bounds.y + bounds.height)) return -2
        var cursorY = bounds.y + 8f
        items.forEachIndexed { index, item ->
            val itemHeight = when {
                item.isDivider -> 16f
                item.isSection -> 28f
                item.supportingText.isBlank() -> 40f
                else -> 56f
            }
            if (localY in cursorY..(cursorY + itemHeight)) {
                return if (item.isDivider) Int.MIN_VALUE else index
            }
            cursorY += itemHeight
        }
        return Int.MIN_VALUE
    }
}

class MaterialExposedDropdownMenu(
    val selectedIndex: State<Int> = stateOf(-1),
    val expanded: State<Boolean> = stateOf(false),
    val options: List<String>,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 280f,
    height: Float = 56f,
    var label: String = "",
    var placeholder: String = "",
    var supportingText: String = "",
    var variant: MaterialTextFieldStyle = MaterialTextFieldStyle.Filled,
    var dismissOnOutsideClick: Boolean = true,
    private val onSelect: (Int, String) -> Unit = { _, _ -> }
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var hoverIndex = Int.MIN_VALUE
    private var pressedIndex = Int.MIN_VALUE
    private var fieldHover = 0f
    private var fieldFocused = 0f

    init {
        focusable = true
        minSize(160f, 56f)
        selectedIndex.subscribe(invalidate)
        expanded.subscribe(invalidate)
        onDispose {
            selectedIndex.unsubscribe(invalidate)
            expanded.unsubscribe(invalidate)
        }
        on(PointerDown) {
            requestFocus()
            val local = sceneToLocal(it.x, it.y)
            val target = itemIndexAt(local.first, local.second)
            if (target >= 0) {
                pressedIndex = target
            } else if (target == -1) {
                pressedIndex = -1
            } else if (target == -2 && dismissOnOutsideClick) {
                expanded.value = false
            }
            setInteraction { copy(pressed = true) }
            it.consume()
        }
        on(PointerMove) {
            val local = sceneToLocal(it.x, it.y)
            hoverIndex = itemIndexAt(local.first, local.second)
            requestRender()
        }
        on(PointerUp) {
            val local = sceneToLocal(it.x, it.y)
            val released = itemIndexAt(local.first, local.second)
            when {
                pressedIndex == -1 && released == -1 -> expanded.value = !expanded.value
                pressedIndex >= 0 && released == pressedIndex -> {
                    selectedIndex.value = released
                    expanded.value = false
                    options.getOrNull(released)?.let { option -> onSelect(released, option) }
                }
                released == -2 && dismissOnOutsideClick -> expanded.value = false
            }
            pressedIndex = Int.MIN_VALUE
            setInteraction { copy(pressed = false) }
            it.consume()
        }
        on(KeyDown) {
            if (handleKey(it)) {
                requestRender()
                it.consume()
            }
        }
        onKey("keyDown") {
            if (it.keyCode == AwtKeyEvent.VK_ESCAPE && expanded.value) {
                expanded.value = false
                it.consume()
            }
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        fieldHover = animate(fieldHover, if (interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        fieldFocused = animate(fieldFocused, if (interaction.focused && !disabled) 1f else 0f, context.time.deltaSeconds, 16f)
        val stateAlpha = 0.03f * fieldHover + 0.04f * fieldFocused
        val base = if (variant == MaterialTextFieldStyle.Filled) p.surfaceContainerHighest else p.surface
        val fieldBackground = overlay(base, p.onSurface, stateAlpha, if (disabled) 0.38f else 1f)
        val fieldBorder = when (variant) {
            MaterialTextFieldStyle.Filled -> if (interaction.focused || expanded.value) p.primary else p.outlineVariant.withAlpha(140)
            MaterialTextFieldStyle.Outlined -> if (interaction.focused || expanded.value) p.primary else p.outline
        }
        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(0f, 0f, measuredWidth, 56f, MaterialShapes.ExtraSmall, fieldBackground, Stroke(fieldBorder, if (interaction.focused || expanded.value) 2f else 1f))
            val bodySize = context.theme.typography.bodySize
            val labelSize = (bodySize - 2f).coerceAtLeast(10f)
            if (label.isNotBlank()) {
                val labelColor = if (interaction.focused || expanded.value) p.primary else p.onSurfaceVariant
                context.backend.drawText(label, 16f, 16f, TextStyle(labelSize, labelColor.withAlpha(if (disabled) 110 else 255), context.theme.typography.fontFamily))
            }
            val selectedText = options.getOrNull(selectedIndex.value).orEmpty()
            val text = if (selectedText.isNotBlank()) selectedText else placeholder
            val color = when {
                disabled -> p.onSurface.withAlpha(97)
                selectedText.isNotBlank() -> p.onSurface
                else -> p.onSurfaceVariant
            }
            val textStyle = TextStyle(bodySize, color, context.theme.typography.fontFamily)
            val baseline = if (label.isNotBlank()) 40f else textBaseline(56f, bodySize)
            context.backend.drawText(text.takeFitting(context, textStyle, measuredWidth - 44f), 16f, baseline, textStyle)
            val arrow = if (expanded.value) "▴" else "▾"
            val arrowWidth = context.backend.measureText(arrow, textStyle).first
            context.backend.drawText(arrow, measuredWidth - 16f - arrowWidth, baseline, textStyle)
            if (supportingText.isNotBlank()) {
                context.backend.drawText(
                    supportingText,
                    16f,
                    70f,
                    TextStyle(labelSize, p.onSurfaceVariant.withAlpha(if (disabled) 110 else 255), context.theme.typography.fontFamily)
                )
            }
            if (expanded.value) {
                val bounds = menuBounds()
                context.backend.drawRoundedRect(bounds.x, bounds.y, bounds.width, bounds.height, MaterialShapes.Medium, p.surfaceContainerHigh, Stroke(p.outlineVariant, 1f))
                options.forEachIndexed { index, option ->
                    val top = bounds.y + 8f + index * 40f
                    val active = hoverIndex == index || pressedIndex == index || selectedIndex.value == index
                    if (active) {
                        context.backend.drawRoundedRect(
                            bounds.x + 8f,
                            top,
                            bounds.width - 16f,
                            32f,
                            MaterialShapes.Small,
                            if (selectedIndex.value == index) p.secondaryContainer else overlay(p.surfaceContainerHigh, p.onSurface, if (pressedIndex == index) 0.10f else 0.06f, 1f),
                            null
                        )
                    }
                    val optionColor = if (selectedIndex.value == index) p.onSecondaryContainer else p.onSurface
                    val optionStyle = TextStyle(context.theme.typography.bodySize, optionColor, context.theme.typography.fontFamily)
                    context.backend.drawText(option.takeFitting(context, optionStyle, bounds.width - 32f), bounds.x + 16f, top + textBaseline(32f, optionStyle.size), optionStyle)
                }
            }
        }
        if (isAnimating(fieldHover, fieldFocused)) requestRender()
    }

    private fun handleKey(event: KeyEvent): Boolean {
        if (disabled) return false
        return when (event.keyCode) {
            AwtKeyEvent.VK_ENTER, AwtKeyEvent.VK_SPACE -> {
                expanded.value = !expanded.value
                true
            }
            AwtKeyEvent.VK_DOWN -> {
                if (!expanded.value) {
                    expanded.value = true
                } else if (options.isNotEmpty()) {
                    selectedIndex.value = (selectedIndex.value + 1).coerceAtMost(options.lastIndex)
                    options.getOrNull(selectedIndex.value)?.let { option -> onSelect(selectedIndex.value, option) }
                }
                true
            }
            AwtKeyEvent.VK_UP -> {
                if (expanded.value && options.isNotEmpty()) {
                    selectedIndex.value = if (selectedIndex.value <= 0) 0 else selectedIndex.value - 1
                    options.getOrNull(selectedIndex.value)?.let { option -> onSelect(selectedIndex.value, option) }
                }
                true
            }
            AwtKeyEvent.VK_ESCAPE -> {
                expanded.value = false
                true
            }
            else -> false
        }
    }

    private fun menuBounds(): DialogBounds {
        val popupY = 64f
        val popupWidth = measuredWidth.coerceAtLeast(120f)
        val popupHeight = options.size * 40f + 16f
        return DialogBounds(0f, popupY, popupWidth, popupHeight)
    }

    internal override fun pointerPath(globalX: Float, globalY: Float): List<Component> {
        if (!visible || disabled) return emptyList()
        // pointerPath receives coordinates in the parent component's space.
        val localX = globalX - x
        val localY = globalY - y
        val inField = localX in 0f..measuredWidth && localY in 0f..56f
        val bounds = menuBounds()
        val inMenu = expanded.value && localX in bounds.x..(bounds.x + bounds.width) && localY in bounds.y..(bounds.y + bounds.height)
        return if (inField || inMenu) listOf(this) else emptyList()
    }

    private fun itemIndexAt(localX: Float, localY: Float): Int {
        if (localX in 0f..measuredWidth && localY in 0f..56f) return -1
        if (!expanded.value) return -2
        val bounds = menuBounds()
        if (localX !in bounds.x..(bounds.x + bounds.width) || localY !in bounds.y..(bounds.y + bounds.height)) return -2
        options.indices.forEach { index ->
            val top = bounds.y + 8f + index * 40f
            if (localY in top..(top + 32f)) return index
        }
        return Int.MIN_VALUE
    }
}

class MaterialTooltip(
    val visibleState: State<Boolean> = stateOf(false),
    var text: String,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 240f,
    height: Float = 120f,
    var anchorX: Float = 0f,
    var anchorY: Float = 0f,
    var tooltipMaxWidth: Float = 220f
) : Component(x, y, width, height) {
    private val invalidate = {
        visible = visibleState.value
        requestRender()
    }

    init {
        visible = visibleState.value
        visibleState.subscribe(invalidate)
        onDispose { visibleState.unsubscribe(invalidate) }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        val p = context.theme.palette
        val style = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), p.inverseOnSurface, context.theme.typography.fontFamily)
        val lines = text.wrapText(context, style, tooltipMaxWidth.coerceAtLeast(80f), 3)
        val tooltipWidth = ((lines.maxOfOrNull { context.backend.measureText(it, style).first } ?: 0f) + 24f).coerceIn(80f, tooltipMaxWidth.coerceAtLeast(80f) + 24f)
        val tooltipHeight = 16f + lines.size * (style.size + 4f)
        val tooltipX = anchorX.coerceIn(8f, (measuredWidth - tooltipWidth - 8f).coerceAtLeast(8f))
        val tooltipY = (anchorY - tooltipHeight - 12f).coerceAtLeast(8f)
        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(tooltipX, tooltipY, tooltipWidth, tooltipHeight, MaterialShapes.Small, p.inverseSurface, null)
            lines.forEachIndexed { index, line ->
                context.backend.drawText(line, tooltipX + 12f, tooltipY + 12f + index * (style.size + 4f), style)
            }
        }
    }
}

enum class MaterialSheetSide { Bottom, Side }

enum class MaterialSheetStyle { Modal, Standard }

open class MaterialBottomSheet(
    val visibleState: State<Boolean> = stateOf(false),
    var title: String,
    var body: String = "",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 240f,
    var sheetHeight: Float = 280f,
    var sheetWidth: Float = 320f,
    var side: MaterialSheetSide = MaterialSheetSide.Bottom,
    var sheetStyle: MaterialSheetStyle = MaterialSheetStyle.Modal,
    var confirmLabel: String = "Done",
    var dismissLabel: String = "Close",
    var dismissOnOutsideClick: Boolean = true,
    private val onConfirm: () -> Unit = {},
    private val onDismiss: () -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = {
        visible = visibleState.value
        requestRender()
    }
    private var hoverAction = Int.MIN_VALUE
    private var pressedAction = Int.MIN_VALUE

    init {
        focusable = true
        visible = visibleState.value
        visibleState.subscribe(invalidate)
        onDispose { visibleState.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!visible) return@on
            requestFocus()
            pressedAction = actionAt(it.x - x, it.y - y)
            it.consume()
        }
        on(PointerMove) {
            if (!visible) return@on
            hoverAction = actionAt(it.x - x, it.y - y)
            requestRender()
        }
        on(PointerUp) {
            if (!visible) return@on
            val releasedAction = actionAt(it.x - x, it.y - y)
            when {
                releasedAction == 1 && releasedAction == pressedAction -> confirm()
                releasedAction == 0 && releasedAction == pressedAction -> dismiss()
                releasedAction == -2 && pressedAction == -2 && dismissOnOutsideClick && sheetStyle == MaterialSheetStyle.Modal -> dismiss()
            }
            pressedAction = Int.MIN_VALUE
            it.consume()
        }
        onKey("keyDown") {
            if (!visible) return@onKey
            if (it.keyCode == AwtKeyEvent.VK_ESCAPE) {
                dismiss()
                it.consume()
            }
        }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        val p = context.theme.palette
        val bounds = sheetBounds()
        val titleStyle = TextStyle(context.theme.typography.titleSize + 1f, p.onSurface, context.theme.typography.fontFamily)
        val bodyStyle = TextStyle(context.theme.typography.bodySize, p.onSurfaceVariant, context.theme.typography.fontFamily)
        val actionStyle = TextStyle(context.theme.typography.bodySize, p.primary, context.theme.typography.fontFamily)
        val dismissStyle = TextStyle(context.theme.typography.bodySize, p.onSurfaceVariant, context.theme.typography.fontFamily)
        val contentWidth = (bounds.width - 48f).coerceAtLeast(48f)
        val lines = body.wrapText(context, bodyStyle, contentWidth, if (side == MaterialSheetSide.Side) 10 else 6)
        context.backend.translated(x, y) {
            if (sheetStyle == MaterialSheetStyle.Modal) {
                context.backend.drawRect(0f, 0f, measuredWidth, measuredHeight, p.scrim.withAlpha(132), null)
            }
            context.backend.drawRoundedRect(bounds.x, bounds.y, bounds.width, bounds.height, MaterialShapes.ExtraLarge, if (sheetStyle == MaterialSheetStyle.Standard) p.surfaceContainer else p.surfaceContainerHigh, null)
            if (side == MaterialSheetSide.Bottom) {
                context.backend.drawRoundedRect(bounds.x + (bounds.width - 32f) / 2f, bounds.y + 8f, 32f, 4f, 2f, p.outlineVariant, null)
            }
            context.backend.drawText(title.takeFitting(context, titleStyle, contentWidth), bounds.x + 24f, bounds.y + 34f, titleStyle)
            lines.forEachIndexed { index, line ->
                context.backend.drawText(line, bounds.x + 24f, bounds.y + 64f + index * (bodyStyle.size + 6f), bodyStyle)
            }
            if (dismissLabel.isNotBlank()) {
                drawDialogAction(context, dismissLabel, dismissStyle, bounds.x + bounds.width - 180f, bounds.y + bounds.height - 40f, 72f, hoverAction == 0, pressedAction == 0)
            }
            if (confirmLabel.isNotBlank()) {
                drawDialogAction(context, confirmLabel, actionStyle, bounds.x + bounds.width - 92f, bounds.y + bounds.height - 40f, 72f, hoverAction == 1, pressedAction == 1)
            }
        }
    }

    fun confirm() {
        if (!visible) return
        onConfirm()
        visibleState.value = false
    }

    fun dismiss() {
        if (!visible) return
        visibleState.value = false
        onDismiss()
    }

    private fun sheetBounds(): DialogBounds {
        return if (side == MaterialSheetSide.Bottom) {
            val resolvedHeight = sheetHeight.coerceAtLeast(160f).coerceAtMost((measuredHeight - 8f).coerceAtLeast(160f))
            DialogBounds(0f, measuredHeight - resolvedHeight, measuredWidth, resolvedHeight)
        } else {
            val resolvedWidth = sheetWidth.coerceAtLeast(240f).coerceAtMost((measuredWidth - 8f).coerceAtLeast(240f))
            DialogBounds(measuredWidth - resolvedWidth, 0f, resolvedWidth, measuredHeight)
        }
    }

    private fun actionAt(localX: Float, localY: Float): Int {
        val bounds = sheetBounds()
        if (localX !in bounds.x..(bounds.x + bounds.width) || localY !in bounds.y..(bounds.y + bounds.height)) return -2
        val actionTop = bounds.y + bounds.height - 44f
        if (localY !in actionTop..(actionTop + 32f)) return Int.MIN_VALUE
        val confirmStart = bounds.x + bounds.width - 96f
        if (confirmLabel.isNotBlank() && localX in confirmStart..(confirmStart + 80f)) return 1
        val dismissStart = confirmStart - 88f
        if (dismissLabel.isNotBlank() && localX in dismissStart..(dismissStart + 80f)) return 0
        return Int.MIN_VALUE
    }
}

class MaterialModalBottomSheet(
    visibleState: State<Boolean> = stateOf(false),
    title: String,
    body: String = "",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 240f,
    sheetHeight: Float = 280f,
    confirmLabel: String = "Done",
    dismissLabel: String = "Close",
    dismissOnOutsideClick: Boolean = true,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
) : MaterialBottomSheet(
    visibleState = visibleState,
    title = title,
    body = body,
    x = x,
    y = y,
    width = width,
    height = height,
    sheetHeight = sheetHeight,
    side = MaterialSheetSide.Bottom,
    sheetStyle = MaterialSheetStyle.Modal,
    confirmLabel = confirmLabel,
    dismissLabel = dismissLabel,
    dismissOnOutsideClick = dismissOnOutsideClick,
    onConfirm = onConfirm,
    onDismiss = onDismiss
)

class MaterialStandardBottomSheet(
    visibleState: State<Boolean> = stateOf(true),
    title: String,
    body: String = "",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 240f,
    sheetHeight: Float = 280f,
    confirmLabel: String = "Done",
    dismissLabel: String = "",
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
) : MaterialBottomSheet(
    visibleState = visibleState,
    title = title,
    body = body,
    x = x,
    y = y,
    width = width,
    height = height,
    sheetHeight = sheetHeight,
    side = MaterialSheetSide.Bottom,
    sheetStyle = MaterialSheetStyle.Standard,
    confirmLabel = confirmLabel,
    dismissLabel = dismissLabel,
    dismissOnOutsideClick = false,
    onConfirm = onConfirm,
    onDismiss = onDismiss
)

class MaterialSideSheet(
    visibleState: State<Boolean> = stateOf(false),
    title: String,
    body: String = "",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 240f,
    sheetWidth: Float = 320f,
    confirmLabel: String = "Done",
    dismissLabel: String = "Close",
    dismissOnOutsideClick: Boolean = true,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
) : MaterialBottomSheet(
    visibleState = visibleState,
    title = title,
    body = body,
    x = x,
    y = y,
    width = width,
    height = height,
    sheetWidth = sheetWidth,
    side = MaterialSheetSide.Side,
    sheetStyle = MaterialSheetStyle.Modal,
    confirmLabel = confirmLabel,
    dismissLabel = dismissLabel,
    dismissOnOutsideClick = dismissOnOutsideClick,
    onConfirm = onConfirm,
    onDismiss = onDismiss
)

class MaterialStandardSideSheet(
    visibleState: State<Boolean> = stateOf(true),
    title: String,
    body: String = "",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 240f,
    sheetWidth: Float = 320f,
    confirmLabel: String = "Done",
    dismissLabel: String = "",
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
) : MaterialBottomSheet(
    visibleState = visibleState,
    title = title,
    body = body,
    x = x,
    y = y,
    width = width,
    height = height,
    sheetWidth = sheetWidth,
    side = MaterialSheetSide.Side,
    sheetStyle = MaterialSheetStyle.Standard,
    confirmLabel = confirmLabel,
    dismissLabel = dismissLabel,
    dismissOnOutsideClick = false,
    onConfirm = onConfirm,
    onDismiss = onDismiss
)

class MaterialDatePicker(
    val selectedDate: State<String> = stateOf(""),
    val visibleState: State<Boolean> = stateOf(false),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 320f,
    var title: String = "Select date",
    private val onConfirm: (String) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = {
        visible = visibleState.value
        requestRender()
    }
    private var hoverDay = -1
    private var pressedDay = -1
    private var draftYear = yearFrom(selectedDate.value)
    private var draftMonth = monthFrom(selectedDate.value)
    private var draftDay = dayFrom(selectedDate.value)

    init {
        focusable = true
        visible = visibleState.value
        visibleState.subscribe(invalidate)
        selectedDate.subscribe {
            draftYear = yearFrom(selectedDate.value)
            draftMonth = monthFrom(selectedDate.value)
            draftDay = dayFrom(selectedDate.value)
            requestRender()
        }
        onDispose {
            visibleState.unsubscribe(invalidate)
        }
        on(PointerDown) {
            if (!visible) return@on
            requestFocus()
            pressedDay = dayAt(it.x - x, it.y - y)
            it.consume()
        }
        on(PointerMove) {
            if (!visible) return@on
            hoverDay = dayAt(it.x - x, it.y - y)
            requestRender()
        }
        on(PointerUp) {
            if (!visible) return@on
            val localX = it.x - x
            val localY = it.y - y
            when {
                localY in 44f..84f && localX in 56f..96f -> shiftMonth(-1)
                localY in 44f..84f && localX in (measuredWidth - 96f)..(measuredWidth - 56f) -> shiftMonth(1)
                else -> {
                    val releasedDay = dayAt(localX, localY)
                    if (releasedDay in 1..daysInMonth(draftYear, draftMonth) && releasedDay == pressedDay) {
                        draftDay = releasedDay
                        selectedDate.value = formatDate(draftYear, draftMonth, draftDay)
                        onConfirm(selectedDate.value)
                        visibleState.value = false
                    }
                }
            }
            pressedDay = -1
            it.consume()
        }
        onKey("keyDown") {
            if (!visible) return@onKey
            when (it.keyCode) {
                AwtKeyEvent.VK_ESCAPE -> {
                    visibleState.value = false
                    it.consume()
                }
                AwtKeyEvent.VK_LEFT -> {
                    draftDay = (draftDay - 1).coerceAtLeast(1)
                    requestRender()
                    it.consume()
                }
                AwtKeyEvent.VK_RIGHT -> {
                    draftDay = (draftDay + 1).coerceAtMost(daysInMonth(draftYear, draftMonth))
                    requestRender()
                    it.consume()
                }
                AwtKeyEvent.VK_UP -> {
                    shiftMonth(-1)
                    it.consume()
                }
                AwtKeyEvent.VK_DOWN -> {
                    shiftMonth(1)
                    it.consume()
                }
                AwtKeyEvent.VK_ENTER -> {
                    selectedDate.value = formatDate(draftYear, draftMonth, draftDay)
                    onConfirm(selectedDate.value)
                    visibleState.value = false
                    it.consume()
                }
            }
        }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        val p = context.theme.palette
        val bounds = DialogBounds(32f, 20f, measuredWidth - 64f, measuredHeight - 40f)
        val titleStyle = TextStyle(context.theme.typography.titleSize + 1f, p.onSurface, context.theme.typography.fontFamily)
        val monthStyle = TextStyle(context.theme.typography.bodySize, p.onSurface, context.theme.typography.fontFamily)
        val dayStyle = TextStyle(context.theme.typography.bodySize, p.onSurface, context.theme.typography.fontFamily)
        val columns = 7
        val cellSize = ((bounds.width - 32f) / columns).coerceAtLeast(28f)
        val monthLabel = monthNames[draftMonth - 1] + " " + draftYear
        val weekdayStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), p.onSurfaceVariant, context.theme.typography.fontFamily)
        context.backend.translated(x, y) {
            context.backend.drawRect(0f, 0f, measuredWidth, measuredHeight, p.scrim.withAlpha(132), null)
            context.backend.drawRoundedRect(bounds.x, bounds.y, bounds.width, bounds.height, MaterialShapes.ExtraLarge, p.surfaceContainerHigh, null)
            context.backend.drawText(title, bounds.x + 24f, bounds.y + 34f, titleStyle)
            drawTopBarAction(context, "‹", bounds.x + 20f, bounds.y + 44f, false, false)
            drawTopBarAction(context, "›", bounds.x + bounds.width - 60f, bounds.y + 44f, false, false)
            val monthWidth = context.backend.measureText(monthLabel, monthStyle).first
            context.backend.drawText(monthLabel, bounds.x + (bounds.width - monthWidth) / 2f, bounds.y + 72f, monthStyle)
            weekdays.forEachIndexed { index, name ->
                val labelWidth = context.backend.measureText(name, weekdayStyle).first
                val labelX = bounds.x + 16f + index * cellSize + ((cellSize - 4f) - labelWidth) / 2f
                context.backend.drawText(name, labelX, bounds.y + 104f, weekdayStyle)
            }
            val maxDay = daysInMonth(draftYear, draftMonth)
            (1..maxDay).forEach { day ->
                val index = day - 1
                val col = index % columns
                val row = index / columns
                val cellX = bounds.x + 16f + col * cellSize
                val cellY = bounds.y + 116f + row * cellSize
                val selected = draftDay == day
                val hovered = hoverDay == day || pressedDay == day
                if (selected || hovered) {
                    context.backend.drawRoundedRect(cellX, cellY, cellSize - 4f, cellSize - 4f, MaterialShapes.Full, if (selected) p.primary else overlay(p.surfaceContainerHigh, p.onSurface, 0.06f, 1f), null)
                }
                val color = if (selected) p.onPrimary else p.onSurface
                val style = TextStyle(dayStyle.size, color, dayStyle.fontFamily)
                val text = day.toString()
                val textWidth = context.backend.measureText(text, style).first
                context.backend.drawText(text, cellX + (cellSize - 4f - textWidth) / 2f, cellY + textBaseline(cellSize - 4f, style.size), style)
            }
        }
    }

    private fun dayAt(localX: Float, localY: Float): Int {
        val bounds = DialogBounds(32f, 20f, measuredWidth - 64f, measuredHeight - 40f)
        val cellSize = ((bounds.width - 32f) / 7f).coerceAtLeast(28f)
        val gridY = bounds.y + 116f
        if (localX < bounds.x + 16f || localY < gridY) return -1
        val col = ((localX - (bounds.x + 16f)) / cellSize).toInt()
        val row = ((localY - gridY) / cellSize).toInt()
        if (col !in 0..6 || row !in 0..5) return -1
        val day = row * 7 + col + 1
        return day.takeIf { it in 1..daysInMonth(draftYear, draftMonth) } ?: -1
    }

    private fun shiftMonth(delta: Int) {
        val absoluteMonth = (draftYear * 12 + (draftMonth - 1)) + delta
        draftYear = absoluteMonth.floorDiv(12)
        draftMonth = absoluteMonth.mod(12) + 1
        draftDay = draftDay.coerceAtMost(daysInMonth(draftYear, draftMonth))
        requestRender()
    }

    private fun yearFrom(value: String): Int = value.substringBefore('-', "2026").toIntOrNull()?.coerceIn(1900, 9999) ?: 2026

    private fun monthFrom(value: String): Int = value.split('-').getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 7

    private fun dayFrom(value: String): Int = value.substringAfterLast('-', "1").toIntOrNull()?.coerceIn(1, 31) ?: 1

    private fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)) 29 else 28
        else -> 31
    }

    private fun formatDate(year: Int, month: Int, day: Int): String = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

    private companion object {
        private val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        private val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    }
}

class MaterialTimePicker(
    val selectedTime: State<String> = stateOf("12:00"),
    val visibleState: State<Boolean> = stateOf(false),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 320f,
    height: Float = 280f,
    var title: String = "Select time",
    var use24Hour: Boolean = true,
    private val onConfirm: (String) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = {
        visible = visibleState.value
        requestRender()
    }
    private var selectedHour = parseHour(selectedTime.value)
    private var selectedMinute = parseMinute(selectedTime.value)
    private var hoverHour = -1
    private var hoverMinute = -1
    private var hourPage = 0
    private var pmSelected = selectedHour >= 12

    init {
        focusable = true
        visible = visibleState.value
        visibleState.subscribe(invalidate)
        selectedTime.subscribe {
            selectedHour = parseHour(selectedTime.value)
            selectedMinute = parseMinute(selectedTime.value)
            pmSelected = selectedHour >= 12
            hourPage = if (use24Hour) selectedHour / 6 else ((displayHour(selectedHour) - 1) / 6)
            requestRender()
        }
        onDispose { visibleState.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!visible) return@on
            requestFocus()
            updateSelection(it.x - x, it.y - y)
            it.consume()
        }
        on(PointerMove) {
            if (!visible) return@on
            updateHover(it.x - x, it.y - y)
            requestRender()
        }
        onKey("keyDown") {
            if (!visible) return@onKey
            when (it.keyCode) {
                AwtKeyEvent.VK_ESCAPE -> {
                    visibleState.value = false
                    it.consume()
                }
                AwtKeyEvent.VK_UP -> {
                    selectedHour = (selectedHour + 1) % 24
                    pmSelected = selectedHour >= 12
                    hourPage = if (use24Hour) selectedHour / 6 else ((displayHour(selectedHour) - 1) / 6)
                    requestRender()
                    it.consume()
                }
                AwtKeyEvent.VK_DOWN -> {
                    selectedHour = (selectedHour + 23) % 24
                    pmSelected = selectedHour >= 12
                    hourPage = if (use24Hour) selectedHour / 6 else ((displayHour(selectedHour) - 1) / 6)
                    requestRender()
                    it.consume()
                }
                AwtKeyEvent.VK_LEFT -> {
                    hourPage = (hourPage - 1).coerceAtLeast(0)
                    requestRender()
                    it.consume()
                }
                AwtKeyEvent.VK_RIGHT -> {
                    hourPage = (hourPage + 1).coerceAtMost(3)
                    requestRender()
                    it.consume()
                }
                AwtKeyEvent.VK_ENTER -> {
                    selectedTime.value = formatTime(selectedHour, selectedMinute)
                    onConfirm(selectedTime.value)
                    visibleState.value = false
                    it.consume()
                }
            }
        }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        val p = context.theme.palette
        val bounds = DialogBounds(32f, 24f, measuredWidth - 64f, measuredHeight - 48f)
        val titleStyle = TextStyle(context.theme.typography.titleSize + 1f, p.onSurface, context.theme.typography.fontFamily)
        val valueStyle = TextStyle(context.theme.typography.titleSize + 8f, p.primary, context.theme.typography.fontFamily)
        val cellStyle = TextStyle(context.theme.typography.bodySize, p.onSurface, context.theme.typography.fontFamily)
        context.backend.translated(x, y) {
            context.backend.drawRect(0f, 0f, measuredWidth, measuredHeight, p.scrim.withAlpha(132), null)
            context.backend.drawRoundedRect(bounds.x, bounds.y, bounds.width, bounds.height, MaterialShapes.ExtraLarge, p.surfaceContainerHigh, null)
            context.backend.drawText(title, bounds.x + 24f, bounds.y + 34f, titleStyle)
            val value = if (use24Hour) formatTime(selectedHour, selectedMinute) else formatDisplayTime(selectedHour, selectedMinute, pmSelected)
            val valueWidth = context.backend.measureText(value, valueStyle).first
            context.backend.drawText(value, bounds.x + (bounds.width - valueWidth) / 2f, bounds.y + 76f, valueStyle)
            drawTopBarAction(context, "‹", bounds.x + 24f, bounds.y + 88f, false, false)
            drawTopBarAction(context, "›", bounds.x + 72f, bounds.y + 88f, false, false)
            drawTimeColumn(context, bounds.x + 24f, bounds.y + 136f, 88f, 96f, visibleHourItems(), hourSelectionIndex(), hoverHour, cellStyle)
            drawTimeColumn(context, bounds.x + bounds.width - 112f, bounds.y + 136f, 88f, 96f, minuteOptions.map { it.toString().padStart(2, '0') }, minuteIndex(selectedMinute), hoverMinute, cellStyle)
            if (!use24Hour) {
                drawMeridiemToggle(context, bounds.x + bounds.width / 2f - 44f, bounds.y + 136f, 88f, 72f)
            }
        }
    }

    private fun updateSelection(localX: Float, localY: Float) {
        val bounds = DialogBounds(32f, 24f, measuredWidth - 64f, measuredHeight - 48f)
        when {
            localX in (bounds.x + 24f)..(bounds.x + 64f) && localY in (bounds.y + 88f)..(bounds.y + 128f) -> {
                hourPage = (hourPage - 1).coerceAtLeast(0)
                return
            }
            localX in (bounds.x + 72f)..(bounds.x + 112f) && localY in (bounds.y + 88f)..(bounds.y + 128f) -> {
                hourPage = (hourPage + 1).coerceAtMost(3)
                return
            }
            !use24Hour && localX in (bounds.x + bounds.width / 2f - 44f)..(bounds.x + bounds.width / 2f + 44f) && localY in (bounds.y + 136f)..(bounds.y + 208f) -> {
                pmSelected = localY >= bounds.y + 172f
                selectedHour = applyMeridiem(displayHour(selectedHour), pmSelected)
                return
            }
        }
        val hour = timeCellAt(localX, localY, true)
        val minute = timeCellAt(localX, localY, false)
        if (hour >= 0) {
            selectedHour = if (use24Hour) hour else applyMeridiem(hour + 1, pmSelected)
            pmSelected = selectedHour >= 12
        }
        if (minute >= 0) selectedMinute = minuteOptions[minute]
        if (hour >= 0 || minute >= 0) {
            selectedTime.value = formatTime(selectedHour, selectedMinute)
            onConfirm(selectedTime.value)
            visibleState.value = false
        }
    }

    private fun updateHover(localX: Float, localY: Float) {
        hoverHour = timeCellAt(localX, localY, true)
        hoverMinute = timeCellAt(localX, localY, false)
    }

    private fun timeCellAt(localX: Float, localY: Float, hourColumn: Boolean): Int {
        val bounds = DialogBounds(32f, 24f, measuredWidth - 64f, measuredHeight - 48f)
        val startX = if (hourColumn) bounds.x + 24f else bounds.x + bounds.width - 112f
        val startY = bounds.y + 136f
        val cellHeight = 24f
        val maxRows = 4
        if (localX !in startX..(startX + 88f) || localY !in startY..(startY + maxRows * cellHeight)) return -1
        val row = ((localY - startY) / cellHeight).toInt().coerceIn(0, 3)
        return if (hourColumn) {
            val index = hourPage * 4 + row
            val maxIndex = if (use24Hour) 23 else 11
            index.takeIf { it in 0..maxIndex } ?: -1
        } else {
            row.coerceAtMost(3)
        }
    }

    private fun visibleHourItems(): List<String> {
        val values = if (use24Hour) (0..23).toList() else (1..12).toList()
        return values.drop(hourPage * 4).take(4).map { it.toString().padStart(2, '0') }
    }

    private fun hourSelectionIndex(): Int {
        val base = hourPage * 4
        val index = if (use24Hour) selectedHour else displayHour(selectedHour) - 1
        return (index - base).coerceIn(0, 3)
    }

    private fun drawMeridiemToggle(context: RenderContext, x: Float, y: Float, width: Float, height: Float) {
        val p = context.theme.palette
        val optionHeight = height / 2f
        val style = TextStyle(context.theme.typography.bodySize, p.onSurface, context.theme.typography.fontFamily)
        listOf("AM", "PM").forEachIndexed { index, label ->
            val selected = if (index == 0) !pmSelected else pmSelected
            val boxY = y + index * optionHeight
            context.backend.drawRoundedRect(x, boxY, width, optionHeight - 4f, 12f, if (selected) p.primaryContainer else p.surfaceContainer, null)
            val textStyle = TextStyle(style.size, if (selected) p.onPrimaryContainer else p.onSurface, style.fontFamily)
            val textWidth = context.backend.measureText(label, textStyle).first
            context.backend.drawText(label, x + (width - textWidth) / 2f, boxY + textBaseline(optionHeight - 4f, textStyle.size), textStyle)
        }
    }

    private fun displayHour(hour24: Int): Int = ((hour24 + 11) % 12) + 1

    private fun applyMeridiem(hour12: Int, pm: Boolean): Int {
        val normalized = hour12.coerceIn(1, 12) % 12
        return if (pm) normalized + 12 else normalized
    }

    private fun parseHour(value: String): Int = value.substringBefore(':').toIntOrNull()?.coerceIn(0, 23) ?: 12

    private fun parseMinute(value: String): Int = value.substringAfter(':', "00").toIntOrNull()?.let { minute -> minuteOptions.minBy { abs(it - minute) } } ?: 0

    private fun minuteIndex(value: Int): Int = minuteOptions.indexOf(value).coerceAtLeast(0)

    private fun formatTime(hour: Int, minute: Int): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

    private fun formatDisplayTime(hour: Int, minute: Int, pm: Boolean): String = "${displayHour(hour).toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} ${if (pm) "PM" else "AM"}"

    private companion object {
        private val minuteOptions = listOf(0, 15, 30, 45)
    }
}

class MaterialDateRangePicker(
    val startDate: State<String> = stateOf(""),
    val endDate: State<String> = stateOf(""),
    val visibleState: State<Boolean> = stateOf(false),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 320f,
    var title: String = "Select date range",
    private val onConfirm: (String, String) -> Unit = { _, _ -> }
) : Component(x, y, width, height) {
    private val invalidate = {
        visible = visibleState.value
        requestRender()
    }
    private var hoverDay = -1
    private var pressedDay = -1
    private var draftStart = startDate.value
    private var draftEnd = endDate.value
    private var draftYear = yearFrom(startDate.value.ifBlank { endDate.value })
    private var draftMonth = monthFrom(startDate.value.ifBlank { endDate.value })
    private var draftDay = dayFrom(startDate.value.ifBlank { endDate.value })

    init {
        focusable = true
        visible = visibleState.value
        visibleState.subscribe(invalidate)
        startDate.subscribe {
            draftStart = startDate.value
            draftYear = yearFrom(startDate.value.ifBlank { endDate.value })
            draftMonth = monthFrom(startDate.value.ifBlank { endDate.value })
            draftDay = dayFrom(startDate.value.ifBlank { endDate.value })
            requestRender()
        }
        endDate.subscribe {
            draftEnd = endDate.value
            requestRender()
        }
        onDispose { visibleState.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!visible) return@on
            requestFocus()
            pressedDay = dayAt(it.x - x, it.y - y)
            it.consume()
        }
        on(PointerMove) {
            if (!visible) return@on
            hoverDay = dayAt(it.x - x, it.y - y)
            requestRender()
        }
        on(PointerUp) {
            if (!visible) return@on
            val localX = it.x - x
            val localY = it.y - y
            when {
                localY in 44f..84f && localX in 56f..96f -> shiftMonth(-1)
                localY in 44f..84f && localX in (measuredWidth - 96f)..(measuredWidth - 56f) -> shiftMonth(1)
                else -> {
                    val releasedDay = dayAt(localX, localY)
                    if (releasedDay in 1..daysInMonth(draftYear, draftMonth) && releasedDay == pressedDay) {
                        selectDay(releasedDay)
                    }
                }
            }
            pressedDay = -1
            it.consume()
        }
        onKey("keyDown") {
            if (!visible) return@onKey
            when (it.keyCode) {
                AwtKeyEvent.VK_ESCAPE -> {
                    visibleState.value = false
                    it.consume()
                }
                AwtKeyEvent.VK_LEFT -> {
                    draftDay = (draftDay - 1).coerceAtLeast(1)
                    requestRender()
                    it.consume()
                }
                AwtKeyEvent.VK_RIGHT -> {
                    draftDay = (draftDay + 1).coerceAtMost(daysInMonth(draftYear, draftMonth))
                    requestRender()
                    it.consume()
                }
                AwtKeyEvent.VK_UP -> {
                    shiftMonth(-1)
                    it.consume()
                }
                AwtKeyEvent.VK_DOWN -> {
                    shiftMonth(1)
                    it.consume()
                }
                AwtKeyEvent.VK_ENTER -> {
                    selectDay(draftDay)
                    it.consume()
                }
            }
        }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        val p = context.theme.palette
        val bounds = DialogBounds(32f, 20f, measuredWidth - 64f, measuredHeight - 40f)
        val titleStyle = TextStyle(context.theme.typography.titleSize + 1f, p.onSurface, context.theme.typography.fontFamily)
        val summaryStyle = TextStyle(context.theme.typography.bodySize, p.primary, context.theme.typography.fontFamily)
        val monthStyle = TextStyle(context.theme.typography.bodySize, p.onSurface, context.theme.typography.fontFamily)
        val dayStyle = TextStyle(context.theme.typography.bodySize, p.onSurface, context.theme.typography.fontFamily)
        val weekdayStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), p.onSurfaceVariant, context.theme.typography.fontFamily)
        val cellSize = ((bounds.width - 32f) / 7f).coerceAtLeast(28f)
        val monthLabel = monthNames[draftMonth - 1] + " " + draftYear
        val summary = when {
            draftStart.isBlank() -> "Select a start date"
            draftEnd.isBlank() -> "$draftStart – …"
            else -> "$draftStart – $draftEnd"
        }
        context.backend.translated(x, y) {
            context.backend.drawRect(0f, 0f, measuredWidth, measuredHeight, p.scrim.withAlpha(132), null)
            context.backend.drawRoundedRect(bounds.x, bounds.y, bounds.width, bounds.height, MaterialShapes.ExtraLarge, p.surfaceContainerHigh, null)
            context.backend.drawText(title, bounds.x + 24f, bounds.y + 34f, titleStyle)
            context.backend.drawText(summary.takeFitting(context, summaryStyle, bounds.width - 48f), bounds.x + 24f, bounds.y + 62f, summaryStyle)
            drawTopBarAction(context, "‹", bounds.x + 20f, bounds.y + 76f, false, false)
            drawTopBarAction(context, "›", bounds.x + bounds.width - 60f, bounds.y + 76f, false, false)
            val monthWidth = context.backend.measureText(monthLabel, monthStyle).first
            context.backend.drawText(monthLabel, bounds.x + (bounds.width - monthWidth) / 2f, bounds.y + 104f, monthStyle)
            weekdays.forEachIndexed { index, name ->
                val labelWidth = context.backend.measureText(name, weekdayStyle).first
                val labelX = bounds.x + 16f + index * cellSize + ((cellSize - 4f) - labelWidth) / 2f
                context.backend.drawText(name, labelX, bounds.y + 136f, weekdayStyle)
            }
            val maxDay = daysInMonth(draftYear, draftMonth)
            (1..maxDay).forEach { day ->
                val index = day - 1
                val col = index % 7
                val row = index / 7
                val cellX = bounds.x + 16f + col * cellSize
                val cellY = bounds.y + 148f + row * cellSize
                val value = formatDate(draftYear, draftMonth, day)
                val selected = value == draftStart || value == draftEnd
                val inRange = draftStart.isNotBlank() && draftEnd.isNotBlank() && value > draftStart && value < draftEnd
                val hovered = hoverDay == day || pressedDay == day || draftDay == day
                when {
                    selected -> context.backend.drawRoundedRect(cellX, cellY, cellSize - 4f, cellSize - 4f, MaterialShapes.Full, p.primary, null)
                    inRange -> context.backend.drawRoundedRect(cellX, cellY, cellSize - 4f, cellSize - 4f, MaterialShapes.Medium, p.primaryContainer, null)
                    hovered -> context.backend.drawRoundedRect(cellX, cellY, cellSize - 4f, cellSize - 4f, MaterialShapes.Full, overlay(p.surfaceContainerHigh, p.onSurface, 0.06f, 1f), null)
                }
                val color = when {
                    selected -> p.onPrimary
                    inRange -> p.onPrimaryContainer
                    else -> p.onSurface
                }
                val style = TextStyle(dayStyle.size, color, dayStyle.fontFamily)
                val text = day.toString()
                val textWidth = context.backend.measureText(text, style).first
                context.backend.drawText(text, cellX + (cellSize - 4f - textWidth) / 2f, cellY + textBaseline(cellSize - 4f, style.size), style)
            }
        }
    }

    private fun selectDay(day: Int) {
        val chosen = formatDate(draftYear, draftMonth, day)
        draftDay = day
        when {
            draftStart.isBlank() || draftEnd.isNotBlank() -> {
                draftStart = chosen
                draftEnd = ""
                startDate.value = draftStart
                endDate.value = draftEnd
                requestRender()
            }
            else -> {
                if (chosen < draftStart) {
                    draftEnd = draftStart
                    draftStart = chosen
                } else {
                    draftEnd = chosen
                }
                startDate.value = draftStart
                endDate.value = draftEnd
                onConfirm(draftStart, draftEnd)
                visibleState.value = false
            }
        }
    }

    private fun dayAt(localX: Float, localY: Float): Int {
        val bounds = DialogBounds(32f, 20f, measuredWidth - 64f, measuredHeight - 40f)
        val cellSize = ((bounds.width - 32f) / 7f).coerceAtLeast(28f)
        val gridY = bounds.y + 148f
        if (localX < bounds.x + 16f || localY < gridY) return -1
        val col = ((localX - (bounds.x + 16f)) / cellSize).toInt()
        val row = ((localY - gridY) / cellSize).toInt()
        if (col !in 0..6 || row !in 0..5) return -1
        val day = row * 7 + col + 1
        return day.takeIf { it in 1..daysInMonth(draftYear, draftMonth) } ?: -1
    }

    private fun shiftMonth(delta: Int) {
        val absoluteMonth = (draftYear * 12 + (draftMonth - 1)) + delta
        draftYear = absoluteMonth.floorDiv(12)
        draftMonth = absoluteMonth.mod(12) + 1
        draftDay = draftDay.coerceAtMost(daysInMonth(draftYear, draftMonth))
        requestRender()
    }

    private fun yearFrom(value: String): Int = value.substringBefore('-', "2026").toIntOrNull()?.coerceIn(1900, 9999) ?: 2026

    private fun monthFrom(value: String): Int = value.split('-').getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 7

    private fun dayFrom(value: String): Int = value.substringAfterLast('-', "1").toIntOrNull()?.coerceIn(1, 31) ?: 1

    private fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)) 29 else 28
        else -> 31
    }

    private fun formatDate(year: Int, month: Int, day: Int): String = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

    private companion object {
        private val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        private val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    }
}

data class MaterialCarouselItem(
    val headline: String,
    val supportingText: String = "",
    val body: String = "",
    val label: String = "",
    val onClick: () -> Unit = {}
)

class MaterialCarousel(
    val items: List<MaterialCarouselItem>,
    val selectedIndex: State<Int> = stateOf(0),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 320f,
    height: Float = 220f,
    var showIndicators: Boolean = true,
    private val onSelect: (Int) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var hoverTarget = Int.MIN_VALUE
    private var pressedTarget = Int.MIN_VALUE
    private var displayIndex = selectedIndex.value.toFloat()

    init {
        focusable = true
        minSize(220f, 180f)
        clipToBounds = true
        selectedIndex.subscribe(invalidate)
        onDispose { selectedIndex.unsubscribe(invalidate) }
        on(PointerDown) {
            if (items.isEmpty() || disabled) return@on
            requestFocus()
            val local = sceneToLocal(it.x, it.y)
            pressedTarget = actionAt(local.first, local.second)
            if (pressedTarget != Int.MIN_VALUE) it.consume()
        }
        on(PointerMove) {
            if (items.isEmpty()) return@on
            val local = sceneToLocal(it.x, it.y)
            hoverTarget = actionAt(local.first, local.second)
            requestRender()
        }
        on(PointerUp) {
            if (items.isEmpty() || disabled) return@on
            val local = sceneToLocal(it.x, it.y)
            val releasedTarget = actionAt(local.first, local.second)
            if (releasedTarget != Int.MIN_VALUE && releasedTarget == pressedTarget) {
                activate(releasedTarget)
                it.consume()
            }
            pressedTarget = Int.MIN_VALUE
        }
        onKey("keyDown") {
            if (items.isEmpty() || disabled) return@onKey
            when (it.keyCode) {
                AwtKeyEvent.VK_LEFT -> {
                    moveSelection(-1)
                    it.consume()
                }
                AwtKeyEvent.VK_RIGHT -> {
                    moveSelection(1)
                    it.consume()
                }
                AwtKeyEvent.VK_ENTER, AwtKeyEvent.VK_SPACE -> {
                    items.getOrNull(selectedIndex.value.coerceIn(0, items.lastIndex))?.onClick?.invoke()
                    it.consume()
                }
            }
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        if (items.isEmpty()) {
            backgroundColor = p.surfaceContainer
            cornerRadius = MaterialShapes.ExtraLarge
            borderColor = p.outlineVariant
            borderWidth = 1f
            super.draw(context)
            val style = TextStyle(context.theme.typography.bodySize, p.onSurfaceVariant, context.theme.typography.fontFamily)
            context.backend.drawText("No items", x + 24f, y + textBaseline(measuredHeight, style.size), style)
            return
        }
        val targetIndex = selectedIndex.value.coerceIn(0, items.lastIndex)
        displayIndex = animate(displayIndex, targetIndex.toFloat(), context.time.deltaSeconds, 14f)
        val viewportWidth = measuredWidth.coerceAtLeast(width)
        val viewportHeight = measuredHeight.coerceAtLeast(height)
        val cardWidth = (viewportWidth * 0.68f).coerceIn(180f, 360f)
        val cardHeight = (viewportHeight - if (showIndicators) 44f else 20f).coerceAtLeast(132f)
        val cardTop = 8f
        val centerX = (viewportWidth - cardWidth) / 2f
        val previewWidth = (cardWidth * 0.22f).coerceAtLeast(28f)
        val arrowSize = 32f
        val indicatorY = cardTop + cardHeight + 18f
        context.backend.translated(x, y) {
            context.backend.clipped(0f, 0f, viewportWidth, viewportHeight) {
            items.forEachIndexed { index, item ->
                val offset = index - displayIndex
                if (abs(offset) > 1.15f) return@forEachIndexed
                val absOffset = abs(offset)
                val scale = 1f - 0.12f * absOffset.coerceAtMost(1f)
                val drawWidth = cardWidth * scale
                val drawHeight = cardHeight * scale
                val drawX = centerX + offset * (cardWidth * 0.84f) + (cardWidth - drawWidth) / 2f
                val drawY = cardTop + 8f * absOffset
                val current = index == targetIndex
                val hovered = hoverTarget == index
                val pressed = pressedTarget == index
                val fill = if (current) p.secondaryContainer else p.surfaceContainerHigh
                val stroke = if (current) null else Stroke(p.outlineVariant.withAlpha((255f * (1f - 0.35f * absOffset)).toInt().coerceIn(0, 255)), 1f)
                context.backend.drawRoundedRect(drawX, drawY, drawWidth, drawHeight, MaterialShapes.ExtraLarge, overlay(fill, if (current) p.onSecondaryContainer else p.onSurface, if (hovered || pressed) if (pressed) 0.10f else 0.06f else 0f, 1f), stroke)
                val textColor = if (current) p.onSecondaryContainer else p.onSurface
                val supportingColor = if (current) p.onSecondaryContainer.withAlpha(190) else p.onSurfaceVariant
                val labelColor = if (current) p.primary.withAlpha(220) else p.primary
                val contentWidth = (drawWidth - 32f).coerceAtLeast(48f)
                val labelStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), labelColor, context.theme.typography.fontFamily)
                val titleStyle = TextStyle((context.theme.typography.bodySize + 6f).coerceAtMost(24f), textColor, context.theme.typography.fontFamily)
                val supportingStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), supportingColor, context.theme.typography.fontFamily)
                val bodyStyle = TextStyle(context.theme.typography.bodySize, supportingColor, context.theme.typography.fontFamily)
                var cursorY = drawY + 28f
                if (item.label.isNotBlank()) {
                    context.backend.drawText(item.label.takeFitting(context, labelStyle, contentWidth), drawX + 16f, cursorY, labelStyle)
                    cursorY += labelStyle.size + 14f
                }
                context.backend.drawText(item.headline.takeFitting(context, titleStyle, contentWidth), drawX + 16f, cursorY, titleStyle)
                cursorY += titleStyle.size + 10f
                if (item.supportingText.isNotBlank()) {
                    val lines = item.supportingText.wrapText(context, supportingStyle, contentWidth, 2)
                    lines.forEach { line ->
                        context.backend.drawText(line, drawX + 16f, cursorY, supportingStyle)
                        cursorY += supportingStyle.size + 5f
                    }
                    cursorY += 4f
                }
                if (item.body.isNotBlank()) {
                    val maxBodyLines = if (current) 4 else 3
                    val bodyLines = item.body.wrapText(context, bodyStyle, contentWidth, maxBodyLines)
                    bodyLines.forEach { line ->
                        context.backend.drawText(line, drawX + 16f, cursorY, bodyStyle)
                        cursorY += bodyStyle.size + 6f
                    }
                }
            }
            if (items.size > 1) {
                drawCarouselArrow(context, centerX - previewWidth - 12f, cardTop + (cardHeight - arrowSize) / 2f, arrowSize, "‹", hoverTarget == -1, pressedTarget == -1)
                drawCarouselArrow(context, centerX + cardWidth + previewWidth - 20f, cardTop + (cardHeight - arrowSize) / 2f, arrowSize, "›", hoverTarget == -2, pressedTarget == -2)
            }
            if (showIndicators) {
                val active = selectedIndex.value.coerceIn(0, items.lastIndex)
                val spacing = 14f
                val startX = (viewportWidth - (items.size - 1) * spacing) / 2f
                items.indices.forEach { index ->
                    val selected = index == active
                    context.backend.drawCircle(startX + index * spacing, indicatorY, if (selected) 4f else 3f, if (selected) p.primary else p.outlineVariant)
                }
            }
            }
        }
        if (abs(displayIndex - targetIndex.toFloat()) > 0.001f) requestRender()
    }

    private fun activate(target: Int) {
        when (target) {
            -1 -> moveSelection(-1)
            -2 -> moveSelection(1)
            else -> {
                val safe = target.coerceIn(0, items.lastIndex)
                if (safe == selectedIndex.value.coerceIn(0, items.lastIndex)) {
                    items[safe].onClick()
                } else {
                    selectedIndex.value = safe
                    onSelect(safe)
                }
            }
        }
    }

    private fun moveSelection(delta: Int) {
        if (items.isEmpty()) return
        val next = (selectedIndex.value + delta).coerceIn(0, items.lastIndex)
        if (next != selectedIndex.value) {
            selectedIndex.value = next
            onSelect(next)
        }
    }

    private fun actionAt(localX: Float, localY: Float): Int {
        val viewportWidth = measuredWidth.coerceAtLeast(width)
        val viewportHeight = measuredHeight.coerceAtLeast(height)
        val cardWidth = (viewportWidth * 0.68f).coerceIn(180f, 360f)
        val cardHeight = (viewportHeight - if (showIndicators) 44f else 20f).coerceAtLeast(132f)
        val cardTop = 8f
        val centerX = (viewportWidth - cardWidth) / 2f
        val previewWidth = (cardWidth * 0.22f).coerceAtLeast(28f)
        val arrowSize = 32f
        val leftArrowX = centerX - previewWidth - 12f
        val arrowY = cardTop + (cardHeight - arrowSize) / 2f
        if (items.size > 1 && localX in leftArrowX..(leftArrowX + arrowSize) && localY in arrowY..(arrowY + arrowSize)) return -1
        val rightArrowX = centerX + cardWidth + previewWidth - 20f
        if (items.size > 1 && localX in rightArrowX..(rightArrowX + arrowSize) && localY in arrowY..(arrowY + arrowSize)) return -2
        val active = selectedIndex.value.coerceIn(0, items.lastIndex)
        val candidates = listOf(active - 1, active, active + 1).filter { it in items.indices }
        candidates.forEach { index ->
            val offset = index - displayIndex
            if (abs(offset) > 1.15f) return@forEach
            val absOffset = abs(offset)
            val scale = 1f - 0.12f * absOffset.coerceAtMost(1f)
            val drawWidth = cardWidth * scale
            val drawHeight = cardHeight * scale
            val drawX = centerX + offset * (cardWidth * 0.84f) + (cardWidth - drawWidth) / 2f
            val drawY = cardTop + 8f * absOffset
            if (localX in drawX..(drawX + drawWidth) && localY in drawY..(drawY + drawHeight)) return index
        }
        return Int.MIN_VALUE
    }
}

private fun drawCarouselArrow(
    context: RenderContext,
    x: Float,
    y: Float,
    size: Float,
    label: String,
    hovered: Boolean,
    pressed: Boolean
) {
    val p = context.theme.palette
    if (hovered || pressed) {
        context.backend.drawRoundedRect(x, y, size, size, MaterialShapes.Full, overlay(p.surfaceContainerHigh, p.onSurface, if (pressed) 0.10f else 0.06f, 1f), null)
    }
    val style = TextStyle(context.theme.typography.bodySize + 2f, p.onSurface, context.theme.typography.fontFamily)
    val textWidth = context.backend.measureText(label, style).first
    context.backend.drawText(label, x + (size - textWidth) / 2f, y + textBaseline(size, style.size), style)
}

enum class MaterialListDensity(val rowHeight: Float) {
    Compact(48f),
    Default(56f),
    Comfortable(72f)
}

class MaterialListSubheader(
    var text: String,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 240f,
    height: Float = 32f,
    var inset: Float = 0f
) : Component(x, y, width, height) {
    init {
        minSize(120f, 32f)
    }

    override fun draw(context: RenderContext) {
        val style = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), context.theme.palette.primary, context.theme.typography.fontFamily)
        context.backend.translated(x, y) {
            context.backend.drawText(text.takeFitting(context, style, (measuredWidth.coerceAtLeast(width) - inset).coerceAtLeast(24f)), inset, textBaseline(measuredHeight, style.size), style)
        }
    }
}

class MaterialListItem(
    var headline: String,
    var supportingText: String = "",
    var leadingLabel: String = "",
    var trailingLabel: String = "",
    val selectedState: State<Boolean>? = null,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 280f,
    height: Float = 56f,
    var density: MaterialListDensity = MaterialListDensity.Default,
    var showDivider: Boolean = false,
    private val onClick: () -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }

    init {
        focusable = true
        minSize(160f, 56f)
        selectedState?.subscribe(invalidate)
        onDispose { selectedState?.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!disabled) {
                setInteraction { copy(pressed = true) }
                requestFocus()
                it.consume()
            }
        }
        on(PointerUp) {
            if (interaction.pressed && !disabled) {
                selectedState?.let { state -> state.value = !state.value }
                onClick()
            }
            setInteraction { copy(pressed = false) }
            if (!disabled) it.consume()
        }
        on(KeyDown.Enter or KeyDown.Space) {
            if (!disabled) {
                selectedState?.let { state -> state.value = !state.value }
                onClick()
                it.consume()
            }
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        val selected = selectedState?.value == true
        val active = interaction.hovered || interaction.pressed || selected
        backgroundColor = if (active) {
            if (selected) p.secondaryContainer else overlay(p.surfaceContainerHigh, p.onSurface, if (interaction.pressed) 0.10f else 0.06f, 1f)
        } else {
            Color(0, 0, 0, 0)
        }
        cornerRadius = MaterialShapes.Medium
        borderColor = null
        borderWidth = 0f
        super.draw(context)
        context.backend.translated(x, y) {
            val rowHeight = maxOf(measuredHeight, density.rowHeight)
            val headlineStyle = TextStyle(context.theme.typography.bodySize, if (selected) p.onSecondaryContainer else p.onSurface, context.theme.typography.fontFamily)
            val supportingStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), if (selected) p.onSecondaryContainer.withAlpha(220) else p.onSurfaceVariant, context.theme.typography.fontFamily)
            val trailingStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), if (selected) p.onSecondaryContainer else p.onSurfaceVariant, context.theme.typography.fontFamily)
            val leadingOffset = if (leadingLabel.isBlank()) 0f else 40f
            if (leadingLabel.isNotBlank()) {
                context.backend.drawRoundedRect(12f, (rowHeight - 24f) / 2f, 24f, 24f, 12f, if (selected) p.primary else p.surfaceContainerHighest, null)
                val leadingStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), if (selected) p.onPrimary else p.onSurfaceVariant, context.theme.typography.fontFamily)
                val leadingWidth = context.backend.measureText(leadingLabel, leadingStyle).first
                context.backend.drawText(leadingLabel, 24f - leadingWidth / 2f, textBaseline(rowHeight, leadingStyle.size), leadingStyle)
            }
            val trailingWidth = if (trailingLabel.isBlank()) 0f else context.backend.measureText(trailingLabel, trailingStyle).first + 12f
            val contentWidth = (measuredWidth - 24f - leadingOffset - trailingWidth).coerceAtLeast(48f)
            val textX = 12f + leadingOffset
            val titleY = when {
                supportingText.isBlank() -> textBaseline(rowHeight, headlineStyle.size)
                density == MaterialListDensity.Compact -> 20f
                density == MaterialListDensity.Comfortable -> 28f
                else -> 24f
            }
            context.backend.drawText(headline.takeFitting(context, headlineStyle, contentWidth), textX, titleY, headlineStyle)
            if (supportingText.isNotBlank()) {
                val supportingY = when (density) {
                    MaterialListDensity.Compact -> 38f
                    MaterialListDensity.Default -> 46f
                    MaterialListDensity.Comfortable -> 54f
                }
                context.backend.drawText(supportingText.takeFitting(context, supportingStyle, contentWidth), textX, supportingY, supportingStyle)
            }
            if (trailingLabel.isNotBlank()) {
                val labelWidth = context.backend.measureText(trailingLabel, trailingStyle).first
                context.backend.drawText(trailingLabel, measuredWidth - 12f - labelWidth, if (supportingText.isBlank()) textBaseline(rowHeight, trailingStyle.size) else titleY, trailingStyle)
            }
            if (showDivider) {
                context.backend.drawLine(12f + leadingOffset, rowHeight - 1f, measuredWidth - 12f, rowHeight - 1f, Stroke(p.outlineVariant, 1f))
            }
        }
    }
}

class MaterialCheckboxListTile(
    var headline: String,
    val checked: State<Boolean> = stateOf(false),
    var supportingText: String = "",
    var leadingLabel: String = "",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 280f,
    height: Float = 56f,
    var density: MaterialListDensity = MaterialListDensity.Default,
    var showDivider: Boolean = false,
    private val onToggle: (Boolean) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }

    init {
        focusable = true
        minSize(160f, 56f)
        checked.subscribe(invalidate)
        onDispose { checked.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!disabled) {
                setInteraction { copy(pressed = true) }
                requestFocus()
                it.consume()
            }
        }
        on(PointerUp) {
            if (interaction.pressed && !disabled) toggle()
            setInteraction { copy(pressed = false) }
            if (!disabled) it.consume()
        }
        on(KeyDown.Enter or KeyDown.Space) {
            if (!disabled) {
                toggle()
                it.consume()
            }
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        val rowHeight = maxOf(measuredHeight, density.rowHeight)
        val active = interaction.hovered || interaction.pressed || checked.value
        backgroundColor = if (active) {
            if (checked.value) p.secondaryContainer else overlay(p.surfaceContainerHigh, p.onSurface, if (interaction.pressed) 0.10f else 0.06f, 1f)
        } else {
            Color(0, 0, 0, 0)
        }
        cornerRadius = MaterialShapes.Medium
        borderColor = null
        borderWidth = 0f
        super.draw(context)
        context.backend.translated(x, y) {
            val headlineColor = if (checked.value) p.onSecondaryContainer else p.onSurface
            val supportingColor = if (checked.value) p.onSecondaryContainer.withAlpha(220) else p.onSurfaceVariant
            val headlineStyle = TextStyle(context.theme.typography.bodySize, headlineColor, context.theme.typography.fontFamily)
            val supportingStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), supportingColor, context.theme.typography.fontFamily)
            val leadingOffset = if (leadingLabel.isBlank()) 0f else 40f
            if (leadingLabel.isNotBlank()) {
                context.backend.drawRoundedRect(12f, (rowHeight - 24f) / 2f, 24f, 24f, 12f, if (checked.value) p.primary else p.surfaceContainerHighest, null)
                val leadingStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), if (checked.value) p.onPrimary else p.onSurfaceVariant, context.theme.typography.fontFamily)
                val leadingWidth = context.backend.measureText(leadingLabel, leadingStyle).first
                context.backend.drawText(leadingLabel, 24f - leadingWidth / 2f, textBaseline(rowHeight, leadingStyle.size), leadingStyle)
            }
            val textX = 12f + leadingOffset
            val checkboxX = measuredWidth - 30f
            val contentWidth = (checkboxX - textX - 12f).coerceAtLeast(48f)
            val titleY = when {
                supportingText.isBlank() -> textBaseline(rowHeight, headlineStyle.size)
                density == MaterialListDensity.Compact -> 20f
                density == MaterialListDensity.Comfortable -> 28f
                else -> 24f
            }
            context.backend.drawText(headline.takeFitting(context, headlineStyle, contentWidth), textX, titleY, headlineStyle)
            if (supportingText.isNotBlank()) {
                val supportingY = when (density) {
                    MaterialListDensity.Compact -> 38f
                    MaterialListDensity.Default -> 46f
                    MaterialListDensity.Comfortable -> 54f
                }
                context.backend.drawText(supportingText.takeFitting(context, supportingStyle, contentWidth), textX, supportingY, supportingStyle)
            }
            val boxSize = 18f
            val boxY = (rowHeight - boxSize) / 2f
            val boxColor = when {
                disabled && checked.value -> p.onSurface.withAlpha(40)
                checked.value -> p.primary
                else -> Color(0, 0, 0, 0)
            }
            val strokeColor = when {
                checked.value -> p.primary
                interaction.focused -> p.primary
                else -> p.outline
            }
            context.backend.drawRoundedRect(checkboxX, boxY, boxSize, boxSize, 2f, boxColor, Stroke(strokeColor.withAlpha(if (disabled) 90 else 255), 2f))
            if (checked.value) {
                val mark = if (disabled) p.onSurface.withAlpha(150) else p.onPrimary
                context.backend.drawLine(checkboxX + 4f, boxY + 9f, checkboxX + 8f, boxY + 13f, Stroke(mark, 2f))
                context.backend.drawLine(checkboxX + 8f, boxY + 13f, checkboxX + 14f, boxY + 5f, Stroke(mark, 2f))
            }
            if (showDivider) {
                context.backend.drawLine(12f + leadingOffset, rowHeight - 1f, measuredWidth - 12f, rowHeight - 1f, Stroke(p.outlineVariant, 1f))
            }
        }
    }

    private fun toggle() {
        checked.value = !checked.value
        onToggle(checked.value)
    }
}

class MaterialRadioListTile<T>(
    var headline: String,
    val selection: State<T>,
    val option: T,
    var supportingText: String = "",
    var leadingLabel: String = "",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 280f,
    height: Float = 56f,
    var density: MaterialListDensity = MaterialListDensity.Default,
    var showDivider: Boolean = false,
    private val onSelect: (T) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }

    init {
        focusable = true
        minSize(160f, 56f)
        selection.subscribe(invalidate)
        onDispose { selection.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!disabled) {
                setInteraction { copy(pressed = true) }
                requestFocus()
                it.consume()
            }
        }
        on(PointerUp) {
            if (interaction.pressed && !disabled) select()
            setInteraction { copy(pressed = false) }
            if (!disabled) it.consume()
        }
        on(KeyDown.Enter or KeyDown.Space) {
            if (!disabled) {
                select()
                it.consume()
            }
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        val rowHeight = maxOf(measuredHeight, density.rowHeight)
        val selected = selection.value == option
        val active = interaction.hovered || interaction.pressed || selected
        backgroundColor = if (active) {
            if (selected) p.secondaryContainer else overlay(p.surfaceContainerHigh, p.onSurface, if (interaction.pressed) 0.10f else 0.06f, 1f)
        } else {
            Color(0, 0, 0, 0)
        }
        cornerRadius = MaterialShapes.Medium
        borderColor = null
        borderWidth = 0f
        super.draw(context)
        context.backend.translated(x, y) {
            val headlineColor = if (selected) p.onSecondaryContainer else p.onSurface
            val supportingColor = if (selected) p.onSecondaryContainer.withAlpha(220) else p.onSurfaceVariant
            val headlineStyle = TextStyle(context.theme.typography.bodySize, headlineColor, context.theme.typography.fontFamily)
            val supportingStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), supportingColor, context.theme.typography.fontFamily)
            val leadingOffset = if (leadingLabel.isBlank()) 0f else 40f
            if (leadingLabel.isNotBlank()) {
                context.backend.drawRoundedRect(12f, (rowHeight - 24f) / 2f, 24f, 24f, 12f, if (selected) p.primary else p.surfaceContainerHighest, null)
                val leadingStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), if (selected) p.onPrimary else p.onSurfaceVariant, context.theme.typography.fontFamily)
                val leadingWidth = context.backend.measureText(leadingLabel, leadingStyle).first
                context.backend.drawText(leadingLabel, 24f - leadingWidth / 2f, textBaseline(rowHeight, leadingStyle.size), leadingStyle)
            }
            val textX = 12f + leadingOffset
            val radioX = measuredWidth - 21f
            val contentWidth = (radioX - textX - 14f).coerceAtLeast(48f)
            val titleY = when {
                supportingText.isBlank() -> textBaseline(rowHeight, headlineStyle.size)
                density == MaterialListDensity.Compact -> 20f
                density == MaterialListDensity.Comfortable -> 28f
                else -> 24f
            }
            context.backend.drawText(headline.takeFitting(context, headlineStyle, contentWidth), textX, titleY, headlineStyle)
            if (supportingText.isNotBlank()) {
                val supportingY = when (density) {
                    MaterialListDensity.Compact -> 38f
                    MaterialListDensity.Default -> 46f
                    MaterialListDensity.Comfortable -> 54f
                }
                context.backend.drawText(supportingText.takeFitting(context, supportingStyle, contentWidth), textX, supportingY, supportingStyle)
            }
            val centerY = rowHeight / 2f
            val ringColor = when {
                disabled -> p.onSurface.withAlpha(90)
                selected || interaction.focused -> p.primary
                else -> p.outline
            }
            context.backend.drawCircle(radioX, centerY, 9f, Color(0, 0, 0, 0), Stroke(ringColor, 2f))
            if (selected) {
                context.backend.drawCircle(radioX, centerY, 4.5f, if (disabled) p.onSurface.withAlpha(110) else p.primary)
            }
            if (showDivider) {
                context.backend.drawLine(12f + leadingOffset, rowHeight - 1f, measuredWidth - 12f, rowHeight - 1f, Stroke(p.outlineVariant, 1f))
            }
        }
    }

    private fun select() {
        selection.value = option
        onSelect(option)
    }
}

class MaterialSwitchListTile(
    var headline: String,
    val checked: State<Boolean> = stateOf(false),
    var supportingText: String = "",
    var leadingLabel: String = "",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 280f,
    height: Float = 56f,
    var density: MaterialListDensity = MaterialListDensity.Default,
    var showDivider: Boolean = false,
    private val onToggle: (Boolean) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var progress = if (checked.value) 1f else 0f

    init {
        focusable = true
        minSize(160f, 56f)
        checked.subscribe(invalidate)
        onDispose { checked.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!disabled) {
                setInteraction { copy(pressed = true) }
                requestFocus()
                it.consume()
            }
        }
        on(PointerUp) {
            if (interaction.pressed && !disabled) toggle()
            setInteraction { copy(pressed = false) }
            if (!disabled) it.consume()
        }
        on(KeyDown.Enter or KeyDown.Space) {
            if (!disabled) {
                toggle()
                it.consume()
            }
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        val rowHeight = maxOf(measuredHeight, density.rowHeight)
        val active = interaction.hovered || interaction.pressed || checked.value
        progress = animate(progress, if (checked.value) 1f else 0f, context.time.deltaSeconds, 18f)
        backgroundColor = if (active) {
            if (checked.value) p.secondaryContainer else overlay(p.surfaceContainerHigh, p.onSurface, if (interaction.pressed) 0.10f else 0.06f, 1f)
        } else {
            Color(0, 0, 0, 0)
        }
        cornerRadius = MaterialShapes.Medium
        borderColor = null
        borderWidth = 0f
        super.draw(context)
        context.backend.translated(x, y) {
            val headlineColor = if (checked.value) p.onSecondaryContainer else p.onSurface
            val supportingColor = if (checked.value) p.onSecondaryContainer.withAlpha(220) else p.onSurfaceVariant
            val headlineStyle = TextStyle(context.theme.typography.bodySize, headlineColor, context.theme.typography.fontFamily)
            val supportingStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), supportingColor, context.theme.typography.fontFamily)
            val leadingOffset = if (leadingLabel.isBlank()) 0f else 40f
            if (leadingLabel.isNotBlank()) {
                context.backend.drawRoundedRect(12f, (rowHeight - 24f) / 2f, 24f, 24f, 12f, if (checked.value) p.primary else p.surfaceContainerHighest, null)
                val leadingStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), if (checked.value) p.onPrimary else p.onSurfaceVariant, context.theme.typography.fontFamily)
                val leadingWidth = context.backend.measureText(leadingLabel, leadingStyle).first
                context.backend.drawText(leadingLabel, 24f - leadingWidth / 2f, textBaseline(rowHeight, leadingStyle.size), leadingStyle)
            }
            val textX = 12f + leadingOffset
            val switchWidth = 52f
            val switchX = measuredWidth - switchWidth - 12f
            val contentWidth = (switchX - textX - 12f).coerceAtLeast(48f)
            val titleY = when {
                supportingText.isBlank() -> textBaseline(rowHeight, headlineStyle.size)
                density == MaterialListDensity.Compact -> 20f
                density == MaterialListDensity.Comfortable -> 28f
                else -> 24f
            }
            context.backend.drawText(headline.takeFitting(context, headlineStyle, contentWidth), textX, titleY, headlineStyle)
            if (supportingText.isNotBlank()) {
                val supportingY = when (density) {
                    MaterialListDensity.Compact -> 38f
                    MaterialListDensity.Default -> 46f
                    MaterialListDensity.Comfortable -> 54f
                }
                context.backend.drawText(supportingText.takeFitting(context, supportingStyle, contentWidth), textX, supportingY, supportingStyle)
            }
            val track = if (disabled) p.onSurface.withAlpha(31) else lerp(p.surfaceContainerHighest, p.primary, progress)
            val stroke = if (!checked.value) Stroke(p.outline, 2f) else null
            val thumbRadius = 8f + progress * 4f
            val thumbX = switchX + 16f + progress * (switchWidth - 32f)
            val thumb = if (disabled) p.onSurface.withAlpha(97) else lerp(p.outline, p.onPrimary, progress)
            context.backend.drawRoundedRect(switchX, (rowHeight - 32f) / 2f, switchWidth, 32f, 16f, track, stroke)
            context.backend.drawCircle(thumbX, rowHeight / 2f, thumbRadius, thumb)
            if (showDivider) {
                context.backend.drawLine(12f + leadingOffset, rowHeight - 1f, measuredWidth - 12f, rowHeight - 1f, Stroke(p.outlineVariant, 1f))
            }
        }
        if (abs(progress - if (checked.value) 1f else 0f) > 0.001f) requestRender()
    }

    private fun toggle() {
        checked.value = !checked.value
        onToggle(checked.value)
    }
}

class MaterialList(
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 280f,
    height: Float = 0f,
    var tonal: Boolean = false,
    var inset: Float = 0f,
    var paddingVertical: Float = 8f,
    var density: MaterialListDensity = MaterialListDensity.Default
) : Component(x, y, width, height) {
    init {
        clipToBounds = false
        minSize(160f, 0f)
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        backgroundColor = if (tonal) p.surfaceContainer else null
        cornerRadius = if (tonal) MaterialShapes.Large else 0f
        borderColor = null
        borderWidth = 0f
        super.draw(context)
        var cursorY = paddingVertical
        children.forEach { child ->
            child.x = inset
            child.width = (measuredWidth - inset * 2f).coerceAtLeast(48f)
            when (child) {
                is MaterialListItem -> {
                    child.density = density
                    child.y = cursorY
                    child.height = child.density.rowHeight
                    cursorY += child.height
                }
                is MaterialCheckboxListTile -> {
                    child.density = density
                    child.y = cursorY
                    child.height = child.density.rowHeight
                    cursorY += child.height
                }
                is MaterialRadioListTile<*> -> {
                    child.density = density
                    child.y = cursorY
                    child.height = child.density.rowHeight
                    cursorY += child.height
                }
                is MaterialSwitchListTile -> {
                    child.density = density
                    child.y = cursorY
                    child.height = child.density.rowHeight
                    cursorY += child.height
                }
                is MaterialListSubheader -> {
                    child.inset = 12f
                    child.y = cursorY
                    child.height = maxOf(child.height, 32f)
                    cursorY += child.height
                }
                else -> {
                    child.y = cursorY
                    cursorY += maxOf(child.height, child.measuredHeight)
                }
            }
        }
        if (height <= 0f) {
            this.height = cursorY + paddingVertical
        }
    }
}

class MaterialSegmentedButtonRow(
    val segments: List<String>,
    val selectedIndex: State<Int> = stateOf(0),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 280f,
    height: Float = 40f,
    private val onSelect: (Int) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var hoverIndex = -1
    private var pressedIndex = -1

    init {
        focusable = true
        minSize(160f, 40f)
        selectedIndex.subscribe(invalidate)
        onDispose { selectedIndex.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!disabled) {
                requestFocus()
                pressedIndex = segmentIndexAt(it.x - x)
                if (pressedIndex >= 0) it.consume()
            }
        }
        on(PointerMove) {
            hoverIndex = segmentIndexAt(it.x - x)
            requestRender()
        }
        on(PointerUp) {
            if (!disabled) {
                val releasedIndex = segmentIndexAt(it.x - x)
                if (releasedIndex >= 0 && releasedIndex == pressedIndex) {
                    selectedIndex.value = releasedIndex
                    onSelect(releasedIndex)
                }
                if (pressedIndex >= 0 || releasedIndex >= 0) it.consume()
            }
            pressedIndex = -1
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        backgroundColor = Color(0, 0, 0, 0)
        cornerRadius = measuredHeight / 2f
        borderColor = p.outline
        borderWidth = 1f
        super.draw(context)
        if (segments.isEmpty()) return
        val segmentWidth = measuredWidth / segments.size
        context.backend.translated(x, y) {
            segments.forEachIndexed { index, label ->
                val selected = selectedIndex.value == index
                val hovered = hoverIndex == index || pressedIndex == index
                val left = index * segmentWidth
                if (selected || hovered) {
                    context.backend.drawRoundedRect(
                        left + 2f,
                        2f,
                        segmentWidth - 4f,
                        measuredHeight - 4f,
                        (measuredHeight - 4f) / 2f,
                        if (selected) p.secondaryContainer else overlay(p.surfaceContainerHigh, p.onSurface, if (pressedIndex == index) 0.10f else 0.06f, 1f),
                        null
                    )
                }
                if (index > 0) {
                    context.backend.drawLine(left, 8f, left, measuredHeight - 8f, Stroke(p.outlineVariant, 1f))
                }
                val style = TextStyle(context.theme.typography.bodySize, if (selected) p.onSecondaryContainer else p.onSurface, context.theme.typography.fontFamily)
                val textWidth = context.backend.measureText(label, style).first
                context.backend.drawText(label.takeFitting(context, style, segmentWidth - 20f), left + (segmentWidth - textWidth) / 2f, textBaseline(measuredHeight, style.size), style)
            }
        }
    }

    private fun segmentIndexAt(localX: Float): Int {
        if (segments.isEmpty() || localX < 0f || localX > measuredWidth) return -1
        val segmentWidth = measuredWidth / segments.size
        return (localX / segmentWidth).toInt().coerceIn(0, segments.lastIndex)
    }
}

class MaterialSearchBar(
    val query: State<String> = stateOf(""),
    val activeState: State<Boolean> = stateOf(false),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 280f,
    height: Float = 56f,
    var placeholder: String = "Search",
    private val onSearch: (String) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var hover = 0f
    private var cursor = query.value.length

    init {
        focusable = true
        minSize(180f, 56f)
        query.subscribe(invalidate)
        activeState.subscribe(invalidate)
        onDispose {
            query.unsubscribe(invalidate)
            activeState.unsubscribe(invalidate)
        }
        on(PointerDown) {
            if (!disabled) {
                requestFocus()
                activeState.value = true
                if (query.value.isNotBlank() && it.x - x >= measuredWidth - 44f) {
                    query.value = ""
                    cursor = 0
                } else {
                    cursor = query.value.length
                }
                it.consume()
            }
        }
        on(TextInput) {
            if (disabled) return@on
            val safeCursor = cursor.coerceIn(0, query.value.length)
            query.value = query.value.substring(0, safeCursor) + it.text + query.value.substring(safeCursor)
            cursor = safeCursor + it.text.length
            it.consume()
        }
        on(KeyDown) {
            if (disabled) return@on
            when (it.keyCode) {
                AwtKeyEvent.VK_BACK_SPACE -> {
                    if (cursor > 0 && query.value.isNotEmpty()) {
                        query.value = query.value.removeRange(cursor - 1, cursor)
                        cursor -= 1
                    }
                    it.consume()
                }
                AwtKeyEvent.VK_DELETE -> {
                    if (cursor < query.value.length) {
                        query.value = query.value.removeRange(cursor, cursor + 1)
                    }
                    it.consume()
                }
                AwtKeyEvent.VK_LEFT -> {
                    cursor = (cursor - 1).coerceAtLeast(0)
                    it.consume()
                }
                AwtKeyEvent.VK_RIGHT -> {
                    cursor = (cursor + 1).coerceAtMost(query.value.length)
                    it.consume()
                }
                AwtKeyEvent.VK_ESCAPE -> {
                    activeState.value = false
                    it.consume()
                }
                AwtKeyEvent.VK_ENTER -> {
                    onSearch(query.value)
                    it.consume()
                }
            }
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        hover = animate(hover, if (interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        val active = activeState.value || interaction.focused
        backgroundColor = overlay(p.surfaceContainerHigh, p.onSurface, if (active) 0.10f else hover * 0.06f, if (disabled) 0.38f else 1f)
        cornerRadius = measuredHeight / 2f
        borderColor = if (active) p.outline else null
        borderWidth = if (active) 1f else 0f
        elevationShadow = if (active) Shadow(Color(0, 0, 0, 40), 4f, offsetY = 1f) else null
        super.draw(context)
        context.backend.translated(x, y) {
            val iconStyle = TextStyle(context.theme.typography.bodySize, p.onSurfaceVariant.withAlpha(if (disabled) 97 else 255), context.theme.typography.fontFamily)
            context.backend.drawText("⌕", 16f, textBaseline(measuredHeight, iconStyle.size), iconStyle)
            val clearWidth = if (query.value.isBlank()) 0f else 24f
            val textStyle = TextStyle(context.theme.typography.bodySize, if (query.value.isBlank()) p.onSurfaceVariant else p.onSurface, context.theme.typography.fontFamily)
            val textX = 40f
            val availableWidth = (measuredWidth - textX - clearWidth - 16f).coerceAtLeast(48f)
            val display = if (query.value.isBlank()) placeholder else query.value
            context.backend.drawText(display.takeFitting(context, textStyle, availableWidth), textX, textBaseline(measuredHeight, textStyle.size), textStyle)
            if (query.value.isNotBlank()) {
                context.backend.drawText("×", measuredWidth - 24f, textBaseline(measuredHeight, iconStyle.size), iconStyle)
            }
        }
        if (hover in 0.001f..0.999f) requestRender()
    }
}

class MaterialBadge(
    var text: String = "",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 20f,
    height: Float = 20f
) : Component(x, y, width, height) {
    init {
        minSize(6f, 6f)
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        val dot = text.isBlank()
        val badgeWidth = if (dot) 6f else measuredWidth.coerceAtLeast(16f)
        val badgeHeight = if (dot) 6f else measuredHeight.coerceAtLeast(16f)
        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(0f, 0f, badgeWidth, badgeHeight, badgeHeight / 2f, p.error, null)
            if (!dot) {
                val style = TextStyle((context.theme.typography.bodySize - 3f).coerceAtLeast(9f), p.onError, context.theme.typography.fontFamily)
                val display = text.take(3)
                val textWidth = context.backend.measureText(display, style).first
                context.backend.drawText(display, (badgeWidth - textWidth) / 2f, textBaseline(badgeHeight, style.size), style)
            }
        }
    }
}

data class MaterialDrawerItem(
    val label: String,
    val badgeText: String = "",
    val supportingText: String = "",
    val enabled: Boolean = true,
    val onClick: () -> Unit = {}
)

enum class MaterialNavigationDrawerStyle { Modal, Dismissible, Permanent }

open class MaterialNavigationDrawer(
    val visibleState: State<Boolean> = stateOf(false),
    val items: List<MaterialDrawerItem>,
    val selectedIndex: State<Int> = stateOf(0),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 240f,
    var title: String = "",
    var drawerWidth: Float = 280f,
    var modal: Boolean = true,
    var drawerStyle: MaterialNavigationDrawerStyle = if (modal) MaterialNavigationDrawerStyle.Modal else MaterialNavigationDrawerStyle.Dismissible,
    var dismissOnOutsideClick: Boolean = true,
    private val onDismiss: () -> Unit = {},
    private val onSelect: (Int) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = {
        visible = visibleState.value
        requestRender()
    }
    private var hoverIndex = Int.MIN_VALUE
    private var pressedIndex = Int.MIN_VALUE

    init {
        focusable = true
        visible = visibleState.value
        minSize(200f, 160f)
        visibleState.subscribe(invalidate)
        selectedIndex.subscribe(invalidate)
        onDispose {
            visibleState.unsubscribe(invalidate)
            selectedIndex.unsubscribe(invalidate)
        }
        on(PointerDown) {
            if (!visible) return@on
            requestFocus()
            pressedIndex = itemIndexAt(it.x - x, it.y - y)
            it.consume()
        }
        on(PointerMove) {
            if (!visible) return@on
            hoverIndex = itemIndexAt(it.x - x, it.y - y)
            requestRender()
        }
        on(PointerUp) {
            if (!visible) return@on
            val releasedIndex = itemIndexAt(it.x - x, it.y - y)
            when {
                releasedIndex >= 0 && releasedIndex == pressedIndex -> {
                    items.getOrNull(releasedIndex)?.takeIf { item -> item.enabled }?.let { item ->
                        selectedIndex.value = releasedIndex
                        item.onClick()
                        onSelect(releasedIndex)
                    }
                    if (modal) visibleState.value = false
                }
                releasedIndex == -2 && pressedIndex == -2 && dismissOnOutsideClick -> dismiss()
            }
            pressedIndex = Int.MIN_VALUE
            it.consume()
        }
        onKey("keyDown") {
            if (!visible) return@onKey
            if (it.keyCode == AwtKeyEvent.VK_ESCAPE) {
                dismiss()
                it.consume()
            }
        }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        val p = context.theme.palette
        val bounds = drawerBounds()
        val titleStyle = TextStyle(context.theme.typography.titleSize + 1f, p.onSurface, context.theme.typography.fontFamily)
        context.backend.translated(x, y) {
            if (drawerStyle == MaterialNavigationDrawerStyle.Modal) {
                context.backend.drawRect(0f, 0f, measuredWidth, measuredHeight, p.scrim.withAlpha(132), null)
            }
            val fill = if (drawerStyle == MaterialNavigationDrawerStyle.Dismissible) p.surfaceContainer else p.surfaceContainer
            val radius = if (drawerStyle == MaterialNavigationDrawerStyle.Dismissible) MaterialShapes.Large else MaterialShapes.ExtraLarge
            context.backend.drawRoundedRect(bounds.x, bounds.y, bounds.width, bounds.height, radius, fill, Stroke(p.outlineVariant, 1f))
            if (title.isNotBlank()) {
                context.backend.drawText(title.takeFitting(context, titleStyle, bounds.width - 32f), bounds.x + 16f, bounds.y + 28f, titleStyle)
            }
            items.forEachIndexed { index, item ->
                val top = bounds.y + if (title.isBlank()) 16f else 56f + index * 56f
                val selected = selectedIndex.value == index
                val hovered = hoverIndex == index || pressedIndex == index
                val rowHeight = if (item.supportingText.isBlank()) 36f else 44f
                if (selected || hovered) {
                    context.backend.drawRoundedRect(bounds.x + 12f, top, bounds.width - 24f, rowHeight, 18f, if (selected) p.secondaryContainer else overlay(fill, p.onSurface, if (pressedIndex == index) 0.10f else 0.06f, 1f), null)
                }
                val color = when {
                    !item.enabled -> p.onSurface.withAlpha(97)
                    selected -> p.onSecondaryContainer
                    else -> p.onSurface
                }
                val style = TextStyle(context.theme.typography.bodySize, color, context.theme.typography.fontFamily)
                val supportStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), if (selected) p.onSecondaryContainer.withAlpha(210) else p.onSurfaceVariant, context.theme.typography.fontFamily)
                val badgeWidth = if (item.badgeText.isBlank()) 0f else 28f
                context.backend.drawText(item.label.takeFitting(context, style, bounds.width - 48f - badgeWidth), bounds.x + 24f, top + if (item.supportingText.isBlank()) textBaseline(36f, style.size) else 18f, style)
                if (item.supportingText.isNotBlank()) {
                    context.backend.drawText(item.supportingText.takeFitting(context, supportStyle, bounds.width - 48f - badgeWidth), bounds.x + 24f, top + 34f, supportStyle)
                }
                if (item.badgeText.isNotBlank()) {
                    val badgeX = bounds.x + bounds.width - 20f - badgeWidth
                    context.backend.drawRoundedRect(badgeX, top + if (item.supportingText.isBlank()) 8f else 12f, badgeWidth, 20f, 10f, p.errorContainer, null)
                    val badgeStyle = TextStyle((context.theme.typography.bodySize - 3f).coerceAtLeast(9f), p.onErrorContainer, context.theme.typography.fontFamily)
                    val badgeTextWidth = context.backend.measureText(item.badgeText.take(3), badgeStyle).first
                    context.backend.drawText(item.badgeText.take(3), badgeX + (badgeWidth - badgeTextWidth) / 2f, top + if (item.supportingText.isBlank()) 8f else 12f + textBaseline(20f, badgeStyle.size), badgeStyle)
                }
            }
        }
    }

    fun dismiss() {
        if (!visible) return
        visibleState.value = false
        onDismiss()
    }

    private fun drawerBounds(): DialogBounds {
        val panelWidth = drawerWidth.coerceAtLeast(200f).coerceAtMost((measuredWidth - 12f).coerceAtLeast(200f))
        return DialogBounds(0f, 0f, panelWidth, measuredHeight)
    }

    private fun itemIndexAt(localX: Float, localY: Float): Int {
        val bounds = drawerBounds()
        if (localX !in bounds.x..(bounds.x + bounds.width) || localY !in bounds.y..(bounds.y + bounds.height)) return -2
        items.indices.forEach { index ->
            val top = bounds.y + if (title.isBlank()) 16f else 56f + index * 48f
            if (localY in top..(top + 36f)) return index
        }
        return Int.MIN_VALUE
    }
}

class MaterialModalNavigationDrawer(
    visibleState: State<Boolean> = stateOf(false),
    items: List<MaterialDrawerItem>,
    selectedIndex: State<Int> = stateOf(0),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 240f,
    title: String = "",
    drawerWidth: Float = 280f,
    dismissOnOutsideClick: Boolean = true,
    onDismiss: () -> Unit = {},
    onSelect: (Int) -> Unit = {}
) : MaterialNavigationDrawer(
    visibleState = visibleState,
    items = items,
    selectedIndex = selectedIndex,
    x = x,
    y = y,
    width = width,
    height = height,
    title = title,
    drawerWidth = drawerWidth,
    modal = true,
    drawerStyle = MaterialNavigationDrawerStyle.Modal,
    dismissOnOutsideClick = dismissOnOutsideClick,
    onDismiss = onDismiss,
    onSelect = onSelect
)

class MaterialDismissibleNavigationDrawer(
    visibleState: State<Boolean> = stateOf(false),
    items: List<MaterialDrawerItem>,
    selectedIndex: State<Int> = stateOf(0),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 240f,
    title: String = "",
    drawerWidth: Float = 280f,
    dismissOnOutsideClick: Boolean = false,
    onDismiss: () -> Unit = {},
    onSelect: (Int) -> Unit = {}
) : MaterialNavigationDrawer(
    visibleState = visibleState,
    items = items,
    selectedIndex = selectedIndex,
    x = x,
    y = y,
    width = width,
    height = height,
    title = title,
    drawerWidth = drawerWidth,
    modal = false,
    drawerStyle = MaterialNavigationDrawerStyle.Dismissible,
    dismissOnOutsideClick = dismissOnOutsideClick,
    onDismiss = onDismiss,
    onSelect = onSelect
)

class MaterialPermanentNavigationDrawer(
    visibleState: State<Boolean> = stateOf(true),
    items: List<MaterialDrawerItem>,
    selectedIndex: State<Int> = stateOf(0),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 240f,
    title: String = "",
    drawerWidth: Float = 280f,
    onSelect: (Int) -> Unit = {}
) : MaterialNavigationDrawer(
    visibleState = visibleState,
    items = items,
    selectedIndex = selectedIndex,
    x = x,
    y = y,
    width = width,
    height = height,
    title = title,
    drawerWidth = drawerWidth,
    modal = false,
    drawerStyle = MaterialNavigationDrawerStyle.Permanent,
    dismissOnOutsideClick = false,
    onDismiss = {},
    onSelect = onSelect
)

data class MaterialCascadingMenuItem(
    val label: String,
    val children: List<MaterialCascadingMenuItem> = emptyList(),
    val enabled: Boolean = true,
    val destructive: Boolean = false,
    val leadingLabel: String = "",
    val trailingLabel: String = "",
    val supportingText: String = "",
    val keepOpen: Boolean = false,
    val onClick: () -> Unit = {}
)

class MaterialContextMenu(
    val expanded: State<Boolean> = stateOf(false),
    val items: List<MaterialMenuItem>,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 240f,
    var anchorX: Float = 0f,
    var anchorY: Float = 0f,
    var menuWidth: Float = 200f,
    var dismissOnOutsideClick: Boolean = true,
    var showDividers: Boolean = false
) : Component(x, y, width, height) {
    private val invalidate = {
        visible = expanded.value
        requestRender()
    }
    private var hoverIndex = Int.MIN_VALUE
    private var pressedIndex = Int.MIN_VALUE

    init {
        focusable = true
        visible = expanded.value
        minSize(120f, 120f)
        expanded.subscribe(invalidate)
        onDispose { expanded.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!visible) return@on
            requestFocus()
            val local = sceneToLocal(it.x, it.y)
            pressedIndex = itemIndexAt(local.first, local.second)
            it.consume()
        }
        on(PointerMove) {
            if (!visible) return@on
            val local = sceneToLocal(it.x, it.y)
            hoverIndex = itemIndexAt(local.first, local.second)
            requestRender()
        }
        on(PointerUp) {
            if (!visible) return@on
            val local = sceneToLocal(it.x, it.y)
            val releasedIndex = itemIndexAt(local.first, local.second)
            when {
                releasedIndex >= 0 && releasedIndex == pressedIndex -> {
                    items.getOrNull(releasedIndex)?.takeIf { menuItem -> menuItem.enabled && !menuItem.isSection && !menuItem.isDivider }?.onClick?.invoke()
                    if (items.getOrNull(releasedIndex)?.keepOpen != true) expanded.value = false
                }
                releasedIndex == -2 && pressedIndex == -2 && dismissOnOutsideClick -> expanded.value = false
            }
            pressedIndex = Int.MIN_VALUE
            it.consume()
        }
        onKey("keyDown") {
            if (!visible) return@onKey
            if (it.keyCode == AwtKeyEvent.VK_ESCAPE) {
                expanded.value = false
                it.consume()
            }
        }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        val p = context.theme.palette
        val bounds = menuBounds()
        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(bounds.x, bounds.y, bounds.width, bounds.height, MaterialShapes.Medium, p.surfaceContainerHigh, Stroke(p.outlineVariant, 1f))
            var cursorY = bounds.y + 8f
            items.forEachIndexed { index, item ->
                when {
                    item.isDivider -> {
                        context.backend.drawLine(bounds.x + 12f, cursorY + 7f, bounds.x + bounds.width - 12f, cursorY + 7f, Stroke(p.outlineVariant, 1f))
                        cursorY += 16f
                    }
                    item.isSection -> {
                        val sectionStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), p.primary, context.theme.typography.fontFamily)
                        context.backend.drawText(item.label.takeFitting(context, sectionStyle, bounds.width - 24f), bounds.x + 12f, cursorY + textBaseline(24f, sectionStyle.size), sectionStyle)
                        cursorY += 28f
                    }
                    else -> {
                        val rowHeight = if (item.supportingText.isBlank()) 40f else 56f
                        val top = cursorY
                        val active = hoverIndex == index || pressedIndex == index
                        if (active) {
                            context.backend.drawRoundedRect(bounds.x + 8f, top, bounds.width - 16f, rowHeight - 8f, MaterialShapes.Small, overlay(p.surfaceContainerHigh, p.onSurface, if (pressedIndex == index) 0.10f else 0.06f, 1f), null)
                        }
                        val color = when {
                            !item.enabled -> p.onSurface.withAlpha(97)
                            item.destructive -> p.error
                            else -> p.onSurface
                        }
                        val leadingOffset = if (item.leadingLabel.isBlank()) 0f else 28f
                        if (item.leadingLabel.isNotBlank()) {
                            val iconStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), color, context.theme.typography.fontFamily)
                            context.backend.drawText(item.leadingLabel, bounds.x + 16f, top + textBaseline(rowHeight - 8f, iconStyle.size), iconStyle)
                        }
                        if (item.trailingLabel.isNotBlank()) {
                            val trailingStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), if (item.enabled) p.onSurfaceVariant else p.onSurface.withAlpha(97), context.theme.typography.fontFamily)
                            val trailingWidth = context.backend.measureText(item.trailingLabel, trailingStyle).first
                            context.backend.drawText(item.trailingLabel, bounds.x + bounds.width - 16f - trailingWidth, top + textBaseline(rowHeight - 8f, trailingStyle.size), trailingStyle)
                        }
                        val trailingReserve = if (item.trailingLabel.isBlank()) 0f else 52f
                        val labelStyle = TextStyle(context.theme.typography.bodySize, color, context.theme.typography.fontFamily)
                        val contentWidth = (bounds.width - 32f - leadingOffset - trailingReserve).coerceAtLeast(48f)
                        val labelX = bounds.x + 16f + leadingOffset
                        val labelY = if (item.supportingText.isBlank()) top + textBaseline(rowHeight - 8f, labelStyle.size) else top + 20f
                        context.backend.drawText(item.label.takeFitting(context, labelStyle, contentWidth), labelX, labelY, labelStyle)
                        if (item.supportingText.isNotBlank()) {
                            val supportingStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), if (item.enabled) p.onSurfaceVariant else p.onSurface.withAlpha(97), context.theme.typography.fontFamily)
                            context.backend.drawText(item.supportingText.takeFitting(context, supportingStyle, contentWidth), labelX, top + 38f, supportingStyle)
                        }
                        cursorY += rowHeight
                        if (showDividers && index < items.lastIndex && !items[index + 1].isDivider && !items[index + 1].isSection) {
                            context.backend.drawLine(bounds.x + 12f, cursorY - 4f, bounds.x + bounds.width - 12f, cursorY - 4f, Stroke(p.outlineVariant, 1f))
                        }
                    }
                }
            }
        }
    }

    private fun menuBounds(): DialogBounds {
        val popupWidth = menuWidth.coerceAtLeast(120f).coerceAtMost((measuredWidth - anchorX - 8f).coerceAtLeast(120f))
        val popupHeight = contentHeight().coerceAtLeast(16f).coerceAtMost((measuredHeight - anchorY - 8f).coerceAtLeast(16f))
        val clampedX = anchorX.coerceIn(0f, (measuredWidth - popupWidth - 8f).coerceAtLeast(0f))
        val clampedY = anchorY.coerceIn(0f, (measuredHeight - popupHeight - 8f).coerceAtLeast(0f))
        return DialogBounds(clampedX, clampedY, popupWidth, popupHeight)
    }

    private fun contentHeight(): Float = items.fold(16f) { total, item ->
        total + when {
            item.isDivider -> 16f
            item.isSection -> 28f
            item.supportingText.isBlank() -> 40f
            else -> 56f
        }
    }

    private fun itemIndexAt(localX: Float, localY: Float): Int {
        val bounds = menuBounds()
        if (localX !in bounds.x..(bounds.x + bounds.width) || localY !in bounds.y..(bounds.y + bounds.height)) return -2
        var cursorY = bounds.y + 8f
        items.forEachIndexed { index, item ->
            val itemHeight = when {
                item.isDivider -> 16f
                item.isSection -> 28f
                item.supportingText.isBlank() -> 40f
                else -> 56f
            }
            if (localY in cursorY..(cursorY + itemHeight)) {
                return if (item.isDivider) Int.MIN_VALUE else index
            }
            cursorY += itemHeight
        }
        return Int.MIN_VALUE
    }
}

class MaterialCascadingMenu(
    val expanded: State<Boolean> = stateOf(false),
    val items: List<MaterialCascadingMenuItem>,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 240f,
    var menuX: Float = 0f,
    var menuY: Float = 0f,
    var menuWidth: Float = 220f,
    var dismissOnOutsideClick: Boolean = true,
    var showDividers: Boolean = false
) : Component(x, y, width, height) {
    private val invalidate = {
        visible = expanded.value
        requestRender()
    }
    private var hoverRoot = Int.MIN_VALUE
    private var pressedRoot = Int.MIN_VALUE
    private var hoverChild = Int.MIN_VALUE
    private var pressedChild = Int.MIN_VALUE
    private var expandedParent = Int.MIN_VALUE

    init {
        focusable = true
        visible = expanded.value
        minSize(160f, 120f)
        expanded.subscribe(invalidate)
        onDispose { expanded.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!visible) return@on
            requestFocus()
            val local = sceneToLocal(it.x, it.y)
            val rootIndex = rootIndexAt(local.first, local.second)
            val childIndex = childIndexAt(local.first, local.second)
            pressedRoot = rootIndex
            pressedChild = childIndex
            it.consume()
        }
        on(PointerMove) {
            if (!visible) return@on
            val (localX, localY) = sceneToLocal(it.x, it.y)
            hoverRoot = rootIndexAt(localX, localY)
            if (hoverRoot >= 0 && items.getOrNull(hoverRoot)?.children?.isNotEmpty() == true) {
                expandedParent = hoverRoot
            }
            hoverChild = childIndexAt(localX, localY)
            requestRender()
        }
        on(PointerUp) {
            if (!visible) return@on
            val (localX, localY) = sceneToLocal(it.x, it.y)
            val releasedRoot = rootIndexAt(localX, localY)
            val releasedChild = childIndexAt(localX, localY)
            when {
                releasedChild >= 0 && releasedChild == pressedChild -> {
                    val parent = items.getOrNull(expandedParent)
                    val child = parent?.children?.getOrNull(releasedChild)
                    child?.takeIf { menuItem -> menuItem.enabled }?.onClick?.invoke()
                    if (child?.keepOpen != true) expanded.value = false
                }
                releasedRoot >= 0 && releasedRoot == pressedRoot -> {
                    val item = items.getOrNull(releasedRoot)
                    when {
                        item == null -> Unit
                        item.children.isNotEmpty() -> expandedParent = releasedRoot
                        item.enabled -> {
                            item.onClick()
                            if (!item.keepOpen) expanded.value = false
                        }
                    }
                }
                releasedRoot == -2 && releasedChild == -2 && pressedRoot == -2 && pressedChild == -2 && dismissOnOutsideClick -> expanded.value = false
            }
            pressedRoot = Int.MIN_VALUE
            pressedChild = Int.MIN_VALUE
            it.consume()
        }
        onKey("keyDown") {
            if (!visible) return@onKey
            if (it.keyCode == AwtKeyEvent.VK_ESCAPE) {
                if (expandedParent >= 0) {
                    expandedParent = Int.MIN_VALUE
                } else {
                    expanded.value = false
                }
                it.consume()
            }
        }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        val p = context.theme.palette
        val rootBounds = rootBounds()
        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(rootBounds.x, rootBounds.y, rootBounds.width, rootBounds.height, MaterialShapes.Medium, p.surfaceContainerHigh, Stroke(p.outlineVariant, 1f))
            var cursorY = rootBounds.y + 8f
            items.forEachIndexed { index, item ->
                val rowHeight = rowHeight(item.supportingText)
                val top = cursorY
                val active = hoverRoot == index || pressedRoot == index || expandedParent == index
                if (active) {
                    context.backend.drawRoundedRect(rootBounds.x + 8f, top, rootBounds.width - 16f, rowHeight - 8f, MaterialShapes.Small, if (expandedParent == index) p.secondaryContainer else overlay(p.surfaceContainerHigh, p.onSurface, if (pressedRoot == index) 0.10f else 0.06f, 1f), null)
                }
                val color = when {
                    !item.enabled -> p.onSurface.withAlpha(97)
                    expandedParent == index -> p.onSecondaryContainer
                    item.destructive -> p.error
                    else -> p.onSurface
                }
                val labelStyle = TextStyle(context.theme.typography.bodySize, color, context.theme.typography.fontFamily)
                val leadingOffset = if (item.leadingLabel.isBlank()) 0f else 28f
                if (item.leadingLabel.isNotBlank()) {
                    val iconStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), color, context.theme.typography.fontFamily)
                    context.backend.drawText(item.leadingLabel, rootBounds.x + 16f, top + textBaseline(rowHeight - 8f, iconStyle.size), iconStyle)
                }
                val arrowLabel = if (item.children.isNotEmpty()) "›" else item.trailingLabel
                val trailingReserve = if (arrowLabel.isBlank()) 0f else 28f
                if (arrowLabel.isNotBlank()) {
                    val trailingStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), if (item.enabled) color else p.onSurface.withAlpha(97), context.theme.typography.fontFamily)
                    val trailingWidth = context.backend.measureText(arrowLabel, trailingStyle).first
                    context.backend.drawText(arrowLabel, rootBounds.x + rootBounds.width - 16f - trailingWidth, top + textBaseline(rowHeight - 8f, trailingStyle.size), trailingStyle)
                }
                val contentWidth = (rootBounds.width - 32f - leadingOffset - trailingReserve).coerceAtLeast(48f)
                val labelX = rootBounds.x + 16f + leadingOffset
                val labelY = if (item.supportingText.isBlank()) top + textBaseline(rowHeight - 8f, labelStyle.size) else top + 20f
                context.backend.drawText(item.label.takeFitting(context, labelStyle, contentWidth), labelX, labelY, labelStyle)
                if (item.supportingText.isNotBlank()) {
                    val supportingStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), if (item.enabled) p.onSurfaceVariant else p.onSurface.withAlpha(97), context.theme.typography.fontFamily)
                    context.backend.drawText(item.supportingText.takeFitting(context, supportingStyle, contentWidth), labelX, top + 38f, supportingStyle)
                }
                cursorY += rowHeight
                if (showDividers && index < items.lastIndex) {
                    context.backend.drawLine(rootBounds.x + 12f, cursorY - 4f, rootBounds.x + rootBounds.width - 12f, cursorY - 4f, Stroke(p.outlineVariant, 1f))
                }
            }
            if (expandedParent >= 0) {
                val parent = items.getOrNull(expandedParent)
                val children = parent?.children.orEmpty()
                if (children.isNotEmpty()) {
                    val childBounds = childBounds(children)
                    context.backend.drawRoundedRect(childBounds.x, childBounds.y, childBounds.width, childBounds.height, MaterialShapes.Medium, p.surfaceContainerHigh, Stroke(p.outlineVariant, 1f))
                    var childCursorY = childBounds.y + 8f
                    children.forEachIndexed { index, item ->
                        val rowHeight = rowHeight(item.supportingText)
                        val top = childCursorY
                        val active = hoverChild == index || pressedChild == index
                        if (active) {
                            context.backend.drawRoundedRect(childBounds.x + 8f, top, childBounds.width - 16f, rowHeight - 8f, MaterialShapes.Small, overlay(p.surfaceContainerHigh, p.onSurface, if (pressedChild == index) 0.10f else 0.06f, 1f), null)
                        }
                        val color = when {
                            !item.enabled -> p.onSurface.withAlpha(97)
                            item.destructive -> p.error
                            else -> p.onSurface
                        }
                        val leadingOffset = if (item.leadingLabel.isBlank()) 0f else 28f
                        if (item.leadingLabel.isNotBlank()) {
                            val iconStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), color, context.theme.typography.fontFamily)
                            context.backend.drawText(item.leadingLabel, childBounds.x + 16f, top + textBaseline(rowHeight - 8f, iconStyle.size), iconStyle)
                        }
                        if (item.trailingLabel.isNotBlank()) {
                            val trailingStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), if (item.enabled) p.onSurfaceVariant else p.onSurface.withAlpha(97), context.theme.typography.fontFamily)
                            val trailingWidth = context.backend.measureText(item.trailingLabel, trailingStyle).first
                            context.backend.drawText(item.trailingLabel, childBounds.x + childBounds.width - 16f - trailingWidth, top + textBaseline(rowHeight - 8f, trailingStyle.size), trailingStyle)
                        }
                        val trailingReserve = if (item.trailingLabel.isBlank()) 0f else 52f
                        val labelStyle = TextStyle(context.theme.typography.bodySize, color, context.theme.typography.fontFamily)
                        val contentWidth = (childBounds.width - 32f - leadingOffset - trailingReserve).coerceAtLeast(48f)
                        val labelX = childBounds.x + 16f + leadingOffset
                        val labelY = if (item.supportingText.isBlank()) top + textBaseline(rowHeight - 8f, labelStyle.size) else top + 20f
                        context.backend.drawText(item.label.takeFitting(context, labelStyle, contentWidth), labelX, labelY, labelStyle)
                        if (item.supportingText.isNotBlank()) {
                            val supportingStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), if (item.enabled) p.onSurfaceVariant else p.onSurface.withAlpha(97), context.theme.typography.fontFamily)
                            context.backend.drawText(item.supportingText.takeFitting(context, supportingStyle, contentWidth), labelX, top + 38f, supportingStyle)
                        }
                        childCursorY += rowHeight
                    }
                }
            }
        }
    }

    private fun rootBounds(): DialogBounds {
        val popupWidth = menuWidth.coerceAtLeast(160f).coerceAtMost((measuredWidth - menuX - 8f).coerceAtLeast(160f))
        val popupHeight = contentHeight(items).coerceAtLeast(16f).coerceAtMost((measuredHeight - menuY - 8f).coerceAtLeast(16f))
        return DialogBounds(menuX.coerceAtLeast(0f), menuY.coerceAtLeast(0f), popupWidth, popupHeight)
    }

    private fun childBounds(children: List<MaterialCascadingMenuItem>): DialogBounds {
        val rootBounds = rootBounds()
        val popupWidth = menuWidth.coerceAtLeast(160f).coerceAtMost((measuredWidth - rootBounds.x - rootBounds.width - 16f).coerceAtLeast(160f))
        val popupHeight = contentHeight(children).coerceAtLeast(16f).coerceAtMost((measuredHeight - rootBounds.y - 8f).coerceAtLeast(16f))
        val desiredX = rootBounds.x + rootBounds.width + 8f
        val clampedX = desiredX.coerceIn(0f, (measuredWidth - popupWidth - 8f).coerceAtLeast(0f))
        return DialogBounds(clampedX, rootBounds.y, popupWidth, popupHeight)
    }

    private fun contentHeight(menuItems: List<MaterialCascadingMenuItem>): Float = menuItems.fold(16f) { total, item ->
        total + rowHeight(item.supportingText)
    }

    private fun rowHeight(supportingText: String): Float = if (supportingText.isBlank()) 40f else 56f

    private fun rootIndexAt(localX: Float, localY: Float): Int {
        val bounds = rootBounds()
        if (localX !in bounds.x..(bounds.x + bounds.width) || localY !in bounds.y..(bounds.y + bounds.height)) return -2
        var cursorY = bounds.y + 8f
        items.forEachIndexed { index, item ->
            val itemHeight = rowHeight(item.supportingText)
            if (localY in cursorY..(cursorY + itemHeight)) return index
            cursorY += itemHeight
        }
        return Int.MIN_VALUE
    }

    private fun childIndexAt(localX: Float, localY: Float): Int {
        val parent = items.getOrNull(expandedParent) ?: return -2
        if (parent.children.isEmpty()) return -2
        val bounds = childBounds(parent.children)
        if (localX !in bounds.x..(bounds.x + bounds.width) || localY !in bounds.y..(bounds.y + bounds.height)) return -2
        var cursorY = bounds.y + 8f
        parent.children.forEachIndexed { index, item ->
            val itemHeight = rowHeight(item.supportingText)
            if (localY in cursorY..(cursorY + itemHeight)) return index
            cursorY += itemHeight
        }
        return Int.MIN_VALUE
    }
}

class MaterialAutocompleteField(
    val value: State<String> = stateOf(""),
    val expanded: State<Boolean> = stateOf(false),
    val options: List<String>,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 280f,
    height: Float = 56f,
    var label: String = "",
    var placeholder: String = "",
    var supportingText: String = "",
    var variant: MaterialTextFieldStyle = MaterialTextFieldStyle.Filled,
    var maxVisibleOptions: Int = 5,
    var dismissOnOutsideClick: Boolean = true,
    private val onSelect: (String) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var hoverIndex = Int.MIN_VALUE
    private var pressedIndex = Int.MIN_VALUE
    private var cursor = value.value.length
    private var fieldHover = 0f
    private var fieldFocused = 0f

    init {
        focusable = true
        minSize(180f, 56f)
        value.subscribe(invalidate)
        expanded.subscribe(invalidate)
        onDispose {
            value.unsubscribe(invalidate)
            expanded.unsubscribe(invalidate)
        }
        on(PointerDown) {
            if (disabled) return@on
            requestFocus()
            val target = itemIndexAt(it.x - x, it.y - y)
            if (target >= 0) {
                pressedIndex = target
            } else if (target == -1) {
                pressedIndex = -1
                cursor = value.value.length
            } else if (target == -2 && dismissOnOutsideClick) {
                expanded.value = false
            }
            setInteraction { copy(pressed = true) }
            it.consume()
        }
        on(PointerMove) {
            hoverIndex = itemIndexAt(it.x - x, it.y - y)
            requestRender()
        }
        on(PointerUp) {
            if (disabled) return@on
            val released = itemIndexAt(it.x - x, it.y - y)
            when {
                pressedIndex == -1 && released == -1 -> expanded.value = true
                pressedIndex >= 0 && released == pressedIndex -> {
                    val option = filteredOptions().getOrNull(released)
                    if (option != null) {
                        value.value = option
                        cursor = option.length
                        expanded.value = false
                        onSelect(option)
                    }
                }
                released == -2 && dismissOnOutsideClick -> expanded.value = false
            }
            pressedIndex = Int.MIN_VALUE
            setInteraction { copy(pressed = false) }
            it.consume()
        }
        on(TextInput) {
            if (disabled) return@on
            val safeCursor = cursor.coerceIn(0, value.value.length)
            value.value = value.value.substring(0, safeCursor) + it.text + value.value.substring(safeCursor)
            cursor = safeCursor + it.text.length
            expanded.value = true
            it.consume()
        }
        on(KeyDown) {
            if (handleKey(it)) {
                requestRender()
                it.consume()
            }
        }
        onKey("keyDown") {
            if (it.keyCode == AwtKeyEvent.VK_ESCAPE && expanded.value) {
                expanded.value = false
                it.consume()
            }
        }
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        fieldHover = animate(fieldHover, if (interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        fieldFocused = animate(fieldFocused, if (interaction.focused && !disabled) 1f else 0f, context.time.deltaSeconds, 16f)
        val stateAlpha = 0.03f * fieldHover + 0.04f * fieldFocused
        val base = if (variant == MaterialTextFieldStyle.Filled) p.surfaceContainerHighest else p.surface
        val fieldBackground = overlay(base, p.onSurface, stateAlpha, if (disabled) 0.38f else 1f)
        val fieldBorder = when (variant) {
            MaterialTextFieldStyle.Filled -> if (interaction.focused || expanded.value) p.primary else p.outlineVariant.withAlpha(140)
            MaterialTextFieldStyle.Outlined -> if (interaction.focused || expanded.value) p.primary else p.outline
        }
        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(0f, 0f, measuredWidth, 56f, MaterialShapes.ExtraSmall, fieldBackground, Stroke(fieldBorder, if (interaction.focused || expanded.value) 2f else 1f))
            val bodySize = context.theme.typography.bodySize
            val labelSize = (bodySize - 2f).coerceAtLeast(10f)
            if (label.isNotBlank()) {
                val labelColor = if (interaction.focused || expanded.value) p.primary else p.onSurfaceVariant
                context.backend.drawText(label, 16f, 16f, TextStyle(labelSize, labelColor.withAlpha(if (disabled) 110 else 255), context.theme.typography.fontFamily))
            }
            val text = if (value.value.isBlank()) placeholder else value.value
            val color = when {
                disabled -> p.onSurface.withAlpha(97)
                value.value.isNotBlank() -> p.onSurface
                else -> p.onSurfaceVariant
            }
            val textStyle = TextStyle(bodySize, color, context.theme.typography.fontFamily)
            val baseline = if (label.isNotBlank()) 40f else textBaseline(56f, bodySize)
            context.backend.drawText(text.takeFitting(context, textStyle, measuredWidth - 44f), 16f, baseline, textStyle)
            val arrow = if (expanded.value) "▴" else "▾"
            val arrowWidth = context.backend.measureText(arrow, textStyle).first
            context.backend.drawText(arrow, measuredWidth - 16f - arrowWidth, baseline, textStyle)
            if (supportingText.isNotBlank()) {
                context.backend.drawText(
                    supportingText,
                    16f,
                    70f,
                    TextStyle(labelSize, p.onSurfaceVariant.withAlpha(if (disabled) 110 else 255), context.theme.typography.fontFamily)
                )
            }
            if (expanded.value) {
                val bounds = menuBounds(filteredOptions())
                context.backend.drawRoundedRect(bounds.x, bounds.y, bounds.width, bounds.height, MaterialShapes.Medium, p.surfaceContainerHigh, Stroke(p.outlineVariant, 1f))
                filteredOptions().forEachIndexed { index, option ->
                    val top = bounds.y + 8f + index * 40f
                    val active = hoverIndex == index || pressedIndex == index
                    if (active) {
                        context.backend.drawRoundedRect(
                            bounds.x + 8f,
                            top,
                            bounds.width - 16f,
                            32f,
                            MaterialShapes.Small,
                            overlay(p.surfaceContainerHigh, p.onSurface, if (pressedIndex == index) 0.10f else 0.06f, 1f),
                            null
                        )
                    }
                    val optionStyle = TextStyle(context.theme.typography.bodySize, p.onSurface, context.theme.typography.fontFamily)
                    context.backend.drawText(option.takeFitting(context, optionStyle, bounds.width - 32f), bounds.x + 16f, top + textBaseline(32f, optionStyle.size), optionStyle)
                }
            }
        }
        if (isAnimating(fieldHover, fieldFocused)) requestRender()
    }

    private fun filteredOptions(): List<String> {
        val query = value.value.trim()
        val matches = if (query.isBlank()) options else options.filter { option -> option.contains(query, ignoreCase = true) }
        return matches.take(maxVisibleOptions.coerceAtLeast(1))
    }

    private fun handleKey(event: KeyEvent): Boolean {
        if (disabled) return false
        val filtered = filteredOptions()
        return when (event.keyCode) {
            AwtKeyEvent.VK_BACK_SPACE -> {
                if (cursor > 0 && value.value.isNotEmpty()) {
                    value.value = value.value.removeRange(cursor - 1, cursor)
                    cursor -= 1
                    expanded.value = true
                }
                true
            }
            AwtKeyEvent.VK_DELETE -> {
                if (cursor < value.value.length) {
                    value.value = value.value.removeRange(cursor, cursor + 1)
                    expanded.value = true
                }
                true
            }
            AwtKeyEvent.VK_LEFT -> {
                cursor = (cursor - 1).coerceAtLeast(0)
                true
            }
            AwtKeyEvent.VK_RIGHT -> {
                cursor = (cursor + 1).coerceAtMost(value.value.length)
                true
            }
            AwtKeyEvent.VK_DOWN -> {
                if (!expanded.value) {
                    expanded.value = true
                } else if (filtered.isNotEmpty()) {
                    hoverIndex = if (hoverIndex < 0) 0 else (hoverIndex + 1).coerceAtMost(filtered.lastIndex)
                }
                true
            }
            AwtKeyEvent.VK_UP -> {
                if (expanded.value && filtered.isNotEmpty()) {
                    hoverIndex = if (hoverIndex <= 0) 0 else hoverIndex - 1
                }
                true
            }
            AwtKeyEvent.VK_ENTER -> {
                if (expanded.value && hoverIndex in filtered.indices) {
                    val option = filtered[hoverIndex]
                    value.value = option
                    cursor = option.length
                    expanded.value = false
                    onSelect(option)
                } else {
                    expanded.value = true
                }
                true
            }
            AwtKeyEvent.VK_ESCAPE -> {
                expanded.value = false
                true
            }
            else -> false
        }
    }

    private fun menuBounds(filtered: List<String>): DialogBounds {
        val popupY = 64f
        val popupWidth = measuredWidth.coerceAtLeast(160f)
        val popupHeight = (filtered.size * 40f + 16f).coerceAtMost((measuredHeight - popupY).coerceAtLeast(16f))
        return DialogBounds(0f, popupY, popupWidth, popupHeight)
    }

    private fun itemIndexAt(localX: Float, localY: Float): Int {
        if (localX in 0f..measuredWidth && localY in 0f..56f) return -1
        if (!expanded.value) return -2
        val filtered = filteredOptions()
        val bounds = menuBounds(filtered)
        if (localX !in bounds.x..(bounds.x + bounds.width) || localY !in bounds.y..(bounds.y + bounds.height)) return -2
        filtered.indices.forEach { index ->
            val top = bounds.y + 8f + index * 40f
            if (localY in top..(top + 32f)) return index
        }
        return Int.MIN_VALUE
    }
}

class MaterialSearchView(
    val query: State<String> = stateOf(""),
    val visibleState: State<Boolean> = stateOf(false),
    val suggestions: List<String> = emptyList(),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 240f,
    var title: String = "",
    var supportingText: String = "",
    var placeholder: String = "Search",
    var maxVisibleSuggestions: Int = 6,
    var dismissOnOutsideClick: Boolean = true,
    private val onSearch: (String) -> Unit = {},
    private val onDismiss: () -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = {
        visible = visibleState.value
        requestRender()
    }
    private var cursor = query.value.length
    private var pressedIndex = Int.MIN_VALUE
    private var hoverIndex = Int.MIN_VALUE
    private var fieldHover = 0f

    init {
        focusable = true
        visible = visibleState.value
        minSize(200f, 160f)
        query.subscribe(invalidate)
        visibleState.subscribe(invalidate)
        onDispose {
            query.unsubscribe(invalidate)
            visibleState.unsubscribe(invalidate)
        }
        on(PointerDown) {
            if (!visible || disabled) return@on
            requestFocus()
            pressedIndex = hitTarget(it.x - x, it.y - y)
            if (pressedIndex == -1) cursor = query.value.length
            it.consume()
        }
        on(PointerMove) {
            if (!visible) return@on
            hoverIndex = hitTarget(it.x - x, it.y - y)
            requestRender()
        }
        on(PointerUp) {
            if (!visible || disabled) return@on
            val released = hitTarget(it.x - x, it.y - y)
            when {
                pressedIndex >= 0 && released == pressedIndex -> {
                    filteredSuggestions().getOrNull(released)?.let { suggestion ->
                        query.value = suggestion
                        cursor = suggestion.length
                        onSearch(suggestion)
                        visibleState.value = false
                    }
                }
                pressedIndex == -3 && released == -3 -> dismiss()
                pressedIndex == -4 && released == -4 -> {
                    query.value = ""
                    cursor = 0
                }
                released == -2 && pressedIndex == -2 && dismissOnOutsideClick -> dismiss()
            }
            pressedIndex = Int.MIN_VALUE
            it.consume()
        }
        on(TextInput) {
            if (disabled) return@on
            val safeCursor = cursor.coerceIn(0, query.value.length)
            query.value = query.value.substring(0, safeCursor) + it.text + query.value.substring(safeCursor)
            cursor = safeCursor + it.text.length
            it.consume()
        }
        on(KeyDown) {
            if (handleKey(it)) {
                requestRender()
                it.consume()
            }
        }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        val p = context.theme.palette
        fieldHover = animate(fieldHover, if (interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        val bounds = cardBounds()
        val titleStyle = TextStyle(context.theme.typography.titleSize + 1f, p.onSurface, context.theme.typography.fontFamily)
        val bodyStyle = TextStyle(context.theme.typography.bodySize, p.onSurfaceVariant, context.theme.typography.fontFamily)
        context.backend.translated(x, y) {
            context.backend.drawRect(0f, 0f, measuredWidth, measuredHeight, p.scrim.withAlpha(132), null)
            context.backend.drawRoundedRect(bounds.x, bounds.y, bounds.width, bounds.height, MaterialShapes.ExtraLarge, p.surface, Stroke(p.outlineVariant, 1f))
            context.backend.drawRoundedRect(bounds.x + 16f, bounds.y + 16f, bounds.width - 32f, 56f, 28f, overlay(p.surfaceContainerHigh, p.onSurface, 0.04f * fieldHover, 1f), null)
            val iconStyle = TextStyle(context.theme.typography.bodySize, p.onSurfaceVariant, context.theme.typography.fontFamily)
            context.backend.drawText("←", bounds.x + 28f, bounds.y + 16f + textBaseline(56f, iconStyle.size), iconStyle)
            val clearLabel = if (query.value.isNotBlank()) "×" else "⌕"
            val clearWidth = context.backend.measureText(clearLabel, iconStyle).first
            context.backend.drawText(clearLabel, bounds.x + bounds.width - 28f - clearWidth, bounds.y + 16f + textBaseline(56f, iconStyle.size), iconStyle)
            val textStyle = TextStyle(context.theme.typography.bodySize, if (query.value.isBlank()) p.onSurfaceVariant else p.onSurface, context.theme.typography.fontFamily)
            context.backend.drawText((if (query.value.isBlank()) placeholder else query.value).takeFitting(context, textStyle, bounds.width - 112f), bounds.x + 56f, bounds.y + 16f + textBaseline(56f, textStyle.size), textStyle)
            var contentY = bounds.y + 92f
            if (title.isNotBlank()) {
                context.backend.drawText(title.takeFitting(context, titleStyle, bounds.width - 32f), bounds.x + 16f, contentY + textBaseline(24f, titleStyle.size), titleStyle)
                contentY += 28f
            }
            if (supportingText.isNotBlank()) {
                val lines = supportingText.wrapText(context, bodyStyle, bounds.width - 32f, 2)
                lines.forEachIndexed { index, line ->
                    context.backend.drawText(line, bounds.x + 16f, contentY + index * (bodyStyle.size + 6f), bodyStyle)
                }
                contentY += lines.size * (bodyStyle.size + 6f) + 8f
            }
            filteredSuggestions().forEachIndexed { index, suggestion ->
                val top = contentY + index * 44f
                val active = hoverIndex == index || pressedIndex == index
                if (active) {
                    context.backend.drawRoundedRect(bounds.x + 8f, top, bounds.width - 16f, 36f, 18f, overlay(p.surfaceContainerHigh, p.onSurface, if (pressedIndex == index) 0.10f else 0.06f, 1f), null)
                }
                context.backend.drawText(suggestion.takeFitting(context, textStyle, bounds.width - 48f), bounds.x + 24f, top + textBaseline(36f, textStyle.size), textStyle)
            }
        }
        if (fieldHover in 0.001f..0.999f) requestRender()
    }

    fun dismiss() {
        if (!visible) return
        visibleState.value = false
        onDismiss()
    }

    private fun handleKey(event: KeyEvent): Boolean {
        if (disabled) return false
        val filtered = filteredSuggestions()
        return when (event.keyCode) {
            AwtKeyEvent.VK_BACK_SPACE -> {
                if (cursor > 0 && query.value.isNotEmpty()) {
                    query.value = query.value.removeRange(cursor - 1, cursor)
                    cursor -= 1
                }
                true
            }
            AwtKeyEvent.VK_DELETE -> {
                if (cursor < query.value.length) {
                    query.value = query.value.removeRange(cursor, cursor + 1)
                }
                true
            }
            AwtKeyEvent.VK_LEFT -> {
                cursor = (cursor - 1).coerceAtLeast(0)
                true
            }
            AwtKeyEvent.VK_RIGHT -> {
                cursor = (cursor + 1).coerceAtMost(query.value.length)
                true
            }
            AwtKeyEvent.VK_DOWN -> {
                if (filtered.isNotEmpty()) hoverIndex = if (hoverIndex < 0) 0 else (hoverIndex + 1).coerceAtMost(filtered.lastIndex)
                true
            }
            AwtKeyEvent.VK_UP -> {
                if (filtered.isNotEmpty()) hoverIndex = if (hoverIndex <= 0) 0 else hoverIndex - 1
                true
            }
            AwtKeyEvent.VK_ENTER -> {
                if (hoverIndex in filtered.indices) {
                    val suggestion = filtered[hoverIndex]
                    query.value = suggestion
                    cursor = suggestion.length
                    onSearch(suggestion)
                    visibleState.value = false
                } else {
                    onSearch(query.value)
                }
                true
            }
            AwtKeyEvent.VK_ESCAPE -> {
                dismiss()
                true
            }
            else -> false
        }
    }

    private fun filteredSuggestions(): List<String> {
        val trimmed = query.value.trim()
        val matches = if (trimmed.isBlank()) suggestions else suggestions.filter { suggestion -> suggestion.contains(trimmed, ignoreCase = true) }
        return matches.take(maxVisibleSuggestions.coerceAtLeast(1))
    }

    private fun cardBounds(): DialogBounds {
        val cardWidth = (measuredWidth - 32f).coerceAtLeast(220f)
        val suggestionHeight = filteredSuggestions().size * 44f
        val infoHeight = (if (title.isBlank()) 0f else 28f) + (if (supportingText.isBlank()) 0f else 36f)
        val cardHeight = (100f + suggestionHeight + infoHeight).coerceAtMost(measuredHeight - 24f).coerceAtLeast(120f)
        return DialogBounds((measuredWidth - cardWidth) / 2f, 12f, cardWidth, cardHeight)
    }

    private fun hitTarget(localX: Float, localY: Float): Int {
        val bounds = cardBounds()
        if (localX !in bounds.x..(bounds.x + bounds.width) || localY !in bounds.y..(bounds.y + bounds.height)) return -2
        val fieldLeft = bounds.x + 16f
        val fieldTop = bounds.y + 16f
        val fieldRight = bounds.x + bounds.width - 16f
        val fieldBottom = fieldTop + 56f
        if (localY in fieldTop..fieldBottom && localX in fieldLeft..fieldRight) {
            if (localX <= fieldLeft + 32f) return -3
            if (localX >= fieldRight - 32f) return -4
            return -1
        }
        var contentY = bounds.y + 92f
        if (title.isNotBlank()) contentY += 28f
        if (supportingText.isNotBlank()) contentY += 36f
        filteredSuggestions().indices.forEach { index ->
            val top = contentY + index * 44f
            if (localY in top..(top + 36f) && localX in (bounds.x + 8f)..(bounds.x + bounds.width - 8f)) return index
        }
        return Int.MIN_VALUE
    }
}

class MaterialDivider(
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 160f,
    height: Float = 1f
) : Component(x, y, width, height) {
    override fun draw(context: RenderContext) {
        context.backend.drawLine(x, y + height / 2f, x + width, y + height / 2f, Stroke(context.theme.palette.outlineVariant, height.coerceAtLeast(1f)))
    }
}

class MaterialExpansionPanel(
    var title: String,
    var supportingText: String = "",
    val expanded: State<Boolean> = stateOf(false),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 280f,
    height: Float = 48f,
    private val onToggle: (Boolean) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = { requestRender() }
    private var expandProgress = if (expanded.value) 1f else 0f
    private var hovered = false
    private var pressed = false

    init {
        focusable = true
        minSize(160f, 48f)
        expanded.subscribe(invalidate)
        onDispose { expanded.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!disabled) {
                pressed = true
                requestFocus()
                requestRender()
                it.consume()
            }
        }
        on(PointerMove) {
            hovered = true
            requestRender()
        }
        on(PointerUp) {
            if (pressed && !disabled) toggle()
            pressed = false
            requestRender()
            if (!disabled) it.consume()
        }
        on(KeyDown.Enter or KeyDown.Space) {
            if (!disabled) {
                toggle()
                it.consume()
            }
        }
    }

    private fun toggle() {
        expanded.value = !expanded.value
        onToggle(expanded.value)
    }

    override fun draw(context: RenderContext) {
        val p = context.theme.palette
        val target = if (expanded.value) 1f else 0f
        expandProgress = animate(expandProgress, target, context.time.deltaSeconds, 14f)
        val isExpanded = expanded.value
        val active = hovered || pressed
        backgroundColor = when {
            isExpanded -> p.secondaryContainer.withAlpha(80)
            active -> overlay(p.surfaceContainerHigh, p.onSurface, if (pressed) 0.10f else 0.06f, 1f)
            else -> Color(0, 0, 0, 0)
        }
        cornerRadius = MaterialShapes.Medium
        borderColor = if (isExpanded) p.outlineVariant else null
        borderWidth = if (isExpanded) 1f else 0f
        super.draw(context)
        val headerH = 48f
        context.backend.translated(x, y) {
            val titleColor = if (isExpanded) p.onSecondaryContainer else p.onSurface
            val titleStyle = TextStyle(context.theme.typography.bodySize, titleColor, context.theme.typography.fontFamily)
            val supportStyle = TextStyle((context.theme.typography.bodySize - 1f).coerceAtLeast(10f), p.onSurfaceVariant, context.theme.typography.fontFamily)
            val chevron = if (expandProgress > 0.5f) "▲" else "▼"
            val chevronStyle = TextStyle(context.theme.typography.bodySize - 2f, p.onSurfaceVariant, context.theme.typography.fontFamily)
            val chevronW = context.backend.measureText(chevron, chevronStyle).first
            val contentW = (measuredWidth - 32f - chevronW - 8f).coerceAtLeast(48f)
            val titleY = if (supportingText.isBlank()) textBaseline(headerH, titleStyle.size) else 20f
            context.backend.drawText(title.takeFitting(context, titleStyle, contentW), 16f, titleY, titleStyle)
            if (supportingText.isNotBlank()) {
                context.backend.drawText(supportingText.takeFitting(context, supportStyle, contentW), 16f, 36f, supportStyle)
            }
            context.backend.drawText(chevron, measuredWidth - 16f - chevronW, textBaseline(headerH, chevronStyle.size), chevronStyle)
            if (expandProgress > 0.01f) {
                val bodyTop = headerH
                val bodyH = (measuredHeight - bodyTop).coerceAtLeast(0f)
                if (bodyH > 0f) {
                    context.backend.clipped(0f, bodyTop, measuredWidth, bodyH) {
                        if (isExpanded || expandProgress > 0.95f) {
                            children.forEach { it.draw(context) }
                        }
                    }
                }
            }
        }
        children.forEach { it.visible = expandProgress > 0.01f }
        if (isAnimating(expandProgress)) requestRender()
    }
}

class MaterialSnackbarHost(
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 320f,
    height: Float = 48f,
    var durationMs: Long = 4000L,
    private val onAction: (String) -> Unit = {},
    private val onDismiss: (String) -> Unit = {}
) : Component(x, y, width, height) {
    private val queue = mutableListOf<SnackbarEntry>()
    private var currentEntry: SnackbarEntry? = null
    private var showTimestamp = 0L
    private var pressedAction = Int.MIN_VALUE
    private var hoverAction = Int.MIN_VALUE
    private var manualDismiss = false
    private var cachedActionStart = 0f
    private var cachedActionEnd = 0f

    data class SnackbarEntry(
        val message: String,
        val actionLabel: String = "",
        val id: String = ""
    )

    init {
        minSize(160f, 48f)
        visible = false
        on(PointerDown) {
            if (!visible || currentEntry == null) return@on
            pressedAction = actionIndexAt(it.x - x, it.y - y)
            it.consume()
        }
        on(PointerMove) {
            if (!visible || currentEntry == null) return@on
            hoverAction = actionIndexAt(it.x - x, it.y - y)
            requestRender()
        }
        on(PointerUp) {
            if (!visible || currentEntry == null) return@on
            val released = actionIndexAt(it.x - x, it.y - y)
            if (released == pressedAction && released == 1) {
                val entry = currentEntry
                advance()
                entry?.let { onAction(it.id) }
            }
            pressedAction = Int.MIN_VALUE
            it.consume()
        }
    }

    fun show(message: String, actionLabel: String = "", id: String = "") {
        val entry = SnackbarEntry(message, actionLabel, id)
        if (currentEntry == null) {
            beginEntry(entry)
        } else {
            if (queue.none { it.id == id && id.isNotBlank() }) {
                queue.add(entry)
            }
        }
    }

    fun dismiss() {
        if (currentEntry != null) {
            manualDismiss = true
            val entry = currentEntry
            advance()
            entry?.let { onDismiss(it.id) }
        }
    }

    private fun beginEntry(entry: SnackbarEntry) {
        currentEntry = entry
        showTimestamp = System.currentTimeMillis()
        visible = true
        manualDismiss = false
        pressedAction = Int.MIN_VALUE
        hoverAction = Int.MIN_VALUE
        requestRender()
    }

    private fun advance() {
        currentEntry = if (queue.isNotEmpty()) queue.removeAt(0) else null
        if (currentEntry != null) {
            showTimestamp = System.currentTimeMillis()
            visible = true
            manualDismiss = false
        } else {
            visible = false
        }
        pressedAction = Int.MIN_VALUE
        hoverAction = Int.MIN_VALUE
        requestRender()
    }

    override fun draw(context: RenderContext) {
        val entry = currentEntry ?: run { visible = false; return }
        val elapsed = System.currentTimeMillis() - showTimestamp
        if (!manualDismiss && elapsed >= durationMs) {
            advance()
            onDismiss(entry.id)
            return
        }
        val p = context.theme.palette
        backgroundColor = p.inverseSurface
        cornerRadius = MaterialShapes.Small
        borderColor = null
        borderWidth = 0f
        elevationShadow = Shadow(Color(0, 0, 0, 85), 10f, offsetY = 4f)
        super.draw(context)
        context.backend.translated(x, y) {
            val bodyStyle = TextStyle(context.theme.typography.bodySize, p.inverseOnSurface, context.theme.typography.fontFamily)
            val actionStyle = TextStyle(context.theme.typography.bodySize, p.primary, context.theme.typography.fontFamily)
            val actionText = entry.actionLabel
            val actionWidth = if (actionText.isBlank()) 0f else context.backend.measureText(actionText, actionStyle).first + 24f
            val availableW = (measuredWidth - 32f - actionWidth).coerceAtLeast(48f)
            context.backend.drawText(entry.message.takeFitting(context, bodyStyle, availableW), 16f, textBaseline(measuredHeight, bodyStyle.size), bodyStyle)
            if (actionText.isNotBlank()) {
                val textW = context.backend.measureText(actionText, actionStyle).first
                val actionX = measuredWidth - 16f - textW
                cachedActionStart = actionX - 8f
                cachedActionEnd = actionX + textW + 8f
                if (hoverAction == 1 || pressedAction == 1) {
                    context.backend.drawRoundedRect(actionX - 8f, 8f, textW + 16f, measuredHeight - 16f, (measuredHeight - 16f) / 2f, overlay(p.inverseSurface, p.primary, if (pressedAction == 1) 0.18f else 0.10f, 1f), null)
                }
                context.backend.drawText(actionText, actionX, textBaseline(measuredHeight, actionStyle.size), actionStyle)
            }
        }
    }

    private fun actionIndexAt(localX: Float, localY: Float): Int {
        val entry = currentEntry ?: return Int.MIN_VALUE
        if (entry.actionLabel.isBlank()) return Int.MIN_VALUE
        return if (localX in cachedActionStart..cachedActionEnd && localY in 0f..measuredHeight) 1 else Int.MIN_VALUE
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

data class DialogBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

private fun drawDialogAction(
    context: RenderContext,
    label: String,
    style: TextStyle,
    x: Float,
    y: Float,
    width: Float,
    hovered: Boolean,
    pressed: Boolean
) {
    if (hovered || pressed) {
        context.backend.drawRoundedRect(
            x,
            y,
            width,
            32f,
            MaterialShapes.Small,
            overlay(context.theme.palette.surfaceContainerHigh, context.theme.palette.onSurface, if (pressed) 0.10f else 0.06f, 1f),
            null
        )
    }
    val textWidth = context.backend.measureText(label, style).first
    context.backend.drawText(label, x + (width - textWidth) / 2f, y + textBaseline(32f, style.size), style)
}

private fun String.wrapText(context: RenderContext, style: TextStyle, maxWidth: Float, maxLines: Int): List<String> {
    if (isBlank()) return emptyList()
    val words = trim().split(Regex("\\s+"))
    if (words.isEmpty()) return emptyList()
    val lines = mutableListOf<String>()
    var current = ""
    for (word in words) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (context.backend.measureText(candidate, style).first <= maxWidth) {
            current = candidate
            continue
        }
        if (current.isNotEmpty()) {
            lines += current
            if (lines.size == maxLines) return lines.dropLast(1) + lines.last().takeFitting(context, style, maxWidth)
        }
        current = word
        if (lines.size == maxLines) break
    }
    if (current.isNotEmpty() && lines.size < maxLines) {
        lines += current.takeFitting(context, style, maxWidth)
    }
    return lines.take(maxLines).ifEmpty { listOf(takeFitting(context, style, maxWidth)) }
}

private fun drawTimeColumn(
    context: RenderContext,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    items: List<String>,
    selectedIndex: Int,
    hoverIndex: Int,
    style: TextStyle
) {
    val p = context.theme.palette
    val rowHeight = height / items.size.coerceAtLeast(1)
    context.backend.drawRoundedRect(x, y, width, height, MaterialShapes.Medium, p.surfaceContainer, null)
    items.forEachIndexed { index, item ->
        val top = y + index * rowHeight
        val selected = index == selectedIndex
        val hovered = index == hoverIndex
        if (selected || hovered) {
            context.backend.drawRoundedRect(
                x + 4f,
                top + 2f,
                width - 8f,
                rowHeight - 4f,
                MaterialShapes.Small,
                if (selected) p.primaryContainer else overlay(p.surfaceContainerHigh, p.onSurface, 0.06f, 1f),
                null
            )
        }
        val textStyle = if (selected) {
            TextStyle(style.size, p.onPrimaryContainer, style.fontFamily)
        } else if (hovered) {
            TextStyle(style.size, p.onSurface, style.fontFamily)
        } else {
            style
        }
        val textWidth = context.backend.measureText(item, textStyle).first
        context.backend.drawText(item, x + (width - textWidth) / 2f, top + textBaseline(rowHeight, textStyle.size), textStyle)
    }
}

private fun drawTopBarAction(context: RenderContext, label: String, x: Float, y: Float, hovered: Boolean, pressed: Boolean) {
    if (hovered || pressed) {
        context.backend.drawRoundedRect(x, y, 40f, 40f, 20f, overlay(context.theme.palette.surface, context.theme.palette.onSurface, if (pressed) 0.10f else 0.06f, 1f), null)
    }
    val style = TextStyle(context.theme.typography.bodySize, context.theme.palette.onSurfaceVariant, context.theme.typography.fontFamily)
    val textWidth = context.backend.measureText(label, style).first
    context.backend.drawText(label, x + (40f - textWidth) / 2f, y + textBaseline(40f, style.size), style)
}

private fun String.takeFitting(context: RenderContext, style: TextStyle, maxWidth: Float): String {
    if (context.backend.measureText(this, style).first <= maxWidth) return this
    if (isEmpty() || maxWidth <= 0f) return ""
    var end = length
    while (end > 1) {
        val candidate = substring(0, end) + "…"
        if (context.backend.measureText(candidate, style).first <= maxWidth) return candidate
        end -= 1
    }
    return "…"
}

private fun drawArcStroke(context: RenderContext, centerX: Float, centerY: Float, radius: Float, startAngle: Float, sweep: Float, stroke: Stroke) {
    if (radius <= 0f || sweep == 0f) return
    context.backend.drawArc(centerX, centerY, radius, startAngle, sweep, stroke)
}

private fun animate(current: Float, target: Float, dt: Float, speed: Float): Float =
    current + (target - current) * (1f - exp(-speed * dt.coerceIn(0f, 0.1f)))

private fun isAnimating(vararg values: Float) = values.any { it in 0.001f..0.999f }

private fun textBaseline(height: Float, size: Float) = height * 0.5f + size * 0.36f

private fun overlay(base: Color, tint: Color, stateAlpha: Float, enabledAlpha: Float): Color {
    val mixed = lerp(base, tint, stateAlpha.coerceIn(0f, 1f))
    return mixed.withAlpha((mixed.alpha * enabledAlpha).toInt())
}

private fun lerp(a: Color, b: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        (a.red + (b.red - a.red) * t).toInt().coerceIn(0, 255),
        (a.green + (b.green - a.green) * t).toInt().coerceIn(0, 255),
        (a.blue + (b.blue - a.blue) * t).toInt().coerceIn(0, 255),
        (a.alpha + (b.alpha - a.alpha) * t).toInt().coerceIn(0, 255)
    )
}

private fun Color.withAlpha(alpha: Int) = Color(red, green, blue, alpha.coerceIn(0, 255))
