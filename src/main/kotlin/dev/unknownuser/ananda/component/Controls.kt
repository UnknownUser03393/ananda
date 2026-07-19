package dev.unknownuser.ananda.component

import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextStyle
import dev.unknownuser.ananda.event.ImeCommit
import dev.unknownuser.ananda.event.ImeCompose
import dev.unknownuser.ananda.event.KeyDown
import dev.unknownuser.ananda.event.KeyEvent
import dev.unknownuser.ananda.event.Focus
import dev.unknownuser.ananda.event.PointerDown
import dev.unknownuser.ananda.event.PointerMove
import dev.unknownuser.ananda.event.PointerScroll
import dev.unknownuser.ananda.event.PointerUp
import dev.unknownuser.ananda.event.TextInput
import dev.unknownuser.ananda.event.or
import dev.unknownuser.ananda.layout.ColumnLayout
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.stateOf
import java.awt.Color
import java.awt.event.InputEvent
import java.awt.event.KeyEvent as AwtKeyEvent
import kotlin.math.abs
import kotlin.math.exp

class Panel(
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 0f,
    height: Float = 0f
) : Component(x, y, width, height)

class Spacer(
    width: Float = 0f,
    height: Float = 0f
) : Component(width = width, height = height)

class Button(
    var text: String,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 120f,
    height: Float = 36f,
    private val onClick: () -> Unit = {}
) : Component(x, y, width, height) {
    private var hoverAmount = 0f
    private var pressAmount = 0f
    private var focusAmount = 0f

    init {
        focusable = true
        padding(12f, 8f)
        on(PointerDown) {
            setInteraction { copy(pressed = true) }
            requestFocus()
            it.consume()
        }
        on(PointerUp) {
            if (interaction.pressed) onClick()
            setInteraction { copy(pressed = false) }
            it.consume()
        }
        on(KeyDown.Enter or KeyDown.Space) {
            onClick()
            it.consume()
        }
        render { context ->
            val theme = context.theme
            val textColor = lerpColor(
                if (disabled) theme.palette.mutedText.withAlpha(90) else theme.palette.mutedText,
                theme.palette.primary.brighterSoft(46),
                (hoverAmount * 0.85f + pressAmount * 0.15f).coerceIn(0f, 1f)
            )
            val textSize = 12.5f
            val textWidth = context.backend.measureText(text, TextStyle(textSize, textColor, theme.typography.fontFamily)).first
            context.backend.drawText(
                text,
                (measuredWidth - textWidth) / 2f,
                centerTextBaseline(measuredHeight, textSize),
                TextStyle(textSize, textColor, theme.typography.fontFamily)
            )
        }
    }

    override fun draw(context: dev.unknownuser.ananda.backend.RenderContext) {
        val theme = context.theme
        hoverAmount = animate(hoverAmount, if (interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 10f)
        pressAmount = animate(pressAmount, if (interaction.pressed && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        focusAmount = animate(focusAmount, if (interaction.focused && !disabled) 1f else 0f, context.time.deltaSeconds, 12f)

        val fill = lerpColor(
            theme.palette.surface.withAlpha(if (disabled) 14 else 34),
            theme.palette.primary.withAlpha(62 + (pressAmount * 24f).toInt()),
            (hoverAmount * 0.65f + pressAmount * 0.35f).coerceIn(0f, 1f)
        )
        backgroundColor = fill
        gradientColors = null
        cornerRadius = 6f
        elevationShadow = null
        borderColor = lerpColor(
            theme.palette.border.withAlpha(215),
            theme.palette.primary.withAlpha(210),
            (hoverAmount * 0.55f + focusAmount * 0.75f + pressAmount * 0.35f).coerceIn(0f, 1f)
        )
        borderWidth = 1.1f
        if (isUnitAnimationActive(hoverAmount, pressAmount, focusAmount)) requestRender()
        super.draw(context)
    }
}

class TextField(
    val value: State<String> = stateOf(""),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 220f,
    height: Float = 36f,
    var placeholder: String = ""
) : Component(x, y, width, height) {
    private var composingText = ""
    private val invalidate: () -> Unit = {
        charPositionsDirty = true
        cursor = cursor.coerceIn(0, value.value.length)
        selectionAnchor = selectionAnchor?.coerceIn(0, value.value.length)
        requestRender()
    }
    private var cursor = value.value.length
    private var selectionAnchor: Int? = null
    private var draggingSelection = false
    private var justFocusedByPointer = false
    private var charPositions = FloatArray(value.value.length + 1)
    private var charPositionsDirty = true
    private var scrollOffsetX = 0f
    private val hasSelection: Boolean
        get() = selectionRange() != null

    init {
        focusable = true
        padding(10f, 7f)
        value.subscribe(invalidate)
        onDispose { value.unsubscribe(invalidate) }
        on(Focus) {
            justFocusedByPointer = true
        }
        on(PointerDown) {
            requestFocus()
            if (!justFocusedByPointer) {
                cursor = cursorFromPointer(it.x)
                selectionAnchor = cursor
                draggingSelection = true
                ensureCursorVisible()
                requestRender()
            }
            justFocusedByPointer = false
            it.consume()
        }
        on(PointerMove) {
            if (draggingSelection && interaction.pressed) {
                cursor = cursorFromPointer(it.x)
                ensureCursorVisible()
                requestRender()
                it.consume()
            }
        }
        on(PointerUp) {
            draggingSelection = false
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
        render { context ->
            val theme = context.theme
            val textStyle = TextStyle(12.5f, theme.palette.text, theme.typography.fontFamily)
            updateCharPositions(context, textStyle)
            val display = when {
                value.value.isNotEmpty() || composingText.isNotEmpty() -> value.value + composingText
                else -> placeholder
            }
            val color = when {
                value.value.isEmpty() && composingText.isEmpty() -> theme.palette.mutedText
                else -> theme.palette.text
            }
            context.backend.clipped(padding.left, 1f, textViewportWidth(), measuredHeight - 2f) {
                if (interaction.focused) {
                    val selection = selectionRange()
                    if (selection != null) {
                        val startX = padding.left + charPositions[selection.first] - scrollOffsetX
                        val endX = padding.left + charPositions[selection.second] - scrollOffsetX
                        context.backend.drawRect(startX, 7f, endX - startX, measuredHeight - 14f, theme.palette.primary.withAlpha(70))
                    }
                }
                context.backend.drawText(
                    display,
                    padding.left - scrollOffsetX,
                    centerTextBaseline(measuredHeight, 12.5f),
                    TextStyle(12.5f, color, theme.typography.fontFamily)
                )
                if (interaction.focused && !hasSelection) {
                    val x = padding.left + charPositions[cursor.coerceIn(0, value.value.length)] - scrollOffsetX + 2f
                    context.backend.drawLine(x, 7f, x, measuredHeight - 7f, dev.unknownuser.ananda.backend.Stroke(theme.palette.accent, 1f))
                }
            }
        }
    }

    override fun draw(context: dev.unknownuser.ananda.backend.RenderContext) {
        val theme = context.theme
        backgroundColor = theme.palette.surface.withAlpha(if (interaction.focused) 50 else 26)
        gradientColors = null
        cornerRadius = 3f
        elevationShadow = null
        borderColor = if (interaction.focused) theme.palette.primary.withAlpha(160) else null
        borderWidth = 1f
        super.draw(context)
    }

    private fun handleKey(event: KeyEvent): Boolean {
        val ctrl = event.modifiers and InputEvent.CTRL_DOWN_MASK != 0
        val shift = event.modifiers and InputEvent.SHIFT_DOWN_MASK != 0
        return when (event.keyCode) {
            AwtKeyEvent.VK_A -> if (ctrl) {
                cursor = value.value.length
                selectionAnchor = 0
                ensureCursorVisible()
                true
            } else false
            AwtKeyEvent.VK_C -> if (ctrl) {
                selectedText()?.let { writeClipboard(it) }
                true
            } else false
            AwtKeyEvent.VK_X -> if (ctrl) {
                selectedText()?.let {
                    writeClipboard(it)
                    replaceSelection("")
                }
                true
            } else false
            AwtKeyEvent.VK_V -> if (ctrl) {
                replaceSelection(readClipboard().orEmpty())
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
                    charPositionsDirty = true
                    ensureCursorVisible()
                }
                true
            }
            AwtKeyEvent.VK_DELETE -> {
                if (selectionRange() != null) replaceSelection("") else if (cursor < value.value.length) {
                    value.value = value.value.removeRange(cursor, cursor + 1)
                    charPositionsDirty = true
                    ensureCursorVisible()
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
        ensureCursorVisible()
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
        charPositionsDirty = true
        ensureCursorVisible()
    }

    private fun selectionRange(): Pair<Int, Int>? {
        val anchor = selectionAnchor ?: return null
        if (anchor == cursor) return null
        val start = minOf(anchor, cursor).coerceIn(0, value.value.length)
        val end = maxOf(anchor, cursor).coerceIn(0, value.value.length)
        return start to end
    }

    private fun selectedText(): String? =
        selectionRange()?.let { value.value.substring(it.first, it.second) }

    private fun updateCharPositions(context: dev.unknownuser.ananda.backend.RenderContext, style: TextStyle) {
        if (!charPositionsDirty && charPositions.size == value.value.length + 1) return
        val positions = FloatArray(value.value.length + 1)
        for (index in value.value.indices) {
            positions[index + 1] = context.backend.measureText(value.value.substring(0, index + 1), style).first
        }
        charPositions = positions
        charPositionsDirty = false
        ensureCursorVisible()
    }

    private fun cursorFromPointer(pointerX: Float): Int {
        val relativeX = pointerX - x - padding.left + scrollOffsetX
        if (value.value.isEmpty()) return 0
        for (index in value.value.indices) {
            val mid = (charPositions[index] + charPositions[index + 1]) * 0.5f
            if (relativeX < mid) return index
        }
        return value.value.length
    }

    private fun ensureCursorVisible() {
        val visibleWidth = textViewportWidth()
        if (visibleWidth <= 0f || charPositions.isEmpty()) return
        val safeIndex = cursor.coerceIn(0, charPositions.lastIndex)
        val caretX = charPositions[safeIndex]
        scrollOffsetX = when {
            caretX - scrollOffsetX < 0f -> caretX
            caretX - scrollOffsetX > visibleWidth -> caretX - visibleWidth
            else -> scrollOffsetX
        }.coerceAtLeast(0f)
    }

    private fun textViewportWidth(): Float =
        (measuredWidth - padding.left - padding.right).coerceAtLeast(0f)
}

class Checkbox(
    val checked: State<Boolean> = stateOf(false),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 28f,
    height: Float = 28f
) : Component(x, y, width, height) {
    init {
        focusable = true
        checked.subscribe { requestRender() }
        on(PointerDown) {
            checked.value = !checked.value
            requestFocus()
            it.consume()
        }
    }

    override fun draw(context: dev.unknownuser.ananda.backend.RenderContext) {
        val theme = context.theme
        backgroundColor = if (checked.value) theme.palette.primary.withAlpha(170) else theme.palette.border.withAlpha(80)
        gradientColors = null
        cornerRadius = 3f
        elevationShadow = null
        borderColor = if (interaction.focused) theme.palette.primary.withAlpha(160) else null
        borderWidth = 1f
        super.draw(context)
        if (checked.value) {
            context.backend.translated(x, y) {
                context.backend.drawLine(7f, 14f, 12f, 19f, dev.unknownuser.ananda.backend.Stroke(theme.palette.text.withAlpha(230), 2f))
                context.backend.drawLine(12f, 19f, 22f, 8f, dev.unknownuser.ananda.backend.Stroke(theme.palette.text.withAlpha(230), 2f))
            }
        }
    }
}

class ToggleSwitch(
    val checked: State<Boolean> = stateOf(false),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 36f,
    height: Float = 20f
) : Component(x, y, width, height) {
    private val invalidate: () -> Unit = { requestRender() }
    private var hoverAmount = 0f
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

    override fun draw(context: dev.unknownuser.ananda.backend.RenderContext) {
        val theme = context.theme
        hoverAmount = animate(hoverAmount, if (interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        checkedAmount = animate(checkedAmount, if (checked.value) 1f else 0f, context.time.deltaSeconds, 18f)

        val trackColor = when {
            disabled -> theme.palette.border.withAlpha(60)
            else -> lerpColor(theme.palette.border.withAlpha(110 + (hoverAmount * 38f).toInt()), theme.palette.primary, checkedAmount)
        }
        val border = if (interaction.focused) Stroke(theme.palette.primary.withAlpha(170), 1f) else null
        val knobRadius = (measuredHeight - 8f).coerceAtLeast(4f) / 2f
        val knobX = knobRadius + 4f + checkedAmount * (measuredWidth - knobRadius * 2f - 8f)
        val knobColor = if (disabled) theme.palette.mutedText else Color.WHITE

        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(0f, 0f, measuredWidth, measuredHeight, measuredHeight / 2f, trackColor, border)
            context.backend.drawCircle(knobX, measuredHeight / 2f, knobRadius, knobColor)
        }
        if (isUnitAnimationActive(hoverAmount) || abs(checkedAmount - if (checked.value) 1f else 0f) > 0.001f) {
            requestRender()
        }
    }

    private fun toggle() {
        checked.value = !checked.value
    }
}

class ProgressBar(
    val progress: State<Float> = stateOf(0f),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 180f,
    height: Float = 12f
) : Component(x, y, width, height) {
    private val invalidate: () -> Unit = { requestRender() }
    private var displayProgress = progress.value.coerceIn(0f, 1f)

    init {
        progress.subscribe(invalidate)
        onDispose { progress.unsubscribe(invalidate) }
    }

    override fun draw(context: dev.unknownuser.ananda.backend.RenderContext) {
        val theme = context.theme
        val value = progress.value.coerceIn(0f, 1f)
        displayProgress = animate(displayProgress, value, context.time.deltaSeconds, 18f)
        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(0f, 0f, measuredWidth, measuredHeight, measuredHeight / 2f, theme.palette.border.withAlpha(75))
            if (displayProgress > 0f) {
                context.backend.drawRoundedRect(0f, 0f, measuredWidth * displayProgress, measuredHeight, measuredHeight / 2f, theme.palette.primary.withAlpha(210))
            }
        }
        if (abs(displayProgress - value) > 0.001f) requestRender()
    }
}

class RadioButton<T>(
    val selection: State<T>,
    val option: T,
    var text: String = option.toString(),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 160f,
    height: Float = 28f
) : Component(x, y, width, height) {
    private val invalidate: () -> Unit = { requestRender() }

    init {
        focusable = true
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

    override fun draw(context: dev.unknownuser.ananda.backend.RenderContext) {
        val theme = context.theme
        val selectedNow = selection.value == option
        val ringColor = when {
            interaction.focused -> theme.palette.accent
            selectedNow -> theme.palette.primary
            else -> theme.palette.border
        }
        val textColor = if (disabled) theme.palette.mutedText else theme.palette.text
        val centerY = measuredHeight / 2f

        context.backend.translated(x, y) {
            context.backend.drawCircle(10f, centerY, 7f, null, Stroke(ringColor.withAlpha(190), 1.4f))
            if (selectedNow) {
                context.backend.drawCircle(10f, centerY, 3.6f, theme.palette.primary)
            }
            context.backend.drawText(
                text,
                24f,
                centerTextBaseline(measuredHeight, 12.5f),
                TextStyle(12.5f, textColor, theme.typography.fontFamily)
            )
        }
    }

    private fun select() {
        selection.value = option
    }
}

enum class SeparatorOrientation {
    Horizontal,
    Vertical
}

class Separator(
    var orientation: SeparatorOrientation = SeparatorOrientation.Horizontal,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = if (orientation == SeparatorOrientation.Horizontal) 160f else 1f,
    height: Float = if (orientation == SeparatorOrientation.Horizontal) 1f else 160f
) : Component(x, y, width, height) {
    override fun draw(context: dev.unknownuser.ananda.backend.RenderContext) {
        val theme = context.theme
        context.backend.translated(x, y) {
            if (orientation == SeparatorOrientation.Horizontal) {
                context.backend.drawLine(0f, measuredHeight / 2f, measuredWidth, measuredHeight / 2f, Stroke(theme.palette.border.withAlpha(155), measuredHeight.coerceAtLeast(1f)))
            } else {
                context.backend.drawLine(measuredWidth / 2f, 0f, measuredWidth / 2f, measuredHeight, Stroke(theme.palette.border.withAlpha(155), measuredWidth.coerceAtLeast(1f)))
            }
        }
    }
}

class Slider(
    val value: State<Float> = stateOf(0f),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 160f,
    height: Float = 28f,
    private val valueFormatter: (Float) -> String = { "%.2f".format(it.coerceIn(0f, 1f)) }
) : Component(x, y, width, height) {
    private var hoverAmount = 0f
    private var pressAmount = 0f
    private var animatedValue = value.value.coerceIn(0f, 1f)
    private var valueGlowAmount = 0f
    private var pressStartX = 0f
    private var pendingClick = false
    private var dragging = false

    init {
        focusable = true
        value.subscribe { requestRender() }
        on(PointerDown) {
            val localX = sceneToLocal(it.x, it.y).first
            pressStartX = localX
            dragging = isOnKnob(localX)
            pendingClick = !dragging
            if (dragging) {
                updateFromPointer(localX, immediateVisual = true)
            }
            requestFocus()
            it.consume()
        }
        on(PointerMove) {
            if (interaction.pressed) {
                val localX = sceneToLocal(it.x, it.y).first
                if (dragging || abs(localX - pressStartX) >= DragThreshold) {
                    dragging = true
                    pendingClick = false
                    updateFromPointer(localX, immediateVisual = true)
                }
                it.consume()
            }
        }
        on(PointerUp) {
            if (pendingClick && !dragging) {
                updateFromPointer(sceneToLocal(it.x, it.y).first, immediateVisual = false)
            }
            pendingClick = false
            dragging = false
            if (interaction.pressed) {
                it.consume()
            }
        }
    }

    override fun draw(context: dev.unknownuser.ananda.backend.RenderContext) {
        val theme = context.theme
        val textStyle = TextStyle(11f, theme.palette.mutedText, theme.typography.fontFamily)
        val valueText = valueFormatter(value.value.coerceIn(0f, 1f))
        val valueWidth = context.backend.measureText(valueText, textStyle).first
        val trackWidth = sliderTrackWidth(measuredWidth)
        val trackY = measuredHeight / 2f
        hoverAmount = animate(hoverAmount, if (interaction.hovered) 1f else 0f, context.time.deltaSeconds, 14f)
        pressAmount = animate(pressAmount, if (interaction.pressed) 1f else 0f, context.time.deltaSeconds, 20f)
        val targetValue = value.value.coerceIn(0f, 1f)
        val distance = abs(targetValue - animatedValue)
        if (dragging) {
            animatedValue = targetValue
        } else {
            animatedValue = animate(animatedValue, targetValue, context.time.deltaSeconds, 20f)
        }
        valueGlowAmount = animate(valueGlowAmount, (distance * 4f).coerceIn(0f, 1f), context.time.deltaSeconds, 14f)
        val normalized = animatedValue.coerceIn(0f, 1f)
        val knobX = trackWidth * normalized
        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(0f, trackY - 1.5f, trackWidth, 3f, 1.5f, theme.palette.border.withAlpha(95))
            context.backend.drawRoundedRect(0f, trackY - 1.5f, (trackWidth * normalized).coerceAtLeast(3f), 3f, 1.5f, theme.palette.primary.withAlpha(190 + (valueGlowAmount * 45f).toInt()))
            val knobRadius = 4.5f + hoverAmount * 1.1f + pressAmount * 1.3f + valueGlowAmount * 0.8f
            val knobColor = lerpColor(
                theme.palette.primary,
                Color.WHITE,
                (hoverAmount * 0.3f + pressAmount * 0.35f + valueGlowAmount * 0.25f).coerceIn(0f, 1f)
            )
            context.backend.drawCircle(knobX, trackY, knobRadius, knobColor)
            context.backend.drawText(valueText, measuredWidth - valueWidth, centerTextBaseline(measuredHeight, 11f), textStyle)
        }
        if (isUnitAnimationActive(hoverAmount, pressAmount, valueGlowAmount) || abs(animatedValue - targetValue) > 0.001f) {
            requestRender()
        }
    }

    private fun updateFromPointer(localX: Float, immediateVisual: Boolean) {
        val trackWidth = sliderTrackWidth(measuredWidth)
        val nextValue = (localX / trackWidth).coerceIn(0f, 1f)
        value.value = nextValue
        if (immediateVisual) {
            animatedValue = nextValue
        }
    }

    private fun isOnKnob(localX: Float): Boolean {
        val trackWidth = sliderTrackWidth(measuredWidth)
        val knobX = value.value.coerceIn(0f, 1f) * trackWidth
        return abs(localX - knobX) <= KnobHitSlop
    }

    private fun sliderTrackWidth(width: Float): Float =
        (width - sliderReservedValueArea()).coerceAtLeast(36f)

    private fun sliderReservedValueArea(): Float = 44f

    companion object {
        private const val DragThreshold = 2f
        private const val KnobHitSlop = 10f
    }
}

private fun Color.withAlpha(alpha: Int): Color =
    Color(red, green, blue, alpha.coerceIn(0, 255))

private fun animate(current: Float, target: Float, deltaSeconds: Float, speed: Float): Float {
    val step = 1f - exp(-speed * deltaSeconds.coerceIn(0f, 0.1f))
    return current + (target - current) * step
}

private fun isUnitAnimationActive(vararg values: Float): Boolean =
    values.any { it > 0.001f && it < 0.999f }

private fun centerTextBaseline(height: Float, fontSize: Float): Float =
    height * 0.5f + fontSize * 0.36f

private fun lerpColor(from: Color, to: Color, progress: Float): Color {
    val t = progress.coerceIn(0f, 1f)
    return Color(
        (from.red + (to.red - from.red) * t).toInt().coerceIn(0, 255),
        (from.green + (to.green - from.green) * t).toInt().coerceIn(0, 255),
        (from.blue + (to.blue - from.blue) * t).toInt().coerceIn(0, 255),
        (from.alpha + (to.alpha - from.alpha) * t).toInt().coerceIn(0, 255)
    )
}

private fun Color.brighterSoft(amount: Int): Color =
    Color((red + amount).coerceAtMost(255), (green + amount).coerceAtMost(255), (blue + amount).coerceAtMost(255), alpha)

private fun Color.darkerSoft(amount: Int): Color =
    Color((red - amount).coerceAtLeast(0), (green - amount).coerceAtLeast(0), (blue - amount).coerceAtLeast(0), alpha)

class ScrollContainer(
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 0f,
    height: Float = 0f
) : Component(x, y, width, height) {
    var scrollY: Float = 0f
    var scrollStep: Float = 18f

    init {
        layout = ColumnLayout()
        clipToBounds = true
        on(PointerScroll) {
            scrollY = (scrollY - it.deltaY * scrollStep).coerceIn(0f, maxScrollY())
            requestRender()
            it.consume()
        }
    }

    override fun draw(context: dev.unknownuser.ananda.backend.RenderContext) {
        if (!visible) return
        measure(
            dev.unknownuser.ananda.layout.Constraints(
                resolvedMeasureWidth(context.width.toFloat()),
                resolvedMeasureHeight(context.height.toFloat())
            )
        )
        layout.layout(this, dev.unknownuser.ananda.layout.Constraints(measuredWidth, measuredHeight))
        scrollY = scrollY.coerceIn(0f, maxScrollY())
        context.backend.translated(x, y) {
            context.backend.clipped(0f, 0f, measuredWidth, measuredHeight) {
                context.backend.translated(0f, -scrollY) {
                    children.forEach { it.draw(context) }
                }
            }
        }
    }

    internal override fun pointerPath(globalX: Float, globalY: Float): List<Component> {
        val hitWidth = if (measuredWidth > 0f) measuredWidth else width
        val hitHeight = if (measuredHeight > 0f) measuredHeight else height
        if (!visible || disabled || globalX < x || globalY < y || globalX > x + hitWidth || globalY > y + hitHeight) {
            return emptyList()
        }
        val localX = globalX - x
        val localY = globalY - y + scrollY
        children.asReversed().forEach { child ->
            val childPath = child.pointerPath(localX, localY)
            if (childPath.isNotEmpty()) return listOf(this) + childPath
        }
        return listOf(this)
    }

    private fun maxScrollY(): Float =
        children.maxOfOrNull { it.y + (if (it.measuredHeight > 0f) it.measuredHeight else it.height) }
            ?.minus(measuredHeight)
            ?.coerceAtLeast(0f)
            ?: 0f
}
