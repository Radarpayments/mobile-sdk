package net.payrdr.mobile.payment.sdk.payment.ui.screens

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

// The animation target is already 0 when the keyboard starts to hide; the plain ime inset only at the end.
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal actual fun rememberKeyboardOpen(): Boolean = WindowInsets.imeAnimationTarget.getBottom(LocalDensity.current) > 0
