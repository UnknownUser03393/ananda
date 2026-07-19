package dev.unknownuser.ananda.material3

import dev.unknownuser.ananda.backend.RenderBackend
import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextStyle
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.draw.Scene
import dev.unknownuser.ananda.event.PointerEvent
import dev.unknownuser.ananda.event.KeyEvent as AnandaKeyEvent
import dev.unknownuser.ananda.reactive.stateOf
import dev.unknownuser.ananda.time.TimeFrame
import java.awt.Color
import java.awt.event.KeyEvent as AwtKeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MaterialTextFieldAnimationTest {
    @Test
    fun `focus floats label before fading placeholder in`() {
        val backend = RecordingBackend()
        val field = MaterialTextField(
            value = stateOf(""),
            width = 280f,
            height = 56f,
            label = "Name",
            placeholder = "Enter your name",
            variant = MaterialTextFieldStyle.Outlined
        )
        val scene = Scene().apply {
            theme = MaterialTheme.dark()
            add(field)
        }

        render(scene, backend)
        val restingLabel = backend.text("Name")
        assertNotNull(restingLabel)
        assertNull(backend.text("Enter your name"), "placeholder must not collide with the resting label")

        scene.focus(field)
        repeat(8) { render(scene, backend, 0.1f) }
        val floatingLabel = backend.text("Name")
        val placeholder = backend.text("Enter your name")

        assertNotNull(floatingLabel)
        assertNotNull(placeholder)
        assertTrue(floatingLabel.y < restingLabel.y, "label should move upward on focus")
        assertTrue(floatingLabel.style.size < restingLabel.style.size, "floating label should use the smaller type scale")
        assertTrue(placeholder.style.color.alpha > 220, "placeholder should finish its fade-in")
        val caret = assertNotNull(backend.lines.lastOrNull { it.x1 == it.x2 })
        val expectedTextCenter = placeholder.y - placeholder.style.size * 0.36f
        assertEquals(expectedTextCenter, (caret.y1 + caret.y2) / 2f, 0.01f, "caret must be vertically centered on the text line")
        assertTrue(backend.rects.any { it.y < 0f }, "outlined field should cut a notch behind the floating label")
    }

    @Test
    fun `clicking non-focusable content or empty space clears focus`() {
        val backend = RecordingBackend()
        val background = Component(width = 320f, height = 100f)
        val field = MaterialTextField(width = 180f, height = 56f, label = "Name").at(16f, 16f)
        val scene = Scene().apply {
            add(background)
            add(field)
        }
        render(scene, backend)

        scene.focus(field)
        scene.dispatch(PointerEvent("pointerDown", 280f, 80f, 0))
        assertNull(scene.focusedComponent, "a non-focusable hit should blur the field")

        scene.focus(field)
        scene.dispatch(PointerEvent("pointerDown", 400f, 200f, 0))
        assertNull(scene.focusedComponent, "an empty-space hit should blur the field")
    }

    @Test
    fun `backspace and delete edit the focused value`() {
        val value = stateOf("abc")
        val field = MaterialTextField(value = value)
        val scene = Scene().apply { add(field) }
        scene.focus(field)

        scene.dispatch(AnandaKeyEvent("keyDown", AwtKeyEvent.VK_BACK_SPACE))
        assertEquals("ab", value.value)

        scene.dispatch(AnandaKeyEvent("keyDown", AwtKeyEvent.VK_HOME))
        scene.dispatch(AnandaKeyEvent("keyDown", AwtKeyEvent.VK_DELETE))
        assertEquals("b", value.value)
    }

    private fun render(scene: Scene, backend: RecordingBackend, deltaSeconds: Float = 0f) {
        backend.texts.clear()
        scene.render(
            RenderContext(
                backend = backend,
                width = 320,
                height = 100,
                time = TimeFrame(deltaSeconds = deltaSeconds, unscaledDeltaSeconds = deltaSeconds)
            )
        )
    }

    private data class DrawnText(val text: String, val x: Float, val y: Float, val style: TextStyle)
    private data class DrawnRect(val y: Float)
    private data class DrawnLine(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

    private class RecordingBackend : RenderBackend {
        val texts = mutableListOf<DrawnText>()
        val rects = mutableListOf<DrawnRect>()
        val lines = mutableListOf<DrawnLine>()

        fun text(value: String): DrawnText? = texts.lastOrNull { it.text == value }

        override fun clear(argb: Int) = Unit
        override fun drawRect(x: Float, y: Float, width: Float, height: Float, fill: Color?, stroke: Stroke?) {
            rects += DrawnRect(y)
        }
        override fun drawRoundedRect(x: Float, y: Float, width: Float, height: Float, radius: Float, fill: Color?, stroke: Stroke?) = Unit
        override fun drawCircle(x: Float, y: Float, radius: Float, fill: Color?, stroke: Stroke?) = Unit
        override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, stroke: Stroke) {
            lines += DrawnLine(x1, y1, x2, y2)
        }
        override fun drawText(text: String, x: Float, y: Float, style: TextStyle) {
            texts += DrawnText(text, x, y, style)
        }
    }
}
