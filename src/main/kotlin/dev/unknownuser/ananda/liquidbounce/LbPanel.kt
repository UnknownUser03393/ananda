package dev.unknownuser.ananda.liquidbounce

import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.component.Label
import dev.unknownuser.ananda.component.ScrollContainer
import dev.unknownuser.ananda.event.PointerDown
import dev.unknownuser.ananda.layout.Alignment
import dev.unknownuser.ananda.layout.ColumnLayout
import dev.unknownuser.ananda.layout.Constraints
import dev.unknownuser.ananda.layout.GridLayout
import dev.unknownuser.ananda.layout.Insets
import dev.unknownuser.ananda.layout.MainAxisAlignment
import dev.unknownuser.ananda.layout.Positioning
import dev.unknownuser.ananda.layout.fr
import dev.unknownuser.ananda.layout.maxContent
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.stateOf
import dev.unknownuser.ananda.style.BackgroundLayer
import dev.unknownuser.ananda.style.BorderSides
import dev.unknownuser.ananda.style.GradientStop
import dev.unknownuser.ananda.style.Style
import java.awt.event.MouseEvent
import kotlin.math.min

/**
 * LiquidBounce `clickgui/Panel.svelte` — category header + collapsible module list.
 */
class LbPanel(
    val category: String,
    val expanded: State<Boolean> = stateOf(true),
    modules: List<Component>,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 270f,
    private val maxModulesHeight: Float = 320f
) : Component(x, y, width) {
    private lateinit var title: Component

    private val scroll = ScrollContainer(width = width).apply {
        background(LbPalette.withAlpha(LbPalette.ClickGuiBase, 72))
        layout = ColumnLayout(mainAxisAlignment = MainAxisAlignment.Start)
        modules.forEach { add(it) }
    }

    init {
        position(Positioning.Absolute)
        cornerRadius = 6f
        clipToBounds = true
        backdropBlur(10f)
        backgroundLayers = listOf(
            BackgroundLayer.Gradient(
                listOf(
                    GradientStop(LbPalette.withAlpha(LbPalette.ClickGuiPanelElevated, 68), 0f),
                    GradientStop(LbPalette.withAlpha(LbPalette.ClickGuiPanelElevated, 0), 0.42f)
                ),
                GradientDirection.Vertical
            ),
            BackgroundLayer.Solid(LbPalette.withAlpha(LbPalette.ClickGuiPanel, 238))
        )
        shadow(Shadow(LbPalette.withAlpha(java.awt.Color.BLACK, 52), blurRadius = 18f, offsetY = 10f))
        border(LbPalette.ClickGuiBorder, 1f)
        layout = ColumnLayout(mainAxisAlignment = MainAxisAlignment.Start)

        title = buildTitleRow()
        add(title)
        add(scroll)
        resizeScrollToContent(maxModulesHeight)

        expanded.subscribe {
            scroll.visible = expanded.value
            requestRender()
        }
        scroll.visible = expanded.value
    }

    fun resizeScrollToContent(maxHeight: Float) {
        updateScrollHeight(maxHeight)
        requestRender()
    }

    override fun draw(context: RenderContext) {
        updateScrollHeight(maxModulesHeight)
        super.draw(context)
    }

    private fun updateScrollHeight(maxHeight: Float) {
        scroll.measure(Constraints(width, Float.MAX_VALUE))
        scroll.layout.layout(scroll, Constraints(width, Float.MAX_VALUE))
        val contentHeight = scroll.children.maxOfOrNull { it.y + it.measuredHeight } ?: 0f
        scroll.height = min(maxHeight, contentHeight.coerceAtLeast(0f))
    }

    private fun buildTitleRow(): Component =
        Component(width = width, height = 36f).apply {
            clipToBounds = true
            padding = Insets.xy(12f, 10f)
            background(LbPalette.withAlpha(LbPalette.ClickGuiPanelElevated, 214))
            borderSides(BorderSides(bottom = Stroke(LbPalette.ClickGuiBorder, 1f)))
            layout = GridLayout(columns = listOf(maxContent, fr(), maxContent), columnGap = 10f, crossAxisAlignment = Alignment.Center)
            on(PointerDown) {
                if (it.button == MouseEvent.BUTTON3) {
                    expanded.value = !expanded.value
                    it.consume()
                }
            }
            add(
                Component(width = 15f, height = 15f).apply {
                    cornerRadius = 3f
                    background(LbPalette.withAlpha(LbPalette.Accent, 90))
                }
            )
            add(
                Label(category, height = 14f).apply {
                    style(Style(textSize = 12f, foreground = LbPalette.ClickGuiText, fontWeight = 700))
                }
            )
            add(
                Component(width = 22f, height = 22f).apply {
                    cornerRadius = 5f
                    focusable = true
                    on(PointerDown) {
                        expanded.value = !expanded.value
                        it.consume()
                    }
                    render { context ->
                        val color = LbPalette.ClickGuiTextDimmed
                        val cx = measuredWidth / 2f
                        val cy = measuredHeight / 2f
                        if (expanded.value) {
                            context.backend.drawLine(cx - 5f, cy, cx + 5f, cy, Stroke(color, 1.6f))
                        } else {
                            context.backend.drawLine(cx, cy - 5f, cx, cy + 5f, Stroke(color, 1.6f))
                            context.backend.drawLine(cx - 5f, cy, cx + 5f, cy, Stroke(color, 1.6f))
                        }
                    }
                }
            )
        }
}
