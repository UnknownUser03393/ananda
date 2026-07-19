package dev.unknownuser.ananda.dsl

import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.component.Button
import dev.unknownuser.ananda.component.CurveChart
import dev.unknownuser.ananda.component.ElevatedPanel
import dev.unknownuser.ananda.component.FunctionalComponent
import dev.unknownuser.ananda.component.FunctionalComponentBody
import dev.unknownuser.ananda.component.GlowFrame
import dev.unknownuser.ananda.component.GlowLine
import dev.unknownuser.ananda.component.GlowOrb
import dev.unknownuser.ananda.component.Label
import dev.unknownuser.ananda.component.Panel
import dev.unknownuser.ananda.theme.Palette
import dev.unknownuser.ananda.theme.Spacing
import dev.unknownuser.ananda.theme.Theme
import dev.unknownuser.ananda.theme.Typography
import java.awt.Color

@DslMarker
annotation class AnandaDsl

fun rgb(red: Int, green: Int, blue: Int): Color =
    Color(red, green, blue)

fun rgba(red: Int, green: Int, blue: Int, alpha: Int): Color =
    Color(red, green, blue, alpha.coerceIn(0, 255))

@AnandaDsl
abstract class BoundsBuilder {
    var x: Float = 0f
    var y: Float = 0f
    var width: Float = 0f
    var height: Float = 0f

    fun frame(x: Number = this.x, y: Number = this.y, width: Number = this.width, height: Number = this.height) {
        this.x = x.toFloat()
        this.y = y.toFloat()
        this.width = width.toFloat()
        this.height = height.toFloat()
    }

    fun size(width: Number, height: Number) {
        this.width = width.toFloat()
        this.height = height.toFloat()
    }

    fun offset(x: Number = this.x, y: Number = this.y) {
        this.x = x.toFloat()
        this.y = y.toFloat()
    }
}

@AnandaDsl
class ThemeBuilder(private val base: Theme = Theme.Default) {
    var palette: Palette = base.palette
    var typography: Typography = base.typography
    var spacing: Spacing = base.spacing

    fun palette(block: PaletteBuilder.() -> Unit) {
        palette = PaletteBuilder(palette).apply(block).build()
    }

    fun typography(block: TypographyBuilder.() -> Unit) {
        typography = TypographyBuilder(typography).apply(block).build()
    }

    fun spacing(block: SpacingBuilder.() -> Unit) {
        spacing = SpacingBuilder(spacing).apply(block).build()
    }

    fun build(): Theme =
        base.copy(palette = palette, typography = typography, spacing = spacing)
}

@AnandaDsl
class PaletteBuilder(base: Palette = Palette()) {
    var background: Color = base.background
    var surface: Color = base.surface
    var primary: Color = base.primary
    var accent: Color = base.accent
    var danger: Color = base.danger
    var text: Color = base.text
    var mutedText: Color = base.mutedText
    var border: Color = base.border

    fun build(): Palette =
        Palette(background, surface, primary, accent, danger, text, mutedText, border)
}

@AnandaDsl
class TypographyBuilder(base: Typography = Typography()) {
    var fontFamily: String = base.fontFamily
    var bodySize: Float = base.bodySize
    var titleSize: Float = base.titleSize

    fun build(): Typography =
        Typography(fontFamily, bodySize, titleSize)
}

@AnandaDsl
class SpacingBuilder(base: Spacing = Spacing()) {
    var xs: Float = base.xs
    var sm: Float = base.sm
    var md: Float = base.md
    var lg: Float = base.lg
    var xl: Float = base.xl

    fun build(): Spacing =
        Spacing(xs, sm, md, lg, xl)
}

@AnandaDsl
class StrokeBuilder {
    var color: Color = Color.WHITE
    var width: Float = 1f

    fun build(): Stroke =
        Stroke(color, width)
}

@AnandaDsl
class ShadowBuilder {
    var color: Color = Color(0, 0, 0, 120)
    var blurRadius: Float = 18f
    var offsetX: Float = 0f
    var offsetY: Float = 6f
    var spread: Float = 0f

    fun build(): Shadow =
        Shadow(color, blurRadius, offsetX, offsetY, spread)
}

@AnandaDsl
class ElevatedPanelBuilder : BoundsBuilder() {
    var radius: Float = 18f
    var gradientDirection: GradientDirection = GradientDirection.DiagonalDown
    var shadow: Shadow? = Shadow(Color(0, 0, 0, 120), blurRadius = 18f, offsetY = 8f)
    var outline: Stroke? = Stroke(Color(255, 255, 255, 34), 1f)
    var outlineColors: List<Color>? = null
    var outlineGradientDirection: GradientDirection = GradientDirection.DiagonalDown
    private var colors: List<Color> = listOf(Color(38, 43, 54, 230), Color(25, 28, 36, 230))

    fun colors(vararg colors: Color) {
        this.colors = colors.toList()
    }

    fun colors(colors: List<Color>) {
        this.colors = colors
    }

    fun shadow(block: ShadowBuilder.() -> Unit) {
        shadow = ShadowBuilder().apply(block).build()
    }

    fun noShadow() {
        shadow = null
    }

    fun outline(block: StrokeBuilder.() -> Unit) {
        outline = StrokeBuilder().apply(block).build()
    }

    fun noOutline() {
        outline = null
        outlineColors = null
    }

    fun outlineColors(vararg colors: Color, direction: GradientDirection = outlineGradientDirection) {
        outlineColors = colors.toList()
        outlineGradientDirection = direction
    }

    fun build(): ElevatedPanel =
        ElevatedPanel(x, y, width, height, radius, colors, gradientDirection, shadow, outline, outlineColors, outlineGradientDirection)
}

@AnandaDsl
class GlowOrbBuilder : BoundsBuilder() {
    var shadow: Shadow? = null
    private var colors: List<Color> = listOf(Color(234, 179, 8, 110), Color(234, 179, 8, 0))

    init {
        width = 120f
        height = 120f
    }

    fun colors(vararg colors: Color) {
        this.colors = colors.toList()
    }

    fun shadow(block: ShadowBuilder.() -> Unit) {
        shadow = ShadowBuilder().apply(block).build()
    }

    fun build(): GlowOrb =
        GlowOrb(x, y, width, height, colors, shadow)
}

@AnandaDsl
class GlowLineBuilder : BoundsBuilder() {
    var x2: Float = 0f
    var y2: Float = 0f
    var color: Color = Color(125, 211, 252)
    var strokeWidth: Float = 2f
    var glowColor: Color = Color(125, 211, 252, 90)
    var glowWidth: Float = 10f

    fun target(x: Number, y: Number) {
        x2 = x.toFloat()
        y2 = y.toFloat()
    }

    fun build(): GlowLine =
        GlowLine(x, y, x2, y2, color, strokeWidth, glowColor, glowWidth)
}

@AnandaDsl
class GlowFrameBuilder : BoundsBuilder() {
    var radius: Float = 14f
    var glow: Shadow = Shadow(Color(94, 234, 212, 82), blurRadius = 16f, spread = 2f)
    var outlineWidth: Float = 1.2f
    var outlineGradientDirection: GradientDirection = GradientDirection.Horizontal
    private var outlineColors: List<Color> = listOf(Color(94, 234, 212, 190), Color(255, 222, 126, 150))

    init {
        width = 120f
        height = 48f
    }

    fun glow(block: ShadowBuilder.() -> Unit) {
        glow = ShadowBuilder().apply(block).build()
    }

    fun outlineColors(vararg colors: Color, direction: GradientDirection = outlineGradientDirection) {
        outlineColors = colors.toList()
        outlineGradientDirection = direction
    }

    fun build(): GlowFrame =
        GlowFrame(x, y, width, height, radius, glow, outlineWidth, outlineColors, outlineGradientDirection)
}

@AnandaDsl
class CurveChartBuilder : BoundsBuilder() {
    var minValue: Float? = null
    var maxValue: Float? = null
    var smooth: Boolean = true
    var showGrid: Boolean = true
    var gridLines: Int = 4
    var lineStroke: Stroke = Stroke(Color(94, 234, 212), 2f)
    var glowStroke: Stroke? = Stroke(Color(94, 234, 212, 56), 2f)
    var glowRadius: Float = 10f
    var interpolationSteps: Int = 8
    var pointColor: Color? = Color.WHITE
    var chartPadding: Float = 12f
    private var values: List<Float> = emptyList()

    init {
        width = 240f
        height = 120f
    }

    fun values(vararg values: Number) {
        this.values = values.map { it.toFloat() }
    }

    fun values(values: List<Number>) {
        this.values = values.map { it.toFloat() }
    }

    fun line(color: Color, width: Number = lineStroke.width) {
        lineStroke = Stroke(color, width.toFloat())
    }

    fun glow(color: Color, width: Number = glowStroke?.width ?: 10f) {
        glowStroke = Stroke(color, width.toFloat())
    }

    fun noGlow() {
        glowStroke = null
    }

    fun build(): CurveChart =
        CurveChart(x, y, width, height, values, minValue, maxValue, smooth, showGrid, gridLines, lineStroke, glowStroke, glowRadius, interpolationSteps, pointColor, chartPadding)
}

@AnandaDsl
class FunctionalBuilder : BoundsBuilder() {
    private var content: FunctionalComponentBody = {}

    fun content(block: FunctionalComponentBody) {
        content = block
    }

    fun build(): FunctionalComponent =
        FunctionalComponent(x, y, width, height, content)
}

@AnandaDsl
class PanelBuilder : BoundsBuilder() {
    fun build(): Panel =
        Panel(x, y, width, height)
}

@AnandaDsl
class LabelBuilder : BoundsBuilder() {
    var text: String = ""
    var fontSize: Float? = null
    var color: Color? = null

    fun build(): Label =
        Label(text, x, y, width, height, fontSize, color)
}

@AnandaDsl
class ButtonBuilder : BoundsBuilder() {
    var text: String = ""
    private var onClick: () -> Unit = {}

    init {
        width = 120f
        height = 36f
    }

    fun onClick(block: () -> Unit) {
        onClick = block
    }

    fun build(): Button =
        Button(text, x, y, width, height, onClick)
}
