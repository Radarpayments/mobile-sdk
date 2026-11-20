package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Caption
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Footer
import net.payrdr.mobile.payment.sdk.qa.ui.kit.FooterWhy
import net.payrdr.mobile.payment.sdk.qa.ui.kit.InputLook
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Note
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaButton
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaScreen
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaType
import net.payrdr.mobile.payment.sdk.qa.ui.kit.TextInput

/** A stand or a configuration typed or pasted into a field, for a device whose clipboard is not the tester's. */
@Composable
internal fun ImportTextScreen(
    session: QaSession,
    onBack: () -> Unit,
) {
    val import = session.textImport
    QaScreen(
        title = "Import from text",
        onBack = onBack,
        footer = {
            Footer(why = { import.problem?.let { FooterWhy("${it.field}: ${it.message}", Modifier.testTag(Tags.VALIDATION_ERROR)) } }) {
                QaButton("Import", Tags.IMPORT_TEXT_APPLY, { if (import.submit(session::accept)) onBack() }, enabled = import.canImport)
            }
        },
    ) {
        Note(HINT)
        Caption("Text")
        TextInput(
            import.text,
            import::edit,
            Tags.IMPORT_TEXT_FIELD,
            isProblem = import.problem != null,
            look = InputLook(singleLine = false, style = QaType.inputSmall, autoCorrect = false),
        )
    }
}

private const val HINT = "A stand or a configuration as JSON or a payrdrqa:// link"
