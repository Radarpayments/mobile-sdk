package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable

/**
 * Status and navigation bar icons that stay readable on the checkout: light icons on a [dark]
 * background, dark ones otherwise — whatever the system theme is. The previous look is restored
 * when the checkout leaves the composition.
 */
@Composable
internal expect fun SystemBarsAppearance(dark: Boolean)
