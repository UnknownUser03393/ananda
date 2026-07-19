package dev.unknownuser.ananda.component

import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextStyle
import dev.unknownuser.ananda.draw.OutsideClickBehavior
import dev.unknownuser.ananda.event.KeyDown
import dev.unknownuser.ananda.event.PointerDown
import dev.unknownuser.ananda.event.PointerMove
import dev.unknownuser.ananda.event.PointerUp
import dev.unknownuser.ananda.layout.ColumnLayout
import dev.unknownuser.ananda.layout.RowLayout
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.stateOf
import java.awt.Color
import java.awt.event.KeyEvent as AwtKeyEvent
import kotlin.math.abs
import kotlin.math.exp

class Dropdown<T>(
    var options: List<T>,
    val selection: State<T>,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 160f,
    height: Float = 28f,
    private val label: (T) -> String = { it.toString() }
) : Component(x, y, width, height) {
    private var popup: DropdownPopup<T>? = null
    private var hoverAmount = 0f
    private var openAmount = 0f
    private val invalidate: () -> Unit = { requestRender() }

    init {
        focusable = true
        padding(10f, 6f)
        selection.subscribe(invalidate)
        onDispose { selection.unsubscribe(invalidate) }
        on(PointerDown) {
            requestFocus()
            togglePopup()
            it.consume()
        }
        render { context ->
            val theme = context.theme
            val textColor = lerpColor(theme.palette.mutedText, theme.palette.text, hoverAmount)
            val style = TextStyle(theme.typography.bodySize, textColor, theme.typography.fontFamily)
            context.backend.drawText(label(selection.value), padding.left, centerTextBaseline(measuredHeight, style.size), style)
            val arrowX = measuredWidth - padding.right - 8f
            val arrowY = measuredHeight / 2f
            val arrow = if (popup == null) 1f else -1f
            context.backend.drawLine(arrowX - 4f, arrowY - 2f * arrow, arrowX, arrowY + 2f * arrow, Stroke(textColor, 1.3f))
            context.backend.drawLine(arrowX, arrowY + 2f * arrow, arrowX + 4f, arrowY - 2f * arrow, Stroke(textColor, 1.3f))
        }
    }

    override fun draw(context: RenderContext) {
        val theme = context.theme
        hoverAmount = animate(hoverAmount, if (interaction.hovered && !disabled) 1f else 0f, context.time.deltaSeconds, 14f)
        openAmount = animate(openAmount, if (popup != null) 1f else 0f, context.time.deltaSeconds, 18f)
        backgroundColor = lerpColor(theme.palette.surface.withAlpha(30), theme.palette.surface.withAlpha(58), hoverAmount)
        borderColor = lerpColor(theme.palette.border.withAlpha(120), theme.palette.primary.withAlpha(160), maxOf(hoverAmount, openAmount))
        borderWidth = 1f
        cornerRadius = 4f
        super.draw(context)
        if (isUnitAnimationActive(hoverAmount, openAmount)) requestRender()
    }

    private fun togglePopup() {
        val openPopup = popup
        if (openPopup != null) {
            ownerScene()?.popLayer(openPopup)
            popup = null
            return
        }
        val scene = ownerScene() ?: return
        val (globalX, globalY) = globalPosition()
        val newPopup = DropdownPopup(
            options = options,
            selection = selection,
            anchorX = globalX,
            anchorY = globalY,
            anchorWidth = measuredWidth.takeIf { it > 0f } ?: width,
            anchorHeight = measuredHeight.takeIf { it > 0f } ?: height,
            rowHeight = (height.takeIf { it > 0f } ?: 28f).coerceAtLeast(22f),
            label = label,
            onClosed = { popup = null }
        )
        popup = newPopup
        scene.pushOverlay(newPopup, outsideClickBehavior = OutsideClickBehavior.Custom { newPopup.close() })
    }
}

private class DropdownPopup<T>(
    private val options: List<T>,
    private val selection: State<T>,
    private val anchorX: Float,
    private val anchorY: Float,
    private val anchorWidth: Float,
    private val anchorHeight: Float,
    private val rowHeight: Float,
    private val label: (T) -> String,
    private val onClosed: () -> Unit
) : Component(anchorX, anchorY + anchorHeight, anchorWidth, options.size * rowHeight) {
    private var hoveredIndex = -1
    private var closing = false
    private var expandAmount = 0f

    init {
        clipToBounds()
        interpolationKey("dropdown-popup:${System.identityHashCode(this)}")
        on(PointerMove) {
            val (_, localY) = sceneToLocal(it.x, it.y)
            hoveredIndex = if (localY in 0f..height) {
                (localY / rowHeight).toInt().coerceIn(0, options.lastIndex)
            } else {
                -1
            }
            requestRender()
            it.consume()
        }
        on(PointerDown) {
            val index = (sceneToLocal(it.x, it.y).second / rowHeight).toInt()
            if (index in options.indices) {
                selection.value = options[index]
                close()
            }
            it.consume()
        }
    }

    fun close() {
        closing = true
        disabled = true
        requestRender()
    }

    override fun draw(context: RenderContext) {
        val target = if (closing) 0f else 1f
        expandAmount = animate(expandAmount, target, context.time.deltaSeconds, 20f)
        if (closing && expandAmount <= 0.01f) {
            ownerScene()?.popLayer(this)
            onClosed()
            return
        }

        val viewportHeight = context.height.toFloat()
        val popupHeight = options.size * rowHeight
        val belowY = anchorY + anchorHeight
        val aboveY = anchorY - popupHeight
        y = if (belowY + popupHeight > viewportHeight && aboveY >= 0f) aboveY else belowY
        height = (popupHeight * expandAmount).coerceAtLeast(0f)

        val theme = context.theme
        backgroundColor = theme.palette.surface.withAlpha((245 * expandAmount).toInt())
        borderColor = theme.palette.border.withAlpha((190 * expandAmount).toInt())
        borderWidth = 1f
        cornerRadius = 5f
        super.draw(context)

        context.backend.translated(x, y) {
            context.backend.clipped(0f, 0f, width, height) {
                options.forEachIndexed { index, option ->
                    val rowY = index * rowHeight
                    val selectedNow = selection.value == option
                    if (index == hoveredIndex || selectedNow) {
                        val fill = if (selectedNow) theme.palette.primary.withAlpha(120) else theme.palette.border.withAlpha(60)
                        context.backend.drawRoundedRect(4f, rowY + 2f, width - 8f, rowHeight - 4f, 3f, fill)
                    }
                    val color = if (selectedNow) theme.palette.text else theme.palette.mutedText
                    context.backend.drawText(label(option), 10f, centerTextBaseline(rowHeight, theme.typography.bodySize) + rowY, TextStyle(theme.typography.bodySize, color, theme.typography.fontFamily))
                }
            }
        }
        if (isUnitAnimationActive(expandAmount)) requestRender()
    }
}

class KeyBind(
    val keyCode: State<Int?> = stateOf(null),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 120f,
    height: Float = 28f
) : Component(x, y, width, height) {
    private var listening = false
    private var hoverAmount = 0f

    init {
        focusable = true
        on(PointerDown) {
            listening = true
            requestFocus()
            requestRender()
            it.consume()
        }
        on(KeyDown) {
            if (!listening) return@on
            keyCode.value = when (it.keyCode) {
                AwtKeyEvent.VK_ESCAPE, AwtKeyEvent.VK_BACK_SPACE, AwtKeyEvent.VK_DELETE -> null
                else -> it.keyCode
            }
            listening = false
            it.consume()
            requestRender()
        }
        render { context ->
            val theme = context.theme
            val label = when {
                listening -> "Press key..."
                keyCode.value == null -> "Unbound"
                else -> AwtKeyEvent.getKeyText(keyCode.value ?: 0)
            }
            val color = if (listening) theme.palette.primary else lerpColor(theme.palette.mutedText, theme.palette.text, hoverAmount)
            val style = TextStyle(theme.typography.bodySize, color, theme.typography.fontFamily)
            val textWidth = context.backend.measureText(label, style).first
            context.backend.drawText(label, (measuredWidth - textWidth) / 2f, centerTextBaseline(measuredHeight, style.size), style)
        }
    }

    override fun draw(context: RenderContext) {
        val theme = context.theme
        hoverAmount = animate(hoverAmount, if (interaction.hovered || listening) 1f else 0f, context.time.deltaSeconds, 14f)
        backgroundColor = theme.palette.surface.withAlpha(30 + (hoverAmount * 28f).toInt())
        borderColor = lerpColor(theme.palette.border.withAlpha(130), theme.palette.primary.withAlpha(170), hoverAmount)
        borderWidth = 1f
        cornerRadius = 4f
        super.draw(context)
        if (isUnitAnimationActive(hoverAmount)) requestRender()
    }
}

class ColorPicker(
    val color: State<Color> = stateOf(Color(0xE0, 0x35, 0x35)),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 176f,
    height: Float = 32f
) : Component(x, y, width, height) {
    private var hoverAmount = 0f

    init {
        focusable = true
        on(PointerDown) {
            requestFocus()
            openDialog()
            it.consume()
        }
        render { context ->
            val theme = context.theme
            context.backend.drawRoundedRect(6f, 6f, measuredHeight - 12f, measuredHeight - 12f, 4f, color.value, Stroke(Color.WHITE.withAlpha(55), 1f))
            val hex = "#%06X".format(color.value.rgb and 0xFFFFFF)
            context.backend.drawText(hex, measuredHeight, centerTextBaseline(measuredHeight, theme.typography.bodySize), TextStyle(theme.typography.bodySize, lerpColor(theme.palette.mutedText, theme.palette.text, hoverAmount), theme.typography.fontFamily))
        }
    }

    override fun draw(context: RenderContext) {
        val theme = context.theme
        hoverAmount = animate(hoverAmount, if (interaction.hovered) 1f else 0f, context.time.deltaSeconds, 14f)
        backgroundColor = theme.palette.surface.withAlpha(28 + (hoverAmount * 24f).toInt())
        borderColor = lerpColor(theme.palette.border.withAlpha(120), theme.palette.primary.withAlpha(160), hoverAmount)
        borderWidth = 1f
        cornerRadius = 5f
        super.draw(context)
        if (isUnitAnimationActive(hoverAmount)) requestRender()
    }

    private fun openDialog() {
        val scene = ownerScene() ?: return
        val dialog = ColorPickerDialog(color)
        scene.pushOverlay(dialog, outsideClickBehavior = OutsideClickBehavior.Custom { dialog.close() }, transparent = false)
    }
}

private class ColorPickerDialog(
    private val color: State<Color>
) : Component(width = 240f, height = 172f) {
    private val pending = stateOf(color.value)
    private val hex = stateOf("%06X".format(color.value.rgb and 0xFFFFFF))
    private val picker = InlineColorPicker(pending, 14f, 40f, 212f, 66f)
    private var closing = false
    private var openAmount = 0f

    init {
        interpolationKey("color-dialog:${System.identityHashCode(this)}")
        padding(14f)
        add(Label("Color", 14f, 12f, 120f, 18f, 13f))
        add(picker)
        add(TextField(hex, 14f, 114f, 132f, 26f, "RRGGBB"))
        add(Button("Cancel", 76f, 146f, 72f, 22f) { close() })
        add(Button("Apply", 154f, 146f, 72f, 22f) {
            parseHex(hex.value)?.let { color.value = it }
            close()
        })
        pending.subscribe {
            hex.value = "%06X".format(pending.value.rgb and 0xFFFFFF)
        }
        hex.subscribe {
            parseHex(hex.value)?.let { pending.value = it }
        }
    }

    fun close() {
        closing = true
        disabled = true
        requestRender()
    }

    override fun draw(context: RenderContext) {
        val target = if (closing) 0f else 1f
        openAmount = animate(openAmount, target, context.time.deltaSeconds, 18f)
        if (closing && openAmount <= 0.02f) {
            ownerScene()?.popLayer(this)
            return
        }
        x = ((context.width - width) / 2f).coerceAtLeast(0f)
        y = ((context.height - height) / 2f).coerceAtLeast(0f) + (1f - openAmount) * 12f
        backgroundColor = context.theme.palette.surface.withAlpha((246 * openAmount).toInt())
        borderColor = context.theme.palette.border.withAlpha((210 * openAmount).toInt())
        borderWidth = 1f
        cornerRadius = 8f
        elevationShadow = dev.unknownuser.ananda.backend.Shadow(Color(0, 0, 0, (72 * openAmount).toInt()), 10f, 0f, 5f)
        super.draw(context)
        if (isUnitAnimationActive(openAmount)) requestRender()
    }

    private fun parseHex(input: String): Color? {
        val normalized = input.trim().removePrefix("#")
        if (normalized.length != 6 && normalized.length != 8) return null
        if (!normalized.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
        return runCatching {
            val raw = normalized.toLong(16)
            if (normalized.length == 6) Color((0xFF000000L or raw).toInt(), true) else Color(raw.toInt(), true)
        }.getOrNull()
    }
}

private class InlineColorPicker(
    private val color: State<Color>,
    x: Float,
    y: Float,
    width: Float,
    height: Float
) : Component(x, y, width, height) {
    private var hue = 0f
    private var saturation = 1f
    private var brightness = 1f
    private var dragging = Drag.None

    private enum class Drag { None, Hue, Saturation, Brightness }

    init {
        initFromColor(color.value)
        on(PointerDown) {
            val (localX, localY) = sceneToLocal(it.x, it.y)
            dragging = stripAt(localY)
            if (dragging != Drag.None) {
                updateFromPointer(localX)
                it.consume()
            }
        }
        on(PointerMove) {
            if (dragging != Drag.None && interaction.pressed) {
                updateFromPointer(sceneToLocal(it.x, it.y).first)
                it.consume()
            }
        }
        on(PointerUp) {
            dragging = Drag.None
            it.consume()
        }
    }

    override fun draw(context: RenderContext) {
        val stripHeight = 6f
        val gap = 13f
        val stripWidth = (measuredWidth - 34f).coerceAtLeast(20f)
        val startX = 0f
        listOf(Drag.Hue, Drag.Saturation, Drag.Brightness).forEachIndexed { index, strip ->
            val y = index * (stripHeight + gap)
            val segments = if (strip == Drag.Hue) 80 else 48
            for (segment in 0 until segments) {
                val t0 = segment / segments.toFloat()
                val color0 = when (strip) {
                    Drag.Hue -> Color(Color.HSBtoRGB(t0, 1f, 1f))
                    Drag.Saturation -> Color(Color.HSBtoRGB(hue, t0, brightness))
                    Drag.Brightness -> Color(Color.HSBtoRGB(hue, saturation, t0))
                    Drag.None -> Color.BLACK
                }
                context.backend.drawRect(startX + stripWidth * t0, y, stripWidth / segments + 0.5f, stripHeight, color0)
            }
            val normalized = when (strip) {
                Drag.Hue -> hue
                Drag.Saturation -> saturation
                Drag.Brightness -> brightness
                Drag.None -> 0f
            }
            val knobX = startX + normalized * stripWidth
            context.backend.drawCircle(knobX, y + stripHeight / 2f, 5f, Color.WHITE, Stroke(Color(0, 0, 0, 90), 1f))
        }
        context.backend.drawCircle(measuredWidth - 12f, 25f, 10f, color.value, Stroke(Color.WHITE.withAlpha(70), 1f))
    }

    private fun stripAt(localY: Float): Drag =
        when {
            localY in -4f..10f -> Drag.Hue
            localY in 15f..29f -> Drag.Saturation
            localY in 34f..48f -> Drag.Brightness
            else -> Drag.None
        }

    private fun updateFromPointer(localX: Float) {
        val stripWidth = (measuredWidth - 34f).coerceAtLeast(20f)
        val t = (localX / stripWidth).coerceIn(0f, 1f)
        when (dragging) {
            Drag.Hue -> hue = t
            Drag.Saturation -> saturation = t
            Drag.Brightness -> brightness = t
            Drag.None -> Unit
        }
        color.value = Color(Color.HSBtoRGB(hue, saturation, brightness))
        requestRender()
    }

    private fun initFromColor(value: Color) {
        val hsb = Color.RGBtoHSB(value.red, value.green, value.blue, null)
        hue = hsb[0]
        saturation = hsb[1]
        brightness = hsb[2]
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
