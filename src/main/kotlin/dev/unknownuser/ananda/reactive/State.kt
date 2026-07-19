package dev.unknownuser.ananda.reactive

import java.util.concurrent.CopyOnWriteArraySet

class State<T>(initialValue: T) {
    private val observers = CopyOnWriteArraySet<() -> Unit>()

    var value: T = initialValue
        get() {
            ReactiveRuntime.record(this)
            return field
        }
        set(value) {
            if (field == value) return
            field = value
            observers.forEach { it() }
        }

    fun subscribe(observer: () -> Unit) {
        observers += observer
    }

    fun unsubscribe(observer: () -> Unit) {
        observers -= observer
    }

    override fun toString(): String =
        value.toString()
}

fun <T> stateOf(value: T): State<T> = State(value)

operator fun State<Int>.plusAssign(delta: Int) {
    value += delta
}

operator fun State<Int>.inc(): State<Int>               { value += 1; return this }
operator fun State<Int>.dec(): State<Int>               { value -= 1; return this }
operator fun State<Float>.plusAssign(delta: Float)      { value += delta }
operator fun State<Double>.plusAssign(delta: Double)    { value += delta }

internal object ReactiveRuntime {
    private val activeCollector = ThreadLocal<MutableSet<State<*>>?>()

    fun record(state: State<*>) {
        activeCollector.get()?.add(state)
    }

    fun <T> collect(block: () -> T): Pair<T, Set<State<*>>> {
        val previous = activeCollector.get()
        val current = linkedSetOf<State<*>>()
        activeCollector.set(current)
        return try { block() to current } finally {  activeCollector.set(previous) }
    }
}
