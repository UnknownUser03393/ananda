package dev.unknownuser.ananda.remote

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import dev.unknownuser.ananda.backend.GradientDirection
import dev.unknownuser.ananda.backend.Shadow
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextStyle
import dev.unknownuser.ananda.component.Button
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.component.Label
import dev.unknownuser.ananda.component.ScrollContainer
import dev.unknownuser.ananda.component.TextField
import dev.unknownuser.ananda.component.ToggleSwitch
import dev.unknownuser.ananda.dsl.SceneBuilder
import dev.unknownuser.ananda.draw.Scene
import dev.unknownuser.ananda.event.PointerDown
import dev.unknownuser.ananda.reactive.stateOf
import dev.unknownuser.ananda.theme.Palette
import dev.unknownuser.ananda.theme.Theme
import dev.unknownuser.ananda.theme.Typography
import dev.unknownuser.ananda.window.RenderMode
import dev.unknownuser.ananda.window.SkiaWindowManagementProvider
import dev.unknownuser.ananda.window.WindowOptions
import java.awt.Color
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.util.Locale

private const val WindowWidth = 920
private const val WindowHeight = 560
private const val SidebarWidth = 148f
private const val PanelRadius = 14f

fun main() {
    RemoteClickGui.start()
}

object RemoteClickGui {
    private val gson = Gson()
    private var writer: OutputStreamWriter? = null
    @Volatile private var modules = mutableListOf<ModuleInfo>()
    @Volatile private var connectionStatus = "Connecting"
    private lateinit var scene: Scene
    private var selectedCategory = "All"
    private var searchQuery = ""

    data class ModuleInfo(val name: String, val category: String, val enabled: Boolean, val keybind: Int)

    fun start(host: String = "127.0.0.1", port: Int = 54545) {
        scene = buildInitialScene()
        connect(host, port)
        SkiaWindowManagementProvider.createWindow(
            options = WindowOptions(
                title = "Ananda Native ClickGUI",
                width = WindowWidth,
                height = WindowHeight,
                renderMode = RenderMode.OnDemand
            ),
            scene = scene,
            interactions = scene
        ).show(blocking = true)
    }

    private fun connect(host: String, port: Int) {
        Thread({
            while (true) {
                try {
                    connectionStatus = "Connecting"
                    rebuildScene(scene)
                    val socket = Socket(host, port)
                    writer = OutputStreamWriter(socket.getOutputStream())
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    connectionStatus = "Connected"
                    rebuildScene(scene)
                    sendRaw("""{"type":"refresh"}""")
                    while (true) {
                        val line = reader.readLine() ?: break
                        val msg = try {
                            gson.fromJson(line, JsonObject::class.java)
                        } catch (e: Exception) {
                            println("[RemoteGUI] Parse err: ${e.message}")
                            continue
                        }
                        handle(msg)
                    }
                } catch (e: Exception) {
                    connectionStatus = "Retrying"
                    rebuildScene(scene)
                    println("[RemoteGUI] Connection failed, retrying in 2s: ${e.message}")
                    Thread.sleep(2000)
                }
            }
        }, "RemoteGUI-net").apply { isDaemon = true }.start()
    }

    private fun handle(msg: JsonObject) {
        val type = msg.get("type")?.asString ?: return
        when (type) {
            "modules" -> {
                val listType = object : TypeToken<List<ModuleInfo>>() {}.type
                val list: List<ModuleInfo> = try {
                    gson.fromJson(msg.get("data"), listType)
                } catch (e: Exception) {
                    println("[RemoteGUI] Modules parse error: ${e.message}")
                    return
                }
                modules.clear()
                modules.addAll(list.sortedWith(compareBy<ModuleInfo> { it.category }.thenBy { it.name }))
                if (selectedCategory != "All" && modules.none { it.category == selectedCategory }) {
                    selectedCategory = "All"
                }
                rebuildScene(scene)
            }
        }
    }

    private fun sendRaw(json: String) {
        try {
            writer?.write(json + "\n")
            writer?.flush()
        } catch (_: Exception) {
        }
    }

    private fun toggleModule(name: String) {
        sendRaw(gson.toJson(mapOf("type" to "toggle", "module" to name)))
        println("[RemoteGUI] Sent toggle: $name")
    }

    private fun buildInitialScene(): Scene =
        Scene().also(::rebuildScene)

    private fun rebuildScene(sc: Scene) {
        val snapshot = modules.toList()
        val categories = listOf("All") + snapshot.map { it.category }.distinct().sorted()
        val visibleModules = filteredModules(snapshot)
        sc.clear()

        SceneBuilder(sc).apply {
            theme(nativeTheme())
            component(0f, 0f, WindowWidth.toFloat(), WindowHeight.toFloat()) {
                background(Color(7, 10, 14))
            }
            glowOrb(
                x = -54f,
                y = -52f,
                width = 230f,
                height = 230f,
                colors = listOf(Color(56, 189, 248, 44), Color(56, 189, 248, 0))
            )
            glowOrb(
                x = WindowWidth - 220f,
                y = WindowHeight - 210f,
                width = 260f,
                height = 260f,
                colors = listOf(Color(250, 204, 21, 34), Color(250, 204, 21, 0))
            )
            elevatedPanel(
                x = 42f,
                y = 38f,
                width = WindowWidth - 84f,
                height = WindowHeight - 76f,
                radius = 18f,
                colors = listOf(Color(22, 27, 34, 238), Color(12, 16, 22, 238)),
                gradientDirection = GradientDirection.DiagonalDown,
                shadow = Shadow(Color(0, 0, 0, 126), blurRadius = 24f, offsetY = 12f),
                outline = Stroke(Color(255, 255, 255, 42), 1f)
            )

            addHeader(snapshot)
            addSidebar(categories, snapshot)
            addModuleGrid(visibleModules)
        }

        sc.requestRender()
    }

    private fun filteredModules(snapshot: List<ModuleInfo>): List<ModuleInfo> {
        val query = searchQuery.trim().lowercase(Locale.ROOT)
        return snapshot.filter { module ->
            (selectedCategory == "All" || module.category == selectedCategory) &&
                (query.isEmpty() || module.name.lowercase(Locale.ROOT).contains(query))
        }
    }

    private fun SceneBuilder.addHeader(snapshot: List<ModuleInfo>) {
        text("Ananda", 66f, 58f, 22f, Color.WHITE)
        text("native clickgui", 160f, 66f, 10.5f, MutedText)

        val enabled = snapshot.count { it.enabled }
        val statusColor = when (connectionStatus) {
            "Connected" -> Success
            "Retrying" -> Warning
            else -> MutedText
        }
        statusChip(WindowWidth - 248f, 58f, connectionStatus, statusColor)
        statusChip(WindowWidth - 152f, 58f, "$enabled / ${snapshot.size}", AccentBlue)

        button("Refresh", WindowWidth - 118f, 92f, 54f, 22f) {
            sendRaw("""{"type":"refresh"}""")
        }
    }

    private fun SceneBuilder.addSidebar(categories: List<String>, snapshot: List<ModuleInfo>) {
        elevatedPanel(
            x = 58f,
            y = 92f,
            width = SidebarWidth,
            height = WindowHeight - 146f,
            radius = PanelRadius,
            colors = listOf(Color(20, 25, 32, 224), Color(15, 19, 25, 224)),
            shadow = Shadow(Color(0, 0, 0, 70), blurRadius = 14f, offsetY = 5f),
            outline = Stroke(Color(255, 255, 255, 30), 1f)
        )
        text("Categories", 75f, 111f, 11f, Color.WHITE)

        categories.forEachIndexed { index, category ->
            val y = 138f + index * 30f
            if (y > WindowHeight - 90f) return@forEachIndexed
            categoryRow(74f, y, SidebarWidth - 32f, category, category == selectedCategory, snapshot)
        }
    }

    private fun SceneBuilder.addModuleGrid(visibleModules: List<ModuleInfo>) {
        val x = 224f
        val y = 92f
        val width = WindowWidth - x - 58f
        val height = WindowHeight - 146f

        elevatedPanel(
            x = x,
            y = y,
            width = width,
            height = height,
            radius = PanelRadius,
            colors = listOf(Color(20, 25, 32, 224), Color(14, 18, 24, 224)),
            shadow = Shadow(Color(0, 0, 0, 70), blurRadius = 14f, offsetY = 5f),
            outline = Stroke(Color(255, 255, 255, 30), 1f)
        )
        text(if (selectedCategory == "All") "Modules" else selectedCategory, x + 18f, y + 19f, 13f, Color.WHITE)
        text("${visibleModules.size} visible", x + 96f, y + 20f, 9.5f, MutedText)

        val search = stateOf(searchQuery)
        search.subscribe {
            searchQuery = search.value
            rebuildScene(scene)
        }
        scSearchField(search, x + width - 216f, y + 12f)

        val list = ScrollContainer(x + 14f, y + 50f, width - 28f, height - 64f).apply {
            scrollStep = 24f
        }
        var rowX = 0f
        var rowY = 0f
        visibleModules.forEach { module ->
            val cardWidth = (width - 42f) / 2f
            list.add(moduleCard(module, rowX, rowY, cardWidth, 62f))
            if (rowX == 0f) {
                rowX = cardWidth + 14f
            } else {
                rowX = 0f
                rowY += 74f
            }
        }
        if (visibleModules.isEmpty()) {
            list.add(emptyState(0f, 0f, width - 28f, 130f))
        }
        addRaw(list)
    }

    private fun SceneBuilder.scSearchField(value: dev.unknownuser.ananda.reactive.State<String>, x: Float, y: Float) {
        addRaw(TextField(value, x, y, 188f, 24f, "Search modules").apply {
            cornerRadius = 6f
        })
    }

    private fun SceneBuilder.categoryRow(
        x: Float,
        y: Float,
        width: Float,
        category: String,
        selected: Boolean,
        snapshot: List<ModuleInfo>
    ) {
        val count = if (category == "All") snapshot.size else snapshot.count { it.category == category }
        val fill = if (selected) AccentBlue.withAlpha(82) else Color(255, 255, 255, 10)
        val border = if (selected) AccentBlue.withAlpha(160) else Color(255, 255, 255, 22)
        component(x, y, width, 22f) {
            background(fill)
            border(border, 1f)
        }.apply {
            cornerRadius = 7f
            on(PointerDown) {
                selectedCategory = category
                rebuildScene(scene)
                it.consume()
            }
            render { context ->
                context.backend.drawText(category, 9f, 14.5f, TextStyle(10.5f, Color.WHITE, context.theme.typography.fontFamily))
                val text = count.toString()
                val style = TextStyle(9f, MutedText, context.theme.typography.fontFamily)
                val textWidth = context.backend.measureText(text, style).first
                context.backend.drawText(text, width - textWidth - 9f, 14f, style)
            }
        }
    }

    private fun moduleCard(module: ModuleInfo, x: Float, y: Float, width: Float, height: Float): Component {
        val enabled = stateOf(module.enabled)
        enabled.subscribe {
            toggleModule(module.name)
        }

        return Component(x, y, width, height).apply {
            cornerRadius = 10f
            backgroundColor = if (module.enabled) AccentBlue.withAlpha(44) else Color(255, 255, 255, 12)
            borderColor = if (module.enabled) AccentBlue.withAlpha(140) else Color(255, 255, 255, 24)
            borderWidth = 1f
            elevationShadow = Shadow(Color(0, 0, 0, 42), blurRadius = 8f, offsetY = 3f)
            on(PointerDown) {
                if (it.button == 0) {
                    toggleModule(module.name)
                    it.consume()
                }
            }
            render { context ->
                val titleStyle = TextStyle(12.5f, Color.WHITE, context.theme.typography.fontFamily)
                val metaStyle = TextStyle(9.5f, MutedText, context.theme.typography.fontFamily)
                context.backend.drawText(module.name, 13f, 21f, titleStyle)
                context.backend.drawText(module.category, 13f, 42f, metaStyle)
                context.backend.drawText(keybindLabel(module.keybind), width - 84f, 42f, metaStyle)
            }
            add(ToggleSwitch(enabled, width - 50f, 18f, 34f, 18f))
        }
    }

    private fun emptyState(x: Float, y: Float, width: Float, height: Float): Component =
        Component(x, y, width, height).apply {
            render { context ->
                val title = "No modules"
                val body = "Try another category or search."
                val titleStyle = TextStyle(16f, Color.WHITE, context.theme.typography.fontFamily)
                val bodyStyle = TextStyle(11f, MutedText, context.theme.typography.fontFamily)
                val titleWidth = context.backend.measureText(title, titleStyle).first
                val bodyWidth = context.backend.measureText(body, bodyStyle).first
                context.backend.drawText(title, (width - titleWidth) / 2f, 52f, titleStyle)
                context.backend.drawText(body, (width - bodyWidth) / 2f, 76f, bodyStyle)
            }
        }

    private fun SceneBuilder.statusChip(x: Float, y: Float, text: String, color: Color) {
        component(x, y, 82f, 21f) {
            background(color.withAlpha(38))
            border(color.withAlpha(130), 1f)
        }.apply {
            cornerRadius = 8f
            render { context ->
                val style = TextStyle(9.5f, color, context.theme.typography.fontFamily)
                val textWidth = context.backend.measureText(text, style).first
                context.backend.drawText(text, (82f - textWidth) / 2f, 14f, style)
            }
        }
    }

    private fun SceneBuilder.addRaw(component: Component) {
        val field = SceneBuilder::class.java.getDeclaredMethod("add", dev.unknownuser.ananda.draw.Drawable::class.java)
        field.isAccessible = true
        field.invoke(this, component)
    }

    private fun keybindLabel(keybind: Int): String =
        if (keybind <= 0) "unbound" else "key $keybind"

    private fun nativeTheme(): Theme =
        Theme(
            palette = Palette(
                background = Color(7, 10, 14),
                surface = Color(20, 25, 32),
                primary = AccentBlue,
                accent = Warning,
                danger = Color(239, 68, 68),
                text = Color.WHITE,
                mutedText = MutedText,
                border = Color(111, 122, 138)
            ),
            typography = Typography(fontFamily = "Segoe UI", bodySize = 12.5f, titleSize = 22f)
        )

    private val AccentBlue = Color(56, 189, 248)
    private val Success = Color(74, 222, 128)
    private val Warning = Color(250, 204, 21)
    private val MutedText = Color(148, 163, 184)
}

private fun Color.withAlpha(alpha: Int): Color =
    Color(red, green, blue, alpha.coerceIn(0, 255))
