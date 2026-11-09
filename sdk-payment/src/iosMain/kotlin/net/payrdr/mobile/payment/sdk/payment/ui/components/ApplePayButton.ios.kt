package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.convert
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.colorsAreDark
import platform.PassKit.PKPaymentButton
import platform.PassKit.PKPaymentButtonStyleBlack
import platform.PassKit.PKPaymentButtonStyleWhite
import platform.PassKit.PKPaymentButtonTypeBuy
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIView
import platform.UIKit.accessibilityLabel
import platform.darwin.NSObject
import platform.objc.objc_setAssociatedObject
import platform.objc.sel_registerName

/**
 * iOS actual: the real Apple-branded `PKPaymentButton` presented via Compose's `UIKitView`.
 * White on dark checkout themes, black on light ones.
 *
 * Inside the checkout the step only keeps the button's place, and the sheet's [WalletButtonLayer] draws it there: a UIKit
 * view inside a Compose transition would be hidden for the whole of it.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun ApplePayButton(
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val latestClick = rememberUpdatedState(onClick)
    // The native view stays out of VoiceOver's reach (interop views are by default): its place in the Compose tree reads
    // as the button instead, in reading order and only while its step shows it.
    val name = remember { applePayButtonName() }
    val slotModifier =
        modifier.fillMaxWidth().height(WalletButtonHeight).semantics {
            role = Role.Button
            contentDescription = name
            onClick {
                latestClick.value()
                true
            }
        }
    val layer = LocalWalletButtonLayer.current
    if (layer != null) {
        val alpha = LocalStepAlpha.current
        val settled = LocalStepSettled.current
        val slot = remember(layer, alpha, settled) { WalletSlot(alpha, settled) { latestClick.value() } }
        DisposableEffect(layer, slot) {
            layer.slot = slot
            onDispose { if (layer.slot === slot) layer.slot = null }
        }
        Box(
            slotModifier.onGloballyPositioned {
                slot.coordinates = it
                slot.size = it.size
                slot.placements++
            },
        )
        return
    }
    // Same corners and height as the other checkout buttons; the style follows the checkout theme,
    // not the system one (white on dark themes, black on light ones).
    val colors = LocalPaymentColors.current
    val dark = colorsAreDark(colors)
    val radius = LocalPaymentShapes.current.button.walletButtonCornerRadius()
    key(dark, radius, colors.background) {
        UIKitView(
            modifier = slotModifier,
            factory = { applePayButtonView(dark, radius, colors.background) { latestClick.value() } },
        )
    }
}

/**
 * The real button on an opaque backdrop of the sheet's colour. The interop view sits under a hole in the Compose canvas:
 * without the backdrop the rounded corners would show the host screen.
 */
@OptIn(ExperimentalForeignApi::class)
private fun applePayButtonView(
    dark: Boolean,
    radius: Dp?,
    background: Color,
    onClick: () -> Unit,
): UIView {
    val button = payButton(dark, radius, onClick)
    return UIView().apply {
        backgroundColor = background.toUIColor()
        button.translatesAutoresizingMaskIntoConstraints = false
        addSubview(button)
        NSLayoutConstraint.activateConstraints(
            listOf(
                button.leadingAnchor.constraintEqualToAnchor(leadingAnchor),
                button.trailingAnchor.constraintEqualToAnchor(trailingAnchor),
                button.topAnchor.constraintEqualToAnchor(topAnchor),
                button.bottomAnchor.constraintEqualToAnchor(bottomAnchor),
            ),
        )
    }
}

/** What VoiceOver calls the button: PassKit's own name for it ("Buy with Apple Pay"), in the system language. */
private fun applePayButtonName(): String =
    PKPaymentButton(paymentButtonType = PKPaymentButtonTypeBuy, paymentButtonStyle = PKPaymentButtonStyleBlack)
        .accessibilityLabel
        ?.takeIf { it.isNotBlank() } ?: "Buy with Apple Pay"

/** `PKPaymentButton` in the checkout's style, calling [onClick] when tapped. */
@OptIn(ExperimentalForeignApi::class)
internal fun payButton(
    dark: Boolean,
    radius: Dp?,
    onClick: () -> Unit,
): PKPaymentButton {
    val button =
        PKPaymentButton(
            paymentButtonType = PKPaymentButtonTypeBuy,
            paymentButtonStyle = if (dark) PKPaymentButtonStyleWhite else PKPaymentButtonStyleBlack,
        )
    radius?.let { button.cornerRadius = it.value.toDouble() }
    val target = ClickTarget(onClick)
    button.addTarget(target = target, action = sel_registerName("onTap"), forControlEvents = UIControlEventTouchUpInside)
    // Keep a strong reference attached to the button via associated object
    // so the ClickTarget survives for the button's lifetime.
    objc_setAssociatedObject(button, clickTargetKey, target, 1u.convert())
    return button
}

/**
 * NSObject subclass that owns the Compose `onClick` lambda so it survives for the button's
 * lifetime. Exposed as `@ObjCAction` to be callable from the UIKit target/action hook.
 */
@OptIn(BetaInteropApi::class)
internal class ClickTarget(
    private val callback: () -> Unit,
) : NSObject() {
    @ObjCAction
    fun onTap() {
        callback()
    }
}

/** Arbitrary associated-object key (must be a stable address for the process lifetime). */
@OptIn(ExperimentalForeignApi::class)
private val clickTargetKey = StableRef.create(Any()).asCPointer()
