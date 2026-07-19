package dev.unknownuser.ananda

import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.draw.Scene
import dev.unknownuser.ananda.event.PointerEvent
import dev.unknownuser.ananda.liquidbounce.LbBooleanSetting
import dev.unknownuser.ananda.liquidbounce.LbDescription
import dev.unknownuser.ananda.liquidbounce.LbDescriptionAnchor
import dev.unknownuser.ananda.liquidbounce.LbDescriptionSide
import dev.unknownuser.ananda.liquidbounce.LbFloatSetting
import dev.unknownuser.ananda.liquidbounce.LbModule
import dev.unknownuser.ananda.liquidbounce.LbPanel
import dev.unknownuser.ananda.liquidbounce.LbPalette
import dev.unknownuser.ananda.liquidbounce.LbRangeSlider
import dev.unknownuser.ananda.liquidbounce.LbSearch
import dev.unknownuser.ananda.liquidbounce.LbSearchModule
import dev.unknownuser.ananda.liquidbounce.LbSwitch
import dev.unknownuser.ananda.layout.Alignment
import dev.unknownuser.ananda.layout.ColumnLayout
import dev.unknownuser.ananda.layout.Constraints
import dev.unknownuser.ananda.layout.placeOutOfFlow
import dev.unknownuser.ananda.reactive.stateOf
import dev.unknownuser.ananda.time.TimeFrame
import java.awt.event.MouseEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiquidBounceWidgetsTest {

    @Test
    fun lbSwitchDrawsAccentTrackWhenChecked() {
        val checked = stateOf(true)
        val sw = LbSwitch(checked, width = 28f, height = 16f)
        val backend = TestRenderBackend()
        sw.draw(RenderContext(backend, width = 40, height = 20))
        assertTrue(backend.roundedRects.any { it.fill == LbPalette.Accent || it.fill?.red == LbPalette.Accent.red })
    }

    @Test
    fun lbBooleanSettingLaysOutSwitchAndLabel() {
        val setting = LbBooleanSetting("Through Walls", stateOf(true), width = 200f)
        setting.measure(Constraints(220f, 80f))
        assertEquals(2, setting.children.size)
        assertTrue(setting.measuredHeight >= 28f)
    }

    @Test
    fun lbFloatSettingUsesGridRowsForNameValueAndSlider() {
        val value = stateOf(2.5f)
        val setting = LbFloatSetting("Horizontal", value, rangeMin = 0f, rangeMax = 4f, suffix = "blocks", width = 220f)
        setting.measure(Constraints(240f, 80f))
        assertTrue(setting.measuredHeight >= 46f)
        assertEquals(4, setting.children.size)
    }

    @Test
    fun lbRangeSliderUsesSceneCoordinatesWhenNested() {
        val value = stateOf(0f)
        val setting = LbFloatSetting("Horizontal", value, rangeMin = 0f, rangeMax = 4f, suffix = "blocks", width = 240f)
        val module = LbModule(
            name = "Velocity",
            expanded = stateOf(true),
            hasSettings = true,
            width = 270f,
            settings = listOf(setting)
        )
        val panel = LbPanel(
            category = "Combat",
            modules = listOf(module),
            x = 40f,
            y = 80f
        )
        val scene = Scene()
        scene.add(panel)
        panel.draw(RenderContext(TestRenderBackend(), width = 360, height = 260))

        val slider = setting.children.last() as LbRangeSlider
        val (sliderX, sliderY) = slider.globalPosition()
        val trackWidth = slider.measuredWidth - slider.padding.right
        val pointerX = sliderX + trackWidth * 0.75f
        val pointerY = sliderY + slider.measuredHeight / 2f

        scene.dispatch(PointerEvent("pointerDown", pointerX, pointerY, MouseEvent.BUTTON1))
        scene.dispatch(PointerEvent("pointerUp", pointerX, pointerY, MouseEvent.BUTTON1))

        assertEquals(3f, value.value, 0.05f)
    }

    @Test
    fun lbModuleEnabledDrawsAccentGradientAndGlowBar() {
        val module = LbModule(name = "Velocity", enabled = stateOf(true), width = 270f)
        val backend = TestRenderBackend()
        module.draw(RenderContext(backend, width = 270, height = 40))
        val gradient = backend.gradients.first { it.direction == GradientDirection.Horizontal }
        assertEquals(30, gradient.stops.first().first.alpha)
        assertTrue(backend.shadows.any { it.shadow.inset })
        assertEquals(3f, backend.roundedRects.first { it.width == 3f }.width)
    }

    @Test
    fun lbModuleCollapseOmitsSettingsFromLayout() {
        val expanded = stateOf(false)
        val module = LbModule(
            name = "Velocity",
            enabled = stateOf(true),
            expanded = expanded,
            hasSettings = true,
            width = 270f,
            settings = listOf(LbBooleanSetting("Through Walls", stateOf(false), width = 240f))
        )
        assertEquals(0, module.children[1].children.size)
        module.measure(Constraints(270f, 400f))
        val collapsedHeight = module.measuredHeight
        expanded.value = true
        assertEquals(1, module.children[1].children.size)
        repeat(20) {
            module.draw(RenderContext(TestRenderBackend(), width = 270, height = 400, time = TimeFrame(deltaSeconds = 0.016f)))
        }
        module.measure(Constraints(270f, 400f))
        val expandedHeight = module.measuredHeight
        assertTrue(expandedHeight > collapsedHeight + 20f)
    }

    @Test
    fun lbModuleSettingsFadeInWhenExpanded() {
        val expanded = stateOf(false)
        val module = LbModule(
            name = "Velocity",
            expanded = expanded,
            hasSettings = true,
            width = 270f,
            settings = listOf(LbBooleanSetting("Through Walls", stateOf(false), width = 240f))
        )
        val settingsPanel = module.children[1]

        expanded.value = true
        module.draw(RenderContext(TestRenderBackend(), width = 270, height = 120, time = TimeFrame(deltaSeconds = 0.016f)))

        assertTrue(settingsPanel.visible)
        assertTrue(settingsPanel.opacity in 0f..0.999f)
        assertTrue(settingsPanel.measuredHeight in 0f..27.999f)
    }

    @Test
    fun lbPanelStacksModulesCompactly() {
        val panel = LbPanel(
            category = "Combat",
            modules = listOf(
                LbModule("Velocity", enabled = stateOf(true), width = 270f),
                LbModule("KillAura", width = 270f),
                LbModule("AutoBlock", enabled = stateOf(true), width = 270f)
            )
        )
        panel.measure(Constraints(270f, 600f))
        panel.layout.layout(panel, Constraints(270f, 600f))
        val scroll = panel.children[1] as dev.unknownuser.ananda.component.ScrollContainer
        val modules = scroll.children
        assertEquals(0f, modules[0].y)
        assertEquals(modules[0].measuredHeight, modules[1].y, 0.5f)
        assertEquals(modules[1].y + modules[1].measuredHeight, modules[2].y, 0.5f)
    }

    @Test
    fun lbSearchCanBePlacedAbovePanelWithoutOverlap() {
        val search = LbSearch(
            modules = listOf(LbSearchModule("Velocity", stateOf(true))),
            y = 32f
        )
        val panel = LbPanel(
            category = "Combat",
            modules = listOf(LbModule("Velocity", width = 270f)),
            x = 40f,
            y = 108f
        )

        search.measure(Constraints(920f, 640f))
        search.placeOutOfFlow(920f, 640f)

        assertTrue(search.y + search.measuredHeight <= panel.y)
    }

    @Test
    fun collapsedModuleDoesNotReserveSettingsHeight() {
        val module = LbModule(
            name = "Velocity",
            expanded = stateOf(false),
            hasSettings = true,
            width = 270f,
            settings = listOf(LbBooleanSetting("Through Walls", stateOf(false), width = 240f))
        )
        module.measure(Constraints(270f, 400f))
        assertEquals(34f, module.measuredHeight, 0.5f)
    }

    @Test
    fun layoutDoesNotTurnMeasuredSizeIntoExplicitSize() {
        val parent = Component(width = 180f).apply { layout = ColumnLayout(crossAxisAlignment = Alignment.Stretch) }
        val child = Component(height = 20f)
        parent.add(child)
        parent.measure(Constraints(180f, 100f))
        parent.layout.layout(parent, Constraints(parent.measuredWidth, parent.measuredHeight))
        child.draw(RenderContext(TestRenderBackend(), width = 180, height = 100))
        parent.width = 320f
        parent.measure(Constraints(320f, 100f))
        parent.layout.layout(parent, Constraints(parent.measuredWidth, parent.measuredHeight))
        assertEquals(320f, child.measuredWidth, 0.5f)
    }

    @Test
    fun lbSearchShowsResultsWhenQueryPresent() {
        val velocity = stateOf(true)
        val search = LbSearch(
            modules = listOf(
                LbSearchModule("Velocity", velocity),
                LbSearchModule("KillAura", stateOf(false))
            ),
            query = stateOf("vel")
        )
        search.measure(Constraints(560f, 400f))
        assertTrue(search.children.size >= 2)
        val results = search.children[1]
        assertTrue(results.visible)
    }

    @Test
    fun lbDescriptionDrawsWhenAnchorSet() {
        val anchor = stateOf<LbDescriptionAnchor?>(
            LbDescriptionAnchor(400f, 200f, LbDescriptionSide.Right, "Reduces knockback")
        )
        val tooltip = LbDescription(anchor)
        val backend = TestRenderBackend()
        tooltip.draw(RenderContext(backend, width = 800, height = 600))
        assertTrue(backend.texts.any { it.text.contains("knockback") })
        assertTrue(backend.roundedRects.isNotEmpty())
    }

    @Test
    fun expandedModuleRowStillPublishesDescriptionAnchor() {
        val anchor = stateOf<LbDescriptionAnchor?>(null)
        val expanded = stateOf(true)
        val module = LbModule(
            name = "Velocity",
            expanded = expanded,
            hasSettings = true,
            width = 270f,
            onDescription = { anchor.value = it },
            settings = listOf(LbBooleanSetting("Through Walls", stateOf(false), width = 240f))
        )
        val scene = Scene()
        scene.add(module)
        module.draw(RenderContext(TestRenderBackend(), width = 400, height = 400, time = TimeFrame(deltaSeconds = 0.016f)))

        val rowCenterY = module.globalPosition().second + 17f
        scene.dispatch(PointerEvent("pointerMove", 135f, 17f))

        val resolvedAnchor = anchor.value
        assertEquals(LbDescriptionSide.Right, resolvedAnchor?.anchor)
        assertTrue(resolvedAnchor != null)
        assertEquals(rowCenterY, resolvedAnchor.y, 1.5f)
    }

    @Test
    fun lbDescriptionWrapsLongTextIntoMultipleLines() {
        val anchor = stateOf<LbDescriptionAnchor?>(
            LbDescriptionAnchor(320f, 200f, LbDescriptionSide.Right, "Blocks incoming attacks automatically")
        )
        val tooltip = LbDescription(anchor)
        val backend = TestRenderBackend()

        tooltip.draw(RenderContext(backend, width = 800, height = 600))

        assertTrue(backend.texts.size >= 2)
    }

    @Test
    fun lbDescriptionCentersSingleLineTextVertically() {
        val anchor = stateOf<LbDescriptionAnchor?>(
            LbDescriptionAnchor(320f, 200f, LbDescriptionSide.Right, "AutoBlock")
        )
        val tooltip = LbDescription(anchor)
        val backend = TestRenderBackend()

        tooltip.draw(RenderContext(backend, width = 800, height = 600))

        val bubble = backend.roundedRects.last()
        val text = backend.texts.last { it.text == "AutoBlock" }
        val expectedBaseline = bubble.y + (bubble.height - 12f * 1.45f) / 2f + 12f * 0.86f
        assertEquals(expectedBaseline, text.y, 1.5f)
    }
}
