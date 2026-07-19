package dev.unknownuser.ananda.material3

import java.awt.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MaterialThemeTest {
    @Test
    fun `dark scheme derives tonal roles from brand seed`() {
        val seed = Color(150, 45, 45)
        val colors = MaterialTheme.dark(seed).palette

        assertNotEquals(seed.rgb, colors.primary.rgb)
        assertNotEquals(colors.primary.rgb, colors.primaryContainer.rgb)
        assertTrue(contrast(colors.primary, colors.onPrimary) >= 4.5)
        assertTrue(contrast(colors.primaryContainer, colors.onPrimaryContainer) >= 4.5)
    }

    @Test
    fun `dark surface containers form an ordered tonal hierarchy`() {
        val colors = MaterialTheme.dark(Color(150, 45, 45)).palette
        val tones = listOf(
            colors.surfaceContainerLowest,
            colors.surfaceContainerLow,
            colors.surfaceContainer,
            colors.surfaceContainerHigh,
            colors.surfaceContainerHighest
        ).map(::luminance)

        assertTrue(tones.zipWithNext().all { (lower, higher) -> lower <= higher })
        assertTrue(tones.last() - tones.first() > 0.02)
    }

    private fun contrast(first: Color, second: Color): Double {
        val a = luminance(first)
        val b = luminance(second)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    private fun luminance(color: Color): Double {
        fun channel(value: Int): Double {
            val normalized = value / 255.0
            return if (normalized <= 0.04045) normalized / 12.92 else Math.pow((normalized + 0.055) / 1.055, 2.4)
        }
        return channel(color.red) * 0.2126 + channel(color.green) * 0.7152 + channel(color.blue) * 0.0722
    }
}
