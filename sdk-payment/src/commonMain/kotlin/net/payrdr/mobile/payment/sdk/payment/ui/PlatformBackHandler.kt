package net.payrdr.mobile.payment.sdk.payment.ui

import androidx.compose.runtime.Composable

/** System back (Android back button / gesture). No-op on iOS, which has no system back. */
@Composable
internal expect fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)
