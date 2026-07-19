package dev.unknownuser.ananda.minecraft

import dev.unknownuser.ananda.backend.RenderBackend
import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.ShapeInterpolatingRenderBackend
import dev.unknownuser.ananda.draw.Scene
import dev.unknownuser.ananda.event.KeyEvent
import dev.unknownuser.ananda.event.PointerEvent
import dev.unknownuser.ananda.event.TextInputEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent as AwtKeyEvent

interface MinecraftGuiSurface {
    val width: Int
    val height: Int
    val scaleFactor: Float
    val backend: RenderBackend
    val nanoTime: Long
}

class MinecraftGuiAdapter(
    val scene: Scene = Scene()
) {
    private var lastNanoTime: Long = 0L
    private var interpolationBackend: ShapeInterpolatingRenderBackend? = null

    fun render(surface: MinecraftGuiSurface) {
        val deltaSeconds = if (lastNanoTime == 0L) 0f else (surface.nanoTime - lastNanoTime) / 1_000_000_000f
        lastNanoTime = surface.nanoTime
        val backend = interpolationBackend ?: ShapeInterpolatingRenderBackend(surface.backend).also {
            interpolationBackend = it
        }
        backend.beginFrame(surface.backend, deltaSeconds)
        scene.update(deltaSeconds)
        scene.render(
            RenderContext(
                backend = backend,
                width = surface.width,
                height = surface.height,
                nanoTime = surface.nanoTime,
                time = scene.time.snapshot(),
                scaleFactor = surface.scaleFactor
            )
        )
        if (backend.endFrame()) {
            scene.requestRender()
        }
    }

    fun mouseMoved(mouseX: Double, mouseY: Double) {
        scene.dispatch(PointerEvent("pointerMove", mouseX.toFloat(), mouseY.toFloat()))
    }

    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        scene.dispatch(PointerEvent("pointerDown", mouseX.toFloat(), mouseY.toFloat(), button))
        return true
    }

    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        scene.dispatch(PointerEvent("pointerUp", mouseX.toFloat(), mouseY.toFloat(), button))
        return true
    }

    fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        scene.dispatch(
            PointerEvent(
                "pointerMove",
                mouseX.toFloat(),
                mouseY.toFloat(),
                button,
                deltaX.toFloat(),
                deltaY.toFloat()
            )
        )
        return true
    }

    fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        scene.dispatch(
            PointerEvent(
                "pointerScroll",
                mouseX.toFloat(),
                mouseY.toFloat(),
                deltaX = horizontalAmount.toFloat(),
                deltaY = verticalAmount.toFloat()
            )
        )
        return true
    }

    fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == AwtKeyEvent.VK_TAB && scene.focusNext(reverse = modifiers and InputEvent.SHIFT_DOWN_MASK != 0)) {
            return true
        }
        scene.dispatch(KeyEvent("keyDown", keyCode, modifiers = modifiers))
        return true
    }

    fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        scene.dispatch(KeyEvent("keyUp", keyCode, modifiers = modifiers))
        return true
    }

    fun charTyped(character: Char, modifiers: Int): Boolean {
        if (!character.isISOControl()) {
            scene.dispatch(TextInputEvent(character.toString()))
            return true
        }
        return false
    }

    fun removed() {
        scene.clear()
        lastNanoTime = 0L
        interpolationBackend = null
    }
}
