package net.payrdr.mobile.payment.sdk.payment.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal actual fun pixelsToImageBitmap(
    pixels: IntArray,
    width: Int,
    height: Int,
): ImageBitmap {
    // A new bitmap every keyframe (~10 KB): the previous one may still be drawn while this one is filled.
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap.asImageBitmap()
}
