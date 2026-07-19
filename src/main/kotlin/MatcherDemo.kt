package dev.unknownuser

import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextStyle
import dev.unknownuser.ananda.component.Button
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.component.ToggleSwitch
import dev.unknownuser.ananda.draw.Scene
import dev.unknownuser.ananda.event.KeyDown
import dev.unknownuser.ananda.event.PointerDown
import dev.unknownuser.ananda.event.digitValue
import dev.unknownuser.ananda.event.or
import dev.unknownuser.ananda.layout.Alignment
import dev.unknownuser.ananda.layout.ColumnLayout
import dev.unknownuser.ananda.layout.RowLayout
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.stateOf
import dev.unknownuser.ananda.theme.Palette
import dev.unknownuser.ananda.theme.Theme
import dev.unknownuser.ananda.window.SkiaWindow
import java.awt.Color

fun main() {
    val inputDisabled = stateOf(false)
    val activations = stateOf(0)
    val pointerDowns = stateOf(0)
    val lastDigit = stateOf("-")
    val target = MatcherDemoTarget(
        inputDisabled = inputDisabled,
        activations = activations,
        pointerDowns = pointerDowns,
        lastDigit = lastDigit,
        width = 420f,
        height = 172f
    )

    val scene = Scene().apply {
        theme = Theme(
            palette = Palette(
                background = Color(20, 21, 24),
                surface = Color(31, 34, 40),
                primary = Color(82, 168, 255),
                accent = Color(255, 198, 76),
                danger = Color(224, 86, 86),
                text = Color(245, 247, 250),
                mutedText = Color(148, 157, 171),
                border = Color(68, 74, 86)
            )
        )
        add(
            Component(width = 520f, height = 360f).apply {
                layout = ColumnLayout(crossAxisAlignment = Alignment.Center)
                add(Component(width = 520f, height = 72f))
                add(target)
                add(Component(width = 520f, height = 32f))
                add(
                    Component(width = 420f, height = 34f).apply {
                        layout = RowLayout(gap = 12f, crossAxisAlignment = Alignment.Center)
                        add(Button("Reset", width = 92f, height = 34f) {
                            activations.value = 0
                            pointerDowns.value = 0
                            lastDigit.value = "-"
                        })
                        add(ToggleSwitch(inputDisabled, width = 48f, height = 26f))
                        add(DemoLabel(inputDisabled, width = 260f, height = 28f))
                    }
                )
            }
        )
    }

    SkiaWindow(scene = scene, width = 520, height = 360).show()
}

private class MatcherDemoTarget(
    private val inputDisabled: State<Boolean>,
    private val activations: State<Int>,
    private val pointerDowns: State<Int>,
    private val lastDigit: State<String>,
    width: Float,
    height: Float
) : Component(width = width, height = height) {
    private val invalidate: () -> Unit = {
        disabled = inputDisabled.value
        requestRender()
    }

    init {
        focusable = true
        disabled = inputDisabled.value
        inputDisabled.subscribe(invalidate)
        activations.subscribe(invalidate)
        pointerDowns.subscribe(invalidate)
        lastDigit.subscribe(invalidate)
        onDispose {
            inputDisabled.unsubscribe(invalidate)
            activations.unsubscribe(invalidate)
            pointerDowns.unsubscribe(invalidate)
            lastDigit.unsubscribe(invalidate)
        }

        on(PointerDown) {
            pointerDowns.value += 1
            requestFocus()
            it.consume()
        }

        on(KeyDown.Enter or KeyDown.Space) {
            activations.value += 1
            it.consume()
        }

        on(KeyDown.Digit) {
            lastDigit.value = it.digitValue()?.toString() ?: "-"
            it.consume()
        }

        render { context ->
            drawDemo(context)
        }
    }

    private fun drawDemo(context: RenderContext) {
        val theme = context.theme
        val fill = if (disabled) Color(40, 42, 48) else Color(35, 44, 56)
        val border = if (interaction.focused) theme.palette.accent else theme.palette.border
        context.backend.drawRoundedRect(
            0f,
            0f,
            measuredWidth,
            measuredHeight,
            8f,
            fill,
            Stroke(border, 1.4f)
        )

        val titleColor = if (disabled) theme.palette.mutedText else theme.palette.text
        val bodyColor = if (disabled) theme.palette.mutedText else Color(198, 207, 221)
        val mono = theme.typography.fontFamily
        context.backend.drawText("Matcher target", 20f, 34f, TextStyle(18f, titleColor, mono))
        context.backend.drawText("PointerDown: ${pointerDowns.value}", 20f, 70f, TextStyle(13f, bodyColor, mono))
        context.backend.drawText("KeyDown.Enter or KeyDown.Space: ${activations.value}", 20f, 96f, TextStyle(13f, bodyColor, mono))
        context.backend.drawText("KeyDown.Digit: ${lastDigit.value}", 20f, 122f, TextStyle(13f, bodyColor, mono))
        context.backend.drawText(
            if (disabled) "disabled: input events are filtered before handlers" else "click to focus, then press Enter / Space / 0-9",
            20f,
            150f,
            TextStyle(12f, theme.palette.mutedText, mono)
        )
    }
}

private class DemoLabel(
    private val inputDisabled: State<Boolean>,
    width: Float,
    height: Float
) : Component(width = width, height = height) {
    private val invalidate: () -> Unit = { requestRender() }

    init {
        inputDisabled.subscribe(invalidate)
        onDispose { inputDisabled.unsubscribe(invalidate) }
        render { context ->
            context.backend.drawText(
                if (inputDisabled.value) "Target disabled" else "Target enabled",
                0f,
                20f,
                TextStyle(13f, context.theme.palette.text, context.theme.typography.fontFamily)
            )
        }
    }
}
