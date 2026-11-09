package net.payrdr.mobile.payment.sdk.payment.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun rememberResultHaptics(): (success: Boolean) -> Unit {
    val view = LocalView.current
    return remember(view) {
        { success ->
            // CONFIRM and REJECT came with Android 11; older systems get no tap rather than a generic one.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(if (success) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT)
            }
        }
    }
}
