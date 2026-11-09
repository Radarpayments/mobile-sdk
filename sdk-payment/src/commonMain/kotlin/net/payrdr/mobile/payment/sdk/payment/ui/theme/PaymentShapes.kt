package net.payrdr.mobile.payment.sdk.payment.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Corner radii for SDK UI surfaces.
 *
 * - [field] — text-field background
 * - [button] — pay button, wallet buttons, ghost buttons
 * - [sheet] — bottom-sheet container (only top corners rounded in practice)
 * - [card] — payment-method rows, saved-card plate, receipt
 */
@Immutable
public data class PaymentShapes(
    val field: Shape,
    val button: Shape,
    val sheet: Shape,
    val card: Shape,
) {
    public companion object {
        public val Default: PaymentShapes =
            PaymentShapes(
                field = RoundedCornerShape(12.dp),
                button = RoundedCornerShape(14.dp),
                sheet = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                card = RoundedCornerShape(14.dp),
            )
    }
}
