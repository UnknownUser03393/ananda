package dev.unknownuser.ananda.backend

import dev.unknownuser.ananda.draw.toSkiaColor
import dev.unknownuser.ananda.theme.DefaultFontFamily
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Data
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontEdging
import org.jetbrains.skia.FontHinting
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.GradientStyle
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Rect
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Shader
import org.jetbrains.skia.Typeface
import org.jetbrains.skia.svg.SVGDOM
import java.awt.Color
import java.nio.file.InvalidPathException
import java.nio.file.Files
import java.nio.file.Path as NioPath
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.math.max
import kotlin.math.min

class SkiaRenderBackend(val canvas: Canvas) : RenderBackend {
    private val images = mutableMapOf<String, Image>()
    private val svgImages = mutableMapOf<String, SVGDOM>()
    private val textureRoots = mutableListOf<NioPath>()
    private val missingTextures = mutableSetOf<String>()
    private val registeredTypefaces = mutableMapOf<FontRegistrationKey, Typeface>()

    fun registerTexture(id: String, image: Image) {
        images[id] = image
    }

    fun registerSvgTexture(id: String, bytes: ByteArray) {
        svgImages[id] = SVGDOM(Data.makeFromBytes(bytes))
    }

    fun registerTextureFromFile(id: String, path: NioPath) {
        val bytes = Files.readAllBytes(path)
        if (path.extension.equals("svg", ignoreCase = true)) {
            registerSvgTexture(id, bytes)
        } else {
            registerTexture(id, Image.makeFromEncoded(bytes))
        }
    }

    fun registerTextureRoot(root: NioPath) {
        textureRoots.add(root)
    }

    fun registerTypeface(fontFamily: String, typeface: Typeface, weight: Int = 400) {
        registeredTypefaces[FontRegistrationKey(normalizeFontFamily(fontFamily), normalizeFontWeight(weight))] = typeface
        clearFontCaches()
    }

    fun registerFontFromFile(fontFamily: String, path: NioPath, weight: Int = 400) {
        val typeface = FontMgr.default.makeFromFile(path.toAbsolutePath().toString(), 0) ?: return
        registerTypeface(fontFamily, typeface, weight)
    }

    fun registerFontDirectory(root: NioPath, family: String? = null) {
        if (!root.exists()) return
        Files.walk(root).use { paths ->
            paths.filter { it.isRegularFile() && it.extension.lowercase() in FontFileExtensions }
                .forEach { path ->
                    registerFontFromFile(
                        family ?: inferFontFamily(path),
                        path,
                        inferFontWeight(path)
                    )
                }
        }
    }

    override fun clear(argb: Int) {
        canvas.clear(argb)
    }

    override fun drawRect(x: Float, y: Float, width: Float, height: Float, fill: Color?, stroke: Stroke?) {
        val rect = Rect.makeXYWH(x, y, width, height)
        fillPaint(fill)?.use { canvas.drawRect(rect, it) }
        strokePaint(stroke)?.use { canvas.drawRect(rect, it) }
    }

    override fun drawRoundedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        fill: Color?,
        stroke: Stroke?
    ) {
        val rrect = RRect.makeXYWH(x, y, width, height, radius)
        fillPaint(fill)?.use { canvas.drawRRect(rrect, it) }
        strokePaint(stroke)?.use { canvas.drawRRect(rrect, it) }
    }

    override fun drawRoundedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radii: CornerRadii,
        fill: Color?,
        stroke: Stroke?
    ) {
        if (radii.isUniform) {
            drawRoundedRect(x, y, width, height, radii.topLeft, fill, stroke)
            return
        }
        val rrect = RRect.makeComplexLTRB(
            x,
            y,
            x + width,
            y + height,
            floatArrayOf(
                radii.topLeft, radii.topLeft,
                radii.topRight, radii.topRight,
                radii.bottomRight, radii.bottomRight,
                radii.bottomLeft, radii.bottomLeft
            )
        )
        fillPaint(fill)?.use { canvas.drawRRect(rrect, it) }
        strokePaint(stroke)?.use { canvas.drawRRect(rrect, it) }
    }

    override fun drawFrostedRoundedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        blurRadius: Float,
        tint: Color,
        stroke: Stroke?
    ) {
        if (blurRadius <= 0f) {
            drawRoundedRect(x, y, width, height, radius, tint, stroke)
            return
        }

        val rrect = RRect.makeXYWH(x, y, width, height, radius)
        val margin = blurRadius * 2.5f
        val bounds = Rect.makeXYWH(x - margin, y - margin, width + margin * 2f, height + margin * 2f)
        val backdrop = ImageFilter.makeBlur(blurRadius, blurRadius, FilterTileMode.CLAMP)

        canvas.save()
        canvas.clipRRect(rrect, true)
        canvas.saveLayer(
            Canvas.SaveLayerRec(
                bounds = bounds,
                backdrop = backdrop,
                saveLayerFlags = Canvas.SaveLayerFlags(Canvas.SaveLayerFlagsSet.InitWithPrevious)
            )
        )
        fillPaint(tint)?.use { canvas.drawRRect(rrect, it) }
        canvas.restore()
        strokePaint(stroke)?.use { canvas.drawRRect(rrect, it) }
        canvas.restore()
    }

    fun drawPath(path: Path, fill: Color? = null, stroke: Stroke? = null) {
        fillPaint(fill)?.use { canvas.drawPath(path, it) }
        strokePaint(stroke)?.use { canvas.drawPath(path, it) }
    }

    override fun drawCircle(x: Float, y: Float, radius: Float, fill: Color?, stroke: Stroke?) {
        fillPaint(fill)?.use { canvas.drawCircle(x, y, radius, it) }
        strokePaint(stroke)?.use { canvas.drawCircle(x, y, radius, it) }
    }

    override fun drawArc(x: Float, y: Float, radius: Float, startAngle: Float, sweepAngle: Float, stroke: Stroke) {
        strokePaint(stroke)?.use { paint ->
            canvas.drawArc(
                x - radius,
                y - radius,
                x + radius,
                y + radius,
                Math.toDegrees(startAngle.toDouble()).toFloat(),
                Math.toDegrees(sweepAngle.toDouble()).toFloat(),
                false,
                paint
            )
        }
    }

    override fun drawRoundedGradientRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        colors: List<Color>,
        direction: GradientDirection,
        stroke: Stroke?
    ) {
        drawRoundedGradientStops(x, y, width, height, radius, colors.map { it to null }, direction, stroke)
    }

    override fun drawRoundedGradientStops(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        stops: List<Pair<Color, Float?>>,
        direction: GradientDirection,
        stroke: Stroke?
    ) {
        val resolvedStops = stops.takeIf { it.isNotEmpty() } ?: return
        if (resolvedStops.size == 1) {
            drawRoundedRect(x, y, width, height, radius, resolvedStops.first().first, stroke)
            return
        }

        val (x1, y1, x2, y2) = gradientLine(x, y, width, height, direction)
        val colors = resolvedStops.map { it.first.toSkiaColor() }.toIntArray()
        val positions = resolvedStops.mapIndexed { index, stop ->
            stop.second ?: index / (resolvedStops.size - 1).toFloat().coerceAtLeast(1f)
        }.toFloatArray()
        Paint().use { paint ->
            paint.mode = PaintMode.FILL
            paint.isAntiAlias = true
            paint.shader = Shader.makeLinearGradient(
                x1,
                y1,
                x2,
                y2,
                colors,
                positions,
                GradientStyle.DEFAULT
            )
            canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint)
        }
        strokePaint(stroke)?.use { canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), it) }
    }

    override fun drawRadialGradientCircle(
        x: Float,
        y: Float,
        radius: Float,
        colors: List<Color>,
        stroke: Stroke?
    ) {
        val resolvedColors = colors.takeIf { it.isNotEmpty() } ?: return
        if (resolvedColors.size == 1) {
            drawCircle(x, y, radius, resolvedColors.first(), stroke)
            return
        }

        Paint().use { paint ->
            paint.mode = PaintMode.FILL
            paint.isAntiAlias = true
            paint.shader = Shader.makeRadialGradient(
                x,
                y,
                radius,
                resolvedColors.map { it.toSkiaColor() }.toIntArray(),
                null,
                GradientStyle.DEFAULT
            )
            canvas.drawCircle(x, y, radius, paint)
        }
        strokePaint(stroke)?.use { canvas.drawCircle(x, y, radius, it) }
    }

    override fun drawRoundedGradientOutline(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        strokeWidth: Float,
        colors: List<Color>,
        direction: GradientDirection
    ) {
        val resolvedColors = colors.takeIf { it.isNotEmpty() && strokeWidth > 0f } ?: return
        gradientStrokePaint(x, y, width, height, strokeWidth, resolvedColors, direction)?.use { paint ->
            canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint)
        }
    }

    override fun drawGlowingRoundedGradientOutline(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        strokeWidth: Float,
        colors: List<Color>,
        blurRadius: Float,
        direction: GradientDirection
    ) {
        val resolvedColors = colors.takeIf { it.isNotEmpty() && strokeWidth > 0f && blurRadius > 0f } ?: return
        val margin = blurRadius * 3f + strokeWidth
        val bounds = Rect.makeXYWH(x - margin, y - margin, width + margin * 2f, height + margin * 2f)
        drawBlurredAdditiveLayer(bounds, blurRadius) {
            gradientStrokePaint(x, y, width, height, strokeWidth, resolvedColors, direction)?.use { paint ->
                canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint)
            }
        }
    }

    override fun drawShadowedRoundedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        shadow: Shadow,
        fill: Color?,
        stroke: Stroke?
    ) {
        if (shadow.inset) {
            drawInsetShadowRoundedRect(x, y, width, height, radius, shadow)
            if (fill != null || stroke != null) {
                drawRoundedRect(x, y, width, height, radius, fill, stroke)
            }
            return
        }
        shadowPaint(shadow)?.use { paint ->
            val spread = shadow.spread.coerceAtLeast(0f)
            canvas.drawRRect(
                RRect.makeXYWH(
                    x + shadow.offsetX - spread,
                    y + shadow.offsetY - spread,
                    width + spread * 2f,
                    height + spread * 2f,
                    radius + spread
                ),
                paint
            )
        }
        if (fill != null || stroke != null) {
            drawRoundedRect(x, y, width, height, radius, fill, stroke)
        }
    }

    private fun drawInsetShadowRoundedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        shadow: Shadow
    ) {
        if (shadow.color.alpha <= 0) return
        val spread = shadow.spread.coerceAtLeast(0f)
        val rrect = RRect.makeXYWH(
            x + spread,
            y + spread,
            max(0f, width - spread * 2f),
            max(0f, height - spread * 2f),
            max(0f, radius - spread)
        )
        val margin = shadow.blurRadius * 2.5f + spread
        val bounds = Rect.makeXYWH(x - margin, y - margin, width + margin * 2f, height + margin * 2f)
        val blur = if (shadow.blurRadius > 0f) {
            ImageFilter.makeBlur(shadow.blurRadius, shadow.blurRadius, FilterTileMode.CLAMP)
        } else {
            null
        }

        canvas.save()
        canvas.clipRRect(rrect, true)
        canvas.saveLayer(
            Canvas.SaveLayerRec(
                bounds = bounds,
                paint = Paint().apply {
                    mode = PaintMode.FILL
                    color = shadow.color.toSkiaColor()
                    if (blur != null) imageFilter = blur
                },
                saveLayerFlags = Canvas.SaveLayerFlags(Canvas.SaveLayerFlagsSet.InitWithPrevious)
            )
        )
        canvas.drawRRect(
            RRect.makeXYWH(
                x + shadow.offsetX,
                y + shadow.offsetY,
                width,
                height,
                radius
            ),
            Paint().apply {
                mode = PaintMode.FILL
                color = shadow.color.toSkiaColor()
            }
        )
        canvas.restore()
        canvas.restore()
    }

    override fun drawShadowedCircle(
        x: Float,
        y: Float,
        radius: Float,
        shadow: Shadow,
        fill: Color?,
        stroke: Stroke?
    ) {
        shadowPaint(shadow)?.use { paint ->
            canvas.drawCircle(
                x + shadow.offsetX,
                y + shadow.offsetY,
                radius + shadow.spread.coerceAtLeast(0f),
                paint
            )
        }
        if (fill != null || stroke != null) {
            drawCircle(x, y, radius, fill, stroke)
        }
    }

    override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, stroke: Stroke) {
        strokePaint(stroke)?.use { canvas.drawLine(x1, y1, x2, y2, it) }
    }

    override fun drawGlowingLine(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        stroke: Stroke,
        blurRadius: Float
    ) {
        if (blurRadius <= 0f) {
            drawLine(x1, y1, x2, y2, stroke)
            return
        }
        val margin = blurRadius * 3f + stroke.width
        val bounds = Rect.makeXYWH(
            min(x1, x2) - margin,
            min(y1, y2) - margin,
            kotlin.math.abs(x2 - x1) + margin * 2f,
            kotlin.math.abs(y2 - y1) + margin * 2f
        )
        drawBlurredAdditiveLayer(bounds, blurRadius) {
            glowSourceStrokePaint(stroke)?.use { canvas.drawLine(x1, y1, x2, y2, it) }
        }
    }

    override fun drawPolyline(points: List<ChartPoint>, stroke: Stroke, smooth: Boolean) {
        if (points.size < 2) return
        val path = if (smooth && points.size >= 3) smoothPolylinePath(points) else straightPolylinePath(points)
        strokePaint(stroke)?.use { canvas.drawPath(path, it) }
        path.close()
    }

    override fun drawGlowingPolyline(points: List<ChartPoint>, stroke: Stroke, blurRadius: Float, smooth: Boolean) {
        if (points.size < 2) return
        val margin = blurRadius * 3f + stroke.width
        val bounds = Rect.makeXYWH(
            points.minOf { it.x } - margin,
            points.minOf { it.y } - margin,
            (points.maxOf { it.x } - points.minOf { it.x }) + margin * 2f,
            (points.maxOf { it.y } - points.minOf { it.y }) + margin * 2f
        )
        val path = if (smooth && points.size >= 3) smoothPolylinePath(points) else straightPolylinePath(points)
        drawBlurredAdditiveLayer(bounds, blurRadius) {
            glowSourceStrokePaint(stroke)?.use { canvas.drawPath(path, it) }
        }
        path.close()
    }

    override fun drawText(text: String, x: Float, y: Float, style: TextStyle) {
        if (text.isEmpty()) return
        Paint().use { paint ->
            paint.color = style.color.toSkiaColor()
            var cursorX = x
            val runs = textRuns(text, style.fontFamily, style.fontWeight)
            runs.forEachIndexed { index, run ->
                Font(run.typeface, style.size).use { font ->
                    configureFont(font)
                    cursorX += drawRunText(run.text, cursorX, y, font, paint, style.letterSpacing)
                    if (style.letterSpacing != 0f && index < runs.lastIndex) cursorX += style.letterSpacing
                }
            }
        }
    }

    override fun measureText(text: String, style: TextStyle): Pair<Float, Float> {
        if (text.isEmpty()) return 0f to style.size * 1.4f
        val key = TextMeasureKey(text, style.size, style.fontFamily, style.fontWeight, style.letterSpacing)
        textMeasureCache[key]?.let { return it }
        Paint().use { paint ->
            var width = 0f
            textRuns(text, style.fontFamily, style.fontWeight).forEach { run ->
                Font(run.typeface, style.size).use { font ->
                    configureFont(font)
                    width += measureRunWidth(font, run.text, paint)
                }
            }
            width += style.letterSpacing * (text.codePointCount(0, text.length) - 1).coerceAtLeast(0)
            return (width to style.size * 1.4f).also { textMeasureCache[key] = it }
        }
    }

    override fun drawTexture(texture: TextureRegion, x: Float, y: Float, width: Float, height: Float, alpha: Float) {
        ensureTextureLoaded(texture.id)
        svgImages[texture.id]?.let { svg ->
            withAlpha(alpha) {
                clipped(x, y, width, height) {
                    translated(x, y) {
                        svg.setContainerSize(width, height)
                        svg.render(canvas)
                    }
                }
            }
            return
        }
        val image = images[texture.id] ?: return
        Paint().use { paint ->
            paint.alpha = (alpha.coerceIn(0f, 1f) * 255f).toInt()
            val source = Rect.makeXYWH(
                texture.u * image.width,
                texture.v * image.height,
                texture.width * image.width,
                texture.height * image.height
            )
            canvas.drawImageRect(image, source, Rect.makeXYWH(x, y, width, height), paint)
        }
    }

    private fun ensureTextureLoaded(id: String) {
        if (id in images || id in svgImages || id in missingTextures) return
        val path = resolveTexturePath(id)
        if (path == null) {
            missingTextures += id
            return
        }
        registerTextureFromFile(id, path)
    }

    private fun resolveTexturePath(id: String): NioPath? {
        val direct = try {
            NioPath.of(id)
        } catch (_: InvalidPathException) {
            null
        }
        if (direct != null && direct.exists() && direct.isRegularFile()) return direct
        val normalized = id.trimStart('/', '\\')
        return textureRoots
            .asSequence()
            .mapNotNull {
                try {
                    it.resolve(normalized)
                } catch (_: InvalidPathException) {
                    null
                }
            }
            .firstOrNull { it.exists() && it.isRegularFile() }
    }

    override fun clipped(x: Float, y: Float, width: Float, height: Float, block: () -> Unit) {
        canvas.save()
        try {
            canvas.clipRect(Rect.makeXYWH(x, y, width, height), true)
            block()
        } finally {
            canvas.restore()
        }
    }

    override fun translated(x: Float, y: Float, block: () -> Unit) {
        canvas.save()
        try {
            canvas.translate(x, y)
            block()
        } finally {
            canvas.restore()
        }
    }

    override fun scaled(scaleX: Float, scaleY: Float, block: () -> Unit) {
        canvas.save()
        try {
            canvas.scale(scaleX, scaleY)
            block()
        } finally {
            canvas.restore()
        }
    }

    override fun rotated(degrees: Float, pivotX: Float, pivotY: Float, block: () -> Unit) {
        canvas.save()
        try {
            canvas.rotate(degrees, pivotX, pivotY)
            block()
        } finally {
            canvas.restore()
        }
    }

    override fun withAlpha(alpha: Float, block: () -> Unit) {
        val normalized = alpha.coerceIn(0f, 1f)
        if (normalized >= 0.999f) {
            block()
            return
        }
        Paint().use { paint ->
            paint.alpha = (normalized * 255f).toInt()
            canvas.saveLayer(null, paint)
            try {
                block()
            } finally {
                canvas.restore()
            }
        }
    }

    private fun fillPaint(color: Color?): Paint? =
        color?.let {
            Paint().apply {
                mode = PaintMode.FILL
                this.color = it.toSkiaColor()
            }
        }

    private fun strokePaint(stroke: Stroke?): Paint? =
        stroke?.takeIf { it.width > 0f }?.let {
            Paint().apply {
                isAntiAlias = true
                mode = PaintMode.STROKE
                strokeWidth = it.width
                strokeCap = PaintStrokeCap.ROUND
                color = it.color.toSkiaColor()
            }
        }

    private fun gradientStrokePaint(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        strokeWidth: Float,
        colors: List<Color>,
        direction: GradientDirection
    ): Paint? =
        colors.takeIf { it.isNotEmpty() && strokeWidth > 0f }?.let { resolvedColors ->
            val (x1, y1, x2, y2) = gradientLine(x, y, width, height, direction)
            Paint().apply {
                mode = PaintMode.STROKE
                isAntiAlias = true
                this.strokeWidth = strokeWidth
                shader = Shader.makeLinearGradient(
                    x1,
                    y1,
                    x2,
                    y2,
                    resolvedColors.map { it.toSkiaColor() }.toIntArray(),
                    null,
                    GradientStyle.DEFAULT
                )
            }
        }

    private fun glowSourceStrokePaint(stroke: Stroke): Paint? =
        stroke.takeIf { it.width > 0f && it.color.alpha > 0 }?.let {
            Paint().apply {
                isAntiAlias = true
                mode = PaintMode.STROKE
                strokeWidth = it.width
                strokeCap = PaintStrokeCap.ROUND
                color = it.color.toSkiaColor()
            }
        }

    private fun drawBlurredAdditiveLayer(bounds: Rect, blurRadius: Float, block: () -> Unit) {
        Paint().use { layerPaint ->
            layerPaint.blendMode = BlendMode.PLUS
            layerPaint.imageFilter = ImageFilter.makeBlur(
                blurRadius,
                blurRadius,
                FilterTileMode.DECAL,
                null,
                null
            )
            canvas.saveLayer(bounds, layerPaint)
            try {
                block()
            } finally {
                canvas.restore()
            }
        }
    }

    private fun shadowPaint(shadow: Shadow): Paint? =
        shadow.color.takeIf { it.alpha > 0 }?.let {
            Paint().apply {
                mode = PaintMode.FILL
                isAntiAlias = true
                color = it.toSkiaColor()
                if (shadow.blurRadius > 0f) {
                    maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, shadow.blurRadius, true)
                }
            }
        }

    private fun gradientLine(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        direction: GradientDirection
    ): FloatArray =
        when (direction) {
            GradientDirection.Horizontal -> floatArrayOf(x, y + height / 2f, x + width, y + height / 2f)
            GradientDirection.Vertical -> floatArrayOf(x + width / 2f, y, x + width / 2f, y + height)
            GradientDirection.DiagonalDown -> floatArrayOf(x, y, x + width, y + height)
            GradientDirection.DiagonalUp -> floatArrayOf(x, y + height, x + width, y)
        }

    private fun smoothPolylinePath(points: List<ChartPoint>) =
        PathBuilder().apply {
            moveTo(points.first().x, points.first().y)
            for (index in 0 until points.lastIndex) {
                val current = points[index]
                val next = points[index + 1]
                val controlOffset = (next.x - current.x) * 0.5f
                cubicTo(
                    current.x + controlOffset,
                    current.y,
                    next.x - controlOffset,
                    next.y,
                    next.x,
                    next.y
                )
            }
        }.detach()

    private fun straightPolylinePath(points: List<ChartPoint>) =
        PathBuilder().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { point ->
                lineTo(point.x, point.y)
            }
        }.detach()

    private fun typeface(fontFamily: String, fontWeight: Int): Typeface {
        val weight = normalizeFontWeight(fontWeight)
        registeredTypeface(fontFamily, weight)?.let { return it }
        return typefaceCache.getOrPut(FontKey(normalizeFontFamily(fontFamily), weight)) {
            val style = FontStyle.NORMAL.withWeight(weight)
            FontMgr.default.matchFamilyStyle(fontFamily, style)
                ?: FontMgr.default.legacyMakeTypeface(fontFamily, style)
                ?: FontMgr.default.matchFamilyStyle(DefaultFontFamily, style)
                ?: FontMgr.default.legacyMakeTypeface("Segoe UI", style)
                ?: error("No Skia typeface is available for '$fontFamily'")
        }
    }

    private fun typefaceForCharacter(fontFamily: String, fontWeight: Int, codePoint: Int): Typeface {
        val weight = normalizeFontWeight(fontWeight)
        val cacheKey = CharacterFallbackKey(normalizeFontFamily(fontFamily), weight, codePoint)
        characterTypefaceCache[cacheKey]?.let { return it }
        val style = FontStyle.NORMAL.withWeight(weight)
        val resolved = registeredTypeface(fontFamily, weight)?.takeIf { it.getUTF32Glyph(codePoint) != 0.toShort() }
            ?: FontMgr.default.matchFamilyStyleCharacter(fontFamily, style, CjkLocales, codePoint)
            ?: FontMgr.default.matchFamiliesStyleCharacter(fallbackFamilies(fontFamily), style, CjkLocales, codePoint)
            ?: typeface(fontFamily, weight)
        characterTypefaceCache[cacheKey] = resolved
        return resolved
    }

    private fun textRuns(text: String, fontFamily: String, fontWeight: Int): List<ShapedTextRun> {
        if (text.isEmpty()) return emptyList()
        val runs = ArrayList<ShapedTextRun>()
        var runStart = 0
        var index = 0
        var currentTypeface = typefaceForCharacter(fontFamily, fontWeight, text.codePointAt(0))
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            val resolvedTypeface = typefaceForCharacter(fontFamily, fontWeight, codePoint)
            if (resolvedTypeface !== currentTypeface) {
                runs += ShapedTextRun(text.substring(runStart, index), currentTypeface)
                runStart = index
                currentTypeface = resolvedTypeface
            }
            index += Character.charCount(codePoint)
        }
        runs += ShapedTextRun(text.substring(runStart), currentTypeface)
        return runs
    }

    private fun configureFont(font: Font) {
        font.edging = FontEdging.SUBPIXEL_ANTI_ALIAS
        font.hinting = FontHinting.FULL
        font.isSubpixel = true
        font.isLinearMetrics = false
    }

    private fun drawRunText(text: String, x: Float, y: Float, font: Font, paint: Paint, letterSpacing: Float): Float {
        if (text.isEmpty()) return 0f
        if (letterSpacing == 0f) {
            canvas.drawString(text, x, y, font, paint)
            return font.measureTextWidth(text, paint)
        }
        var cursorX = x
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            val glyph = text.substring(index, index + Character.charCount(codePoint))
            canvas.drawString(glyph, cursorX, y, font, paint)
            cursorX += font.measureTextWidth(glyph, paint) + letterSpacing
            index += Character.charCount(codePoint)
        }
        return cursorX - x - letterSpacing
    }

    private fun measureRunWidth(font: Font, text: String, paint: Paint): Float {
        if (text.isEmpty()) return 0f
        return font.measureTextWidth(text, paint)
    }

    private fun fallbackFamilies(primaryFamily: String): Array<String?> =
        arrayOf(primaryFamily, DefaultFontFamily, "Source Han Sans SC", "Microsoft YaHei UI", "Microsoft YaHei", "SimHei", "Segoe UI")

    private fun registeredTypeface(fontFamily: String, fontWeight: Int): Typeface? {
        val family = normalizeFontFamily(fontFamily)
        val exact = registeredTypefaces[FontRegistrationKey(family, fontWeight)]
            ?: bundledTypefaces[FontRegistrationKey(family, fontWeight)]
        if (exact != null) return exact
        return (registeredTypefaces.asSequence() + bundledTypefaces.asSequence())
            .filter { (key, _) -> key.family == family }
            .minByOrNull { (key, _) -> kotlin.math.abs(key.weight - fontWeight) }
            ?.value
    }

    private data class ShapedTextRun(val text: String, val typeface: Typeface)

    private data class TextMeasureKey(
        val text: String,
        val size: Float,
        val fontFamily: String,
        val fontWeight: Int,
        val letterSpacing: Float
    )

    private data class CharacterFallbackKey(val fontFamily: String, val fontWeight: Int, val codePoint: Int)

    private data class FontKey(val family: String, val weight: Int)

    private data class FontRegistrationKey(val family: String, val weight: Int)

    companion object {
        private val CjkLocales = arrayOf("zh", "en")
        private val FontFileExtensions = setOf("ttf", "otf", "ttc", "otc")
        private val bundledTypefaces: Map<FontRegistrationKey, Typeface> by lazy {
            listOf(
                Triple(DefaultFontFamily, 300, "/assets/ananda/fonts/harmony_sc_light.ttf"),
                Triple(DefaultFontFamily, 400, "/assets/ananda/fonts/harmony_sc_regular.ttf"),
                Triple(DefaultFontFamily, 700, "/assets/ananda/fonts/harmony_sc_bold.ttf"),
                Triple("Source Han Sans SC", 400, "/assets/ananda/fonts/source_han_sans_sc.ttf")
            ).mapNotNull { (family, weight, resource) ->
                SkiaRenderBackend::class.java.getResourceAsStream(resource)?.use { stream ->
                    Data.makeFromBytes(stream.readAllBytes()).use { data ->
                        FontMgr.default.makeFromData(data, 0)?.let {
                            FontRegistrationKey(normalizeFontFamily(family), weight) to it
                        }
                    }
                }
            }.toMap()
        }
        private val typefaceCache = mutableMapOf<FontKey, Typeface>()
        private val characterTypefaceCache = object : LinkedHashMap<CharacterFallbackKey, Typeface>(512, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CharacterFallbackKey, Typeface>): Boolean =
                size > 4096
        }
        private val textMeasureCache = object : LinkedHashMap<TextMeasureKey, Pair<Float, Float>>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<TextMeasureKey, Pair<Float, Float>>): Boolean =
                size > 1024
        }

        private fun normalizeFontFamily(fontFamily: String): String =
            fontFamily.substringBefore(',').trim().trim('"', '\'').lowercase()

        private fun normalizeFontWeight(fontWeight: Int): Int =
            fontWeight.coerceIn(1, 1000)

        private fun inferFontFamily(path: NioPath): String {
            val name = path.fileName.toString().substringBeforeLast('.')
            return name.substringBefore('-').substringBefore('_')
        }

        private fun inferFontWeight(path: NioPath): Int {
            val name = path.fileName.toString().lowercase()
            return when {
                "thin" in name -> 100
                "extralight" in name || "ultralight" in name -> 200
                "light" in name -> 300
                "regular" in name || "normal" in name -> 400
                "medium" in name -> 500
                "semibold" in name || "demibold" in name -> 600
                "extrabold" in name || "ultrabold" in name -> 800
                "black" in name || "heavy" in name -> 900
                "bold" in name -> 700
                else -> 400
            }
        }

        private fun clearFontCaches() {
            typefaceCache.clear()
            characterTypefaceCache.clear()
            textMeasureCache.clear()
        }
    }
}
