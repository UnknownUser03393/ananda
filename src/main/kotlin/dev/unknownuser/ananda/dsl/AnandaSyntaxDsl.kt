package dev.unknownuser.ananda.dsl

import java.awt.Color

typealias ComponentBody = CpScope.() -> Unit

fun cp(block: ComponentBody): ComponentDefinition =
    ComponentDefinition(block)

class ComponentDefinition internal constructor(
    internal val body: ComponentBody
) {
    fun renderSyntaxOnly() {
        CpScope().body()
    }
}

@AnandaDsl
class CpScope internal constructor() {
    private val hooks = HookCursor()

    fun <T> useState(initialValue: T): Pair<T, (T) -> Unit> =
        hooks.useState(initialValue)

    fun window(block: WindowScope.() -> Unit): WindowNode =
        WindowNode().also { WindowScope(it).block() }
}

@AnandaDsl
class WindowScope internal constructor(
    private val node: WindowNode
) {
    fun style(block: StyleScope.() -> Unit) {
        node.styles += StyleScope().apply(block).build()
    }

    fun button(text: String, block: ButtonScope.() -> Unit): ButtonNode =
        ButtonNode(text).also {
            node.children += it
            ButtonScope(it).block()
        }

    fun button(text: String): ButtonStyleStage =
        ButtonStyleStage(ButtonNode(text).also { node.children += it })
}

@AnandaDsl
class ButtonScope internal constructor(
    private val node: ButtonNode
) {
    fun style(block: StyleScope.() -> Unit) {
        node.styles += StyleScope().apply(block).build()
    }

    fun onClick(handler: ClickHandler) {
        node.onClick = handler
    }

    fun attr(block: AttributeScope.() -> Unit) {
        node.attributes += AttributeScope().apply(block).build()
    }
}

@AnandaDsl
class StyleScope internal constructor(
    private val state: PseudoState = PseudoState.Base
) {
    private val declarations = mutableListOf<StyleDeclaration>()

    fun background(color: Color) {
        declarations += StyleDeclaration.Background(color, state)
    }

    fun rounded_radius(radius: Length) {
        declarations += StyleDeclaration.RoundedRadius(radius, state)
    }

    fun transition(target: TransitionTarget) {
        declarations += StyleDeclaration.Transition(target, state)
    }

    fun scale(value: Double) {
        declarations += StyleDeclaration.Scale(value, state)
    }

    fun width(value: Length) {
        declarations += StyleDeclaration.Width(value, state)
    }

    fun height(value: Length) {
        declarations += StyleDeclaration.Height(value, state)
    }

    fun hover(block: StyleScope.() -> Unit) {
        declarations += StyleScope(PseudoState.Hover).apply(block).build().declarations
    }

    fun active(block: StyleScope.() -> Unit) {
        declarations += StyleScope(PseudoState.Active).apply(block).build().declarations
    }

    internal fun build(): StyleBlock =
        StyleBlock(declarations.toList())
}

@AnandaDsl
class AttributeScope internal constructor() {
    private val declarations = mutableListOf<AttributeDeclaration>()

    fun width(value: Length) {
        declarations += AttributeDeclaration.Width(value)
    }

    fun height(value: Length) {
        declarations += AttributeDeclaration.Height(value)
    }

    fun id(value: String) {
        declarations += AttributeDeclaration.Id(value)
    }

    internal fun build(): AttributeBlock =
        AttributeBlock(declarations.toList())
}

private class HookCursor {
    private val slots = mutableListOf<Any?>()
    private var index = 0

    fun <T> useState(initialValue: T): Pair<T, (T) -> Unit> {
        val slot = index++
        if (slot == slots.size) slots += initialValue

        @Suppress("UNCHECKED_CAST")
        val value = slots[slot] as T
        val setter: (T) -> Unit = { next -> slots[slot] = next }
        return value to setter
    }
}

sealed class Node

class WindowNode internal constructor(
    internal val styles: MutableList<StyleBlock> = mutableListOf(),
    internal val children: MutableList<Node> = mutableListOf()
) : Node()

class ButtonNode internal constructor(
    val text: String,
    internal val styles: MutableList<StyleBlock> = mutableListOf(),
    internal val attributes: MutableList<AttributeBlock> = mutableListOf(),
    internal var onClick: ClickHandler? = null
) : Node()

fun interface ClickHandler {
    fun invoke()
}

data class StyleBlock internal constructor(
    val declarations: List<StyleDeclaration>
)

sealed class StyleDeclaration {
    data class Background(val color: Color, val state: PseudoState) : StyleDeclaration()
    data class RoundedRadius(val radius: Length, val state: PseudoState) : StyleDeclaration()
    data class Transition(val target: TransitionTarget, val state: PseudoState) : StyleDeclaration()
    data class Scale(val value: Double, val state: PseudoState) : StyleDeclaration()
    data class Width(val value: Length, val state: PseudoState) : StyleDeclaration()
    data class Height(val value: Length, val state: PseudoState) : StyleDeclaration()
}

data class AttributeBlock internal constructor(
    val declarations: List<AttributeDeclaration>
)

sealed class AttributeDeclaration {
    data class Width(val value: Length) : AttributeDeclaration()
    data class Height(val value: Length) : AttributeDeclaration()
    data class Id(val value: String) : AttributeDeclaration()
}

enum class PseudoState {
    Base,
    Hover,
    Active
}

sealed interface TransitionTarget {
    data object All : TransitionTarget
}

val all: TransitionTarget = TransitionTarget.All

data class Length(
    val value: Float,
    val unit: LengthUnit
)

enum class LengthUnit {
    Px,
    Dp,
    Sp
}

val Int.px: Length get() = Length(toFloat(), LengthUnit.Px)
val Float.px: Length get() = Length(this, LengthUnit.Px)
val Double.px: Length get() = Length(toFloat(), LengthUnit.Px)

val Int.dp: Length get() = Length(toFloat(), LengthUnit.Dp)
val Float.dp: Length get() = Length(this, LengthUnit.Dp)
val Double.dp: Length get() = Length(toFloat(), LengthUnit.Dp)

val Int.sp: Length get() = Length(toFloat(), LengthUnit.Sp)
val Float.sp: Length get() = Length(this, LengthUnit.Sp)
val Double.sp: Length get() = Length(toFloat(), LengthUnit.Sp)

val blue: Color = Color(0x2F, 0x80, 0xED)
val darkBlue: Color = Color(0x13, 0x4D, 0x8A)

sealed class DecoratedObjectBuilder protected constructor(
    internal val node: ButtonNode
)

class ButtonStyleStage internal constructor(
    node: ButtonNode
) : DecoratedObjectBuilder(node) {
    fun style(block: StyleScope.() -> Unit): ButtonClickStage {
        node.styles += StyleScope().apply(block).build()
        return ButtonClickStage(node)
    }
}

class ButtonClickStage internal constructor(
    node: ButtonNode
) : DecoratedObjectBuilder(node) {
    fun onClick(handler: ClickHandler): ButtonAttributeStage {
        node.onClick = handler
        return ButtonAttributeStage(node)
    }
}

class ButtonAttributeStage internal constructor(
    node: ButtonNode
) : DecoratedObjectBuilder(node) {
    fun attr(block: AttributeScope.() -> Unit): ButtonDoneStage {
        node.attributes += AttributeScope().apply(block).build()
        return ButtonDoneStage(node)
    }
}

class ButtonDoneStage internal constructor(
    node: ButtonNode
) : DecoratedObjectBuilder(node)

infix fun ButtonStyleStage.withStyle(block: StyleScope.() -> Unit): ButtonClickStage =
    style(block)

infix fun ButtonClickStage.on(handler: ClickHandler): ButtonAttributeStage =
    onClick(handler)

infix fun ButtonAttributeStage.withAttrs(block: AttributeScope.() -> Unit): ButtonDoneStage =
    attr(block)

/*
Usage:

fun MyButton() = cp {
    val (count, setCount) = useState(0)

    window {
        style {
            background(blue)
            transition(all)
        }

        button("Click me: $count") {
            style {
                rounded_radius(8.px)
                hover { background(darkBlue) }
                active { scale(0.95) }
            }
            onClick {
                setCount(count + 1)
            }
        }

        button("Click me")
            .style { background(blue) }
            .onClick { setCount(count + 1) }
            .attr { width(100.px) }
    }
}
*/
