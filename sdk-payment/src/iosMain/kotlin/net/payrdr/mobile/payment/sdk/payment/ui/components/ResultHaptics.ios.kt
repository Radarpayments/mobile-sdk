package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType.UINotificationFeedbackTypeError
import platform.UIKit.UINotificationFeedbackType.UINotificationFeedbackTypeSuccess

@Composable
internal actual fun rememberResultHaptics(): (success: Boolean) -> Unit =
    remember {
        { success ->
            UINotificationFeedbackGenerator().notificationOccurred(
                if (success) UINotificationFeedbackTypeSuccess else UINotificationFeedbackTypeError,
            )
        }
    }
