package dev.unknownuser.ananda.liquidbounce

import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.component.Label
import dev.unknownuser.ananda.layout.Alignment
import dev.unknownuser.ananda.layout.RowLayout
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.stateOf
import dev.unknownuser.ananda.style.Style
import dev.unknownuser.ananda.style.TextOverflow
import dev.unknownuser.ananda.style.WhiteSpace

/**
 * LiquidBounce `clickgui/setting/BooleanSetting.svelte` + `Switch.svelte`.
 */
class LbBooleanSetting(
    val name: String,
    val value: State<Boolean> = stateOf(false),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 0f,
    height: Float = 0f,
    private val onChange: (Boolean) -> Unit = {}
) : Component(x, y, width, height) {
    init {
        clipToBounds = true
        padding(0f, 7f)
        layout = RowLayout(gap = 8f, crossAxisAlignment = Alignment.Center)
        add(LbSwitch(value, onChange = onChange))
        add(
            Label(name, height = 14f).apply {
                style(
                    Style(
                        textSize = 12f,
                        foreground = LbPalette.ClickGuiTextDimmed,
                        whiteSpace = WhiteSpace.NoWrap,
                        textOverflow = TextOverflow.Ellipsis
                    )
                )
            }
        )
    }
}