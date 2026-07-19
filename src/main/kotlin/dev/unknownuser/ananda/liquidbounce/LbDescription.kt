package dev.unknownuser.ananda.liquidbounce

import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextStyle
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.layout.Positioning
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.stateOf

/**
 * LiquidBounce `clickgui/Description.svelte` — fixed tooltip to the right of the panel.
 */
class LbDescription(
    val anchor: State<LbDescriptionAnchor?> = stateOf(null),
    x: Float = 0f,
    y: Float = 0f
) : Component(x, y) {
    private val invalidate: () -> Unit = { requestRender() }

    init {
        positioning = Positioning.Fixed
        zIndex = 10_000
        anchor.subscribe(invalidate)
        onDispose { anchor.unsubscribe(invalidate) }
        render { context ->
            val data = anchor.value ?: return@render
            val textStyle = TextStyle(12f, LbPalette.ClickGuiText, "Inter")
            val lines = wrapText(data.text, 220f, context, textStyle)
            val lineHeight = 12f * 1.45f
            val textBlockHeight = lineHeight * lines.size
            val boxWidth = lines.maxOf { context.backend.measureText(it, textStyle).first } + 24f
            val boxHeight = textBlockHeight + 20f
            val preferredLeft = when (data.anchor) {
                LbDescriptionSide.Left -> data.x - boxWidth - 18f
                LbDescriptionSide.Right -> data.x + 18f
            }
            val left = preferredLeft.coerceIn(12f, context.width - boxWidth - 12f)
            val top = (data.y - boxHeight / 2f).coerceIn(12f, context.height - boxHeight - 12f)

            context.backend.translated(left, top) {
                context.backend.drawShadowedRoundedRect(
                    0f, 0f, boxWidth, boxHeight, 7f,
                    Shadow(LbPalette.withAlpha(java.awt.Color.BLACK, 32), blurRadius = 10f, offsetY = 4f)
                )
                context.backend.drawRoundedRect(
                    0f, 0f, boxWidth, boxHeight, 6f,
                    LbPalette.ClickGuiPanelElevated,
                    Stroke(LbPalette.ClickGuiBorder, 1f)
                )
                val firstBaseline = (boxHeight - textBlockHeight) / 2f + textStyle.size * 0.86f
                lines.forEachIndexed { index, line ->
                    context.backend.drawText(line, 12f, firstBaseline + lineHeight * index, textStyle)
                }
            }
        }
    }

    override fun draw(context: RenderContext) {
        if (anchor.value == null) return
        super.draw(context)
    }

    private fun wrapText(text: String, maxWidth: Float, context: RenderContext, style: TextStyle): List<String> {
        val words = text.split(' ')
        if (words.isEmpty()) return listOf(text)
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "${current} $word"
            val width = context.backend.measureText(candidate, style).first
            if (width > maxWidth && current.isNotEmpty()) {
                lines += current.toString()
                current = StringBuilder(word)
            } else {
                current = StringBuilder(candidate)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines.ifEmpty { listOf(text) }
    }
}
