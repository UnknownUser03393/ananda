package dev.unknownuser.ananda

import dev.unknownuser.ananda.animation.Animator
import dev.unknownuser.ananda.animation.RepeatMode
import dev.unknownuser.ananda.annotations.FunctionalComponent as FunctionalComponentAnnotation
import dev.unknownuser.ananda.annotations.functional
import dev.unknownuser.ananda.backend.CornerRadii
import dev.unknownuser.ananda.backend.FramebufferRegion
import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.ImageFit
import dev.unknownuser.ananda.backend.ImagePosition
import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextureRegion
import dev.unknownuser.ananda.component.Button
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.component.CurveChart
import dev.unknownuser.ananda.component.Dropdown
import dev.unknownuser.ananda.component.ElevatedPanel
import dev.unknownuser.ananda.component.FramebufferView
import dev.unknownuser.ananda.component.FunctionalComponent
import dev.unknownuser.ananda.component.FunctionalScope
import dev.unknownuser.ananda.component.GlowFrame
import dev.unknownuser.ananda.component.GlowLine
import dev.unknownuser.ananda.component.GlowOrb
import dev.unknownuser.ananda.component.KeyBind
import dev.unknownuser.ananda.component.Label as ComponentLabel
import dev.unknownuser.ananda.component.ProgressBar
import dev.unknownuser.ananda.component.RadioButton
import dev.unknownuser.ananda.component.ScrollContainer
import dev.unknownuser.ananda.component.Separator
import dev.unknownuser.ananda.component.TextField
import dev.unknownuser.ananda.component.ToggleSwitch
import dev.unknownuser.ananda.component.currentFunctionalScope
import dev.unknownuser.ananda.draw.Scene
import dev.unknownuser.ananda.dsl.Button
import dev.unknownuser.ananda.dsl.Label
import dev.unknownuser.ananda.dsl.Ui
import dev.unknownuser.ananda.dsl.scene
import dev.unknownuser.ananda.dsl.ui
import dev.unknownuser.ananda.event.ImeEvent
import dev.unknownuser.ananda.event.KeyDown
import dev.unknownuser.ananda.event.KeyEvent
import dev.unknownuser.ananda.event.PointerDown
import dev.unknownuser.ananda.event.PointerEvent
import dev.unknownuser.ananda.event.TextInputEvent
import dev.unknownuser.ananda.event.digitValue
import dev.unknownuser.ananda.event.or
import dev.unknownuser.ananda.interaction.InteractionProvider
import dev.unknownuser.ananda.layout.ColumnLayout
import dev.unknownuser.ananda.layout.Constraints
import dev.unknownuser.ananda.layout.GridLayout
import dev.unknownuser.ananda.layout.Insets
import dev.unknownuser.ananda.layout.MainAxisAlignment
import dev.unknownuser.ananda.layout.Positioning
import dev.unknownuser.ananda.layout.RowLayout
import dev.unknownuser.ananda.layout.WrapLayout
import dev.unknownuser.ananda.layout.fr
import dev.unknownuser.ananda.layout.gridAreas
import dev.unknownuser.ananda.layout.maxContent
import dev.unknownuser.ananda.layout.px
import dev.unknownuser.ananda.layout.repeat
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.inc
import dev.unknownuser.ananda.reactive.plusAssign
import dev.unknownuser.ananda.reactive.stateOf
import dev.unknownuser.ananda.style.BackgroundLayer
import dev.unknownuser.ananda.style.BorderSides
import dev.unknownuser.ananda.style.GradientStop
import dev.unknownuser.ananda.style.PseudoElement
import dev.unknownuser.ananda.style.PseudoVisibility
import dev.unknownuser.ananda.style.StateStyles
import dev.unknownuser.ananda.style.Style
import dev.unknownuser.ananda.style.TextAlign
import dev.unknownuser.ananda.style.TextOverflow
import dev.unknownuser.ananda.style.OverflowWrap
import dev.unknownuser.ananda.style.WhiteSpace
import dev.unknownuser.ananda.settings.SettingGroup
import dev.unknownuser.ananda.settings.SettingsPanel
import dev.unknownuser.ananda.settings.bool
import dev.unknownuser.ananda.theme.Palette
import dev.unknownuser.ananda.theme.Theme
import dev.unknownuser.ananda.time.TimeFrame
import dev.unknownuser.ananda.time.TimeSystem
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.awt.Color
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnandaSystemsTest {
    @Test
    fun skiaTextMeasurementIncludesWhitespaceAdvance() {
        org.jetbrains.skia.Surface.makeRasterN32Premul(200, 80).use { surface ->
            val backend = dev.unknownuser.ananda.backend.SkiaRenderBackend(surface.canvas)
            val style = dev.unknownuser.ananda.backend.TextStyle(14f, Color.WHITE, "Segoe UI")

            assertTrue(backend.measureText("A B", style).first > backend.measureText("AB", style).first)
        }
    }

    @Test
    fun skiaBackendCanRegisterFontFilesForCssFontFaceFamilies() {
        val windir = System.getenv("WINDIR") ?: "C:\\Windows"
        val font = Path.of(windir, "Fonts", "arial.ttf")
        assumeTrue(Files.exists(font), "System Arial font is not available")

        org.jetbrains.skia.Surface.makeRasterN32Premul(200, 80).use { surface ->
            val backend = dev.unknownuser.ananda.backend.SkiaRenderBackend(surface.canvas)
            backend.registerFontFromFile("AnandaRegisteredFont", font, weight = 700)
            val style = dev.unknownuser.ananda.backend.TextStyle(
                size = 14f,
                color = Color.WHITE,
                fontFamily = "AnandaRegisteredFont",
                fontWeight = 700
            )

            assertTrue(backend.measureText("Registered", style).first > 0f)
        }
    }

    @Test
    fun animationAdvancesProgress() {
        val samples = mutableListOf<Float>()
        val animator = Animator()

        animator.animate(durationSeconds = 1f) { progress -> samples += progress }
        animator.update(0.25f)
        animator.update(0.75f)

        assertEquals(listOf(0.25f, 1f), samples)
    }

    @Test
    fun animationCanPauseResumeAndCancel() {
        val samples = mutableListOf<Float>()
        var cancelled = false
        val animator = Animator()
        val animation = animator.add(
            dev.unknownuser.ananda.animation.Animation(
                durationSeconds = 1f,
                update = { samples += it },
                onCancelled = { cancelled = true }
            )
        )

        animator.update(0.25f)
        animation.pause()
        animator.update(0.25f)
        animation.resume()
        animator.update(0.25f)
        animation.cancel()
        animator.update(0.25f)

        assertEquals(listOf(0.25f, 0.5f), samples)
        assertTrue(cancelled)
        assertTrue(!animator.hasActiveAnimations())
    }

    @Test
    fun animationSupportsFiniteReverseRepeatsAndFloatTweens() {
        val progressSamples = mutableListOf<Float>()
        val valueSamples = mutableListOf<Float>()
        val animator = Animator()

        animator.animate(durationSeconds = 1f, repeatCount = 1, repeatMode = RepeatMode.Reverse) {
            progressSamples += it
        }
        animator.animateFloat(from = 10f, to = 20f, durationSeconds = 1f) {
            valueSamples += it
        }

        animator.update(1f)
        animator.update(0.5f)
        animator.update(0.5f)

        assertEquals(listOf(1f, 0.5f, 0f), progressSamples)
        assertEquals(20f, valueSamples.first())
    }

    @Test
    fun timeSystemTracksScaledAndUnscaledTime() {
        val time = TimeSystem()

        time.timeScale = 2f
        time.update(0.5f)
        time.pause()
        time.update(0.5f)

        assertEquals(1f, time.elapsedSeconds)
        assertEquals(1f, time.unscaledElapsedSeconds)
        assertEquals(0f, time.deltaSeconds)
        assertEquals(0.5f, time.unscaledDeltaSeconds)
        assertEquals(2L, time.frame)
    }

    @Test
    fun timeSystemRunsScaledAndUnscaledTimers() {
        val time = TimeSystem()
        var scaledTicks = 0
        var unscaledTicks = 0

        time.every(0.5f) { scaledTicks += 1 }
        time.every(0.5f, useScaledTime = false) { unscaledTicks += 1 }
        time.pause()
        time.update(0.5f)
        time.resume()
        time.update(0.5f)

        assertEquals(1, scaledTicks)
        assertEquals(2, unscaledTicks)
    }

    @Test
    fun sceneAnimationsUseScaledTime() {
        val scene = Scene()
        var value = 0f

        scene.time.timeScale = 2f
        scene.animateFloat(0f, 10f, durationSeconds = 1f) {
            value = it
        }
        scene.update(0.25f)

        assertEquals(5f, value)
    }

    @Test
    fun rowLayoutPositionsChildren() {
        val parent = Component(width = 300f, height = 80f).apply {
            layout = RowLayout(gap = 10f)
            padding(5f)
            add(Component(width = 40f, height = 20f))
            add(Component(width = 60f, height = 20f))
        }

        parent.layout.layout(parent, dev.unknownuser.ananda.layout.Constraints(parent.width, parent.height))

        assertEquals(5f, parent.children[0].x)
        assertEquals(55f, parent.children[1].x)
        assertEquals(5f, parent.children[1].y)
    }

    @Test
    fun measureRecursesThroughPadding() {
        val parent = Component(width = 0f, height = 0f).apply {
            layout = ColumnLayout(gap = 4f)
            padding(10f)
            add(Component(width = 80f, height = 20f))
            add(Component(width = 40f, height = 30f))
        }

        val measured = parent.measure(Constraints(500f, 500f))

        assertEquals(100f, measured.width)
        assertEquals(74f, measured.height)
    }

    @Test
    fun sceneDispatchesPointerToTopmostComponentFirst() {
        val scene = Scene()
        val bottom = Component(0f, 0f, 100f, 100f)
        val top = Component(10f, 10f, 80f, 80f)
        var topHandled = false
        var bottomHandled = false

        bottom.on("pointerDown") { bottomHandled = true }
        top.on("pointerDown") {
            topHandled = true
            it.consume()
        }

        scene.add(bottom)
        scene.add(top)
        scene.dispatch(PointerEvent("pointerDown", 20f, 20f))

        assertTrue(topHandled)
        assertTrue(!bottomHandled)
    }

    @Test
    fun pointerDoesNotFallThroughToCoveredSibling() {
        val scene = Scene()
        val bottom = Component(0f, 0f, 100f, 100f)
        val top = Component(10f, 10f, 80f, 80f)
        var bottomHandled = false
        var topHandled = false

        bottom.on("pointerDown") { bottomHandled = true }
        top.on("pointerDown") { topHandled = true }

        scene.add(bottom)
        scene.add(top)
        scene.dispatch(PointerEvent("pointerDown", 20f, 20f))

        assertTrue(topHandled)
        assertTrue(!bottomHandled)
    }

    @Test
    fun typedEventMatcherDispatchesMatchingEventsOnly() {
        val component = Component()
        var pointerDownHandled = false
        var keyActivations = 0
        val digits = mutableListOf<Int>()

        component.on(PointerDown) {
            pointerDownHandled = true
        }
        component.on(KeyDown.Enter or KeyDown.Space) {
            keyActivations += 1
        }
        component.on(KeyDown.Digit) {
            digits += it.digitValue() ?: -1
        }

        component.events.emit(PointerEvent("pointerMove", 0f, 0f))
        component.events.emit(PointerEvent("pointerDown", 0f, 0f))
        component.events.emit(KeyEvent("keyDown", java.awt.event.KeyEvent.VK_ESCAPE))
        component.events.emit(KeyEvent("keyDown", java.awt.event.KeyEvent.VK_ENTER))
        component.events.emit(KeyEvent("keyDown", java.awt.event.KeyEvent.VK_SPACE))
        component.events.emit(KeyEvent("keyDown", java.awt.event.KeyEvent.VK_7))
        component.events.emit(KeyEvent("keyDown", java.awt.event.KeyEvent.VK_NUMPAD7))

        assertTrue(pointerDownHandled)
        assertEquals(2, keyActivations)
        assertEquals(listOf(7, 7), digits)
    }

    @Test
    fun disabledFocusedComponentFiltersInputDuringDispatch() {
        val scene = Scene()
        val component = Component(0f, 0f, 100f, 40f).apply {
            focusable = true
        }
        var keyHandled = false

        component.on(KeyDown) {
            keyHandled = true
        }

        scene.add(component)
        scene.focus(component)
        component.disabled = true
        scene.dispatch(KeyEvent("keyDown", java.awt.event.KeyEvent.VK_ENTER))

        assertTrue(!keyHandled)
    }

    @Test
    fun buttonClickRunsOnPointerUp() {
        val scene = Scene()
        var clicked = 0
        scene.add(Button("Click", 0f, 0f, 100f, 40f) { clicked += 1 })

        scene.dispatch(PointerEvent("pointerDown", 10f, 10f))
        scene.dispatch(PointerEvent("pointerUp", 10f, 10f))

        assertEquals(1, clicked)
    }

    @Test
    fun toggleSwitchTogglesState() {
        val scene = Scene()
        val checked = stateOf(false)
        scene.add(ToggleSwitch(checked, 0f, 0f, 52f, 28f))

        scene.dispatch(PointerEvent("pointerDown", 10f, 10f))

        assertTrue(checked.value)
    }

    @Test
    fun radioButtonSelectsOption() {
        val scene = Scene()
        val selected = stateOf("a")
        scene.add(RadioButton(selected, "b", "Option B", 0f, 0f, 160f, 28f))

        scene.dispatch(PointerEvent("pointerDown", 10f, 10f))

        assertEquals("b", selected.value)
    }

    @Test
    fun passiveGuiComponentsRender() {
        val backend = TestRenderBackend()
        val context = RenderContext(backend, width = 800, height = 600)

        ProgressBar(stateOf(0.5f), width = 120f, height = 12f).draw(context)
        Separator(width = 120f, height = 1f).draw(context)
        ElevatedPanel(width = 120f, height = 64f).draw(context)
        GlowOrb(width = 80f, height = 80f).draw(context)
        GlowLine(0f, 0f, 80f, 40f).draw(context)
        GlowFrame(width = 120f, height = 48f).draw(context)
        CurveChart(width = 160f, height = 80f, values = listOf(0.1f, 0.8f, 0.4f)).draw(context)
        assertTrue(backend.lines.isNotEmpty())
    }

    @Test
    fun framebufferViewDelegatesToBackend() {
        val framebuffer = FramebufferRegion(
            id = "minecraft:main",
            colorAttachment = 42,
            width = 320,
            height = 240
        )
        val backend = TestRenderBackend()

        FramebufferView(framebuffer, width = 160f, height = 120f, alpha = 0.75f)
            .draw(RenderContext(backend, width = 800, height = 600))

        val draw = backend.framebuffers.single()
        assertEquals(framebuffer, draw.framebuffer)
        assertEquals(160f, draw.width)
        assertEquals(120f, draw.height)
        assertEquals(0.75f, draw.alpha)
    }

    @Test
    fun sceneSatisfiesInteractionProviderBoundary() {
        val scene = Scene()
        val interactions: InteractionProvider = scene
        val component = Component(0f, 0f, 100f, 100f).apply {
            focusable = true
        }
        var focused = false

        component.events.on("focus") { focused = true }
        scene.add(component)
        interactions.dispatch(PointerEvent("pointerDown", 20f, 20f))

        assertEquals(component, interactions.focusedComponent)
        assertEquals(component, interactions.hoveredComponent)
        assertTrue(component.interaction.pressed)
        assertTrue(focused)

        interactions.dispatch(PointerEvent("pointerUp", 20f, 20f))

        assertTrue(!component.interaction.pressed)
    }

    @Test
    fun functionalComponentRebuildsWhenStateChanges() {
        val count = stateOf(1)
        val component = FunctionalComponent(width = 200f, height = 40f) {
            label("count=${count.value}", width = 200f, height = 40f)
        }
        val backend = TestRenderBackend()
        val context = RenderContext(backend, width = 800, height = 600)

        component.draw(context)
        count.value = 2
        component.draw(context)

        assertEquals("count=1", backend.texts[0].text)
        assertEquals("count=2", backend.texts[1].text)
    }

    @Test
    fun annotatedFunctionalComponentCanBeMountedFromDsl() {
        val count = stateOf(1)
        val scene = scene {
            mount(width = 200f, height = 40f) {
                CounterLabel(count)
            }
        }
        val backend = TestRenderBackend()
        val context = RenderContext(backend, width = 800, height = 600)

        scene.render(context)
        count.value = 2
        scene.render(context)

        assertEquals("count=1", backend.texts[0].text)
        assertEquals("count=2", backend.texts[1].text)
    }

    @Test
    fun uiFunctionCanHoldLocalStateAcrossRebuilds() {
        val scene = scene {
            mount(width = 200f, height = 80f, component = CounterUi("Counter"))
        }
        val backend = TestRenderBackend()
        val context = RenderContext(backend, width = 800, height = 600)

        scene.render(context)
        scene.dispatch(PointerEvent("pointerDown", 10f, 50f))
        scene.dispatch(PointerEvent("pointerUp", 10f, 50f))
        scene.render(context)

        assertEquals("Counter : 0", backend.texts[0].text)
        assertTrue(backend.texts.any { it.text == "Counter : 1" })
    }

    @Test
    fun themeFlowsThroughFunctionalComponent() {
        val theme = Theme(palette = Palette(text = Color(12, 34, 56)))
        val scene = Scene().apply {
            this.theme = theme
            add(FunctionalComponent(width = 200f, height = 40f) {
                label("themed", width = 200f, height = 40f)
            })
        }
        val backend = TestRenderBackend()

        scene.render(RenderContext(backend, width = 800, height = 600))

        assertEquals(Color(12, 34, 56), backend.texts.single().style.color)
    }

    @Test
    fun lifecycleHooksRunOnSceneAttachAndDetach() {
        val scene = Scene()
        var mounted = 0
        var unmounted = 0
        val component = Component(width = 10f, height = 10f)
            .onMount { mounted += 1 }
            .onUnmount { unmounted += 1 }

        scene.add(component)
        scene.remove(component)

        assertEquals(1, mounted)
        assertEquals(1, unmounted)
    }

    @Test
    fun focusedTextFieldReceivesTextAndImeCommit() {
        val state = stateOf("")
        val field = TextField(state, width = 120f, height = 32f)
        val scene = Scene().apply { add(field) }

        scene.dispatch(PointerEvent("pointerDown", 4f, 4f))
        scene.dispatch(TextInputEvent("a"))
        scene.dispatchFocused(ImeEvent("imeCommit", committedText = "中"))

        assertEquals("a中", state.value)
    }
    @Test
    fun focusNextCyclesFocusableComponents() {
        val scene = Scene()
        val first = Component(0f, 0f, 20f, 20f).apply { focusable = true }
        val second = Component(24f, 0f, 20f, 20f).apply { focusable = true }
        scene.add(first)
        scene.add(second)

        assertTrue(scene.focusNext())
        assertEquals(first, scene.focusedComponent)
        assertTrue(scene.focusNext())
        assertEquals(second, scene.focusedComponent)
    }

    @Test
    fun scrollContainerConsumesWheelAndOffsetsHitTesting() {
        val scene = Scene()
        val scroll = ScrollContainer(0f, 0f, 100f, 40f)
        scroll.add(Component(width = 100f, height = 30f))
        scroll.add(Component(width = 100f, height = 30f))
        val child = Component(width = 100f, height = 30f)
        var hit = false
        child.on("pointerDown") { hit = true }
        scroll.add(child)
        scene.add(scroll)

        scroll.measure(Constraints(100f, 40f))
        scroll.layout.layout(scroll, Constraints(100f, 40f))
        scene.dispatch(PointerEvent("pointerScroll", 10f, 10f, deltaY = -3f))
        repeat(6) { scroll.advanceScroll(1f / 60f) }
        scene.dispatch(PointerEvent("pointerDown", 10f, 20f))

        assertTrue(scroll.scrollY > 0f)
        assertTrue(hit)
    }

    @Test
    fun weightedRowAllocatesRemainingWidth() {
        val parent = Component(width = 300f, height = 40f).apply {
            layout = RowLayout(gap = 10f)
            add(Component(width = 50f, height = 20f))
            add(Component(width = 0f, height = 20f).weight(1f))
        }

        parent.measure(Constraints(300f, 40f))
        parent.layout.layout(parent, Constraints(300f, 40f))

        assertEquals(240f, parent.children[1].width)
    }

    @Test
    fun rowLayoutRespectsChildMargins() {
        val parent = Component(width = 200f, height = 40f).apply {
            layout = RowLayout(gap = 10f)
            padding(5f)
            add(Component(width = 40f, height = 20f).margin(horizontal = 3f, vertical = 2f))
            add(Component(width = 20f, height = 20f))
        }

        parent.measure(Constraints(200f, 40f))
        parent.layout.layout(parent, Constraints(200f, 40f))

        assertEquals(8f, parent.children[0].x)
        assertEquals(7f, parent.children[0].y)
        assertEquals(61f, parent.children[1].x)
    }

    @Test
    fun functionalComponentCanRebuildFromFrameHook() {
        val component = FunctionalComponent(width = 200f, height = 40f) {
            val frame = useFrame()
            label("frame=${frame.frame}", width = 200f, height = 40f)
        }
        val backend = TestRenderBackend()

        component.draw(RenderContext(backend, width = 800, height = 600, time = TimeFrame(frame = 1)))
        component.draw(RenderContext(backend, width = 800, height = 600, time = TimeFrame(frame = 2)))

        assertEquals("frame=1", backend.texts[0].text)
        assertEquals("frame=2", backend.texts[1].text)
    }

    @Test
    fun functionalWidgetCanRenderFromCurrentScope() {
        val component = FunctionalComponent(width = 200f, height = 40f) {
            CurrentScopeWidget("direct")
        }
        val backend = TestRenderBackend()

        component.draw(RenderContext(backend, width = 800, height = 600))

        assertEquals("direct", backend.texts.single().text)
    }

    @Test
    fun textFieldSupportsCaretNavigationAndDelete() {
        val state = stateOf("ab")
        val field = TextField(state, width = 120f, height = 32f)
        val scene = Scene().apply { add(field) }

        scene.dispatch(PointerEvent("pointerDown", 4f, 4f))
        scene.dispatch(KeyEvent("keyDown", java.awt.event.KeyEvent.VK_LEFT))
        scene.dispatch(KeyEvent("keyDown", java.awt.event.KeyEvent.VK_BACK_SPACE))

        assertEquals("b", state.value)
    }

    @Test
    fun overlayOutsideClickClosesAndConsumesPointer() {
        val scene = Scene()
        var baseHit = false
        val base = Component(0f, 0f, 100f, 100f).on(PointerDown) { baseHit = true }
        val overlay = Component(20f, 20f, 30f, 30f)

        scene.add(base)
        scene.pushOverlay(overlay)
        scene.dispatch(PointerEvent("pointerDown", 5f, 5f))

        assertTrue(scene.layers.isEmpty)
        assertTrue(!baseHit)
    }

    @Test
    fun dropdownUsesOverlayPopupToSelectOption() {
        val scene = Scene()
        val selected = stateOf("a")
        scene.add(Dropdown(listOf("a", "b", "c"), selected, width = 100f, height = 28f))
        scene.render(RenderContext(TestRenderBackend(), width = 200, height = 160))

        scene.dispatch(PointerEvent("pointerDown", 10f, 10f))
        assertTrue(!scene.layers.isEmpty)

        scene.dispatch(PointerEvent("pointerDown", 10f, 58f))

        assertEquals("b", selected.value)
    }

    @Test
    fun keyBindCapturesNextKeyPress() {
        val scene = Scene()
        val key = stateOf<Int?>(null)
        scene.add(KeyBind(key, width = 100f, height = 28f))

        scene.dispatch(PointerEvent("pointerDown", 10f, 10f))
        scene.dispatch(KeyEvent("keyDown", java.awt.event.KeyEvent.VK_K))

        assertEquals(java.awt.event.KeyEvent.VK_K, key.value)
    }

    @Test
    fun settingsPanelBuildsControlsBoundToSettings() {
        val root = SettingGroup("Root")
        val enabledDelegate = root.bool("Enabled", false)
        val enabledSetting = enabledDelegate.setting
        val panel = SettingsPanel(root, width = 240f)
        val scene = Scene().apply { add(panel) }

        scene.render(RenderContext(TestRenderBackend(), width = 400, height = 300))
        scene.dispatch(PointerEvent("pointerDown", 150f, 40f))

        assertTrue(enabledSetting.value)
    }

    @Test
    fun cssLikeStyleAppliesSizingAndTextProperties() {
        val label = ComponentLabel("Styled").style(
            Style(
                width = 120f,
                height = 32f,
                minWidth = 160f,
                padding = Insets.xy(8f, 4f),
                foreground = Color(10, 20, 30),
                textSize = 13f
            )
        )
        val measured = label.measure(Constraints(500f, 100f))
        val backend = TestRenderBackend()

        label.draw(RenderContext(backend, width = 500, height = 100))

        assertEquals(160f, measured.width)
        assertEquals(32f, measured.height)
        assertEquals(13f, backend.texts.single().style.size)
        assertEquals(Color(10, 20, 30), backend.texts.single().style.color)
    }

    @Test
    fun zIndexControlsChildDrawOrder() {
        val root = Component(width = 160f, height = 40f).apply {
            add(ComponentLabel("low", width = 40f, height = 20f).zIndex(0))
            add(ComponentLabel("high", width = 40f, height = 20f).zIndex(10))
        }
        val backend = TestRenderBackend()

        root.draw(RenderContext(backend, width = 160, height = 40))

        assertEquals(listOf("low", "high"), backend.texts.map { it.text })
    }

    @Test
    fun rowLayoutSupportsCssSpaceBetweenMainAxisAlignment() {
        val first = Component(width = 20f, height = 10f)
        val second = Component(width = 20f, height = 10f)
        val root = Component(width = 100f, height = 20f).apply {
            layout = RowLayout(mainAxisAlignment = MainAxisAlignment.SpaceBetween)
            add(first)
            add(second)
        }

        root.draw(RenderContext(TestRenderBackend(), width = 100, height = 20))

        assertEquals(0f, first.x)
        assertEquals(80f, second.x)
    }

    @Test
    fun wrapLayoutMovesOverflowingChildrenToNextLine() {
        val children = listOf(
            Component(width = 30f, height = 10f),
            Component(width = 30f, height = 20f),
            Component(width = 30f, height = 10f)
        )
        val root = Component(width = 70f).apply {
            layout = WrapLayout(columnGap = 5f, rowGap = 6f, crossAxisAlignment = dev.unknownuser.ananda.layout.Alignment.Center)
            children.forEach(::add)
        }

        val measured = root.measure(Constraints(70f, 100f))
        root.draw(RenderContext(TestRenderBackend(), width = 70, height = 100))

        assertEquals(70f, measured.width)
        assertEquals(36f, measured.height)
        assertEquals(0f, children[0].x)
        assertEquals(5f, children[0].y)
        assertEquals(35f, children[1].x)
        assertEquals(0f, children[1].y)
        assertEquals(0f, children[2].x)
        assertEquals(26f, children[2].y)
    }

    @Test
    fun gridLayoutSupportsMaxContentAndFractionColumns() {
        val leading = Component(width = 30f, height = 10f)
        val middle = Component(height = 10f)
        val trailing = Component(width = 20f, height = 10f)
        val root = Component(width = 200f, height = 20f).apply {
            layout = GridLayout(listOf(maxContent, fr(), maxContent), columnGap = 10f)
            add(leading)
            add(middle)
            add(trailing)
        }

        root.draw(RenderContext(TestRenderBackend(), width = 200, height = 20))

        assertEquals(0f, leading.x)
        assertEquals(40f, middle.x)
        assertEquals(130f, middle.width)
        assertEquals(180f, trailing.x)
    }

    @Test
    fun gridLayoutSupportsRepeatFixedTracksAndRowGap() {
        val children = List(4) { Component(width = 50f, height = 10f) }
        val root = Component(width = 200f).apply {
            layout = GridLayout(repeat(3, px(50)), columnGap = 5f, rowGap = 6f)
            children.forEach(::add)
        }

        val measured = root.measure(Constraints(200f, 200f))
        root.draw(RenderContext(TestRenderBackend(), width = 200, height = 200))

        assertEquals(200f, measured.width)
        assertEquals(26f, measured.height)
        assertEquals(0f, children[0].x)
        assertEquals(55f, children[1].x)
        assertEquals(110f, children[2].x)
        assertEquals(0f, children[3].x)
        assertEquals(16f, children[3].y)
    }

    @Test
    fun gridLayoutPlacesChildrenByNamedAreas() {
        val a = Component(width = 20f, height = 10f).gridArea("a")
        val b = Component(width = 20f, height = 10f).gridArea("b")
        val c = Component(width = 20f, height = 10f).gridArea("c")
        val root = Component(width = 120f).apply {
            layout = GridLayout(
                columns = listOf(fr(), px(30)),
                columnGap = 5f,
                rowGap = 7f,
                areas = gridAreas(
                    "a b",
                    "a c"
                )
            )
            add(c)
            add(a)
            add(b)
        }

        root.draw(RenderContext(TestRenderBackend(), width = 120, height = 100))

        assertEquals(0f, a.x)
        assertEquals(0f, a.y)
        assertEquals(90f, b.x)
        assertEquals(0f, b.y)
        assertEquals(90f, c.x)
        assertEquals(17f, c.y)
    }

    @Test
    fun gridLayoutMeasuresMaxContentColumnsFromNamedPlacements() {
        val wide = Component(width = 70f, height = 10f).gridArea("b")
        val narrow = Component(width = 10f, height = 10f).gridArea("a")
        val root = Component(width = 120f).apply {
            layout = GridLayout(
                columns = listOf(maxContent, maxContent),
                columnGap = 5f,
                areas = gridAreas("a b")
            )
            add(wide)
            add(narrow)
        }

        root.draw(RenderContext(TestRenderBackend(), width = 120, height = 100))

        assertEquals(0f, narrow.x)
        assertEquals(15f, wide.x)
    }

    @Test
    fun gridLayoutPlacesChildrenByExplicitCell() {
        val first = Component(width = 20f, height = 10f).gridCell(row = 1, column = 1)
        val overlay = Component(width = 30f, height = 12f).gridCell(row = 1, column = 1)
        val second = Component(width = 20f, height = 10f).gridCell(row = 1, column = 2)
        val root = Component(width = 100f).apply {
            layout = GridLayout(listOf(px(40), px(40)), columnGap = 6f)
            add(first)
            add(second)
            add(overlay)
        }

        root.draw(RenderContext(TestRenderBackend(), width = 100, height = 100))

        assertEquals(0f, first.x)
        assertEquals(0f, overlay.x)
        assertEquals(46f, second.x)
    }

    @Test
    fun absolutePositioningUsesRightAndBottomAnchors() {
        val child = Component(width = 20f, height = 10f).apply {
            style = Style(positioning = Positioning.Absolute, right = 5f, bottom = 7f)
        }
        val root = Component(width = 100f, height = 60f).apply {
            add(child)
        }

        root.draw(RenderContext(TestRenderBackend(), width = 100, height = 60))

        assertEquals(75f, child.x)
        assertEquals(43f, child.y)
    }

    @Test
    fun labelUsesStyleTextAlignmentLineHeightAndFontFamily() {
        val label = ComponentLabel("A\nBB", width = 100f).style(
            Style(
                textSize = 10f,
                lineHeight = 20f,
                textAlign = TextAlign.Center,
                fontFamily = "Inter",
                fontWeight = 600,
                letterSpacing = 1f
            )
        )
        val backend = TestRenderBackend()

        label.draw(RenderContext(backend, width = 100, height = 100))

        assertEquals(listOf("A", "BB"), backend.texts.map { it.text })
        assertEquals(47.25f, backend.texts[0].x)
        assertEquals(10f, backend.texts[0].y)
        assertEquals(44f, backend.texts[1].x)
        assertEquals(30f, backend.texts[1].y)
        assertEquals("Inter", backend.texts[0].style.fontFamily)
        assertEquals(600, backend.texts[0].style.fontWeight)
        assertEquals(1f, backend.texts[0].style.letterSpacing)
    }

    @Test
    fun labelAppliesNowrapEllipsisWithinMeasuredWidth() {
        val label = ComponentLabel("abcdefghij", width = 40f).style(
            Style(
                textSize = 10f,
                whiteSpace = WhiteSpace.NoWrap,
                textOverflow = TextOverflow.Ellipsis
            )
        )
        val backend = TestRenderBackend()

        label.draw(RenderContext(backend, width = 100, height = 40))

        assertEquals("abcd...", backend.texts.single().text)
    }

    @Test
    fun labelWrapsNormalWhitespaceByWords() {
        val label = ComponentLabel("alpha beta", width = 40f).style(
            Style(textSize = 10f)
        )
        val backend = TestRenderBackend()

        val measured = label.measure(Constraints(40f, 100f))
        label.draw(RenderContext(backend, width = 40, height = 100))

        assertEquals(28f, measured.height)
        assertEquals(listOf("alpha", "beta"), backend.texts.map { it.text })
        assertEquals(10f, backend.texts[0].y)
        assertEquals(24f, backend.texts[1].y)
    }

    @Test
    fun labelUsesStyleMaxWidthForWrappingWhenWidthIsImplicit() {
        val label = ComponentLabel("alpha beta").style(
            Style(
                textSize = 10f,
                maxWidth = 40f
            )
        )
        val backend = TestRenderBackend()

        val measured = label.measure(Constraints(200f, 100f))
        label.draw(RenderContext(backend, width = 200, height = 100))

        assertTrue(measured.width <= 40f)
        assertEquals(listOf("alpha", "beta"), backend.texts.map { it.text })
    }

    @Test
    fun labelSupportsOverflowWrapAnywhere() {
        val label = ComponentLabel("abcdefgh", width = 20f).style(
            Style(
                textSize = 10f,
                overflowWrap = OverflowWrap.Anywhere
            )
        )
        val backend = TestRenderBackend()

        val measured = label.measure(Constraints(20f, 100f))
        label.draw(RenderContext(backend, width = 20, height = 100))

        assertEquals(42f, measured.height)
        assertEquals(listOf("abc", "def", "gh"), backend.texts.map { it.text })
    }

    @Test
    fun labelClipOverflowKeepsOriginalTextButNormalizesNowrapWhitespace() {
        val label = ComponentLabel("alpha\nbeta", width = 40f).style(
            Style(
                textSize = 10f,
                whiteSpace = WhiteSpace.NoWrap,
                textOverflow = TextOverflow.Clip
            )
        )
        val backend = TestRenderBackend()

        label.draw(RenderContext(backend, width = 100, height = 40))

        assertEquals(listOf("alpha beta"), backend.texts.map { it.text })
    }

    @Test
    fun labelPreWhitespaceKeepsExplicitLines() {
        val label = ComponentLabel("a  b\nc", width = 100f).style(
            Style(
                textSize = 10f,
                whiteSpace = WhiteSpace.Pre
            )
        )
        val backend = TestRenderBackend()

        label.draw(RenderContext(backend, width = 100, height = 80))

        assertEquals(listOf("a  b", "c"), backend.texts.map { it.text })
    }

    @Test
    fun componentDrawsBackgroundImageWithContainFit() {
        val texture = TextureRegion("icon", width = 2f, height = 1f)
        val component = Component(width = 100f, height = 100f).backgroundImage(texture, ImageFit.Contain, alpha = 0.75f)
        val backend = TestRenderBackend()

        component.draw(RenderContext(backend, width = 100, height = 100))

        val draw = backend.textures.single()
        assertEquals("icon", draw.texture.id)
        assertEquals(0f, draw.x)
        assertEquals(25f, draw.y)
        assertEquals(100f, draw.width)
        assertEquals(50f, draw.height)
        assertEquals(0.75f, draw.alpha)
    }

    @Test
    fun componentDrawsCssLikeBackgroundImageSizeAndPosition() {
        val texture = TextureRegion("search-icon", width = 1f, height = 1f)
        val component = Component(width = 100f, height = 40f).backgroundImage(
            texture,
            width = 18f,
            height = 18f,
            positionX = ImagePosition.pixels(20f),
            positionY = ImagePosition.Center
        )
        val backend = TestRenderBackend()

        component.draw(RenderContext(backend, width = 100, height = 40))

        val draw = backend.textures.single()
        assertEquals("search-icon", draw.texture.id)
        assertEquals(20f, draw.x)
        assertEquals(11f, draw.y)
        assertEquals(18f, draw.width)
        assertEquals(18f, draw.height)
    }

    @Test
    fun componentInfersBackgroundImageWidthFromExplicitHeightAndTextureAspect() {
        val texture = TextureRegion("wide", width = 2f, height = 1f)
        val component = Component(width = 100f, height = 40f).backgroundImage(
            texture,
            height = 12f,
            positionX = ImagePosition.End,
            positionY = ImagePosition.pixels(4f)
        )
        val backend = TestRenderBackend()

        component.draw(RenderContext(backend, width = 100, height = 40))

        val draw = backend.textures.single()
        assertEquals(76f, draw.x)
        assertEquals(4f, draw.y)
        assertEquals(24f, draw.width)
        assertEquals(12f, draw.height)
    }

    @Test
    fun componentDrawsIndependentCornerRadiiForCssShorthand() {
        val component = Component(width = 80f, height = 20f).apply {
            backgroundColor = Color.RED
            cornerRadii(4f, 0f, 0f, 4f)
        }
        val backend = TestRenderBackend()

        component.draw(RenderContext(backend, width = 80, height = 20))

        assertEquals(CornerRadii(4f, 0f, 0f, 4f), backend.roundedRects.single().radii)
    }

    @Test
    fun styleCanApplyIndependentCornerRadii() {
        val component = Component(width = 80f, height = 20f).style(
            Style(
                background = Color.BLUE,
                cornerRadii = CornerRadii(8f, 8f, 4f, 4f)
            )
        )
        val backend = TestRenderBackend()

        component.draw(RenderContext(backend, width = 80, height = 20))

        assertEquals(CornerRadii(8f, 8f, 4f, 4f), backend.roundedRects.single().radii)
    }

    @Test
    fun skiaBackendLazilyLoadsSvgTexturesFromRegisteredRoot() {
        val root = Files.createTempDirectory("ananda-texture-test")
        val svg = root.resolve("img/icon.svg")
        Files.createDirectories(svg.parent)
        Files.writeString(
            svg,
            """<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"><rect width="16" height="16" fill="#ffffff"/></svg>"""
        )

        org.jetbrains.skia.Surface.makeRasterN32Premul(32, 32).use { surface ->
            val backend = dev.unknownuser.ananda.backend.SkiaRenderBackend(surface.canvas)
            backend.registerTextureRoot(root)
            backend.drawTexture(TextureRegion("img/icon.svg"), 0f, 0f, 16f, 16f)
        }
    }

    @Test
    fun skiaBackendIgnoresTextureIdsThatAreNotLocalPaths() {
        org.jetbrains.skia.Surface.makeRasterN32Premul(16, 16).use { surface ->
            val backend = dev.unknownuser.ananda.backend.SkiaRenderBackend(surface.canvas)

            backend.drawTexture(TextureRegion("https://example.invalid/icon.png?x=1"), 0f, 0f, 8f, 8f)
        }
    }

    @Test
    fun componentDrawsLayeredBackgroundsLikeLiquidBouncePanel() {
        val component = Component(width = 100f, height = 40f).backgrounds(
            BackgroundLayer.Gradient(
                listOf(
                    GradientStop(Color(40, 44, 52, 107), 0f),
                    GradientStop(Color(0, 0, 0, 0), 0.32f)
                ),
                GradientDirection.Vertical
            ),
            BackgroundLayer.Solid(Color(24, 26, 32, 230))
        )
        val backend = TestRenderBackend()

        component.draw(RenderContext(backend, width = 100, height = 40))

        assertEquals(1, backend.roundedRects.size)
        assertEquals(1, backend.rects.size)
        assertEquals(Color(24, 26, 32, 230), backend.rects.single().fill)
    }

    @Test
    fun componentDrawsEnabledAccentBarPseudoElement() {
        val accent = Color(100, 180, 255)
        val component = Component(width = 120f, height = 32f).apply {
            selected = true
            before(
                PseudoElement(
                    left = 0f,
                    top = 8f,
                    bottom = 8f,
                    width = 3f,
                    background = accent,
                    cornerRadii = CornerRadii(0f, 3f, 3f, 0f),
                    visibility = PseudoVisibility.Selected
                )
            )
        }
        val backend = TestRenderBackend()

        component.draw(RenderContext(backend, width = 120, height = 32))

        val bar = backend.roundedRects.single()
        assertEquals(0f, bar.x)
        assertEquals(8f, bar.y)
        assertEquals(3f, bar.width)
        assertEquals(16f, bar.height)
        assertEquals(accent, bar.fill)
    }

    @Test
    fun componentDrawsPerSideBorders() {
        val component = Component(width = 80f, height = 20f).borderSides(
            BorderSides(bottom = Stroke(Color.RED, 1f), left = Stroke(Color.BLUE, 2f))
        )
        val backend = TestRenderBackend()

        component.draw(RenderContext(backend, width = 80, height = 20))

        assertEquals(2, backend.lines.size)
        assertEquals(Color.RED, backend.lines.first { it.y1 == 20f && it.y2 == 20f }.stroke.color)
        assertEquals(Color.BLUE, backend.lines.first { it.x1 == 0f && it.x2 == 0f }.stroke.color)
    }

    @Test
    fun componentDrawsInsetShadowForEnabledModuleState() {
        val component = Component(width = 120f, height = 32f).style(
            Style(
                background = Color(30, 32, 38),
                shadow = Shadow(Color(100, 180, 255, 20), blurRadius = 0f, spread = 1f, inset = true)
            )
        )
        val backend = TestRenderBackend()

        component.draw(RenderContext(backend, width = 120, height = 32))

        assertTrue(backend.shadows.any { it.shadow.inset })
    }

    @Test
    fun componentAppliesHoverStateStyle() {
        val component = Component(width = 80f, height = 20f).style(
            Style(
                background = Color.BLACK,
                stateStyles = StateStyles(
                    hover = Style(background = Color.GRAY)
                )
            )
        )
        component.setInteraction { copy(hovered = true) }
        val backend = TestRenderBackend()

        component.draw(RenderContext(backend, width = 80, height = 20))

        assertEquals(Color.GRAY, backend.rects.single().fill)
    }

    @Test
    fun weightedRowChildWithMinWidthZeroSupportsEllipsis() {
        val row = Component(width = 40f, height = 20f).apply {
            layout = RowLayout()
            add(
                ComponentLabel("abcdefghij", width = 0f, height = 20f).apply {
                    weight = 1f
                    minWidth = 0f
                    clipToBounds = true
                    style = Style(
                        textSize = 10f,
                        whiteSpace = WhiteSpace.NoWrap,
                        textOverflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
        val backend = TestRenderBackend()

        row.draw(RenderContext(backend, width = 40, height = 20))

        val expected = expectedEllipsisText("abcdefghij", 40f, textSize = 10f)
        assertEquals(expected, backend.texts.single().text)
    }

    @Test
    fun scrollContainerExposesDslEntryPoint() {
        val built = scene {
            scroll(width = 100, height = 80) {
                label("item", height = 20f)
            }
        }

        val container = built.snapshot().single() as ScrollContainer
        assertEquals(100f, container.width)
        assertEquals(80f, container.height)
        assertEquals(1, container.children.size)
    }
}

@FunctionalComponentAnnotation
private fun FunctionalScope.CounterLabel(count: State<Int>) {
    label("count=${count.value}", width = 200f, height = 40f)
}

@functional
private fun CounterUi(name: String): Ui = ui {
    var count = useState(0)

    Label("$name : $count") {
        size(200, 40)
    }
    Button("+") {
        offset(y = 42)
        size(80, 30)
        onClick {
            count++
        }
    }
}

@functional
private fun CurrentScopeWidget(text: String) {
    with(currentFunctionalScope()) {
        label(text, width = 200f, height = 40f)
    }
}
