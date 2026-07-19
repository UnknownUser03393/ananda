package dev.unknownuser.ananda.theme

import dev.unknownuser.ananda.layout.Insets
import dev.unknownuser.ananda.style.Style
import java.awt.Color

const val DefaultFontFamily: String = "HarmonyOS Sans SC"

data class Palette(
    val background: Color = Color(24, 26, 31),
    val surface: Color = Color(34, 39, 48),
    val primary: Color = Color(45, 111, 167),
    val accent: Color = Color(234, 179, 8),
    val danger: Color = Color(220, 79, 77),
    val text: Color = Color.WHITE,
    val mutedText: Color = Color(195, 203, 215),
    val border: Color = Color(149, 164, 185),
    val onPrimary: Color = Color.WHITE,
    val primaryContainer: Color = Color(40, 58, 92),
    val onPrimaryContainer: Color = Color(218, 226, 255),
    val secondary: Color = Color(190, 198, 220),
    val onSecondary: Color = Color(40, 48, 64),
    val secondaryContainer: Color = Color(62, 70, 88),
    val onSecondaryContainer: Color = Color(222, 226, 245),
    val error: Color = Color(255, 180, 171),
    val onError: Color = Color(105, 0, 5),
    val errorContainer: Color = Color(147, 0, 10),
    val onErrorContainer: Color = Color(255, 218, 214),
    val onSurface: Color = text,
    val onSurfaceVariant: Color = mutedText,
    val surfaceVariant: Color = Color(73, 69, 79),
    val surfaceContainer: Color = surface,
    val surfaceContainerHigh: Color = Color(43, 41, 44),
    val surfaceContainerHighest: Color = Color(54, 52, 55),
    val outline: Color = border,
    val outlineVariant: Color = Color(73, 69, 79),
    val inverseSurface: Color = Color(230, 225, 229),
    val inverseOnSurface: Color = Color(49, 48, 51),
    val scrim: Color = Color.BLACK,
    val surfaceContainerLowest: Color = Color(15, 13, 18),
    val surfaceContainerLow: Color = Color(29, 27, 32),
    val surfaceTint: Color = primary,
    val tertiary: Color = Color(239, 184, 200),
    val onTertiary: Color = Color(73, 37, 50),
    val tertiaryContainer: Color = Color(95, 59, 72),
    val onTertiaryContainer: Color = Color(255, 216, 228)
)

data class Typography(
    val fontFamily: String = DefaultFontFamily,
    val bodySize: Float = 18f,
    val titleSize: Float = 34f
)

data class Spacing(
    val xs: Float = 4f,
    val sm: Float = 8f,
    val md: Float = 16f,
    val lg: Float = 24f,
    val xl: Float = 32f
)

data class Theme(
    val palette: Palette = Palette(),
    val typography: Typography = Typography(),
    val spacing: Spacing = Spacing(),
    val controlStyle: Style = Style(
        padding = Insets.xy(12f, 8f),
        borderWidth = 1f
    )
) {
    companion object {
        val Default = Theme()
    }
}
