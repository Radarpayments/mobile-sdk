package net.payrdr.mobile.payment.sdk.payment.ui.payer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PayerFieldSpec
import net.payrdr.mobile.payment.sdk.payment.ui.LocalSdkLanguage
import net.payrdr.mobile.payment.sdk.payment.ui.components.FieldGroupHeader
import net.payrdr.mobile.payment.sdk.payment.ui.components.PaymentIcons
import net.payrdr.mobile.payment.sdk.payment.ui.components.TextAction
import net.payrdr.mobile.payment.sdk.payment.ui.payer.phone.PhoneNumbers
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.util.countryDisplayName
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.action_edit
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_summary_collapse
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_summary_title
import org.jetbrains.compose.resources.stringResource

/**
 * Payer fields of a saved card shown as a short summary — email, phone, the address in one line — with "Edit"
 * that opens them as [PayerFieldsSection] in place. The fields start open when there is nothing known to sum up
 * or a known value would not pass (a required field is empty, say); "Hide" closes them only once they are valid.
 */
@Composable
internal fun PayerSummarySection(
    state: PayerFieldsState,
    paymentSystems: List<String>,
    modifier: Modifier = Modifier,
) {
    val specs = state.specsFor(paymentSystems)
    val lines = summaryLines(state, specs)
    if (lines.isEmpty()) {
        PayerFieldsSection(state = state, paymentSystems = paymentSystems, modifier = modifier)
        return
    }
    var expanded by remember(state) { mutableStateOf(specs.any { state.errorOf(it) != null }) }
    AnimatedContent(
        targetState = expanded,
        transitionSpec = { summaryTransition() },
        label = "payer summary",
        modifier = modifier.fillMaxWidth(),
    ) { open ->
        if (open) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FieldGroupHeader(title = stringResource(Res.string.payer_summary_title)) {
                    TextAction(
                        text = stringResource(Res.string.payer_summary_collapse),
                        // Hiding a wrong value would send it unseen: the errors show instead.
                        onClick = { if (state.validateAll(state.specsFor(paymentSystems))) expanded = false },
                        modifier = Modifier.offset(x = 8.dp),
                    )
                }
                PayerFieldsSection(state = state, paymentSystems = paymentSystems)
            }
        } else {
            PayerSummaryCard(
                title = stringResource(Res.string.payer_summary_title),
                lines = lines,
                onEdit = { expanded = true },
                modifier = Modifier.testTag("payer_summary"),
            )
        }
    }
}

/** Framed [title] with "Edit" over one-line values, each behind its icon. */
@Composable
internal fun PayerSummaryCard(
    title: String,
    lines: List<SummaryLine>,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPaymentColors.current
    val shape = LocalPaymentShapes.current.field
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.background, shape)
                .border(1.dp, colors.outline, shape)
                .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FieldGroupHeader(title = title, modifier = Modifier.padding(top = 8.dp)) {
            TextAction(text = stringResource(Res.string.action_edit), onClick = onEdit, modifier = Modifier.offset(x = 8.dp))
        }
        lines.forEach { SummaryRow(it) }
    }
}

@Composable
private fun SummaryRow(line: SummaryLine) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(line.icon, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(
            text = line.text,
            style = typography.body.copy(color = colors.onBackground),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal class SummaryLine(
    val icon: ImageVector,
    val text: String,
)

/** Known values of the fields the gateway asks for: email, phone, the billing address joined into one line. */
@Composable
private fun summaryLines(
    state: PayerFieldsState,
    specs: List<PayerFieldSpec>,
): List<SummaryLine> {
    val asked = specs.map { it.field }.toSet()

    fun known(field: PayerField) = state.value(field).trim().takeIf { field in asked && it.isNotEmpty() }

    val phone =
        known(PayerField.MOBILE_PHONE)?.let {
            val country = state.phoneCountry ?: return@let ltr(it)
            ltr("+${country.dialCode} ${PhoneNumbers.format(state.phoneNational, country)}")
        }
    val address = billingSummary(state, specs)
    return listOfNotNull(
        known(PayerField.EMAIL)?.let { SummaryLine(PaymentIcons.Mail, it) },
        phone?.let { SummaryLine(PaymentIcons.Phone, it) },
        address.takeIf { it.isNotEmpty() }?.let { SummaryLine(PaymentIcons.Pin, it) },
    )
}

/** Known billing values of [specs] in one line: street, city and postal code, state, country. */
@Composable
internal fun billingSummary(
    state: PayerFieldsState,
    specs: List<PayerFieldSpec>,
): String {
    val asked = specs.map { it.field }.toSet()

    fun known(field: PayerField) = state.value(field).trim().takeIf { field in asked && it.isNotEmpty() }

    val language = LocalSdkLanguage.current
    return listOfNotNull(
        known(PayerField.BILLING_ADDRESS_LINE1),
        known(PayerField.BILLING_ADDRESS_LINE2),
        known(PayerField.BILLING_ADDRESS_LINE3),
        listOfNotNull(known(PayerField.BILLING_CITY), known(PayerField.BILLING_POSTAL_CODE))
            .joinToString(" ")
            .takeIf { it.isNotEmpty() },
        known(PayerField.BILLING_STATE),
        known(PayerField.BILLING_COUNTRY)?.let { countryDisplayName(it, language) },
    ).joinToString(", ")
}

/** Cross-fade between a summary and its fields, the height following. */
internal fun <S> AnimatedContentTransitionScope<S>.summaryTransition(): ContentTransform =
    fadeIn(tween(SUMMARY_FADE_MS, delayMillis = SUMMARY_FADE_MS)) togetherWith
        fadeOut(tween(SUMMARY_FADE_MS)) using SizeTransform(clip = true)

private const val SUMMARY_FADE_MS = 140
