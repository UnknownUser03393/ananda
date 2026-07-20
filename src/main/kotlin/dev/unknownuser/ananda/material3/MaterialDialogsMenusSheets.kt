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

