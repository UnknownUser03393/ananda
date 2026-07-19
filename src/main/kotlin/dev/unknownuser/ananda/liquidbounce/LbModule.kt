package dev.unknownuser.ananda.liquidbounce

import dev.unknownuser.ananda.backend.CornerRadii
import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.component.Component

import dev.unknownuser.ananda.event.PointerDown
import dev.unknownuser.ananda.event.PointerMove
import dev.unknownuser.ananda.layout.ColumnLayout
import dev.unknownuser.ananda.layout.Constraints
import dev.unknownuser.ananda.layout.Insets
import dev.unknownuser.ananda.layout.Positioning
import dev.unknownuser.ananda.layout.Size
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.stateOf
import dev.unknownuser.ananda.style.BackgroundLayer
import dev.unknownuser.ananda.style.BorderSides
import dev.unknownuser.ananda.style.GradientStop
import dev.unknownuser.ananda.style.PseudoElement
import dev.unknownuser.ananda.style.PseudoVisibility
import dev.unknownuser.ananda.style.StateStyles
import dev.unknownuser.ananda.style.Style
import java.awt.event.MouseEvent
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/**
 * LiquidBounce `clickgui/Module.svelte` — toggle row, optional expandable settings, description hook.
 */
class LbModule(
    val name: String,
    val enabled: State<Boolean> = stateOf(false),
    val expanded: State<Boolean> = stateOf(false),
    val description: String = "",
    val aliases: List<String> = emptyList(),
    val hasSettings: Boolean = false,
    val highlighted: State<Boolean> = stateOf(false),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 270f,
    height: Float = 0f,
    private val onToggle: (Boolean) -> Unit = {},
    private val onDescription: ((LbDescriptionAnchor?) -> Unit)? = null,
    settings: List<Component> = emptyList()
) : Component(x, y, width, height) {
    private val storedSettings = settings.toMutableList()
    private var settingsProgress = if (expanded.value) 1f else 0f
    private val nameRow = LbModuleNameRow(
        name = name,
        rowWidth = width,
        enabled = enabled,
        highlighted = highlighted,
        hasSettings = hasSettings,
        expanded = expanded,
        onToggle = onToggle,
        onExpand = { expanded.value = !expanded.value },
        onDescription = onDescription,
        descriptionText = fullDescription(),
        moduleHost = this
    )
    private val settingsPanel = LbModuleSettingsPanel(width = width).apply {
        visible = expanded.value
        clipToBounds = true
        expansionProgress = settingsProgress
        padding = Insets(top = 3f, left = 12f, bottom = 8f, right = 12f)
        background(LbPalette.withAlpha(LbPalette.ClickGuiBase, 54))
        borderSides(BorderSides(left = Stroke(LbPalette.ClickGuiBorderStrong, 1f)))
        layout = ColumnLayout()
    }

    init {
        layout = ColumnLayout()
        borderSides(BorderSides(bottom = Stroke(LbPalette.withAlpha(LbPalette.ClickGuiText, 14), 1f)))
        add(nameRow)
        add(settingsPanel)
        applyExpandedState()
        expanded.subscribe {
            applyExpandedState()
            requestRender()
        }
        enabled.subscribe { requestRender() }
        highlighted.subscribe { requestRender() }
    }

    private fun applyExpandedState() {
        if (expanded.value) {
            mountSettings()
            settingsPanel.visible = true
        }
        settingsPanel.expansionProgress = settingsProgress
        settingsPanel.opacity = easeOutCubic(settingsProgress)
    }

    fun setSettings(children: List<Component>) {
        storedSettings.clear()
        storedSettings.addAll(children)
        settingsPanel.clear()
        applyExpandedState()
    }

    override fun draw(context: RenderContext) {
        updateSettingsAnimation(context.time.deltaSeconds)
        super.draw(context)
    }

    private fun updateSettingsAnimation(deltaSeconds: Float) {
        val target = if (expanded.value) 1f else 0f
        if (expanded.value) {
            mountSettings()
            settingsPanel.visible = true
        }
        settingsProgress = lbAnimate(settingsProgress, target, deltaSeconds, SettingsAnimationSpeed)
        val eased = easeOutCubic(settingsProgress)
        settingsPanel.expansionProgress = eased
        settingsPanel.opacity = eased
        if (!expanded.value && settingsProgress <= 0.01f) {
            settingsProgress = 0f
            settingsPanel.expansionProgress = 0f
            settingsPanel.visible = false
        }
        if (abs(settingsProgress - target) > 0.001f) requestRender()
    }

    private fun mountSettings() {
        if (settingsPanel.children.size == storedSettings.size && settingsPanel.children.containsAll(storedSettings)) return
        settingsPanel.clear()
        storedSettings.forEach { settingsPanel.add(it) }
    }

    private fun fullDescription(): String {
        if (aliases.isEmpty()) return description
        val aka = aliases.joinToString(", ")
        return if (description.isEmpty()) "(aka $aka)" else "$description (aka $aka)"
    }

    private companion object {
        private const val SettingsAnimationSpeed = 18f
    }
}

private class LbModuleSettingsPanel(
    width: Float
) : Component(width = width) {
    var expansionProgress: Float = 0f

    override fun measure(constraints: Constraints): Size {
        applyStyle()
        val contentConstraints = constraints.inset(padding)
        var contentWidth = 0f
        var contentHeight = 0f
        children.filter { it.visible && it.positioning == Positioning.Flow }.forEach { child ->
            val measured = child.measure(contentConstraints.inset(child.margin))
            contentWidth = max(contentWidth, measured.width + child.margin.left + child.margin.right)
            contentHeight += measured.height + child.margin.top + child.margin.bottom
        }
        val fullSize = resolveSize(
            constraints,
            contentWidth + padding.left + padding.right,
            contentHeight + padding.top + padding.bottom
        )
        val animatedSize = Size(fullSize.width, fullSize.height * expansionProgress.coerceIn(0f, 1f))
        setMeasuredSize(animatedSize)
        return animatedSize
    }
}

private fun lbAnimate(current: Float, target: Float, deltaSeconds: Float, speed: Float): Float {
    val step = 1f - exp(-speed * deltaSeconds.coerceIn(0f, 0.1f))
    return current + (target - current) * step
}

private fun easeOutCubic(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return 1f - (1f - t) * (1f - t) * (1f - t)
}

data class LbDescriptionAnchor(
    val x: Float,
    val y: Float,
    val anchor: LbDescriptionSide,
    val text: String
)

enum class LbDescriptionSide { Left, Right }

private class LbModuleNameRow(
    val name: String,
    rowWidth: Float,
    val enabled: State<Boolean>,
    val highlighted: State<Boolean>,
    val hasSettings: Boolean,
    val expanded: State<Boolean>,
    val onToggle: (Boolean) -> Unit,
    val onExpand: () -> Unit,
    val onDescription: ((LbDescriptionAnchor?) -> Unit)?,
    val descriptionText: String,
    private val moduleHost: LbModule
) : Component(width = rowWidth, height = 34f) {
    private var hoverAmount = 0f
    private var paddingShift = 0f

    init {
        clipToBounds = true
        padding = Insets(top = 10f, right = 34f, bottom = 10f, left = 14f)
        focusable = true
        enabled.subscribe { requestRender() }
        highlighted.subscribe { requestRender() }
        expanded.subscribe { requestRender() }
        on(PointerDown) {
            when (it.button) {
                MouseEvent.BUTTON3 -> {
                    onExpand()
                    onDescription?.invoke(null)
                    it.consume()
                }
                else -> {
                    val next = !enabled.value
                    enabled.value = next
                    onToggle(next)
                    it.consume()
                }
            }
        }
        on(PointerMove) {
            val hovered = interaction.hovered
            hoverAmount = if (interaction.hovered) 1f else 0f
            paddingShift = if (interaction.hovered) 3f else 0f
            onDescription?.invoke(
                if (hovered) {
                    val anchor = sceneAnchorRightCenter()
                    LbDescriptionAnchor(
                        x = anchor.first,
                        y = anchor.second,
                        anchor = LbDescriptionSide.Right,
                        text = descriptionText
                    )
                } else {
                    null
                }
            )
            requestRender()
        }
        render { context ->
            val textColor = when {
                enabled.value -> LbPalette.ClickGuiText
                hoverAmount > 0f -> LbPalette.ClickGuiText
                else -> LbPalette.ClickGuiTextDimmed
            }
            context.backend.drawText(
                name,
                padding.left + paddingShift,
                measuredHeight * 0.5f + 4f,
                dev.unknownuser.ananda.backend.TextStyle(12f, textColor, "Inter")
            )
            if (hasSettings) {
                val iconX = measuredWidth - padding.right - 6f
                val iconY = measuredHeight / 2f
                val opacity = if (expanded.value) 255 else 140
                val color = java.awt.Color(LbPalette.ClickGuiTextDimmed.red, LbPalette.ClickGuiTextDimmed.green, LbPalette.ClickGuiTextDimmed.blue, opacity)
                if (expanded.value) {
                    context.backend.drawLine(iconX - 4f, iconY, iconX + 4f, iconY, Stroke(color, 1.5f))
                } else {
                    context.backend.drawLine(iconX, iconY - 4f, iconX, iconY + 4f, Stroke(color, 1.4f))
                    context.backend.drawLine(iconX - 4f, iconY, iconX + 4f, iconY, Stroke(color, 1.4f))
                }
            }
        }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        applyModuleNameStyle()
        super.draw(context)
    }

    private fun applyModuleNameStyle() {
        selected = enabled.value
        style(
            Style(
                textSize = 12f,
                foreground = if (enabled.value) LbPalette.ClickGuiText else LbPalette.ClickGuiTextDimmed,
                stateStyles = StateStyles(
                    selected = Style(
                        backgroundLayers = listOf(
                            BackgroundLayer.Gradient(
                                listOf(
                                    GradientStop(LbPalette.withAlpha(LbPalette.Accent, 30), 0f),
                                    GradientStop(LbPalette.withAlpha(LbPalette.Accent, 8), 1f)
                                ),
                                GradientDirection.Horizontal
                            )
                        ),
                        shadow = Shadow(LbPalette.withAlpha(LbPalette.Accent, 11), spread = 1f, inset = true)
                    ),
                    hover = Style(background = LbPalette.withAlpha(LbPalette.ClickGuiText, 10))
                )
            )
        )
        if (enabled.value) {
            before(
                PseudoElement(
                    left = 0f,
                    top = 8f,
                    bottom = 8f,
                    width = 3f,
                    background = LbPalette.Accent,
                    cornerRadii = CornerRadii(0f, 3f, 3f, 0f),
                    visibility = PseudoVisibility.Selected
                )
            )
        } else {
            beforeElement = null
        }
        if (highlighted.value) {
            after(
                PseudoElement(
                    left = 4f,
                    top = 4f,
                    right = 4f,
                    bottom = 4f,
                    border = Stroke(LbPalette.Accent, 1f),
                    cornerRadii = CornerRadii(6f),
                    visibility = PseudoVisibility.Always
                )
            )
        } else {
            afterElement = null
        }
        if (hoverAmount > 0f && !enabled.value) {
            backgroundColor = LbPalette.withAlpha(LbPalette.ClickGuiText, 10)
        } else if (!enabled.value) {
            backgroundColor = null
        }
    }
}
