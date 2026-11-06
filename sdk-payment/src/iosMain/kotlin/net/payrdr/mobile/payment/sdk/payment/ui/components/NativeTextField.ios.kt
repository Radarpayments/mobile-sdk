package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.coroutines.flow.first
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSAttributedString
import platform.Foundation.create
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.NSTextAlignmentLeft
import platform.UIKit.NSTextAlignmentNatural
import platform.UIKit.NSTextAlignmentRight
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIBarButtonSystemItem
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventEditingChanged
import platform.UIKit.UIControlEventEditingDidBegin
import platform.UIKit.UIControlEventEditingDidEnd
import platform.UIKit.UIControlEventEditingDidEndOnExit
import platform.UIKit.UIFont
import platform.UIKit.UIFontWeightRegular
import platform.UIKit.UIKeyboardTypeASCIICapable
import platform.UIKit.UIKeyboardTypeNumberPad
import platform.UIKit.UIReturnKeyType
import platform.UIKit.UISemanticContentAttributeForceLeftToRight
import platform.UIKit.UISemanticContentAttributeUnspecified
import platform.UIKit.UITextBorderStyle
import platform.UIKit.UITextContentTypeAddressCity
import platform.UIKit.UITextContentTypeAddressState
import platform.UIKit.UITextContentTypeCreditCardNumber
import platform.UIKit.UITextContentTypeEmailAddress
import platform.UIKit.UITextContentTypeFullStreetAddress
import platform.UIKit.UITextContentTypeName
import platform.UIKit.UITextContentTypePostalCode
import platform.UIKit.UITextContentTypeTelephoneNumber
import platform.UIKit.UITextField
import platform.UIKit.UITextFieldViewMode
import platform.UIKit.UIToolbar
import platform.UIKit.accessibilityLabel
import platform.darwin.NSObject

private const val FONT_SIZE = 16f

/** The tracking UIKit applies to the system font at [FONT_SIZE]; Compose draws the same font without it. */
private const val SYSTEM_FONT_TRACKING = -0.31f

/** What UIKit draws for a secure field: one bullet per character. */
private const val SECURE_BULLET = '\u2022'

/**
 * The field as the UITextField draws it — same size, font, colours and alignment, no caret — so swapping one for the
 * other when the step settles is not visible.
 */
@Composable
private fun NativeFieldStandIn(
    value: String,
    placeholder: String,
    contentType: TextFieldContentType,
    rtl: Boolean,
    modifier: Modifier,
) {
    val colors = LocalPaymentColors.current
    // The UITextField's size: its points times the system text size, see UIKitTextField.
    val density = LocalDensity.current
    val fontSize = FONT_SIZE.toFloat().sp
    val tracking = with(density) { SYSTEM_FONT_TRACKING.dp.toSp() }
    val shown =
        when {
            value.isEmpty() -> placeholder
            contentType == TextFieldContentType.CREDIT_CARD_SECURITY_CODE -> SECURE_BULLET.toString().repeat(value.length)
            else -> value
        }
    val style =
        TextStyle(
            color = if (value.isEmpty()) colors.placeholder else colors.onBackground,
            fontSize = fontSize,
            letterSpacing = tracking,
            fontFeatureSettings = if (contentType.isNumeric || contentType == TextFieldContentType.PHONE) "tnum" else null,
            textAlign = if (contentType.isLatin && rtl) TextAlign.Right else TextAlign.Start,
        )
    val direction = if (contentType.isLatin) LayoutDirection.Ltr else LocalLayoutDirection.current
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Box(
            modifier = modifier.defaultMinSize(minHeight = 44.dp).background(colors.background),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicText(text = shown, style = style, maxLines = 1, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * Card data goes into a UITextField, which iOS fills from the saved cards and VoiceOver reads as a native field.
 * The payer's details are drawn by Compose, which gets their autofill type to the keyboard as well (see
 * `rememberPlatformKeyboard`) and draws them like the rest of the sheet: no interop hole, no stand-in while a step
 * animates.
 */
private val TextFieldContentType.isCardData: Boolean
    get() =
        this == TextFieldContentType.CREDIT_CARD_NUMBER ||
            this == TextFieldContentType.CREDIT_CARD_EXPIRATION ||
            this == TextFieldContentType.CREDIT_CARD_SECURITY_CODE ||
            this == TextFieldContentType.CREDIT_CARD_HOLDER

@Composable
internal actual fun NativeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    contentType: TextFieldContentType,
    isError: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onFocusChange: (Boolean) -> Unit,
    label: String?,
    errorMessage: String?,
) {
    if (contentType.isCardData) {
        // VoiceOver reads the field's name and its error before the value.
        val name = listOfNotNull(label ?: placeholder, errorMessage).joinToString(", ")
        UIKitTextField(value, onValueChange, placeholder, name, contentType, enabled, modifier, onFocusChange)
    } else {
        ComposeTextField(
            value,
            onValueChange,
            placeholder,
            keyboardOptions,
            contentType,
            enabled,
            modifier,
            onFocusChange,
            label = label,
            errorMessage = errorMessage,
        )
    }
}

@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class, BetaInteropApi::class)
@Composable
private fun UIKitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accessibilityName: String,
    contentType: TextFieldContentType,
    enabled: Boolean,
    modifier: Modifier,
    onFocusChange: (Boolean) -> Unit,
) {
    val colors = LocalPaymentColors.current
    // Sized like the Compose text around it, which follows the system text size (Dynamic Type).
    val fontSize = FONT_SIZE * LocalDensity.current.fontScale
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val latestValue = rememberUpdatedState(value)
    val latestOnChange = rememberUpdatedState(onValueChange)
    val latestOnFocus = rememberUpdatedState(onFocusChange)
    val autoFocus = LocalNativeFieldAutoFocus.current
    val settled = LocalStepSettled.current
    // Bumped on every edit so `update` runs even when the caller rejects the edit (a 4th CVV digit, a 20th PAN
    // digit): the value stays the same, nothing else would recompose, and the field would keep the extra text.
    var edits by remember { mutableIntStateOf(0) }

    val delegate =
        remember {
            TextFieldTarget(
                onChange = { text ->
                    latestOnChange.value(text)
                    edits++
                },
                onFocus = { focused -> latestOnFocus.value(focused) },
            )
        }

    LaunchedEffect(Unit) {
        if (autoFocus) {
            snapshotFlow { settled.value }.first { it }
            // The UITextField replaces the stand-in, and is created and attached, during the next frame.
            withFrameNanos { }
            delegate.attachedField?.becomeFirstResponder()
        }
    }

    if (!settled.value) {
        // While the step animates, a Compose copy stands in: the UIKit field ignores Compose's alpha and scale and
        // would show as a solid box over a fading, zooming step. The real field takes over once the step settles.
        NativeFieldStandIn(value, placeholder, contentType, rtl, modifier)
        return
    }

    UIKitView(
        // UITextField has no Compose-visible intrinsic height; without this, `UIKitView` lays
        // out at 0 tall and the field becomes invisible/untappable.
        modifier = modifier.defaultMinSize(minHeight = 44.dp),
        // NonCooperative = touch goes to UITextField immediately, bypassing Compose's 150ms
        // pan-gesture disambiguation. Text fields don't participate in scroll gestures, so the
        // Cooperative default (scroll-friendly) is wrong for them.
        properties =
            UIKitInteropProperties(
                interactionMode = UIKitInteropInteractionMode.NonCooperative,
                // VoiceOver reaches the field itself, in the Compose reading order.
                isNativeAccessibilityEnabled = true,
            ),
        factory = {
            val field = UITextField()
            // Chromeless: the Compose FieldBox around it draws the frame, ring and error state.
            field.borderStyle = UITextBorderStyle.UITextBorderStyleNone
            field.clearButtonMode = UITextFieldViewMode.UITextFieldViewModeNever
            field.autocorrectionType = platform.UIKit.UITextAutocorrectionType.UITextAutocorrectionTypeNo
            field.autocapitalizationType =
                if (contentType == TextFieldContentType.CREDIT_CARD_HOLDER) {
                    platform.UIKit.UITextAutocapitalizationType.UITextAutocapitalizationTypeAllCharacters
                } else {
                    platform.UIKit.UITextAutocapitalizationType.UITextAutocapitalizationTypeNone
                }
            // The holder's name on the letter keyboard, the rest on the number pad.
            field.keyboardType =
                if (contentType == TextFieldContentType.CREDIT_CARD_HOLDER) UIKeyboardTypeASCIICapable else UIKeyboardTypeNumberPad
            field.textContentType = contentTypeFor(contentType)
            field.secureTextEntry = contentType == TextFieldContentType.CREDIT_CARD_SECURITY_CODE
            if (field.keyboardType == UIKeyboardTypeNumberPad) {
                // The number pad has no return key — give the user a way to hide the keyboard.
                field.inputAccessoryView = doneToolbar(delegate)
            } else {
                // Return hides the keyboard: the Pay button is hidden while it is open.
                field.returnKeyType = UIReturnKeyType.UIReturnKeyDone
                field.addTarget(
                    delegate,
                    action = nsSelectorFromString("onDone"),
                    forControlEvents = UIControlEventEditingDidEndOnExit,
                )
            }
            field.addTarget(
                delegate,
                action = nsSelectorFromString("onTextChanged:"),
                forControlEvents = UIControlEventEditingChanged,
            )
            field.addTarget(
                delegate,
                action = nsSelectorFromString("onBegin:"),
                forControlEvents = UIControlEventEditingDidBegin,
            )
            field.addTarget(
                delegate,
                action = nsSelectorFromString("onEnd:"),
                forControlEvents = UIControlEventEditingDidEnd,
            )
            delegate.attachedField = field
            field
        },
        update = { field ->
            edits // read so that every edit re-runs this block
            if (field.text != latestValue.value) {
                field.text = latestValue.value
            }
            field.enabled = enabled
            field.accessibilityLabel = accessibilityName
            field.font =
                if (contentType.isNumeric || contentType == TextFieldContentType.PHONE) {
                    UIFont.monospacedDigitSystemFontOfSize(fontSize.toDouble(), UIFontWeightRegular)
                } else {
                    UIFont.systemFontOfSize(fontSize.toDouble())
                }
            if (contentType.isLatin) {
                // Latin text runs left to right in any layout, aligned to the layout's start.
                field.semanticContentAttribute = UISemanticContentAttributeForceLeftToRight
                field.textAlignment = if (rtl) NSTextAlignmentRight else NSTextAlignmentLeft
            } else {
                field.semanticContentAttribute = UISemanticContentAttributeUnspecified
                field.textAlignment = NSTextAlignmentNatural
            }
            // The interop view sits under the Compose canvas, so a clear field shows the host's own
            // (white) background through the hole — unreadable in the dark theme. Paint it with the
            // same colour FieldBox fills the field with.
            field.backgroundColor = colors.background.toUIColor()
            field.textColor = colors.onBackground.toUIColor()
            field.tintColor = colors.primary.toUIColor()
            field.attributedPlaceholder =
                NSAttributedString.create(
                    string = placeholder,
                    attributes = mapOf<Any?, Any?>(NSForegroundColorAttributeName to colors.placeholder.toUIColor()),
                )
        },
    )
}

internal fun Color.toUIColor(): UIColor =
    UIColor(
        red = red.toDouble(),
        green = green.toDouble(),
        blue = blue.toDouble(),
        alpha = alpha.toDouble(),
    )

/** A bar with "Done" for over the keys; the button calls `onDone` on [target]. */
@OptIn(ExperimentalForeignApi::class)
internal fun doneToolbar(target: NSObject): UIToolbar {
    val toolbar = UIToolbar(frame = CGRectMake(0.0, 0.0, 320.0, 44.0))
    val flexible =
        UIBarButtonItem(
            barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemFlexibleSpace,
            target = null,
            action = null,
        )
    val done =
        UIBarButtonItem(
            barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemDone,
            target = target,
            action = nsSelectorFromString("onDone"),
        )
    toolbar.setItems(listOf(flexible, done), animated = false)
    toolbar.sizeToFit()
    return toolbar
}

/**
 * Maps [TextFieldContentType] to the best available `UITextContentType` string literal.
 * `.creditCardExpiration` and `.creditCardSecurityCode` are iOS 17+ only — older systems
 * ignore the unknown literal.
 */
internal fun contentTypeFor(contentType: TextFieldContentType): String? =
    when (contentType) {
        TextFieldContentType.CREDIT_CARD_NUMBER -> UITextContentTypeCreditCardNumber
        TextFieldContentType.CREDIT_CARD_EXPIRATION ->
            // iOS 17+: `UITextContentTypeCreditCardExpiration` literal = "creditCardExpiration"
            "creditCardExpiration"
        TextFieldContentType.CREDIT_CARD_SECURITY_CODE -> "creditCardSecurityCode"
        TextFieldContentType.CREDIT_CARD_HOLDER -> UITextContentTypeName
        TextFieldContentType.EMAIL -> UITextContentTypeEmailAddress
        TextFieldContentType.PHONE -> UITextContentTypeTelephoneNumber
        TextFieldContentType.ADDRESS_LINE -> UITextContentTypeFullStreetAddress
        TextFieldContentType.CITY -> UITextContentTypeAddressCity
        TextFieldContentType.STATE -> UITextContentTypeAddressState
        TextFieldContentType.POSTAL_CODE -> UITextContentTypePostalCode
        TextFieldContentType.NONE -> null
    }

/**
 * Bridges Objective-C `UIControl` target-action back to Kotlin lambdas. We cannot attach a
 * closure directly to `addTarget(_:action:forControlEvents:)`, so we use a tiny NSObject subclass.
 */
@OptIn(BetaInteropApi::class)
private class TextFieldTarget(
    private val onChange: (String) -> Unit,
    private val onFocus: (Boolean) -> Unit,
) : NSObject() {
    var attachedField: UITextField? = null

    @ObjCAction
    fun onTextChanged(sender: UITextField) {
        onChange(sender.text ?: "")
    }

    @ObjCAction
    fun onBegin(sender: UITextField) {
        onFocus(true)
    }

    @ObjCAction
    fun onEnd(sender: UITextField) {
        onFocus(false)
    }

    @ObjCAction
    fun onDone() {
        attachedField?.resignFirstResponder()
    }
}

/** Bridge to `NSSelectorFromString` — returns COpaquePointer? matching the `SEL` C type. */
@OptIn(ExperimentalForeignApi::class)
private fun nsSelectorFromString(name: String): COpaquePointer? = platform.Foundation.NSSelectorFromString(name)
