package dev.unknownuser.ananda

import dev.unknownuser.ananda.animation.Animatable
import dev.unknownuser.ananda.animation.Easings
import dev.unknownuser.ananda.animation.EnterTransition
import dev.unknownuser.ananda.animation.ExitTransition
import dev.unknownuser.ananda.animation.SpringSpec
import dev.unknownuser.ananda.animation.TweenSpec
import dev.unknownuser.ananda.backend.RenderBackend
import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.ShapeInterpolatingRenderBackend
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.interpolationPart
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.draw.Scene
import dev.unknownuser.ananda.theme.Palette
import dev.unknownuser.ananda.theme.Theme
import dev.unknownuser.ananda.time.TimeFrame
import java.awt.Color
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnimationFrameworkTest {
    @Test
    fun `animatable supports tween retargeting`() {
        val scene = Scene()
        val value = Animatable.float(0f)
        scene.animateTo(value, 10f, TweenSpec(1f, Easings.Linear))

        scene.update(0.5f)
        assertEquals(5f, value.value, 0.001f)

        scene.animateTo(value, 9f, TweenSpec(0.4f, Easings.Linear))
        scene.update(0.2f)
        assertEquals(7f, value.value, 0.001f)
        scene.update(0.2f)
        assertEquals(9f, value.value, 0.001f)
    }

    @Test
    fun `spring clamps long frames and settles exactly on target`() {
        val scene = Scene()
        val value = Animatable.float(0f)
        scene.animateTo(value, 1f, SpringSpec(stiffness = 260f, dampingRatio = 0.72f))

        scene.update(5f)
        assertTrue(value.value.isFinite())
        assertTrue(abs(value.value) < 4f, "a resumed window must not launch the spring to infinity")
        repeat(300) { scene.update(1f / 120f) }
        assertEquals(1f, value.value, 0.0001f)
        assertEquals(0, scene.animator.activeAnimations)
    }

    @Test
    fun `scene transitions all visible theme tokens`() {
        val from = Theme(palette = Palette(primary = Color.BLACK), typography = dev.unknownuser.ananda.theme.Typography(bodySize = 12f))
        val to = Theme(palette = Palette(primary = Color.WHITE), typography = dev.unknownuser.ananda.theme.Typography(bodySize = 20f))
        val scene = Scene().apply { theme = from }

        scene.transitionTheme(to, TweenSpec(1f, Easings.Linear))
        scene.update(0.5f)
        val middle = scene.resolvedTheme()
        assertTrue(abs(middle.palette.primary.red - 127) <= 1)
        assertEquals(16f, middle.typography.bodySize, 0.001f)

        scene.update(0.5f)
        assertEquals(to, scene.resolvedTheme())
    }

    @Test
    fun `enter transition applies alpha transform and requests followup frames`() {
        val backend = RecordingBackend()
        val component = Component(width = 100f, height = 40f)
            .background(Color.WHITE)
            .animateEnter(EnterTransition(slideY = 20f, initialAlpha = 0f, durationSeconds = 1f, easing = Easings.Linear))
        val scene = Scene().apply { add(component) }

        repeat(5) {
            scene.render(RenderContext(backend, 100, 40, time = TimeFrame(deltaSeconds = 0.1f)))
        }
        assertTrue(backend.alphas.any { abs(it - 0.5f) < 0.001f })
        assertTrue(backend.translations.any { (_, y) -> abs(y - 10f) < 0.001f })
        assertTrue(scene.consumeRenderRequest())
    }

    @Test
    fun `container stagger offsets each child entrance`() {
        val backend = RecordingBackend()
        val root = Component(width = 100f, height = 80f).apply {
            add(Component(width = 40f, height = 20f).background(Color.WHITE))
            add(Component(width = 40f, height = 20f).background(Color.WHITE))
            staggerChildren(
                staggerSeconds = 0.1f,
                transition = EnterTransition(initialAlpha = 0f, slideY = 0f, durationSeconds = 0.2f, easing = Easings.Linear)
            )
        }
        val scene = Scene().apply { add(root) }

        scene.render(RenderContext(backend, 100, 80, time = TimeFrame(deltaSeconds = 0.1f)))
        assertTrue(backend.alphas.any { abs(it - 0.5f) < 0.001f })
        assertTrue(backend.alphas.any { abs(it) < 0.001f })
    }

    @Test
    fun `exit transition becomes non interactive and hides after completion`() {
        val backend = RecordingBackend()
        var finished = false
        val component = Component(width = 100f, height = 40f).background(Color.WHITE)
        val scene = Scene().apply { add(component) }
        component.animateExit(
            ExitTransition(slideY = -10f, finalAlpha = 0f, durationSeconds = 0.2f, easing = Easings.Linear)
        ) { finished = true }

        assertTrue(component.pointerPath(10f, 10f).isEmpty())
        repeat(2) {
            scene.render(RenderContext(backend, 100, 40, time = TimeFrame(deltaSeconds = 0.1f)))
        }
        assertTrue(finished)
        assertTrue(!component.visible)
        assertTrue(backend.alphas.any { abs(it - 0.5f) < 0.001f })
    }

    @Test
    fun `explicit part keys survive draw order changes and wrapper delegates alpha`() {
        val delegate = RecordingBackend()
        val backend = ShapeInterpolatingRenderBackend(delegate)
        val owner = Any()

        backend.beginFrame(delegate, 0.1f)
        RenderContext(backend).interpolationPart(owner, "a") { backend.drawRect(0f, 0f, 10f, 10f, Color.BLACK) }
        RenderContext(backend).interpolationPart(owner, "b") { backend.drawRect(100f, 0f, 10f, 10f, Color.BLACK) }
        backend.withAlpha(0.25f) { }
        backend.endFrame()

        delegate.rectXs.clear()
        backend.beginFrame(delegate, 0.1f)
        RenderContext(backend).interpolationPart(owner, "b") { backend.drawRect(200f, 0f, 10f, 10f, Color.WHITE) }
        RenderContext(backend).interpolationPart(owner, "a") { backend.drawRect(50f, 0f, 10f, 10f, Color.WHITE) }
        backend.endFrame()

        assertTrue(delegate.rectXs[0] > 175f, "part b must continue from its own previous 100px position")
        assertTrue(delegate.rectXs[1] in 35f..49.9f, "part a must continue from its own previous 0px position")
        assertEquals(0.25f, delegate.alphas.last(), 0.001f)
    }

    private class RecordingBackend : RenderBackend {
        val rectXs = mutableListOf<Float>()
        val translations = mutableListOf<Pair<Float, Float>>()
        val alphas = mutableListOf<Float>()

        override fun clear(argb: Int) = Unit
        override fun drawRect(x: Float, y: Float, width: Float, height: Float, fill: Color?, stroke: Stroke?) {
            rectXs += x
        }
        override fun drawRoundedRect(x: Float, y: Float, width: Float, height: Float, radius: Float, fill: Color?, stroke: Stroke?) = Unit
        override fun drawCircle(x: Float, y: Float, radius: Float, fill: Color?, stroke: Stroke?) = Unit
        override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, stroke: Stroke) = Unit
        override fun drawText(text: String, x: Float, y: Float, style: dev.unknownuser.ananda.backend.TextStyle) = Unit
        override fun translated(x: Float, y: Float, block: () -> Unit) {
            translations += x to y
            block()
        }
        override fun withAlpha(alpha: Float, block: () -> Unit) {
            alphas += alpha
            block()
        }
    }
}
