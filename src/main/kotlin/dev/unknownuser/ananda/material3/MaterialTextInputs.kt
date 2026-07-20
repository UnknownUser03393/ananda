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
    private var menuProgress = if (expanded.value) 1f else 0f

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
    private var composingText = ""
    private var fieldHover = 0f
    private var fieldFocused = 0f
    private var menuProgress = if (expanded.value) 1f else 0f

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
                        composingText = ""
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
            insertText(it.text)
            it.consume()
        }
        on(ImeCompose) {
            if (disabled) return@on
            composingText = it.composedText
            expanded.value = true
            requestRender()
        }
        on(ImeCommit) {
            if (disabled) return@on
            insertText(it.committedText)
            composingText = ""
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
        menuProgress = animate(menuProgress, if (expanded.value) 1f else 0f, context.time.deltaSeconds, 18f)
        val stateAlpha = 0.03f * fieldHover + 0.04f * fieldFocused
        val base = if (variant == MaterialTextFieldStyle.Filled) p.surfaceContainerHighest else p.surface
        val fieldBackground = overlay(base, p.onSurface, stateAlpha, if (disabled) 0.38f else 1f)
        val expandedNow = menuProgress > 0.001f
        val fieldBorder = when (variant) {
            MaterialTextFieldStyle.Filled -> if (interaction.focused || expandedNow) p.primary else p.outlineVariant.withAlpha(140)
            MaterialTextFieldStyle.Outlined -> if (interaction.focused || expandedNow) p.primary else p.outline
        }
        val filtered = filteredOptions()
        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(0f, 0f, measuredWidth, 56f, MaterialShapes.ExtraSmall, fieldBackground, Stroke(fieldBorder, if (interaction.focused || expandedNow) 2f else 1f))
            val bodySize = context.theme.typography.bodySize
            val labelSize = (bodySize - 2f).coerceAtLeast(10f)
            if (label.isNotBlank()) {
                val labelColor = if (interaction.focused || expandedNow) p.primary else p.onSurfaceVariant
                context.backend.drawText(label, 16f, 16f, TextStyle(labelSize, labelColor.withAlpha(if (disabled) 110 else 255), context.theme.typography.fontFamily))
            }
            val displayValue = value.value + composingText
            val text = if (displayValue.isBlank()) placeholder else displayValue
            val color = when {
                disabled -> p.onSurface.withAlpha(97)
                displayValue.isNotBlank() -> p.onSurface
                else -> p.onSurfaceVariant
            }
            val textStyle = TextStyle(bodySize, color, context.theme.typography.fontFamily)
            val baseline = if (label.isNotBlank()) 40f else textBaseline(56f, bodySize)
            context.backend.drawText(text.takeFitting(context, textStyle, measuredWidth - 44f), 16f, baseline, textStyle)
            val arrow = if (expandedNow) "▴" else "▾"
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
            if (menuProgress > 0.001f && filtered.isNotEmpty()) {
                val bounds = menuBounds(filtered)
                val menuHeight = (bounds.height * menuProgress).coerceAtLeast(16f)
                val visibleBounds = DialogBounds(bounds.x, bounds.y, bounds.width, menuHeight)
                context.backend.drawRoundedRect(visibleBounds.x, visibleBounds.y, visibleBounds.width, visibleBounds.height, MaterialShapes.Medium, p.surfaceContainerHigh, Stroke(p.outlineVariant, 1f))
                val visibleRows = ((filtered.size * menuProgress) + 0.999f).toInt().coerceAtMost(filtered.size)
                filtered.take(visibleRows).forEachIndexed { index, option ->
                    val top = visibleBounds.y + 8f + index * 40f
                    val selected = value.value == option
                    val active = hoverIndex == index || pressedIndex == index || selected
                    if (active) {
                        context.backend.drawRoundedRect(
                            visibleBounds.x + 8f,
                            top,
                            visibleBounds.width - 16f,
                            32f,
                            MaterialShapes.Small,
                            if (selected) p.secondaryContainer else overlay(p.surfaceContainerHigh, p.onSurface, if (pressedIndex == index) 0.10f else 0.06f, 1f),
                            null
                        )
                    }
                    val optionColor = if (selected) p.onSecondaryContainer else p.onSurface
                    val optionStyle = TextStyle(context.theme.typography.bodySize, optionColor, context.theme.typography.fontFamily)
                    context.backend.drawText(option.takeFitting(context, optionStyle, visibleBounds.width - 32f), visibleBounds.x + 16f, top + textBaseline(32f, optionStyle.size), optionStyle)
                }
            }
        }
        if (isAnimating(fieldHover, fieldFocused) || abs(menuProgress - if (expanded.value) 1f else 0f) > 0.001f) requestRender()
    }

    private fun filteredOptions(): List<String> {
        val query = (value.value + composingText).trim()
        val matches = if (query.isBlank()) options else options.filter { option -> option.contains(query, ignoreCase = true) }
        return matches.take(maxVisibleOptions.coerceAtLeast(1))
    }

    private fun insertText(text: String) {
        val safeCursor = cursor.coerceIn(0, value.value.length)
        value.value = value.value.substring(0, safeCursor) + text + value.value.substring(safeCursor)
        cursor = safeCursor + text.length
        expanded.value = true
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
                    composingText = ""
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

