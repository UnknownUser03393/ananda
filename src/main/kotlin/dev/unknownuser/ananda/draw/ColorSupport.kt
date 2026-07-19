package dev.unknownuser.ananda.draw

import java.awt.Color

fun Color.toSkiaColor(): Int =
    org.jetbrains.skia.Color.makeARGB(alpha, red, green, blue)
