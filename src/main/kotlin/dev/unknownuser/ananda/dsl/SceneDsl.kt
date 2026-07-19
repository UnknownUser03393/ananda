package dev.unknownuser.ananda.dsl

import dev.unknownuser.ananda.animation.Easing
import dev.unknownuser.ananda.animation.Easings
import dev.unknownuser.ananda.animation.Animation
import dev.unknownuser.ananda.animation.AnimationDirection
import dev.unknownuser.ananda.animation.RepeatMode
import dev.unknownuser.ananda.backend.FramebufferRegion
import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.ImageFit
import dev.unknownuser.ananda.backend.ImagePosition
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextureRegion
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.component.Button
import dev.unknownuser.ananda.component.Checkbox
import dev.unknownuser.ananda.component.CurveChart
import dev.unknownuser.ananda.component.ElevatedPanel
import dev.unknownuser.ananda.component.FramebufferView
import dev.unknownuser.ananda.component.FunctionalComponent
import dev.unknownuser.ananda.component.FunctionalComponentBody
import dev.unknownuser.ananda.component.GlowFrame
import dev.unknownuser.ananda.component.GlowLine
import dev.unknownuser.ananda.component.GlowOrb
import dev.unknownuser.ananda.component.Label
import dev.unknownuser.ananda.component.Panel
import dev.unknownuser.ananda.component.ProgressBar
import dev.unknownuser.ananda.component.RadioButton
import dev.unknownuser.ananda.component.ScrollContainer
import dev.unknownuser.ananda.component.Separator
import dev.unknownuser.ananda.component.SeparatorOrientation
import dev.unknownuser.ananda.component.Slider
import dev.unknownuser.ananda.component.Spacer
import dev.unknownuser.ananda.component.TextField
import dev.unknownuser.ananda.component.TextureView
import dev.unknownuser.ananda.component.ToggleSwitch
import dev.unknownuser.ananda.draw.Drawable
import dev.unknownuser.ananda.draw.Scene
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
import dev.unknownuser.ananda.shapes.Circle
import dev.unknownuser.ananda.shapes.CornerContinuity
import dev.unknownuser.ananda.shapes.Line
import dev.unknownuser.ananda.shapes.Rectangle
import dev.unknownuser.ananda.shapes.RoundedRectangle
import dev.unknownuser.ananda.text.TextRun
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.style.BackgroundLayer
import dev.unknownuser.ananda.style.BorderSides
import dev.unknownuser.ananda.style.PseudoElement
import dev.unknownuser.ananda.style.StateStyles
import dev.unknownuser.ananda.style.Style
import dev.unknownuser.ananda.theme.Theme
import dev.unknownuser.ananda.time.TimeFrame
import dev.unknownuser.ananda.time.TimeSubscription
import dev.unknownuser.ananda.time.TimeSystem
import java.awt.Color

fun scene(block: SceneBuilder.() -> Unit): Scene =
    Scene().also { SceneBuilder(it).block() }

class SceneBuilder(private val scene: Scene) {
    val time: TimeSystem get() = scene.time

    fun theme(theme: Theme) {
        scene.theme = theme
    }

    fun theme(block: ThemeBuilder.() -> Unit) {
        scene.theme = ThemeBuilder(scene.theme).apply(block).build()
    }

    fun rectangle(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        block: Rectangle.() -> Unit = {}
    ): Rectangle = add(Rectangle(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()).apply(block))

    fun roundedRectangle(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        radius: Number = 0f,
        continuity: CornerContinuity = CornerContinuity.Tangent,
        block: RoundedRectangle.() -> Unit = {}
    ): RoundedRectangle = add(
        RoundedRectangle(
            x.toFloat(),
            y.toFloat(),
            width.toFloat(),
            height.toFloat(),
            radius.toFloat(),
            continuity
        ).apply(block)
    )

    fun circle(
        x: Number = 0f,
        y: Number = 0f,
        radius: Number = 0f,
        block: Circle.() -> Unit = {}
    ): Circle = add(Circle(x.toFloat(), y.toFloat(), radius.toFloat()).apply(block))

    fun line(
        x1: Number = 0f,
        y1: Number = 0f,
        x2: Number = 0f,
        y2: Number = 0f,
        block: Line.() -> Unit = {}
    ): Line = add(Line(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat()).apply(block))

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
        )
        scene.add(panel)
        ComponentBuilder(panel).block()
        return panel
    }

    fun ElevatedPanel(block: ElevatedPanelBuilder.() -> Unit): ElevatedPanel {
        val panel = ElevatedPanelBuilder().apply(block).build()
        scene.add(panel)
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
        val orb = GlowOrb(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), colors, shadow)
        scene.add(orb)
        return orb
    }

    fun GlowOrb(block: GlowOrbBuilder.() -> Unit): GlowOrb {
        val orb = GlowOrbBuilder().apply(block).build()
        scene.add(orb)
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
        val line = GlowLine(x.toFloat(), y.toFloat(), x2.toFloat(), y2.toFloat(), color, width.toFloat(), glowColor, glowWidth.toFloat())
        scene.add(line)
        return line
    }

    fun GlowLine(block: GlowLineBuilder.() -> Unit): GlowLine {
        val line = GlowLineBuilder().apply(block).build()
        scene.add(line)
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
        )
        scene.add(frame)
        return frame
    }

    fun GlowFrame(block: GlowFrameBuilder.() -> Unit): GlowFrame {
        val frame = GlowFrameBuilder().apply(block).build()
        scene.add(frame)
        return frame
    }

    fun curveChart(
        values: List<Float>,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 240f,
        height: Number = 120f,
        block: CurveChart.() -> Unit = {}
    ): CurveChart =
        add(CurveChart(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), values).apply(block))

    fun CurveChart(block: CurveChartBuilder.() -> Unit): CurveChart {
        val chart = CurveChartBuilder().apply(block).build()
        scene.add(chart)
        return chart
    }

    fun text(
        value: String,
        x: Number = 0f,
        y: Number = 0f,
        size: Number = 18f,
        color: Color = Color.WHITE,
        block: TextRun.() -> Unit = {}
    ): TextRun = add(TextRun(value, x.toFloat(), y.toFloat(), size.toFloat(), color).apply(block))

    fun texture(
        texture: TextureRegion,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        alpha: Number = 1f
    ): TextureView = add(TextureView(texture, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), alpha.toFloat()))

    fun framebuffer(
        framebuffer: FramebufferRegion,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        alpha: Number = 1f
    ): FramebufferView = add(FramebufferView(framebuffer, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), alpha.toFloat()))

    fun component(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        block: ComponentBuilder.() -> Unit = {}
    ): Component {
        val component = Component(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
        scene.add(component)
        ComponentBuilder(component).block()
        return component
    }

    fun button(
        text: String,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 120f,
        height: Number = 36f,
        onClick: () -> Unit = {}
    ): Button {
        val button = Button(text, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), onClick)
        scene.add(button)
        return button
    }

    fun button(block: ButtonBuilder.() -> Unit): Button {
        val button = ButtonBuilder().apply(block).build()
        scene.add(button)
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
        val field = TextField(value, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), placeholder)
        scene.add(field)
        return field
    }

    fun checkbox(
        checked: State<Boolean>,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 28f,
        height: Number = 28f
    ): Checkbox {
        val checkbox = Checkbox(checked, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
        scene.add(checkbox)
        return checkbox
    }

    fun toggleSwitch(
        checked: State<Boolean>,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 36f,
        height: Number = 20f
    ): ToggleSwitch {
        val toggle = ToggleSwitch(checked, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
        scene.add(toggle)
        return toggle
    }

    fun progressBar(
        progress: State<Float>,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 180f,
        height: Number = 12f
    ): ProgressBar {
        val progressBar = ProgressBar(progress, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
        scene.add(progressBar)
        return progressBar
    }

    fun slider(
        value: State<Float>,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 160f,
        height: Number = 28f
    ): Slider {
        val slider = Slider(value, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
        scene.add(slider)
        return slider
    }

    fun spacer(width: Number = 0f, height: Number = 0f, weight: Number = 0f): Spacer {
        val spacer = Spacer(width.toFloat(), height.toFloat())
        if (weight.toFloat() > 0f) spacer.weight(weight)
        scene.add(spacer)
        return spacer
    }

    fun scroll(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        block: ComponentBuilder.() -> Unit = {}
    ): ScrollContainer {
        val container = ScrollContainer(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
        scene.add(container)
        ComponentBuilder(container).block()
        return container
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
        val radio = RadioButton(selected, option, text, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
        scene.add(radio)
        return radio
    }

    fun separator(
        orientation: SeparatorOrientation = SeparatorOrientation.Horizontal,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = if (orientation == SeparatorOrientation.Horizontal) 160f else 1f,
        height: Number = if (orientation == SeparatorOrientation.Horizontal) 1f else 160f
    ): Separator {
        val separator = Separator(orientation, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
        scene.add(separator)
        return separator
    }

    fun functional(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        block: FunctionalComponentBody
    ): FunctionalComponent {
        val component = FunctionalComponent(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), block)
        scene.add(component)
        return component
    }

    fun Functional(block: FunctionalBuilder.() -> Unit): FunctionalComponent {
        val component = FunctionalBuilder().apply(block).build()
        scene.add(component)
        return component
    }

    fun mount(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        component: FunctionalComponentBody
    ): FunctionalComponent = functional(x, y, width, height, component)

    fun animate(
        durationSeconds: Number,
        delaySeconds: Number = 0f,
        easing: Easing = Easings.Linear,
        repeat: Boolean = false,
        repeatCount: Int = if (repeat) Animation.RepeatForever else 0,
        repeatMode: RepeatMode = RepeatMode.Restart,
        direction: AnimationDirection = AnimationDirection.Forward,
        onFinished: () -> Unit = {},
        block: (Float) -> Unit
    ) = scene.animate(durationSeconds.toFloat(), delaySeconds.toFloat(), easing, repeat, repeatCount, repeatMode, direction, onFinished, block)

    fun animateFloat(
        from: Number,
        to: Number,
        durationSeconds: Number,
        delaySeconds: Number = 0f,
        easing: Easing = Easings.Linear,
        repeat: Boolean = false,
        repeatCount: Int = if (repeat) Animation.RepeatForever else 0,
        repeatMode: RepeatMode = RepeatMode.Restart,
        direction: AnimationDirection = AnimationDirection.Forward,
        onFinished: () -> Unit = {},
        block: (Float) -> Unit
    ) = scene.animateFloat(from.toFloat(), to.toFloat(), durationSeconds.toFloat(), delaySeconds.toFloat(), easing, repeat, repeatCount, repeatMode, direction, onFinished, block)

    fun onUpdate(block: (TimeFrame) -> Unit): TimeSubscription =
        scene.onUpdate(block)

    fun after(
        delaySeconds: Number,
        useScaledTime: Boolean = true,
        block: (TimeFrame) -> Unit
    ): TimeSubscription = scene.after(delaySeconds.toFloat(), useScaledTime, block)

    fun every(
        intervalSeconds: Number,
        immediate: Boolean = false,
        useScaledTime: Boolean = true,
        block: (TimeFrame) -> Unit
    ): TimeSubscription = scene.every(intervalSeconds.toFloat(), immediate, useScaledTime, block)

    fun timeScale(value: Number) {
        scene.time.timeScale = value.toFloat()
    }

    fun pauseTime() {
        scene.time.pause()
    }

    fun resumeTime() {
        scene.time.resume()
    }

    fun onPointer(type: String, handler: (PointerEvent) -> Unit) {
        scene.events.on(type) { event ->
            if (event is PointerEvent) handler(event)
        }
    }

    fun <E : UIEvent> on(matcher: EventMatcher<E>, handler: (E) -> Unit) {
        scene.events.on(matcher, handler)
    }

    private fun <T : Drawable> add(drawable: T): T {
        scene.add(drawable)
        return drawable
    }
}

@AnandaDsl
class ComponentBuilder(private val component: Component) {
    fun row(
        gap: Number = 0f,
        crossAxisAlignment: Alignment = Alignment.Start,
        mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start
    ) {
        component.layout = RowLayout(gap.toFloat(), crossAxisAlignment, mainAxisAlignment)
    }

    fun column(
        gap: Number = 0f,
        crossAxisAlignment: Alignment = Alignment.Start,
        mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start
    ) {
        component.layout = ColumnLayout(gap.toFloat(), crossAxisAlignment, mainAxisAlignment)
    }

    fun stack() {
        component.layout = StackLayout
    }

    fun row(
        gap: Number = 0f,
        crossAxisAlignment: Alignment = Alignment.Start,
        mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start,
        block: ComponentBuilder.() -> Unit
    ): Component = component {
        row(gap, crossAxisAlignment, mainAxisAlignment)
        block()
    }

    fun column(
        gap: Number = 0f,
        crossAxisAlignment: Alignment = Alignment.Start,
        mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start,
        block: ComponentBuilder.() -> Unit
    ): Component = component {
        column(gap, crossAxisAlignment, mainAxisAlignment)
        block()
    }

    fun stack(block: ComponentBuilder.() -> Unit): Component = component {
        stack()
        block()
    }

    fun box(
        horizontal: Alignment = Alignment.Start,
        vertical: Alignment = Alignment.Start,
        block: ComponentBuilder.() -> Unit
    ): Component = component {
        component.layout = BoxLayout(horizontal, vertical)
        block()
    }

    fun flowRow(
        horizontalGap: Number = 8f,
        verticalGap: Number = 8f,
        alignment: Alignment = Alignment.Center,
        block: ComponentBuilder.() -> Unit
    ): Component = component {
        component.layout = FlowRowLayout(horizontalGap.toFloat(), verticalGap.toFloat(), alignment)
        block()
    }

    fun adaptiveGrid(
        minCellWidth: Number = 160f,
        horizontalGap: Number = 12f,
        verticalGap: Number = 12f,
        maxColumns: Int = Int.MAX_VALUE,
        block: ComponentBuilder.() -> Unit
    ): Component = component {
        component.layout = AdaptiveGridLayout(minCellWidth.toFloat(), horizontalGap.toFloat(), verticalGap.toFloat(), maxColumns)
        block()
    }

    fun grid(
        columns: List<GridTrack> = listOf(GridTrack.Fraction()),
        gap: Number = 0f,
        columnGap: Number = gap,
        rowGap: Number = gap,
        crossAxisAlignment: Alignment = Alignment.Stretch,
        areas: List<List<String?>> = emptyList()
    ) {
        component.layout = GridLayout(columns, columnGap.toFloat(), rowGap.toFloat(), crossAxisAlignment, areas)
    }

    fun wrap(
        gap: Number = 0f,
        columnGap: Number = gap,
        rowGap: Number = gap,
        crossAxisAlignment: Alignment = Alignment.Start
    ) {
        component.layout = WrapLayout(columnGap.toFloat(), rowGap.toFloat(), crossAxisAlignment)
    }

    fun padding(all: Number) {
        component.padding = Insets.all(all.toFloat())
    }

    fun padding(horizontal: Number, vertical: Number) {
        component.padding = Insets.xy(horizontal.toFloat(), vertical.toFloat())
    }

    fun margin(all: Number) {
        component.margin = Insets.all(all.toFloat())
    }

    fun margin(horizontal: Number, vertical: Number) {
        component.margin = Insets.xy(horizontal.toFloat(), vertical.toFloat())
    }

    fun background(color: Color?) {
        component.backgroundColor = color
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
        component.backgroundImage(texture, fit, alpha, width, height, positionX, positionY)
    }

    fun backgroundImageSize(width: Number? = null, height: Number? = null) {
        component.backgroundImageSize(width, height)
    }

    fun backgroundImagePosition(x: ImagePosition = ImagePosition.Center, y: ImagePosition = ImagePosition.Center) {
        component.backgroundImagePosition(x, y)
    }

    fun backgrounds(vararg layers: BackgroundLayer) {
        component.backgrounds(*layers)
    }

    fun backdropBlur(radius: Number) {
        component.backdropBlur(radius)
    }

    fun borderSides(sides: BorderSides?) {
        component.borderSides(sides)
    }

    fun translate(x: Number = 0f, y: Number = 0f, xPercent: Number? = null, yPercent: Number? = null) {
        component.translate(x, y, xPercent, yPercent)
    }

    fun before(element: PseudoElement?) {
        component.before(element)
    }

    fun after(element: PseudoElement?) {
        component.after(element)
    }

    fun stateStyles(styles: StateStyles?) {
        component.stateStyles(styles)
    }

    fun border(color: Color?, width: Number = 1f) {
        component.borderColor = color
        component.borderWidth = width.toFloat()
    }

    fun cornerRadii(
        topLeft: Number,
        topRight: Number = topLeft,
        bottomRight: Number = topLeft,
        bottomLeft: Number = topLeft
    ) {
        component.cornerRadii(topLeft, topRight, bottomRight, bottomLeft)
    }

    fun theme(theme: Theme) {
        component.theme = theme
    }

    fun theme(block: ThemeBuilder.() -> Unit) {
        component.theme = ThemeBuilder(component.theme ?: Theme.Default).apply(block).build()
    }

    fun style(style: Style) {
        component.style = style
    }

    fun fontWeight(weight: Int) {
        component.fontWeight(weight)
    }

    fun letterSpacing(spacing: Number) {
        component.letterSpacing(spacing)
    }

    fun opacity(value: Number) {
        component.opacity(value)
    }

    fun scale(x: Number, y: Number = x) {
        component.scale(x, y)
    }

    fun rotate(degrees: Number) {
        component.rotate(degrees)
    }

    fun zIndex(value: Int) {
        component.zIndex(value)
    }

    fun position(positioning: Positioning) {
        component.position(positioning)
    }

    fun absolute(left: Number? = null, top: Number? = null, right: Number? = null, bottom: Number? = null) {
        component.absolute(left, top, right, bottom)
    }

    fun fixed(left: Number? = null, top: Number? = null, right: Number? = null, bottom: Number? = null) {
        component.fixed(left, top, right, bottom)
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
        component.anchor(left, top, right, bottom, leftPercent, topPercent, rightPercent, bottomPercent)
    }

    fun radius(value: Number) {
        component.radius(value)
    }

    fun shadow(shadow: Shadow?) {
        component.shadow(shadow)
    }

    fun clipToBounds(enabled: Boolean = true) {
        component.clipToBounds(enabled)
    }

    fun weight(value: Number) {
        component.weight(value)
    }

    fun size(width: Number, height: Number) {
        component.size(width, height)
    }

    fun width(value: Number) {
        component.width = value.toFloat()
    }

    fun height(value: Number) {
        component.height = value.toFloat()
    }

    fun fillMaxWidth(enabled: Boolean = true) {
        component.fillMaxWidth(enabled)
    }

    fun fillMaxHeight(enabled: Boolean = true) {
        component.fillMaxHeight(enabled)
    }

    fun fillMaxSize(enabled: Boolean = true) {
        component.fillMaxSize(enabled)
    }

    fun align(alignment: Alignment?) {
        component.align(alignment)
    }

    fun <T : Component> add(child: T, block: ComponentBuilder.() -> Unit = {}): T {
        component.add(child)
        ComponentBuilder(child).block()
        return child
    }

    fun disabled(value: Boolean) {
        component.disabled = value
    }

    fun gridArea(name: String?) {
        component.gridArea(name)
    }

    fun gridCell(row: Int? = null, column: Int? = null) {
        component.gridCell(row, column)
    }

    fun minSize(width: Number = component.minWidth, height: Number = component.minHeight) {
        component.minSize(width, height)
    }

    fun maxSize(width: Number = component.maxWidth, height: Number = component.maxHeight) {
        component.maxSize(width, height)
    }

    fun component(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        block: ComponentBuilder.() -> Unit = {}
    ): Component {
        val child = Component(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()).at(x, y)
        component.add(child)
        ComponentBuilder(child).block()
        return child
    }

    fun scroll(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        block: ComponentBuilder.() -> Unit = {}
    ): ScrollContainer {
        val child = ScrollContainer(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())
        component.add(child)
        ComponentBuilder(child).block()
        return child
    }

    fun panel(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        block: ComponentBuilder.() -> Unit = {}
    ): Panel {
        val child = Panel(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()).apply { at(x, y) }
        component.add(child)
        ComponentBuilder(child).block()
        return child
    }

    fun Panel(block: PanelBuilder.() -> Unit): Panel {
        val child = PanelBuilder().apply(block).build().apply { at(x, y) }
        component.add(child)
        return child
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
        val child = ElevatedPanel(
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
        component.add(child)
        ComponentBuilder(child).block()
        return child
    }

    fun ElevatedPanel(block: ElevatedPanelBuilder.() -> Unit): ElevatedPanel {
        val child = ElevatedPanelBuilder().apply(block).build().apply { at(x, y) }
        component.add(child)
        return child
    }

    fun glowOrb(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 120f,
        height: Number = 120f,
        colors: List<Color> = listOf(Color(234, 179, 8, 110), Color(234, 179, 8, 0)),
        shadow: Shadow? = null
    ): GlowOrb {
        val child = GlowOrb(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), colors, shadow).apply { at(x, y) }
        component.add(child)
        return child
    }

    fun GlowOrb(block: GlowOrbBuilder.() -> Unit): GlowOrb {
        val child = GlowOrbBuilder().apply(block).build().apply { at(x, y) }
        component.add(child)
        return child
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
        val child = GlowLine(x.toFloat(), y.toFloat(), x2.toFloat(), y2.toFloat(), color, width.toFloat(), glowColor, glowWidth.toFloat()).apply { at(x, y) }
        component.add(child)
        return child
    }

    fun GlowLine(block: GlowLineBuilder.() -> Unit): GlowLine {
        val child = GlowLineBuilder().apply(block).build().apply { at(x, y) }
        component.add(child)
        return child
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
        val child = GlowFrame(
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
        component.add(child)
        return child
    }

    fun GlowFrame(block: GlowFrameBuilder.() -> Unit): GlowFrame {
        val child = GlowFrameBuilder().apply(block).build().apply { at(x, y) }
        component.add(child)
        return child
    }

    fun curveChart(
        values: List<Float>,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 240f,
        height: Number = 120f,
        block: CurveChart.() -> Unit = {}
    ): CurveChart {
        val child = CurveChart(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), values).apply(block).apply { at(x, y) }
        component.add(child)
        return child
    }

    fun CurveChart(block: CurveChartBuilder.() -> Unit): CurveChart {
        val child = CurveChartBuilder().apply(block).build().apply { at(x, y) }
        component.add(child)
        return child
    }

    fun button(
        text: String,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 120f,
        height: Number = 36f,
        onClick: () -> Unit = {}
    ): Button {
        val child = Button(text, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), onClick).apply { at(x, y) }
        component.add(child)
        return child
    }

    fun button(block: ButtonBuilder.() -> Unit): Button {
        val child = ButtonBuilder().apply(block).build().apply { at(x, y) }
        component.add(child)
        return child
    }

    fun textField(
        value: State<String>,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 220f,
        height: Number = 36f,
        placeholder: String = ""
    ): TextField {
        val child = TextField(value, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), placeholder).apply { at(x, y) }
        component.add(child)
        return child
    }

    fun functional(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        block: FunctionalComponentBody
    ): FunctionalComponent {
        val child = FunctionalComponent(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), block).apply { at(x, y) }
        component.add(child)
        return child
    }

    fun Functional(block: FunctionalBuilder.() -> Unit): FunctionalComponent {
        val child = FunctionalBuilder().apply(block).build().apply { at(x, y) }
        component.add(child)
        return child
    }

    fun mount(
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        component: FunctionalComponentBody
    ): FunctionalComponent = functional(x, y, width, height, component)

    fun checkbox(
        checked: State<Boolean>,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 28f,
        height: Number = 28f
    ): Checkbox {
        val child = Checkbox(checked, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()).apply { at(x, y) }
        component.add(child)
        return child
    }

    fun toggleSwitch(
        checked: State<Boolean>,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 36f,
        height: Number = 20f
    ): ToggleSwitch {
        val child = ToggleSwitch(checked, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()).apply { at(x, y) }
        component.add(child)
        return child
    }

    fun progressBar(
        progress: State<Float>,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 180f,
        height: Number = 12f
    ): ProgressBar {
        val child = ProgressBar(progress, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()).apply { at(x, y) }
        component.add(child)
        return child
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
        val child = RadioButton(selected, option, text, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()).apply { at(x, y) }
        component.add(child)
        return child
    }

    fun separator(
        orientation: SeparatorOrientation = SeparatorOrientation.Horizontal,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = if (orientation == SeparatorOrientation.Horizontal) 160f else 1f,
        height: Number = if (orientation == SeparatorOrientation.Horizontal) 1f else 160f
    ): Separator {
        val child = Separator(orientation, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()).apply { at(x, y) }
        component.add(child)
        return child
    }

    fun slider(
        value: State<Float>,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 160f,
        height: Number = 28f
    ): Slider {
        val child = Slider(value, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()).apply { at(x, y) }
        component.add(child)
        return child
    }

    fun spacer(width: Number = 0f, height: Number = 0f, weight: Number = 1f): Spacer {
        val child = Spacer(width.toFloat(), height.toFloat())
        if (weight.toFloat() > 0f) child.weight(weight)
        component.add(child)
        return child
    }

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
        val child = Label(
            text,
            x.toFloat(),
            y.toFloat(),
            width.toFloat(),
            height.toFloat(),
            size?.toFloat(),
            color
        ).apply(block)
        child.at(x, y)
        component.add(child)
        return child
    }

    fun label(block: LabelBuilder.() -> Unit): Label {
        val child = LabelBuilder().apply(block).build()
        child.at(child.x, child.y)
        component.add(child)
        return child
    }

    fun texture(
        texture: TextureRegion,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        alpha: Number = 1f
    ): TextureView {
        val child = TextureView(texture, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), alpha.toFloat()).apply { at(x, y) }
        component.add(child)
        return child
    }

    fun framebuffer(
        framebuffer: FramebufferRegion,
        x: Number = 0f,
        y: Number = 0f,
        width: Number = 0f,
        height: Number = 0f,
        alpha: Number = 1f
    ): FramebufferView {
        val child = FramebufferView(framebuffer, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), alpha.toFloat()).apply { at(x, y) }
        component.add(child)
        return child
    }

    fun onPointer(type: String, handler: (PointerEvent) -> Unit) {
        component.on(type, handler)
    }

    fun <E : UIEvent> on(matcher: EventMatcher<E>, handler: (E) -> Unit) {
        component.on(matcher, handler)
    }
}
