package dev.unknownuser.ananda

import dev.unknownuser.ananda.backend.SkiaRenderBackend
import dev.unknownuser.ananda.backend.TextStyle
import dev.unknownuser.ananda.theme.DefaultFontFamily
import org.jetbrains.skia.Surface
import java.awt.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BundledCjkFontTest {
    @Test
    fun `default family and bundled CJK glyphs are available`() {
        assertEquals(DefaultFontFamily, TextStyle().fontFamily)
        assertNotNull(javaClass.getResource("/assets/ananda/fonts/harmony_sc_regular.ttf"))
        assertNotNull(javaClass.getResource("/assets/ananda/fonts/source_han_sans_sc.ttf"))

        Surface.makeRasterN32Premul(320, 80).use { surface ->
            val backend = SkiaRenderBackend(surface.canvas)
            val (width, height) = backend.measureText(
                "中文字体 Ananda",
                TextStyle(20f, Color.WHITE, DefaultFontFamily)
            )
            assertTrue(width > 80f)
            assertTrue(height > 0f)
        }
    }
}
