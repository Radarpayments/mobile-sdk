package net.payrdr.mobile.payment.sdk.payment.ui.payer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.payrdr.mobile.payment.sdk.payment.result.PayerDataRequest
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.ui.util.deviceRegion

@Composable
internal fun rememberPayerFieldsState(
    request: PayerDataRequest,
    prefillOverride: Map<PayerField, String> = emptyMap(),
): PayerFieldsState = remember(request, prefillOverride) { PayerFieldsState(request, prefillOverride, deviceRegion()) }
