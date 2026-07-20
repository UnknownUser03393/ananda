package dev.unknownuser.ananda.material3

import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.RenderBackend
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextStyle
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.event.ImeCommit
import dev.unknownuser.ananda.event.ImeCompose
import dev.unknownuser.ananda.event.Blur
import dev.unknownuser.ananda.event.KeyDown
import dev.unknownuser.ananda.event.KeyEvent
import dev.unknownuser.ananda.event.PointerDown
import dev.unknownuser.ananda.event.PointerMove
import dev.unknownuser.ananda.event.PointerUp
import dev.unknownuser.ananda.event.TextInput
import dev.unknownuser.ananda.event.or
import dev.unknownuser.ananda.layout.Insets
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.stateOf
import java.awt.Color
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.event.InputEvent
import java.awt.event.KeyEvent as AwtKeyEvent
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

class MaterialDatePicker(
    val selectedDate: State<String> = stateOf(""),
    val visibleState: State<Boolean> = stateOf(false),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 320f,
    var title: String = "Select date",
    private val onConfirm: (String) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = {
        visible = visibleState.value
        requestRender()
    }
    private var hoverDay = -1
    private var pressedDay = -1
    private var draftYear = yearFrom(selectedDate.value)
    private var draftMonth = monthFrom(selectedDate.value)
    private var draftDay = dayFrom(selectedDate.value)

    init {
        focusable = true
        visible = visibleState.value
        visibleState.subscribe(invalidate)
        selectedDate.subscribe {
            draftYear = yearFrom(selectedDate.value)
            draftMonth = monthFrom(selectedDate.value)
            draftDay = dayFrom(selectedDate.value)
            requestRender()
        }
        onDispose {
            visibleState.unsubscribe(invalidate)
        }
        on(PointerDown) {
            if (!visible) return@on
            requestFocus()
            pressedDay = dayAt(it.x - x, it.y - y)
            it.consume()
        }
        on(PointerMove) {
            if (!visible) return@on
            hoverDay = dayAt(it.x - x, it.y - y)
            requestRender()
        }
        on(PointerUp) {
            if (!visible) return@on
            val localX = it.x - x
            val localY = it.y - y
            when {
                localY in 44f..84f && localX in 56f..96f -> shiftMonth(-1)
                localY in 44f..84f && localX in (measuredWidth - 96f)..(measuredWidth - 56f) -> shiftMonth(1)
                else -> {
                    val releasedDay = dayAt(localX, localY)
                    if (releasedDay in 1..daysInMonth(draftYear, draftMonth) && releasedDay == pressedDay) {
                        draftDay = releasedDay
                        selectedDate.value = formatDate(draftYear, draftMonth, draftDay)
                        onConfirm(selectedDate.value)
                        visibleState.value = false
                    }
                }
            }
            pressedDay = -1
            it.consume()
        }
        onKey("keyDown") {
            if (!visible) return@onKey
            when (it.keyCode) {
                AwtKeyEvent.VK_ESCAPE -> {
                    visibleState.value = false
                    it.consume()
                }
                AwtKeyEvent.VK_LEFT -> {
                    draftDay = (draftDay - 1).coerceAtLeast(1)
                    requestRender()
                    it.consume()
                }
                AwtKeyEvent.VK_RIGHT -> {
                    draftDay = (draftDay + 1).coerceAtMost(daysInMonth(draftYear, draftMonth))
                    requestRender()
                    it.consume()
                }
                AwtKeyEvent.VK_UP -> {
                    shiftMonth(-1)
                    it.consume()
                }
                AwtKeyEvent.VK_DOWN -> {
                    shiftMonth(1)
                    it.consume()
                }
                AwtKeyEvent.VK_ENTER -> {
                    selectedDate.value = formatDate(draftYear, draftMonth, draftDay)
                    onConfirm(selectedDate.value)
                    visibleState.value = false
                    it.consume()
                }
            }
        }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        val p = context.theme.palette
        val bounds = DialogBounds(32f, 20f, measuredWidth - 64f, measuredHeight - 40f)
        val titleStyle = TextStyle(context.theme.typography.titleSize + 1f, p.onSurface, context.theme.typography.fontFamily)
        val monthStyle = TextStyle(context.theme.typography.bodySize, p.onSurface, context.theme.typography.fontFamily)
        val dayStyle = TextStyle(context.theme.typography.bodySize, p.onSurface, context.theme.typography.fontFamily)
        val columns = 7
        val cellSize = ((bounds.width - 32f) / columns).coerceAtLeast(28f)
        val monthLabel = monthNames[draftMonth - 1] + " " + draftYear
        val weekdayStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), p.onSurfaceVariant, context.theme.typography.fontFamily)
        context.backend.translated(x, y) {
            context.backend.drawRect(0f, 0f, measuredWidth, measuredHeight, p.scrim.withAlpha(132), null)
            context.backend.drawRoundedRect(bounds.x, bounds.y, bounds.width, bounds.height, MaterialShapes.ExtraLarge, p.surfaceContainerHigh, null)
            context.backend.drawText(title, bounds.x + 24f, bounds.y + 34f, titleStyle)
            drawTopBarAction(context, "‹", bounds.x + 20f, bounds.y + 44f, false, false)
            drawTopBarAction(context, "›", bounds.x + bounds.width - 60f, bounds.y + 44f, false, false)
            val monthWidth = context.backend.measureText(monthLabel, monthStyle).first
            context.backend.drawText(monthLabel, bounds.x + (bounds.width - monthWidth) / 2f, bounds.y + 72f, monthStyle)
            weekdays.forEachIndexed { index, name ->
                val labelWidth = context.backend.measureText(name, weekdayStyle).first
                val labelX = bounds.x + 16f + index * cellSize + ((cellSize - 4f) - labelWidth) / 2f
                context.backend.drawText(name, labelX, bounds.y + 104f, weekdayStyle)
            }
            val maxDay = daysInMonth(draftYear, draftMonth)
            (1..maxDay).forEach { day ->
                val index = day - 1
                val col = index % columns
                val row = index / columns
                val cellX = bounds.x + 16f + col * cellSize
                val cellY = bounds.y + 116f + row * cellSize
                val selected = draftDay == day
                val hovered = hoverDay == day || pressedDay == day
                if (selected || hovered) {
                    context.backend.drawRoundedRect(cellX, cellY, cellSize - 4f, cellSize - 4f, MaterialShapes.Full, if (selected) p.primary else overlay(p.surfaceContainerHigh, p.onSurface, 0.06f, 1f), null)
                }
                val color = if (selected) p.onPrimary else p.onSurface
                val style = TextStyle(dayStyle.size, color, dayStyle.fontFamily)
                val text = day.toString()
                val textWidth = context.backend.measureText(text, style).first
                context.backend.drawText(text, cellX + (cellSize - 4f - textWidth) / 2f, cellY + textBaseline(cellSize - 4f, style.size), style)
            }
        }
    }

    private fun dayAt(localX: Float, localY: Float): Int {
        val bounds = DialogBounds(32f, 20f, measuredWidth - 64f, measuredHeight - 40f)
        val cellSize = ((bounds.width - 32f) / 7f).coerceAtLeast(28f)
        val gridY = bounds.y + 116f
        if (localX < bounds.x + 16f || localY < gridY) return -1
        val col = ((localX - (bounds.x + 16f)) / cellSize).toInt()
        val row = ((localY - gridY) / cellSize).toInt()
        if (col !in 0..6 || row !in 0..5) return -1
        val day = row * 7 + col + 1
        return day.takeIf { it in 1..daysInMonth(draftYear, draftMonth) } ?: -1
    }

    private fun shiftMonth(delta: Int) {
        val absoluteMonth = (draftYear * 12 + (draftMonth - 1)) + delta
        draftYear = absoluteMonth.floorDiv(12)
        draftMonth = absoluteMonth.mod(12) + 1
        draftDay = draftDay.coerceAtMost(daysInMonth(draftYear, draftMonth))
        requestRender()
    }

    private fun yearFrom(value: String): Int = value.substringBefore('-', "2026").toIntOrNull()?.coerceIn(1900, 9999) ?: 2026

    private fun monthFrom(value: String): Int = value.split('-').getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 7

    private fun dayFrom(value: String): Int = value.substringAfterLast('-', "1").toIntOrNull()?.coerceIn(1, 31) ?: 1

    private fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)) 29 else 28
        else -> 31
    }

    private fun formatDate(year: Int, month: Int, day: Int): String = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

    private companion object {
        private val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        private val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    }
}

class MaterialTimePicker(
    val selectedTime: State<String> = stateOf("12:00"),
    val visibleState: State<Boolean> = stateOf(false),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 320f,
    height: Float = 280f,
    var title: String = "Select time",
    var use24Hour: Boolean = true,
    private val onConfirm: (String) -> Unit = {}
) : Component(x, y, width, height) {
    private val invalidate = {
        visible = visibleState.value
        requestRender()
    }
    private var selectedHour = parseHour(selectedTime.value)
    private var selectedMinute = parseMinute(selectedTime.value)
    private var hoverHour = -1
    private var hoverMinute = -1
    private var hourPage = 0
    private var pmSelected = selectedHour >= 12

    init {
        focusable = true
        visible = visibleState.value
        visibleState.subscribe(invalidate)
        selectedTime.subscribe {
            selectedHour = parseHour(selectedTime.value)
            selectedMinute = parseMinute(selectedTime.value)
            pmSelected = selectedHour >= 12
            hourPage = if (use24Hour) selectedHour / 6 else ((displayHour(selectedHour) - 1) / 6)
            requestRender()
        }
        onDispose { visibleState.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!visible) return@on
            requestFocus()
            updateSelection(it.x - x, it.y - y)
            it.consume()
        }
        on(PointerMove) {
            if (!visible) return@on
            updateHover(it.x - x, it.y - y)
            requestRender()
        }
        onKey("keyDown") {
            if (!visible) return@onKey
            when (it.keyCode) {
                AwtKeyEvent.VK_ESCAPE -> {
                    visibleState.value = false
                    it.consume()
                }
                AwtKeyEvent.VK_UP -> {
                    selectedHour = (selectedHour + 1) % 24
                    pmSelected = selectedHour >= 12
                    hourPage = if (use24Hour) selectedHour / 6 else ((displayHour(selectedHour) - 1) / 6)
                    requestRender()
                    it.consume()
                }
                AwtKeyEvent.VK_DOWN -> {
                    selectedHour = (selectedHour + 23) % 24
                    pmSelected = selectedHour >= 12
                    hourPage = if (use24Hour) selectedHour / 6 else ((displayHour(selectedHour) - 1) / 6)
                    requestRender()
                    it.consume()
                }
                AwtKeyEvent.VK_LEFT -> {
                    hourPage = (hourPage - 1).coerceAtLeast(0)
                    requestRender()
                    it.consume()
                }
                AwtKeyEvent.VK_RIGHT -> {
                    hourPage = (hourPage + 1).coerceAtMost(3)
                    requestRender()
                    it.consume()
                }
                AwtKeyEvent.VK_ENTER -> {
                    selectedTime.value = formatTime(selectedHour, selectedMinute)
                    onConfirm(selectedTime.value)
                    visibleState.value = false
                    it.consume()
                }
            }
        }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        val p = context.theme.palette
        val bounds = DialogBounds(32f, 24f, measuredWidth - 64f, measuredHeight - 48f)
        val titleStyle = TextStyle(context.theme.typography.titleSize + 1f, p.onSurface, context.theme.typography.fontFamily)
        val valueStyle = TextStyle(context.theme.typography.titleSize + 8f, p.primary, context.theme.typography.fontFamily)
        val cellStyle = TextStyle(context.theme.typography.bodySize, p.onSurface, context.theme.typography.fontFamily)
        context.backend.translated(x, y) {
            context.backend.drawRect(0f, 0f, measuredWidth, measuredHeight, p.scrim.withAlpha(132), null)
            context.backend.drawRoundedRect(bounds.x, bounds.y, bounds.width, bounds.height, MaterialShapes.ExtraLarge, p.surfaceContainerHigh, null)
            context.backend.drawText(title, bounds.x + 24f, bounds.y + 34f, titleStyle)
            val value = if (use24Hour) formatTime(selectedHour, selectedMinute) else formatDisplayTime(selectedHour, selectedMinute, pmSelected)
            val valueWidth = context.backend.measureText(value, valueStyle).first
            context.backend.drawText(value, bounds.x + (bounds.width - valueWidth) / 2f, bounds.y + 76f, valueStyle)
            drawTopBarAction(context, "‹", bounds.x + 24f, bounds.y + 88f, false, false)
            drawTopBarAction(context, "›", bounds.x + 72f, bounds.y + 88f, false, false)
            drawTimeColumn(context, bounds.x + 24f, bounds.y + 136f, 88f, 96f, visibleHourItems(), hourSelectionIndex(), hoverHour, cellStyle)
            drawTimeColumn(context, bounds.x + bounds.width - 112f, bounds.y + 136f, 88f, 96f, minuteOptions.map { it.toString().padStart(2, '0') }, minuteIndex(selectedMinute), hoverMinute, cellStyle)
            if (!use24Hour) {
                drawMeridiemToggle(context, bounds.x + bounds.width / 2f - 44f, bounds.y + 136f, 88f, 72f)
            }
        }
    }

    private fun updateSelection(localX: Float, localY: Float) {
        val bounds = DialogBounds(32f, 24f, measuredWidth - 64f, measuredHeight - 48f)
        when {
            localX in (bounds.x + 24f)..(bounds.x + 64f) && localY in (bounds.y + 88f)..(bounds.y + 128f) -> {
                hourPage = (hourPage - 1).coerceAtLeast(0)
                return
            }
            localX in (bounds.x + 72f)..(bounds.x + 112f) && localY in (bounds.y + 88f)..(bounds.y + 128f) -> {
                hourPage = (hourPage + 1).coerceAtMost(3)
                return
            }
            !use24Hour && localX in (bounds.x + bounds.width / 2f - 44f)..(bounds.x + bounds.width / 2f + 44f) && localY in (bounds.y + 136f)..(bounds.y + 208f) -> {
                pmSelected = localY >= bounds.y + 172f
                selectedHour = applyMeridiem(displayHour(selectedHour), pmSelected)
                return
            }
        }
        val hour = timeCellAt(localX, localY, true)
        val minute = timeCellAt(localX, localY, false)
        if (hour >= 0) {
            selectedHour = if (use24Hour) hour else applyMeridiem(hour + 1, pmSelected)
            pmSelected = selectedHour >= 12
        }
        if (minute >= 0) selectedMinute = minuteOptions[minute]
        if (hour >= 0 || minute >= 0) {
            selectedTime.value = formatTime(selectedHour, selectedMinute)
            onConfirm(selectedTime.value)
            visibleState.value = false
        }
    }

    private fun updateHover(localX: Float, localY: Float) {
        hoverHour = timeCellAt(localX, localY, true)
        hoverMinute = timeCellAt(localX, localY, false)
    }

    private fun timeCellAt(localX: Float, localY: Float, hourColumn: Boolean): Int {
        val bounds = DialogBounds(32f, 24f, measuredWidth - 64f, measuredHeight - 48f)
        val startX = if (hourColumn) bounds.x + 24f else bounds.x + bounds.width - 112f
        val startY = bounds.y + 136f
        val cellHeight = 24f
        val maxRows = 4
        if (localX !in startX..(startX + 88f) || localY !in startY..(startY + maxRows * cellHeight)) return -1
        val row = ((localY - startY) / cellHeight).toInt().coerceIn(0, 3)
        return if (hourColumn) {
            val index = hourPage * 4 + row
            val maxIndex = if (use24Hour) 23 else 11
            index.takeIf { it in 0..maxIndex } ?: -1
        } else {
            row.coerceAtMost(3)
        }
    }

    private fun visibleHourItems(): List<String> {
        val values = if (use24Hour) (0..23).toList() else (1..12).toList()
        return values.drop(hourPage * 4).take(4).map { it.toString().padStart(2, '0') }
    }

    private fun hourSelectionIndex(): Int {
        val base = hourPage * 4
        val index = if (use24Hour) selectedHour else displayHour(selectedHour) - 1
        return (index - base).coerceIn(0, 3)
    }

    private fun drawMeridiemToggle(context: RenderContext, x: Float, y: Float, width: Float, height: Float) {
        val p = context.theme.palette
        val optionHeight = height / 2f
        val style = TextStyle(context.theme.typography.bodySize, p.onSurface, context.theme.typography.fontFamily)
        listOf("AM", "PM").forEachIndexed { index, label ->
            val selected = if (index == 0) !pmSelected else pmSelected
            val boxY = y + index * optionHeight
            context.backend.drawRoundedRect(x, boxY, width, optionHeight - 4f, 12f, if (selected) p.primaryContainer else p.surfaceContainer, null)
            val textStyle = TextStyle(style.size, if (selected) p.onPrimaryContainer else p.onSurface, style.fontFamily)
            val textWidth = context.backend.measureText(label, textStyle).first
            context.backend.drawText(label, x + (width - textWidth) / 2f, boxY + textBaseline(optionHeight - 4f, textStyle.size), textStyle)
        }
    }

    private fun displayHour(hour24: Int): Int = ((hour24 + 11) % 12) + 1

    private fun applyMeridiem(hour12: Int, pm: Boolean): Int {
        val normalized = hour12.coerceIn(1, 12) % 12
        return if (pm) normalized + 12 else normalized
    }

    private fun parseHour(value: String): Int = value.substringBefore(':').toIntOrNull()?.coerceIn(0, 23) ?: 12

    private fun parseMinute(value: String): Int = value.substringAfter(':', "00").toIntOrNull()?.let { minute -> minuteOptions.minBy { abs(it - minute) } } ?: 0

    private fun minuteIndex(value: Int): Int = minuteOptions.indexOf(value).coerceAtLeast(0)

    private fun formatTime(hour: Int, minute: Int): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

    private fun formatDisplayTime(hour: Int, minute: Int, pm: Boolean): String = "${displayHour(hour).toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} ${if (pm) "PM" else "AM"}"

    private companion object {
        private val minuteOptions = listOf(0, 15, 30, 45)
    }
}

class MaterialDateRangePicker(
    val startDate: State<String> = stateOf(""),
    val endDate: State<String> = stateOf(""),
    val visibleState: State<Boolean> = stateOf(false),
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 360f,
    height: Float = 320f,
    var title: String = "Select date range",
    private val onConfirm: (String, String) -> Unit = { _, _ -> }
) : Component(x, y, width, height) {
    private val invalidate = {
        visible = visibleState.value
        requestRender()
    }
    private var hoverDay = -1
    private var pressedDay = -1
    private var draftStart = startDate.value
    private var draftEnd = endDate.value
    private var draftYear = yearFrom(startDate.value.ifBlank { endDate.value })
    private var draftMonth = monthFrom(startDate.value.ifBlank { endDate.value })
    private var draftDay = dayFrom(startDate.value.ifBlank { endDate.value })

    init {
        focusable = true
        visible = visibleState.value
        visibleState.subscribe(invalidate)
        startDate.subscribe {
            draftStart = startDate.value
            draftYear = yearFrom(startDate.value.ifBlank { endDate.value })
            draftMonth = monthFrom(startDate.value.ifBlank { endDate.value })
            draftDay = dayFrom(startDate.value.ifBlank { endDate.value })
            requestRender()
        }
        endDate.subscribe {
            draftEnd = endDate.value
            requestRender()
        }
        onDispose { visibleState.unsubscribe(invalidate) }
        on(PointerDown) {
            if (!visible) return@on
            requestFocus()
            pressedDay = dayAt(it.x - x, it.y - y)
            it.consume()
        }
        on(PointerMove) {
            if (!visible) return@on
            hoverDay = dayAt(it.x - x, it.y - y)
            requestRender()
        }
        on(PointerUp) {
            if (!visible) return@on
            val localX = it.x - x
            val localY = it.y - y
            when {
                localY in 44f..84f && localX in 56f..96f -> shiftMonth(-1)
                localY in 44f..84f && localX in (measuredWidth - 96f)..(measuredWidth - 56f) -> shiftMonth(1)
                else -> {
                    val releasedDay = dayAt(localX, localY)
                    if (releasedDay in 1..daysInMonth(draftYear, draftMonth) && releasedDay == pressedDay) {
                        selectDay(releasedDay)
                    }
                }
            }
            pressedDay = -1
            it.consume()
        }
        onKey("keyDown") {
            if (!visible) return@onKey
            when (it.keyCode) {
                AwtKeyEvent.VK_ESCAPE -> {
                    visibleState.value = false
                    it.consume()
                }
                AwtKeyEvent.VK_LEFT -> {
                    draftDay = (draftDay - 1).coerceAtLeast(1)
                    requestRender()
                    it.consume()
                }
                AwtKeyEvent.VK_RIGHT -> {
                    draftDay = (draftDay + 1).coerceAtMost(daysInMonth(draftYear, draftMonth))
                    requestRender()
                    it.consume()
                }
                AwtKeyEvent.VK_UP -> {
                    shiftMonth(-1)
                    it.consume()
                }
                AwtKeyEvent.VK_DOWN -> {
                    shiftMonth(1)
                    it.consume()
                }
                AwtKeyEvent.VK_ENTER -> {
                    selectDay(draftDay)
                    it.consume()
                }
            }
        }
    }

    override fun draw(context: RenderContext) {
        if (!visible) return
        val p = context.theme.palette
        val bounds = DialogBounds(32f, 20f, measuredWidth - 64f, measuredHeight - 40f)
        val titleStyle = TextStyle(context.theme.typography.titleSize + 1f, p.onSurface, context.theme.typography.fontFamily)
        val summaryStyle = TextStyle(context.theme.typography.bodySize, p.primary, context.theme.typography.fontFamily)
        val monthStyle = TextStyle(context.theme.typography.bodySize, p.onSurface, context.theme.typography.fontFamily)
        val dayStyle = TextStyle(context.theme.typography.bodySize, p.onSurface, context.theme.typography.fontFamily)
        val weekdayStyle = TextStyle((context.theme.typography.bodySize - 2f).coerceAtLeast(10f), p.onSurfaceVariant, context.theme.typography.fontFamily)
        val cellSize = ((bounds.width - 32f) / 7f).coerceAtLeast(28f)
        val monthLabel = monthNames[draftMonth - 1] + " " + draftYear
        val summary = when {
            draftStart.isBlank() -> "Select a start date"
            draftEnd.isBlank() -> "$draftStart – …"
            else -> "$draftStart – $draftEnd"
        }
        context.backend.translated(x, y) {
            context.backend.drawRect(0f, 0f, measuredWidth, measuredHeight, p.scrim.withAlpha(132), null)
            context.backend.drawRoundedRect(bounds.x, bounds.y, bounds.width, bounds.height, MaterialShapes.ExtraLarge, p.surfaceContainerHigh, null)
            context.backend.drawText(title, bounds.x + 24f, bounds.y + 34f, titleStyle)
            context.backend.drawText(summary.takeFitting(context, summaryStyle, bounds.width - 48f), bounds.x + 24f, bounds.y + 62f, summaryStyle)
            drawTopBarAction(context, "‹", bounds.x + 20f, bounds.y + 76f, false, false)
            drawTopBarAction(context, "›", bounds.x + bounds.width - 60f, bounds.y + 76f, false, false)
            val monthWidth = context.backend.measureText(monthLabel, monthStyle).first
            context.backend.drawText(monthLabel, bounds.x + (bounds.width - monthWidth) / 2f, bounds.y + 104f, monthStyle)
            weekdays.forEachIndexed { index, name ->
                val labelWidth = context.backend.measureText(name, weekdayStyle).first
                val labelX = bounds.x + 16f + index * cellSize + ((cellSize - 4f) - labelWidth) / 2f
                context.backend.drawText(name, labelX, bounds.y + 136f, weekdayStyle)
            }
            val maxDay = daysInMonth(draftYear, draftMonth)
            (1..maxDay).forEach { day ->
                val index = day - 1
                val col = index % 7
                val row = index / 7
                val cellX = bounds.x + 16f + col * cellSize
                val cellY = bounds.y + 148f + row * cellSize
                val value = formatDate(draftYear, draftMonth, day)
                val selected = value == draftStart || value == draftEnd
                val inRange = draftStart.isNotBlank() && draftEnd.isNotBlank() && value > draftStart && value < draftEnd
                val hovered = hoverDay == day || pressedDay == day || draftDay == day
                when {
                    selected -> context.backend.drawRoundedRect(cellX, cellY, cellSize - 4f, cellSize - 4f, MaterialShapes.Full, p.primary, null)
                    inRange -> context.backend.drawRoundedRect(cellX, cellY, cellSize - 4f, cellSize - 4f, MaterialShapes.Medium, p.primaryContainer, null)
                    hovered -> context.backend.drawRoundedRect(cellX, cellY, cellSize - 4f, cellSize - 4f, MaterialShapes.Full, overlay(p.surfaceContainerHigh, p.onSurface, 0.06f, 1f), null)
                }
                val color = when {
                    selected -> p.onPrimary
                    inRange -> p.onPrimaryContainer
                    else -> p.onSurface
                }
                val style = TextStyle(dayStyle.size, color, dayStyle.fontFamily)
                val text = day.toString()
                val textWidth = context.backend.measureText(text, style).first
                context.backend.drawText(text, cellX + (cellSize - 4f - textWidth) / 2f, cellY + textBaseline(cellSize - 4f, style.size), style)
            }
        }
    }

    private fun selectDay(day: Int) {
        val chosen = formatDate(draftYear, draftMonth, day)
        draftDay = day
        when {
            draftStart.isBlank() || draftEnd.isNotBlank() -> {
                draftStart = chosen
                draftEnd = ""
                startDate.value = draftStart
                endDate.value = draftEnd
                requestRender()
            }
            else -> {
                if (chosen < draftStart) {
                    draftEnd = draftStart
                    draftStart = chosen
                } else {
                    draftEnd = chosen
                }
                startDate.value = draftStart
                endDate.value = draftEnd
                onConfirm(draftStart, draftEnd)
                visibleState.value = false
            }
        }
    }

    private fun dayAt(localX: Float, localY: Float): Int {
        val bounds = DialogBounds(32f, 20f, measuredWidth - 64f, measuredHeight - 40f)
        val cellSize = ((bounds.width - 32f) / 7f).coerceAtLeast(28f)
        val gridY = bounds.y + 148f
        if (localX < bounds.x + 16f || localY < gridY) return -1
        val col = ((localX - (bounds.x + 16f)) / cellSize).toInt()
        val row = ((localY - gridY) / cellSize).toInt()
        if (col !in 0..6 || row !in 0..5) return -1
        val day = row * 7 + col + 1
        return day.takeIf { it in 1..daysInMonth(draftYear, draftMonth) } ?: -1
    }

    private fun shiftMonth(delta: Int) {
        val absoluteMonth = (draftYear * 12 + (draftMonth - 1)) + delta
        draftYear = absoluteMonth.floorDiv(12)
        draftMonth = absoluteMonth.mod(12) + 1
        draftDay = draftDay.coerceAtMost(daysInMonth(draftYear, draftMonth))
        requestRender()
    }

    private fun yearFrom(value: String): Int = value.substringBefore('-', "2026").toIntOrNull()?.coerceIn(1900, 9999) ?: 2026

    private fun monthFrom(value: String): Int = value.split('-').getOrNull(1)?.toIntOrNull()?.coerceIn(1, 12) ?: 7

    private fun dayFrom(value: String): Int = value.substringAfterLast('-', "1").toIntOrNull()?.coerceIn(1, 31) ?: 1

    private fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)) 29 else 28
        else -> 31
    }

    private fun formatDate(year: Int, month: Int, day: Int): String = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

    private companion object {
        private val monthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        private val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    }
}

private fun drawTimeColumn(
    context: RenderContext,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    items: List<String>,
    selectedIndex: Int,
    hoverIndex: Int,
    style: TextStyle
) {
    val p = context.theme.palette
    val rowHeight = height / items.size.coerceAtLeast(1)
    context.backend.drawRoundedRect(x, y, width, height, MaterialShapes.Medium, p.surfaceContainer, null)
    items.forEachIndexed { index, item ->
        val top = y + index * rowHeight
        val selected = index == selectedIndex
        val hovered = index == hoverIndex
        if (selected || hovered) {
            context.backend.drawRoundedRect(
                x + 4f,
                top + 2f,
                width - 8f,
                rowHeight - 4f,
                MaterialShapes.Small,
                if (selected) p.primaryContainer else overlay(p.surfaceContainerHigh, p.onSurface, 0.06f, 1f),
                null
            )
        }
        val textStyle = if (selected) {
            TextStyle(style.size, p.onPrimaryContainer, style.fontFamily)
        } else if (hovered) {
            TextStyle(style.size, p.onSurface, style.fontFamily)
        } else {
            style
        }
        val textWidth = context.backend.measureText(item, textStyle).first
        context.backend.drawText(item, x + (width - textWidth) / 2f, top + textBaseline(rowHeight, textStyle.size), textStyle)
    }
}

