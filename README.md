# Ananda

Ananda is a small Kotlin/JVM UI and drawing experiment backed by Skiko/Skia. The current codebase is intentionally compact: rendering, layout, events, animation, reactive state, themes, and controls are visible as plain Kotlin types.

## Core Model

- `Scene` owns top-level drawables, the event pipeline, animation ticks, focus, and the active `Theme`.
- `Component` is the base UI node. It supports lifecycle hooks, recursive measurement, layout, pointer hit testing, focus, interaction state, style, and children.
- `RenderBackend` abstracts drawing, including gradient fills, radial glows, and soft shadows. `SkiaRenderBackend` is the default backend.
- `WindowManagementProvider` abstracts window creation and lifecycle. `SkiaWindowManagementProvider` is the default Swing/Skiko implementation.
- `InteractionProvider` splits pointer, keyboard, text input, focused dispatch, focus management, and component interaction state behind small interfaces. `Scene` is the default implementation.
- `TimeSystem` tracks scaled and unscaled time, frame count, pause state, and timer callbacks.
- `FunctionalComponent` rebuilds its child tree when a read `State` changes or when the viewport changes.

## DSL

```kotlin
val count = stateOf(0)

val scene = scene {
    theme(Theme.Default)

    component(32f, 32f, 360f, 160f) {
        column(12f)
        button("Increment") {
            count.value += 1
        }
    }

    functional(32f, 220f, 360f, 80f) {
        label("Count: ${count.value}", width = 360f, height = 32f)
    }
}
```

Reusable functional components can be packaged as `FunctionalScope` extension functions:

```kotlin
import dev.unknownuser.ananda.annotations.FunctionalComponent
import dev.unknownuser.ananda.component.FunctionalScope
import dev.unknownuser.ananda.reactive.State

@FunctionalComponent
fun FunctionalScope.CounterLabel(count: State<Int>) {
    label("Count: ${count.value}", width = 240f, height = 32f)
}

val scene = scene {
    mount(32f, 32f, 240f, 32f) {
        CounterLabel(count)
    }
}
```

For a more composable-style DSL, return `ui { ... }` from a reusable function and mount it as a normal functional body:

```kotlin
import dev.unknownuser.ananda.annotations.functional
import dev.unknownuser.ananda.dsl.Button
import dev.unknownuser.ananda.dsl.Label
import dev.unknownuser.ananda.dsl.Ui
import dev.unknownuser.ananda.dsl.ui
import dev.unknownuser.ananda.reactive.inc

@functional
fun Counter(name: String): Ui = ui {
    var count = useState(0)

    Label("$name : $count") {
        size(200, 32)
    }
    Button("+") {
        offset(y = 38)
        onClick {
            count++
        }
    }
}

val scene = scene {
    mount(32f, 32f, 200f, 72f, Counter("Clicks"))
}
```

Kotlin requires `var` for `count++`, because `++` reassigns the local variable after calling `State.inc()`. The returned object is the same state instance, so remembered state is preserved.

## Layout And Measurement

Layout is split into recursive `measure` and `layout`.

- Measurement flows from parent constraints into children.
- Parent padding reduces child constraints.
- `StackLayout`, `RowLayout`, and `ColumnLayout` all measure children before placing them.
- Explicit `width` and `height` win; otherwise content size is used.
- Containers can be nested directly in `ComponentBuilder`/`FunctionalScope` with `row {}`, `column {}`, `box {}`, `flowRow {}`, and `adaptiveGrid {}`.
- `fillMaxWidth`, `fillMaxHeight`, `fillMaxSize`, and per-child `align` modifiers are available alongside the existing CSS grid and positioned layout APIs.

## Material 3

The `material3` package provides seed-generated light/dark themes and a Skia-native Material 3 component set. Its outlined text field includes an animated floating label, border notch, placeholder transition, IME/text editing, and a vertically aligned caret. HarmonyOS Sans SC and Source Han Sans SC are bundled for stable Chinese rendering while the Skia backend retains per-codepoint system fallback.

Run the interactive showcase with:

```powershell
.\gradlew.bat material3Demo
```

See `src/main/kotlin/dev/unknownuser/ananda/material3/README.md` for the component catalog.

## Input

The event pipeline currently supports:

- pointer: `pointerDown`, `pointerUp`, `pointerMove`
- pointer wheel: `pointerScroll`
- keyboard: `keyDown`, `keyUp`
- text input: `textInput`
- IME: `imeCompose`, `imeCommit`
- focus: `focus`, `blur`

Prefer typed matchers over string event names in component code:

```kotlin
on(PointerDown) {
    requestFocus()
    it.consume()
}

on(KeyDown.Enter or KeyDown.Space) {
    select()
    it.consume()
}
```

Input events are filtered against `Component.disabled` during dispatch. Lifecycle and focus events still dispatch normally.

`SkiaWindow` connects Swing key events and `InputMethodEvent` to Ananda events. Text controls should listen to `textInput` and IME events rather than assuming keyboard events produce text.
`Scene.focusNext()` provides keyboard focus traversal for host integrations such as Minecraft `Screen`.

## Themes And Styles

`Theme` contains `Palette`, `Typography`, `Spacing`, and a default control `Style`. A scene theme flows down the component tree, and any component may override it locally.

`Style` is a lightweight override object for background, foreground, border, padding, and text size. Controls use theme tokens by default and component state for hover, pressed, focused, disabled, and selected behavior.

## Controls

The first control set includes:

- `Panel`
- `Label`
- `Button`
- `TextField`
- `Checkbox`
- `ToggleSwitch`
- `RadioButton`
- `Slider`
- `ProgressBar`
- `Separator`
- `ScrollContainer`
- `TextureView`
- `ElevatedPanel`
- `GlowOrb`

These are minimal but wired through the same component, event, theme, and reactive-state systems as user components.

`ElevatedPanel` and `GlowOrb` are Skia-native visual-effect components. They are modeled after common client UI effects such as gradient rounded panels, blurred shadows, and glow layers, without requiring callers to manage shader passes directly.

`ScrollContainer` clips its children to bounds and handles `pointerScroll`.
`TextureView` draws a `TextureRegion` through the active backend, so desktop and Minecraft integrations can map the same UI node to different texture systems.

## Minecraft Integration

The core runtime includes a version-neutral `MinecraftGuiAdapter` in `dev.unknownuser.ananda.minecraft`.
It maps a Minecraft-style screen lifecycle into `Scene` rendering and input dispatch without taking a hard dependency on Fabric, Forge, NeoForge, or a specific Minecraft version.

See `docs/minecraft-adapter.md` for the loader-specific module shape and backend requirements.

## Rendering Modes

`SkiaWindow` supports:

- `RenderMode.Continuous`: requests frames every 16 ms.
- `RenderMode.OnDemand`: requests frames only when the scene is invalidated.

Continuous mode is useful while animation is active. On-demand mode avoids repainting static scenes.
`ManagedWindow` also exposes common lifecycle operations such as `requestRender`, `setTitle`, `resize`, `close`, and `dispose`.

Window creation can go through the provider boundary:

```kotlin
val window = SkiaWindowManagementProvider.createWindow(
    options = WindowOptions(title = "Ananda", width = 800, height = 600),
    scene = scene,
    interactions = scene
)
window.show()
```

## Animation

`Scene.animate` keeps the original progress callback API and now returns a controllable `Animation`:

```kotlin
val animation = scene.animate(
    durationSeconds = 0.6f,
    easing = Easings.EaseOutCubic,
    repeatCount = 1,
    repeatMode = RepeatMode.Reverse
) { progress ->
    component.x = 32f + progress * 120f
}

animation.pause()
animation.resume()
animation.cancel()
```

For simple numeric transitions, use `animateFloat`:

```kotlin
scene.animateFloat(0f, 1f, durationSeconds = 0.25f) { alpha ->
    // apply alpha
}
```

In `RenderMode.OnDemand`, active animations request the next render frame automatically.

## Time

Each `Scene` owns a `TimeSystem` exposed as `scene.time` and through the DSL:

```kotlin
val scene = scene {
    timeScale(1.5f)

    onUpdate { frame ->
        println("scaled=${frame.elapsedSeconds} unscaled=${frame.unscaledElapsedSeconds}")
    }

    every(intervalSeconds = 1f) {
        println("one scaled second")
    }

    after(delaySeconds = 3f, useScaledTime = false) {
        pauseTime()
    }
}
```

`RenderContext.time` and `FunctionalScope.time` expose the latest `TimeFrame`. Animations use scaled time, so `scene.time.pause()` and `scene.time.timeScale` affect animation playback.

## Debugger Bridge

`DebuggerBridge` opens a TCP socket and streams line-delimited JSON-like render events to connected debugger clients.

```kotlin
val bridge = DebuggerBridge(54231).start()
SkiaWindow(scene = scene, debuggerBridge = bridge).show()
```

Clients can connect to `localhost:54231` and receive operations such as `clear`, `rect`, `line`, `circle`, and `text`. This is deliberately dependency-free so it can evolve into a richer debugger protocol later.

## Verification

Run:

```powershell
.\gradlew.bat test
```
