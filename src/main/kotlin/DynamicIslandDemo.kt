package dev.unknownuser

import dev.unknownuser.ananda.animation.Easings
import dev.unknownuser.ananda.animation.lerp
import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextStyle
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.draw.Scene
import dev.unknownuser.ananda.event.KeyDown
import dev.unknownuser.ananda.event.PointerDown
import dev.unknownuser.ananda.event.PointerUp
import dev.unknownuser.ananda.event.or
import dev.unknownuser.ananda.layout.Alignment
import dev.unknownuser.ananda.layout.ColumnLayout
import dev.unknownuser.ananda.layout.RowLayout
import dev.unknownuser.ananda.layout.StackLayout
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.stateOf
import dev.unknownuser.ananda.theme.Palette
import dev.unknownuser.ananda.theme.Theme
import dev.unknownuser.ananda.theme.Typography
import dev.unknownuser.ananda.window.SkiaWindow
import java.awt.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

private const val WindowWidth = 420f
private const val WindowHeight = 780f

private val IslandTint = Color(8, 8, 12, 215)
private val IslandStroke = Color(255, 255, 255, 48)
private val PhoneBezel = Color(42, 42, 45)
private val PhoneBezelHighlight = Color(72, 72, 76)
private val ScreenBackground = Color(8, 8, 10)
private val AccentGreen = Color(48, 209, 88)
private val AccentOrange = Color(255, 159, 10)
private val AccentBlue = Color(10, 132, 255)

enum class IslandMode {
    Idle,
    Music,
    Timer,
    Call
}

fun main() {
    val mode = stateOf(IslandMode.Idle)

    val scene = Scene().apply {
        theme = Theme(
            palette = Palette(
                background = Color(22, 22, 24),
                surface = Color(44, 44, 46),
                primary = AccentBlue,
                accent = AccentGreen,
                text = Color(255, 255, 255),
                mutedText = Color(152, 152, 157),
                border = Color(72, 72, 74)
            ),
            typography = Typography(fontFamily = "Microsoft YaHei UI")
        )
        add(DynamicIslandDemoRoot(mode))
    }

    SkiaWindow(scene = scene, width = WindowWidth.toInt(), height = WindowHeight.toInt()).show()
}

private class DynamicIslandDemoRoot(
    private val mode: State<IslandMode>
) : Component(width = WindowWidth, height = WindowHeight) {
    init {
        layout = ColumnLayout(gap = 18f, crossAxisAlignment = Alignment.Center)
        padding(0f, 22f)
        add(PhoneFrame(mode, width = 360f, height = 620f))
        add(ModeControls(mode, width = 360f, height = 40f))
        add(
            HintLabel(
                "切换状态时观察模糊、形变与内容过渡动画",
                width = 360f,
                height = 28f
            )
        )
    }
}

private class PhoneFrame(
    private val mode: State<IslandMode>,
    width: Float,
    height: Float
) : Component(width = width, height = height) {
    private val screenInset = 10f

    init {
        val screenWidth = width - screenInset * 2f
        val screenHeight = height - screenInset * 2f
        add(
            PhoneScreen(
                mode = mode,
                x = screenInset,
                y = screenInset,
                width = screenWidth,
                height = screenHeight
            )
        )
        render { context ->
            val backend = context.backend
            backend.drawRoundedRect(0f, 0f, measuredWidth, measuredHeight, 44f, PhoneBezel)
            backend.drawRoundedGradientOutline(
                1.5f,
                1.5f,
                measuredWidth - 3f,
                measuredHeight - 3f,
                42f,
                1.2f,
                listOf(PhoneBezelHighlight, Color(32, 32, 34)),
                GradientDirection.DiagonalDown
            )
            backend.drawRoundedRect(
                screenInset - 1f,
                screenInset - 1f,
                screenWidth + 2f,
                screenHeight + 2f,
                36f,
                null,
                Stroke(Color(0, 0, 0), 2f)
            )
        }
    }
}

private class PhoneScreen(
    private val mode: State<IslandMode>,
    x: Float,
    y: Float,
    width: Float,
    height: Float
) : Component(x, y, width, height) {
    init {
        layout = StackLayout
        clipToBounds = true
        cornerRadius = 36f
        add(Wallpaper(width = width, height = height))
        add(DynamicIsland(mode, width = width, height = 88f))
        add(StatusBar(width = width, height = 48f))
        add(HomeIndicator(width = width, height = height))
    }
}

private class Wallpaper(width: Float, height: Float) : Component(width = width, height = height) {
    init {
        render { context ->
            drawWallpaperGradients(context.backend, measuredWidth, measuredHeight)
        }
    }
}

private class StatusBar(width: Float, height: Float) : Component(width = width, height = height) {
    init {
        render { context ->
            val mono = context.theme.typography.fontFamily
            val timeStyle = TextStyle(15f, Color(255, 255, 255, 220), mono)
            val iconStyle = TextStyle(13f, Color(255, 255, 255, 200), mono)
            val backend = context.backend
            backend.drawText("9:41", 22f, 30f, timeStyle)
            backend.drawText("5G", measuredWidth - 78f, 30f, iconStyle)
            backend.drawText("100%", measuredWidth - 34f, 30f, iconStyle)
        }
    }
}

private class HomeIndicator(width: Float, height: Float) : Component(width = width, height = height) {
    init {
        render { context ->
            val barWidth = 134f
            val barHeight = 5f
            val x = (measuredWidth - barWidth) / 2f
            val y = measuredHeight - 18f
            context.backend.drawRoundedRect(x, y, barWidth, barHeight, barHeight / 2f, Color(255, 255, 255, 180))
        }
    }
}

private class DynamicIsland(
    private val mode: State<IslandMode>,
    width: Float,
    height: Float
) : Component(width = width, height = height) {
    private var transitionProgress = 1f
    private var outgoingMode = IslandMode.Idle
    private var incomingMode = IslandMode.Idle
    private val onModeChange: () -> Unit = {
        val target = mode.value
        if (target != incomingMode || transitionProgress < 0.999f) {
            outgoingMode = incomingMode
            incomingMode = target
            transitionProgress = 0f
        }
        requestRender()
    }

    init {
        outgoingMode = mode.value
        incomingMode = mode.value
        mode.subscribe(onModeChange)
        onDispose { mode.unsubscribe(onModeChange) }

        render { context ->
            val previousTransition = transitionProgress
            transitionProgress = smoothStep(transitionProgress, 1f, context.time.deltaSeconds, 5.8f)
            drawIsland(context)
            if (isAnimating(previousTransition, transitionProgress, 1f)) {
                requestRender()
            }
        }
    }

    private fun drawIsland(context: RenderContext) {
        val compactWidth = 118f
        val compactHeight = 34f
        val expandedWidth = minOf(measuredWidth - 24f, 320f)
        val expandedHeight = 78f

        val expandFromIdle = outgoingMode == IslandMode.Idle && incomingMode != IslandMode.Idle
        val collapseToIdle = outgoingMode != IslandMode.Idle && incomingMode == IslandMode.Idle
        val switchExpanded = outgoingMode != IslandMode.Idle &&
            incomingMode != IslandMode.Idle &&
            outgoingMode != incomingMode

        val expandAmount = when {
            expandFromIdle -> easedPhase(transitionProgress, 0.08f, 0.82f)
            collapseToIdle -> 1f - easedPhase(transitionProgress, 0.28f, 1f)
            else -> 1f
        }
        val squeeze = if (switchExpanded) sin(transitionProgress * PI.toFloat()) * 16f else 0f

        val islandWidth = (lerp(compactWidth, expandedWidth, expandAmount) - squeeze).coerceAtLeast(compactWidth)
        val islandHeight = lerp(compactHeight, expandedHeight, expandAmount)
        val islandRadius = islandHeight / 2f
        val islandX = (measuredWidth - islandWidth) / 2f
        val islandY = 12f

        val backend = context.backend
        val elapsed = context.time.elapsedSeconds
        val transitionBlurBoost = if (transitionProgress < 0.999f) sin(transitionProgress * PI.toFloat()) * 14f else 0f

        backend.drawFrostedRoundedRect(
            islandX,
            islandY,
            islandWidth,
            islandHeight,
            islandRadius,
            blurRadius = 20f + transitionBlurBoost,
            tint = IslandTint,
            stroke = Stroke(IslandStroke, 0.85f)
        )

        val cameraRadius = lerp(5.5f, 0f, expandAmount.coerceIn(0f, 0.65f) / 0.65f)
        if (cameraRadius > 0.2f) {
            val cameraX = islandX + islandWidth - 18f
            val cameraY = islandY + islandHeight / 2f
            backend.drawCircle(cameraX, cameraY, cameraRadius, Color(22, 22, 28, 180))
            backend.drawCircle(cameraX, cameraY, cameraRadius * 0.55f, Color(48, 48, 58, 140))
        }

        val fontFamily = context.theme.typography.fontFamily
        val outgoingAlpha = when {
            collapseToIdle -> 1f - easedPhase(transitionProgress, 0f, 0.38f)
            expandFromIdle -> 0f
            switchExpanded -> 1f - easedPhase(transitionProgress, 0f, 0.48f)
            else -> 0f
        }
        val incomingAlpha = when {
            collapseToIdle -> 0f
            expandFromIdle -> easedPhase(transitionProgress, 0.28f, 0.95f) * expandAmount.coerceIn(0f, 1f)
            switchExpanded -> easedPhase(transitionProgress, 0.48f, 1f)
            else -> if (incomingMode != IslandMode.Idle) 1f else 0f
        }
        val outgoingOffsetY = -10f * easedPhase(transitionProgress, 0f, 0.5f)
        val incomingOffsetY = 10f * (1f - easedPhase(transitionProgress, 0.5f, 1f))

        if (outgoingAlpha > 0.01f && outgoingMode != IslandMode.Idle) {
            drawModeContent(
                backend,
                fontFamily,
                outgoingMode,
                islandX,
                islandY + outgoingOffsetY,
                islandWidth,
                islandHeight,
                outgoingAlpha,
                elapsed
            )
        }
        if (incomingAlpha > 0.01f && incomingMode != IslandMode.Idle) {
            drawModeContent(
                backend,
                fontFamily,
                incomingMode,
                islandX,
                islandY + incomingOffsetY,
                islandWidth,
                islandHeight,
                incomingAlpha,
                elapsed
            )
        }
    }

    private fun drawModeContent(
        backend: dev.unknownuser.ananda.backend.RenderBackend,
        fontFamily: String,
        islandMode: IslandMode,
        islandX: Float,
        islandY: Float,
        islandWidth: Float,
        islandHeight: Float,
        alpha: Float,
        elapsed: Float
    ) {
        when (islandMode) {
            IslandMode.Music -> drawMusicContent(backend, fontFamily, islandX, islandY, islandWidth, islandHeight, alpha, elapsed)
            IslandMode.Timer -> drawTimerContent(backend, fontFamily, islandX, islandY, islandWidth, islandHeight, alpha, elapsed)
            IslandMode.Call -> drawCallContent(backend, fontFamily, islandX, islandY, islandWidth, islandHeight, alpha, elapsed)
            IslandMode.Idle -> Unit
        }
    }

    private fun drawMusicContent(
        backend: dev.unknownuser.ananda.backend.RenderBackend,
        fontFamily: String,
        islandX: Float,
        islandY: Float,
        islandWidth: Float,
        islandHeight: Float,
        alpha: Float,
        elapsed: Float
    ) {
        val artSize = 52f
        val padding = 13f
        val artX = islandX + padding
        val artY = islandY + (islandHeight - artSize) / 2f
        backend.drawRoundedGradientRect(
            artX,
            artY,
            artSize,
            artSize,
            12f,
            listOf(Color(255, 55, 95), Color(175, 82, 222)),
            GradientDirection.DiagonalDown
        )

        val textX = artX + artSize + 12f
        val titleColor = withAlpha(Color.WHITE, (255 * alpha).toInt())
        val mutedColor = withAlpha(Color(180, 180, 186), (255 * alpha).toInt())
        backend.drawText("Blinding Lights", textX, islandY + 30f, TextStyle(15.5f, titleColor, fontFamily))
        backend.drawText("The Weeknd", textX, islandY + 52f, TextStyle(13f, mutedColor, fontFamily))

        val barCount = 5
        val barWidth = 4f
        val barGap = 5f
        val barsWidth = barCount * barWidth + (barCount - 1) * barGap
        var barX = islandX + islandWidth - padding - barsWidth
        val barBaseY = islandY + islandHeight - 18f
        repeat(barCount) { index ->
            val wave = sin(elapsed * 5.5f + index * 1.35f) * 0.5f + 0.5f
            val barHeight = 10f + wave * 22f
            backend.drawRoundedRect(
                barX,
                barBaseY - barHeight,
                barWidth,
                barHeight,
                2f,
                withAlpha(AccentGreen, (255 * alpha).toInt())
            )
            barX += barWidth + barGap
        }
    }

    private fun drawTimerContent(
        backend: dev.unknownuser.ananda.backend.RenderBackend,
        fontFamily: String,
        islandX: Float,
        islandY: Float,
        islandWidth: Float,
        islandHeight: Float,
        alpha: Float,
        elapsed: Float
    ) {
        val padding = 16f
        val iconSize = 46f
        val iconX = islandX + padding
        val iconY = islandY + (islandHeight - iconSize) / 2f
        backend.drawCircle(iconX + iconSize / 2f, iconY + iconSize / 2f, iconSize / 2f, withAlpha(AccentOrange, (40 * alpha).toInt()))
        backend.drawCircle(
            iconX + iconSize / 2f,
            iconY + iconSize / 2f,
            iconSize / 2f - 3f,
            null,
            Stroke(withAlpha(AccentOrange, (255 * alpha).toInt()), 2.5f)
        )
        val handAngle = elapsed * 0.8f
        val cx = iconX + iconSize / 2f
        val cy = iconY + iconSize / 2f
        backend.drawLine(
            cx,
            cy,
            cx + sin(handAngle) * 14f,
            cy - cos(handAngle) * 14f,
            Stroke(withAlpha(AccentOrange, (255 * alpha).toInt()), 2.5f)
        )

        val textX = iconX + iconSize + 14f
        backend.drawText(
            "计时器",
            textX,
            islandY + 28f,
            TextStyle(13f, withAlpha(Color(180, 180, 186), (255 * alpha).toInt()), fontFamily)
        )
        val seconds = (8 * 60 + 24 - elapsed.toInt() % 524).coerceAtLeast(0)
        val minutes = seconds / 60
        val secs = seconds % 60
        backend.drawText(
            "%d:%02d".format(minutes, secs),
            textX,
            islandY + 54f,
            TextStyle(22f, withAlpha(Color.WHITE, (255 * alpha).toInt()), fontFamily)
        )

        val ringX = islandX + islandWidth - padding - 46f
        val ringY = islandY + (islandHeight - 46f) / 2f
        val progress = (elapsed * 0.06f) % 1f
        backend.drawCircle(ringX + 23f, ringY + 23f, 20f, null, Stroke(withAlpha(Color(60, 60, 64), (255 * alpha).toInt()), 3f))
        drawArcSegment(backend, ringX + 23f, ringY + 23f, 20f, -PI.toFloat() / 2f, progress * PI.toFloat() * 2f, AccentOrange, alpha)
    }

    private fun drawCallContent(
        backend: dev.unknownuser.ananda.backend.RenderBackend,
        fontFamily: String,
        islandX: Float,
        islandY: Float,
        islandWidth: Float,
        islandHeight: Float,
        alpha: Float,
        elapsed: Float
    ) {
        val padding = 16f
        val dotX = islandX + padding + 8f
        val dotY = islandY + islandHeight / 2f
        val pulse = sin(elapsed * 4f) * 0.5f + 0.5f
        backend.drawCircle(dotX, dotY, 8f + pulse * 3f, withAlpha(AccentGreen, (36 * alpha).toInt()))
        backend.drawCircle(dotX, dotY, 6f, withAlpha(AccentGreen, (255 * alpha).toInt()))

        val textX = dotX + 18f
        backend.drawText("妈妈", textX, islandY + 30f, TextStyle(16f, withAlpha(Color.WHITE, (255 * alpha).toInt()), fontFamily))
        val callSeconds = 42 + elapsed.toInt() % 3600
        backend.drawText(
            "%d:%02d".format(callSeconds / 60, callSeconds % 60),
            textX,
            islandY + 52f,
            TextStyle(13f, withAlpha(AccentGreen, (255 * alpha).toInt()), fontFamily)
        )

        val buttonWidth = 64f
        val buttonHeight = 34f
        val declineX = islandX + islandWidth - padding - buttonWidth * 2f - 10f
        val acceptX = declineX + buttonWidth + 10f
        val buttonY = islandY + (islandHeight - buttonHeight) / 2f
        backend.drawRoundedRect(declineX, buttonY, buttonWidth, buttonHeight, 17f, withAlpha(Color(255, 69, 58), (255 * alpha).toInt()))
        backend.drawRoundedRect(acceptX, buttonY, buttonWidth, buttonHeight, 17f, withAlpha(AccentGreen, (255 * alpha).toInt()))
        drawCenteredButtonText(backend, "挂断", declineX, buttonY, buttonWidth, buttonHeight, 12f, fontFamily, alpha)
        drawCenteredButtonText(backend, "接听", acceptX, buttonY, buttonWidth, buttonHeight, 12f, fontFamily, alpha)
    }

    private fun drawCenteredButtonText(
        backend: dev.unknownuser.ananda.backend.RenderBackend,
        label: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        size: Float,
        fontFamily: String,
        alpha: Float
    ) {
        val style = TextStyle(size, withAlpha(Color.WHITE, (255 * alpha).toInt()), fontFamily)
        val textWidth = backend.measureText(label, style).first
        backend.drawText(label, x + (width - textWidth) / 2f, textBaselineY(y, height, size), style)
    }

    private fun drawArcSegment(
        backend: dev.unknownuser.ananda.backend.RenderBackend,
        cx: Float,
        cy: Float,
        radius: Float,
        startAngle: Float,
        sweep: Float,
        color: Color,
        alpha: Float
    ) {
        val segments = 24
        val step = sweep / segments
        var angle = startAngle
        repeat(segments) {
            val x1 = cx + cos(angle) * radius
            val y1 = cy + sin(angle) * radius
            angle += step
            val x2 = cx + cos(angle) * radius
            val y2 = cy + sin(angle) * radius
            backend.drawLine(x1, y1, x2, y2, Stroke(withAlpha(color, (255 * alpha).toInt()), 3f))
        }
    }
}

private class ModeControls(
    private val mode: State<IslandMode>,
    width: Float,
    height: Float
) : Component(width = width, height = height) {
    init {
        layout = RowLayout(gap = 8f, crossAxisAlignment = Alignment.Center)
        add(ModeButton("待机", IslandMode.Idle, mode, width = 78f, height = height))
        add(ModeButton("音乐", IslandMode.Music, mode, width = 78f, height = height))
        add(ModeButton("计时", IslandMode.Timer, mode, width = 78f, height = height))
        add(ModeButton("通话", IslandMode.Call, mode, width = 78f, height = height))
    }
}

private class ModeButton(
    private val text: String,
    private val targetMode: IslandMode,
    private val mode: State<IslandMode>,
    width: Float,
    height: Float
) : Component(width = width, height = height) {
    private val invalidate: () -> Unit = { requestRender() }

    init {
        focusable = true
        mode.subscribe(invalidate)
        onDispose { mode.unsubscribe(invalidate) }

        on(PointerDown) {
            requestFocus()
            it.consume()
        }
        on(PointerUp) {
            mode.value = targetMode
            it.consume()
        }
        on(KeyDown.Enter or KeyDown.Space) {
            mode.value = targetMode
            it.consume()
        }

        render { context ->
            val theme = context.theme
            val selected = mode.value == targetMode
            val fill = if (selected) Color(58, 58, 62) else Color(36, 36, 40)
            val border = if (selected) AccentBlue else theme.palette.border
            val textColor = if (selected) Color.WHITE else theme.palette.mutedText
            context.backend.drawRoundedRect(0f, 0f, measuredWidth, measuredHeight, 10f, fill, Stroke(border, 1.2f))
            val textSize = 13f
            val textWidth = context.backend.measureText(text, TextStyle(textSize, textColor, theme.typography.fontFamily)).first
            context.backend.drawText(
                text,
                (measuredWidth - textWidth) / 2f,
                textBaselineY(0f, measuredHeight, textSize),
                TextStyle(textSize, textColor, theme.typography.fontFamily)
            )
        }
    }
}

private class HintLabel(text: String, width: Float, height: Float) : Component(width = width, height = height) {
    init {
        render { context ->
            val style = TextStyle(12f, Color(130, 130, 136), context.theme.typography.fontFamily)
            val textWidth = context.backend.measureText(text, style).first
            context.backend.drawText(text, (measuredWidth - textWidth) / 2f, textBaselineY(0f, measuredHeight, 12f), style)
        }
    }
}

private fun drawWallpaperGradients(
    backend: dev.unknownuser.ananda.backend.RenderBackend,
    width: Float,
    height: Float
) {
    backend.drawRoundedRect(0f, 0f, width, height, 36f, ScreenBackground)
    backend.drawRadialGradientCircle(
        width * 0.28f,
        height * 0.22f,
        width * 0.42f,
        listOf(Color(72, 52, 212, 90), Color(72, 52, 212, 0))
    )
    backend.drawRadialGradientCircle(
        width * 0.78f,
        height * 0.38f,
        width * 0.36f,
        listOf(Color(255, 45, 85, 72), Color(255, 45, 85, 0))
    )
    backend.drawRadialGradientCircle(
        width * 0.52f,
        height * 0.72f,
        width * 0.44f,
        listOf(Color(10, 132, 255, 58), Color(10, 132, 255, 0))
    )
}

private fun easedPhase(progress: Float, start: Float, end: Float): Float =
    Easings.EaseInOutCubic.transform(windowPhase(progress, start, end))

private fun windowPhase(progress: Float, start: Float, end: Float): Float {
    if (progress <= start) return 0f
    if (progress >= end) return 1f
    return (progress - start) / (end - start)
}

private fun smoothStep(current: Float, target: Float, deltaSeconds: Float, speed: Float): Float {
    val step = 1f - exp(-speed * deltaSeconds.coerceIn(0f, 0.1f))
    return current + (target - current) * step
}

private fun isAnimating(previous: Float, current: Float, target: Float): Boolean =
    kotlin.math.abs(current - target) > 0.002f || kotlin.math.abs(current - previous) > 0.0005f

private fun withAlpha(color: Color, alpha: Int): Color =
    Color(color.red, color.green, color.blue, alpha.coerceIn(0, 255))

private fun textBaselineY(y: Float, height: Float, fontSize: Float): Float =
    y + height * 0.5f + fontSize * 0.36f