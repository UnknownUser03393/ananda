package dev.unknownuser.ananda.component

import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.CornerRadii
import dev.unknownuser.ananda.backend.ImageFit
import dev.unknownuser.ananda.backend.ImagePosition
import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.ShapeInterpolationScope
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextureRegion
import dev.unknownuser.ananda.draw.Drawable
import dev.unknownuser.ananda.draw.Scene
import dev.unknownuser.ananda.event.UIEvent
import dev.unknownuser.ananda.event.EventDispatchPolicy
import dev.unknownuser.ananda.event.EventMatcher
import dev.unknownuser.ananda.event.EventPipeline
import dev.unknownuser.ananda.event.KeyEvent
import dev.unknownuser.ananda.event.PointerEvent
import dev.unknownuser.ananda.interaction.ComponentHost
import dev.unknownuser.ananda.layout.Constraints
import dev.unknownuser.ananda.layout.Alignment
import dev.unknownuser.ananda.layout.Insets
import dev.unknownuser.ananda.layout.Layout
import dev.unknownuser.ananda.layout.Positioning
import dev.unknownuser.ananda.layout.Size
import dev.unknownuser.ananda.layout.StackLayout
import dev.unknownuser.ananda.style.BackgroundLayer
import dev.unknownuser.ananda.style.BorderSides
import dev.unknownuser.ananda.style.GradientStop
import dev.unknownuser.ananda.style.PseudoElement
import dev.unknownuser.ananda.style.PseudoVisibility
import dev.unknownuser.ananda.style.StateStyles
import dev.unknownuser.ananda.style.Style
import dev.unknownuser.ananda.style.TransformOffset
import dev.unknownuser.ananda.theme.Theme
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

open class Component(
    var x: Float = 0f,
    var y: Float = 0f,
    width: Float = 0f,
    height: Float = 0f
) : Drawable {
    private var updatingLayoutSize = false
    private var explicitWidth = width > 0f
    private var explicitHeight = height > 0f

    var width: Float = width
        set(value) {
            field = value
            if (!updatingLayoutSize) explicitWidth = value > 0f
        }
    var height: Float = height
        set(value) {
            field = value
            if (!updatingLayoutSize) explicitHeight = value > 0f
        }

    internal val hasExplicitWidth: Boolean
        get() = explicitWidth

    internal val hasExplicitHeight: Boolean
        get() = explicitHeight

    var visible: Boolean = true
    var backgroundColor: Color? = null
    var borderColor: Color? = null
    var borderWidth: Float = 0f
    var cornerRadius: Float = 0f
    var cornerRadii: CornerRadii? = null
    var gradientColors: List<Color>? = null
    var backgroundGradientDirection: GradientDirection = GradientDirection.Vertical
    var backgroundLayers: List<BackgroundLayer>? = null
    var backdropBlurRadius: Float = 0f
    var borderSides: BorderSides? = null
    var translate: TransformOffset = TransformOffset()
    var beforeElement: PseudoElement? = null
    var afterElement: PseudoElement? = null
    var stateStyles: StateStyles? = null
    var backgroundImage: TextureRegion? = null
    var backgroundImageFit: ImageFit = ImageFit.Fill
    var backgroundImageAlpha: Float = 1f
    var backgroundImageWidth: Float? = null
    var backgroundImageHeight: Float? = null
    var backgroundImagePositionX: ImagePosition = ImagePosition.Center
    var backgroundImagePositionY: ImagePosition = ImagePosition.Center
    var elevationShadow: Shadow? = null
    var opacity: Float = 1f
    var scaleX: Float = 1f
    var scaleY: Float = 1f
    var rotationDegrees: Float = 0f
    var zIndex: Int = 0
    var minWidth: Float = 0f
    var minHeight: Float = 0f
    var maxWidth: Float = Float.MAX_VALUE
    var maxHeight: Float = Float.MAX_VALUE
    var positioning: Positioning = Positioning.Flow
    var left: Float? = null
    var top: Float? = null
    var right: Float? = null
    var bottom: Float? = null
    var leftPercent: Float? = null
    var topPercent: Float? = null
    var rightPercent: Float? = null
    var bottomPercent: Float? = null
    var gridArea: String? = null
    var gridRow: Int? = null
    var gridColumn: Int? = null
    var padding: Insets = Insets()
    var margin: Insets = Insets()
    var layout: Layout = StackLayout
    var clipToBounds: Boolean = false
    var weight: Float = 0f
    var fillWidth: Boolean = false
    var fillHeight: Boolean = false
    var layoutAlignment: Alignment? = null
    var theme: Theme? = null
    var style: Style? = null
    var focusable: Boolean = false
    var disabled: Boolean = false
    var selected: Boolean = false
    var positionedByUser: Boolean = false
        internal set
    var interpolationKey: Any? = null
    var measuredWidth: Float = width
        private set
    var measuredHeight: Float = height
        private set
    var interaction: ComponentInteraction = ComponentInteraction()
        private set

    val events = EventPipeline()
    val children = CopyOnWriteArrayList<Component>()

    private val mountHooks = CopyOnWriteArrayList<() -> Unit>()
    private val unmountHooks = CopyOnWriteArrayList<() -> Unit>()
    private val disposeHooks = CopyOnWriteArrayList<() -> Unit>()
    private var renderHook: ((RenderContext) -> Unit)? = null
    private var parent: Component? = null
    private var ownerHost: ComponentHost? = null
    private var mounted = false
    private var disposed = false
    private var skipMeasureOnce = false
    private var measureWidthOverride: Float? = null
    private var measureHeightOverride: Float? = null

    private data class BoundsState(
        var x: Float,
        var y: Float,
        var width: Float,
        var height: Float
    )

    override fun draw(context: RenderContext) {
        if (!visible || disposed) return
        applyStyle()
        val childContext = theme?.let { context.withTheme(it) } ?: context
        if (skipMeasureOnce) {
            skipMeasureOnce = false
            measuredWidth = measureWidthOverride ?: measuredWidth
            measuredHeight = measureHeightOverride ?: measuredHeight
        } else {
            measure(Constraints(resolveConstraint(width, context.width.toFloat()), resolveConstraint(height, context.height.toFloat())))
        }
        layout.layout(this, Constraints(measuredWidth, measuredHeight))
        val bounds = animatedBounds(childContext)
        val interpolationScope = childContext.backend as? ShapeInterpolationScope
        interpolationScope?.pushInterpolationKey(interpolationKey ?: this)
        try {
            childContext.backend.translated(bounds.x, bounds.y) {
                childContext.backend.rotated(rotationDegrees, bounds.width / 2f, bounds.height / 2f) {
                    childContext.backend.scaled(scaleX, scaleY) {
                        childContext.backend.translated(
                            translate.resolveX(bounds.width),
                            translate.resolveY(bounds.height)
                        ) {
                            childContext.backend.withAlpha(opacity) {
                                drawSelfAndChildren(childContext, bounds.width, bounds.height)
                            }
                        }
                    }
                }
            }
        } finally {
            interpolationScope?.popInterpolationKey()
        }
    }

    private fun drawSelfAndChildren(context: RenderContext, boundsWidth: Float, boundsHeight: Float) {
        val fill = backgroundColor
        val stroke = borderColor?.takeIf { borderWidth > 0f }?.let { Stroke(it, borderWidth) }
        val gradient = gradientColors?.takeIf { it.isNotEmpty() }
        val radii = cornerRadii
        val shadow = elevationShadow
        val hasBackground = fill != null || gradient != null || !backgroundLayers.isNullOrEmpty() || backdropBlurRadius > 0f
        if (shadow != null && !shadow.inset && hasBackground) {
            context.backend.drawShadowedRoundedRect(0f, 0f, boundsWidth, boundsHeight, cornerRadius, shadow)
        }
        drawBackgroundLayers(context, boundsWidth, boundsHeight, fill, gradient, radii, stroke)
        backgroundImage?.let { image ->
            drawBackgroundImage(context, image, boundsWidth, boundsHeight)
        }
        beforeElement?.takeIf { pseudoVisible(it) }?.let { drawPseudoElement(context, it, boundsWidth, boundsHeight) }
        renderHook?.invoke(context)
        val orderedChildren = children.sortedBy { it.zIndex }
        if (clipToBounds) {
            context.backend.clipped(0f, 0f, boundsWidth, boundsHeight) {
                orderedChildren.forEach { it.draw(context) }
            }
        } else {
            orderedChildren.forEach { it.draw(context) }
        }
        afterElement?.takeIf { pseudoVisible(it) }?.let { drawPseudoElement(context, it, boundsWidth, boundsHeight) }
    }

    private fun drawBackgroundLayers(
        context: RenderContext,
        boundsWidth: Float,
        boundsHeight: Float,
        fill: Color?,
        gradient: List<Color>?,
        radii: CornerRadii?,
        stroke: Stroke?
    ) {
        if (backdropBlurRadius > 0f) {
            context.backend.drawFrostedRoundedRect(
                0f,
                0f,
                boundsWidth,
                boundsHeight,
                cornerRadius,
                backdropBlurRadius,
                fill ?: Color(0, 0, 0, 0),
                stroke
            )
            backgroundLayers?.forEach { layer ->
                drawBackgroundLayer(context, layer, boundsWidth, boundsHeight, radii, null)
            }
            borderSides?.takeUnless { it.isEmpty() }?.let {
                context.backend.drawBorderSides(0f, 0f, boundsWidth, boundsHeight, it)
            }
            elevationShadow?.takeIf { it.inset }?.let {
                context.backend.drawShadowedRoundedRect(0f, 0f, boundsWidth, boundsHeight, cornerRadius, it, fill, stroke)
            }
            return
        }

        val layers = backgroundLayers
        if (!layers.isNullOrEmpty()) {
            layers.forEach { layer ->
                drawBackgroundLayer(context, layer, boundsWidth, boundsHeight, radii, null)
            }
        } else when {
            gradient != null -> context.backend.drawRoundedGradientRect(
                0f,
                0f,
                boundsWidth,
                boundsHeight,
                cornerRadius,
                gradient,
                backgroundGradientDirection,
                null
            )
            radii != null && fill != null ->
                context.backend.drawRoundedRect(0f, 0f, boundsWidth, boundsHeight, radii, fill, null)
            cornerRadius > 0f && fill != null ->
                context.backend.drawRoundedRect(0f, 0f, boundsWidth, boundsHeight, cornerRadius, fill, null)
            fill != null ->
                context.backend.drawRect(0f, 0f, boundsWidth, boundsHeight, fill, null)
        }

        if (stroke != null) {
            when {
                radii != null -> context.backend.drawRoundedRect(0f, 0f, boundsWidth, boundsHeight, radii, null, stroke)
                cornerRadius > 0f -> context.backend.drawRoundedRect(0f, 0f, boundsWidth, boundsHeight, cornerRadius, null, stroke)
                else -> context.backend.drawRect(0f, 0f, boundsWidth, boundsHeight, null, stroke)
            }
        }
        borderSides?.takeUnless { it.isEmpty() }?.let {
            context.backend.drawBorderSides(0f, 0f, boundsWidth, boundsHeight, it)
        }
        elevationShadow?.takeIf { it.inset }?.let {
            context.backend.drawShadowedRoundedRect(0f, 0f, boundsWidth, boundsHeight, cornerRadius, it, null, null)
        }
    }

    private fun drawBackgroundLayer(
        context: RenderContext,
        layer: BackgroundLayer,
        boundsWidth: Float,
        boundsHeight: Float,
        radii: CornerRadii?,
        stroke: Stroke?
    ) {
        when (layer) {
            is BackgroundLayer.Solid -> drawFilledBackground(context, boundsWidth, boundsHeight, radii, layer.color, stroke)
            is BackgroundLayer.Gradient -> {
                val stops = layer.stops.map { it.color to it.position }
                context.backend.drawRoundedGradientStops(
                    0f,
                    0f,
                    boundsWidth,
                    boundsHeight,
                    cornerRadius,
                    stops,
                    layer.direction,
                    stroke
                )
            }
        }
    }

    private fun drawFilledBackground(
        context: RenderContext,
        boundsWidth: Float,
        boundsHeight: Float,
        radii: CornerRadii?,
        fill: Color,
        stroke: Stroke?
    ) {
        when {
            radii != null -> context.backend.drawRoundedRect(0f, 0f, boundsWidth, boundsHeight, radii, fill, stroke)
            cornerRadius > 0f -> context.backend.drawRoundedRect(0f, 0f, boundsWidth, boundsHeight, cornerRadius, fill, stroke)
            else -> context.backend.drawRect(0f, 0f, boundsWidth, boundsHeight, fill, stroke)
        }
    }

    private fun drawPseudoElement(
        context: RenderContext,
        pseudo: PseudoElement,
        boundsWidth: Float,
        boundsHeight: Float
    ) {
        val width = pseudo.width ?: pseudo.left?.let { left ->
            pseudo.right?.let { right -> max(0f, boundsWidth - left - right) }
        } ?: boundsWidth
        val height = pseudo.height ?: pseudo.top?.let { top ->
            pseudo.bottom?.let { bottom -> max(0f, boundsHeight - top - bottom) }
        } ?: boundsHeight
        val x = pseudo.left ?: pseudo.right?.let { boundsWidth - it - width } ?: 0f
        val y = pseudo.top ?: pseudo.bottom?.let { boundsHeight - it - height } ?: 0f
        val radii = pseudo.cornerRadii
        val radius = pseudo.radius
        val stroke = pseudo.border
        context.backend.translated(x, y) {
            context.backend.translated(
                pseudo.translate.resolveX(width),
                pseudo.translate.resolveY(height)
            ) {
                context.backend.rotated(pseudo.rotate, width / 2f, height / 2f) {
                    context.backend.withAlpha(pseudo.opacity) {
                        pseudo.shadow?.takeIf { !it.inset }?.let {
                            context.backend.drawShadowedRoundedRect(0f, 0f, width, height, radius, it)
                        }
                        val gradient = pseudo.gradient
                        when {
                            gradient != null -> context.backend.drawRoundedGradientStops(
                                0f,
                                0f,
                                width,
                                height,
                                radius,
                                gradient.map { it.color to it.position },
                                pseudo.gradientDirection,
                                stroke
                            )
                            radii != null && pseudo.background != null ->
                                context.backend.drawRoundedRect(0f, 0f, width, height, radii, pseudo.background, stroke)
                            radius > 0f && pseudo.background != null ->
                                context.backend.drawRoundedRect(0f, 0f, width, height, radius, pseudo.background, stroke)
                            pseudo.background != null ->
                                context.backend.drawRect(0f, 0f, width, height, pseudo.background, stroke)
                            stroke != null ->
                                context.backend.drawRect(0f, 0f, width, height, null, stroke)
                        }
                        pseudo.shadow?.takeIf { it.inset }?.let {
                            context.backend.drawShadowedRoundedRect(0f, 0f, width, height, radius, it, pseudo.background, stroke)
                        }
                    }
                }
            }
        }
    }

    private fun pseudoVisible(pseudo: PseudoElement): Boolean =
        when (pseudo.visibility) {
            PseudoVisibility.Always -> true
            PseudoVisibility.Hovered -> interaction.hovered
            PseudoVisibility.Pressed -> interaction.pressed
            PseudoVisibility.Selected -> selected
            PseudoVisibility.Enabled -> !disabled
            PseudoVisibility.Disabled -> disabled
        }

    private fun animatedBounds(context: RenderContext): BoundsState {
        val key = interpolationKey ?: return BoundsState(x, y, measuredWidth, measuredHeight)
        val state = boundsAnimations.getOrPut(key) { BoundsState(x, y, measuredWidth, measuredHeight) }
        val amount = (1f - exp(-BoundsAnimationSpeed * context.time.deltaSeconds.coerceIn(0f, 0.1f))).coerceIn(0f, 1f)
        state.x += (x - state.x) * amount
        state.y += (y - state.y) * amount
        state.width += (measuredWidth - state.width) * amount
        state.height += (measuredHeight - state.height) * amount
        if (
            abs(state.x - x) > 0.1f ||
            abs(state.y - y) > 0.1f ||
            abs(state.width - measuredWidth) > 0.1f ||
            abs(state.height - measuredHeight) > 0.1f
        ) {
            requestRender()
        }
        return state
    }

    open fun measure(constraints: Constraints): Size {
        applyStyle()
        val size = layout.measure(this, constraints)
        measuredWidth = size.width.coerceIn(minWidth, maxWidth)
        measuredHeight = size.height.coerceIn(minHeight, maxHeight)
        return size
    }

    fun resolveSize(constraints: Constraints, contentWidth: Float, contentHeight: Float): Size {
        val resolvedWidth = when {
            explicitWidth && width > 0f -> width
            fillWidth && constraints.maxWidth.isFinite() -> constraints.maxWidth
            else -> measureWidthOverride ?: contentWidth
        }
        val resolvedHeight = when {
            explicitHeight && height > 0f -> height
            fillHeight && constraints.maxHeight.isFinite() -> constraints.maxHeight
            else -> measureHeightOverride ?: contentHeight
        }
        val upperWidth = constraints.maxWidth.coerceAtMost(maxWidth).coerceAtLeast(minWidth)
        val upperHeight = constraints.maxHeight.coerceAtMost(maxHeight).coerceAtLeast(minHeight)
        return Size(
            resolvedWidth.coerceIn(minWidth, upperWidth),
            resolvedHeight.coerceIn(minHeight, upperHeight)
        )
    }

    protected fun setMeasuredSize(size: Size) {
        measuredWidth = size.width
        measuredHeight = size.height
    }

    fun applyMeasuredSize() {
        skipMeasureOnce = true
    }

    fun applyLayoutSize(width: Float? = null, height: Float? = null) {
        updatingLayoutSize = true
        try {
            width?.let {
                this.width = it
                measureWidthOverride = it
            }
            height?.let {
                this.height = it
                measureHeightOverride = it
            }
        } finally {
            updatingLayoutSize = false
        }
    }

    internal fun layoutWidthOrMeasured(): Float =
        if (hasExplicitWidth && width > 0f) width else measureWidthOverride ?: measuredWidth

    internal fun layoutHeightOrMeasured(): Float =
        if (hasExplicitHeight && height > 0f) height else measureHeightOverride ?: measuredHeight

    fun add(child: Component): Component {
        child.parent?.remove(child)
        child.parent = this
        children += child
        child.attachTo(ownerHost, mounted)
        requestRender()
        return child
    }

    fun remove(child: Component): Boolean {
        val removed = children.remove(child)
        if (removed) {
            child.detachFromParent()
            requestRender()
        }
        return removed
    }

    fun clear() {
        children.forEach { it.detachFromParent() }
        children.clear()
        requestRender()
    }

    fun at(x: Number, y: Number) = apply {
        this.x = x.toFloat()
        this.y = y.toFloat()
        positionedByUser = true
    }

    fun size(width: Number, height: Number) = apply {
        this.width = width.toFloat()
        this.height = height.toFloat()
        explicitWidth = this.width > 0f
        explicitHeight = this.height > 0f
    }

    fun background(color: Color?) = apply {
        backgroundColor = color
    }

    fun border(color: Color?, width: Number = 1f) = apply {
        borderColor = color
        borderWidth = width.toFloat()
    }

    fun radius(radius: Number) = apply {
        cornerRadius = radius.toFloat()
        cornerRadii = null
    }

    fun cornerRadii(
        topLeft: Number,
        topRight: Number = topLeft,
        bottomRight: Number = topLeft,
        bottomLeft: Number = topLeft
    ) = apply {
        cornerRadii = CornerRadii(topLeft.toFloat(), topRight.toFloat(), bottomRight.toFloat(), bottomLeft.toFloat())
        cornerRadius = 0f
    }

    fun gradient(colors: List<Color>, direction: GradientDirection = GradientDirection.Vertical) = apply {
        gradientColors = colors
        backgroundGradientDirection = direction
    }

    fun backgrounds(vararg layers: BackgroundLayer) = apply {
        backgroundLayers = layers.toList()
    }

    fun backdropBlur(radius: Number) = apply {
        backdropBlurRadius = radius.toFloat().coerceAtLeast(0f)
    }

    fun borderSides(sides: BorderSides?) = apply {
        borderSides = sides
    }

    fun translate(x: Number = 0f, y: Number = 0f, xPercent: Number? = null, yPercent: Number? = null) = apply {
        translate = TransformOffset(x.toFloat(), y.toFloat(), xPercent?.toFloat(), yPercent?.toFloat())
    }

    fun before(element: PseudoElement?) = apply {
        beforeElement = element
    }

    fun after(element: PseudoElement?) = apply {
        afterElement = element
    }

    fun stateStyles(styles: StateStyles?) = apply {
        stateStyles = styles
    }

    fun backgroundImage(
        texture: TextureRegion?,
        fit: ImageFit = backgroundImageFit,
        alpha: Number = backgroundImageAlpha,
        width: Number? = backgroundImageWidth,
        height: Number? = backgroundImageHeight,
        positionX: ImagePosition = backgroundImagePositionX,
        positionY: ImagePosition = backgroundImagePositionY
    ) = apply {
        backgroundImage = texture
        backgroundImageFit = fit
        backgroundImageAlpha = alpha.toFloat().coerceIn(0f, 1f)
        backgroundImageWidth = width?.toFloat()
        backgroundImageHeight = height?.toFloat()
        backgroundImagePositionX = positionX
        backgroundImagePositionY = positionY
    }

    fun backgroundImageSize(width: Number? = null, height: Number? = null) = apply {
        backgroundImageWidth = width?.toFloat()
        backgroundImageHeight = height?.toFloat()
    }

    fun backgroundImagePosition(x: ImagePosition = backgroundImagePositionX, y: ImagePosition = backgroundImagePositionY) = apply {
        backgroundImagePositionX = x
        backgroundImagePositionY = y
    }

    fun shadow(shadow: Shadow?) = apply {
        elevationShadow = shadow
    }

    fun theme(theme: Theme?) = apply {
        this.theme = theme
    }

    fun style(style: Style?) = apply {
        this.style = style
    }

    fun fontWeight(weight: Int) = apply {
        style = (style ?: Style()).copy(fontWeight = weight)
    }

    fun letterSpacing(spacing: Number) = apply {
        style = (style ?: Style()).copy(letterSpacing = spacing.toFloat())
    }

    fun interpolationKey(key: Any?) = apply {
        interpolationKey = key
    }

    fun padding(all: Number) = apply {
        padding = Insets.all(all.toFloat())
    }

    fun padding(horizontal: Number, vertical: Number) = apply {
        padding = Insets.xy(horizontal.toFloat(), vertical.toFloat())
    }

    fun margin(all: Number) = apply {
        margin = Insets.all(all.toFloat())
    }

    fun margin(horizontal: Number, vertical: Number) = apply {
        margin = Insets.xy(horizontal.toFloat(), vertical.toFloat())
    }

    fun clipToBounds(enabled: Boolean = true) = apply {
        clipToBounds = enabled
    }

    fun opacity(value: Number) = apply {
        opacity = value.toFloat().coerceIn(0f, 1f)
    }

    fun scale(x: Number, y: Number = x) = apply {
        scaleX = x.toFloat()
        scaleY = y.toFloat()
    }

    fun rotate(degrees: Number) = apply {
        rotationDegrees = degrees.toFloat()
    }

    fun zIndex(value: Int) = apply {
        zIndex = value
    }

    fun position(positioning: Positioning) = apply {
        this.positioning = positioning
        if (positioning != Positioning.Flow) positionedByUser = true
    }

    fun inset(left: Number? = this.left, top: Number? = this.top, right: Number? = this.right, bottom: Number? = this.bottom) = apply {
        this.left = left?.toFloat()
        this.top = top?.toFloat()
        this.right = right?.toFloat()
        this.bottom = bottom?.toFloat()
        if (positioning == Positioning.Flow) positioning = Positioning.Absolute
        positionedByUser = true
    }

    fun anchor(
        left: Number? = this.left,
        top: Number? = this.top,
        right: Number? = this.right,
        bottom: Number? = this.bottom,
        leftPercent: Number? = this.leftPercent,
        topPercent: Number? = this.topPercent,
        rightPercent: Number? = this.rightPercent,
        bottomPercent: Number? = this.bottomPercent
    ) = apply {
        this.left = left?.toFloat()
        this.top = top?.toFloat()
        this.right = right?.toFloat()
        this.bottom = bottom?.toFloat()
        this.leftPercent = leftPercent?.toFloat()
        this.topPercent = topPercent?.toFloat()
        this.rightPercent = rightPercent?.toFloat()
        this.bottomPercent = bottomPercent?.toFloat()
        if (positioning == Positioning.Flow) positioning = Positioning.Absolute
        positionedByUser = true
    }

    fun absolute(left: Number? = this.left, top: Number? = this.top, right: Number? = this.right, bottom: Number? = this.bottom) = apply {
        positioning = Positioning.Absolute
        inset(left, top, right, bottom)
    }

    fun fixed(left: Number? = this.left, top: Number? = this.top, right: Number? = this.right, bottom: Number? = this.bottom) = apply {
        positioning = Positioning.Fixed
        inset(left, top, right, bottom)
    }

    fun gridArea(name: String?) = apply {
        gridArea = name
    }

    fun gridCell(row: Int? = gridRow, column: Int? = gridColumn) = apply {
        gridRow = row
        gridColumn = column
    }

    fun minSize(width: Number = minWidth, height: Number = minHeight) = apply {
        minWidth = width.toFloat()
        minHeight = height.toFloat()
    }

    fun maxSize(width: Number = maxWidth, height: Number = maxHeight) = apply {
        maxWidth = width.toFloat()
        maxHeight = height.toFloat()
    }

    fun weight(value: Number) = apply {
        weight = value.toFloat()
    }

    fun fillMaxWidth(enabled: Boolean = true) = apply { fillWidth = enabled }

    fun fillMaxHeight(enabled: Boolean = true) = apply { fillHeight = enabled }

    fun fillMaxSize(enabled: Boolean = true) = apply {
        fillWidth = enabled
        fillHeight = enabled
    }

    fun align(alignment: Alignment?) = apply { layoutAlignment = alignment }

    fun render(block: (RenderContext) -> Unit) = apply {
        renderHook = block
    }

    fun on(type: String, handler: (PointerEvent) -> Unit) = apply {
        events.on(type) { event ->
            if (event is PointerEvent) handler(event)
        }
    }

    fun <E : UIEvent> on(matcher: EventMatcher<E>, handler: (E) -> Unit) = apply {
        events.on(matcher, handler)
    }

    fun onKey(type: String, handler: (KeyEvent) -> Unit) = apply {
        events.on(type) { event ->
            if (event is KeyEvent) handler(event)
        }
    }

    fun onMount(block: () -> Unit) = apply {
        mountHooks += block
    }

    fun onUnmount(block: () -> Unit) = apply {
        unmountHooks += block
    }

    fun onDispose(block: () -> Unit) = apply {
        disposeHooks += block
    }

    fun requestFocus() {
        if (focusable && !disabled) ownerHost?.focus(this)
    }

    fun clearFocus() {
        ownerHost?.takeIf { it.focusedComponent === this }?.focus(null)
    }

    fun globalPosition(): Pair<Float, Float> {
        val parentPosition = parent?.globalPosition() ?: (0f to 0f)
        return parentPosition.first + x to parentPosition.second + y
    }

    fun sceneToLocal(sceneX: Float, sceneY: Float): Pair<Float, Float> {
        val (globalX, globalY) = globalPosition()
        var scrollOffsetY = 0f
        var node: Component? = parent
        while (node != null) {
            if (node is ScrollContainer) scrollOffsetY += node.scrollY
            node = node.parent
        }
        return (sceneX - globalX) to (sceneY - globalY + scrollOffsetY)
    }

    /** Right-center anchor in scene space, adjusted for scroll container viewport offset. */
    fun sceneAnchorRightCenter(): Pair<Float, Float> {
        val (gx, gy) = globalPosition()
        var scrollOffset = 0f
        var node: Component? = parent
        while (node != null) {
            if (node is ScrollContainer) scrollOffset += node.scrollY
            node = node.parent
        }
        return (gx + measuredWidth) to (gy - scrollOffset + measuredHeight / 2f)
    }

    protected fun readClipboard(): String? =
        ownerHost?.readClipboard()

    protected fun writeClipboard(text: String) {
        ownerHost?.writeClipboard(text)
    }

    protected fun ownerScene(): Scene? =
        ownerHost as? Scene

    fun dispose() {
        if (disposed) return
        detachFromParent()
        children.forEach { it.dispose() }
        children.clear()
        disposed = true
        disposeHooks.forEach { it() }
        events.clear()
        ownerHost = null
        parent = null
    }

    internal fun attachTo(host: ComponentHost?, shouldMount: Boolean) {
        ownerHost = host
        children.forEach { it.attachTo(host, shouldMount && mounted) }
        if (host != null && shouldMount && !mounted) mountRecursively()
    }

    internal fun mountRecursively() {
        if (mounted || disposed) return
        mounted = true
        mountHooks.forEach { it() }
        children.forEach { it.attachTo(ownerHost, true) }
    }

    internal fun unmountRecursively() {
        if (!mounted) return
        children.forEach { it.unmountRecursively() }
        mounted = false
        unmountHooks.forEach { it() }
        if (ownerHost?.focusedComponent === this) ownerHost?.focus(null)
    }

    internal fun setInteraction(update: ComponentInteraction.() -> ComponentInteraction) {
        interaction = interaction.update()
        requestRender()
    }

    internal fun dispatch(event: UIEvent) {
        if (disabled && event.dispatchPolicy == EventDispatchPolicy.IgnoreWhenDisabled) return
        events.emit(event)
    }

    protected fun requestRender() {
        ownerHost?.requestRender()
        parent?.requestRender()
    }

    internal open fun pointerPath(globalX: Float, globalY: Float): List<Component> {
        val hitWidth = if (measuredWidth > 0f) measuredWidth else width
        val hitHeight = if (measuredHeight > 0f) measuredHeight else height
        if (disposed || !visible || disabled || globalX < x || globalY < y || globalX > x + hitWidth || globalY > y + hitHeight) {
            return emptyList()
        }

        val localX = globalX - x
        val localY = globalY - y
        children.sortedBy { it.zIndex }.asReversed().forEach { child ->
            val childPath = child.pointerPath(localX, localY)
            if (childPath.isNotEmpty()) return listOf(this) + childPath
        }
        return listOf(this)
    }

    internal fun focusableDescendants(): List<Component> =
        buildList {
            if (focusable && !disabled && visible) add(this@Component)
            children.forEach { addAll(it.focusableDescendants()) }
        }

    private fun detachFromParent() {
        unmountRecursively()
        parent = null
        ownerHost = null
    }

    private fun resolveConstraint(explicit: Float, fallback: Float): Float =
        when {
            explicit > 0f -> explicit
            fallback > 0f -> fallback
            else -> Float.MAX_VALUE
        }

    protected fun resolvedMeasureWidth(fallback: Float): Float =
        width.takeIf { it > 0f } ?: measureWidthOverride ?: fallback

    protected fun resolvedMeasureHeight(fallback: Float): Float =
        height.takeIf { it > 0f } ?: measureHeightOverride ?: fallback

    protected fun applyStyle() {
        val current = resolveEffectiveStyle() ?: return
        current.width?.let { width = it }
        current.height?.let { height = it }
        current.minWidth?.let { minWidth = it }
        current.minHeight?.let { minHeight = it }
        current.maxWidth?.let { maxWidth = it }
        current.maxHeight?.let { maxHeight = it }
        current.background?.let { backgroundColor = it }
        current.backgroundLayers?.let { backgroundLayers = it }
        current.backgroundGradient?.let { gradientColors = it }
        current.backgroundGradientDirection?.let { backgroundGradientDirection = it }
        current.backdropBlurRadius?.let { backdropBlurRadius = it.coerceAtLeast(0f) }
        current.backgroundImage?.let { backgroundImage = it }
        current.backgroundImageFit?.let { backgroundImageFit = it }
        current.backgroundImageAlpha?.let { backgroundImageAlpha = it.coerceIn(0f, 1f) }
        current.backgroundImageWidth?.let { backgroundImageWidth = it }
        current.backgroundImageHeight?.let { backgroundImageHeight = it }
        current.backgroundImagePositionX?.let { backgroundImagePositionX = it }
        current.backgroundImagePositionY?.let { backgroundImagePositionY = it }
        current.border?.let { borderColor = it }
        current.borderWidth?.let { borderWidth = it }
        current.borderSides?.let { borderSides = it }
        current.radius?.let {
            cornerRadius = it
            cornerRadii = null
        }
        current.cornerRadii?.let {
            cornerRadii = it
            cornerRadius = 0f
        }
        current.padding?.let { padding = it }
        current.margin?.let { margin = it }
        current.opacity?.let { opacity = it.coerceIn(0f, 1f) }
        current.shadow?.let { elevationShadow = it }
        current.translate?.let { translate = it }
        current.before?.let { beforeElement = it }
        current.after?.let { afterElement = it }
        current.clipToBounds?.let { clipToBounds = it }
        current.layout?.let { layout = it }
        current.positioning?.let { positioning = it }
        current.left?.let {
            left = it
            positionedByUser = true
        }
        current.top?.let {
            top = it
            positionedByUser = true
        }
        current.right?.let {
            right = it
            positionedByUser = true
        }
        current.bottom?.let {
            bottom = it
            positionedByUser = true
        }
        current.leftPercent?.let {
            leftPercent = it
            positionedByUser = true
        }
        current.topPercent?.let {
            topPercent = it
            positionedByUser = true
        }
        current.rightPercent?.let {
            rightPercent = it
            positionedByUser = true
        }
        current.bottomPercent?.let {
            bottomPercent = it
            positionedByUser = true
        }
    }

    private fun resolveEffectiveStyle(): Style? {
        val base = style ?: return null
        var effective = base
        stateStyles?.let { states ->
            if (disabled) states.disabled?.let { effective = effective.merge(it) }
            if (!disabled) states.enabled?.let { effective = effective.merge(it) }
            if (selected) states.selected?.let { effective = effective.merge(it) }
            if (interaction.pressed) states.pressed?.let { effective = effective.merge(it) }
            if (interaction.hovered) states.hover?.let { effective = effective.merge(it) }
        }
        base.stateStyles?.let { states ->
            if (disabled) states.disabled?.let { effective = effective.merge(it) }
            if (!disabled) states.enabled?.let { effective = effective.merge(it) }
            if (selected) states.selected?.let { effective = effective.merge(it) }
            if (interaction.pressed) states.pressed?.let { effective = effective.merge(it) }
            if (interaction.hovered) states.hover?.let { effective = effective.merge(it) }
        }
        return effective
    }

    private fun drawBackgroundImage(context: RenderContext, texture: TextureRegion, boundsWidth: Float, boundsHeight: Float) {
        if (boundsWidth <= 0f || boundsHeight <= 0f) return
        val textureAspect = if (texture.width > 0f && texture.height > 0f) texture.width / texture.height else 1f
        val boundsAspect = boundsWidth / boundsHeight
        val fittedSize = when (backgroundImageFit) {
            ImageFit.Fill -> boundsWidth to boundsHeight
            ImageFit.Contain -> if (textureAspect > boundsAspect) {
                boundsWidth to boundsWidth / textureAspect
            } else {
                boundsHeight * textureAspect to boundsHeight
            }
            ImageFit.Cover -> if (textureAspect > boundsAspect) {
                boundsHeight * textureAspect to boundsHeight
            } else {
                boundsWidth to boundsWidth / textureAspect
            }
        }
        val explicitWidth = backgroundImageWidth
        val explicitHeight = backgroundImageHeight
        val drawWidth = explicitWidth ?: explicitHeight?.let { it * textureAspect } ?: fittedSize.first
        val drawHeight = explicitHeight ?: explicitWidth?.let { it / textureAspect } ?: fittedSize.second
        val drawX = backgroundImagePositionX.resolve(boundsWidth, drawWidth)
        val drawY = backgroundImagePositionY.resolve(boundsHeight, drawHeight)
        context.backend.clipped(0f, 0f, boundsWidth, boundsHeight) {
            context.backend.drawTexture(texture, drawX, drawY, max(0f, drawWidth), max(0f, drawHeight), backgroundImageAlpha)
        }
    }

    private companion object {
        private const val BoundsAnimationSpeed = 14f
        private val boundsAnimations = linkedMapOf<Any, BoundsState>()
    }
}

data class ComponentInteraction(
    val hovered: Boolean = false,
    val pressed: Boolean = false,
    val focused: Boolean = false
)
