package dev.unknownuser.ananda.material3

import dev.unknownuser.ananda.backend.RenderContext
import dev.unknownuser.ananda.component.Component

enum class MaterialSurfaceLevel { Lowest, Low, Default, High, Highest }

/** A tonal container without Card semantics or shadow elevation. */
class MaterialSurface(
    x: Float = 0f,
    y: Float = 0f,
    width: Float = 0f,
    height: Float = 0f,
    var level: MaterialSurfaceLevel = MaterialSurfaceLevel.Default,
    var shape: Float = MaterialShapes.Large
) : Component(x, y, width, height) {
    override fun draw(context: RenderContext) {
        val colors = context.theme.palette
        backgroundColor = when (level) {
            MaterialSurfaceLevel.Lowest -> colors.surfaceContainerLowest
            MaterialSurfaceLevel.Low -> colors.surfaceContainerLow
            MaterialSurfaceLevel.Default -> colors.surfaceContainer
            MaterialSurfaceLevel.High -> colors.surfaceContainerHigh
            MaterialSurfaceLevel.Highest -> colors.surfaceContainerHighest
        }
        cornerRadius = shape
        borderColor = null
        borderWidth = 0f
        elevationShadow = null
        super.draw(context)
    }
}
