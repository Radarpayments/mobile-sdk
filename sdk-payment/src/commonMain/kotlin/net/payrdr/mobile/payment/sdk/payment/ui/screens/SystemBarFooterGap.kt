package net.payrdr.mobile.payment.sdk.payment.ui.screens

import androidx.compose.ui.unit.Dp

/**
 * Extra space between a pinned footer and the bottom system inset: none on iOS, where the home
 * indicator area is part of the screen, some on Android, where the navigation bar is opaque.
 */
internal expect val systemBarFooterGap: Dp
