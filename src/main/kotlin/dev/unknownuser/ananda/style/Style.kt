package dev.unknownuser.ananda.style

import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.CornerRadii
import dev.unknownuser.ananda.backend.ImageFit
import dev.unknownuser.ananda.backend.ImagePosition
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.TextureRegion
import dev.unknownuser.ananda.layout.Layout
import dev.unknownuser.ananda.layout.Insets
import dev.unknownuser.ananda.layout.Positioning
import java.awt.Color

enum class TextAlign {
    Start,
    Center,
    End
}

enum class WhiteSpace {
    Normal,
    NoWrap,
    Pre
}

enum class TextOverflow {
    Clip,
    Ellipsis
}

enum class OverflowWrap {
    Normal,
    Anywhere
}

data class Style(
    val width: Float? = null,
    val height: Float? = null,
    val minWidth: Float? = null,
    val minHeight: Float? = null,
    val maxWidth: Float? = null,
    val maxHeight: Float? = null,
    val background: Color? = null,
    val backgroundLayers: List<BackgroundLayer>? = null,
    val backgroundGradient: List<Color>? = null,
    val backgroundGradientDirection: GradientDirection? = null,
    val backdropBlurRadius: Float? = null,
    val backgroundImage: TextureRegion? = null,
    val backgroundImageFit: ImageFit? = null,
    val backgroundImageAlpha: Float? = null,
    val backgroundImageWidth: Float? = null,
    val backgroundImageHeight: Float? = null,
    val backgroundImagePositionX: ImagePosition? = null,
    val backgroundImagePositionY: ImagePosition? = null,
    val foreground: Color? = null,
    val border: Color? = null,
    val borderWidth: Float? = null,
    val borderSides: BorderSides? = null,
    val radius: Float? = null,
    val cornerRadii: CornerRadii? = null,
    val padding: Insets? = null,
    val margin: Insets? = null,
    val textSize: Float? = null,
    val textAlign: TextAlign? = null,
    val lineHeight: Float? = null,
    val fontFamily: String? = null,
    val fontWeight: Int? = null,
    val letterSpacing: Float? = null,
    val whiteSpace: WhiteSpace? = null,
    val textOverflow: TextOverflow? = null,
    val overflowWrap: OverflowWrap? = null,
    val opacity: Float? = null,
    val shadow: Shadow? = null,
    val translate: TransformOffset? = null,
    val before: PseudoElement? = null,
    val after: PseudoElement? = null,
    val stateStyles: StateStyles? = null,
    val clipToBounds: Boolean? = null,
    val layout: Layout? = null,
    val positioning: Positioning? = null,
    val left: Float? = null,
    val top: Float? = null,
    val right: Float? = null,
    val bottom: Float? = null,
    val leftPercent: Float? = null,
    val topPercent: Float? = null,
    val rightPercent: Float? = null,
    val bottomPercent: Float? = null,
    val disabledAlpha: Float = 0.45f
) {
    fun merge(override: Style?): Style {
        if (override == null) return this
        return Style(
            width = override.width ?: width,
            height = override.height ?: height,
            minWidth = override.minWidth ?: minWidth,
            minHeight = override.minHeight ?: minHeight,
            maxWidth = override.maxWidth ?: maxWidth,
            maxHeight = override.maxHeight ?: maxHeight,
            background = override.background ?: background,
            backgroundLayers = override.backgroundLayers ?: backgroundLayers,
            backgroundGradient = override.backgroundGradient ?: backgroundGradient,
            backgroundGradientDirection = override.backgroundGradientDirection ?: backgroundGradientDirection,
            backdropBlurRadius = override.backdropBlurRadius ?: backdropBlurRadius,
            backgroundImage = override.backgroundImage ?: backgroundImage,
            backgroundImageFit = override.backgroundImageFit ?: backgroundImageFit,
            backgroundImageAlpha = override.backgroundImageAlpha ?: backgroundImageAlpha,
            backgroundImageWidth = override.backgroundImageWidth ?: backgroundImageWidth,
            backgroundImageHeight = override.backgroundImageHeight ?: backgroundImageHeight,
            backgroundImagePositionX = override.backgroundImagePositionX ?: backgroundImagePositionX,
            backgroundImagePositionY = override.backgroundImagePositionY ?: backgroundImagePositionY,
            foreground = override.foreground ?: foreground,
            border = override.border ?: border,
            borderWidth = override.borderWidth ?: borderWidth,
            borderSides = override.borderSides ?: borderSides,
            radius = override.radius ?: radius,
            cornerRadii = override.cornerRadii ?: cornerRadii,
            padding = override.padding ?: padding,
            margin = override.margin ?: margin,
            textSize = override.textSize ?: textSize,
            textAlign = override.textAlign ?: textAlign,
            lineHeight = override.lineHeight ?: lineHeight,
            fontFamily = override.fontFamily ?: fontFamily,
            fontWeight = override.fontWeight ?: fontWeight,
            letterSpacing = override.letterSpacing ?: letterSpacing,
            whiteSpace = override.whiteSpace ?: whiteSpace,
            textOverflow = override.textOverflow ?: textOverflow,
            overflowWrap = override.overflowWrap ?: overflowWrap,
            opacity = override.opacity ?: opacity,
            shadow = override.shadow ?: shadow,
            translate = override.translate ?: translate,
            before = override.before ?: before,
            after = override.after ?: after,
            stateStyles = override.stateStyles ?: stateStyles,
            clipToBounds = override.clipToBounds ?: clipToBounds,
            layout = override.layout ?: layout,
            positioning = override.positioning ?: positioning,
            left = override.left ?: left,
            top = override.top ?: top,
            right = override.right ?: right,
            bottom = override.bottom ?: bottom,
            leftPercent = override.leftPercent ?: leftPercent,
            topPercent = override.topPercent ?: topPercent,
            rightPercent = override.rightPercent ?: rightPercent,
            bottomPercent = override.bottomPercent ?: bottomPercent,
            disabledAlpha = override.disabledAlpha
        )
    }
}
