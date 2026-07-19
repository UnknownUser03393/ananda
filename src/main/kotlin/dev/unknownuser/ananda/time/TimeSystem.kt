package dev.unknownuser.ananda.time

import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

data class TimeFrame(
    val deltaSeconds: Float = 0f,
    val elapsedSeconds: Float = 0f,
    val unscaledDeltaSeconds: Float = 0f,
    val unscaledElapsedSeconds: Float = 0f,
    val frame: Long = 0L,
    val timeScale: Float = 1f,
    val paused: Boolean = false
)

data class TimeUpdateResult(
    val callbacksFired: Boolean,
    val hasActiveDrivers: Boolean
)

fun interface TimeSubscription {
    fun cancel()
}

class TimeSystem {
    private val updateListeners = CopyOnWriteArrayList<TimeListener>()
    private val timers = CopyOnWriteArrayList<TimeTimer>()

    var timeScale: Float = 1f
        set(value) {
            field = max(0f, value)
        }
    var paused: Boolean = false
        private set
    var deltaSeconds: Float = 0f
        private set
    var elapsedSeconds: Float = 0f
        private set
    var unscaledDeltaSeconds: Float = 0f
        private set
    var unscaledElapsedSeconds: Float = 0f
        private set
    var frame: Long = 0L
        private set
    val advancing: Boolean
        get() = !paused && timeScale > 0f

    fun update(deltaSeconds: Float): TimeUpdateResult {
        unscaledDeltaSeconds = max(0f, deltaSeconds)
        this.deltaSeconds = if (paused) 0f else unscaledDeltaSeconds * timeScale
        unscaledElapsedSeconds += unscaledDeltaSeconds
        elapsedSeconds += this.deltaSeconds
        frame += 1

        val frameSnapshot = snapshot()
        var callbacksFired = false

        updateListeners.forEach { listener ->
            if (listener.active) {
                listener.handler(frameSnapshot)
                callbacksFired = true
            }
        }

        timers.forEach { timer ->
            if (timer.tick(frameSnapshot)) {
                callbacksFired = true
            }
            if (!timer.active) {
                timers -= timer
            }
        }

        return TimeUpdateResult(callbacksFired, hasActiveDrivers())
    }

    fun snapshot(): TimeFrame = TimeFrame(
        deltaSeconds = deltaSeconds,
        elapsedSeconds = elapsedSeconds,
        unscaledDeltaSeconds = unscaledDeltaSeconds,
        unscaledElapsedSeconds = unscaledElapsedSeconds,
        frame = frame,
        timeScale = timeScale,
        paused = paused
    )

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
    }

    fun reset() {
        deltaSeconds = 0f
        elapsedSeconds = 0f
        unscaledDeltaSeconds = 0f
        unscaledElapsedSeconds = 0f
        frame = 0L
    }

    fun onUpdate(handler: (TimeFrame) -> Unit): TimeSubscription {
        val listener = TimeListener(handler)
        updateListeners += listener
        return TimeSubscription {
            listener.active = false
            updateListeners -= listener
        }
    }

    fun after(
        delaySeconds: Float,
        useScaledTime: Boolean = true,
        handler: (TimeFrame) -> Unit
    ): TimeSubscription {
        val timer = TimeTimer(
            intervalSeconds = max(0f, delaySeconds),
            repeat = false,
            useScaledTime = useScaledTime,
            handler = handler
        )
        timers += timer
        return timer
    }

    fun every(
        intervalSeconds: Float,
        immediate: Boolean = false,
        useScaledTime: Boolean = true,
        handler: (TimeFrame) -> Unit
    ): TimeSubscription {
        val timer = TimeTimer(
            intervalSeconds = max(0f, intervalSeconds),
            repeat = true,
            useScaledTime = useScaledTime,
            handler = handler
        )
        if (immediate) {
            timer.elapsed = timer.intervalSeconds
        }
        timers += timer
        return timer
    }

    fun clear() {
        updateListeners.clear()
        timers.clear()
    }

    fun hasActiveDrivers(): Boolean =
        updateListeners.any { it.active } ||
            timers.any { it.active && (!it.useScaledTime || advancing) }

    private class TimeListener(
        val handler: (TimeFrame) -> Unit,
        var active: Boolean = true
    )

    private class TimeTimer(
        val intervalSeconds: Float,
        val repeat: Boolean,
        val useScaledTime: Boolean,
        val handler: (TimeFrame) -> Unit
    ) : TimeSubscription {
        var elapsed: Float = 0f
        var active: Boolean = true

        fun tick(frame: TimeFrame): Boolean {
            if (!active) return false
            val delta = if (useScaledTime) frame.deltaSeconds else frame.unscaledDeltaSeconds
            elapsed += delta
            if (elapsed < intervalSeconds) return false

            handler(frame)
            if (repeat) {
                elapsed = if (intervalSeconds <= 0f) 0f else elapsed - intervalSeconds
            } else {
                active = false
            }
            return true
        }

        override fun cancel() {
            active = false
        }
    }
}
