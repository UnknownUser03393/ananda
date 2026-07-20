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

