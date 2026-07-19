package dev.unknownuser.ananda.window

import dev.unknownuser.ananda.event.ImeEvent
import dev.unknownuser.ananda.event.KeyEvent
import dev.unknownuser.ananda.event.PointerEvent
import dev.unknownuser.ananda.event.TextInputEvent
import dev.unknownuser.ananda.interaction.InteractionProvider
import java.awt.Component as AwtComponent
import java.awt.event.InputMethodEvent
import java.awt.event.InputMethodListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent as AwtKeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.text.AttributedCharacterIterator

interface InputAdapter<in T> {
    fun install(target: T)
}

class AwtInputAdapter(
    private val interactions: InteractionProvider
) : InputAdapter<AwtComponent> {
    override fun install(target: AwtComponent) {
        target.isFocusable = true
        target.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(event: AwtKeyEvent) {
                interactions.dispatch(KeyEvent("keyDown", event.keyCode, event.keyChar.takeIf { it != AwtKeyEvent.CHAR_UNDEFINED }, event.modifiersEx))
            }

            override fun keyReleased(event: AwtKeyEvent) {
                interactions.dispatch(KeyEvent("keyUp", event.keyCode, event.keyChar.takeIf { it != AwtKeyEvent.CHAR_UNDEFINED }, event.modifiersEx))
            }

            override fun keyTyped(event: AwtKeyEvent) {
                val char = event.keyChar
                if (!char.isISOControl()) {
                    interactions.dispatch(TextInputEvent(char.toString()))
                }
            }
        })
        target.addInputMethodListener(object : InputMethodListener {
            override fun inputMethodTextChanged(event: InputMethodEvent) {
                val text = event.text ?: return
                val committed = text.readText(0, event.committedCharacterCount)
                val composed = text.readText(event.committedCharacterCount, Int.MAX_VALUE)
                if (committed.isNotEmpty()) {
                    interactions.dispatch(ImeEvent("imeCommit", committedText = committed))
                }
                interactions.dispatch(ImeEvent("imeCompose", composedText = composed, caretOffset = event.caret?.insertionIndex ?: 0))
                event.consume()
            }

            override fun caretPositionChanged(event: InputMethodEvent) = Unit
        })
        target.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                target.requestFocusInWindow()
                interactions.dispatch(PointerEvent("pointerDown", event.x.toFloat(), event.y.toFloat(), event.button))
            }

            override fun mouseReleased(event: MouseEvent) {
                interactions.dispatch(PointerEvent("pointerUp", event.x.toFloat(), event.y.toFloat(), event.button))
            }
        })
        target.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(event: MouseEvent) {
                interactions.dispatch(PointerEvent("pointerMove", event.x.toFloat(), event.y.toFloat()))
            }

            override fun mouseDragged(event: MouseEvent) {
                interactions.dispatch(PointerEvent("pointerMove", event.x.toFloat(), event.y.toFloat()))
            }
        })
        target.addMouseWheelListener(object : MouseWheelListener {
            override fun mouseWheelMoved(event: MouseWheelEvent) {
                interactions.dispatch(
                    PointerEvent(
                        "pointerScroll",
                        event.x.toFloat(),
                        event.y.toFloat(),
                        deltaY = -event.preciseWheelRotation.toFloat()
                    )
                )
            }
        })
    }

    private fun AttributedCharacterIterator.readText(start: Int, endExclusive: Int): String {
        val builder = StringBuilder()
        var index = 0
        var char = first()
        while (char != AttributedCharacterIterator.DONE) {
            if (index >= start && index < endExclusive) builder.append(char)
            index += 1
            char = next()
        }
        return builder.toString()
    }
}
