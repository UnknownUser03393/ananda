package dev.unknownuser.ananda.debug

import dev.unknownuser.ananda.backend.RenderBackend
import dev.unknownuser.ananda.backend.Stroke
import dev.unknownuser.ananda.backend.TextStyle
import java.awt.Color
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

class DebuggerBridge(private val port: Int = 54231) : AutoCloseable {
    private val clients = CopyOnWriteArrayList<PrintWriter>()
    private var server: ServerSocket? = null
    private var running = false

    fun start(): DebuggerBridge {
        if (running) return this
        running = true
        server = ServerSocket(port)
        thread(name = "ananda-debugger-bridge", isDaemon = true) {
            while (running) {
                runCatching {
                    val socket = server?.accept() ?: return@thread
                    register(socket)
                }
            }
        }
        return this
    }

    fun publish(message: String) {
        val line = message.replace('\n', ' ')
        clients.forEach { client ->
            runCatching {
                client.println(line)
                client.flush()
            }.onFailure {
                clients -= client
                client.close()
            }
        }
    }

    override fun close() {
        running = false
        clients.forEach { it.close() }
        clients.clear()
        server?.close()
        server = null
    }

    private fun register(socket: Socket) {
        val writer = PrintWriter(socket.getOutputStream(), true)
        clients += writer
        writer.println("""{"type":"hello","source":"ananda","port":$port}""")
    }
}

class DebugRenderBackend(
    private val delegate: RenderBackend,
    private val bridge: DebuggerBridge
) : RenderBackend by delegate {
    override fun clear(argb: Int) {
        bridge.publish("""{"op":"clear","argb":$argb}""")
        delegate.clear(argb)
    }

    override fun drawRect(x: Float, y: Float, width: Float, height: Float, fill: Color?, stroke: Stroke?) {
        bridge.publish("""{"op":"rect","x":$x,"y":$y,"w":$width,"h":$height}""")
        delegate.drawRect(x, y, width, height, fill, stroke)
    }

    override fun drawRoundedRect(x: Float, y: Float, width: Float, height: Float, radius: Float, fill: Color?, stroke: Stroke?) {
        bridge.publish("""{"op":"rrect","x":$x,"y":$y,"w":$width,"h":$height,"r":$radius}""")
        delegate.drawRoundedRect(x, y, width, height, radius, fill, stroke)
    }

    override fun drawCircle(x: Float, y: Float, radius: Float, fill: Color?, stroke: Stroke?) {
        bridge.publish("""{"op":"circle","x":$x,"y":$y,"r":$radius}""")
        delegate.drawCircle(x, y, radius, fill, stroke)
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, stroke: Stroke) {
        bridge.publish("""{"op":"line","x1":$x1,"y1":$y1,"x2":$x2,"y2":$y2}""")
        delegate.drawLine(x1, y1, x2, y2, stroke)
    }

    override fun drawText(text: String, x: Float, y: Float, style: TextStyle) {
        bridge.publish("""{"op":"text","x":$x,"y":$y,"text":"${text.escape()}"}""")
        delegate.drawText(text, x, y, style)
    }

    override fun translated(x: Float, y: Float, block: () -> Unit) {
        bridge.publish("""{"op":"translate","x":$x,"y":$y}""")
        delegate.translated(x, y, block)
    }
}

private fun String.escape(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")
