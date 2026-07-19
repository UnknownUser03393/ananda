package dev.unknownuser.ananda.event

import java.util.concurrent.CopyOnWriteArrayList
import java.awt.event.KeyEvent as AwtKeyEvent

enum class EventDispatchPolicy {
    Normal,
    IgnoreWhenDisabled
}

open class UIEvent(
    val type: String,
    val dispatchPolicy: EventDispatchPolicy = EventDispatchPolicy.Normal
) {
    var consumed: Boolean = false
        private set

    fun consume() {
        consumed = true
    }
}

class PointerEvent(
    type: String,
    val x: Float,
    val y: Float,
    val button: Int = 0,
    val deltaX: Float = 0f,
    val deltaY: Float = 0f
) : UIEvent(type, EventDispatchPolicy.IgnoreWhenDisabled)

class KeyEvent(
    type: String,
    val keyCode: Int,
    val character: Char? = null,
    val modifiers: Int = 0
) : UIEvent(type, EventDispatchPolicy.IgnoreWhenDisabled)

class TextInputEvent(
    val text: String
) : UIEvent("textInput", EventDispatchPolicy.IgnoreWhenDisabled)

class ImeEvent(
    type: String,
    val committedText: String = "",
    val composedText: String = "",
    val caretOffset: Int = 0
) : UIEvent(type, EventDispatchPolicy.IgnoreWhenDisabled)

class FocusEvent(
    type: String
) : UIEvent(type)

open class EventMatcher<E : UIEvent>(
    val description: String,
    private val matcher: (UIEvent) -> E?
) {
    internal fun match(event: UIEvent): E? = matcher(event)

    fun where(description: String = "where", predicate: (E) -> Boolean): EventMatcher<E> =
        EventMatcher("${this.description}.$description") { event ->
            match(event)?.takeIf(predicate)
        }
}

infix fun <E : UIEvent> EventMatcher<E>.or(other: EventMatcher<E>): EventMatcher<E> =
    EventMatcher("${this.description} || ${other.description}") { event ->
        this.match(event) ?: other.match(event)
    }

infix fun <E : UIEvent> EventMatcher<E>.and(predicate: (E) -> Boolean): EventMatcher<E> =
    where("and", predicate)

object EventType {
    fun any(type: String): EventMatcher<UIEvent> =
        EventMatcher(type) { event -> event.takeIf { it.type == type } }
}

object PointerDown : EventMatcher<PointerEvent>("PointerDown", { event ->
    (event as? PointerEvent)?.takeIf { it.type == "pointerDown" }
})

object PointerUp : EventMatcher<PointerEvent>("PointerUp", { event ->
    (event as? PointerEvent)?.takeIf { it.type == "pointerUp" }
})

object PointerMove : EventMatcher<PointerEvent>("PointerMove", { event ->
    (event as? PointerEvent)?.takeIf { it.type == "pointerMove" }
})

object PointerScroll : EventMatcher<PointerEvent>("PointerScroll", { event ->
    (event as? PointerEvent)?.takeIf { it.type == "pointerScroll" }
})

object KeyDown : EventMatcher<KeyEvent>("KeyDown", { event ->
    (event as? KeyEvent)?.takeIf { it.type == "keyDown" }
}) {
    val Enter = where("Enter") { it.keyCode == AwtKeyEvent.VK_ENTER }
    val Space = where("Space") { it.keyCode == AwtKeyEvent.VK_SPACE }
    val Digit = where("Digit") { it.keyCode.isDigitKeyCode() }
}

fun KeyEvent.digitValue(): Int? =
    when (keyCode) {
        in AwtKeyEvent.VK_0..AwtKeyEvent.VK_9 -> keyCode - AwtKeyEvent.VK_0
        in AwtKeyEvent.VK_NUMPAD0..AwtKeyEvent.VK_NUMPAD9 -> keyCode - AwtKeyEvent.VK_NUMPAD0
        else -> null
    }

private fun Int.isDigitKeyCode(): Boolean =
    this in AwtKeyEvent.VK_0..AwtKeyEvent.VK_9 || this in AwtKeyEvent.VK_NUMPAD0..AwtKeyEvent.VK_NUMPAD9

object KeyUp : EventMatcher<KeyEvent>("KeyUp", { event ->
    (event as? KeyEvent)?.takeIf { it.type == "keyUp" }
})

object TextInput : EventMatcher<TextInputEvent>("TextInput", { event ->
    event as? TextInputEvent
})

object ImeCompose : EventMatcher<ImeEvent>("ImeCompose", { event ->
    (event as? ImeEvent)?.takeIf { it.type == "imeCompose" }
})

object ImeCommit : EventMatcher<ImeEvent>("ImeCommit", { event ->
    (event as? ImeEvent)?.takeIf { it.type == "imeCommit" }
})

object Focus : EventMatcher<FocusEvent>("Focus", { event ->
    (event as? FocusEvent)?.takeIf { it.type == "focus" }
})

object Blur : EventMatcher<FocusEvent>("Blur", { event ->
    (event as? FocusEvent)?.takeIf { it.type == "blur" }
})

class EventPipeline {
    private class Subscription<E : UIEvent>(
        val matcher: EventMatcher<E>,
        val handler: (E) -> Unit
    ) {
        fun emit(event: UIEvent) {
            matcher.match(event)?.let(handler)
        }
    }

    private val subscriptions = CopyOnWriteArrayList<Subscription<out UIEvent>>()

    fun on(type: String, handler: (UIEvent) -> Unit) {
        on(EventType.any(type), handler)
    }

    fun <E : UIEvent> on(matcher: EventMatcher<E>, handler: (E) -> Unit) {
        subscriptions += Subscription(matcher, handler)
    }

    fun emit(event: UIEvent) {
        subscriptions.forEach { subscription ->
            if (!event.consumed) subscription.emit(event)
        }
    }

    fun clear() = subscriptions.clear()
}
