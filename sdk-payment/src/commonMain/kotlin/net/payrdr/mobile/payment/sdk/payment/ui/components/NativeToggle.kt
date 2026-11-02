package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-native boolean toggle. On Android this renders a Material3 `Checkbox`; on iOS this
 * wraps `UIKitView(UISwitch)` — iOS has no native checkbox and Apple HIG uses a switch for
 * boolean settings. Visuals deliberately differ per platform.
 */
@Composable
public expect fun NativeToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
)
