package dev.unknownuser.ananda.dsl

import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.component.Button
import dev.unknownuser.ananda.component.CurveChart
import dev.unknownuser.ananda.component.FunctionalComponentBody
import dev.unknownuser.ananda.component.FunctionalScope
import dev.unknownuser.ananda.component.GlowFrame
import dev.unknownuser.ananda.component.GlowLine
import dev.unknownuser.ananda.component.Label
import dev.unknownuser.ananda.component.Spacer
import java.awt.Color

typealias Ui = FunctionalComponentBody

fun ui(block: Ui): Ui = block

data class CurveChartStyle(
    val minValue: Float? = null,
    val maxValue: Float? = null,
    val smooth: Boolean = true,
    val showGrid: Boolean = true,
    val gridLines: Int = 4,
    val lineStroke: Stroke = Stroke(Color(94, 234, 212), 2f),
    val glowStroke: Stroke? = Stroke(Color(94, 234, 212, 56), 2f),
    val glowRadius: Float = 10f,
    val interpolationSteps: Int = 8,
    val pointColor: Color? = Color.WHITE,
    val chartPadding: Float = 12f
)

fun FunctionalScope.Label(text: String, block: Label.() -> Unit = {}): Label =
    label(text).apply(block)

fun FunctionalScope.Label(block: LabelBuilder.() -> Unit): Label =
    label(block)

fun FunctionalScope.Button(text: String = "", block: ButtonBuilder.() -> Unit = {}): Button {
    return button {
        this.text = text
        block()
    }
}

fun FunctionalScope.GlowLine(block: GlowLineBuilder.() -> Unit): GlowLine =
    glowLine(block)

fun FunctionalScope.GlowFrame(block: GlowFrameBuilder.() -> Unit): GlowFrame =
    glowFrame(block)

fun FunctionalScope.CurveChart(block: CurveChartBuilder.() -> Unit): CurveChart =
    curveChart(block)

fun FunctionalScope.CurveChart(
    values: List<Float>,
    width: Number = 240f,
    height: Number = 120f,
    style: CurveChartStyle = CurveChartStyle()
): CurveChart =
    curveChart(
        values = values,
        width = width,
        height = height
    ) {
        minValue = style.minValue
        maxValue = style.maxValue
        smooth = style.smooth
        showGrid = style.showGrid
        gridLines = style.gridLines
        lineStroke = style.lineStroke
        glowStroke = style.glowStroke
        glowRadius = style.glowRadius
        interpolationSteps = style.interpolationSteps
        pointColor = style.pointColor
        chartPadding = style.chartPadding
    }

fun FunctionalScope.Spacer(width: Number = 0f, height: Number = 0f, weight: Number = 0f): Spacer =
    spacer(width, height, weight)
