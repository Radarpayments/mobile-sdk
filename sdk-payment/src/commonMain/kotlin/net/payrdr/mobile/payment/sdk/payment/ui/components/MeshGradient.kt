package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Animated mesh-gradient card background ("diagonal flow" shape of the approved design): coloured points
 * drift along Lissajous paths and every pixel blends them with weight 1/d^p, after a soft sine
 * distortion and a diagonal stream.
 * The algorithm follows the Paper Shaders mesh gradient (Apache-2.0, https://github.com/paper-design/shaders).
 *
 * The gradient is very smooth and slow, so it stays cheap:
 * - it is computed on the CPU into a small grid ([MESH_GRID_WIDTH] wide), doubled with Catmull-Rom
 *   interpolation (bilinear alone shows a staircase along thin ridges) and stretched with bilinear
 *   filtering: indistinguishable from a full-resolution render, identical on every platform and Android
 *   version (no runtime shaders needed);
 * - only keyframes [MESH_KEYFRAME_SECONDS] apart are computed, off the main thread; the frames between
 *   them are a cross-fade of two keyframes drawn by the GPU;
 * - the animation stands still while the card is off screen (scrolled away) or the app is in the
 *   background.
 *
 * @param palette 4 colours; the points of the shape cycle through them
 * @param animate false keeps a single still frame (reduce motion)
 */
@Composable
internal fun MeshGradientBackground(
    palette: List<Color>,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    var grid by remember { mutableStateOf(IntSize.Zero) }
    var visible by remember { mutableStateOf(true) }
    // `next` fades in over `current` as `blend` goes from 0 to 1.
    var current by remember { mutableStateOf<ImageBitmap?>(null) }
    var next by remember { mutableStateOf<ImageBitmap?>(null) }
    var blend by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(palette, animate, grid) {
        if (grid == IntSize.Zero) return@LaunchedEffect
        val (width, height) = grid

        suspend fun keyframe(index: Int): ImageBitmap =
            withContext(Dispatchers.Default) {
                val pixels = IntArray(width * height)
                MeshGradient.render(pixels, width, height, index * MESH_KEYFRAME_SECONDS * MESH_SPEED, palette)
                pixelsToImageBitmap(MeshGradient.upscale2x(pixels, width, height), width * 2, height * 2)
            }

        current = keyframe(0)
        next = null
        blend = 0f
        if (!animate) return@LaunchedEffect

        var index = 0
        next = keyframe(1)
        var upcoming = async { keyframe(2) } // computed while the current span plays
        var played = 0L
        var lastFrame = -1L
        var lastDrawn = -1L
        while (true) {
            if (!visible) {
                snapshotFlow { visible }.first { it }
                lastFrame = -1L
            }
            // Frames stop while the app is in the background, so the animation waits here.
            val now = withFrameNanos { it }
            if (lastFrame >= 0) played += (now - lastFrame).coerceAtMost(MAX_STEP_NANOS)
            lastFrame = now
            // Every 2nd frame at 60 Hz, every 4th at 120 Hz: an even rhythm. Everything visible changes
            // only on these frames, the keyframe switch included.
            if (lastDrawn >= 0 && now - lastDrawn < FRAME_INTERVAL_NANOS - VSYNC_SLACK_NANOS) continue
            lastDrawn = now
            if (played >= KEYFRAME_NANOS) {
                played = (played - KEYFRAME_NANOS).coerceAtMost(KEYFRAME_NANOS - 1)
                index++
                current = next
                next = upcoming.await()
                val following = index + 2
                upcoming = async { keyframe(following) }
            }
            blend = played.toFloat() / KEYFRAME_NANOS
        }
    }

    Canvas(
        modifier
            .onSizeChanged { size ->
                if (size.width > 0 && size.height > 0) {
                    val height = (MESH_GRID_WIDTH * size.height / size.width.toFloat()).roundToInt().coerceAtLeast(MIN_GRID_HEIGHT)
                    grid = IntSize(MESH_GRID_WIDTH, height)
                }
            }.onGloballyPositioned { coordinates ->
                // Clipped by the scroll viewport and the window: empty once the card is scrolled away.
                val bounds = coordinates.boundsInWindow()
                visible = bounds.width > 0f && bounds.height > 0f
            },
    ) {
        current?.let { drawStretched(it, alpha = 1f) }
        val fading = next
        if (fading != null && blend > 0f) drawStretched(fading, alpha = blend)
    }
}

/** Draws the small grid [image] over the whole canvas with bilinear filtering. */
private fun DrawScope.drawStretched(
    image: ImageBitmap,
    alpha: Float,
) {
    drawImage(
        image = image,
        srcSize = IntSize(image.width, image.height),
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        alpha = alpha,
        filterQuality = FilterQuality.Low,
    )
}

/** A new bitmap with ARGB [pixels]; safe to call off the main thread. */
internal expect fun pixelsToImageBitmap(
    pixels: IntArray,
    width: Int,
    height: Int,
): ImageBitmap

/**
 * One shape of the mesh: where the colour points travel and how space is bent before blending.
 * Values match the generator in `.superpowers/mesh-backgrounds`; trajectories are fixed so that every
 * device draws the same picture.
 *
 * @property power weight exponent: 1/d^power — higher gives crisper colour regions
 * @property flow strength of the diagonal "stream" displacement (0 = none)
 */
internal class MeshStyle(
    val trajectories: List<Trajectory>,
    val power: Float,
    val distortion: Float,
    val swirl: Float,
    val flow: Float = 0f,
) {
    /** Lissajous path of one colour point: phase, x rate and y rate. */
    class Trajectory(
        val phase: Float,
        val rateX: Float,
        val rateY: Float,
    )

    companion object {
        /** "Диагональный поток": space streams along a diagonal, colours stretch into slanted bands. */
        val DiagonalFlow =
            MeshStyle(
                trajectories =
                    listOf(
                        Trajectory(0.5685f, 1.3953f, 1.6785f),
                        Trajectory(3.8699f, 1.3389f, 1.4230f),
                        Trajectory(4.3609f, 1.0788f, 0.9897f),
                        Trajectory(4.8837f, 1.0235f, 1.5624f),
                        Trajectory(5.1730f, 1.0591f, 1.2585f),
                    ),
                power = 3.2f,
                distortion = 0.5f,
                swirl = 0.05f,
                flow = 0.6f,
            )

        /** "Ячейки": crisp soft-edged colour cells (the previous card design). */
        val Cells =
            MeshStyle(
                trajectories =
                    listOf(
                        Trajectory(4.6477f, 1.1331f, 1.3653f),
                        Trajectory(3.9144f, 1.1500f, 1.6402f),
                        Trajectory(3.5958f, 0.9612f, 1.0053f),
                        Trajectory(2.8207f, 1.3642f, 1.4893f),
                        Trajectory(6.6513f, 0.9067f, 0.9393f),
                        Trajectory(6.5778f, 0.9802f, 1.6207f),
                        Trajectory(5.3836f, 0.8140f, 0.9602f),
                    ),
                power = 8f,
                distortion = 0.25f,
                swirl = 0.05f,
            )
    }
}

/** Pure mesh-gradient maths, kept separate from Compose so it can be unit-tested. */
internal object MeshGradient {
    private const val TIME_OFFSET = 54.5f // first-frame offset of the approved design (41.5 + seed 13)

    // Direction of the diagonal flow, normalize(1, 0.55), and its perpendicular.
    private const val FLOW_X = 0.8762f
    private const val FLOW_Y = 0.4819f

    /**
     * Renders one frame into [pixels] (row-major ARGB, row 0 at the top).
     *
     * @param time animation time in seconds, already multiplied by the speed
     */
    fun render(
        pixels: IntArray,
        width: Int,
        height: Int,
        time: Float,
        palette: List<Color>,
        style: MeshStyle = MeshStyle.DiagonalFlow,
    ) {
        require(pixels.size >= width * height && palette.isNotEmpty())
        val t = 0.5f * (time + TIME_OFFSET)
        val points = style.trajectories
        val pointX = FloatArray(points.size)
        val pointY = FloatArray(points.size)
        points.forEachIndexed { i, tr ->
            pointX[i] = 0.5f + 0.5f * sin(t * tr.rateX + tr.phase)
            pointY[i] = 0.5f + 0.5f * cos(t * tr.rateY + tr.phase * 1.5f)
        }
        val red = FloatArray(points.size) { palette[it % palette.size].red }
        val green = FloatArray(points.size) { palette[it % palette.size].green }
        val blue = FloatArray(points.size) { palette[it % palette.size].blue }
        val halfPower = style.power / 2f
        val aspect = width.toFloat() / height
        for (y in 0 until height) {
            // "cover" mapping of the square world onto the card, as the design does
            val rowY = 0.5f + (0.5f - (y + 0.5f) / height) / aspect
            // The first horizontal bend depends on the row only.
            val rowBend = sin(t + 0.4f * smoothstep(rowY)) * cos(0.2f * t + 2.4f * smoothstep(rowY))
            for (x in 0 until width) {
                var ux = (x + 0.5f) / width
                var uy = rowY
                val radius = smoothstep(hypot(ux - 0.5f, uy - 0.5f))
                val center = 1f - radius
                for (i in 1..2) {
                    val fi = i.toFloat()
                    val bend = if (i == 1) rowBend else sin(t + fi * 0.4f * smoothstep(uy)) * cos(0.2f * t + fi * 2.4f * smoothstep(uy))
                    ux += style.distortion * center / fi * bend
                    uy += style.distortion * center / fi * cos(t + fi * 2f * smoothstep(ux))
                }
                if (style.flow != 0f) {
                    // distance across the stream picks the phase of a wave pushed along it
                    val across = (ux - 0.5f) * -FLOW_Y + (uy - 0.5f) * FLOW_X
                    val push = style.flow * sin(across * 4f + t * 0.8f)
                    ux += FLOW_X * push
                    uy += FLOW_Y * push
                }
                val angle = -3f * style.swirl * radius
                val dx0 = ux - 0.5f
                val dy0 = uy - 0.5f
                val cosA = cos(angle)
                val sinA = sin(angle)
                val qx = cosA * dx0 - sinA * dy0 + 0.5f
                val qy = sinA * dx0 + cosA * dy0 + 0.5f
                var r = 0f
                var g = 0f
                var b = 0f
                var total = 0f
                for (i in pointX.indices) {
                    val dx = qx - pointX[i]
                    val dy = qy - pointY[i]
                    val w = 1f / (weightDistance(dx * dx + dy * dy, halfPower) + 1e-3f)
                    r += red[i] * w
                    g += green[i] * w
                    b += blue[i] * w
                    total += w
                }
                pixels[y * width + x] = argb(r / total, g / total, b / total)
            }
        }
    }

    /**
     * [src] (width × height ARGB) twice as large with Catmull-Rom interpolation. Stretching the grid
     * bilinearly bends the gradient at every cell, which shows as a staircase along thin bright ridges;
     * doubled this way first, the rest of the stretch is too small for that to be visible.
     */
    fun upscale2x(
        src: IntArray,
        width: Int,
        height: Int,
    ): IntArray {
        val outWidth = width * 2
        val outHeight = height * 2
        // Horizontal pass into a float buffer per channel, then the vertical pass into ARGB.
        val rows = Array(3) { FloatArray(outWidth * height) }
        for (y in 0 until height) {
            for (x in 0 until outWidth) {
                for (c in 0 until 3) {
                    val shift = 16 - 8 * c
                    rows[c][y * outWidth + x] = interpolate(x, width) { i -> (src[y * width + i] shr shift and 0xFF).toFloat() }
                }
            }
        }
        val out = IntArray(outWidth * outHeight)
        for (y in 0 until outHeight) {
            for (x in 0 until outWidth) {
                val r = interpolate(y, height) { j -> rows[0][j * outWidth + x] }
                val g = interpolate(y, height) { j -> rows[1][j * outWidth + x] }
                val b = interpolate(y, height) { j -> rows[2][j * outWidth + x] }
                out[y * outWidth + x] = argb(r / 255f, g / 255f, b / 255f)
            }
        }
        return out
    }

    /**
     * Value at output index [out] of a line of [size] samples doubled: the output pixel centre sits at
     * source position out / 2 - 0.25, between samples k and k + 1 at fraction t (0.75 or 0.25).
     */
    private inline fun interpolate(
        out: Int,
        size: Int,
        sample: (Int) -> Float,
    ): Float {
        val even = out % 2 == 0
        val k = if (even) out / 2 - 1 else out / 2
        val w = if (even) CATMULL_ROM_075 else CATMULL_ROM_025
        var v = 0f
        for (i in 0..3) v += w[i] * sample((k - 1 + i).coerceIn(0, size - 1))
        return v
    }

    /** Catmull-Rom weights of samples k-1..k+2 at fraction t: (-t³+2t²-t, 3t³-5t²+2, -3t³+4t²+t, t³-t²) / 2. */
    private val CATMULL_ROM_025 = catmullRom(0.25f)
    private val CATMULL_ROM_075 = catmullRom(0.75f)

    private fun catmullRom(t: Float): FloatArray {
        val t2 = t * t
        val t3 = t2 * t
        return floatArrayOf(
            (-t3 + 2 * t2 - t) / 2,
            (3 * t3 - 5 * t2 + 2) / 2,
            (-3 * t3 + 4 * t2 + t) / 2,
            (t3 - t2) / 2,
        )
    }

    /** d^power from the squared distance; the integer power of the cells shape skips `pow`. */
    private fun weightDistance(
        d2: Float,
        halfPower: Float,
    ): Float =
        if (halfPower == 4f) {
            val d4 = d2 * d2
            d4 * d4
        } else {
            d2.pow(halfPower)
        }

    private fun smoothstep(x: Float): Float {
        val c = x.coerceIn(0f, 1f)
        return c * c * (3f - 2f * c)
    }

    private fun hypot(
        x: Float,
        y: Float,
    ): Float = sqrt(x * x + y * y)

    private fun argb(
        r: Float,
        g: Float,
        b: Float,
    ): Int =
        (0xFF shl 24) or
            ((r * 255f + 0.5f).toInt().coerceIn(0, 255) shl 16) or
            ((g * 255f + 0.5f).toInt().coerceIn(0, 255) shl 8) or
            (b * 255f + 0.5f).toInt().coerceIn(0, 255)
}

/** Approved palettes ("Электрик"): icy pastels on the light theme, deep navy with electric blues on the dark one. */
internal val MeshPaletteLight = listOf(Color(0xFFF3F6FF), Color(0xFFC7D4FF), Color(0xFFD6F0FF), Color(0xFFE4D4FF))
internal val MeshPaletteDark = listOf(Color(0xFF040A2A), Color(0xFF2F5BFF), Color(0xFF00C2FF), Color(0xFF7A3CFF))

/** Width of the CPU grid; the height follows the card's aspect ratio. */
internal const val MESH_GRID_WIDTH = 128

/** Animation speed: seconds of the design's clock per real second. */
internal const val MESH_SPEED = 0.3f

/** Real seconds between computed keyframes; the frames between them are cross-faded. */
internal const val MESH_KEYFRAME_SECONDS = 0.15f

private const val MIN_GRID_HEIGHT = 16
private const val KEYFRAME_NANOS = (MESH_KEYFRAME_SECONDS * 1_000_000_000).toLong()
private const val FRAME_INTERVAL_NANOS = 33_333_333L // ~30 fps is plenty for this slow motion
private const val VSYNC_SLACK_NANOS = 4_000_000L
private const val MAX_STEP_NANOS = 100_000_000L // no jump after a pause
