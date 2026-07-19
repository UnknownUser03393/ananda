package dev.unknownuser.ananda.component

import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.RenderBackend
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextStyle
import dev.unknownuser.ananda.layout.Constraints
import dev.unknownuser.ananda.layout.Size
import dev.unknownuser.ananda.style.OverflowWrap
import dev.unknownuser.ananda.style.TextAlign
import dev.unknownuser.ananda.style.TextOverflow
import dev.unknownuser.ananda.style.WhiteSpace
import java.awt.Color
import kotlin.math.max

class Label(
    var text: String,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 0f,
    height: Float = 0f,
    var size: Float? = null,
    var color: Color? = null
) : Component(x, y, width, height) {
    override fun measure(constraints: Constraints): Size {
        applyStyle()
        val textSize = size ?: style?.textSize ?: 18f
        val letterSpacing = style?.letterSpacing ?: 0f
        val availableWidth = resolveMeasureTextWidth(constraints)
        val lines = estimateDisplayLines(textSize, letterSpacing, availableWidth)
        val lineHeight = style?.lineHeight ?: textSize * 1.4f
        val contentWidth = lines.maxOfOrNull { estimateTextWidth(it, textSize, letterSpacing) }?.plus(padding.left + padding.right) ?: 0f
        val contentHeight = max(textSize, lineHeight * lines.size) + padding.top + padding.bottom
        val measured = resolveSize(constraints, contentWidth, contentHeight)
        setMeasuredSize(measured)
        return measured
    }

    override fun draw(context: RenderContext) {
        if (!visible || text.isEmpty()) return
        applyStyle()
        val resolvedTheme = theme ?: context.theme
        val childContext = theme?.let { context.withTheme(it) } ?: context
        measure(Constraints(width.takeIf { it > 0f } ?: context.width.toFloat(), height.takeIf { it > 0f } ?: context.height.toFloat()))
        layout.layout(this, Constraints(measuredWidth, measuredHeight))
        childContext.backend.translated(x, y) {
            childContext.backend.withAlpha(opacity) {
                val stroke = borderColor?.takeIf { borderWidth > 0f }?.let { Stroke(it, borderWidth) }
                if (backgroundColor != null || stroke != null) {
                    childContext.backend.drawRect(0f, 0f, measuredWidth, measuredHeight, backgroundColor, stroke)
                }
                val textSize = size ?: style?.textSize ?: resolvedTheme.typography.bodySize
                val lineHeight = style?.lineHeight ?: textSize * 1.4f
                val textStyle = TextStyle(
                    textSize,
                    color ?: style?.foreground ?: resolvedTheme.palette.text,
                    style?.fontFamily ?: resolvedTheme.typography.fontFamily,
                    style?.fontWeight ?: 400,
                    style?.letterSpacing ?: 0f
                )
                val drawLines = displayLines(textStyle, maxTextWidth(), childContext.backend).map { line ->
                    resolveOverflowText(line, textStyle, maxTextWidth(), childContext.backend)
                }
                val drawTextBlock = {
                    drawLines.forEachIndexed { index, line ->
                        val textWidth = childContext.backend.measureText(line, textStyle).first
                        val textX = when (style?.textAlign ?: TextAlign.Start) {
                            TextAlign.Start -> padding.left
                            TextAlign.Center -> padding.left + (measuredWidth - padding.left - padding.right - textWidth) / 2f
                            TextAlign.End -> measuredWidth - padding.right - textWidth
                        }
                        val baseline = padding.top + textSize + index * lineHeight
                        childContext.backend.drawText(line, textX, baseline, textStyle)
                    }
                }
                if (shouldClipText()) {
                    childContext.backend.clipped(padding.left, padding.top, maxTextWidth(), max(0f, measuredHeight - padding.top - padding.bottom)) {
                        drawTextBlock()
                    }
                } else {
                    drawTextBlock()
                }
                children.sortedBy { it.zIndex }.forEach { it.draw(childContext) }
            }
        }
    }

    private fun logicalLines(): List<String> =
        when (style?.whiteSpace) {
            WhiteSpace.NoWrap -> listOf(text.replace(Regex("\\s+"), " "))
            WhiteSpace.Pre -> text.split('\n')
            else -> text.lines().map { it.replace(Regex("\\s+"), " ").trim() }
        }

    private fun displayLines(textStyle: TextStyle, maxWidth: Float, backend: RenderBackend): List<String> {
        val lines = logicalLines()
        if (style?.whiteSpace == WhiteSpace.NoWrap || style?.whiteSpace == WhiteSpace.Pre) return lines
        return lines.flatMap { line -> wrapLine(line, maxWidth, backend, textStyle) }
    }

    private fun estimateDisplayLines(textSize: Float, letterSpacing: Float, maxWidth: Float): List<String> {
        val lines = logicalLines()
        if (style?.whiteSpace == WhiteSpace.NoWrap || style?.whiteSpace == WhiteSpace.Pre) return lines
        return lines.flatMap { line -> wrapLineEstimated(line, maxWidth, textSize, letterSpacing) }
    }

    private fun maxTextWidth(): Float =
        max(0f, measuredWidth - padding.left - padding.right)

    private fun shouldClipText(): Boolean =
        clipToBounds || style?.textOverflow != null || style?.whiteSpace == WhiteSpace.NoWrap

    private fun wrapLine(line: String, maxWidth: Float, backend: RenderBackend, textStyle: TextStyle): List<String> {
        if (line.isEmpty() || maxWidth <= 0f || backend.measureText(line, textStyle).first <= maxWidth) return listOf(line)
        if (style?.overflowWrap == OverflowWrap.Anywhere) {
            return wrapAnywhere(line, maxWidth) { backend.measureText(it, textStyle).first }
        }
        return wrapWords(line, maxWidth) { backend.measureText(it, textStyle).first }
    }

    private fun wrapLineEstimated(line: String, maxWidth: Float, textSize: Float, letterSpacing: Float): List<String> {
        if (line.isEmpty() || maxWidth <= 0f || maxWidth == Float.MAX_VALUE || estimateTextWidth(line, textSize, letterSpacing) <= maxWidth) return listOf(line)
        if (style?.overflowWrap == OverflowWrap.Anywhere) {
            return wrapAnywhere(line, maxWidth) { estimateTextWidth(it, textSize, letterSpacing) }
        }
        return wrapWords(line, maxWidth) { estimateTextWidth(it, textSize, letterSpacing) }
    }

    private fun wrapWords(line: String, maxWidth: Float, widthOf: (String) -> Float): List<String> {
        val words = line.split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return listOf("")
        val result = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (current.isEmpty() || widthOf(candidate) <= maxWidth) {
                current = candidate
            } else {
                result += current
                current = word
            }
        }
        if (current.isNotEmpty()) result += current
        return result
    }

    private fun wrapAnywhere(line: String, maxWidth: Float, widthOf: (String) -> Float): List<String> {
        val result = mutableListOf<String>()
        var current = ""
        var index = 0
        while (index < line.length) {
            val codePoint = line.codePointAt(index)
            val glyph = line.substring(index, index + Character.charCount(codePoint))
            val candidate = current + glyph
            if (current.isNotEmpty() && widthOf(candidate) > maxWidth) {
                result += current
                current = glyph
            } else {
                current = candidate
            }
            index += Character.charCount(codePoint)
        }
        if (current.isNotEmpty()) result += current
        return result.ifEmpty { listOf("") }
    }

    private fun resolveMeasureTextWidth(constraints: Constraints): Float {
        val explicitWidth = width.takeIf { it > 0f } ?: style?.width
        val maxWidth = explicitWidth
            ?: style?.maxWidth
            ?: constraints.maxWidth.takeIf { it.isFinite() }
            ?: Float.MAX_VALUE
        return max(0f, maxWidth - padding.left - padding.right)
    }

    private fun estimateTextWidth(line: String, textSize: Float, letterSpacing: Float): Float =
        line.length * textSize * 0.55f + letterSpacing * (line.codePointCount(0, line.length) - 1).coerceAtLeast(0)

    private fun resolveOverflowText(line: String, textStyle: TextStyle, maxWidth: Float, backend: RenderBackend): String {
        if (maxWidth <= 0f) return ""
        if (style?.textOverflow != TextOverflow.Ellipsis) return line
        if (backend.measureText(line, textStyle).first <= maxWidth) return line
        val ellipsis = "..."
        val ellipsisWidth = backend.measureText(ellipsis, textStyle).first
        if (ellipsisWidth > maxWidth) return ""
        var low = 0
        var high = line.length
        while (low < high) {
            val mid = (low + high + 1) / 2
            val candidate = line.take(mid) + ellipsis
            if (backend.measureText(candidate, textStyle).first <= maxWidth) {
                low = mid
            } else {
                high = mid - 1
            }
        }
        return line.take(low) + ellipsis
    }
}
