package dev.unknownuser.ananda.component

import dev.unknownuser.ananda.backend.FramebufferRegion
import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.ImageFit
import dev.unknownuser.ananda.backend.ImagePosition
import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextureRegion
import dev.unknownuser.ananda.dsl.ButtonBuilder
import dev.unknownuser.ananda.dsl.ComponentBuilder
import dev.unknownuser.ananda.dsl.CurveChartBuilder
import dev.unknownuser.ananda.dsl.ElevatedPanelBuilder
import dev.unknownuser.ananda.dsl.GlowFrameBuilder
import dev.unknownuser.ananda.dsl.GlowLineBuilder
import dev.unknownuser.ananda.dsl.GlowOrbBuilder
import dev.unknownuser.ananda.dsl.LabelBuilder
import dev.unknownuser.ananda.dsl.ThemeBuilder
import dev.unknownuser.ananda.event.UIEvent
import dev.unknownuser.ananda.event.EventMatcher
import dev.unknownuser.ananda.event.PointerEvent
import dev.unknownuser.ananda.layout.ColumnLayout
import dev.unknownuser.ananda.layout.AdaptiveGridLayout
import dev.unknownuser.ananda.layout.Alignment
import dev.unknownuser.ananda.layout.BoxLayout
import dev.unknownuser.ananda.layout.FlowRowLayout
import dev.unknownuser.ananda.layout.GridLayout
import dev.unknownuser.ananda.layout.GridTrack
import dev.unknownuser.ananda.layout.Insets
import dev.unknownuser.ananda.layout.MainAxisAlignment
import dev.unknownuser.ananda.layout.Positioning
import dev.unknownuser.ananda.layout.RowLayout
import dev.unknownuser.ananda.layout.StackLayout
import dev.unknownuser.ananda.layout.WrapLayout
import dev.unknownuser.ananda.reactive.ReactiveRuntime
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.stateOf
import dev.unknownuser.ananda.style.BackgroundLayer
import dev.unknownuser.ananda.style.BorderSides
import dev.unknownuser.ananda.style.PseudoElement
import dev.unknownuser.ananda.style.StateStyles
import dev.unknownuser.ananda.style.Style
import dev.unknownuser.ananda.theme.Theme
import dev.unknownuser.ananda.time.TimeFrame
import java.awt.Color

class FunctionalComponent(
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 0f,
    height: Float = 0f,
    private val content: FunctionalScope.() -> Unit
) : Component(x, y, width, height) {
    private val invalidate: () -> Unit = {
        dirty = true
        requestRender()
    }
    private var dirty = true
    private var dependencies = emptySet<State<*>>()
    private var lastViewportWidth = -1
    private var lastViewportHeight = -1
    private var observesFrame = false
    private var lastFrame = -1L
    private val slots = mutableListOf<Any?>()

    override fun draw(context: RenderContext) {
        if (
            dirty ||
            context.width != lastViewportWidth ||
            context.height != lastViewportHeight ||
            (observesFrame && context.time.frame != lastFrame)
        ) {
            rebuild(context)
        }
        super.draw(context)
    }

    fun invalidate() {
        dirty = true
        requestRender()
    }

    private fun rebuild(context: RenderContext) {
        dependencies.forEach { it.unsubscribe(invalidate) }
        clear()
        val scope = FunctionalScope(this, context, slots)
        val (_, nextDependencies) = withCurrentFunctionalScope(scope) {
            ReactiveRuntime.collect {
                scope.content()
            }
        }
        nextDependencies.forEach { it.subscribe(invalidate) }
        dependencies = nextDependencies
        lastViewportWidth = context.width
        lastViewportHeight = context.height
        observesFrame = scope.observesFrame
        lastFrame = context.time.frame
        dirty = false
    }
}

private val CurrentFunctionalScope = ThreadLocal<FunctionalScope?>()

fun currentFunctionalScope(): FunctionalScope =
    CurrentFunctionalScope.get() ?: error("Functional widgets can only run inside a FunctionalScope")

private inline fun <T> withCurrentFunctionalScope(scope: FunctionalScope, block: () -> T): T {
    val previous = CurrentFunctionalScope.get()
    CurrentFunctionalScope.set(scope)
    return try {
        block()
    } finally {
        CurrentFunctionalScope.set(previous)
    }
}

class FunctionalScope internal constructor(
    private val host: Component,
    val context: RenderContext,
    private val slots: MutableList<Any?> = mutableListOf()
) {
    private var slotIndex = 0

    val theme: Theme get() = context.theme
    val time: TimeFrame get() = context.time
    internal var observesFrame: Boolean = false
        private set
    val viewportWidth: Int get() = context.width
    val viewportHeight: Int get() = context.height
    val breakpoint: Breakpoint
        get() = when {
            viewportWidth < 640 -> Breakpoint.Compact
            viewportWidth < 1024 -> Breakpoint.Medium
            else -> Breakpoint.Expanded
        }

    fun <T> remember(factory: () -> T): T {
        val index = slotIndex++
        if (index == slots.size) slots += factory()
        @Suppress("UNCHECKED_CAST")
        return slots[index] as T
    }

    fun <T> useState(initialValue: T): State<T> =
        remember { stateOf(initialValue) }

    fun useFrame(): TimeFrame {
        observesFrame = true
        return time
    }

    fun component(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        block: Component.() -> Unit = {}
    ): Component = host.add(Component(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()).at(x, y).apply(block))

    fun label(
        text: String,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        size: Number? = null,
        color: Color? = null,
        block: Label.() -> Unit = {}
    ): Label {
        val label = Label(
            text,
            x.toFloat(),
            y.toFloat(),
            width.toFloat(),
            height.toFloat(),
            size?.toFloat(),
            color
        ).apply(block)
        label.at(x, y)
        host.add(label)
        return label
    }

    fun label(block: LabelBuilder.() -> Unit): Label {
        val label = LabelBuilder().apply(block).build()
        label.at(label.x, label.y)
        host.add(label)
        return label
    }

    fun button(
        text: String,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 120f,
        height: Number = 36f,
        onClick: () -> Unit = {}
    ): Button {
        val button = Button(text, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), onClick).apply { at(x, y) }
        host.add(button)
        return button
    }

    fun button(block: ButtonBuilder.() -> Unit): Button {
        val button = ButtonBuilder().apply(block).build().apply { at(x, y) }
        host.add(button)
        return button
    }

    fun textField(
        value: State<String>,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 220f,
        height: Number = 36f,
        placeholder: String = ""
    ): TextField {
        val field = TextField(value, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), placeholder).apply { at(x, y) }
        host.add(field)
        return field
    }

    fun texture(
        texture: TextureRegion,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        alpha: Number = 1f
    ): TextureView {
        val view = TextureView(texture, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), alpha.toFloat()).apply { at(x, y) }
        host.add(view)
        return view
    }

    fun framebuffer(
        framebuffer: FramebufferRegion,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        alpha: Number = 1f
    ): FramebufferView {
        val view = FramebufferView(framebuffer, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), alpha.toFloat()).apply { at(x, y) }
        host.add(view)
        return view
    }

    fun checkbox(
        checked: State<Boolean>,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 28f,
        height: Number = 28f
    ): Checkbox {
        val checkbox = Checkbox(checked, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()).apply { at(x, y) }
        host.add(checkbox)
        return checkbox
    }

    fun elevatedPanel(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        radius: Number = 18f,
        colors: List<Color> = listOf(Color(38, 43, 54, 230), Color(25, 28, 36, 230)),
        gradientDirection: GradientDirection = GradientDirection.DiagonalDown,
        shadow: Shadow? = Shadow(Color(0, 0, 0, 120), blurRadius = 18f, offsetY = 8f),
        outline: Stroke? = Stroke(Color(255, 255, 255, 34), 1f),
        outlineColors: List<Color>? = null,
        outlineGradientDirection: GradientDirection = GradientDirection.DiagonalDown,
        block: ComponentBuilder.() -> Unit = {}
    ): ElevatedPanel {
        val panel = ElevatedPanel(
            x.toFloat(),
            y.toFloat(),
            width.toFloat(),
            height.toFloat(),
            radius.toFloat(),
            colors,
            gradientDirection,
            shadow,
            outline,
            outlineColors,
            outlineGradientDirection
        ).apply { at(x, y) }
        host.add(panel)
        ComponentBuilder(panel).block()
        return panel
    }

    fun elevatedPanel(block: ElevatedPanelBuilder.() -> Unit): ElevatedPanel {
        val panel = ElevatedPanelBuilder().apply(block).build().apply { at(x, y) }
        host.add(panel)
        return panel
    }

    fun glowOrb(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 120f,
        height: Number = 120f,
        colors: List<Color> = listOf(Color(234, 179, 8, 110), Color(234, 179, 8, 0)),
        shadow: Shadow? = null
    ): GlowOrb {
        val orb = GlowOrb(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), colors, shadow).apply { at(x, y) }
        host.add(orb)
        return orb
    }

    fun glowOrb(block: GlowOrbBuilder.() -> Unit): GlowOrb {
        val orb = GlowOrbBuilder().apply(block).build().apply { at(x, y) }
        host.add(orb)
        return orb
    }

    fun glowLine(
        x: Number = 0f,
        y: Number = 0f,
        x2: Number = 0f,
        y2: Number = 0f,
        color: Color = Color(125, 211, 252),
        width: Number = 2f,
        glowColor: Color = Color(125, 211, 252, 90),
        glowWidth: Number = 10f
    ): GlowLine {
        val line = GlowLine(x.toFloat(), y.toFloat(), x2.toFloat(), y2.toFloat(), color, width.toFloat(), glowColor, glowWidth.toFloat()).apply { at(x, y) }
        host.add(line)
        return line
    }

    fun glowLine(block: GlowLineBuilder.() -> Unit): GlowLine {
        val line = GlowLineBuilder().apply(block).build().apply { at(x, y) }
        host.add(line)
        return line
    }

    fun glowFrame(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 120f,
        height: Number = 48f,
        radius: Number = 14f,
        glow: Shadow = Shadow(Color(94, 234, 212, 82), blurRadius = 16f, spread = 2f),
        outlineWidth: Number = 1.2f,
        outlineColors: List<Color> = listOf(Color(94, 234, 212, 190), Color(255, 222, 126, 150)),
        outlineGradientDirection: GradientDirection = GradientDirection.Horizontal
    ): GlowFrame {
        val frame = GlowFrame(
            x.toFloat(),
            y.toFloat(),
            width.toFloat(),
            height.toFloat(),
            radius.toFloat(),
            glow,
            outlineWidth.toFloat(),
            outlineColors,
            outlineGradientDirection
        ).apply { at(x, y) }
        host.add(frame)
        return frame
    }

    fun glowFrame(block: GlowFrameBuilder.() -> Unit): GlowFrame {
        val frame = GlowFrameBuilder().apply(block).build().apply { at(x, y) }
        host.add(frame)
        return frame
    }

    fun curveChart(
        values: List<Float>,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 240f,
        height: Number = 120f,
        block: CurveChart.() -> Unit = {}
    ): CurveChart {
        val chart = CurveChart(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), values).apply(block).apply { at(x, y) }
        host.add(chart)
        return chart
    }

    fun curveChart(block: CurveChartBuilder.() -> Unit): CurveChart {
        val chart = CurveChartBuilder().apply(block).build().apply { at(x, y) }
        host.add(chart)
        return chart
    }

    fun toggleSwitch(
        checked: State<Boolean>,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 36f,
        height: Number = 20f
    ): ToggleSwitch {
        val toggle = ToggleSwitch(checked, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()).apply { at(x, y) }
        host.add(toggle)
        return toggle
    }

    fun progressBar(
        progress: State<Float>,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 180f,
        height: Number = 12f
    ): ProgressBar {
        val progressBar = ProgressBar(progress, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()).apply { at(x, y) }
        host.add(progressBar)
        return progressBar
    }

    fun <T> radioButton(
        selected: State<T>,
        option: T,
        text: String = option.toString(),
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 160f,
        height: Number = 28f
    ): RadioButton<T> {
        val radio = RadioButton(selected, option, text, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()).apply { at(x, y) }
        host.add(radio)
        return radio
    }

    fun separator(
        orientation: SeparatorOrientation = SeparatorOrientation.Horizontal,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = if (orientation == SeparatorOrientation.Horizontal) 160f else 1f,
        height: Number = if (orientation == SeparatorOrientation.Horizontal) 1f else 160f
    ): Separator {
        val separator = Separator(orientation, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()).apply { at(x, y) }
        host.add(separator)
        return separator
    }

    fun slider(
        value: State<Float>,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 160f,
        height: Number = 28f
    ): Slider {
        val slider = Slider(value, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()).apply { at(x, y) }
        host.add(slider)
        return slider
    }

    fun spacer(width: Number = 0f, height: Number = 0f, weight: Number = 0f): Spacer {
        val spacer = Spacer(width.toFloat(), height.toFloat())
        if (weight.toFloat() > 0f) spacer.weight(weight)
        host.add(spacer)
        return spacer
    }

    fun scroll(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        block: FunctionalScope.() -> Unit = {}
    ): ScrollContainer {
        val container = ScrollContainer(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
        host.add(container)
        FunctionalScope(container, context).block()
        return container
    }

    fun functional(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        block: FunctionalComponentBody
    ): FunctionalComponent {
        val child = FunctionalComponent(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), block).apply { at(x, y) }
        host.add(child)
        return child
    }

    fun mount(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        component: FunctionalComponentBody
    ): FunctionalComponent = functional(x, y, width, height, component)

    fun row(
        gap: Number = 0f,
        crossAxisAlignment: Alignment = Alignment.Start,
        mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start
    ) {
        host.layout = RowLayout(gap.toFloat(), crossAxisAlignment, mainAxisAlignment)
    }

    fun column(
        gap: Number = 0f,
        crossAxisAlignment: Alignment = Alignment.Start,
        mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start
    ) {
        host.layout = ColumnLayout(gap.toFloat(), crossAxisAlignment, mainAxisAlignment)
    }

    fun stack() {
        host.layout = StackLayout
    }

    fun row(
        gap: Number = 0f,
        crossAxisAlignment: Alignment = Alignment.Start,
        mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start,
        block: FunctionalScope.() -> Unit
    ): Component = container(RowLayout(gap.toFloat(), crossAxisAlignment, mainAxisAlignment), block)

    fun column(
        gap: Number = 0f,
        crossAxisAlignment: Alignment = Alignment.Start,
        mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start,
        block: FunctionalScope.() -> Unit
    ): Component = container(ColumnLayout(gap.toFloat(), crossAxisAlignment, mainAxisAlignment), block)

    fun stack(block: FunctionalScope.() -> Unit): Component = container(StackLayout, block)

    fun box(
        horizontal: Alignment = Alignment.Start,
        vertical: Alignment = Alignment.Start,
        block: FunctionalScope.() -> Unit
    ): Component = container(BoxLayout(horizontal, vertical), block)

    fun flowRow(
        horizontalGap: Number = 8f,
        verticalGap: Number = 8f,
        alignment: Alignment = Alignment.Center,
        block: FunctionalScope.() -> Unit
    ): Component = container(FlowRowLayout(horizontalGap.toFloat(), verticalGap.toFloat(), alignment), block)

    fun adaptiveGrid(
        minCellWidth: Number = 160f,
        horizontalGap: Number = 12f,
        verticalGap: Number = 12f,
        maxColumns: Int = Int.MAX_VALUE,
        block: FunctionalScope.() -> Unit
    ): Component = container(
        AdaptiveGridLayout(minCellWidth.toFloat(), horizontalGap.toFloat(), verticalGap.toFloat(), maxColumns),
        block
    )

    fun grid(
        columns: List<GridTrack> = listOf(GridTrack.Fraction()),
        gap: Number = 0f,
        columnGap: Number = gap,
        rowGap: Number = gap,
        crossAxisAlignment: Alignment = Alignment.Stretch,
        areas: List<List<String?>> = emptyList()
    ) {
        host.layout = GridLayout(columns, columnGap.toFloat(), rowGap.toFloat(), crossAxisAlignment, areas)
    }

    fun wrap(
        gap: Number = 0f,
        columnGap: Number = gap,
        rowGap: Number = gap,
        crossAxisAlignment: Alignment = Alignment.Start
    ) {
        host.layout = WrapLayout(columnGap.toFloat(), rowGap.toFloat(), crossAxisAlignment)
    }

    fun padding(all: Number) {
        host.padding = Insets.all(all.toFloat())
    }

    fun padding(horizontal: Number, vertical: Number) {
        host.padding = Insets.xy(horizontal.toFloat(), vertical.toFloat())
    }

    fun margin(all: Number) {
        host.margin = Insets.all(all.toFloat())
    }

    fun margin(horizontal: Number, vertical: Number) {
        host.margin = Insets.xy(horizontal.toFloat(), vertical.toFloat())
    }

    fun background(color: Color?) {
        host.backgroundColor = color
    }

    fun backgroundImage(
        texture: TextureRegion?,
        fit: ImageFit = ImageFit.Fill,
        alpha: Number = 1f,
        width: Number? = null,
        height: Number? = null,
        positionX: ImagePosition = ImagePosition.Center,
        positionY: ImagePosition = ImagePosition.Center
    ) {
        host.backgroundImage(texture, fit, alpha, width, height, positionX, positionY)
    }

    fun backgroundImageSize(width: Number? = null, height: Number? = null) {
        host.backgroundImageSize(width, height)
    }

    fun backgroundImagePosition(x: ImagePosition = ImagePosition.Center, y: ImagePosition = ImagePosition.Center) {
        host.backgroundImagePosition(x, y)
    }

    fun backgrounds(vararg layers: BackgroundLayer) {
        host.backgrounds(*layers)
    }

    fun backdropBlur(radius: Number) {
        host.backdropBlur(radius)
    }

    fun borderSides(sides: BorderSides?) {
        host.borderSides(sides)
    }

    fun translate(x: Number = 0f, y: Number = 0f, xPercent: Number? = null, yPercent: Number? = null) {
        host.translate(x, y, xPercent, yPercent)
    }

    fun before(element: PseudoElement?) {
        host.before(element)
    }

    fun after(element: PseudoElement?) {
        host.after(element)
    }

    fun stateStyles(styles: StateStyles?) {
        host.stateStyles(styles)
    }

    fun border(color: Color?, width: Number = 1f) {
        host.borderColor = color
        host.borderWidth = width.toFloat()
    }

    fun cornerRadii(
        topLeft: Number,
        topRight: Number = topLeft,
        bottomRight: Number = topLeft,
        bottomLeft: Number = topLeft
    ) {
        host.cornerRadii(topLeft, topRight, bottomRight, bottomLeft)
    }

    fun style(style: Style) {
        host.style = style
    }

    fun fontWeight(weight: Int) {
        host.fontWeight(weight)
    }

    fun letterSpacing(spacing: Number) {
        host.letterSpacing(spacing)
    }

    fun opacity(value: Number) {
        host.opacity(value)
    }

    fun scale(x: Number, y: Number = x) {
        host.scale(x, y)
    }

    fun rotate(degrees: Number) {
        host.rotate(degrees)
    }

    fun zIndex(value: Int) {
        host.zIndex(value)
    }

    fun position(positioning: Positioning) {
        host.position(positioning)
    }

    fun absolute(left: Number? = null, top: Number? = null, right: Number? = null, bottom: Number? = null) {
        host.absolute(left, top, right, bottom)
    }

    fun fixed(left: Number? = null, top: Number? = null, right: Number? = null, bottom: Number? = null) {
        host.fixed(left, top, right, bottom)
    }

    fun anchor(
        left: Number? = null,
        top: Number? = null,
        right: Number? = null,
        bottom: Number? = null,
        leftPercent: Number? = null,
        topPercent: Number? = null,
        rightPercent: Number? = null,
        bottomPercent: Number? = null
    ) {
        host.anchor(left, top, right, bottom, leftPercent, topPercent, rightPercent, bottomPercent)
    }

    fun radius(value: Number) {
        host.radius(value)
    }

    fun shadow(shadow: Shadow?) {
        host.shadow(shadow)
    }

    fun clipToBounds(enabled: Boolean = true) {
        host.clipToBounds(enabled)
    }

    fun gridArea(name: String?) {
        host.gridArea(name)
    }

    fun gridCell(row: Int? = null, column: Int? = null) {
        host.gridCell(row, column)
    }

    fun minSize(width: Number = host.minWidth, height: Number = host.minHeight) {
        host.minSize(width, height)
    }

    fun maxSize(width: Number = host.maxWidth, height: Number = host.maxHeight) {
        host.maxSize(width, height)
    }

    fun size(width: Number, height: Number) {
        host.size(width, height)
    }

    fun width(value: Number) {
        host.width = value.toFloat()
    }

    fun height(value: Number) {
        host.height = value.toFloat()
    }

    fun weight(value: Number) {
        host.weight(value)
    }

    fun fillMaxWidth(enabled: Boolean = true) {
        host.fillMaxWidth(enabled)
    }

    fun fillMaxHeight(enabled: Boolean = true) {
        host.fillMaxHeight(enabled)
    }

    fun fillMaxSize(enabled: Boolean = true) {
        host.fillMaxSize(enabled)
    }

    fun align(alignment: Alignment?) {
        host.align(alignment)
    }

    fun theme(theme: Theme) {
        host.theme = theme
    }

    fun theme(block: ThemeBuilder.() -> Unit) {
        host.theme = ThemeBuilder(host.theme ?: Theme.Default).apply(block).build()
    }

    fun onPointer(type: String, handler: (PointerEvent) -> Unit) {
        host.on(type, handler)
    }

    fun <E : UIEvent> on(matcher: EventMatcher<E>, handler: (E) -> Unit) {
        host.on(matcher, handler)
    }

    private fun container(layout: dev.unknownuser.ananda.layout.Layout, block: FunctionalScope.() -> Unit): Component {
        val child = Component().apply { this.layout = layout }
        host.add(child)
        FunctionalScope(child, context).block()
        return child
    }
}

enum class Breakpoint {
    Compact,
    Medium,
    Expanded
}

typealias FunctionalComponentBody = FunctionalScope.() -> Unit
