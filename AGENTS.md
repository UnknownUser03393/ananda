# Repository Guidelines

## Project Structure & Module Organization

Ananda is a compact Kotlin/JVM UI and drawing experiment backed by Skiko/Skia. Main source code lives in `src/main/kotlin`. Core packages include `component`, `draw`, `backend`, `layout`, `event`, `reactive`, `theme`, `time`, `window`, and `minecraft`. Demo entry points are `src/main/kotlin/Main.kt` and `src/main/kotlin/MatcherDemo.kt`.

Tests live in `src/test/kotlin/dev/unknownuser/ananda`, with helpers such as `TestRenderBackend.kt`. Documentation belongs in `docs/`, DirectWrite JNI code in `native/directwrite/`, and helper scripts in `tools/`. Build outputs in `build/` are generated.

## Build, Test, and Development Commands

- `./gradlew.bat build`: compile, test, and assemble the project.
- `./gradlew.bat test`: run the Kotlin test suite on JUnit Platform.
- `./gradlew.bat run`: launch the main Skiko demo defined by `dev.unknownuser.MainKt`.
- `./gradlew.bat matcherDemo`: run the typed event matcher demo.
- `./gradlew.bat renderSkiaTextComparison`: render the Skia text comparison image.
- `./gradlew.bat buildDirectWriteJni`: build the DirectWrite JNI library. Requires CMake and Visual Studio 2022 x64 tools.

Use `./gradlew` instead of `./gradlew.bat` on Unix-like shells.

## Coding Style & Naming Conventions

Use Kotlin idioms and keep APIs small, explicit, and package-oriented. Match the existing style: 4-space indentation in Kotlin files, concise expression bodies where readable, and clear domain names such as `Scene`, `RenderBackend`, and `PointerEvent`. Classes and objects use `PascalCase`; functions, properties, and test methods use `camelCase`; private file-local constants currently use `PascalCase`.

Prefer typed event matchers such as `PointerDown` and `KeyDown.Enter` over string event names in new component code.

## Testing Guidelines

Tests use `kotlin.test` with JUnit Platform. Add tests beside related system tests in `src/test/kotlin/dev/unknownuser/ananda`, naming files after the feature or subsystem. Test methods should describe behavior, for example `animationCanPauseResumeAndCancel`. Run `./gradlew.bat test` before submitting changes, and add focused coverage when layout, event dispatch, reactive state, rendering backend contracts, or controls change.

## Commit & Pull Request Guidelines

This repository currently has no commit history, so no established commit convention is available. Use short, imperative commit subjects such as `Add slider interaction tests` or `Fix focus traversal order`.

Pull requests should include a concise description, verification commands, and screenshots or generated comparison images for visible rendering changes. Link related issues when available and call out native-toolchain requirements for DirectWrite changes.

## Security & Configuration Tips

Do not commit generated files from `build/`, IDE metadata, local Gradle caches, or machine-specific native build artifacts. Keep dependency and JVM toolchain changes in `build.gradle.kts` explicit.
