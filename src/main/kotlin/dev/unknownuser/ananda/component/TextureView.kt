package dev.unknownuser.ananda.component

import dev.unknownuser.ananda.backend.FramebufferRegion
import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.backend.TextureRegion

class TextureView(
    var texture: TextureRegion,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 0f,
    height: Float = 0f,
    var alpha: Float = 1f
) : Component(x, y, width, height) {
    override fun draw(context: RenderContext) {
        if (!visible) return
        applyStyle()
        measure(dev.unknownuser.ananda.layout.Constraints(width.takeIf { it > 0f } ?: context.width.toFloat(), height.takeIf { it > 0f } ?: context.height.toFloat()))
        context.backend.translated(x, y) {
            context.backend.withAlpha(opacity) {
                context.backend.drawTexture(texture, 0f, 0f, measuredWidth.takeIf { it > 0f } ?: width, measuredHeight.takeIf { it > 0f } ?: height, alpha)
                if (clipToBounds) {
                    context.backend.clipped(0f, 0f, measuredWidth, measuredHeight) {
                        children.sortedBy { it.zIndex }.forEach { it.draw(context) }
                    }
                } else {
                    children.sortedBy { it.zIndex }.forEach { it.draw(context) }
                }
            }
        }
    }
}

class FramebufferView(
    var framebuffer: FramebufferRegion,
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 0f,
    height: Float = 0f,
    var alpha: Float = 1f
) : Component(x, y, width, height) {
    override fun draw(context: RenderContext) {
        if (!visible) return
        applyStyle()
        measure(dev.unknownuser.ananda.layout.Constraints(width.takeIf { it > 0f } ?: context.width.toFloat(), height.takeIf { it > 0f } ?: context.height.toFloat()))
        context.backend.translated(x, y) {
            context.backend.withAlpha(opacity) {
                context.backend.drawFramebuffer(
                    framebuffer,
                    0f,
                    0f,
                    measuredWidth.takeIf { it > 0f } ?: width,
                    measuredHeight.takeIf { it > 0f } ?: height,
                    alpha
                )
                if (clipToBounds) {
                    context.backend.clipped(0f, 0f, measuredWidth, measuredHeight) {
                        children.sortedBy { it.zIndex }.forEach { it.draw(context) }
                    }
                } else {
                    children.sortedBy { it.zIndex }.forEach { it.draw(context) }
                }
            }
        }
    }
}
