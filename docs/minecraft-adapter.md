# Minecraft Adapter Roadmap

Ananda now has a version-neutral Minecraft GUI boundary in
`dev.unknownuser.ananda.minecraft.MinecraftGuiAdapter`.

The adapter intentionally does not depend on Fabric, Forge, NeoForge, or a
specific Minecraft client package name. A loader-specific module should wrap it
from the real `Screen` implementation.

## Screen Mapping

Map Minecraft screen callbacks to `MinecraftGuiAdapter`:

- `render` -> create a `MinecraftGuiSurface`, then call `adapter.render(surface)`
- `mouseMoved` -> `adapter.mouseMoved`
- `mouseClicked` -> `adapter.mouseClicked`
- `mouseReleased` -> `adapter.mouseReleased`
- `mouseDragged` -> `adapter.mouseDragged`
- `mouseScrolled` -> `adapter.mouseScrolled`
- `keyPressed` -> `adapter.keyPressed`
- `keyReleased` -> `adapter.keyReleased`
- `charTyped` -> `adapter.charTyped`
- `removed` -> `adapter.removed`

## Backend Work Still Needed

A production Minecraft module still needs a concrete `RenderBackend` that binds
to the active Minecraft render context. That backend should implement:

- `translated` with the active matrix stack
- `clipped` with scissor coordinates adjusted for GUI scale
- shape rendering through Minecraft's draw APIs or a GL/Skia bridge
- `drawTexture` through resource locations or the active texture atlas
- `drawFramebuffer` for `FramebufferRegion` values backed by Minecraft color
  attachments
- text rendering through Minecraft's font renderer when resource-pack fidelity
  matters

## Framebuffer Integration

Core now exposes `FramebufferRegion` and `FramebufferView` without taking a
Minecraft dependency. Loader-specific code should adapt Minecraft framebuffer
objects into this value:

```kotlin
val region = FramebufferRegion(
    id = "minecraft:main",
    colorAttachment = framebuffer.colorAttachment,
    width = framebuffer.textureWidth,
    height = framebuffer.textureHeight,
    flipY = true
)
```

Then draw it from Ananda with either the DSL or component API:

```kotlin
scene {
    framebuffer(region, x = 16f, y = 16f, width = 160f, height = 90f)
}
```

The Minecraft backend owns the actual GL/Minecraft calls. For modern versions
this usually means binding the framebuffer color attachment as a texture,
emitting a textured quad through the active render pipeline, then restoring any
texture, shader, depth, blend, and framebuffer state it changed. `flipY`
defaults to `true` because OpenGL framebuffer texture coordinates are commonly
bottom-left oriented while GUI coordinates are top-left oriented.

## Packaging

Keep loader-specific code in a separate module, for example:

- `ananda-core`: current platform-independent runtime
- `ananda-minecraft-common`: shared adapter and backend contracts
- `ananda-minecraft-fabric`: Fabric screen/backend implementation
- `ananda-minecraft-neoforge`: NeoForge screen/backend implementation

This keeps the core runtime testable without launching Minecraft and isolates
version churn to thin adapter modules.
