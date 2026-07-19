package dev.unknownuser.ananda

import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.component.Label as ComponentLabel
import dev.unknownuser.ananda.component.ScrollContainer
import dev.unknownuser.ananda.dsl.scene
import dev.unknownuser.ananda.layout.Constraints
import dev.unknownuser.ananda.layout.placeOutOfFlow
import dev.unknownuser.ananda.style.BackgroundLayer
import dev.unknownuser.ananda.style.GradientStop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiquidBounceThemeFragmentsTest {

    @Test
    fun convertedClickGuiPanelDslLaysOutTitleGrid() {
        val panel = LiquidBounceConvertedThemeDsl.clickGuiPanel("Combat")
        panel.measure(Constraints(400f, 600f))
        panel.layout.layout(panel, Constraints(panel.measuredWidth, panel.measuredHeight))

        assertEquals(270f, panel.measuredWidth)
        val title = panel.children.first()
        title.layout.layout(title, Constraints(title.measuredWidth, title.measuredHeight))
        assertEquals(3, title.children.size)
        assertTrue(title.children[1].x > title.children[0].x)
    }

    @Test
    fun convertedClickGuiPanelDslDrawsLayeredBackgroundAndBackdrop() {
        val panel = LiquidBounceConvertedThemeDsl.clickGuiPanel()
        val backend = TestRenderBackend()

        panel.draw(RenderContext(backend, width = 400, height = 600))

        assertTrue(backend.frostedRects.isNotEmpty())
        assertTrue(backend.gradients.any { it.direction == GradientDirection.Vertical })
        assertTrue(backend.rects.any { it.fill?.alpha == 230 })
    }

    @Test
    fun convertedClickGuiModuleEnabledDrawsGradientInsetShadowAndGlowBar() {
        val module = LiquidBounceConvertedThemeDsl.clickGuiModuleRow("Velocity", enabled = true)
        val backend = TestRenderBackend()

        module.draw(RenderContext(backend, width = 270, height = 40))

        val enabledGradient = backend.gradients.first { it.direction == GradientDirection.Horizontal }
        assertEquals(46, enabledGradient.stops.first().first.alpha)
        assertEquals(14, enabledGradient.stops.last().first.alpha)
        assertTrue(backend.shadows.any { it.shadow.inset })
        val glow = backend.shadows.first { it.shadow.blurRadius == 12f }
        assertTrue(!glow.shadow.inset)
        assertEquals(3f, backend.roundedRects.first { it.width == 3f }.width)
    }

    @Test
    fun convertedClickGuiSearchBarAnchorsWithPercentAndDrawsBackdrop() {
        val search = LiquidBounceConvertedThemeDsl.clickGuiSearchBar()
        search.measure(Constraints(800f, 600f))
        search.placeOutOfFlow(800f, 600f)

        assertEquals(0.5f, search.leftPercent)
        assertEquals(400f, search.x)

        val backend = TestRenderBackend()
        search.draw(RenderContext(backend, width = 800, height = 600))

        assertTrue(backend.frostedRects.isNotEmpty())
        assertTrue(backend.shadows.isNotEmpty())
    }

    @Test
    fun convertedHudKeyBindsChipEllipsisOnRealTree() {
        val longName = "SuperLongModuleNameThatShouldEllipsize"
        val chip = LiquidBounceConvertedThemeDsl.hudKeyBindsChip(longName)
        val measured = chip.measure(Constraints(400f, 200f))
        assertTrue(measured.width in 150f..200f)

        val backend = TestRenderBackend()
        chip.draw(RenderContext(backend, width = 400, height = 200))

        val moduleLabel = findLabel(chip, "Super")
        val maxTextWidth = moduleLabel.measuredWidth - moduleLabel.padding.left - moduleLabel.padding.right
        val expected = expectedEllipsisText(longName, maxTextWidth, textSize = 14f)
        val drawn = backend.texts.first { it.text.startsWith("Super") }.text
        assertEquals(expected, drawn)
    }

    @Test
    fun convertedThemeScrollListViaSceneDsl() {
        val panel = LiquidBounceConvertedThemeDsl.clickGuiPanel()
        val scroll = panel.children.last() as ScrollContainer
        assertTrue(scroll.clipToBounds)
        assertEquals(1, scroll.children.size)
    }

    @Test
    fun sceneDslScrollEntryStillWorks() {
        val built = scene {
            scroll(width = 270, height = 120) {
                label("Module A", height = 34f)
            }
        }
        assertEquals(1, (built.snapshot().single() as ScrollContainer).children.size)
    }

    private fun findLabel(root: Component, prefix: String): ComponentLabel =
        findLabelOrNull(root, prefix) ?: error("label not found for prefix=$prefix")

    private fun findLabelOrNull(root: Component, prefix: String): ComponentLabel? {
        if (root is ComponentLabel && root.text.startsWith(prefix)) return root
        return root.children.firstNotNullOfOrNull { findLabelOrNull(it, prefix) }
    }
}