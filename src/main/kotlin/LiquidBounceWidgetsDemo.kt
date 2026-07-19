package dev.unknownuser

import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.draw.Scene
import dev.unknownuser.ananda.liquidbounce.LbBooleanSetting
import dev.unknownuser.ananda.liquidbounce.LbDescription
import dev.unknownuser.ananda.liquidbounce.LbDescriptionAnchor
import dev.unknownuser.ananda.liquidbounce.LbFloatSetting
import dev.unknownuser.ananda.liquidbounce.LbModule
import dev.unknownuser.ananda.liquidbounce.LbPalette
import dev.unknownuser.ananda.liquidbounce.LbPanel
import dev.unknownuser.ananda.liquidbounce.LbSearch
import dev.unknownuser.ananda.liquidbounce.LbSearchModule
import dev.unknownuser.ananda.reactive.stateOf
import dev.unknownuser.ananda.theme.Palette
import dev.unknownuser.ananda.theme.Theme
import dev.unknownuser.ananda.theme.Typography
import dev.unknownuser.ananda.window.RenderMode
import dev.unknownuser.ananda.window.SkiaWindow
import java.awt.Color

private const val WindowWidth = 920
private const val WindowHeight = 640
private const val DebugScale = 2f
private const val ShowSearchInDebug = false
private const val SearchTop = 32f
private const val PanelTop = 108f

fun main() {
    val descriptionAnchor = stateOf<LbDescriptionAnchor?>(null)
    val panelExpanded = stateOf(true)
    val velocityEnabled = stateOf(true)
    val killAuraEnabled = stateOf(false)
    val autoBlockEnabled = stateOf(true)
    val velocityExpanded = stateOf(false)
    val horizontalRange = stateOf(3.2f)
    val verticalRange = stateOf(1.5f)
    val throughWalls = stateOf(false)

    val searchModules = listOf(
        LbSearchModule("Velocity", velocityEnabled, listOf("Vel")),
        LbSearchModule("KillAura", killAuraEnabled, listOf("Aura", "KA")),
        LbSearchModule("AutoBlock", autoBlockEnabled)
    )

    lateinit var panel: LbPanel
    panel = LbPanel(
        category = "Combat",
        expanded = panelExpanded,
        x = 40f,
        y = PanelTop,
        maxModulesHeight = 320f,
        modules = listOf(
            LbModule(
                name = "Velocity",
                enabled = velocityEnabled,
                expanded = velocityExpanded,
                description = "Reduces knockback taken from hits",
                aliases = listOf("Vel"),
                hasSettings = true,
                onDescription = { descriptionAnchor.value = it },
                settings = listOf(
                    LbFloatSetting("Horizontal", horizontalRange, rangeMin = 0f, rangeMax = 4f, suffix = "blocks", width = 240f),
                    LbFloatSetting("Vertical", verticalRange, rangeMin = 0f, rangeMax = 2f, suffix = "blocks", width = 240f),
                    LbBooleanSetting("Through Walls", throughWalls, width = 240f)
                )
            ),
            LbModule(
                name = "KillAura",
                enabled = killAuraEnabled,
                description = "Automatically attacks nearby entities",
                aliases = listOf("Aura"),
                hasSettings = false,
                onDescription = { descriptionAnchor.value = it }
            ),
            LbModule(
                name = "AutoBlock",
                enabled = autoBlockEnabled,
                description = "Blocks incoming attacks automatically",
                hasSettings = false,
                onDescription = { descriptionAnchor.value = it }
            )
        )
    )
    velocityExpanded.subscribe { panel.resizeScrollToContent(320f) }

    val search = LbSearch(
        modules = searchModules,
        y = SearchTop,
        onToggleModule = { name, enabled ->
            when (name) {
                "Velocity" -> velocityEnabled.value = enabled
                "KillAura" -> killAuraEnabled.value = enabled
                "AutoBlock" -> autoBlockEnabled.value = enabled
            }
        }
    )

    val description = LbDescription(descriptionAnchor)

    val scene = Scene().apply {
        uiScale = DebugScale
        theme = Theme(
            palette = Palette(
                background = Color.WHITE,
                surface = LbPalette.ClickGuiPanelElevated,
                primary = LbPalette.Accent,
                accent = LbPalette.Accent,
                text = LbPalette.ClickGuiText,
                mutedText = LbPalette.ClickGuiTextDimmed,
                border = LbPalette.ClickGuiBorder
            ),
            typography = Typography(fontFamily = "Inter", bodySize = 12f)
        )
        add(Component(width = WindowWidth.toFloat(), height = WindowHeight.toFloat()).background(Color.WHITE))
        add(panel)
        if (ShowSearchInDebug) add(search)
        add(description)
    }

    SkiaWindow(
        title = "LiquidBounce Widgets Demo",
        width = (WindowWidth * DebugScale).toInt(),
        height = (WindowHeight * DebugScale).toInt(),
        scene = scene,
        clearColor = org.jetbrains.skia.Color.WHITE,
        renderMode = RenderMode.OnDemand
    ).show()
}
