package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.math.abs
import kotlin.test.Test

class MeshGradientTest {
    private fun render(
        time: Float,
        palette: List<Color> = MeshPaletteDark,
        width: Int = 32,
        height: Int = 14,
        style: MeshStyle = MeshStyle.DiagonalFlow,
    ) = IntArray(width * height).also { MeshGradient.render(it, width, height, time, palette, style) }

    @Test
    fun `same time gives the same frame and later time moves the colours`() {
        render(3f).contentEquals(render(3f)) shouldBe true
        render(3f).contentEquals(render(9f)) shouldBe false
    }

    @Test
    fun `every pixel is opaque`() {
        render(1.5f).all { it ushr 24 == 0xFF } shouldBe true
    }

    @Test
    fun `a single colour palette paints a flat card`() {
        val red = Color(0xFFE53935)
        render(2f, listOf(red)).toSet() shouldBe setOf(red.toArgb())
    }

    @Test
    fun `blended colours stay within the palette range`() {
        val pixels = render(4f)

        fun channel(
            c: Int,
            shift: Int,
        ) = c shr shift and 0xFF
        for (shift in listOf(16, 8, 0)) {
            val bounds = MeshPaletteDark.map { channel(it.toArgb(), shift) }
            pixels.all { channel(it, shift) in bounds.min()..bounds.max() } shouldBe true
        }
    }

    @Test
    fun `the frame is not flat with the real palette`() {
        render(0f).toSet().size shouldNotBe 1
        render(0f, MeshPaletteLight).toSet().size shouldNotBe 1
    }

    @Test
    fun `shapes differ and the cells shape still renders`() {
        render(2f, style = MeshStyle.Cells).contentEquals(render(2f)) shouldBe false
        render(2f, style = MeshStyle.Cells).all { it ushr 24 == 0xFF } shouldBe true
    }

    @Test
    fun `blending neighbouring keyframes stays close to the frame between them`() {
        val step = MESH_KEYFRAME_SECONDS * MESH_SPEED
        val error = Error()
        for (k in 0 until (40f / MESH_KEYFRAME_SECONDS).toInt()) {
            val before = render(k * step, width = 64, height = 40)
            val after = render((k + 1) * step, width = 64, height = 40)
            val between = render((k + 0.5f) * step, width = 64, height = 40)
            for (i in between.indices) {
                error.add(between[i]) { shift -> ((before[i] shr shift and 0xFF) + (after[i] shr shift and 0xFF)) / 2f }
            }
        }
        (error.max <= 16) shouldBe true
        (error.mean < 0.1f) shouldBe true
    }

    @Test
    fun `doubling keeps a flat colour flat`() {
        val flat = IntArray(6 * 4) { 0xFF336699.toInt() }
        MeshGradient.upscale2x(flat, 6, 4).toSet() shouldBe setOf(0xFF336699.toInt())
    }

    @Test
    fun `doubling follows a linear ramp exactly away from the edges`() {
        val (width, height) = 8 to 3
        val ramp = IntArray(width * height) { i -> argbGrey(40 + 20 * (i % width)) }
        val doubled = MeshGradient.upscale2x(ramp, width, height)
        // Output pixel x sits at source position x / 2 - 0.25; the ramp there is 40 + 20 * position.
        for (x in 4 until 2 * width - 4) {
            val expected = 40 + 20 * (x / 2f - 0.25f)
            abs((doubled[2 * width + x] and 0xFF) - expected) shouldBeLessThanOrEqual 0.5f
        }
    }

    @Test
    fun `doubling a hard edge stays within the colour range`() {
        val edge = IntArray(8 * 2) { i -> if (i % 8 < 4) argbGrey(0) else argbGrey(255) }
        MeshGradient.upscale2x(edge, 8, 2).all { it ushr 24 == 0xFF } shouldBe true
    }

    private fun argbGrey(v: Int) = (0xFF shl 24) or (v shl 16) or (v shl 8) or v

    /** Largest and mean difference of colour channels (0–255). */
    private class Error {
        var max = 0
        private var sum = 0L
        private var count = 0

        val mean: Float get() = sum.toFloat() / count

        fun add(
            expected: Int,
            actual: (shift: Int) -> Float,
        ) {
            for (shift in listOf(16, 8, 0)) {
                val d = abs(actual(shift) - (expected shr shift and 0xFF)).toInt()
                max = maxOf(max, d)
                sum += d
                count++
            }
        }
    }
}
