package dev.unknownuser.ananda.text

import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.TextStyle
import dev.unknownuser.ananda.draw.Drawable
import dev.unknownuser.ananda.theme.DefaultFontFamily
import org.jetbrains.skia.FontEdging
import org.jetbrains.skia.FontHinting
import java.awt.Color

class TextRun(
    var text: String,
    var x: Float = 0f,
    var y: Float = 0f,
    var size: Float = 18f,
    var color: Color = Color.WHITE,
    var fontFamily: String = DefaultFontFamily,
    var edging: FontEdging = FontEdging.SUBPIXEL_ANTI_ALIAS,
    var hinting: FontHinting = FontHinting.FULL,
    var subpixel: Boolean = true,
    var linearMetrics: Boolean = false,
    var visible: Boolean = true
) : Drawable {
    override fun draw(context: RenderContext) {
        if (!visible || text.isEmpty()) return
        context.backend.drawText(text, x, y, TextStyle(size, color, fontFamily))
    }

    fun at(x: Number, y: Number) = apply {
        this.x = x.toFloat()
        this.y = y.toFloat()
    }
}
