package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

internal actual fun pixelsToImageBitmap(
    pixels: IntArray,
    width: Int,
    height: Int,
): ImageBitmap {
    // Skia images are immutable, so a small new raster is made per keyframe (~10 KB at the grid size).
    // ARGB ints laid out little-endian are B, G, R, A bytes = BGRA_8888.
    val bytes = ByteArray(width * height * 4)
    for (i in 0 until width * height) {
        val p = pixels[i]
        bytes[i * 4] = (p and 0xFF).toByte()
        bytes[i * 4 + 1] = (p shr 8 and 0xFF).toByte()
        bytes[i * 4 + 2] = (p shr 16 and 0xFF).toByte()
        bytes[i * 4 + 3] = (p ushr 24).toByte()
    }
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)
    return Image.makeRaster(info, bytes, width * 4).toComposeImageBitmap()
}
