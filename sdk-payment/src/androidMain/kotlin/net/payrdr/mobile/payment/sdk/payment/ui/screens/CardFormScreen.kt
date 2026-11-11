package net.payrdr.mobile.payment.sdk.payment.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.result.OrderSummary
import net.payrdr.mobile.payment.sdk.payment.result.PayerDataRequest
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerConfig
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerResult
import net.payrdr.mobile.payment.sdk.payment.ui.PlatformBackHandler
import net.payrdr.mobile.payment.sdk.payment.ui.components.ApplePayButton
import net.payrdr.mobile.payment.sdk.payment.ui.components.BottomInsetReach
import net.payrdr.mobile.payment.sdk.payment.ui.components.CardDetailsGroup
import net.payrdr.mobile.payment.sdk.payment.ui.components.CheckboxRow
import net.payrdr.mobile.payment.sdk.payment.ui.components.CoverWhile
import net.payrdr.mobile.payment.sdk.payment.ui.components.GooglePayButton
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupFieldStatus
import net.payrdr.mobile.payment.sdk.payment.ui.components.LabeledDivider
import net.payrdr.mobile.payment.sdk.payment.ui.components.PayButton
import net.payrdr.mobile.payment.sdk.payment.ui.components.PaymentIcons
import net.payrdr.mobile.payment.sdk.payment.ui.components.SIDE_PADDING
import net.payrdr.mobile.payment.sdk.payment.ui.components.StepHeader
import net.payrdr.mobile.payment.sdk.payment.ui.components.SystemBarsAppearance
import net.payrdr.mobile.payment.sdk.payment.ui.components.TextAction
import net.payrdr.mobile.payment.sdk.payment.ui.components.payLabel
import net.payrdr.mobile.payment.sdk.payment.ui.components.rememberKeyboardDismiss
import net.payrdr.mobile.payment.sdk.payment.ui.components.rememberWalletAvailability
import net.payrdr.mobile.payment.sdk.payment.ui.payer.CountryPickerSheet
import net.payrdr.mobile.payment.sdk.payment.ui.payer.PayerFieldsSection
import net.payrdr.mobile.payment.sdk.payment.ui.payer.rememberPayerFieldsState
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardFormatters
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardInputValidation
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetworkResolver
import net.payrdr.mobile.payment.sdk.payment.ui.util.FieldError
import net.payrdr.mobile.payment.sdk.payment.ui.util.gatewayPaymentSystems
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.StringResource
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.action_close
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_save_card
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_save_card_hint
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_scan
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_country_empty
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_cvv_3
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_cvv_4
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_date_invalid
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_email_empty
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_email_invalid
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_expiry_format
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_expiry_month
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_expiry_past
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_field_required
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_holder_empty
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_holder_format
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_holder_short
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_latin_only
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_pan_empty
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_pan_invalid
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_pan_short
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_phone_empty
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_phone_long
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_phone_no_code
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_phone_short
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.error_postal_code_invalid
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.scan_body
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.scan_manual
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.scan_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.scan_turn_on
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.sheet_or_pay_by_card
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.stringResource

/** The camera explainer is shown once per app process — after that the system already asked. */
private var scanIntroSeen = false

private enum class Field { PAN, EXPIRY, CVV, HOLDER }

/**
 * Shows the name-on-card row in the card group. Switched in code for now, not by the integrator. When off, no
 * name is sent — not even the scanner's — and the gateway gets its default.
 */
private const val CARD_HOLDER_FIELD_ENABLED = false

/**
 * New-card step. Holds all field states internally (the FSM only receives the submitted
 * `SubmitNewCard` intent). A live card preview sits above the fields; errors appear under the
 * field once it is complete or after Pay is tapped — Pay itself stays enabled.
 *
 * @param config top-level SDK config (.googlePay / .applePay / .cardScannerProvider are read here)
 * @param onSubmit fires when the user taps Pay with fully-valid fields
 * @param onBack when non-null, the top bar gets a back (or close, see [backIsClose]) action
 * @param order amount for the Pay button and the top bar; null = plain "Pay"
 * @param canSaveCard show the "Save card" checkbox
 * @param transfer the order is a transfer (AFT): the button says "Transfer"
 * @param saveCardByDefault where the "Save card" box starts
 * @param saveCardLocked the box stays ticked: the order demands a saved card
 * @param saveCardOnly the order only saves the card (`VERIFY`): the button says "Save card"
 * @param payerData payer fields the gateway asks for per payment system, with prefill values
 * @param backIsClose draw the navigation action as a close cross — when this is the first step
 * @param showWallets render wallet buttons above the form (off when a payment sheet already offers them)
 * @param onClose when non-null, the top bar gets a close action that leaves the whole checkout
 */
@Composable
internal fun CardFormScreen(
    config: SdkPaymentConfig,
    onSubmit: (PaymentIntent.SubmitNewCard) -> Unit,
    onGooglePayClick: () -> Unit = {},
    onApplePayClick: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    order: OrderSummary? = null,
    canSaveCard: Boolean = true,
    payerData: PayerDataRequest = PayerDataRequest(),
    backIsClose: Boolean = false,
    showWallets: Boolean = true,
    onClose: (() -> Unit)? = null,
    transfer: Boolean = false,
    saveCardByDefault: Boolean = true,
    saveCardLocked: Boolean = false,
    saveCardOnly: Boolean = false,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current

    var pan by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }
    // Name on the card; used only while the field is on (the scanner's reading included).
    var holder by remember { mutableStateOf("") }
    var holderVisited by remember { mutableStateOf(false) }
    var saveCard by remember { mutableStateOf(canSaveCard && (saveCardByDefault || saveCardLocked)) }
    var touched by remember { mutableStateOf(emptySet<Field>()) }
    // On the camera screen, before the camera itself: what it is for, until the first "Turn on camera".
    var cameraOn by remember { mutableStateOf(scanIntroSeen) }
    var scannerOpen by remember { mutableStateOf(false) }

    val network = CardNetworkResolver.resolve(pan)
    val payer = rememberPayerFieldsState(payerData)
    val paymentSystems = network.gatewayPaymentSystems
    val errors: Map<Field, FieldError?> =
        buildMap {
            put(Field.PAN, CardInputValidation.pan(pan))
            put(Field.EXPIRY, CardInputValidation.expiry(expiry))
            put(Field.CVV, CardInputValidation.cvv(cvc, network))
            if (CARD_HOLDER_FIELD_ENABLED) put(Field.HOLDER, CardInputValidation.holder(holder))
        }

    @Composable
    fun errorOf(field: Field): String? = errors[field]?.takeIf { field in touched }?.let { stringResource(it.message()) }

    fun validOf(field: Field): Boolean = field in touched && errors[field] == null

    @Composable
    fun statusOf(field: Field): GroupFieldStatus =
        GroupFieldStatus(isError = errors[field] != null && field in touched, isValid = validOf(field), message = errorOf(field))

    val scanner = config.cardScannerProvider
    val dismissKeyboard = rememberKeyboardDismiss()
    val onScanned: (CardScannerResult) -> Unit = { result ->
        if (result is CardScannerResult.Success) {
            pan = result.card.pan
            val mm =
                result.card.expiryMonth
                    .toString()
                    .padStart(2, '0')
            val yy = (result.card.expiryYear % 100).toString().padStart(2, '0')
            expiry = "$mm$yy"
            if (CARD_HOLDER_FIELD_ENABLED) {
                result.card.cardholder
                    ?.takeIf { it.isNotBlank() }
                    ?.let { holder = it.uppercase() }
            }
            touched = touched + Field.PAN + Field.EXPIRY
        }
        scannerOpen = false
    }
    val openScanner = {
        dismissKeyboard()
        cameraOn = scanIntroSeen
        scannerOpen = true
    }

    val payLabel = payLabel(order, transfer, saveCardOnly)

    val inSheet = LocalSheetPageHost.current != null
    PageScaffold(
        modifier = modifier.testTag("CardFormScreen"),
        topBar = {
            StepHeader(
                title = stringResource(Res.string.card_form_title),
                onBack = onBack?.takeIf { !backIsClose },
                onClose = onClose ?: onBack?.takeIf { backIsClose },
                // In the checkout sheet the handle above already gives the room.
                modifier = Modifier.padding(start = SIDE_PADDING, end = SIDE_PADDING, top = if (inSheet) 2.dp else 12.dp, bottom = 8.dp),
            )
        },
        footer = {
            PayButton(
                label = payLabel,
                onClick = {
                    touched = errors.keys
                    val payerSpecs = payer.specsFor(paymentSystems)
                    val payerValid = payer.validateAll(payerSpecs)
                    if (errors.values.all { it == null } && payerValid) {
                        onSubmit(
                            PaymentIntent.SubmitNewCard(
                                pan = pan,
                                expiryMMYY = CardFormatters.formatExpiry(expiry),
                                cvc = cvc,
                                cardHolder = holder.trim().takeIf { CARD_HOLDER_FIELD_ENABLED && it.isNotEmpty() },
                                saveCard = canSaveCard && saveCard,
                                payerData = payer.submitValues(payerSpecs),
                            ),
                        )
                    }
                },
            )
        },
        overlay = {
            CountryPickerSheet(payer)
            // The camera covers the whole screen like the bank page, not the sheet: no rounded top, no sheet height.
            if (scanner != null) {
                PlatformBackHandler(enabled = scannerOpen) { scannerOpen = false }
                CoverWhile(scannerOpen)
                AnimatedVisibility(
                    visible = scannerOpen,
                    enter = slideInVertically(tween(SCANNER_SLIDE_MS)) { it },
                    exit = slideOutVertically(tween(SCANNER_SLIDE_MS)) { it },
                ) {
                    // Light status bar icons over the black camera; the form's look comes back when it closes.
                    SystemBarsAppearance(dark = true)
                    val density = LocalDensity.current
                    val safeBottom = with(density) { WindowInsets.safeDrawing.getBottom(density).toDp() }
                    // The bottom card and buttons end where the sheet's Pay button does.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                            .padding(bottom = (safeBottom - BottomInsetReach).coerceAtLeast(0.dp)),
                    ) {
                        if (cameraOn) {
                            scanner.CardScannerScreen(config = CardScannerConfig(), onResult = onScanned)
                        } else {
                            ScanIntro(
                                onTurnOn = {
                                    scanIntroSeen = true
                                    cameraOn = true
                                },
                                onManual = { scannerOpen = false },
                            )
                        }
                    }
                }
            }
        },
    ) {
        val wallets = if (showWallets) rememberWalletAvailability(config) else null
        if (wallets != null && wallets.any) {
            if (wallets.googlePay) GooglePayButton(onClick = onGooglePayClick, modifier = Modifier.fillMaxWidth().height(52.dp))
            if (wallets.applePay) ApplePayButton(onClick = onApplePayClick, modifier = Modifier.fillMaxWidth().height(52.dp))
            LabeledDivider(stringResource(Res.string.sheet_or_pay_by_card))
        }
        // "Save card" belongs to the card: right under its fields, 12dp apart (8dp here plus the row's own 4dp).
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CardDetailsGroup(
                pan = pan,
                onPanChange = { digits ->
                    pan = digits
                    val complete = digits.length >= CardFormatters.maxPanLength(CardNetworkResolver.resolve(digits)).coerceAtMost(16)
                    if (complete) touched = touched + Field.PAN
                },
                expiry = expiry,
                onExpiryChange = {
                    expiry = it
                    if (it.length == 4) touched = touched + Field.EXPIRY
                },
                cvc = cvc,
                onCvcChange = {
                    cvc = it
                    if (it.length == CardFormatters.cvvLength(network)) touched = touched + Field.CVV
                },
                network = network,
                panStatus = statusOf(Field.PAN),
                expiryStatus = statusOf(Field.EXPIRY),
                cvcStatus = statusOf(Field.CVV),
                // Every error in words, not only the first: the others would show only as a red frame.
                message =
                    listOf(Field.PAN, Field.EXPIRY, Field.CVV, Field.HOLDER)
                        .mapNotNull { errorOf(it) }
                        .joinToString("\n")
                        .ifEmpty { null },
                holder = holder.takeIf { CARD_HOLDER_FIELD_ENABLED },
                onHolderChange = { holder = it },
                onHolderFocusChange = { focused ->
                    // Checked once the buyer leaves the field, not while typing.
                    if (focused) {
                        holderVisited = true
                    } else if (holderVisited) {
                        touched = touched + Field.HOLDER
                    }
                },
                holderStatus = statusOf(Field.HOLDER),
                labelActions =
                    if (scanner != null) {
                        {
                            TextAction(
                                text = stringResource(Res.string.card_form_scan),
                                icon = PaymentIcons.Scan,
                                onClick = openScanner,
                                // Line the text up with the group's right edge: TextAction carries its own 8dp padding.
                                modifier = Modifier.offset(x = 8.dp),
                            )
                        }
                    } else {
                        null
                    },
            )
            if (canSaveCard) {
                CheckboxRow(
                    title = stringResource(Res.string.card_form_save_card),
                    description = stringResource(Res.string.card_form_save_card_hint),
                    checked = saveCard,
                    onCheckedChange = { if (!saveCardLocked) saveCard = it },
                )
            }
        }
        PayerFieldsSection(state = payer, paymentSystems = paymentSystems, collapsibleBilling = true)
    }
}

internal fun FieldError.message(): StringResource =
    when (this) {
        FieldError.PAN_EMPTY -> Res.string.error_pan_empty
        FieldError.PAN_SHORT -> Res.string.error_pan_short
        FieldError.PAN_INVALID -> Res.string.error_pan_invalid
        FieldError.EXPIRY_FORMAT -> Res.string.error_expiry_format
        FieldError.EXPIRY_MONTH -> Res.string.error_expiry_month
        FieldError.EXPIRY_PAST -> Res.string.error_expiry_past
        FieldError.CVV_3 -> Res.string.error_cvv_3
        FieldError.CVV_4 -> Res.string.error_cvv_4
        FieldError.HOLDER_EMPTY -> Res.string.error_holder_empty
        FieldError.HOLDER_FORMAT -> Res.string.error_holder_format
        FieldError.HOLDER_SHORT -> Res.string.error_holder_short
        FieldError.EMAIL_EMPTY -> Res.string.error_email_empty
        FieldError.EMAIL_INVALID -> Res.string.error_email_invalid
        FieldError.PHONE_EMPTY -> Res.string.error_phone_empty
        FieldError.PHONE_SHORT -> Res.string.error_phone_short
        FieldError.PHONE_LONG -> Res.string.error_phone_long
        FieldError.PHONE_NO_CODE -> Res.string.error_phone_no_code
        FieldError.FIELD_REQUIRED -> Res.string.error_field_required
        FieldError.COUNTRY_EMPTY -> Res.string.error_country_empty
        FieldError.LATIN_ONLY -> Res.string.error_latin_only
        FieldError.POSTAL_CODE_INVALID -> Res.string.error_postal_code_invalid
        FieldError.DATE_INVALID -> Res.string.error_date_invalid
    }

/** How long the camera takes to slide over the screen and away. */
private const val SCANNER_SLIDE_MS = 300

/**
 * The camera screen before the camera: a frame where the card will go and, under it, why the camera is asked for.
 * The system's permission prompt comes only after [onTurnOn], so it reads as the answer to this card.
 */
@Composable
private fun ScanIntro(
    onTurnOn: () -> Unit,
    onManual: () -> Unit,
) {
    val typography = LocalPaymentTypography.current
    val shapes = LocalPaymentShapes.current
    Box(Modifier.fillMaxSize()) {
        CameraButton(
            PaymentIcons.Close,
            stringResource(Res.string.action_close),
            onManual,
            Modifier.align(Alignment.TopStart).padding(16.dp),
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .aspectRatio(CARD_ASPECT)
                .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .fillMaxWidth()
                    .background(Color(0xFF1C1C1E), RoundedCornerShape(22.dp))
                    .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(Res.string.scan_title),
                style = typography.headline.copy(color = Color.White),
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(Res.string.scan_body),
                style = typography.body.copy(color = Color.White.copy(alpha = 0.7f)),
                modifier = Modifier.padding(bottom = 6.dp),
            )
            CameraTextButton(stringResource(Res.string.scan_turn_on), onTurnOn, Color.White, Color.Black, shapes.button)
            CameraTextButton(stringResource(Res.string.scan_manual), onManual, Color.White.copy(alpha = 0.14f), Color.White, shapes.button)
        }
    }
}

@Composable
private fun CameraTextButton(
    text: String,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    shape: Shape,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(shape)
                .background(container)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = LocalPaymentTypography.current.button.copy(color = content))
    }
}

@Composable
private fun CameraButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

/** ID-1 card: 85.60 × 53.98 mm. */
private const val CARD_ASPECT = 1.586f
