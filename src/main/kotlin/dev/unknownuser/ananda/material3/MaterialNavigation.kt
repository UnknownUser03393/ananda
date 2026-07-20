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

