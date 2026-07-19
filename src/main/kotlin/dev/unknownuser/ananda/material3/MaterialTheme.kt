package dev.unknownuser.ananda.material3

import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeTonalSpot
import dev.unknownuser.ananda.layout.Insets
import dev.unknownuser.ananda.style.Style
import dev.unknownuser.ananda.theme.Palette
import dev.unknownuser.ananda.theme.DefaultFontFamily
import dev.unknownuser.ananda.theme.Spacing
import dev.unknownuser.ananda.theme.Theme
import dev.unknownuser.ananda.theme.Typography
import java.awt.Color

/** Material 3 theme generated from a seed through the official HCT tonal model. */
object MaterialTheme {
    fun dark(
        primary: Color = Color(103, 80, 164),
        fontFamily: String = DefaultFontFamily,
        density: Float = 1f
    ): Theme = generated(primary, dark = true, fontFamily, density)

    fun light(
        primary: Color = Color(103, 80, 164),
        fontFamily: String = DefaultFontFamily,
        density: Float = 1f
    ): Theme = generated(primary, dark = false, fontFamily, density)

    private fun generated(seed: Color, dark: Boolean, fontFamily: String, density: Float): Theme {
        val scheme = SchemeTonalSpot(
            Hct.fromInt(seed.rgb),
            dark,
            0.0,
            ColorSpec.SpecVersion.SPEC_2021,
            DynamicScheme.Platform.PHONE
        )
        val palette = Palette(
            background = scheme.background.awt(),
            surface = scheme.surface.awt(),
            primary = scheme.primary.awt(),
            accent = scheme.primary.awt(),
            danger = scheme.error.awt(),
            text = scheme.onSurface.awt(),
            mutedText = scheme.onSurfaceVariant.awt(),
            border = scheme.outlineVariant.awt(),
            onPrimary = scheme.onPrimary.awt(),
            primaryContainer = scheme.primaryContainer.awt(),
            onPrimaryContainer = scheme.onPrimaryContainer.awt(),
            secondary = scheme.secondary.awt(),
            onSecondary = scheme.onSecondary.awt(),
            secondaryContainer = scheme.secondaryContainer.awt(),
            onSecondaryContainer = scheme.onSecondaryContainer.awt(),
            error = scheme.error.awt(),
            onError = scheme.onError.awt(),
            errorContainer = scheme.errorContainer.awt(),
            onErrorContainer = scheme.onErrorContainer.awt(),
            onSurface = scheme.onSurface.awt(),
            onSurfaceVariant = scheme.onSurfaceVariant.awt(),
            surfaceVariant = scheme.surfaceVariant.awt(),
            surfaceContainer = scheme.surfaceContainer.awt(),
            surfaceContainerHigh = scheme.surfaceContainerHigh.awt(),
            surfaceContainerHighest = scheme.surfaceContainerHighest.awt(),
            outline = scheme.outline.awt(),
            outlineVariant = scheme.outlineVariant.awt(),
            inverseSurface = scheme.inverseSurface.awt(),
            inverseOnSurface = scheme.inverseOnSurface.awt(),
            scrim = scheme.scrim.awt(),
            surfaceContainerLowest = scheme.surfaceContainerLowest.awt(),
            surfaceContainerLow = scheme.surfaceContainerLow.awt(),
            surfaceTint = scheme.surfaceTint.awt(),
            tertiary = scheme.tertiary.awt(),
            onTertiary = scheme.onTertiary.awt(),
            tertiaryContainer = scheme.tertiaryContainer.awt(),
            onTertiaryContainer = scheme.onTertiaryContainer.awt()
        )
        return Theme(
            palette = palette,
            typography = Typography(fontFamily, 14f * density, 22f * density),
            spacing = Spacing(4f * density, 8f * density, 12f * density, 16f * density, 24f * density),
            controlStyle = Style(padding = Insets.xy(24f * density, 10f * density), borderWidth = 1f)
        )
    }

    private fun Int.awt() = Color(this, true)
}

object MaterialShapes {
    const val ExtraSmall = 4f
    const val Small = 8f
    const val Medium = 12f
    const val Large = 16f
    const val ExtraLarge = 28f
    const val Full = 1000f
}
