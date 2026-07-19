package dev.unknownuser.ananda.animation

import java.util.concurrent.CopyOnWriteArrayList
import java.awt.Color
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

fun interface Easing {
    fun transform(t: Float): Float
}

object Easings {
    val Linear = Easing { it }
    val EaseIn = Easing { t -> t * t }
    val EaseOut = Easing { t -> 1f - (1f - t) * (1f - t) }
    val EaseInOut = Easing { t -> ((1.0 - cos(t * PI)) / 2.0).toFloat() }
    val EaseInCubic = Easing { t -> t * t * t }
    val EaseOutCubic = Easing { t -> 1f - (1f - t).pow(3) }
    val EaseInOutCubic = Easing { t ->
        if (t < 0.5f) {
            4f * t * t * t
        } else {
            1f - (-2f * t + 2f).pow(3) / 2f
        }
    }
}

enum class AnimationState {
    Pending,
    Delayed,
    Running,
    Paused,
    Finished,
    Cancelled
}

enum class AnimationDirection {
    Forward,
    Reverse
}

enum class RepeatMode {
    Restart,
    Reverse
}

data class AnimationSpec(
    val durationSeconds: Float,
    val delaySeconds: Float = 0f,
    val easing: Easing = Easings.Linear,
    val repeatCount: Int = 0,
    val repeatMode: RepeatMode = RepeatMode.Restart,
    val direction: AnimationDirection = AnimationDirection.Forward
)

data class AnimationFrame(
    val rawProgress: Float,
    val progress: Float,
    val iteration: Int,
    val elapsedSeconds: Float
)

typealias AnimationUpdate = (Float) -> Unit
typealias AnimationFrameUpdate = (AnimationFrame) -> Unit

interface AnimationDriver {
    val active: Boolean
    val running: Boolean
    fun tick(deltaSeconds: Float): Boolean
    fun cancel()
    fun finish()
    fun pause()
    fun resume()
}

fun interface ValueInterpolator<T> {
    /** [progress] is intentionally not clamped so springs can overshoot. */
    fun interpolate(from: T, to: T, progress: Float): T
}

object ValueInterpolators {
    val Float = ValueInterpolator<Float> { from, to, progress -> from + (to - from) * progress }
    val Color = ValueInterpolator<Color> { from, to, progress ->
        Color(
            (from.red + (to.red - from.red) * progress).toInt().coerceIn(0, 255),
            (from.green + (to.green - from.green) * progress).toInt().coerceIn(0, 255),
            (from.blue + (to.blue - from.blue) * progress).toInt().coerceIn(0, 255),
            (from.alpha + (to.alpha - from.alpha) * progress).toInt().coerceIn(0, 255)
        )
    }
}

sealed interface MotionSpec

data class TweenSpec(
    val durationSeconds: Float = 0.25f,
    val easing: Easing = Easings.EaseOutCubic,
    val delaySeconds: Float = 0f
) : MotionSpec

data class SpringSpec(
    val stiffness: Float = 220f,
    val dampingRatio: Float = 0.78f,
    val visibilityThreshold: Float = 0.001f,
    val maxDeltaSeconds: Float = 0.05f
) : MotionSpec

class Animatable<T>(
    initialValue: T,
    internal val interpolator: ValueInterpolator<T>
) {
    var value: T = initialValue
        internal set
    var targetValue: T = initialValue
        internal set
    internal var animation: MotionAnimation<T>? = null

    fun snapTo(value: T) {
        animation?.cancel()
        animation = null
        this.value = value
        targetValue = value
    }

    companion object {
        fun float(initialValue: Float): Animatable<Float> = Animatable(initialValue, ValueInterpolators.Float)
        fun color(initialValue: Color): Animatable<Color> = Animatable(initialValue, ValueInterpolators.Color)
    }
}

fun lerp(start: Float, end: Float, progress: Float): Float =
    start + (end - start) * progress.coerceIn(0f, 1f)

fun lerp(start: Int, end: Int, progress: Float): Int =
    (start + (end - start) * progress.coerceIn(0f, 1f)).toInt()

class Animation(
    val durationSeconds: Float,
    val delaySeconds: Float = 0f,
    val easing: Easing = Easings.Linear,
    val repeat: Boolean = false,
    private val update: AnimationUpdate,
    private val onFinished: () -> Unit = {},
    val repeatCount: Int = if (repeat) RepeatForever else 0,
    val repeatMode: RepeatMode = RepeatMode.Restart,
    val direction: AnimationDirection = AnimationDirection.Forward,
    private val onStarted: () -> Unit = {},
    private val onRepeated: (Int) -> Unit = {},
    private val onCancelled: () -> Unit = {}
) : AnimationDriver {
    private var elapsed = 0f
    private var started = false
    var state: AnimationState = AnimationState.Pending
        private set
    var iteration: Int = 0
        private set
    var rawProgress: Float = 0f
        private set
    var progress: Float = 0f
        private set
    override val running: Boolean
        get() = state == AnimationState.Pending || state == AnimationState.Delayed || state == AnimationState.Running
    override val active: Boolean
        get() = running || state == AnimationState.Paused

    override fun tick(deltaSeconds: Float): Boolean {
        if (state == AnimationState.Finished || state == AnimationState.Cancelled) return true
        if (state == AnimationState.Paused) return false

        elapsed += max(0f, deltaSeconds)
        if (elapsed < delaySeconds) {
            state = AnimationState.Delayed
            return false
        }

        if (!started) {
            started = true
            onStarted()
        }
        state = AnimationState.Running

        val localElapsed = elapsed - delaySeconds
        rawProgress = if (durationSeconds <= 0f) 1f else min(localElapsed / durationSeconds, 1f)
        progress = easing.transform(directedProgress(rawProgress).coerceIn(0f, 1f))
        update(progress)

        if (rawProgress >= 1f) {
            if (shouldRepeat()) {
                iteration += 1
                onRepeated(iteration)
                elapsed = delaySeconds + max(0f, localElapsed - durationSeconds)
                rawProgress = 0f
            } else {
                state = AnimationState.Finished
                onFinished()
                return true
            }
        }
        return false
    }

    override fun pause() {
        if (running) state = AnimationState.Paused
    }

    override fun resume() {
        if (state == AnimationState.Paused) {
            state = if (elapsed < delaySeconds) AnimationState.Delayed else AnimationState.Running
        }
    }

    fun restart() {
        elapsed = 0f
        started = false
        iteration = 0
        rawProgress = 0f
        progress = 0f
        state = AnimationState.Pending
    }

    override fun finish() {
        if (state == AnimationState.Finished || state == AnimationState.Cancelled) return
        rawProgress = 1f
        progress = easing.transform(directedProgress(1f).coerceIn(0f, 1f))
        update(progress)
        state = AnimationState.Finished
        onFinished()
    }

    override fun cancel() {
        if (state == AnimationState.Finished || state == AnimationState.Cancelled) return
        state = AnimationState.Cancelled
        onCancelled()
    }

    fun stop() {
        cancel()
    }

    private fun directedProgress(value: Float): Float {
        val reversedByDirection = direction == AnimationDirection.Reverse
        val reversedByRepeatMode = repeatMode == RepeatMode.Reverse && iteration % 2 == 1
        return if (reversedByDirection.xor(reversedByRepeatMode)) 1f - value else value
    }

    private fun shouldRepeat(): Boolean =
        repeatCount == RepeatForever || iteration < repeatCount

    companion object {
        const val RepeatForever: Int = -1
    }
}

class MotionAnimation<T> internal constructor(
    private val animatable: Animatable<T>,
    private val from: T,
    private val to: T,
    private val spec: MotionSpec,
    private val onFinished: () -> Unit
) : AnimationDriver {
    private var elapsed = 0f
    private var progress = 0f
    private var velocity = 0f
    private var cancelled = false
    private var finished = false
    private var paused = false

    override val active: Boolean get() = !cancelled && !finished
    override val running: Boolean get() = active && !paused

    override fun tick(deltaSeconds: Float): Boolean {
        if (!active) return true
        if (paused) return false
        val done = when (val motion = spec) {
            is TweenSpec -> tickTween(deltaSeconds, motion)
            is SpringSpec -> tickSpring(deltaSeconds, motion)
        }
        animatable.value = animatable.interpolator.interpolate(from, to, progress)
        if (done) finish()
        return done
    }

    override fun cancel() {
        cancelled = true
        if (animatable.animation === this) animatable.animation = null
    }

    override fun pause() {
        if (active) paused = true
    }

    override fun resume() {
        if (active) paused = false
    }

    private fun tickTween(deltaSeconds: Float, spec: TweenSpec): Boolean {
        elapsed += deltaSeconds.coerceAtLeast(0f)
        if (elapsed < spec.delaySeconds) return false
        val raw = if (spec.durationSeconds <= 0f) 1f
        else ((elapsed - spec.delaySeconds) / spec.durationSeconds).coerceIn(0f, 1f)
        progress = spec.easing.transform(raw)
        return raw >= 1f
    }

    private fun tickSpring(deltaSeconds: Float, spec: SpringSpec): Boolean {
        val dt = deltaSeconds.coerceIn(0f, spec.maxDeltaSeconds.coerceAtLeast(0.001f))
        val steps = ceil(dt / StableSpringStep).toInt().coerceAtLeast(1)
        val step = dt / steps
        val stiffness = spec.stiffness.coerceAtLeast(0.01f)
        val damping = 2f * spec.dampingRatio.coerceAtLeast(0f) * sqrt(stiffness)
        repeat(steps) {
            val acceleration = -stiffness * (progress - 1f) - damping * velocity
            velocity += acceleration * step
            progress += velocity * step
        }
        return kotlin.math.abs(1f - progress) <= spec.visibilityThreshold &&
            kotlin.math.abs(velocity) <= spec.visibilityThreshold
    }

    override fun finish() {
        if (!active) return
        progress = 1f
        animatable.value = to
        animatable.targetValue = to
        if (animatable.animation === this) animatable.animation = null
        finished = true
        onFinished()
    }

    private companion object {
        const val StableSpringStep = 1f / 120f
    }
}

class Animator {
    private val animations = CopyOnWriteArrayList<AnimationDriver>()
    val activeAnimations: Int
        get() = animations.count { it.active }

    fun add(animation: Animation): Animation {
        animations += animation
        return animation
    }

    fun <T> animateTo(
        animatable: Animatable<T>,
        targetValue: T,
        spec: MotionSpec = TweenSpec(),
        onFinished: () -> Unit = {}
    ): MotionAnimation<T> {
        animatable.animation?.cancel()
        val animation = MotionAnimation(animatable, animatable.value, targetValue, spec, onFinished)
        animatable.targetValue = targetValue
        animatable.animation = animation
        animations += animation
        return animation
    }

    fun animate(
        durationSeconds: Float,
        delaySeconds: Float = 0f,
        easing: Easing = Easings.Linear,
        repeat: Boolean = false,
        repeatCount: Int = if (repeat) Animation.RepeatForever else 0,
        repeatMode: RepeatMode = RepeatMode.Restart,
        direction: AnimationDirection = AnimationDirection.Forward,
        onFinished: () -> Unit = {},
        update: AnimationUpdate
    ): Animation = add(
        Animation(
            durationSeconds = durationSeconds,
            delaySeconds = delaySeconds,
            easing = easing,
            repeat = repeat,
            update = update,
            onFinished = onFinished,
            repeatCount = repeatCount,
            repeatMode = repeatMode,
            direction = direction
        )
    )

    fun animate(
        spec: AnimationSpec,
        onFinished: () -> Unit = {},
        update: AnimationUpdate
    ): Animation = add(
        Animation(
            durationSeconds = spec.durationSeconds,
            delaySeconds = spec.delaySeconds,
            easing = spec.easing,
            repeat = spec.repeatCount == Animation.RepeatForever,
            update = update,
            onFinished = onFinished,
            repeatCount = spec.repeatCount,
            repeatMode = spec.repeatMode,
            direction = spec.direction
        )
    )

    fun animateFrame(
        spec: AnimationSpec,
        onFinished: () -> Unit = {},
        update: AnimationFrameUpdate
    ): Animation {
        lateinit var animation: Animation
        animation = animate(spec, onFinished) { progress ->
            update(AnimationFrame(animation.rawProgress, progress, animation.iteration, animationElapsed(animation)))
        }
        return animation
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
    ): Animation = animate(durationSeconds, delaySeconds, easing, repeat, repeatCount, repeatMode, direction, onFinished) {
        update(lerp(from, to, it))
    }

    fun update(deltaSeconds: Float) {
        animations.forEach { animation ->
            if (animation.tick(deltaSeconds)) {
                animations -= animation
            }
        }
    }

    fun pauseAll() {
        animations.forEach { it.pause() }
    }

    fun resumeAll() {
        animations.forEach { it.resume() }
    }

    fun cancelAll() {
        animations.forEach { it.cancel() }
        animations.clear()
    }

    fun finishAll() {
        animations.forEach { it.finish() }
        animations.clear()
    }

    fun clear() = animations.clear()

    fun hasActiveAnimations(): Boolean =
        animations.any { it.active }

    fun hasRunningAnimations(): Boolean =
        animations.any { it.running }

    private fun animationElapsed(animation: Animation): Float =
        animation.iteration * animation.durationSeconds + animation.rawProgress * animation.durationSeconds
}
