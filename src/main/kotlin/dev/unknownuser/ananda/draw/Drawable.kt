package dev.unknownuser.ananda.draw

import dev.unknownuser.ananda.backend.RenderContext

fun interface Drawable {
    fun draw(context: RenderContext)
}
