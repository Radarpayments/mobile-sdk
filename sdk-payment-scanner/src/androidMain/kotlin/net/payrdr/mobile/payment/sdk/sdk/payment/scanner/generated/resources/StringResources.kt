package net.payrdr.mobile.payment.sdk.sdk.payment.scanner.generated.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.payrdr.mobile.payment.sdk.payment.scanner.presentation.ScannerTexts
import net.payrdr.mobile.payment.sdk.payment.ui.LocalSdkLanguage

/** The scanner's text of [resource] in the checkout's language, English when that language lacks it. */
@Composable
internal fun stringResource(resource: StringResource): String {
    val language = LocalSdkLanguage.current
    return remember(language) { ScannerTexts(language) }.text(resource.key)
}
