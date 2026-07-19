# Ananda Material 3

Material 3 is implemented directly on Ananda/Skia; it has no Android, Compose,
CSS, or browser runtime dependency.

## Theme

```kotlin
scene.theme = MaterialTheme.dark(primary = Color(150, 45, 45))
// MaterialTheme.light(...) is also available.
```

The normal Ananda palette fields remain available. Material semantic roles such
as `primaryContainer`, `onSurfaceVariant`, `surfaceContainerHigh`, `outline`,
and `errorContainer` live on the same `Palette` object.

## Components

```kotlin
scene.add(MaterialButton("Save", 20f, 20f, variant = MaterialButtonStyle.Filled) {
    save()
})

scene.add(MaterialCard(20f, 72f, 260f, 120f, MaterialCardStyle.Elevated).apply {
    add(Label("Material card", 16f, 12f))
})

val enabled = stateOf(false)
scene.add(MaterialSwitch(enabled, 20f, 208f))
scene.add(MaterialChip("Experimental", enabled, 84f, 208f))
scene.add(MaterialAssistChip("Reply", "↩", 188f, 208f))
scene.add(MaterialFilterChip("Unread", stateOf(true), x = 300f, y = 208f))
scene.add(MaterialInputChip("Alex", avatarLabel = "A", x = 20f, y = 248f))
scene.add(MaterialDivider(20f, 292f, 360f))

val text = stateOf("")
scene.add(MaterialTextField(text, 20f, 312f, label = "Name", placeholder = "Enter text"))
scene.add(MaterialTextArea(stateOf("Line 1\nLine 2"), 20f, 380f, 320f, 120f, label = "Notes"))
scene.add(MaterialCheckbox("Enabled", enabled, 20f, 516f))
scene.add(MaterialRadioButton(stateOf("A"), "A", "Option A", 20f, 564f))
scene.add(MaterialIconToggleButton("★", stateOf(true), 144f, 516f))
scene.add(MaterialSlider(stateOf(0.4f), 20f, 612f))
scene.add(MaterialRangeSlider(stateOf(0.2f), stateOf(0.8f), 20f, 656f, 280f))
scene.add(MaterialLinearProgressIndicator(stateOf(0.65f), 20f, 704f, 260f))
scene.add(MaterialIndeterminateLinearProgressIndicator(20f, 724f, 260f))
scene.add(MaterialCircularProgressIndicator(stateOf(0.72f), 296f, 686f))
scene.add(MaterialIndeterminateCircularProgressIndicator(296f, 736f))
scene.add(MaterialTopAppBar("Inbox", 0f, 0f, 360f, subtitle = "Today", navigationLabel = "≡", actions = listOf(MaterialActionItem("+") { compose() }))
scene.add(MaterialCenterAlignedTopAppBar("Mailbox", 0f, 72f, 360f, navigationLabel = "←", actions = listOf(MaterialActionItem("⋮") { more() }))
scene.add(MaterialMediumTopAppBar("Threads", 0f, 144f, 360f, navigationLabel = "←"))
scene.add(MaterialLargeTopAppBar("Library", 0f, 264f, 360f, subtitle = "Collections", actions = listOf(MaterialActionItem("⌕") { search() })))
scene.add(MaterialTabRow(listOf("Home", "Library", "Profile"), stateOf(0), 20f, 520f, 320f))
scene.add(MaterialSecondaryTabRow(listOf("Overview", "Files", "Activity"), stateOf(1), 20f, 572f, 320f))
scene.add(MaterialNavigationBar(listOf("Home", "Search", "Settings"), stateOf(0), 0f, 560f, 360f))
scene.add(MaterialFloatingActionButton("+", 284f, 620f) { compose() })
scene.add(MaterialSmallFloatingActionButton("+", 236f, 628f) { shrink() })
scene.add(MaterialLargeFloatingActionButton("+", 20f, 820f))
scene.add(MaterialExtendedFloatingActionButton("+", "Compose", 132f, 820f) { compose() })
scene.add(MaterialSpeedDial(stateOf(true), listOf(
    MaterialSpeedDialAction("New mail", iconLabel = "✉") { compose() },
    MaterialSpeedDialAction("Reminder", iconLabel = "⏰", supportingText = "Create a follow-up task") { remindLater() }
), "+", 120f, 620f, 220f, 220f))
scene.add(MaterialBottomAppBar(0f, 900f, 360f, 80f, navigationLabel = "≡", actions = listOf(MaterialActionItem("🔍") { search() }), fabLabel = "+"))

val dialogOpen = stateOf(true)
scene.add(MaterialAlertDialog(dialogOpen, "Delete draft?", "This action can't be undone.", 0f, 0f, 360f, 720f))

val menuOpen = stateOf(true)
scene.add(MaterialDropdownMenu(menuOpen, listOf(
    MaterialMenuSection("Actions"),
    MaterialMenuItem("Refresh", leadingLabel = "↻") { refresh() },
    MaterialMenuItem("Pin thread", supportingText = "Keep this conversation at the top", trailingLabel = "⌘P") { pin() },
    MaterialMenuDivider(),
    MaterialMenuItem("Delete", destructive = true, leadingLabel = "🗑") { removeSelected() }
), 0f, 0f, 360f, 720f, menuX = 132f, menuY = 88f, showDividers = true))
scene.add(MaterialContextMenu(stateOf(true), listOf(
    MaterialMenuItem("Copy", leadingLabel = "⧉") { copySelection() },
    MaterialMenuItem("Rename", leadingLabel = "✎") { renameSelection() },
    MaterialMenuDivider(),
    MaterialMenuItem("Delete", destructive = true, leadingLabel = "🗑") { removeSelected() }
), 0f, 0f, 360f, 720f, anchorX = 188f, anchorY = 132f))
scene.add(MaterialCascadingMenu(stateOf(true), listOf(
    MaterialCascadingMenuItem("Sort by", children = listOf(
        MaterialCascadingMenuItem("Date") { sortByDate() },
        MaterialCascadingMenuItem("Sender") { sortBySender() },
        MaterialCascadingMenuItem("Unread first") { sortByUnread() }
    )),
    MaterialCascadingMenuItem("Move to", children = listOf(
        MaterialCascadingMenuItem("Archive") { archiveSelection() },
        MaterialCascadingMenuItem("Spam", destructive = true) { markSpam() }
    ))
), 0f, 0f, 360f, 720f, menuX = 20f, menuY = 88f))
scene.add(MaterialExposedDropdownMenu(stateOf(1), stateOf(false), listOf("All", "Unread", "Starred"), 20f, 756f, 320f, 220f, label = "Mailbox"))

val tooltipOpen = stateOf(true)
scene.add(MaterialTooltip(tooltipOpen, "Quick action", 0f, 0f, 360f, 720f, anchorX = 220f, anchorY = 160f))
scene.add(MaterialSnackbar(stateOf("Message archived"), 20f, 872f, 320f, actionLabel = "Undo"))
scene.add(MaterialBanner(stateOf("Offline mode is enabled"), 20f, 920f, 320f, actionLabel = "Retry", supportingText = "Some actions may be delayed until the connection returns."))
scene.add(MaterialBottomSheet(stateOf(true), "Filters", "Adjust the currently visible results.", 0f, 0f, 360f, 720f))
scene.add(MaterialModalBottomSheet(stateOf(true), "Reply options", "Choose how this conversation should behave.", 0f, 0f, 360f, 720f))
scene.add(MaterialStandardBottomSheet(stateOf(true), "Queue", "This standard sheet can stay docked to the layout.", 0f, 0f, 360f, 720f, dismissLabel = ""))
scene.add(MaterialSideSheet(stateOf(true), "Details", "This modal side sheet anchors to the side of the viewport.", 0f, 0f, 360f, 720f))
scene.add(MaterialStandardSideSheet(stateOf(true), "Inspector", "This standard side sheet can stay docked beside content.", 0f, 0f, 360f, 720f, dismissLabel = ""))
scene.add(MaterialDatePicker(stateOf("2026-07-17"), stateOf(true), 0f, 0f, 360f, 720f))
scene.add(MaterialDateRangePicker(stateOf("2026-07-17"), stateOf("2026-07-21"), stateOf(true), 0f, 0f, 360f, 720f))
scene.add(MaterialCarousel(listOf(
    MaterialCarouselItem("Quarterly planning", supportingText = "Roadmap", body = "Review milestones, owners, and delivery windows for the next release.", label = "Featured") { openPlanning() },
    MaterialCarouselItem("Design review", supportingText = "Tomorrow 09:30", body = "Collect the latest mockups and annotate open questions before the sync.", label = "Upcoming") { openReview() },
    MaterialCarouselItem("Release checklist", supportingText = "Ops", body = "Walk through smoke tests, rollback notes, and communication steps.", label = "Ready") { openChecklist() }
), stateOf(1), 20f, 1040f, 320f, 220f))
scene.add(MaterialTimePicker(stateOf("12:30"), stateOf(true), 0f, 0f, 360f, 720f, use24Hour = false))
scene.add(MaterialList(20f, 600f, 320f, tonal = true, density = MaterialListDensity.Comfortable).apply {
    add(MaterialListSubheader("Today"))
    add(MaterialListItem("Inbox", "3 unread", "I", "3", stateOf(true), showDivider = true))
    add(MaterialCheckboxListTile("Downloaded", stateOf(true), "Available offline", "↓", showDivider = true))
    add(MaterialRadioListTile("Sync on Wi‑Fi", stateOf("wifi"), "wifi", "Recommended", "W"))
    add(MaterialSwitchListTile("Notifications", stateOf(true), "Alerts for mentions only", "!"))
})
scene.add(MaterialSegmentedButtonRow(listOf("All", "Unread", "Starred"), stateOf(0), 20f, 704f, 320f))
scene.add(MaterialSearchBar(stateOf(""), stateOf(false), 20f, 756f, 320f))
scene.add(MaterialAutocompleteField(stateOf(""), stateOf(false), listOf("Inbox", "Important", "Invoices", "Invites"), 20f, 824f, 320f, label = "Filter"))
scene.add(MaterialSearchView(stateOf(""), stateOf(true), listOf("Design review", "Release checklist", "Travel receipts"), 0f, 0f, 360f, 720f, title = "Recent searches", supportingText = "Suggestions can be filtered as you type."))
scene.add(MaterialBadge("9", 324f, 20f))
scene.add(MaterialNavigationDrawer(stateOf(true), listOf(
    MaterialDrawerItem("Inbox", "9", "Updates and replies"),
    MaterialDrawerItem("Starred", supportingText = "Pinned for later"),
    MaterialDrawerItem("Archived")
), stateOf(0), 0f, 0f, 360f, 720f, title = "Mail"))
scene.add(MaterialModalNavigationDrawer(stateOf(true), listOf(MaterialDrawerItem("Inbox"), MaterialDrawerItem("Starred")), stateOf(0), 0f, 0f, 360f, 720f, title = "Mail"))
scene.add(MaterialDismissibleNavigationDrawer(stateOf(true), listOf(MaterialDrawerItem("Inbox"), MaterialDrawerItem("Sent", supportingText = "Recently delivered")), stateOf(0), 0f, 0f, 360f, 720f, title = "Mail"))
scene.add(MaterialPermanentNavigationDrawer(stateOf(true), listOf(MaterialDrawerItem("Inbox"), MaterialDrawerItem("Sent")), stateOf(0), 0f, 0f, 360f, 720f, title = "Workspace"))

val settingsExpanded = stateOf(false)
val panel = MaterialExpansionPanel("Appearance", "Theme, density, and font size", settingsExpanded, 20f, 1080f, 320f)
panel.add(Label("Choose between light, dark, and system-default themes.", 16f, 48f))
scene.add(panel)

val snackbarHost = MaterialSnackbarHost(20f, 1140f, 320f, onAction = { id -> println("Action on $id") }, onDismiss = { id -> println("Dismissed $id") })
scene.add(snackbarHost)
snackbarHost.show("File saved", actionLabel = "Undo", id = "save")

val isRefreshing = stateOf(false)
val pullAmount = stateOf(0.45f)
scene.add(MaterialRefreshIndicator(isRefreshing, pullAmount, 180f, 1200f, 40f, 40f))
```

Buttons support `Filled`, `Tonal`, `Elevated`, `Outlined`, and `Text` variants.
Cards support `Filled`, `Elevated`, and `Outlined`. Text fields support `Filled`
and `Outlined` variants with label, placeholder, supporting text, keyboard
editing, selection, clipboard shortcuts, and IME compose/commit, and the layer
now also includes multi-line text areas, assist/suggestion/elevated/filter/input
chip variants, range sliders, exposed dropdown menus, bottom app bars,
small/large/extended floating action buttons, icon toggle buttons, and
determinate + indeterminate circular/linear progress indicators. The Material 3
layer also includes small, center-aligned, medium, and large top app bars; tab
rows including secondary styling; icon/floating action buttons and speed-dial
menus; snackbars and banners; bottom navigation bars; navigation rails;
navigation drawers including modal, dismissible, and permanent variants;
dropdown menus with sections, dividers, leading/trailing content, supporting
text, standalone context menus, and cascading submenus; dialogs including
alert/basic/confirmation variants; tooltips; bottom and side sheets including
modal and standard variants; date pickers with month/year navigation,
date-range selection, carousels, and time pickers with 12/24-hour
presentation; expansion panels with animated expand/collapse and body content;
snackbar hosts with queued message sequencing, action buttons, and auto-dismiss;
pull-to-refresh indicators driven by refresh state and pull progress; list
items, checkbox/radio/switch list tiles, subheaders, and list containers with
density control; segmented buttons; search bars, autocomplete fields, search
views, and badges. Interactive components use the existing Ananda pointer,
keyboard, focus, state, and animation systems.

Material components also declare their minimum visual sizes. For responsive
composition, use `FlowRowLayout`, `AdaptiveGridLayout`, the fill/min/max sizing
modifiers, and `windowSizeClass`; see `ananda/layout/README.md`.
