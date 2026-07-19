package dev.unknownuser.ananda.liquidbounce

import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextStyle
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.component.Label
import dev.unknownuser.ananda.component.ScrollContainer
import dev.unknownuser.ananda.component.TextField
import dev.unknownuser.ananda.event.PointerDown
import dev.unknownuser.ananda.layout.ColumnLayout
import dev.unknownuser.ananda.layout.Alignment
import dev.unknownuser.ananda.layout.Positioning
import dev.unknownuser.ananda.layout.RowLayout
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.stateOf
import dev.unknownuser.ananda.style.BorderSides
import dev.unknownuser.ananda.style.Style
import dev.unknownuser.ananda.style.TransformOffset

data class LbSearchModule(
    val name: String,
    val enabled: State<Boolean>,
    val aliases: List<String> = emptyList()
)

/**
 * LiquidBounce `clickgui/Search.svelte` — centered search bar with filtered module results.
 */
class LbSearch(
    val modules: List<LbSearchModule>,
    val query: State<String> = stateOf(""),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 560f,
    private val onToggleModule: (String, Boolean) -> Unit = { _, _ -> },
    private val onHighlightModule: (String?) -> Unit = {}
) : Component(x, y, width) {
    private var selectedIndex = 0
    private val resultsContainer = Component(width = width)
    private val scroll = ScrollContainer(width = width, height = 250f)
    private val searchField = TextField(query, width = width, height = 44f, placeholder = "Search")

    init {
        positioning = Positioning.Fixed
        leftPercent = 0.5f
        top = if (y > 0f) y else 72f
        translate = TransformOffset(xPercent = -0.5f)
        backdropBlur(10f)
        cornerRadius = 6f
        clipToBounds = true
        background(LbPalette.withAlpha(LbPalette.ClickGuiPanel, 238))
        shadow(Shadow(LbPalette.withAlpha(java.awt.Color.BLACK, 50), blurRadius = 16f, offsetY = 9f))
        border(LbPalette.ClickGuiBorder, 1f)
        layout = ColumnLayout()
        searchField.apply {
            padding(16f, 12f)
            background(null)
            borderColor = null
            cornerRadius = 0f
        }
        query.subscribe { rebuildResults(resetIndex = true) }
        add(searchField)
        add(resultsContainer)
        rebuildResults(resetIndex = true)
    }

    private fun rebuildResults(resetIndex: Boolean) {
        resultsContainer.clear()
        val q = query.value.trim()
        if (q.isEmpty()) {
            resultsContainer.visible = false
            requestRender()
            return
        }
        val pure = q.lowercase().replace(" ", "")
        val filtered = modules.filter { module ->
            module.name.lowercase().contains(pure) ||
                module.aliases.any { it.lowercase().contains(pure) }
        }
        if (resetIndex) selectedIndex = 0
        resultsContainer.visible = true
        resultsContainer.borderSides(BorderSides(top = Stroke(LbPalette.ClickGuiBorder, 1f)))
        resultsContainer.layout = ColumnLayout()
        scroll.clear()
        scroll.height = 250f
        if (filtered.isEmpty()) {
            scroll.add(
                Label("No modules found", height = 14f).apply {
                    padding(12f, 10f)
                    style(Style(textSize = 12f, foreground = LbPalette.ClickGuiTextDimmed))
                }
            )
        } else {
            filtered.forEachIndexed { index, module ->
                scroll.add(buildResultRow(module, index))
            }
        }
        resultsContainer.add(scroll)
        requestRender()
    }

    private fun buildResultRow(module: LbSearchModule, index: Int): Component =
        Component(width = width, height = 34f).apply {
            padding(12f, 9f)
            cornerRadius = 6f
            layout = RowLayout(gap = 10f, crossAxisAlignment = Alignment.Center)
            module.enabled.subscribe { requestRender() }
            on(PointerDown) {
                val next = !module.enabled.value
                module.enabled.value = next
                onToggleModule(module.name, next)
                it.consume()
            }
            render { context ->
                if (index == selectedIndex) {
                    background(LbPalette.withAlpha(LbPalette.Accent, 24))
                } else if (interaction.hovered) {
                    background(LbPalette.withAlpha(java.awt.Color.WHITE, 9))
                } else {
                    backgroundColor = null
                }
            }
            add(
                Label(module.name, height = 14f).apply {
                    style(
                        Style(
                            textSize = 12f,
                            foreground = if (module.enabled.value) LbPalette.Accent else LbPalette.ClickGuiTextDimmed
                        )
                    )
                }
            )
            if (module.aliases.isNotEmpty()) {
                val aka = module.aliases.joinToString(", ")
                add(
                    Label("(aka $aka)", height = 14f).apply {
                        style(Style(textSize = 12f, foreground = LbPalette.withAlpha(LbPalette.ClickGuiTextDimmed, 153)))
                    }
                )
            }
        }

    override fun draw(context: RenderContext) {
        super.draw(context)
    }
}
