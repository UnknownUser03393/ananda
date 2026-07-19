package dev.unknownuser.ananda.liquidbounce

import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.component.Label
import dev.unknownuser.ananda.layout.GridLayout
import dev.unknownuser.ananda.layout.Insets
import dev.unknownuser.ananda.layout.fr
import dev.unknownuser.ananda.layout.maxContent
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.stateOf
import dev.unknownuser.ananda.style.Style

/**
 * LiquidBounce `clickgui/setting/FloatSetting.svelte` — name / value / suffix + slider row.
 */
class LbFloatSetting(
    val name: String,
    val value: State<Float> = stateOf(0f),
    val rangeMin: Float = 0f,
    val rangeMax: Float = 1f,
    val suffix: String = "",
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 0f,
    height: Float = 0f,
    private val onChange: (Float) -> Unit = {}
) : Component(x, y, width, height) {
    private val valueLabel = Label(formatValue(value.value), height = 14f)

    init {
        clipToBounds = true
        minHeight = 46f
        padding = Insets(top = 8f, bottom = 4f)
        value.subscribe {
            valueLabel.text = formatValue(value.value)
            requestRender()
        }
        val columns = if (suffix.isNotEmpty()) {
            listOf(fr(), maxContent, maxContent)
        } else {
            listOf(fr(), maxContent)
        }
        val areas = if (suffix.isNotEmpty()) {
            listOf(
                listOf("name", "value", "suffix"),
                listOf("slider", "slider", "slider")
            )
        } else {
            listOf(
                listOf("name", "value"),
                listOf("slider", "slider")
            )
        }
        layout = GridLayout(columns = columns, columnGap = 5f, rowGap = 4f, areas = areas)

        add(
            Label(name, height = 14f).apply {
                gridArea("name")
                style(Style(textSize = 12f, foreground = LbPalette.ClickGuiTextDimmed))
            }
        )
        add(valueLabel.apply {
            gridArea("value")
            style(Style(textSize = 12f, foreground = LbPalette.ClickGuiTextDimmed))
        })
        if (suffix.isNotEmpty()) {
            add(
                Label(suffix, height = 14f).apply {
                    gridArea("suffix")
                    style(Style(textSize = 12f, foreground = LbPalette.ClickGuiTextDimmed))
                }
            )
        }
        add(
            LbRangeSlider(
                value = value,
                rangeMin = rangeMin,
                rangeMax = rangeMax,
                height = 14f,
                onChange = onChange
            ).apply {
                gridArea("slider")
            }
        )
    }

    private fun formatValue(raw: Float): String =
        when {
            rangeMax > 100f -> "%.1f".format(raw)
            rangeMax <= 0.1f -> "%.4f".format(raw)
            rangeMax <= 1f -> "%.3f".format(raw)
            else -> "%.2f".format(raw)
        }
}