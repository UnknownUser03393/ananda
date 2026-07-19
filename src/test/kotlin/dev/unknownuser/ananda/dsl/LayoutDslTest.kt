package dev.unknownuser.ananda.dsl

import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.layout.AdaptiveGridLayout
import dev.unknownuser.ananda.layout.Alignment
import dev.unknownuser.ananda.layout.ColumnLayout
import dev.unknownuser.ananda.layout.Constraints
import dev.unknownuser.ananda.layout.RowLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LayoutDslTest {
    @Test
    fun `nested row exposes sizing modifiers to every child`() {
        val root = Component()
        val row = ComponentBuilder(root).row(gap = 10, crossAxisAlignment = Alignment.Center) {
            size(300, 100)
            padding(10)
            component(width = 50, height = 20)
            component(height = 20) {
                weight(1)
                minSize(width = 40)
            }
        }

        row.measure(Constraints.tight(300f, 100f))
        row.layout.layout(row, Constraints.tight(row.measuredWidth, row.measuredHeight))

        assertIs<RowLayout>(row.layout)
        assertEquals(2, row.children.size)
        assertEquals(220f, row.children[1].measuredWidth)
        assertEquals(1f, row.children[1].weight)
        assertEquals(70f, row.children[1].x)
    }

    @Test
    fun `containers can be nested without manually creating components`() {
        val root = Component(width = 360f, height = 240f)
        val column = ComponentBuilder(root).column(gap = 12) {
            fillMaxSize()
            row(gap = 8) {
                fillMaxWidth()
                height(40)
                component(width = 80, height = 24)
                component(height = 24) { weight(1) }
            }
            adaptiveGrid(minCellWidth = 100, maxColumns = 3) {
                fillMaxWidth()
                repeat(4) { component(height = 24) }
            }
        }

        assertIs<ColumnLayout>(column.layout)
        assertIs<RowLayout>(column.children[0].layout)
        assertIs<AdaptiveGridLayout>(column.children[1].layout)
        assertTrue(column.fillWidth)
        assertTrue(column.fillHeight)
        assertTrue(column.children[0].fillWidth)
        assertEquals(4, column.children[1].children.size)
    }

    @Test
    fun `layout setters remain compatible and do not create a child`() {
        val component = Component()

        ComponentBuilder(component).apply {
            row(gap = 6)
            weight(2)
            fillMaxWidth()
        }

        assertIs<RowLayout>(component.layout)
        assertEquals(2f, component.weight)
        assertTrue(component.fillWidth)
        assertTrue(component.children.isEmpty())
    }
}
