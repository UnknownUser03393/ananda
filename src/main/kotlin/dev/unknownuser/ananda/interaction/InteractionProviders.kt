package dev.unknownuser.ananda.interaction

import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.event.UIEvent
import dev.unknownuser.ananda.event.ImeEvent
import dev.unknownuser.ananda.event.KeyEvent
import dev.unknownuser.ananda.event.PointerEvent
import dev.unknownuser.ananda.event.TextInputEvent

interface PointerInteractionProvider    { fun dispatch(event: PointerEvent)     }
interface KeyboardInteractionProvider   { fun dispatch(event: KeyEvent)         }
interface TextInputInteractionProvider  { fun dispatch(event: TextInputEvent)   }
interface ImeInteractionProvider        { fun dispatch(event: ImeEvent)         }
interface FocusedInteractionProvider    { fun dispatchFocused(event: UIEvent)   }
interface RenderInvalidationProvider    { fun requestRender()                   }

interface ClipboardProvider {
    fun readClipboard(): String? = null

    fun writeClipboard(text: String) = Unit
}

interface FocusManagementProvider {
    val focusedComponent: Component?

    fun focus(component: Component?)

    fun focusNext(reverse: Boolean = false): Boolean = false
}

interface ComponentHost :
    FocusManagementProvider,
    RenderInvalidationProvider,
    ClipboardProvider

interface ComponentInteractionProvider {
    val hoveredComponent: Component?

    fun updateHover(component: Component?)

    fun updatePressed(component: Component, pressed: Boolean)

    fun updateFocused(component: Component, focused: Boolean)
}

interface InteractionProvider :
    PointerInteractionProvider,
    KeyboardInteractionProvider,
    TextInputInteractionProvider,
    ImeInteractionProvider,
    FocusedInteractionProvider,
    FocusManagementProvider,
    ComponentInteractionProvider
