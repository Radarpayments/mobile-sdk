package net.payrdr.mobile.payment.sdk.payment.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/** System back (back button or gesture). */
@Composable
internal fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    BackHandler(enabled = enabled, onBack = onBack)
}
