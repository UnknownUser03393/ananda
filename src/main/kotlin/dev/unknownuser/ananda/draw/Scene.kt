package dev.unknownuser.ananda.draw

import dev.unknownuser.ananda.animation.Animation
import dev.unknownuser.ananda.animation.AnimationDirection
import dev.unknownuser.ananda.animation.Animator
import dev.unknownuser.ananda.animation.Easing
import dev.unknownuser.ananda.animation.Easings
import dev.unknownuser.ananda.animation.RepeatMode
import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.SkiaRenderBackend
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.event.UIEvent
import dev.unknownuser.ananda.event.EventPipeline
import dev.unknownuser.ananda.event.FocusEvent
import dev.unknownuser.ananda.event.ImeEvent
import dev.unknownuser.ananda.event.KeyEvent
import dev.unknownuser.ananda.event.PointerEvent
import dev.unknownuser.ananda.event.TextInputEvent
import dev.unknownuser.ananda.interaction.ComponentHost
import dev.unknownuser.ananda.interaction.InteractionProvider
import dev.unknownuser.ananda.theme.Theme
import dev.unknownuser.ananda.time.TimeFrame
import dev.unknownuser.ananda.time.TimeSubscription
import dev.unknownuser.ananda.time.TimeSystem
import org.jetbrains.skia.Canvas
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

sealed class OutsideClickBehavior {
    object Ignore : OutsideClickBehavior()
    object Close : OutsideClickBehavior()
    object Deny : OutsideClickBehavior()
    class Custom(val action: () -> Unit) : OutsideClickBehavior()
}

data class SceneLayer(
    val component: Component,
    val priority: Int = 0,
    val outsideClickBehavior: OutsideClickBehavior = OutsideClickBehavior.Deny,
    val transparent: Boolean = true
)

class SceneLayerStack {
    private val visibleLayers = CopyOnWriteArrayList<SceneLayer>()
    private val backgroundLayers = CopyOnWriteArrayList<SceneLayer>()

    fun push(layer: SceneLayer) {
        backgroundLayers.removeIf { it.component === layer.component }
        visibleLayers.removeIf { it.component === layer.component }
        visibleLayers += layer
        visibleLayers.sortBy { it.priority }
    }

    fun pop(component: Component) {
        visibleLayers.removeIf { it.component === component }
        backgroundLayers.removeIf { it.component === component }
    }

    fun popTop(): SceneLayer? =
        top?.also { pop(it.component) }

    fun toBackground(component: Component) {
        val layer = visibleLayers.firstOrNull { it.component === component } ?: return
        visibleLayers.remove(layer)
        backgroundLayers.removeIf { it.component === component }
        backgroundLayers += layer
    }

    fun restore(component: Component) {
        val layer = backgroundLayers.firstOrNull { it.component === component } ?: return
        backgroundLayers.remove(layer)
        push(layer)
    }

    fun clear() {
        visibleLayers.clear()
        backgroundLayers.clear()
    }

    val top: SceneLayer?
        get() = visibleLayers.lastOrNull()

    val visible: List<SceneLayer>
        get() = visibleLayers.toList()

    val background: List<SceneLayer>
        get() = backgroundLayers.toList()

    val isEmpty: Boolean
        get() = visibleLayers.isEmpty()
}

class Scene : InteractionProvider, ComponentHost {
    private val items = CopyOnWriteArrayList<Drawable>()
    val layers = SceneLayerStack()
    val events = EventPipeline()
    val animator = Animator()
    val time = TimeSystem()
    var theme: Theme = Theme.Default
    var uiScale: Float = 1f
    override var focusedComponent: Component? = null
        private set
    override var hoveredComponent: Component? = null
        private set
    private var pressedComponent: Component? = null
    private val renderRequested = AtomicBoolean(true)

    fun add(drawable: Drawable): Drawable {
        items += drawable
        (drawable as? Component)?.attachTo(this, true)
        requestRender()
        return drawable
    }

    fun pushLayer(layer: SceneLayer): SceneLayer {
        layers.push(layer)
        layer.component.attachTo(this, true)
        requestRender()
        return layer
    }

    fun pushOverlay(
        component: Component,
        priority: Int = 100,
        outsideClickBehavior: OutsideClickBehavior = OutsideClickBehavior.Close,
        transparent: Boolean = true
    ): SceneLayer =
        pushLayer(SceneLayer(component, priority, outsideClickBehavior, transparent))

    fun popLayer(component: Component) {
        layers.pop(component)
        component.unmountRecursively()
        component.attachTo(null, false)
        if (focusedComponent === component || component.focusableDescendants().contains(focusedComponent)) {
            focus(null)
        }
        requestRender()
    }

    fun remove(drawable: Drawable): Boolean {
        val removed = items.remove(drawable)
        if (removed) {
            (drawable as? Component)?.let {
                it.unmountRecursively()
                it.attachTo(null, false)
            }
            requestRender()
        }
        return removed
    }

    fun clear() {
        items.forEach {
            (it as? Component)?.let { component ->
                component.unmountRecursively()
                component.attachTo(null, false)
            }
        }
        layers.visible.forEach {
            it.component.unmountRecursively()
            it.component.attachTo(null, false)
        }
        layers.background.forEach {
            it.component.unmountRecursively()
            it.component.attachTo(null, false)
        }
        layers.clear()
        items.clear()
        focusedComponent = null
        requestRender()
    }

    fun snapshot(): List<Drawable> = items.toList()

    fun update(deltaSeconds: Float) {
        val timeResult = time.update(deltaSeconds)
        animator.update(time.deltaSeconds)
        if (animator.hasRunningAnimations() || timeResult.hasActiveDrivers) {
            requestRender()
        }
        if (timeResult.callbacksFired) requestRender()
    }

    override fun requestRender() {
        renderRequested.set(true)
    }

    fun consumeRenderRequest(): Boolean =
        renderRequested.getAndSet(false)

    fun animate(
        durationSeconds: Float,
        delaySeconds: Float = 0f,
        easing: Easing = Easings.Linear,
        repeat: Boolean = false,
        repeatCount: Int = if (repeat) Animation.RepeatForever else 0,
        repeatMode: RepeatMode = RepeatMode.Restart,
        direction: AnimationDirection = AnimationDirection.Forward,
        onFinished: () -> Unit = {},
        update: (Float) -> Unit
    ): Animation {
        requestRender()
        return animator.animate(durationSeconds, delaySeconds, easing, repeat, repeatCount, repeatMode, direction, onFinished, update)
    }

    fun animateFloat(
        from: Float,
        to: Float,
        durationSeconds: Float,
        delaySeconds: Float = 0f,
        easing: Easing = Easings.Linear,
        repeat: Boolean = false,
        repeatCount: Int = if (repeat) Animation.RepeatForever else 0,
        repeatMode: RepeatMode = RepeatMode.Restart,
        direction: AnimationDirection = AnimationDirection.Forward,
        onFinished: () -> Unit = {},
        update: (Float) -> Unit
    ): Animation {
        requestRender()
        return animator.animateFloat(from, to, durationSeconds, delaySeconds, easing, repeat, repeatCount, repeatMode, direction, onFinished, update)
    }

    fun onUpdate(handler: (TimeFrame) -> Unit): TimeSubscription {
        requestRender()
        return time.onUpdate(handler)
    }

    fun after(
        delaySeconds: Float,
        useScaledTime: Boolean = true,
        handler: (TimeFrame) -> Unit
    ): TimeSubscription {
        requestRender()
        return time.after(delaySeconds, useScaledTime, handler)
    }

    fun every(
        intervalSeconds: Float,
        immediate: Boolean = false,
        useScaledTime: Boolean = true,
        handler: (TimeFrame) -> Unit
    ): TimeSubscription {
        requestRender()
        return time.every(intervalSeconds, immediate, useScaledTime, handler)
    }

    override fun dispatch(event: PointerEvent) {
        val logicalEvent = event.toLogical()
        val topLayer = layers.top
        if (topLayer != null && dispatchToLayer(topLayer, logicalEvent)) {
            return
        }

        dispatchToDrawables(snapshot(), logicalEvent)
    }

    private fun dispatchToLayer(layer: SceneLayer, event: PointerEvent): Boolean {
        val path = layer.component.pointerPath(event.x, event.y)
        if (path.isNotEmpty()) {
            dispatchPath(path, event)
            return true
        }

        if (event.type == "pointerDown") {
            focus(null)
            when (val behavior = layer.outsideClickBehavior) {
                OutsideClickBehavior.Ignore -> Unit
                OutsideClickBehavior.Close -> popLayer(layer.component)
                OutsideClickBehavior.Deny -> Unit
                is OutsideClickBehavior.Custom -> behavior.action()
            }
            updateHover(null)
            events.emit(event)
            return behaviorConsumesOutsideClick(layer.outsideClickBehavior)
        }

        return !layer.transparent
    }

    private fun behaviorConsumesOutsideClick(behavior: OutsideClickBehavior): Boolean =
        behavior !is OutsideClickBehavior.Ignore

    private fun dispatchToDrawables(drawables: List<Drawable>, event: PointerEvent) {
        drawables.asReversed().forEach { item ->
            val component = item as? Component ?: return@forEach
            val path = component.pointerPath(event.x, event.y)
            if (path.isEmpty()) return@forEach
            dispatchPath(path, event)
            return
        }
        if (event.type == "pointerDown") focus(null)
        updateHover(null)
        events.emit(event)
        if (event.type == "pointerUp") {
            pressedComponent?.let { updatePressed(it, false) }
            pressedComponent = null
        }
    }

    private fun dispatchPath(path: List<Component>, event: PointerEvent) {
        val target = path.lastOrNull()
        target?.let {
            if (event.type != "pointerScroll") updateHover(it)
            when (event.type) {
                "pointerDown" -> {
                    pressedComponent?.takeIf { pressed -> pressed !== it }?.let { pressed ->
                        updatePressed(pressed, false)
                    }
                    pressedComponent = it
                    updatePressed(it, true)
                    focus(path.asReversed().firstOrNull { candidate -> candidate.focusable && !candidate.disabled })
                }
            }
        }
        path.asReversed().forEach { target ->
            if (!event.consumed) target.dispatch(event)
        }
        if (!event.consumed) events.emit(event)
        if (event.type == "pointerUp") {
            pressedComponent?.let { updatePressed(it, false) }
            pressedComponent = null
        }
    }

    override fun dispatch(event: KeyEvent) {
        dispatchFocused(event)
    }

    override fun dispatch(event: TextInputEvent) {
        dispatchFocused(event)
    }

    override fun dispatch(event: ImeEvent) {
        dispatchFocused(event)
    }

    override fun dispatchFocused(event: UIEvent) {
        focusedComponent?.dispatch(event)
        if (!event.consumed) events.emit(event)
    }

    override fun focus(component: Component?) {
        if (focusedComponent === component) return
        focusedComponent?.let {
            updateFocused(it, false)
            it.events.emit(FocusEvent("blur"))
        }
        focusedComponent = component?.takeIf { !it.disabled }
        focusedComponent?.let {
            updateFocused(it, true)
            it.events.emit(FocusEvent("focus"))
        }
        requestRender()
    }

    override fun focusNext(reverse: Boolean): Boolean {
        val focusables = snapshot()
            .asSequence()
            .filterIsInstance<Component>()
            .flatMap { it.focusableDescendants().asSequence() }
            .plus(layers.visible.asSequence().flatMap { it.component.focusableDescendants().asSequence() })
            .filter { it.focusable && !it.disabled && it.visible }
            .toList()
        if (focusables.isEmpty()) return false
        val currentIndex = focusables.indexOf(focusedComponent)
        val nextIndex = when {
            currentIndex < 0 -> if (reverse) focusables.lastIndex else 0
            reverse -> if (currentIndex == 0) focusables.lastIndex else currentIndex - 1
            else -> (currentIndex + 1) % focusables.size
        }
        focus(focusables[nextIndex])
        return true
    }

    override fun updateHover(component: Component?) {
        if (hoveredComponent === component) return
        hoveredComponent?.setInteraction { copy(hovered = false) }
        hoveredComponent = component
        hoveredComponent?.setInteraction { copy(hovered = true) }
    }

    override fun updatePressed(component: Component, pressed: Boolean) {
        component.setInteraction { copy(pressed = pressed) }
    }

    override fun updateFocused(component: Component, focused: Boolean) {
        component.setInteraction { copy(focused = focused) }
    }

    fun render(context: RenderContext) {
        val scale = uiScale.coerceAtLeast(0.01f)
        val logicalContext = context.copy(
            width = (context.width / scale).toInt(),
            height = (context.height / scale).toInt(),
            uiScale = scale
        ).withTheme(theme)
        logicalContext.backend.scaled(scale) {
            items.forEach { it.draw(logicalContext) }
            layers.visible.forEach { layer -> layer.component.draw(logicalContext) }
        }
    }

    fun render(canvas: Canvas) {
        render(RenderContext(SkiaRenderBackend(canvas), time = time.snapshot()))
    }

    override fun readClipboard(): String? =
        runCatching {
            Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
        }.getOrNull()

    override fun writeClipboard(text: String) {
        runCatching {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        }
    }

    private fun PointerEvent.toLogical(): PointerEvent {
        val scale = uiScale.coerceAtLeast(0.01f)
        if (scale == 1f) return this
        return PointerEvent(
            type = type,
            x = x / scale,
            y = y / scale,
            button = button,
            deltaX = deltaX,
            deltaY = deltaY
        )
    }
}
