package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UISwitch
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
@Composable
public actual fun NativeToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
) {
    val colors = LocalPaymentColors.current
    val latestOnChange = rememberUpdatedState(onCheckedChange)

    val target =
        remember {
            SwitchValueChangedTarget { isOn -> latestOnChange.value(isOn) }
        }

    UIKitView(
        // UISwitch has fixed intrinsic dimensions (~51x31 pt) but `UIKitView` ignores intrinsic
        // size, so we pin it explicitly — otherwise the switch collapses to 0 the same way
        // UITextField did.
        modifier = modifier.size(width = 51.dp, height = 31.dp),
        properties =
            UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                isNativeAccessibilityEnabled = false,
            ),
        factory = {
            val switch = UISwitch()
            switch.on = checked
            switch.onTintColor =
                UIColor(
                    red = colors.primary.red.toDouble(),
                    green = colors.primary.green.toDouble(),
                    blue = colors.primary.blue.toDouble(),
                    alpha = colors.primary.alpha.toDouble(),
                )
            switch.addTarget(
                target,
                action = nsSelectorFromString("onValueChanged:"),
                forControlEvents = UIControlEventValueChanged,
            )
            switch
        },
        update = { switch ->
            if (switch.on != checked) {
                switch.setOn(checked, animated = true)
            }
        },
    )
}

private class SwitchValueChangedTarget(
    private val onChange: (Boolean) -> Unit,
) : NSObject() {
    @ObjCAction
    fun onValueChanged(sender: UISwitch) {
        onChange(sender.on)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun nsSelectorFromString(name: String): COpaquePointer? = platform.Foundation.NSSelectorFromString(name)
