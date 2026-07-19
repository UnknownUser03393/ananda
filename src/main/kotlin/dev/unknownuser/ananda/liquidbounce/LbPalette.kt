package dev.unknownuser.ananda.liquidbounce

import java.awt.Color

/** Colors from LiquidBounce `src-theme/src/colors.scss`. */
object LbPalette {
    val Accent = Color(0x96, 0x2d, 0x2d)
    val ClickGuiBase = Color(0x08, 0x05, 0x06)
    val ClickGuiPanel = Color(0x15, 0x10, 0x15, 230)
    val ClickGuiPanelElevated = Color(0x24, 0x14, 0x1b, 245)
    val ClickGuiBorder = Color(255, 232, 238, 31)
    val ClickGuiBorderStrong = Color(Accent.red, Accent.green, Accent.blue, 143)
    val ClickGuiText = Color(0xf7, 0xee, 0xf1)
    val ClickGuiTextDimmed = Color(0xbf, 0xae, 0xb5)

    fun withAlpha(color: Color, alpha: Int): Color =
        Color(color.red, color.green, color.blue, alpha.coerceIn(0, 255))
}