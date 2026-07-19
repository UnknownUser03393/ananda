package dev.unknownuser.ananda.window

import dev.unknownuser.ananda.debug.DebuggerBridge
import dev.unknownuser.ananda.draw.Scene
import dev.unknownuser.ananda.interaction.InteractionProvider

data class WindowOptions(
    val title: String = "Ananda",
    val width: Int = 800,
    val height: Int = 600,
    val clearColor: Int = 0xFF181A1Fu.toInt(),
    val renderMode: RenderMode = RenderMode.Continuous
)

data class WindowSize(
    val width: Int,
    val height: Int
)

interface ManagedWindow {
    val scene: Scene
    val interactions: InteractionProvider
    val isVisible: Boolean
    val size: WindowSize

    fun show(blocking: Boolean = true)

    fun requestRender()

    fun setTitle(title: String)

    fun resize(width: Int, height: Int)

    fun close()

    fun dispose()
}

interface WindowManagementProvider {
    fun createWindow(
        options: WindowOptions = WindowOptions(),
        scene: Scene = Scene(),
        interactions: InteractionProvider = scene,
        debuggerBridge: DebuggerBridge? = null
    ): ManagedWindow
}

object SkiaWindowManagementProvider : WindowManagementProvider {
    override fun createWindow(
        options: WindowOptions,
        scene: Scene,
        interactions: InteractionProvider,
        debuggerBridge: DebuggerBridge?
    ): ManagedWindow = SkiaWindow(
        title = options.title,
        width = options.width,
        height = options.height,
        scene = scene,
        interactions = interactions,
        clearColor = options.clearColor,
        renderMode = options.renderMode,
        debuggerBridge = debuggerBridge
    )
}
