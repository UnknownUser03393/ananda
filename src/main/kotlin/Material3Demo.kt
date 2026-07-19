package dev.unknownuser

import dev.unknownuser.ananda.animation.EnterTransition
import dev.unknownuser.ananda.animation.SpringSpec
import dev.unknownuser.ananda.component.Component
import dev.unknownuser.ananda.component.Label
import dev.unknownuser.ananda.component.ScrollContainer
import dev.unknownuser.ananda.draw.Scene
import dev.unknownuser.ananda.material3.*
import dev.unknownuser.ananda.reactive.State
import dev.unknownuser.ananda.reactive.stateOf
import dev.unknownuser.ananda.window.RenderMode
import dev.unknownuser.ananda.window.SkiaWindow
import java.awt.Color

private const val DemoWidth = 1280
private const val DemoHeight = 820
private const val SectionWidth = 1180f

fun main() {
    val darkTheme = MaterialTheme.dark(Color(94, 234, 212))
    val lightTheme = MaterialTheme.light(Color(38, 112, 105))
    val scene = Scene().apply { theme = darkTheme }
    val overlays = mutableListOf<Component>()
    var dark = true

    val root = MaterialSurface(width = DemoWidth.toFloat(), height = DemoHeight.toFloat(), level = MaterialSurfaceLevel.Lowest, shape = 0f)
    val header = MaterialSurface(width = DemoWidth.toFloat(), height = 88f, level = MaterialSurfaceLevel.Low).placed(0, 0)
    header.add(Label("Ananda - Material 3 Component Gallery", width = 760f, height = 36f, size = 28f).placed(36, 18))
    header.add(Label("Scroll to browse - interactive controls - launch overlays from the last section", width = 820f, height = 20f, size = 14f).placed(38, 56))
    header.add(MaterialButton("Light / Dark Theme", width = 168f, variant = MaterialButtonStyle.Tonal) {
        dark = !dark
        scene.transitionTheme(if (dark) darkTheme else lightTheme, SpringSpec(stiffness = 180f, dampingRatio = 0.9f))
    }.placed(1060, 24))
    root.add(header)

    val scroll = ScrollContainer(width = DemoWidth.toFloat(), height = DemoHeight - 88f).placed(0, 88)
    scroll.scrollStep = 42f
    val content = Component(width = DemoWidth.toFloat(), height = 4700f).placed(0, 0)
    val gallery = Gallery(content)
    buildGallery(gallery, overlays)
    scroll.add(content)
    root.add(scroll)
    scene.add(root)
    overlays.forEach(scene::add)

    SkiaWindow(
        title = "Ananda Material 3 - All Components",
        width = DemoWidth,
        height = DemoHeight,
        scene = scene,
        renderMode = RenderMode.Continuous
    ).show()
}

private fun buildGallery(ui: Gallery, overlays: MutableList<Component>) {
    val enabled = stateOf(true)
    val selected = stateOf(true)
    val radio = stateOf("A")

    ui.section("Surfaces - Buttons - Cards", 330f) {
        MaterialSurfaceLevel.entries.forEachIndexed { index, level ->
            add(MaterialSurface(width = 200f, height = 64f, level = level).apply {
                add(Label(level.name, width = 160f, height = 22f, size = 14f).placed(16, 18))
            }.placed(24 + index * 220, 58))
        }
        MaterialButtonStyle.entries.forEachIndexed { index, style ->
            add(MaterialButton(style.name, width = 132f, variant = style).placed(24 + index * 150, 144))
        }
        MaterialCardStyle.entries.forEachIndexed { index, style ->
            add(MaterialCard(width = 250f, height = 82f, variant = style, interactive = true).apply {
                add(Label("${style.name} Card", width = 210f, height = 24f, size = 15f).placed(18, 22))
            }.placed(24 + index * 270, 208))
        }
        add(MaterialSwitch(enabled).placed(874, 222))
        add(Label("MaterialSwitch", width = 170f, height = 24f, size = 15f).placed(940, 226))
    }

    ui.section("Chips", 160f) {
        add(MaterialChip("Chip", selected).placed(24, 68))
        add(MaterialAssistChip("Assist", "+").placed(140, 68))
        add(MaterialSuggestionChip("Suggestion").placed(272, 68))
        add(MaterialElevatedAssistChip("Elevated", "*").placed(410, 68))
        add(MaterialFilterChip("Filter", stateOf(true), "v").placed(548, 68))
        add(MaterialInputChip("Input", "A", "x").placed(686, 68))
    }

    ui.section("Text Inputs - Selection - Sliders", 350f) {
        add(MaterialTextField(stateOf(""), width = 330f, label = "Filled TextField", placeholder = "\u8f93\u5165\u4e2d\u6587...").placed(24, 62))
        add(MaterialTextField(stateOf("Ananda"), width = 330f, label = "Outlined TextField", supportingText = "Floating label and outline notch", variant = MaterialTextFieldStyle.Outlined).placed(378, 62))
        add(MaterialTextArea(stateOf("Multiline TextArea\n\u652f\u6301\u4e2d\u6587\u8f93\u5165"), width = 430f, height = 132f, label = "TextArea", variant = MaterialTextFieldStyle.Outlined).placed(732, 62))
        add(MaterialCheckbox("MaterialCheckbox", enabled).placed(24, 164))
        add(MaterialRadioButton(radio, "A", "Radio A").placed(220, 164))
        add(MaterialRadioButton(radio, "B", "Radio B").placed(400, 164))
        add(MaterialSlider(stateOf(0.62f), width = 430f).placed(24, 232))
        add(MaterialRangeSlider(stateOf(0.22f), stateOf(0.78f), width = 430f).placed(500, 232))
        add(MaterialExposedDropdownMenu(stateOf(1), stateOf(false), listOf("All", "Unread", "Starred"), width = 280f, label = "Exposed Dropdown").placed(24, 282))
        add(MaterialAutocompleteField(stateOf(""), stateOf(false), listOf("Ananda", "Animation", "Adaptive Grid", "Autocomplete"), width = 320f, label = "Autocomplete").placed(330, 282))
    }

    ui.section("Progress - Badges - Refresh", 190f) {
        add(MaterialLinearProgressIndicator(stateOf(0.68f), width = 260f).placed(24, 72))
        add(MaterialIndeterminateLinearProgressIndicator(width = 260f).placed(24, 112))
        add(MaterialCircularProgressIndicator(stateOf(0.72f)).placed(340, 60))
        add(MaterialIndeterminateCircularProgressIndicator().placed(410, 60))
        add(MaterialBadge("9").placed(492, 70))
        add(MaterialBadge("").placed(540, 76))
        add(MaterialRefreshIndicator(stateOf(false), stateOf(0.65f)).placed(600, 58))
        add(MaterialRefreshIndicator(stateOf(true), stateOf(1f)).placed(670, 58))
        add(Label("Determinate / Indeterminate / Badge / Pull-to-refresh", width = 620f, height = 22f, size = 14f).placed(24, 138))
    }

    ui.section("Icon Buttons - FAB - Speed Dial", 300f) {
        add(MaterialIconButton("+").placed(24, 64))
        add(MaterialIconButton("*", filled = true).placed(82, 64))
        add(MaterialIconToggleButton("H", stateOf(true)).placed(140, 64))
        add(MaterialFloatingActionButton("+").placed(210, 54))
        add(MaterialSmallFloatingActionButton("S").placed(286, 62))
        add(MaterialLargeFloatingActionButton("L").placed(350, 44))
        add(MaterialExtendedFloatingActionButton("+", "Create").placed(466, 54))
        add(MaterialSpeedDial(
            stateOf(true),
            listOf(MaterialSpeedDialAction("Create", "+"), MaterialSpeedDialAction("Reminder", "!", "Follow up later")),
            "+", width = 260f, height = 210f
        ).placed(820, 54))
    }

    ui.section("Top App Bars - Bottom App Bar", 520f) {
        add(MaterialTopAppBar("Small Top App Bar", width = 540f, subtitle = "MaterialTopAppBar", navigationLabel = "<", actions = actions()).placed(24, 58))
        add(MaterialCenterAlignedTopAppBar("Center Aligned", width = 540f, navigationLabel = "<", actions = actions()).placed(606, 58))
        add(MaterialMediumTopAppBar("Medium Top App Bar", width = 540f, navigationLabel = "<").placed(24, 150))
        add(MaterialLargeTopAppBar("Large Top App Bar", width = 540f, subtitle = "Large presentation", actions = actions()).placed(606, 150))
        add(MaterialBottomAppBar(width = 1128f, navigationLabel = "M", actions = actions(), fabLabel = "+").placed(24, 388))
    }

    val navIndex = stateOf(0)
    ui.section("Tabs - Navigation", 430f) {
        add(MaterialTabRow(listOf("Home", "Library", "Settings"), navIndex, width = 500f).placed(24, 64))
        add(MaterialSecondaryTabRow(listOf("Overview", "Files", "Activity"), stateOf(1), width = 500f).placed(24, 132))
        add(MaterialNavigationBar(listOf("Home", "Search", "Settings"), navIndex, width = 620f).placed(24, 214))
        add(MaterialNavigationRail(listOf("Home", "Search", "Settings"), navIndex, height = 300f).placed(760, 62))
        add(MaterialSegmentedButtonRow(listOf("All", "Unread", "Starred"), stateOf(0), width = 300f).placed(860, 90))
    }

    ui.section("Snackbar - Banner - Tooltip - Snackbar Host", 330f) {
        add(MaterialSnackbar(stateOf("File saved"), width = 520f, actionLabel = "Undo", withDismissAction = true).placed(24, 62))
        add(MaterialBanner(stateOf("Offline mode"), width = 520f, actionLabel = "Retry", supportingText = "Actions resume when connected").placed(24, 132))
        add(MaterialTooltip(stateOf(true), "MaterialTooltip", width = 260f, height = 100f, anchorX = 80f, anchorY = 62f).placed(600, 56))
        val host = MaterialSnackbarHost(width = 430f, height = 48f).placed(600, 188)
        host.durationMs = 12_000L
        host.show("SnackbarHost queued message", "OK", "demo")
        add(host)
    }

    ui.section("Lists - Expansion Panel - Divider", 590f) {
        val list = MaterialList(width = 520f, tonal = true, density = MaterialListDensity.Comfortable).placed(24, 60)
        list.add(MaterialListSubheader("Today"))
        list.add(MaterialListItem("Inbox", "3 unread", "I", "3", stateOf(true), showDivider = true))
        list.add(MaterialCheckboxListTile("Downloaded", stateOf(true), "Available offline", "D", showDivider = true))
        list.add(MaterialRadioListTile("Wi-Fi sync", stateOf("wifi"), "wifi", "Recommended", "W"))
        list.add(MaterialSwitchListTile("Notifications", stateOf(true), "Mentions only", "!"))
        add(list)
        add(MaterialDivider(width = 520f).placed(610, 78))
        add(MaterialListSubheader("Standalone list components", width = 520f).placed(610, 96))
        add(MaterialListItem("MaterialListItem", "Supporting text", "A", "12", stateOf(false)).placed(610, 140))
        val expansion = MaterialExpansionPanel("Appearance", "Theme, density and type", stateOf(true), width = 520f).placed(610, 250)
        expansion.add(Label("Expanded MaterialExpansionPanel content", width = 460f, height = 24f, size = 14f).placed(18, 54))
        add(expansion)
    }

    ui.section("Search - Carousel", 430f) {
        add(MaterialSearchBar(stateOf(""), stateOf(false), width = 520f, placeholder = "SearchBar...").placed(24, 64))
        add(MaterialCarousel(
            listOf(
                MaterialCarouselItem("Framework animation", "Spring + Tween", "Target-driven OnDemand animation", "Featured"),
                MaterialCarouselItem("Material 3", "All components", "Native Skia rendering", "Components"),
                MaterialCarouselItem("CJK fonts", "HarmonyOS Sans SC", "Per-codepoint fallback", "Typography")
            ),
            stateOf(0), width = 540f, height = 280f
        ).placed(610, 64))
    }

    ui.section("Menus - Dialogs - Sheets - Pickers - Drawers", 520f) {
        var x = 24f
        var y = 66f
        fun launch(label: String, state: State<Boolean>, overlay: Component) {
            overlays += overlay
            add(MaterialButton(label, width = 200f, variant = MaterialButtonStyle.Outlined) { state.value = true }.placed(x, y))
            x += 220f
            if (x > 900f) { x = 24f; y += 58f }
        }

        val dropdown = stateOf(false)
        launch("Dropdown Menu", dropdown, MaterialDropdownMenu(dropdown, menuItems(), width = DemoWidth.toFloat(), height = DemoHeight.toFloat(), menuX = 470f, menuY = 160f, showDividers = true))
        val context = stateOf(false)
        launch("Context Menu", context, MaterialContextMenu(context, menuItems(), width = DemoWidth.toFloat(), height = DemoHeight.toFloat(), anchorX = 560f, anchorY = 220f, showDividers = true))
        val cascade = stateOf(false)
        launch("Cascading Menu", cascade, MaterialCascadingMenu(cascade, cascadingItems(), width = DemoWidth.toFloat(), height = DemoHeight.toFloat(), menuX = 430f, menuY = 150f))

        val dialog = stateOf(false)
        launch("Dialog", dialog, MaterialDialog(dialog, "MaterialDialog", "Base dialog", width = DemoWidth.toFloat(), height = DemoHeight.toFloat()))
        val alert = stateOf(false)
        launch("Alert Dialog", alert, MaterialAlertDialog(alert, "Delete draft?", "This cannot be undone", width = DemoWidth.toFloat(), height = DemoHeight.toFloat()))
        val basic = stateOf(false)
        launch("Basic Dialog", basic, MaterialBasicDialog(basic, "Basic Dialog", "Click outside to dismiss", width = DemoWidth.toFloat(), height = DemoHeight.toFloat()))
        val confirmation = stateOf(false)
        launch("Confirmation", confirmation, MaterialConfirmationDialog(confirmation, "Confirm action", "Continue?", width = DemoWidth.toFloat(), height = DemoHeight.toFloat()))

        val bottom = stateOf(false)
        launch("Bottom Sheet", bottom, MaterialBottomSheet(bottom, "Bottom Sheet", "Base bottom sheet", width = DemoWidth.toFloat(), height = DemoHeight.toFloat()))
        val modalBottom = stateOf(false)
        launch("Modal Bottom Sheet", modalBottom, MaterialModalBottomSheet(modalBottom, "Modal Bottom", "Modal sheet", width = DemoWidth.toFloat(), height = DemoHeight.toFloat()))
        val standardBottom = stateOf(false)
        launch("Standard Bottom", standardBottom, MaterialStandardBottomSheet(standardBottom, "Standard Bottom", "Standard sheet", width = DemoWidth.toFloat(), height = DemoHeight.toFloat()))
        val side = stateOf(false)
        launch("Side Sheet", side, MaterialSideSheet(side, "Side Sheet", "Modal side sheet", width = DemoWidth.toFloat(), height = DemoHeight.toFloat()))
        val standardSide = stateOf(false)
        launch("Standard Side", standardSide, MaterialStandardSideSheet(standardSide, "Standard Side", "Standard side sheet", width = DemoWidth.toFloat(), height = DemoHeight.toFloat()))

        val date = stateOf(false)
        launch("Date Picker", date, MaterialDatePicker(stateOf("2026-07-19"), date, width = DemoWidth.toFloat(), height = DemoHeight.toFloat(), title = "Select date"))
        val time = stateOf(false)
        launch("Time Picker", time, MaterialTimePicker(stateOf("12:30"), time, width = DemoWidth.toFloat(), height = DemoHeight.toFloat(), title = "Select time", use24Hour = false))
        val dateRange = stateOf(false)
        launch("Date Range Picker", dateRange, MaterialDateRangePicker(stateOf("2026-07-19"), stateOf("2026-07-25"), dateRange, width = DemoWidth.toFloat(), height = DemoHeight.toFloat()))
        val searchView = stateOf(false)
        launch("Search View", searchView, MaterialSearchView(stateOf(""), searchView, listOf("Ananda", "Material 3", "Animation"), width = DemoWidth.toFloat(), height = DemoHeight.toFloat(), title = "SearchView"))

        drawerLaunchers().forEach { (label, factory) ->
            val visible = stateOf(false)
            launch(label, visible, factory(visible))
        }
    }

    ui.finishEntrance()
}

private class Gallery(private val content: Component) {
    private var y = 24f
    private val sections = mutableListOf<Component>()

    fun section(title: String, height: Float, block: MaterialCard.() -> Unit) {
        val card = MaterialCard(width = SectionWidth, height = height, variant = MaterialCardStyle.Filled).placed(50, y)
        card.add(Label(title, width = SectionWidth - 48f, height = 30f, size = 20f).placed(24, 18))
        card.block()
        content.add(card)
        sections += card
        y += height + 18f
    }

    fun finishEntrance() {
        sections.forEachIndexed { index, section ->
            section.animateEnter(EnterTransition(slideY = 18f, delaySeconds = index * 0.025f, initialScale = 0.99f))
        }
    }
}

private fun actions() = listOf(MaterialActionItem("?"), MaterialActionItem("..."))

private fun menuItems() = listOf(
    MaterialMenuSection("Actions"),
    MaterialMenuItem("Refresh", leadingLabel = "R"),
    MaterialMenuItem("Pin", supportingText = "Keep at the top", trailingLabel = "P"),
    MaterialMenuDivider(),
    MaterialMenuItem("Delete", destructive = true, leadingLabel = "X")
)

private fun cascadingItems() = listOf(
    MaterialCascadingMenuItem("Sort by", children = listOf(MaterialCascadingMenuItem("Date"), MaterialCascadingMenuItem("Name"))),
    MaterialCascadingMenuItem("Move to", children = listOf(MaterialCascadingMenuItem("Archive"), MaterialCascadingMenuItem("Trash", destructive = true)))
)

private fun drawerItems() = listOf(
    MaterialDrawerItem("Inbox", "9", "Updates and replies"),
    MaterialDrawerItem("Starred", supportingText = "Pinned for later"),
    MaterialDrawerItem("Archived")
)

private fun drawerLaunchers(): List<Pair<String, (State<Boolean>) -> Component>> = listOf(
    "Navigation Drawer" to { state -> MaterialNavigationDrawer(state, drawerItems(), stateOf(0), width = DemoWidth.toFloat(), height = DemoHeight.toFloat(), title = "Navigation Drawer") },
    "Modal Drawer" to { state -> MaterialModalNavigationDrawer(state, drawerItems(), stateOf(0), width = DemoWidth.toFloat(), height = DemoHeight.toFloat(), title = "Modal Drawer") },
    "Dismissible Drawer" to { state -> MaterialDismissibleNavigationDrawer(state, drawerItems(), stateOf(0), width = DemoWidth.toFloat(), height = DemoHeight.toFloat(), title = "Dismissible Drawer") },
    "Permanent Drawer" to { state -> MaterialPermanentNavigationDrawer(state, drawerItems(), stateOf(0), width = DemoWidth.toFloat(), height = DemoHeight.toFloat(), title = "Permanent Drawer") }
)

private fun <T : Component> T.placed(x: Number, y: Number): T = apply { at(x, y) }
