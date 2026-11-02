package net.payrdr.mobile.payment.sdk.payment.ui.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSString
import platform.Foundation.create
import platform.UIKit.NSFontAttributeName
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.drawInRect
import platform.posix.memcpy

private val cache = mutableMapOf<String, ImageBitmap?>()

/** Emoji glyphs fill about this share of the font size, so the font is scaled up to fill the box. */
private const val GLYPH_FILL = 0.82

@OptIn(ExperimentalForeignApi::class)
internal actual fun flagBitmap(
    iso: String,
    sizePx: Int,
): ImageBitmap? =
    cache.getOrPut("$iso@$sizePx") {
        val emoji = flagEmoji(iso)
        if (emoji.isEmpty() || sizePx <= 0) return@getOrPut null
        val side = sizePx.toDouble()
        val format = UIGraphicsImageRendererFormat().apply { scale = 1.0 }
        val image =
            UIGraphicsImageRenderer(size = CGSizeMake(side, side), format = format).imageWithActions { _ ->
                NSString.create(string = emoji).drawInRect(
                    CGRectMake(0.0, 0.0, side, side),
                    withAttributes = mapOf<Any?, Any?>(NSFontAttributeName to UIFont.systemFontOfSize(side * GLYPH_FILL)),
                )
            }
        val data = UIImagePNGRepresentation(image) ?: return@getOrPut null
        val bytes = ByteArray(data.length.toInt())
        bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    }
