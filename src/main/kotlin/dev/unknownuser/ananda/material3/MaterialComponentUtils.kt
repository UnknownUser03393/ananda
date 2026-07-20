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

internal fun String.wrapText(context: RenderContext, style: TextStyle, maxWidth: Float, maxLines: Int): List<String> {
    if (isBlank()) return emptyList()
    val words = trim().split(Regex("\\s+"))
    if (words.isEmpty()) return emptyList()
    val lines = mutableListOf<String>()
    var current = ""
    for (word in words) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (context.backend.measureText(candidate, style).first <= maxWidth) {
            current = candidate
            continue
        }
        if (current.isNotEmpty()) {
            lines += current
            if (lines.size == maxLines) return lines.dropLast(1) + lines.last().takeFitting(context, style, maxWidth)
        }
        current = word
        if (lines.size == maxLines) break
    }
    if (current.isNotEmpty() && lines.size < maxLines) {
        lines += current.takeFitting(context, style, maxWidth)
    }
    return lines.take(maxLines).ifEmpty { listOf(takeFitting(context, style, maxWidth)) }
}

internal fun String.takeFitting(context: RenderContext, style: TextStyle, maxWidth: Float): String {
    if (context.backend.measureText(this, style).first <= maxWidth) return this
    if (isEmpty() || maxWidth <= 0f) return ""
    var end = length
    while (end > 1) {
        val candidate = substring(0, end) + "…"
        if (context.backend.measureText(candidate, style).first <= maxWidth) return candidate
        end -= 1
    }
    return "…"
}

internal fun drawArcStroke(context: RenderContext, centerX: Float, centerY: Float, radius: Float, startAngle: Float, sweep: Float, stroke: Stroke) {
    if (radius <= 0f || sweep == 0f) return
    context.backend.drawArc(centerX, centerY, radius, startAngle, sweep, stroke)
}

internal fun animate(current: Float, target: Float, dt: Float, speed: Float): Float =
    current + (target - current) * (1f - exp(-speed * dt.coerceIn(0f, 0.1f)))

internal fun isAnimating(vararg values: Float) = values.any { it in 0.001f..0.999f }

internal fun textBaseline(height: Float, size: Float) = height * 0.5f + size * 0.36f

internal fun overlay(base: Color, tint: Color, stateAlpha: Float, enabledAlpha: Float): Color {
    val mixed = lerp(base, tint, stateAlpha.coerceIn(0f, 1f))
    return mixed.withAlpha((mixed.alpha * enabledAlpha).toInt())
}

internal fun lerp(a: Color, b: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        (a.red + (b.red - a.red) * t).toInt().coerceIn(0, 255),
        (a.green + (b.green - a.green) * t).toInt().coerceIn(0, 255),
        (a.blue + (b.blue - a.blue) * t).toInt().coerceIn(0, 255),
        (a.alpha + (b.alpha - a.alpha) * t).toInt().coerceIn(0, 255)
    )
}

internal fun Color.withAlpha(alpha: Int) = Color(red, green, blue, alpha.coerceIn(0, 255))

internal fun drawTopBarAction(context: RenderContext, label: String, x: Float, y: Float, hovered: Boolean, pressed: Boolean) {
    if (hovered || pressed) {
        context.backend.drawRoundedRect(x, y, 40f, 40f, 20f, overlay(context.theme.palette.surface, context.theme.palette.onSurface, if (pressed) 0.10f else 0.06f, 1f), null)
    }
    val style = TextStyle(context.theme.typography.bodySize, context.theme.palette.onSurfaceVariant, context.theme.typography.fontFamily)
    val textWidth = context.backend.measureText(label, style).first
    context.backend.drawText(label, x + (40f - textWidth) / 2f, y + textBaseline(40f, style.size), style)
}
