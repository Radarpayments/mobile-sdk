package net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.payrdr.mobile.payment.sdk.payment.presentation.PaymentStringTables
import net.payrdr.mobile.payment.sdk.payment.presentation.StringCatalog
import net.payrdr.mobile.payment.sdk.payment.ui.LocalSdkLanguage

/** The text of [resource] in the checkout's language, English when that language lacks it. */
@Composable
internal fun stringResource(resource: StringResource): String = rememberCatalog().text(resource.key)

/** The text of [resource] with its `%1$s`-style placeholders filled from [formatArgs]. */
@Composable
internal fun stringResource(
    resource: StringResource,
    vararg formatArgs: Any,
): String = rememberCatalog().format(resource.key, formatArgs.map { it.toString() })

@Composable
private fun rememberCatalog(): StringCatalog {
    val language = LocalSdkLanguage.current
    return remember(language) { StringCatalog.of(language, PaymentStringTables::table) }
}
