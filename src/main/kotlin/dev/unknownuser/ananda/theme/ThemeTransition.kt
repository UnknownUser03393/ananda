package dev.unknownuser.ananda.theme

import dev.unknownuser.ananda.layout.Insets
import java.awt.Color

fun interpolateTheme(from: Theme, to: Theme, progress: Float): Theme {
    val t = progress.coerceIn(0f, 1f)
    if (t <= 0f) return from
    if (t >= 1f) return to
    return Theme(
        palette = interpolatePalette(from.palette, to.palette, t),
        typography = Typography(
            fontFamily = if (t < 0.5f) from.typography.fontFamily else to.typography.fontFamily,
            bodySize = lerp(from.typography.bodySize, to.typography.bodySize, t),
            titleSize = lerp(from.typography.titleSize, to.typography.titleSize, t)
        ),
        spacing = Spacing(
            xs = lerp(from.spacing.xs, to.spacing.xs, t),
            sm = lerp(from.spacing.sm, to.spacing.sm, t),
            md = lerp(from.spacing.md, to.spacing.md, t),
            lg = lerp(from.spacing.lg, to.spacing.lg, t),
            xl = lerp(from.spacing.xl, to.spacing.xl, t)
        ),
        controlStyle = to.controlStyle.copy(
            padding = interpolateInsets(from.controlStyle.padding, to.controlStyle.padding, t),
            margin = interpolateInsets(from.controlStyle.margin, to.controlStyle.margin, t),
            background = interpolateNullableColor(from.controlStyle.background, to.controlStyle.background, t),
            foreground = interpolateNullableColor(from.controlStyle.foreground, to.controlStyle.foreground, t),
            border = interpolateNullableColor(from.controlStyle.border, to.controlStyle.border, t),
            borderWidth = interpolateNullableFloat(from.controlStyle.borderWidth, to.controlStyle.borderWidth, t),
            radius = interpolateNullableFloat(from.controlStyle.radius, to.controlStyle.radius, t),
            textSize = interpolateNullableFloat(from.controlStyle.textSize, to.controlStyle.textSize, t),
            opacity = interpolateNullableFloat(from.controlStyle.opacity, to.controlStyle.opacity, t),
            disabledAlpha = lerp(from.controlStyle.disabledAlpha, to.controlStyle.disabledAlpha, t)
        )
    )
}

private fun interpolatePalette(a: Palette, b: Palette, t: Float): Palette = Palette(
    background = color(a.background, b.background, t),
    surface = color(a.surface, b.surface, t),
    primary = color(a.primary, b.primary, t),
    accent = color(a.accent, b.accent, t),
    danger = color(a.danger, b.danger, t),
    text = color(a.text, b.text, t),
    mutedText = color(a.mutedText, b.mutedText, t),
    border = color(a.border, b.border, t),
    onPrimary = color(a.onPrimary, b.onPrimary, t),
    primaryContainer = color(a.primaryContainer, b.primaryContainer, t),
    onPrimaryContainer = color(a.onPrimaryContainer, b.onPrimaryContainer, t),
    secondary = color(a.secondary, b.secondary, t),
    onSecondary = color(a.onSecondary, b.onSecondary, t),
    secondaryContainer = color(a.secondaryContainer, b.secondaryContainer, t),
    onSecondaryContainer = color(a.onSecondaryContainer, b.onSecondaryContainer, t),
    error = color(a.error, b.error, t),
    onError = color(a.onError, b.onError, t),
    errorContainer = color(a.errorContainer, b.errorContainer, t),
    onErrorContainer = color(a.onErrorContainer, b.onErrorContainer, t),
    onSurface = color(a.onSurface, b.onSurface, t),
    onSurfaceVariant = color(a.onSurfaceVariant, b.onSurfaceVariant, t),
    surfaceVariant = color(a.surfaceVariant, b.surfaceVariant, t),
    surfaceContainer = color(a.surfaceContainer, b.surfaceContainer, t),
    surfaceContainerHigh = color(a.surfaceContainerHigh, b.surfaceContainerHigh, t),
    surfaceContainerHighest = color(a.surfaceContainerHighest, b.surfaceContainerHighest, t),
    outline = color(a.outline, b.outline, t),
    outlineVariant = color(a.outlineVariant, b.outlineVariant, t),
    inverseSurface = color(a.inverseSurface, b.inverseSurface, t),
    inverseOnSurface = color(a.inverseOnSurface, b.inverseOnSurface, t),
    scrim = color(a.scrim, b.scrim, t),
    surfaceContainerLowest = color(a.surfaceContainerLowest, b.surfaceContainerLowest, t),
    surfaceContainerLow = color(a.surfaceContainerLow, b.surfaceContainerLow, t),
    surfaceTint = color(a.surfaceTint, b.surfaceTint, t),
    tertiary = color(a.tertiary, b.tertiary, t),
    onTertiary = color(a.onTertiary, b.onTertiary, t),
    tertiaryContainer = color(a.tertiaryContainer, b.tertiaryContainer, t),
    onTertiaryContainer = color(a.onTertiaryContainer, b.onTertiaryContainer, t)
)

private fun color(a: Color, b: Color, t: Float): Color = Color(
    lerp(a.red.toFloat(), b.red.toFloat(), t).toInt().coerceIn(0, 255),
    lerp(a.green.toFloat(), b.green.toFloat(), t).toInt().coerceIn(0, 255),
    lerp(a.blue.toFloat(), b.blue.toFloat(), t).toInt().coerceIn(0, 255),
    lerp(a.alpha.toFloat(), b.alpha.toFloat(), t).toInt().coerceIn(0, 255)
)

private fun interpolateNullableColor(a: Color?, b: Color?, t: Float): Color? = when {
    a == null && b == null -> null
    a == null -> color(Color(b!!.red, b.green, b.blue, 0), b, t)
    b == null -> color(a, Color(a.red, a.green, a.blue, 0), t)
    else -> color(a, b, t)
}

private fun interpolateNullableFloat(a: Float?, b: Float?, t: Float): Float? = when {
    a == null && b == null -> null
    else -> lerp(a ?: 0f, b ?: 0f, t)
}

private fun interpolateInsets(a: Insets?, b: Insets?, t: Float): Insets? {
    if (a == null && b == null) return null
    val from = a ?: Insets()
    val to = b ?: Insets()
    return Insets(
        lerp(from.left, to.left, t),
        lerp(from.top, to.top, t),
        lerp(from.right, to.right, t),
        lerp(from.bottom, to.bottom, t)
    )
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
