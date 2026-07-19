package dev.unknownuser.ananda

import dev.unknownuser.ananda.backend.TextStyle
import dev.unknownuser.ananda.style.TextOverflow
import dev.unknownuser.ananda.style.WhiteSpace

internal fun expectedEllipsisText(line: String, maxWidth: Float, textSize: Float, letterSpacing: Float = 0f): String {
    if (maxWidth <= 0f) return ""
    val style = TextStyle(textSize, letterSpacing = letterSpacing)
    if (testMeasureTextWidth(line, style) <= maxWidth) return line
    val ellipsis = "..."
    if (testMeasureTextWidth(ellipsis, style) > maxWidth) return ""
    var low = 0
    var high = line.length
    while (low < high) {
        val mid = (low + high + 1) / 2
        val candidate = line.take(mid) + ellipsis
        if (testMeasureTextWidth(candidate, style) <= maxWidth) low = mid else high = mid - 1
    }
    return line.take(low) + ellipsis
}