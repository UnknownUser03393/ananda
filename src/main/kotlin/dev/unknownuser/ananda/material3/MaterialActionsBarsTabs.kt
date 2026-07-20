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

