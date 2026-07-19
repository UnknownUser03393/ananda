package dev.unknownuser

import dev.unknownuser.ananda.annotations.functional
import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.component.currentFunctionalScope
import dev.unknownuser.ananda.dsl.CurveChart
import dev.unknownuser.ananda.dsl.CurveChartStyle
import dev.unknownuser.ananda.dsl.Spacer
import dev.unknownuser.ananda.dsl.scene
import dev.unknownuser.ananda.layout.Alignment
import dev.unknownuser.ananda.theme.Palette
import dev.unknownuser.ananda.theme.Theme
import dev.unknownuser.ananda.window.SkiaWindow
import java.awt.Color
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private const val WindowWidth = 720f
private const val WindowHeight = 320f
private const val HudContentWidth = 580f
private val HudWaveChartStyle = CurveChartStyle(
    minValue = -1f,
    maxValue = 1f,
    showGrid = false,
    chartPadding = 2f,
    lineStroke = Stroke(Color(247, 247, 250), 2.35f),
    glowStroke = Stroke(Color(255, 255, 255, 68), 2.7f),
    glowRadius = 7f,
    pointColor = null
)

fun main() {
    val scene = scene {
        theme(
            Theme(
                palette = Palette(
                    background = Color(91, 132, 47),
                    surface = Color(21, 19, 24),
                    primary = Color(246, 246, 250),
                    accent = Color(160, 158, 166),
                    text = Color(248, 248, 252),
                    mutedText = Color(176, 172, 184),
                    border = Color(255, 255, 255, 28)
                )
            )
        )

        functional(width = WindowWidth, height = WindowHeight) {
            val frame = useFrame()

            column(crossAxisAlignment = Alignment.Center)
            Spacer(height = 46f)
            BlocksHud(frame.elapsedSeconds)
        }
    }

    SkiaWindow(scene = scene, width = WindowWidth.toInt(), height = WindowHeight.toInt()).show()
}

@functional
fun BlocksHud(elapsedSeconds: Float) {
    with(currentFunctionalScope()) {
        val remainingProgress = (0.59f + sin(elapsedSeconds * 0.9f) * 0.39f).coerceIn(0.2f, 0.98f)
        val bps = 11.8f + sin(elapsedSeconds * 2.8f) * 1.2f
        val count = (remainingProgress * 60f).roundToInt().coerceIn(12, 59)

        elevatedPanel(
            width = 640f,
            height = 132f,
            radius = 32f,
            colors = listOf(Color(25, 22, 29, 242), Color(18, 17, 22, 238)),
            gradientDirection = GradientDirection.DiagonalDown,
            outline = Stroke(Color(255, 255, 255, 24), 1.15f)
        ) {
            padding(horizontal = 30f, vertical = 16f)
            column(gap = 9f)

            component(width = HudContentWidth, height = 30f) {
                row(crossAxisAlignment = Alignment.Center)

                label("Blocks", width = 118f, height = 30f) {
                    size = 23f
                    color = Color(248, 247, 253)
                }

                spacer(weight = 1f)

                label(count.toString(), width = 45f, height = 30f) {
                    size = 24f
                    color = Color(249, 248, 253)
                }
            }

            component(width = HudContentWidth, height = 28f) {
                row(crossAxisAlignment = Alignment.Center)

                label(String.format(Locale.US, "%.1f BPS", bps), width = 136f, height = 28f) {
                    size = 20f
                    color = Color(242, 241, 248)
                }

                spacer(weight = 1f)

                label("${(remainingProgress * 100f).roundToInt()}% remaining", width = 158f, height = 28f) {
                    size = 17f
                    color = Color(221, 218, 229)
                }

                spacer(weight = 1f)
            }

            CurveChart(
                values = waveSamples(elapsedSeconds),
                width = HudContentWidth * remainingProgress,
                height = 34f,
                style = HudWaveChartStyle
            )
        }
    }
}

private fun waveSamples(elapsed: Float): List<Float> =
    List(86) { index ->
        val progress = index / 85f
        sin(progress * PI.toFloat() * 7.4f + elapsed * 3.3f) * 0.62f +
            sin(progress * PI.toFloat() * 15.1f + elapsed * 5.1f) * 0.28f
    }
