package dev.unknownuser

import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextStyle
import dev.unknownuser.ananda.component.Button
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.component.KeyBind
import dev.unknownuser.ananda.component.Label
import dev.unknownuser.ananda.component.Slider
import dev.unknownuser.ananda.component.TextField
import dev.unknownuser.ananda.draw.Scene
import dev.unknownuser.ananda.event.PointerDown
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.theme.Palette
import dev.unknownuser.ananda.theme.Theme
import dev.unknownuser.ananda.theme.Typography
import dev.unknownuser.ananda.window.RenderMode
import dev.unknownuser.ananda.window.SkiaWindow
import java.awt.Color
import java.awt.event.KeyEvent
import kotlin.math.exp

private const val WindowWidth = 980
private const val WindowHeight = 620

private enum class DemoCategory {
    Combat,
    Movement,
    Render,
    Client
}

private enum class DemoMode {
    Compact,
    Focused,
    Experimental
}

fun main() {
    val model = ClickGuiDemoModel()
    val screen = ClickGuiDemoRoot(model)
    val scene = Scene().apply {
        theme = clickTheme()
        add(screen)
    }

    SkiaWindow(
        title = "Ananda Controls Demo",
        width = WindowWidth,
        height = WindowHeight,
        scene = scene,
        renderMode = RenderMode.OnDemand
    ).show()
}

private class ClickGuiDemoRoot(
    private val model: ClickGuiDemoModel
) : Component(width = WindowWidth.toFloat(), height = WindowHeight.toFloat()) {
    private val entranceAnimations = mutableMapOf<String, Float>()
    private val selectionAnimations = mutableMapOf<String, Float>()
    private val activeAnimations = mutableMapOf<String, Float>()
    private val rowPositions = mutableMapOf<String, Pair<Float, Float>>()
    private val widgetGroupHeights = mutableMapOf<String, Float>()
    private val expandedWidgetModules = linkedSetOf<String>()
    private var openEnumKey: String? = null
    private var selectedWidgetId: String? = null
    private var revision = 0

    init {
        rebuild()
    }

    private fun rebuild() {
        clear()
        add(
            Component(0f, 0f, WindowWidth.toFloat(), WindowHeight.toFloat())
                .background(Color(0, 0, 0, 92))
                .on(PointerDown) {
                    if (openEnumKey != null) {
                        openEnumKey = null
                        revision++
                        rebuild()
                        it.consume()
                    }
                }
                .positioned()
        )

        val outerWidth = 872f
        val outerHeight = 438f
        val left = (WindowWidth - outerWidth) / 2f
        val top = (WindowHeight - outerHeight) / 2f

        add(panel("panel:root", left, top, outerWidth, outerHeight, slide = 7f, alpha = 232, shadowAlpha = 80, outlineAlpha = 255, radius = 14f).positioned())
        add(Label("Lettucq", left + 18f, top + 14f, 74f, 14f, 10.5f, Color.WHITE).positioned())
        add(Label("Ananda / libredfin", left + 86f, top + 16f, 126f, 12f, 8f, Color(136, 136, 136)).positioned())
        add(Button("Reset", left + outerWidth - 68f, top + 10f, 52f, 18f) {
            model.reset()
            openEnumKey = null
            revision++
            rebuild()
        }.positioned())

        val contentLeft = left + 12f
        val contentTop = top + 42f
        val contentHeight = outerHeight - 54f
        val categoryWidth = 112f
        val moduleWidth = 238f
        val gap = 9f
        val valueWidth = outerWidth - 24f - categoryWidth - moduleWidth - gap * 2f

        addCategoryColumn(contentLeft, contentTop, categoryWidth, contentHeight)
        addModuleColumn(contentLeft + categoryWidth + gap, contentTop, moduleWidth, contentHeight)
        addValueColumn(contentLeft + categoryWidth + moduleWidth + gap * 2f, contentTop, valueWidth, contentHeight)
    }

    private fun addCategoryColumn(x: Float, y: Float, w: Float, h: Float) {
        add(panel("panel:categories", x, y, w, h, slide = 10f, alpha = 232, shadowAlpha = 45, outlineAlpha = 255, radius = 14f, stableKey = "panel:categories", headerHeight = 30f).positioned())
        add(Label("Categories", x + 12f, y + 9f, w - 24f, 12f, 8.5f, Color.WHITE).positioned())
        DemoCategory.entries.forEachIndexed { index, category ->
            add(categoryRow(category, x + 7f, y + 38f + index * 20f, w - 14f, category == model.selectedCategory).positioned())
        }
    }

    private fun addModuleColumn(x: Float, y: Float, w: Float, h: Float) {
        add(panel("panel:modules", x, y, w, h, slide = 8f, alpha = 232, shadowAlpha = 45, outlineAlpha = 255, radius = 14f, stableKey = "panel:modules", headerHeight = 30f).positioned())
        add(Label("${model.selectedCategory.name} modules", x + 12f, y + 9f, w - 24f, 12f, 8.5f, Color.WHITE).positioned())

        var rowY = y + 38f
        filteredModules().forEachIndexed { index, module ->
            if (rowY > y + h - 24f) return@forEachIndexed
            add(moduleRow(module, x + 7f, rowY, w - 14f, module == model.selectedModule, index).positioned())
            rowY += 19f
            if (expandedWidgetModules.contains(module.key)) {
                val widgets = module.widgets
                val groupHeight = widgets.size * 19f
                if (rowY <= y + h - 24f && widgets.isNotEmpty()) {
                    val group = AnimatedWidgetGroup(module.key, x + 20f, rowY, w - 27f, groupHeight)
                    var widgetY = 0f
                    widgets.forEachIndexed { widgetIndex, widget ->
                        group.add(widgetRow(module, widget, 0f, widgetY, w - 27f, index + widgetIndex).positioned())
                        widgetY += 19f
                    }
                    add(group.positioned())
                }
                rowY += groupHeight
            }
        }
    }

    private fun addValueColumn(x: Float, y: Float, w: Float, h: Float) {
        add(panel("panel:values", x, y, w, h, slide = 6f, alpha = 232, shadowAlpha = 45, outlineAlpha = 255, radius = 14f, stableKey = "panel:values", headerHeight = 42f).positioned())
        val module = model.selectedModule
        val widget = selectedWidget(module)
        val headerKey = widget?.id ?: module.key
        val header = AnimatedValueRow("value:$headerKey:header:$revision", x + 12f, y + 9f, w - 24f, 28f)
        header.add(Label(widget?.label ?: module.name, 0f, 0f, w - 24f, 13f, 10f, Color.WHITE).positioned())
        header.add(Label(
            if (widget != null) "Widget settings exported by ${module.name}." else module.description,
            0f,
            16f,
            w - 24f,
            12f,
            8f,
            Color(136, 136, 136)
        ).positioned())
        add(header.positioned())

        var rowY = y + 56f
        (widget?.values ?: module.values).take(11).forEach { value ->
            rowY += addValueRow(value, x + 12f, rowY, w - 24f)
        }
    }

    private fun categoryRow(category: DemoCategory, x: Float, y: Float, w: Float, selected: Boolean): Component {
        val row = AnimatedRow("category:${category.name}", "category:${category.name}", "category:${category.name}", selected, x, y, w, 16f, 0f)
        row.contentRenderer = { context, _, selectedAmount ->
            drawText(context, category.name, 11f + selectedAmount * 2f, 3f, 8.5f, lerpColor(Color(132, 132, 140), Color.WHITE, selectedAmount))
        }
        row.on(PointerDown) {
            if (model.selectedCategory != category) {
                model.selectedCategory = category
                model.selectedModule = filteredModules(category).first()
                openEnumKey = null
                selectedWidgetId = null
                revision++
                rebuild()
            }
            it.consume()
        }
        return row
    }

    private fun moduleRow(module: DemoModule, x: Float, y: Float, w: Float, selected: Boolean, index: Int): Component {
        val row = AnimatedRow("module:${module.key}:$revision", "module:${module.key}", "module:${module.key}", selected, x, y, w, 16f, 9f + index * 0.55f)
        row.activeKey = "enabled:${module.key}"
        row.activeNow = module.enabled.value
        row.contentRenderer = { context, activeAmount, selectedAmount ->
            drawModuleRowContent(context, module.name, activeAmount, selectedAmount)
        }
        row.on(PointerDown) {
            when (it.button) {
                0 -> module.enabled.value = !module.enabled.value
                1 -> {
                    model.selectedModule = module
                    openEnumKey = null
                    selectedWidgetId = null
                    if (module.widgets.isNotEmpty()) {
                        if (expandedWidgetModules.contains(module.key)) {
                            expandedWidgetModules.remove(module.key)
                        } else {
                            expandedWidgetModules.add(module.key)
                            widgetGroupHeights.remove("widgets:${module.key}")
                        }
                    }
                    revision++
                }
                else -> return@on
            }
            rebuild()
            it.consume()
        }
        if (module.widgets.isNotEmpty()) {
            row.add(centeredLabel(if (expandedWidgetModules.contains(module.key)) "-" else "+", w - 18f, 3f, 14f, 9f, 8f, Color(153, 153, 153)).positioned())
        }
        return row
    }

    private fun widgetRow(module: DemoModule, widget: DemoWidget, x: Float, y: Float, w: Float, index: Int): Component {
        val row = AnimatedRow(
            animationKey = "widget:${module.key}:${widget.id}:$revision",
            stableKey = "widget:${module.key}:${widget.id}:grouped",
            selectionKey = "widget:${module.key}:${widget.id}",
            selectedNow = selectedWidgetId == widget.id,
            x = x,
            y = y,
            width = w,
            height = 16f,
            slide = 9f + index * 0.55f,
            baseAlpha = 7
        )
        row.activeKey = "widget-enabled:${module.key}:${widget.id}"
        row.activeNow = widget.enabled.value
        row.contentRenderer = { context, activeAmount, selectedAmount ->
            drawModuleRowContent(context, widget.label, activeAmount, selectedAmount)
        }
        row.on(PointerDown) {
            if (model.selectedModule != module) {
                model.selectedModule = module
                openEnumKey = null
            }
            when (it.button) {
                0 -> widget.enabled.value = !widget.enabled.value
                1 -> {
                    selectedWidgetId = widget.id
                    revision++
                }
                else -> return@on
            }
            rebuild()
            it.consume()
        }
        return row
    }

    private fun addValueRow(value: DemoValue<*>, x: Float, y: Float, w: Float): Float {
        val labelWidth = 112f
        val controlWidth = (w - labelWidth - 8f).coerceAtMost(220f).coerceAtLeast(92f)
        val controlX = w - controlWidth
        val valueKey = "value:${value.key}"
        val rowHeight = if (value is EnumDemoValue<*> && openEnumKey == valueKey) 22f + value.options.size * 16f else 22f
        val row = AnimatedValueRow("$valueKey:$revision", x, y, w, rowHeight)
        row.add(Label(value.name, 0f, 4f, labelWidth, 11f, 8.5f, Color(153, 153, 169)).positioned())

        when (value) {
            is BoolDemoValue -> row.add(smallSwitch(w - 22f, 1f, value.state.value, "$valueKey:switch") {
                value.state.value = !value.state.value
                rebuild()
            }.positioned())
            is FloatDemoValue -> row.add(Slider(value.state, controlX, 0f, controlWidth, 18f) { "%.2f".format(it) }.interpolationKey("$valueKey:slider").positioned())
            is IntDemoValue -> {
                row.add(Button("-", controlX, 1f, 20f, 16f) {
                    value.state.value = (value.state.value - 1).coerceAtLeast(value.range.first)
                    rebuild()
                }.interpolationKey("$valueKey:minus").positioned())
                row.add(centeredLabel(value.state.value.toString(), controlX + 25f, 3f, controlWidth - 50f, 11f, 8f, Color(153, 153, 169)).interpolationKey("$valueKey:value").positioned())
                row.add(Button("+", controlX + controlWidth - 20f, 1f, 20f, 16f) {
                    value.state.value = (value.state.value + 1).coerceAtMost(value.range.last)
                    rebuild()
                }.interpolationKey("$valueKey:plus").positioned())
            }
            is TextDemoValue -> row.add(TextField(value.state, controlX, 0f, controlWidth, 18f, value.name).interpolationKey("$valueKey:text").positioned())
            is ColorDemoValue -> {
                row.add(colorSwatch(value.state.value, controlX, 2f).interpolationKey("$valueKey:swatch").positioned())
                val colorText = State("%08X".format(value.state.value.rgb))
                colorText.subscribe {
                    parseColorHex(colorText.value, value.state.value.alpha)?.let { parsed ->
                        if (parsed.rgb != value.state.value.rgb) value.state.value = parsed
                    }
                }
                row.add(TextField(colorText, controlX + 17f, 1f, controlWidth - 17f, 16f).interpolationKey("$valueKey:color").positioned())
            }
            is KeyDemoValue -> row.add(KeyBind(value.state, controlX, 0f, controlWidth, 18f).interpolationKey("$valueKey:key").positioned())
            is EnumDemoValue<*> -> {
                val open = openEnumKey == valueKey
                row.add(dropdown(value.currentName(), value.options, controlX, 1f, controlWidth, "$valueKey:enum:$revision", open, {
                    openEnumKey = if (open) null else valueKey
                    revision++
                    rebuild()
                }, {
                    value.selectAny(it)
                    openEnumKey = null
                    revision++
                    rebuild()
                }).positioned())
            }
        }
        add(row.positioned())
        return rowHeight
    }

    private fun filteredModules(category: DemoCategory = model.selectedCategory): List<DemoModule> =
        model.modules.filter { it.category == category }

    private fun selectedWidget(module: DemoModule): DemoWidget? =
        selectedWidgetId?.let { id -> module.widgets.firstOrNull { it.id == id } }
            ?: run {
                selectedWidgetId = null
                null
            }

    private fun smallSwitch(x: Float, y: Float, enabled: Boolean, key: String, onClick: () -> Unit): Component =
        Component(x, y + 3f, 20f, 10f)
            .interpolationKey(key)
            .render { context ->
                val track = if (enabled) Accent.withAlpha(230) else Color(0, 0, 0, 150)
                val border = if (enabled) Accent.withAlpha(120) else Color(255, 255, 255, 28)
                val knobX = if (enabled) 15f else 5f
                context.backend.drawRoundedRect(0f, 0f, 20f, 10f, 5f, track, Stroke(border, 1f))
                context.backend.drawCircle(knobX, 5f, 3.8f, Color.WHITE)
            }
            .on(PointerDown) {
                onClick()
                it.consume()
            }

    private fun dropdown(
        text: String,
        options: List<Any>,
        x: Float,
        y: Float,
        w: Float,
        key: String,
        open: Boolean,
        onToggle: () -> Unit,
        onSelect: (Any) -> Unit
    ): Component {
        val dropdown = AnimatedDropdown(key, open, x, y, w, 16f + if (open) options.size * 16f else 0f)
        dropdown.add(
            Component(0f, 0f, w, 16f)
                .render { context ->
                    drawCenteredText(context, text, 0f, 0f, w, 16f, 8f, if (open) Color.WHITE else Color(155, 155, 160))
                    drawText(context, if (open) "-" else "+", w - 14f, 3f, 8f, Color(150, 150, 156))
                }
                .on(PointerDown) {
                    onToggle()
                    it.consume()
                }
                .positioned()
        )
        if (open) {
            options.forEachIndexed { index, option ->
                dropdown.add(dropdownOption(option.toString(), option.toString() == text, 0f, 16f + index * 16f, w, 16f) {
                    onSelect(option)
                }.positioned())
            }
        }
        return dropdown
    }

    private fun dropdownOption(text: String, selected: Boolean, x: Float, y: Float, w: Float, h: Float, onClick: () -> Unit): Component =
        Component(x, y, w, h)
            .render { context ->
                if (selected) context.backend.drawRoundedRect(3f, 2f, w - 6f, h - 4f, 2f, Accent.withAlpha(185), null)
                drawCenteredText(context, text, 0f, 0f, w, h, 7.8f, if (selected) Color.WHITE else Color(150, 150, 156))
            }
            .on(PointerDown) {
                onClick()
                it.consume()
            }

    private fun colorSwatch(color: Color, x: Float, y: Float): Component =
        Component(x, y, 15f, 15f)
            .radius(3f)
            .background(color)
            .border(Color(255, 255, 255, 85), 1f)

    private fun parseColorHex(input: String, fallbackAlpha: Int): Color? {
        val hex = input.trim().removePrefix("#")
        if (hex.length != 6 && hex.length != 8) return null
        val raw = hex.toLongOrNull(16) ?: return null
        return if (hex.length == 6) {
            Color(((fallbackAlpha.coerceIn(0, 255).toLong() shl 24) or raw).toInt(), true)
        } else {
            Color(raw.toInt(), true)
        }
    }

    private fun panel(
        animationKey: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        slide: Float,
        alpha: Int = 238,
        shadowAlpha: Int = 70,
        outlineAlpha: Int = 255,
        radius: Float = 6f,
        stableKey: String = animationKey,
        headerHeight: Float = 0f
    ): Component =
        AnimatedPanel(
            animationKey = animationKey,
            stableKey = stableKey,
            slide = slide,
            x = x,
            y = y,
            width = w,
            height = h,
            panelRadius = radius,
            colors = listOf(Color(23, 22, 38, alpha), Color(24, 24, 32, alpha)),
            shadow = Shadow(Color(0, 0, 0, shadowAlpha), blurRadius = 6f, offsetX = 2f, offsetY = 4f).takeIf { shadowAlpha > 0 },
            outline = Stroke(Color(255, 255, 255, (outlineAlpha * 0.12f).toInt().coerceIn(0, 255)), 1f).takeIf { outlineAlpha > 0 },
            headerHeight = headerHeight
        )

    private inner class AnimatedPanel(
        private val animationKey: String,
        private val stableKey: String,
        private val slide: Float,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        private val panelRadius: Float,
        private val colors: List<Color>,
        private val shadow: Shadow?,
        private val outline: Stroke?,
        private val headerHeight: Float
    ) : Component(x, y, width, height) {
        init {
            interpolationKey(stableKey)
        }

        override fun draw(context: RenderContext) {
            val progress = animationStep(animationKey, context.time.deltaSeconds, 15f)
            val oldX = x
            x = oldX - slide * (1f - easeOutCubic(progress))
            context.backend.translated(x, y) {
                shadow?.let { context.backend.drawShadowedRoundedRect(0f, 0f, width, height, panelRadius, it) }
                context.backend.drawRoundedGradientRect(0f, 0f, width, height, panelRadius, colors, GradientDirection.Vertical, outline)
                if (headerHeight > 0f) {
                    context.backend.drawLine(10f, headerHeight, width - 10f, headerHeight, Stroke(Color(255, 255, 255, 18), 1f))
                }
            }
            x = oldX
            if (progress < 0.999f) requestRender()
        }
    }

    private inner class AnimatedValueRow(
        private val animationKey: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) : Component(x, y, width, height) {
        init {
            interpolationKey(animationKey)
        }

        override fun draw(context: RenderContext) {
            val progress = animationStep("value-row:$animationKey", context.time.deltaSeconds, 7f)
            val eased = easeOutCubic(progress)
            val oldX = x
            x = oldX + 18f * (1f - eased)
            super.draw(context)
            x = oldX
            if (progress < 0.999f) requestRender()
        }
    }

    private inner class AnimatedDropdown(
        private val animationKey: String,
        private val open: Boolean,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) : Component(x, y, width, height) {
        init {
            radius(3f)
            clipToBounds()
            interpolationKey(animationKey)
        }

        override fun draw(context: RenderContext) {
            val progress = animationStep("dropdown:$animationKey", context.time.deltaSeconds, 8f)
            val eased = easeOutCubic(progress)
            val oldHeight = height
            height = if (open) 16f + (oldHeight - 16f) * eased.coerceIn(0.01f, 1f) else oldHeight
            backgroundColor = Color(0, 0, 0, 135)
            borderColor = if (open) Accent.withAlpha(140) else Color(255, 255, 255, 22)
            borderWidth = 1f
            super.draw(context)
            height = oldHeight
            if (progress < 0.999f) requestRender()
        }
    }

    private inner class AnimatedWidgetGroup(
        private val moduleKey: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) : Component(x, y, width, height) {
        init {
            clipToBounds()
        }

        override fun draw(context: RenderContext) {
            val key = "widgets:$moduleKey"
            val oldHeight = height
            val current = widgetGroupHeights.getOrDefault(key, 0f)
            val next = smoothStep(current, oldHeight, context.time.deltaSeconds, 18f)
            widgetGroupHeights[key] = next
            height = next.coerceIn(0f, oldHeight)
            super.draw(context)
            height = oldHeight
            if (kotlin.math.abs(next - oldHeight) > 0.1f) requestRender()
        }
    }

    private inner class AnimatedRow(
        private val animationKey: String,
        private val stableKey: String,
        private val selectionKey: String,
        private val selectedNow: Boolean,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        private val slide: Float,
        private val baseAlpha: Int = 7
    ) : Component(x, y, width, height) {
        private var selectedAmount = selectionAnimations.getOrDefault(selectionKey, if (selectedNow) 1f else 0f)
        var activeKey: String? = null
        var activeNow: Boolean = false
        var contentRenderer: ((RenderContext, Float, Float) -> Unit)? = null

        init {
            interpolationKey(stableKey)
            radius(3f)
            render { context ->
                val key = activeKey
                val activeAmount = if (key != null) activeAnimations.getOrDefault(key, if (activeNow) 1f else 0f) else 0f
                contentRenderer?.invoke(context, activeAmount, selectedAmount)
            }
        }

        override fun draw(context: RenderContext) {
            selectedAmount = smoothStep(selectedAmount, if (selectedNow) 1f else 0f, context.time.deltaSeconds, 15f)
            selectionAnimations[selectionKey] = selectedAmount
            activeKey?.let { key ->
                val current = activeAnimations.getOrDefault(key, if (activeNow) 1f else 0f)
                activeAnimations[key] = smoothStep(current, if (activeNow) 1f else 0f, context.time.deltaSeconds, 16f)
            }
            val hoverAlpha = if (interaction.hovered) 10 else 0
            backgroundColor = Color(255, 255, 255, baseAlpha + hoverAlpha)
            val oldX = x
            val oldY = y
            val previous = rowPositions.getOrPut(stableKey) { oldX - slide to oldY }
            val displayX = smoothStep(previous.first, oldX, context.time.deltaSeconds, 18f)
            val displayY = smoothStep(previous.second, oldY, context.time.deltaSeconds, 18f)
            rowPositions[stableKey] = displayX to displayY
            x = displayX
            y = displayY
            super.draw(context)
            x = oldX
            y = oldY
            if (selectedAmount in 0.001f..0.999f || kotlin.math.abs(displayX - oldX) > 0.1f) requestRender()
        }
    }

    private fun drawModuleRowContent(context: RenderContext, text: String, activeAmount: Float, selectedAmount: Float) {
        val dotColor = lerpColor(Color(142, 142, 148), Accent, activeAmount)
        context.backend.drawCircle(8f, 8f, 1.7f + activeAmount * 0.5f, dotColor)
        val textColor = lerpColor(lerpColor(Color(118, 118, 126), Color(228, 228, 232), activeAmount), Color.WHITE, selectedAmount * 0.35f)
        drawText(context, text, 18f, 3f, 8.5f, textColor)
    }

    private fun centeredLabel(text: String, x: Float, y: Float, w: Float, h: Float, size: Float, color: Color): Component =
        Component(x, y, w, h).render { context -> drawCenteredText(context, text, 0f, 0f, w, h, size, color) }

    private fun drawCenteredText(context: RenderContext, text: String, x: Float, y: Float, w: Float, h: Float, size: Float, color: Color) {
        val style = TextStyle(size, color, context.theme.typography.fontFamily)
        val textWidth = context.backend.measureText(text, style).first
        context.backend.drawText(text, x + (w - textWidth) / 2f, y + h * 0.5f + size * 0.36f, style)
    }

    private fun drawText(context: RenderContext, text: String, x: Float, y: Float, size: Float, color: Color) {
        context.backend.drawText(text, x, y + size, TextStyle(size, color, context.theme.typography.fontFamily))
    }

    private fun animationStep(key: String, deltaSeconds: Float, speed: Float): Float {
        val current = entranceAnimations.getOrDefault(key, 0f)
        val next = smoothStep(current, 1f, deltaSeconds, speed)
        entranceAnimations[key] = next
        return next
    }

    private fun smoothStep(current: Float, target: Float, deltaSeconds: Float, speed: Float): Float {
        val step = 1f - exp(-speed * deltaSeconds.coerceIn(0f, 0.1f))
        return current + (target - current) * step
    }

    private fun easeOutCubic(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        val inverse = 1f - t
        return 1f - inverse * inverse * inverse
    }
}

private class ClickGuiDemoModel {
    val modules = listOf(
        DemoModule("killaura", "Kill Aura", "Combat targeting and swing tuning.", DemoCategory.Combat, true),
        DemoModule("velocity", "Velocity", "Knockback response controls.", DemoCategory.Combat, false),
        DemoModule("speed", "Speed", "Movement acceleration presets.", DemoCategory.Movement, true),
        DemoModule("flight", "Flight", "Creative style movement tuning.", DemoCategory.Movement, false),
        DemoModule("clickgui", "Click GUI", "Panel visuals and interaction style.", DemoCategory.Render, true),
        DemoModule("hud", "HUD Designer", "Widget placement and theme settings.", DemoCategory.Render, true),
        DemoModule("interface", "Interface", "Client feedback and notifications.", DemoCategory.Client, true),
        DemoModule("profiles", "Profiles", "Config state and key binding.", DemoCategory.Client, false)
    )
    var selectedCategory = DemoCategory.Render
    var selectedModule = modules.first { it.key == "clickgui" }

    init {
        populateValues()
    }

    fun reset() {
        modules.forEach { module ->
            module.enabled.value = module.defaultEnabled
            module.values.clear()
            module.widgets.clear()
        }
        selectedCategory = DemoCategory.Render
        selectedModule = modules.first { it.key == "clickgui" }
        populateValues()
    }

    private fun populateValues() {
        modules.forEach { module ->
            module.values += BoolDemoValue("enabled", "Enabled", module.enabled)
            module.values += FloatDemoValue("opacity", "Opacity", State(0.78f))
            module.values += IntDemoValue("columns", "Columns", State(3), 1..8)
            module.values += TextDemoValue("profile", "Profile", State("Default"))
            module.values += ColorDemoValue("accent", "Accent", State(Accent))
            module.values += EnumDemoValue("mode", "Mode", State(DemoMode.Compact), DemoMode.entries)
            module.values += KeyDemoValue("toggle", "Toggle key", State<Int?>(KeyEvent.VK_R))
        }
        modules.first { it.key == "hud" }.widgets += listOf(
            demoWidget("module-list", "Module List", true),
            demoWidget("watermark", "Watermark", true),
            demoWidget("notifications", "Notifications", false)
        )
        modules.first { it.key == "interface" }.widgets += listOf(
            demoWidget("session", "Session Stats", true),
            demoWidget("status", "Status Feed", false)
        )
    }

    private fun demoWidget(id: String, label: String, enabled: Boolean): DemoWidget =
        DemoWidget(id, label, State(enabled)).apply {
            values += BoolDemoValue("shown:$id", "Shown", this.enabled)
            values += FloatDemoValue("scale:$id", "Scale", State(0.82f))
            values += IntDemoValue("padding:$id", "Padding", State(4), 0..12)
            values += ColorDemoValue("tint:$id", "Tint", State(Accent))
            values += EnumDemoValue("mode:$id", "Mode", State(DemoMode.Focused), DemoMode.entries)
        }
}

private class DemoModule(
    val key: String,
    val name: String,
    val description: String,
    val category: DemoCategory,
    enabled: Boolean
) {
    val defaultEnabled = enabled
    val enabled = State(enabled)
    val values = mutableListOf<DemoValue<*>>()
    val widgets = mutableListOf<DemoWidget>()
}

private class DemoWidget(
    val id: String,
    val label: String,
    val enabled: State<Boolean>
) {
    val values = mutableListOf<DemoValue<*>>()
}

private sealed class DemoValue<T>(val key: String, val name: String, val state: State<T>)
private class BoolDemoValue(key: String, name: String, state: State<Boolean>) : DemoValue<Boolean>(key, name, state)
private class FloatDemoValue(key: String, name: String, state: State<Float>) : DemoValue<Float>(key, name, state)
private class IntDemoValue(key: String, name: String, state: State<Int>, val range: IntRange) : DemoValue<Int>(key, name, state)
private class TextDemoValue(key: String, name: String, state: State<String>) : DemoValue<String>(key, name, state)
private class ColorDemoValue(key: String, name: String, state: State<Color>) : DemoValue<Color>(key, name, state)
private class KeyDemoValue(key: String, name: String, state: State<Int?>) : DemoValue<Int?>(key, name, state)
private class EnumDemoValue<T : Enum<T>>(
    key: String,
    name: String,
    state: State<T>,
    val typedOptions: List<T>
) : DemoValue<T>(key, name, state) {
    val options: List<Any> = typedOptions
    fun currentName(): String = state.value.name
    fun selectAny(option: Any) {
        @Suppress("UNCHECKED_CAST")
        state.value = option as T
    }
}

private fun clickTheme(): Theme =
    Theme(
        palette = Palette(
            background = Color(0, 0, 0),
            surface = Color(23, 22, 38),
            primary = Accent,
            accent = Accent,
            danger = Color(224, 53, 53),
            text = Color.WHITE,
            mutedText = Color(136, 136, 136),
            border = Color(255, 255, 255, 24)
        ),
        typography = Typography(fontFamily = "Segoe UI", bodySize = 8.5f, titleSize = 10.5f)
    )

private fun lerpColor(from: Color, to: Color, progress: Float): Color {
    val t = progress.coerceIn(0f, 1f)
    return Color(
        (from.red + (to.red - from.red) * t).toInt().coerceIn(0, 255),
        (from.green + (to.green - from.green) * t).toInt().coerceIn(0, 255),
        (from.blue + (to.blue - from.blue) * t).toInt().coerceIn(0, 255),
        (from.alpha + (to.alpha - from.alpha) * t).toInt().coerceIn(0, 255)
    )
}

private fun Color.withAlpha(alpha: Int): Color =
    Color(red, green, blue, alpha.coerceIn(0, 255))

private fun <T : Component> T.positioned(): T {
    at(x, y)
    return this
}

private val Accent = Color(150, 45, 45)
