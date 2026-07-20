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
    private var checkProgress = if (checked.value) 1f else 0f

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
        checkProgress = animate(checkProgress, if (checked.value) 1f else 0f, context.time.deltaSeconds, 20f)
        val boxSize = 18f
        val boxY = (measuredHeight - boxSize) / 2f
        val boxColor = lerp(overlay(p.surface, p.onSurface, hover * 0.04f, 1f), p.primary, checkProgress)
        val strokeColor = lerp(p.outline, p.primary, maxOf(checkProgress, if (interaction.focused) 1f else 0f))
        context.backend.translated(x, y) {
            context.backend.drawRoundedRect(0f, boxY, boxSize, boxSize, 2f, boxColor, Stroke(strokeColor.withAlpha(if (disabled) 90 else 255), 2f))
            if (checkProgress > 0.001f) {
                val mark = if (disabled) p.onSurface.withAlpha(150) else p.onPrimary
                val alpha = (checkProgress * 255f).toInt().coerceIn(0, 255)
                context.backend.drawLine(4f, boxY + 9f, 8f, boxY + 13f, Stroke(mark.withAlpha(alpha), 2f))
                context.backend.drawLine(8f, boxY + 13f, 14f, boxY + 5f, Stroke(mark.withAlpha(alpha), 2f))
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
        if (isAnimating(hover, focused) || abs(checkProgress - if (checked.value) 1f else 0f) > 0.001f) requestRender()
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
    private var selectionProgress = if (selection.value == option) 1f else 0f

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
        selectionProgress = animate(selectionProgress, if (selection.value == option) 1f else 0f, context.time.deltaSeconds, 18f)
        val selectedNow = selection.value == option
        val centerY = measuredHeight / 2f
        val ringColor = when {
            disabled -> p.onSurface.withAlpha(90)
            selectedNow || interaction.focused -> p.primary
            else -> p.outline
        }
        context.backend.translated(x, y) {
            context.backend.drawCircle(10f, centerY, 9f, overlay(p.surface, p.onSurface, hover * 0.03f, 1f), Stroke(ringColor, 2f))
            if (selectionProgress > 0.001f) {
                val dotAlpha = (selectionProgress * 255f).toInt().coerceIn(0, 255)
                val dotRadius = 4.5f * selectionProgress.coerceAtLeast(0.55f)
                context.backend.drawCircle(10f, centerY, dotRadius, if (disabled) p.onSurface.withAlpha(110).withAlpha(dotAlpha) else p.primary.withAlpha(dotAlpha))
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
        if (isAnimating(hover, focused) || abs(selectionProgress - if (selectedNow) 1f else 0f) > 0.001f) requestRender()
    }

    private fun select() {
        if (!disabled) selection.value = option
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

