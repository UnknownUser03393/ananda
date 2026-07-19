package dev.unknownuser.ananda.settings

import java.awt.Color
import kotlin.reflect.KProperty

typealias SettingVisibility = () -> Boolean

sealed class SettingNode(
    val name: String,
    val parent: SettingGroup?,
    private val visibleWhen: SettingVisibility
) {
    val key: String = parent?.let { "${it.key}.$name" } ?: name

    val isVisible: Boolean
        get() = visibleWhen()
}

class SettingGroup(
    name: String,
    parent: SettingGroup? = null,
    visibleWhen: SettingVisibility = { true }
) : SettingNode(name, parent, visibleWhen) {
    val groups: MutableList<SettingGroup> = mutableListOf()
    val settings: MutableList<Setting<*>> = mutableListOf()

    fun addGroup(group: SettingGroup) {
        groups += group
    }

    fun addSetting(setting: Setting<*>) {
        settings += setting
    }
}

sealed class Setting<T>(
    name: String,
    parent: SettingGroup,
    defaultValue: T,
    visibleWhen: SettingVisibility = { true }
) : SettingNode(name, parent, visibleWhen) {
    private val listeners = mutableSetOf<() -> Unit>()

    open var value: T = defaultValue
        protected set

    val defaultValue: T = defaultValue

    fun set(newValue: T) {
        val normalized = normalize(newValue)
        if (value == normalized) return
        value = normalized
        listeners.toList().forEach { it() }
    }

    fun subscribe(listener: () -> Unit) {
        listeners += listener
    }

    fun unsubscribe(listener: () -> Unit) {
        listeners -= listener
    }

    protected open fun normalize(newValue: T): T = newValue
}

class BooleanSetting(
    name: String,
    parent: SettingGroup,
    defaultValue: Boolean,
    visibleWhen: SettingVisibility = { true }
) : Setting<Boolean>(name, parent, defaultValue, visibleWhen)

class IntegerSetting(
    name: String,
    parent: SettingGroup,
    defaultValue: Int,
    val range: IntRange,
    visibleWhen: SettingVisibility = { true }
) : Setting<Int>(name, parent, defaultValue.coerceIn(range), visibleWhen) {
    override fun normalize(newValue: Int): Int = newValue.coerceIn(range)
}

class FloatSetting(
    name: String,
    parent: SettingGroup,
    defaultValue: Float,
    val range: ClosedFloatingPointRange<Float>,
    visibleWhen: SettingVisibility = { true }
) : Setting<Float>(name, parent, defaultValue.coerceIn(range), visibleWhen) {
    override fun normalize(newValue: Float): Float = newValue.coerceIn(range)
}

class StringSetting(
    name: String,
    parent: SettingGroup,
    defaultValue: String,
    visibleWhen: SettingVisibility = { true }
) : Setting<String>(name, parent, defaultValue, visibleWhen)

class ColorSetting(
    name: String,
    parent: SettingGroup,
    defaultValue: Color,
    visibleWhen: SettingVisibility = { true }
) : Setting<Color>(name, parent, defaultValue, visibleWhen)

class KeyBindSetting(
    name: String,
    parent: SettingGroup,
    defaultValue: Int?,
    visibleWhen: SettingVisibility = { true }
) : Setting<Int?>(name, parent, defaultValue, visibleWhen)

class EnumSetting<T : Enum<T>>(
    name: String,
    parent: SettingGroup,
    defaultValue: T,
    val options: List<T>,
    visibleWhen: SettingVisibility = { true }
) : Setting<T>(name, parent, defaultValue, visibleWhen)

class SettingDelegate<T>(
    val setting: Setting<T>
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = setting.value

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        setting.set(newValue)
    }
}

class GroupDelegate(
    private val name: String,
    private val parent: SettingGroup,
    private val visibleWhen: SettingVisibility
) {
    private var group: SettingGroup? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): SettingGroup =
        group ?: SettingGroup(name, parent, visibleWhen).also {
            parent.addGroup(it)
            group = it
        }
}

open class SettingsOwner(rootName: String) {
    val settingsRoot = SettingGroup(rootName)

    protected fun group(name: String, visibleWhen: SettingVisibility = { true }): GroupDelegate =
        settingsRoot.group(name, visibleWhen)

    protected fun bool(name: String, defaultValue: Boolean, visibleWhen: SettingVisibility = { true }): SettingDelegate<Boolean> =
        settingsRoot.bool(name, defaultValue, visibleWhen)

    protected fun int(name: String, defaultValue: Int, range: IntRange, visibleWhen: SettingVisibility = { true }): SettingDelegate<Int> =
        settingsRoot.int(name, defaultValue, range, visibleWhen)

    protected fun float(name: String, defaultValue: Float, range: ClosedFloatingPointRange<Float>, visibleWhen: SettingVisibility = { true }): SettingDelegate<Float> =
        settingsRoot.float(name, defaultValue, range, visibleWhen)

    protected fun string(name: String, defaultValue: String, visibleWhen: SettingVisibility = { true }): SettingDelegate<String> =
        settingsRoot.string(name, defaultValue, visibleWhen)

    protected fun color(name: String, defaultValue: Color, visibleWhen: SettingVisibility = { true }): SettingDelegate<Color> =
        settingsRoot.color(name, defaultValue, visibleWhen)

    protected fun keyBind(name: String, defaultValue: Int? = null, visibleWhen: SettingVisibility = { true }): SettingDelegate<Int?> =
        settingsRoot.keyBind(name, defaultValue, visibleWhen)

    protected inline fun <reified T : Enum<T>> enum(name: String, defaultValue: T, noinline visibleWhen: SettingVisibility = { true }): SettingDelegate<T> =
        settingsRoot.enum(name, defaultValue, visibleWhen)
}

fun SettingGroup.group(name: String, visibleWhen: SettingVisibility = { true }): GroupDelegate =
    GroupDelegate(name, this, visibleWhen)

fun SettingGroup.bool(name: String, defaultValue: Boolean, visibleWhen: SettingVisibility = { true }): SettingDelegate<Boolean> =
    SettingDelegate(BooleanSetting(name, this, defaultValue, visibleWhen).also(::addSetting))

fun SettingGroup.int(name: String, defaultValue: Int, range: IntRange, visibleWhen: SettingVisibility = { true }): SettingDelegate<Int> =
    SettingDelegate(IntegerSetting(name, this, defaultValue, range, visibleWhen).also(::addSetting))

fun SettingGroup.float(name: String, defaultValue: Float, range: ClosedFloatingPointRange<Float>, visibleWhen: SettingVisibility = { true }): SettingDelegate<Float> =
    SettingDelegate(FloatSetting(name, this, defaultValue, range, visibleWhen).also(::addSetting))

fun SettingGroup.string(name: String, defaultValue: String, visibleWhen: SettingVisibility = { true }): SettingDelegate<String> =
    SettingDelegate(StringSetting(name, this, defaultValue, visibleWhen).also(::addSetting))

fun SettingGroup.color(name: String, defaultValue: Color, visibleWhen: SettingVisibility = { true }): SettingDelegate<Color> =
    SettingDelegate(ColorSetting(name, this, defaultValue, visibleWhen).also(::addSetting))

fun SettingGroup.keyBind(name: String, defaultValue: Int? = null, visibleWhen: SettingVisibility = { true }): SettingDelegate<Int?> =
    SettingDelegate(KeyBindSetting(name, this, defaultValue, visibleWhen).also(::addSetting))

inline fun <reified T : Enum<T>> SettingGroup.enum(name: String, defaultValue: T, noinline visibleWhen: SettingVisibility = { true }): SettingDelegate<T> =
    SettingDelegate(EnumSetting(name, this, defaultValue, enumValues<T>().toList(), visibleWhen).also(::addSetting))
