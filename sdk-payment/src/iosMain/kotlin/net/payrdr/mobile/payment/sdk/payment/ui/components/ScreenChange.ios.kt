package net.payrdr.mobile.payment.sdk.payment.ui.components

import platform.UIKit.UIAccessibilityPostNotification
import platform.UIKit.UIAccessibilityScreenChangedNotification

// VoiceOver moves to the first element of the new step and reads it.
internal actual fun postScreenChanged() = UIAccessibilityPostNotification(UIAccessibilityScreenChangedNotification, null)
