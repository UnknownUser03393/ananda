package dev.unknownuser.ananda.layout

import dev.unknownuser.ananda.component.Component
import kotlin.math.max

data class Size(val width: Float, val height: Float)

data class Insets(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
) {
    val horizontal: Float get() = left + right
    val vertical: Float get() = top + bottom

    companion object {
        fun all(value: Float) = Insets(value, value, value, value)
        fun xy(x: Float, y: Float) = Insets(x, y, x, y)
    }
}

data class Constraints(
    val maxWidth: Float,
    val maxHeight: Float,
    val minWidth: Float = 0f,
    val minHeight: Float = 0f
) {
    fun constrain(size: Size): Size = Size(
        size.width.coerceIn(minWidth, maxWidth),
        size.height.coerceIn(minHeight, maxHeight)
    )

    fun inset(padding: Insets): Constraints =
        Constraints(
            max(0f, maxWidth - padding.left - padding.right),
            max(0f, maxHeight - padding.top - padding.bottom),
            max(0f, minWidth - padding.left - padding.right),
            max(0f, minHeight - padding.top - padding.bottom)
        )

    fun loosen(): Constraints = Constraints(maxWidth, maxHeight)

    companion object {
        fun loose(width: Float, height: Float) = Constraints(width.coerceAtLeast(0f), height.coerceAtLeast(0f))
        fun tight(width: Float, height: Float): Constraints {
            val w = width.coerceAtLeast(0f)
            val h = height.coerceAtLeast(0f)
            return Constraints(w, h, w, h)
        }
    }
}

enum class Alignment {
    Start,
    Center,
    End,
    Stretch
}

enum class MainAxisAlignment {
    Start,
    Center,
    End,
    SpaceBetween,
    SpaceAround,
    SpaceEvenly
}

typealias Arrangement = MainAxisAlignment

enum class WindowSizeClass { Compact, Medium, Expanded }

fun windowSizeClass(width: Float): WindowSizeClass = when {
    width < 600f -> WindowSizeClass.Compact
    width < 840f -> WindowSizeClass.Medium
    else -> WindowSizeClass.Expanded
}

enum class Positioning {
    Flow,
    Absolute,
    Fixed
}

sealed interface GridTrack {
    data class Fixed(val width: Float) : GridTrack
    data class Fraction(val weight: Float = 1f) : GridTrack
    data object MaxContent : GridTrack
    data class MinMax(val minWidth: Float, val max: GridTrack) : GridTrack
}

fun px(value: Number): GridTrack = GridTrack.Fixed(value.toFloat())
fun fr(value: Number = 1f): GridTrack = GridTrack.Fraction(value.toFloat())
fun minmax(minWidth: Number, max: GridTrack): GridTrack = GridTrack.MinMax(minWidth.toFloat(), max)
val maxContent: GridTrack = GridTrack.MaxContent
fun repeat(count: Int, track: GridTrack): List<GridTrack> = List(count.coerceAtLeast(0)) { track }
fun gridAreas(vararg rows: String): List<List<String?>> =
    rows.map { row ->
        row.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .map { it.takeUnless { token -> token == "." } }
    }

interface Layout {
    fun measure(parent: Component, constraints: Constraints): Size {
        val contentConstraints = constraints.inset(parent.padding)
        var width = 0f
        var height = 0f
        parent.children.filter { it.visible && it.positioning == Positioning.Flow }.forEach { child ->
            val measured = child.measure(contentConstraints.inset(child.margin))
            width = max(width, measured.outerWidth(child))
            height = max(height, measured.outerHeight(child))
        }
        return parent.resolveSize(
            constraints,
            width + parent.padding.left + parent.padding.right,
            height + parent.padding.top + parent.padding.bottom
        )
    }

    fun layout(parent: Component, constraints: Constraints)
}

class GridLayout(
    private val columns: List<GridTrack> = listOf(GridTrack.Fraction()),
    private val columnGap: Float = 0f,
    private val rowGap: Float = columnGap,
    private val crossAxisAlignment: Alignment = Alignment.Stretch,
    private val areas: List<List<String?>> = emptyList()
) : Layout {
    override fun measure(parent: Component, constraints: Constraints): Size {
        val flowChildren = parent.children.filter { it.visible && it.positioning == Positioning.Flow }
        val contentConstraints = constraints.inset(parent.padding)
        flowChildren.forEach { it.measure(Constraints(Float.MAX_VALUE, Float.MAX_VALUE)) }
        val columnWidths = resolveColumns(flowChildren, contentConstraints.maxWidth)
        val placements = placements(flowChildren, columnWidths.size)
        val rowHeights = resolveRowHeights(flowChildren, columnWidths, placements)
        val contentWidth = columnWidths.sum() + columnGap * (columnWidths.size - 1).coerceAtLeast(0)
        val contentHeight = rowHeights.sum() + rowGap * (rowHeights.size - 1).coerceAtLeast(0)
        return parent.resolveSize(
            constraints,
            contentWidth + parent.padding.left + parent.padding.right,
            contentHeight + parent.padding.top + parent.padding.bottom
        )
    }

    override fun layout(parent: Component, constraints: Constraints) {
        val flowChildren = parent.children.filter { it.visible && it.positioning == Positioning.Flow }
        val contentWidth = max(0f, constraints.maxWidth - parent.padding.left - parent.padding.right)
        val columnWidths = resolveColumns(flowChildren, contentWidth)
        val placements = placements(flowChildren, columnWidths.size)
        val rowHeights = resolveRowHeights(flowChildren, columnWidths, placements)
        flowChildren.forEachIndexed { index, child ->
            val placement = placements[index]
            val column = placement.column
            val row = placement.row
            val trackWidth = columnWidths[column]
            val rowHeight = rowHeights.getOrElse(row) { child.measuredHeight + child.margin.vertical() }
            if (!child.hasExplicitWidth && crossAxisAlignment == Alignment.Stretch) {
                val layoutWidth = max(0f, trackWidth - child.margin.horizontal())
                child.applyLayoutSize(width = layoutWidth)
                child.measure(Constraints(layoutWidth, max(0f, rowHeight - child.margin.vertical())))
            }
            child.applyMeasuredSize()
            child.x = parent.padding.left + columnWidths.take(column).sum() + columnGap * column + child.margin.left
            child.y = parent.padding.top + rowHeights.take(row).sum() + rowGap * row + when (crossAxisAlignment) {
                Alignment.Center -> (rowHeight - child.measuredHeight - child.margin.vertical()) / 2f + child.margin.top
                Alignment.End -> rowHeight - child.measuredHeight - child.margin.bottom
                else -> child.margin.top
            }
        }
        parent.children.filter { it.positioning != Positioning.Flow }.forEach {
            it.applyMeasuredSize()
            it.placeOutOfFlow(parent.measuredWidth, parent.measuredHeight)
        }
    }

    private fun resolveColumns(children: List<Component>, availableWidth: Float): List<Float> {
        val resolvedTracks = columns.ifEmpty { listOf(GridTrack.Fraction()) }
        val placements = placements(children, resolvedTracks.size)
        val widths = MutableList(resolvedTracks.size) { 0f }
        var fractionWeight = 0f
        resolvedTracks.forEachIndexed { index, track ->
            when (track) {
                is GridTrack.Fixed -> widths[index] = track.width
                is GridTrack.Fraction -> fractionWeight += track.weight.coerceAtLeast(0f)
                GridTrack.MaxContent -> widths[index] = maxContentWidth(children, placements, index)
                is GridTrack.MinMax -> {
                    widths[index] = track.minWidth
                    when (val maxTrack = track.max) {
                        is GridTrack.Fixed -> widths[index] = max(widths[index], maxTrack.width)
                        is GridTrack.Fraction -> fractionWeight += maxTrack.weight.coerceAtLeast(0f)
                        GridTrack.MaxContent -> widths[index] = max(widths[index], maxContentWidth(children, placements, index))
                        is GridTrack.MinMax -> widths[index] = max(widths[index], maxTrack.minWidth)
                    }
                }
            }
        }
        val totalGap = columnGap * (resolvedTracks.size - 1).coerceAtLeast(0)
        val bounded = availableWidth != Float.MAX_VALUE && availableWidth.isFinite()
        val remaining = if (bounded) max(0f, availableWidth - widths.sum() - totalGap) else 0f
        resolvedTracks.forEachIndexed { index, track ->
            val weight = when (track) {
                is GridTrack.Fraction -> track.weight
                is GridTrack.MinMax -> (track.max as? GridTrack.Fraction)?.weight ?: 0f
                else -> 0f
            }.coerceAtLeast(0f)
            if (weight > 0f) {
                widths[index] = if (fractionWeight > 0f && bounded) {
                    widths[index] + remaining * (weight / fractionWeight)
                } else {
                    max(widths[index], maxContentWidth(children, placements, index))
                }
            }
        }
        return widths
    }

    private fun resolveRowHeights(children: List<Component>, columnWidths: List<Float>, placements: List<GridPlacement>): List<Float> {
        if (children.isEmpty()) return emptyList()
        val columnCount = columnWidths.size.coerceAtLeast(1)
        val rowCount = max((children.size + columnCount - 1) / columnCount, placements.maxOfOrNull { it.row + 1 } ?: 0)
        val heights = MutableList(rowCount) { 0f }
        children.forEachIndexed { index, child ->
            val row = placements[index].row
            heights[row] = max(heights[row], child.measuredHeight + child.margin.vertical())
        }
        return heights
    }

    private fun placements(children: List<Component>, columnCount: Int): List<GridPlacement> {
        val areaMap = areaMap()
        val occupied = mutableSetOf<Pair<Int, Int>>()
        return children.mapIndexed { index, child ->
            val explicit = child.gridArea?.let { areaMap[it] }
                ?: child.gridRow?.let { row ->
                    GridPlacement((row - 1).coerceAtLeast(0), ((child.gridColumn ?: 1) - 1).coerceAtLeast(0))
                }
                ?: child.gridColumn?.let { column ->
                    GridPlacement(0, (column - 1).coerceAtLeast(0))
                }
            val placement = explicit ?: nextAutoPlacement(index, columnCount, occupied)
            occupied += placement.row to placement.column
            placement
        }
    }

    private fun nextAutoPlacement(index: Int, columnCount: Int, occupied: Set<Pair<Int, Int>>): GridPlacement {
        var row = index / columnCount
        var column = index % columnCount
        while (row to column in occupied) {
            column++
            if (column >= columnCount) {
                column = 0
                row++
            }
        }
        return GridPlacement(row, column)
    }

    private fun areaMap(): Map<String, GridPlacement> =
        buildMap {
            areas.forEachIndexed { row, rowAreas ->
                rowAreas.forEachIndexed { column, name ->
                    if (name != null && name != ".") putIfAbsent(name, GridPlacement(row, column))
                }
            }
        }

    private fun maxContentWidth(children: List<Component>, placements: List<GridPlacement>, column: Int): Float =
        children
            .filterIndexed { index, _ -> placements[index].column == column }
            .maxOfOrNull { it.measuredWidth + it.margin.horizontal() }
            ?: 0f

    private data class GridPlacement(val row: Int, val column: Int)
}

object StackLayout : Layout {
    override fun measure(parent: Component, constraints: Constraints): Size {
        val contentConstraints = constraints.inset(parent.padding)
        var width = 0f
        var height = 0f
        parent.children.filter { it.visible && it.positioning == Positioning.Flow }.forEach { child ->
            val measured = child.measure(contentConstraints.inset(child.margin))
            width = max(width, child.x + measured.outerWidth(child))
            height = max(height, child.y + measured.outerHeight(child))
        }
        return parent.resolveSize(
            constraints,
            width + parent.padding.left + parent.padding.right,
            height + parent.padding.top + parent.padding.bottom
        )
    }

    override fun layout(parent: Component, constraints: Constraints) {
        parent.children.forEach { child ->
            child.applyMeasuredSize()
            if (child.positioning != Positioning.Flow) {
                child.placeOutOfFlow(parent.measuredWidth, parent.measuredHeight)
            } else if (!child.positionedByUser) {
                child.x = parent.padding.left + child.margin.left
                child.y = parent.padding.top + child.margin.top
            }
        }
    }
}

class WrapLayout(
    private val columnGap: Float = 0f,
    private val rowGap: Float = columnGap,
    private val crossAxisAlignment: Alignment = Alignment.Start
) : Layout {
    override fun measure(parent: Component, constraints: Constraints): Size {
        val contentConstraints = constraints.inset(parent.padding)
        val flowChildren = parent.children.filter { it.visible && it.positioning == Positioning.Flow }
        flowChildren.forEach { child ->
            child.measure(contentConstraints.inset(child.margin))
        }
        val availableWidth = boundedWidth(contentConstraints.maxWidth)
        val lines = resolveLines(flowChildren, availableWidth)
        val contentWidth = lines.maxOfOrNull { it.width } ?: 0f
        val contentHeight = lines.sumOf { it.height.toDouble() }.toFloat() +
            rowGap * (lines.size - 1).coerceAtLeast(0)
        return parent.resolveSize(
            constraints,
            contentWidth + parent.padding.left + parent.padding.right,
            contentHeight + parent.padding.top + parent.padding.bottom
        )
    }

    override fun layout(parent: Component, constraints: Constraints) {
        val flowChildren = parent.children.filter { it.visible && it.positioning == Positioning.Flow }
        val availableWidth = max(0f, constraints.maxWidth - parent.padding.left - parent.padding.right)
        val lines = resolveLines(flowChildren, boundedWidth(availableWidth))
        var cursorY = parent.padding.top
        lines.forEach { line ->
            var cursorX = parent.padding.left
            line.children.forEach { child ->
                child.applyMeasuredSize()
                val outerHeight = child.measuredHeight + child.margin.vertical()
                val alignment = child.layoutAlignment ?: crossAxisAlignment
                child.x = cursorX + child.margin.left
                child.y = cursorY + when (alignment) {
                    Alignment.Center -> (line.height - outerHeight) / 2f + child.margin.top
                    Alignment.End -> line.height - outerHeight + child.margin.top
                    else -> child.margin.top
                }
                cursorX += child.measuredWidth + child.margin.horizontal() + columnGap
            }
            cursorY += line.height + rowGap
        }
        parent.children.filter { it.positioning != Positioning.Flow }.forEach {
            it.applyMeasuredSize()
            it.placeOutOfFlow(parent.measuredWidth, parent.measuredHeight)
        }
    }

    private fun boundedWidth(width: Float): Float =
        if (width.isFinite() && width > 0f) width else Float.MAX_VALUE

    private fun resolveLines(children: List<Component>, availableWidth: Float): List<WrapLine> {
        if (children.isEmpty()) return emptyList()
        val lines = mutableListOf<WrapLine>()
        var currentChildren = mutableListOf<Component>()
        var currentWidth = 0f
        var currentHeight = 0f
        children.forEach { child ->
            val childWidth = child.measuredWidth + child.margin.horizontal()
            val childHeight = child.measuredHeight + child.margin.vertical()
            val nextWidth = if (currentChildren.isEmpty()) childWidth else currentWidth + columnGap + childWidth
            if (currentChildren.isNotEmpty() && nextWidth > availableWidth) {
                lines += WrapLine(currentChildren, currentWidth, currentHeight)
                currentChildren = mutableListOf()
                currentWidth = 0f
                currentHeight = 0f
            }
            currentChildren += child
            currentWidth = if (currentChildren.size == 1) childWidth else currentWidth + columnGap + childWidth
            currentHeight = max(currentHeight, childHeight)
        }
        lines += WrapLine(currentChildren, currentWidth, currentHeight)
        return lines
    }

    private data class WrapLine(val children: List<Component>, val width: Float, val height: Float)
}

class RowLayout(
    private val gap: Float = 0f,
    private val crossAxisAlignment: Alignment = Alignment.Start,
    private val mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start
) : Layout {
    override fun measure(parent: Component, constraints: Constraints): Size {
        val contentConstraints = constraints.inset(parent.padding)
        val flowChildren = parent.children.filter { it.visible && it.positioning == Positioning.Flow }
        var width = 0f
        var height = 0f
        flowChildren.forEachIndexed { index, child ->
            val measured = if (child.weight > 0f) {
                Size(0f, child.measure(contentConstraints.inset(child.margin)).height)
            } else {
                child.measure(contentConstraints.inset(child.margin))
            }
            width += measured.outerWidth(child)
            if (index > 0) width += gap
            height = max(height, measured.outerHeight(child))
        }
        return parent.resolveSize(
            constraints,
            width + parent.padding.left + parent.padding.right,
            height + parent.padding.top + parent.padding.bottom
        )
    }

    override fun layout(parent: Component, constraints: Constraints) {
        val flowChildren = parent.children.filter { it.visible && it.positioning == Positioning.Flow }
        val availableHeight = max(0f, constraints.maxHeight - parent.padding.top - parent.padding.bottom)
        val availableWidth = max(0f, constraints.maxWidth - parent.padding.left - parent.padding.right)
        val totalGap = gap * (flowChildren.size - 1).coerceAtLeast(0)
        val fixedWidth = flowChildren.sumOf {
            if (it.weight > 0f) it.margin.horizontal().toDouble() else (it.measuredWidth + it.margin.horizontal()).toDouble()
        }.toFloat()
        val totalWeight = flowChildren.sumOf { it.weight.coerceAtLeast(0f).toDouble() }.toFloat()
        val weightedWidth = max(0f, availableWidth - fixedWidth - totalGap)
        val extraWidth = max(0f, availableWidth - fixedWidth - totalGap - if (totalWeight > 0f) weightedWidth else 0f)
        val dynamicGap = when (mainAxisAlignment) {
            MainAxisAlignment.SpaceBetween -> if (flowChildren.size > 1) gap + extraWidth / (flowChildren.size - 1) else gap
            MainAxisAlignment.SpaceAround -> if (flowChildren.isNotEmpty()) gap + extraWidth / flowChildren.size else gap
            MainAxisAlignment.SpaceEvenly -> if (flowChildren.isNotEmpty()) gap + extraWidth / (flowChildren.size + 1) else gap
            else -> gap
        }
        var cursor = parent.padding.left + when (mainAxisAlignment) {
            MainAxisAlignment.Center -> extraWidth / 2f
            MainAxisAlignment.End -> extraWidth
            MainAxisAlignment.SpaceAround -> if (flowChildren.isNotEmpty()) (dynamicGap - gap) / 2f else 0f
            MainAxisAlignment.SpaceEvenly -> if (flowChildren.isNotEmpty()) dynamicGap - gap else 0f
            else -> 0f
        }
        flowChildren.forEach { child ->
            if (child.weight > 0f && totalWeight > 0f) {
                val layoutWidth = weightedWidth * (child.weight / totalWeight)
                child.applyLayoutSize(width = layoutWidth)
                child.measure(Constraints(layoutWidth, max(0f, availableHeight - child.margin.vertical())))
            }
            child.applyMeasuredSize()
            val alignment = child.layoutAlignment ?: crossAxisAlignment
            if (alignment == Alignment.Stretch && !child.hasExplicitHeight) {
                child.applyLayoutSize(height = max(0f, availableHeight - child.margin.vertical()))
            }
            layoutChildSubtree(child)
            val outerHeight = child.measuredHeight + child.margin.vertical()
            child.x = cursor + child.margin.left
            child.y = when (alignment) {
                Alignment.Center -> parent.padding.top + (availableHeight - outerHeight) / 2f + child.margin.top
                Alignment.End -> parent.padding.top + availableHeight - outerHeight + child.margin.top
                else -> parent.padding.top + child.margin.top
            }
            cursor += child.measuredWidth + child.margin.horizontal() + dynamicGap
        }
        parent.children.filter { it.positioning != Positioning.Flow }.forEach {
            it.applyMeasuredSize()
            it.placeOutOfFlow(parent.measuredWidth, parent.measuredHeight)
        }
    }
}

class ColumnLayout(
    private val gap: Float = 0f,
    private val crossAxisAlignment: Alignment = Alignment.Start,
    private val mainAxisAlignment: MainAxisAlignment = MainAxisAlignment.Start
) : Layout {
    override fun measure(parent: Component, constraints: Constraints): Size {
        val contentConstraints = constraints.inset(parent.padding)
        val flowChildren = parent.children.filter { it.visible && it.positioning == Positioning.Flow }
        var width = 0f
        var height = 0f
        flowChildren.forEachIndexed { index, child ->
            val measured = if (child.weight > 0f) {
                Size(child.measure(contentConstraints.inset(child.margin)).width, 0f)
            } else {
                child.measure(contentConstraints.inset(child.margin))
            }
            width = max(width, measured.outerWidth(child))
            height += measured.outerHeight(child)
            if (index > 0) height += gap
        }
        return parent.resolveSize(
            constraints,
            width + parent.padding.left + parent.padding.right,
            height + parent.padding.top + parent.padding.bottom
        )
    }

    override fun layout(parent: Component, constraints: Constraints) {
        val flowChildren = parent.children.filter { it.visible && it.positioning == Positioning.Flow }
        val availableWidth = max(0f, constraints.maxWidth - parent.padding.left - parent.padding.right)
        val availableHeight = max(0f, constraints.maxHeight - parent.padding.top - parent.padding.bottom)
        val totalGap = gap * (flowChildren.size - 1).coerceAtLeast(0)
        val fixedHeight = flowChildren.sumOf {
            if (it.weight > 0f) it.margin.vertical().toDouble() else (it.measuredHeight + it.margin.vertical()).toDouble()
        }.toFloat()
        val totalWeight = flowChildren.sumOf { it.weight.coerceAtLeast(0f).toDouble() }.toFloat()
        val weightedHeight = max(0f, availableHeight - fixedHeight - totalGap)
        val extraHeight = max(0f, availableHeight - fixedHeight - totalGap - if (totalWeight > 0f) weightedHeight else 0f)
        val dynamicGap = when (mainAxisAlignment) {
            MainAxisAlignment.SpaceBetween -> if (flowChildren.size > 1) gap + extraHeight / (flowChildren.size - 1) else gap
            MainAxisAlignment.SpaceAround -> if (flowChildren.isNotEmpty()) gap + extraHeight / flowChildren.size else gap
            MainAxisAlignment.SpaceEvenly -> if (flowChildren.isNotEmpty()) gap + extraHeight / (flowChildren.size + 1) else gap
            else -> gap
        }
        var cursor = parent.padding.top + when (mainAxisAlignment) {
            MainAxisAlignment.Center -> extraHeight / 2f
            MainAxisAlignment.End -> extraHeight
            MainAxisAlignment.SpaceAround -> if (flowChildren.isNotEmpty()) (dynamicGap - gap) / 2f else 0f
            MainAxisAlignment.SpaceEvenly -> if (flowChildren.isNotEmpty()) dynamicGap - gap else 0f
            else -> 0f
        }
        flowChildren.forEach { child ->
            if (child.weight > 0f && totalWeight > 0f) {
                val layoutHeight = weightedHeight * (child.weight / totalWeight)
                child.applyLayoutSize(height = layoutHeight)
                child.measure(Constraints(max(0f, availableWidth - child.margin.horizontal()), layoutHeight))
            }
            child.applyMeasuredSize()
            val alignment = child.layoutAlignment ?: crossAxisAlignment
            if (alignment == Alignment.Stretch && !child.hasExplicitWidth) {
                child.applyLayoutSize(width = max(0f, availableWidth - child.margin.horizontal()))
            }
            layoutChildSubtree(child)
            val outerWidth = child.measuredWidth + child.margin.horizontal()
            child.y = cursor + child.margin.top
            child.x = when (alignment) {
                Alignment.Center -> parent.padding.left + (availableWidth - outerWidth) / 2f + child.margin.left
                Alignment.End -> parent.padding.left + availableWidth - outerWidth + child.margin.left
                else -> parent.padding.left + child.margin.left
            }
            cursor += child.measuredHeight + child.margin.vertical() + dynamicGap
        }
        parent.children.filter { it.positioning != Positioning.Flow }.forEach {
            it.applyMeasuredSize()
            it.placeOutOfFlow(parent.measuredWidth, parent.measuredHeight)
        }
    }
}

class FlowRowLayout(
    horizontalGap: Float = 8f,
    verticalGap: Float = 8f,
    alignment: Alignment = Alignment.Center
) : Layout {
    private val delegate = WrapLayout(horizontalGap, verticalGap, alignment)
    override fun measure(parent: Component, constraints: Constraints): Size = delegate.measure(parent, constraints)
    override fun layout(parent: Component, constraints: Constraints) = delegate.layout(parent, constraints)
}

class BoxLayout(
    private val horizontalAlignment: Alignment = Alignment.Start,
    private val verticalAlignment: Alignment = Alignment.Start
) : Layout {
    override fun measure(parent: Component, constraints: Constraints): Size = StackLayout.measure(parent, constraints)

    override fun layout(parent: Component, constraints: Constraints) {
        val availableWidth = max(0f, parent.measuredWidth - parent.padding.horizontal)
        val availableHeight = max(0f, parent.measuredHeight - parent.padding.vertical)
        parent.children.filter { it.visible && it.positioning == Positioning.Flow }.forEach { child ->
            val horizontal = child.layoutAlignment ?: horizontalAlignment
            val vertical = child.layoutAlignment ?: verticalAlignment
            if (horizontal == Alignment.Stretch && !child.hasExplicitWidth) {
                child.applyLayoutSize(width = max(0f, availableWidth - child.margin.horizontal))
            }
            if (vertical == Alignment.Stretch && !child.hasExplicitHeight) {
                child.applyLayoutSize(height = max(0f, availableHeight - child.margin.vertical))
            }
            child.applyMeasuredSize()
            child.x = parent.padding.left + when (horizontal) {
                Alignment.Center -> (availableWidth - child.measuredWidth - child.margin.horizontal) / 2f + child.margin.left
                Alignment.End -> availableWidth - child.measuredWidth - child.margin.right
                else -> child.margin.left
            }
            child.y = parent.padding.top + when (vertical) {
                Alignment.Center -> (availableHeight - child.measuredHeight - child.margin.vertical) / 2f + child.margin.top
                Alignment.End -> availableHeight - child.measuredHeight - child.margin.bottom
                else -> child.margin.top
            }
        }
        parent.children.filter { it.positioning != Positioning.Flow }.forEach {
            it.applyMeasuredSize()
            it.placeOutOfFlow(parent.measuredWidth, parent.measuredHeight)
        }
    }
}

class AdaptiveGridLayout(
    private val minCellWidth: Float = 160f,
    private val horizontalGap: Float = 12f,
    private val verticalGap: Float = 12f,
    private val maxColumns: Int = Int.MAX_VALUE
) : Layout {
    override fun measure(parent: Component, constraints: Constraints): Size {
        val content = constraints.inset(parent.padding)
        val children = parent.children.filter { it.visible && it.positioning == Positioning.Flow }
        if (children.isEmpty()) return parent.resolveSize(constraints, parent.padding.horizontal, parent.padding.vertical)
        val columns = columnCount(content.maxWidth, children.size)
        val cellWidth = max(0f, (content.maxWidth - horizontalGap * (columns - 1)) / columns)
        var height = 0f
        children.chunked(columns).forEachIndexed { rowIndex, row ->
            val rowHeight = row.maxOf { child ->
                val childWidth = max(0f, cellWidth - child.margin.horizontal)
                if (!child.hasExplicitWidth) child.applyLayoutSize(width = childWidth)
                child.measure(Constraints(childWidth, content.maxHeight)).height + child.margin.vertical
            }
            height += rowHeight
            if (rowIndex > 0) height += verticalGap
        }
        return parent.resolveSize(constraints, content.maxWidth + parent.padding.horizontal, height + parent.padding.vertical)
    }

    override fun layout(parent: Component, constraints: Constraints) {
        val children = parent.children.filter { it.visible && it.positioning == Positioning.Flow }
        if (children.isEmpty()) return
        val available = max(0f, parent.measuredWidth - parent.padding.horizontal)
        val columns = columnCount(available, children.size)
        val cellWidth = max(0f, (available - horizontalGap * (columns - 1)) / columns)
        var y = parent.padding.top
        children.chunked(columns).forEach { row ->
            val rowHeight = row.maxOf { it.measuredHeight + it.margin.vertical }
            row.forEachIndexed { column, child ->
                child.applyMeasuredSize()
                child.x = parent.padding.left + column * (cellWidth + horizontalGap) + child.margin.left
                child.y = y + child.margin.top
            }
            y += rowHeight + verticalGap
        }
    }

    private fun columnCount(width: Float, itemCount: Int): Int =
        kotlin.math.floor((width + horizontalGap) / (minCellWidth + horizontalGap)).toInt()
            .coerceIn(1, minOf(maxColumns, itemCount).coerceAtLeast(1))
}

fun Component.placeOutOfFlow(parentWidth: Float, parentHeight: Float) {
    val resolvedLeft = left ?: leftPercent?.times(parentWidth)
    val resolvedTop = top ?: topPercent?.times(parentHeight)
    val resolvedRight = right ?: rightPercent?.let { parentWidth * it }
    val resolvedBottom = bottom ?: bottomPercent?.let { parentHeight * it }
    resolvedLeft?.let { x = it + margin.left }
    resolvedTop?.let { y = it + margin.top }
    if (resolvedLeft == null && resolvedRight != null) {
        x = parentWidth - resolvedRight - measuredWidth - margin.right
    }
    if (resolvedTop == null && resolvedBottom != null) {
        y = parentHeight - resolvedBottom - measuredHeight - margin.bottom
    }
    if (resolvedLeft != null && resolvedRight != null && !hasExplicitWidth) {
        val layoutWidth = max(0f, parentWidth - resolvedLeft - resolvedRight - margin.horizontal())
        applyLayoutSize(width = layoutWidth)
        measure(Constraints(layoutWidth, measuredHeight.takeIf { it > 0f } ?: parentHeight))
    }
    if (resolvedTop != null && resolvedBottom != null && !hasExplicitHeight) {
        val layoutHeight = max(0f, parentHeight - resolvedTop - resolvedBottom - margin.vertical())
        applyLayoutSize(height = layoutHeight)
        measure(Constraints(measuredWidth.takeIf { it > 0f } ?: parentWidth, layoutHeight))
    }
}

private fun layoutChildSubtree(child: Component) {
    val width = child.layoutWidthOrMeasured()
    val height = child.layoutHeightOrMeasured()
    if (width <= 0f && height <= 0f) return
    child.measure(
        Constraints(
            if (width > 0f) width else Float.MAX_VALUE,
            if (height > 0f) height else Float.MAX_VALUE
        )
    )
    child.layout.layout(child, Constraints(child.measuredWidth, child.measuredHeight))
}

private fun Size.outerWidth(component: Component): Float =
    width + component.margin.horizontal()

private fun Size.outerHeight(component: Component): Float =
    height + component.margin.vertical()

private fun Insets.horizontal(): Float =
    left + right

private fun Insets.vertical(): Float =
    top + bottom
