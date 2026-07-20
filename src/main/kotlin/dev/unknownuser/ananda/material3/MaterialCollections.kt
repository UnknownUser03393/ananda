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

