package dev.unknownuser.ananda

import dev.unknownuser.ananda.backend.CornerRadii
import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.dsl.scene
import dev.unknownuser.ananda.layout.Alignment
import dev.unknownuser.ananda.layout.Positioning
import dev.unknownuser.ananda.style.BorderSides
import dev.unknownuser.ananda.layout.fr
import dev.unknownuser.ananda.layout.maxContent
import dev.unknownuser.ananda.style.BackgroundLayer
import dev.unknownuser.ananda.style.GradientStop
import dev.unknownuser.ananda.style.PseudoElement
import dev.unknownuser.ananda.style.PseudoVisibility
import dev.unknownuser.ananda.style.StateStyles
import dev.unknownuser.ananda.style.Style
import dev.unknownuser.ananda.style.TextOverflow
import dev.unknownuser.ananda.style.WhiteSpace
import java.awt.Color

/**
 * Kotlin DSL a Svelte→Ananda converter would emit for LiquidBounce src-theme fragments.
 */
object LiquidBounceConvertedThemeDsl {

    private val accent = Color(100, 180, 255)
    private val clickguiPanel = Color(24, 26, 32, 230)
    private val clickguiElevated = Color(32, 36, 44, 230)
    private val clickguiBorder = Color(255, 255, 255, 25)
    private val clickguiText = Color(235, 235, 240)

    fun clickGuiPanel(category: String = "Combat"): Component {
        lateinit var root: Component
        scene {
            root = component(width = 270) {
                position(Positioning.Absolute)
                anchor(left = 40, top = 60)
                cornerRadii(8)
                clipToBounds(true)
                backdropBlur(16)
                backgrounds(
                    BackgroundLayer.Gradient(
                        listOf(
                            GradientStop(Color(32, 36, 44, 107), 0f),
                            GradientStop(Color(0, 0, 0, 0), 0.32f)
                        ),
                        GradientDirection.Vertical
                    ),
                    BackgroundLayer.Solid(clickguiPanel)
                )
                shadow(Shadow(Color(0, 0, 0, 87), blurRadius = 27f, offsetY = 20f))
                border(clickguiBorder, 1f)
                column(gap = 0)
                component {
                    grid(columns = listOf(maxContent, fr(), maxContent), columnGap = 10, crossAxisAlignment = Alignment.Center)
                    padding(12, 10)
                    background(clickguiElevated)
                    borderSides(BorderSides(bottom = Stroke(clickguiBorder, 1f)))
                    component(width = 15, height = 15)
                    label(category, height = 14) {
                        fontWeight(700)
                        style(Style(textSize = 12f, foreground = clickguiText))
                    }
                    component(width = 22, height = 22) { cornerRadii(5) }
                }
                scroll(width = 270, height = 120) {
                    label("Velocity", height = 34)
                }
            }
        }
        return root
    }

    fun clickGuiModuleRow(name: String, enabled: Boolean): Component {
        lateinit var root: Component
        scene {
            root = component(width = 270, height = 34) {
                borderSides(BorderSides(bottom = Stroke(Color(clickguiText.red, clickguiText.green, clickguiText.blue, 14), 1f)))
                disabled(!enabled)
                style(
                    Style(
                        textSize = 12f,
                        foreground = if (enabled) clickguiText else Color(clickguiText.red, clickguiText.green, clickguiText.blue, 140),
                        stateStyles = StateStyles(
                            enabled = Style(
                                backgroundLayers = listOf(
                                    BackgroundLayer.Gradient(
                                        listOf(
                                            GradientStop(Color(accent.red, accent.green, accent.blue, 46), 0f),
                                            GradientStop(Color(accent.red, accent.green, accent.blue, 14), 1f)
                                        ),
                                        GradientDirection.Horizontal
                                    )
                                ),
                                shadow = Shadow(Color(accent.red, accent.green, accent.blue, 20), spread = 1f, inset = true)
                            )
                        )
                    )
                )
                if (enabled) {
                    before(
                        PseudoElement(
                            left = 0f,
                            top = 8f,
                            bottom = 8f,
                            width = 3f,
                            background = accent,
                            cornerRadii = CornerRadii(0f, 3f, 3f, 0f),
                            shadow = Shadow(Color(accent.red, accent.green, accent.blue, 122), blurRadius = 12f),
                            visibility = PseudoVisibility.Enabled
                        )
                    )
                }
                label(name, x = 14, height = 34) { fontWeight(600) }
            }
        }
        return root
    }

    fun clickGuiSearchBar(): Component {
        lateinit var root: Component
        scene {
            root = component(width = 560, height = 48) {
                position(Positioning.Fixed)
                anchor(leftPercent = 0.5f, top = 72)
                translate(xPercent = -0.5f)
                backdropBlur(14)
                cornerRadii(8)
                style(
                    Style(
                        background = Color(24, 26, 32, 224),
                        shadow = Shadow(Color(0, 0, 0, 71), blurRadius = 22f, offsetY = 18f),
                        border = clickguiBorder,
                        borderWidth = 1f
                    )
                )
            }
        }
        return root
    }

    fun hudKeyBindsChip(moduleName: String, keyLabel: String = "[R]"): Component {
        lateinit var root: Component
        scene {
            root = component {
                minSize(width = 150)
                maxSize(width = 200)
                cornerRadii(5)
                clipToBounds(true)
                style(Style(minWidth = 150f, maxWidth = 200f, radius = 5f, clipToBounds = true))
                column(crossAxisAlignment = Alignment.Stretch)
                component {
                    row(mainAxisAlignment = dev.unknownuser.ananda.layout.MainAxisAlignment.SpaceBetween, crossAxisAlignment = Alignment.Center)
                    padding(10, 7)
                    background(Color(30, 32, 38, 173))
                    label("Binds", height = 14) {
                        fontWeight(600)
                        style(Style(textSize = 14f))
                    }
                    component(width = 16, height = 16)
                }
                component {
                    column(crossAxisAlignment = Alignment.Stretch)
                    padding(10, 6)
                    background(Color(28, 30, 36, 128))
                    component {
                        row(gap = 12, mainAxisAlignment = dev.unknownuser.ananda.layout.MainAxisAlignment.SpaceBetween, crossAxisAlignment = Alignment.Center)
                        label(moduleName, height = 14) {
                            weight(1f)
                            minSize(width = 0)
                            clipToBounds(true)
                            style(Style(textSize = 14f, whiteSpace = WhiteSpace.NoWrap, textOverflow = TextOverflow.Ellipsis))
                        }
                        label(keyLabel, height = 14) {
                            style(Style(textSize = 14f, fontFamily = "monospace"))
                        }
                    }
                }
            }
        }
        return root
    }
}