package net.payrdr.mobile.payment.sdk.payment.scanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CardScannerOverlayTest {
    @Test
    fun the_card_frame_is_drawn_over_the_whole_camera_area() =
        runComposeUiTest {
            setContent {
                // Grey stands for the camera picture: the overlay must dim it around the frame and leave it clear inside.
                Box(Modifier.size(WIDTH.dp, HEIGHT.dp).background(Color(0xFF808080)).testTag("camera")) {
                    CardScannerOverlay(highlight = false)
                }
            }
            val image = onNodeWithTag("camera").captureToImage()
            val pixels = image.toPixelMap()
            val scale = image.width / WIDTH.toFloat()

            fun at(
                xDp: Float,
                yDp: Float,
            ) = pixels[(xDp * scale).roundToInt(), (yDp * scale).roundToInt()]

            // ID-1 frame 28dp from the sides, centred.
            val frameWidth = WIDTH - 2 * 28f
            val frameTop = (HEIGHT - frameWidth / 1.586f) / 2f
            // The 2dp stroke is centred on the frame edge and antialiased: its brightest row within a few dp of it.
            val stroke = (-3..3).map { at(WIDTH / 2f, frameTop + it) }.maxBy { it.red }
            val inside = at(WIDTH / 2f, HEIGHT / 2f)
            val outside = at(WIDTH / 2f, 20f)

            assertTrue(stroke.red > 0.85f && stroke.green > 0.85f, "no white frame at its top edge: $stroke")
            assertTrue(inside.red in 0.45f..0.55f, "the picture inside the frame is not left clear: $inside")
            assertTrue(outside.red < 0.35f, "the picture around the frame is not dimmed: $outside")
        }
}

private const val WIDTH = 200
private const val HEIGHT = 360
