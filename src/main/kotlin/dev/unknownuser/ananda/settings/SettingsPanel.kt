package dev.unknownuser.ananda.settings

import dev.unknownuser.ananda.component.ColorPicker
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.component.Dropdown
import dev.unknownuser.ananda.component.KeyBind
import dev.unknownuser.ananda.component.Label
import dev.unknownuser.ananda.component.Panel
import dev.unknownuser.ananda.component.Slider
import dev.unknownuser.ananda.component.TextField
import dev.unknownuser.ananda.component.ToggleSwitch
import dev.unknownuser.ananda.layout.ColumnLayout
import dev.unknownuser.ananda.layout.Constraints
import dev.unknownuser.ananda.layout.RowLayout
import dev.unknownuser.ananda.layout.Size
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.stateOf
import java.awt.Color

class SettingsPanel(
    private val group: SettingGroup,
    var title: String = group.name,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 320f,
    height: Float = 0f
) : Component(x, y, width, height) {
    private val column = Component(width = width, height = 0f).apply {
        layout = ColumnLayout(gap = 8f)
        padding(0f)
    }

    init {
        layout = ColumnLayout(gap = 10f)
        padding(12f)
        cornerRadius = 8f
        add(Label(title, width = width - 24f, height = 18f, size = 13f))
        add(column)
        rebuild()
    }

    fun rebuild() {
        column.clear()
        group.settings.forEach { setting ->
            column.add(SettingContainer(setting, buildSettingRow(setting, width - 24f)))
        }
        group.groups.forEach { child ->
            column.add(SettingContainer(child, buildGroupPanel(child, width - 24f)))
        }
        requestRender()
    }
}

private class SettingContainer(
    private val node: SettingNode,
    inner: Component
) : Component(width = inner.width, height = inner.height) {
    private var lastVisible = node.isVisible

    init {
        visible = lastVisible
        add(inner)
    }

    override fun measure(constraints: Constraints): Size {
        visible = node.isVisible
        if (!visible) {
            setMeasuredSize(Size(0f, 0f))
            return Size(0f, 0f)
        }
        val child = children.firstOrNull() ?: return Size(0f, 0f)
        val measured = child.measure(constraints)
        setMeasuredSize(measured)
        if (lastVisible != visible) {
            lastVisible = visible
            requestRender()
        }
        return measured
    }

    override fun draw(context: dev.unknownuser.ananda.backend.RenderContext) {
        visible = node.isVisible
        if (!visible) return
        super.draw(context)
    }
}

private fun buildGroupPanel(group: SettingGroup, width: Float): Component {
    val panel = Panel(width = width, height = 0f).apply {
        layout = ColumnLayout(gap = 7f)
        padding(8f)
        radius(6f)
        background(Color(255, 255, 255, 8))
        border(Color(255, 255, 255, 24), 1f)
        add(Label(group.name, width = width - 16f, height = 16f, size = 10f))
    }
    group.settings.forEach { setting ->
        panel.add(SettingContainer(setting, buildSettingRow(setting, width - 16f)))
    }
    group.groups.forEach { child ->
        panel.add(SettingContainer(child, buildGroupPanel(child, width - 16f)))
    }
    return panel
}

private fun buildSettingRow(setting: Setting<*>, width: Float): Component {
    val row = Component(width = width, height = rowHeight(setting)).apply {
        layout = RowLayout(gap = 8f)
        padding(0f)
    }
    val labelWidth = (width * 0.38f).coerceIn(96f, 150f)
    val controlWidth = (width - labelWidth - 8f).coerceAtLeast(80f)
    row.add(Label(setting.name, width = labelWidth, height = 28f, size = 10f, color = Color(150, 150, 160)))
    row.add(buildSettingWidget(setting, controlWidth))
    return row
}

private fun rowHeight(setting: Setting<*>): Float =
    when (setting) {
        is ColorSetting -> 32f
        else -> 28f
    }

private fun buildSettingWidget(setting: Setting<*>, width: Float): Component =
    when (setting) {
        is BooleanSetting -> {
            val state = stateOf(setting.value)
            state.subscribe { setting.set(state.value) }
            setting.subscribe { state.value = setting.value }
            ToggleSwitch(state, width = 42f, height = 22f)
        }
        is IntegerSetting -> {
            val normalized = stateOf(normalize(setting.value, setting.range))
            normalized.subscribe {
                val next = setting.range.first + ((setting.range.last - setting.range.first) * normalized.value).toInt()
                setting.set(next)
            }
            setting.subscribe { normalized.value = normalize(setting.value, setting.range) }
            Slider(normalized, width = width, height = 28f) {
                val next = setting.range.first + ((setting.range.last - setting.range.first) * it).toInt()
                next.toString()
            }
        }
        is FloatSetting -> {
            val normalized = stateOf(normalize(setting.value, setting.range))
            normalized.subscribe {
                val next = setting.range.start + (setting.range.endInclusive - setting.range.start) * normalized.value
                setting.set(next)
            }
            setting.subscribe { normalized.value = normalize(setting.value, setting.range) }
            Slider(normalized, width = width, height = 28f) {
                val next = setting.range.start + (setting.range.endInclusive - setting.range.start) * it
                "%.2f".format(next)
            }
        }
        is StringSetting -> {
            val state = stateOf(setting.value)
            state.subscribe { setting.set(state.value) }
            setting.subscribe { state.value = setting.value }
            TextField(state, width = width, height = 28f, placeholder = setting.name)
        }
        is ColorSetting -> {
            val state = stateOf(setting.value)
            state.subscribe { setting.set(state.value) }
            setting.subscribe { state.value = setting.value }
            ColorPicker(state, width = width, height = 32f)
        }
        is KeyBindSetting -> {
            val state = State(setting.value)
            state.subscribe { setting.set(state.value) }
            setting.subscribe { state.value = setting.value }
            KeyBind(state, width = width, height = 28f)
        }
        is EnumSetting<*> -> buildEnumWidget(setting, width)
    }

@Suppress("UNCHECKED_CAST")
private fun buildEnumWidget(setting: EnumSetting<*>, width: Float): Component {
    val state = State(setting.value as Enum<*>)
    state.subscribe { (setting as Setting<Enum<*>>).set(state.value) }
    setting.subscribe { state.value = setting.value as Enum<*> }
    return Dropdown(
        options = setting.options as List<Enum<*>>,
        selection = state,
        width = width,
        height = 28f
    ) { it.name }
}

private fun normalize(value: Int, range: IntRange): Float {
    val span = (range.last - range.first).coerceAtLeast(1)
    return ((value - range.first) / span.toFloat()).coerceIn(0f, 1f)
}

private fun normalize(value: Float, range: ClosedFloatingPointRange<Float>): Float {
    val span = (range.endInclusive - range.start).takeIf { it > 0f } ?: 1f
    return ((value - range.start) / span).coerceIn(0f, 1f)
}

fun buildSettingsPanel(group: SettingGroup, title: String = group.name, width: Float = 320f): SettingsPanel =
    SettingsPanel(group, title, width = width)
