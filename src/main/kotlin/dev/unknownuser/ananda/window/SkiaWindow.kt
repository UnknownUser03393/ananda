package dev.unknownuser.ananda.window

import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.ShapeInterpolatingRenderBackend
import dev.unknownuser.ananda.backend.SkiaRenderBackend
import dev.unknownuser.ananda.debug.DebugRenderBackend
import dev.unknownuser.ananda.debug.DebuggerBridge
import dev.unknownuser.ananda.draw.Scene
import dev.unknownuser.ananda.interaction.InteractionProvider
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PixelGeometry
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaLayerRenderDelegate
import org.jetbrains.skiko.SkikoRenderDelegate
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.concurrent.CountDownLatch
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.WindowConstants

class SkiaWindow(
    title: String = "Ananda",
    width: Int = 800,
    height: Int = 600,
    override val scene: Scene = Scene(),
    override val interactions: InteractionProvider = scene,
    private val clearColor: Int = org.jetbrains.skia.Color.makeARGB(255, 24, 26, 31),
    private val renderMode: RenderMode = RenderMode.Continuous,
    private val debuggerBridge: DebuggerBridge? = null
) : ManagedWindow {
    private val layer = SkiaLayer(pixelGeometry = PixelGeometry.RGB_H)
    private val closed = CountDownLatch(1)
    private lateinit var frame: JFrame
    private var renderTimer: Timer? = null
    private var lastRenderNanoTime: Long = 0L
    private var interpolationBackend: ShapeInterpolatingRenderBackend? = null
    @Volatile
    private var disposed = false

    override val isVisible: Boolean
        get() = !disposed && frame.isVisible

    override val size: WindowSize
        get() = WindowSize(frame.width, frame.height)

    init {
        layer.renderDelegate = SkiaLayerRenderDelegate(layer, object : SkikoRenderDelegate {
            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                val deltaSeconds = if (lastRenderNanoTime == 0L) 0f else (nanoTime - lastRenderNanoTime) / 1_000_000_000f
                lastRenderNanoTime = nanoTime
                scene.update(deltaSeconds)
                val backend = SkiaRenderBackend(canvas).let { skia ->
                    debuggerBridge?.let { DebugRenderBackend(skia, it) } ?: skia
                }
                val interpolatingBackend = interpolationBackend ?: ShapeInterpolatingRenderBackend(backend).also {
                    interpolationBackend = it
                }
                interpolatingBackend.beginFrame(backend, deltaSeconds)
                interpolatingBackend.clear(clearColor)
                scene.render(RenderContext(interpolatingBackend, width, height, nanoTime, time = scene.time.snapshot()))
                if (interpolatingBackend.endFrame()) {
                    scene.requestRender()
                }
            }
        })

        SwingUtilities.invokeAndWait {
            frame = JFrame(title).apply {
                defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
                preferredSize = Dimension(width, height)
                addWindowListener(object : WindowAdapter() {
                    override fun windowClosing(event: WindowEvent) {
                        disposed = true
                        renderTimer?.stop()
                    }

                    override fun windowClosed(event: WindowEvent) {
                        disposed = true
                        renderTimer?.stop()
                        closed.countDown()
                    }
                })
            }
            layer.attachTo(frame.contentPane)
            AwtInputAdapter(interactions).install(layer)
            frame.pack()
            frame.setLocationRelativeTo(null)
        }
    }

    fun scene(): Scene = scene

    override fun show(blocking: Boolean) {
        SwingUtilities.invokeLater {
            if (disposed) return@invokeLater
            frame.isVisible = true
            requestLayerRender()
            if (renderTimer == null) {
                renderTimer = Timer(16) {
                    if (renderMode == RenderMode.Continuous || scene.consumeRenderRequest()) {
                        requestLayerRender()
                    }
                }.also { it.start() }
            }
        }
        if (blocking) {
            closed.await()
        }
    }

    override fun requestRender() {
        scene.requestRender()
        SwingUtilities.invokeLater {
            requestLayerRender()
        }
    }

    override fun setTitle(title: String) {
        SwingUtilities.invokeLater {
            if (disposed) return@invokeLater
            frame.title = title
        }
    }

    override fun resize(width: Int, height: Int) {
        SwingUtilities.invokeLater {
            if (disposed) return@invokeLater
            frame.preferredSize = Dimension(width, height)
            frame.setSize(width, height)
            frame.revalidate()
            requestLayerRender()
        }
    }

    override fun close() {
        dispose()
    }

    override fun dispose() {
        SwingUtilities.invokeLater {
            disposed = true
            renderTimer?.stop()
            renderTimer = null
            frame.dispose()
        }
    }

    private fun requestLayerRender() {
        if (disposed || !frame.isDisplayable) {
            renderTimer?.stop()
            renderTimer = null
            return
        }
        try {
            layer.needRender()
        } catch (_: IllegalStateException) {
            disposed = true
            renderTimer?.stop()
            renderTimer = null
            closed.countDown()
        }
    }
}

enum class RenderMode {
    Continuous,
    OnDemand
}
