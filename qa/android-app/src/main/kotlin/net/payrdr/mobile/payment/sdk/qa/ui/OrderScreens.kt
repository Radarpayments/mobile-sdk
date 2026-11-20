package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import net.payrdr.mobile.payment.sdk.qa.config.Currencies
import net.payrdr.mobile.payment.sdk.qa.config.OrderSource
import net.payrdr.mobile.payment.sdk.qa.config.Problem
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.config.launchProblems
import net.payrdr.mobile.payment.sdk.qa.config.namesFromText
import net.payrdr.mobile.payment.sdk.qa.config.namesToText
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Caption
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Chip
import net.payrdr.mobile.payment.sdk.qa.ui.kit.FieldUnit
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Help
import net.payrdr.mobile.payment.sdk.qa.ui.kit.InputLook
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Link
import net.payrdr.mobile.payment.sdk.qa.ui.kit.ListGroup
import net.payrdr.mobile.payment.sdk.qa.ui.kit.NavRow
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Note
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaMenu
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaMenuItem
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaScreen
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaSize
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaType
import net.payrdr.mobile.payment.sdk.qa.ui.kit.TextInput

/** The order: where it comes from and, for one the app registers, what it is. Only the fields of the source show. */
@Composable
internal fun OrderScreen(
    session: QaSession,
    profiles: List<StandProfile>,
    onBack: () -> Unit,
    open: (String) -> Unit,
) {
    val form = session.form
    val order = form.config.order
    val fields = orderFields(order.source)
    // A required id is asked for here, before Launch would refuse it.
    val launchChecks = launchProblems(form.config, profiles.find { it.name == form.config.profile })
    QaScreen("Order", onBack) {
        FormSegments(form, Tags.ORDER_SOURCE, "Source", SOURCES)
        if (Tags.ORDER_TYPE in fields) {
            FormSegments(form, Tags.ORDER_TYPE, "Type", TYPES)
            launchChecks.find { it.field == "order.type" }?.let { Help(rowProblem(it), isProblem = true) }
        }
        if (Tags.ORDER_AMOUNT in fields) AmountField(form)
        if (Tags.ORDER_FEATURES in fields) FeaturesField(form)
        if (Tags.ORDER_CLIENT_ID in fields) {
            Caption("Optional")
            ListGroup {
                NavRow("Client id", clientIdSummary(order), Tags.ORDER_CLIENT_ID_OPEN, { open(Route.field(Tags.ORDER_CLIENT_ID)) })
                NavRow("Extra parameters", extraSummary(order), Tags.ORDER_EXTRA_OPEN, { open(Route.field(Tags.ORDER_EXTRA)) })
            }
        }
        if (Tags.ORDER_MD_ORDER in fields) {
            IdField(form, Tags.ORDER_MD_ORDER, Tags.ORDER_MD_ORDER_PASTE, "mdOrder", "Enter the order's id", launchChecks)
            Note(MD_ORDER_NOTE)
        }
        if (Tags.ORDER_SESSION_ID in fields) {
            IdField(form, Tags.ORDER_SESSION_ID, Tags.ORDER_SESSION_ID_PASTE, "Session id", "Enter the session's id", launchChecks)
            Note(SESSION_ID_NOTE)
        }
    }
}

/** The amount in major units with the currency picker inside the field, and the minor units it makes under it. */
@Composable
private fun AmountField(form: ConfigForm) {
    val order = form.config.order
    val problem = form.problem(Tags.ORDER_AMOUNT)
    var picking by rememberSaveable { mutableStateOf(false) }
    Caption("Amount")
    TextInput(
        value = form.text(Tags.ORDER_AMOUNT),
        onValueChange = { form.edit(Tags.ORDER_AMOUNT, it) },
        tag = Tags.ORDER_AMOUNT,
        isProblem =
            problem != null,
        look = InputLook(keyboardType = KeyboardType.Decimal),
    ) {
        FieldUnit(form.text(Tags.ORDER_CURRENCY), Tags.ORDER_CURRENCY, onClick = { picking = true }) {
            QaMenu(picking, onDismiss = { picking = false }) {
                Currencies.codes.forEach { code ->
                    QaMenuItem(code, Tags.option(Tags.ORDER_CURRENCY, code), {
                        picking = false
                        form.edit(Tags.ORDER_CURRENCY, code)
                    }, selected = code == order.currency)
                }
            }
        }
    }
    if (problem != null) {
        Help(rowProblem(problem), isProblem = true)
    } else {
        Help(if (order.amount == 1L) "1 minor unit" else "${order.amount} minor units")
    }
}

/** The features as chips; Other adds a name the app does not list, which then shows as a chip of its own. */
@Composable
private fun FeaturesField(form: ConfigForm) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    val add = {
        val chosen = namesFromText(form.text(Tags.ORDER_FEATURES))
        val typed = name.trim()
        if (typed.isNotEmpty() && typed !in chosen) form.edit(Tags.ORDER_FEATURES, namesToText(chosen + typed))
        name = ""
        adding = false
    }
    Caption("Features")
    FormChips(form, Tags.ORDER_FEATURES, KNOWN_FEATURES, chipTag = Tags::feature) {
        Chip("+ Other", adding, Tags.ORDER_FEATURES_OTHER) { adding = !adding }
    }
    if (adding) {
        Box(Modifier.padding(top = QaSize.gap)) {
            TextInput(value = name, onValueChange = {
                name = it
            }, tag = Tags.ORDER_FEATURES_OTHER_NAME, look = InputLook(placeholder = "FEATURE_NAME")) {
                Link("Add", Tags.ORDER_FEATURES_OTHER_ADD, add)
            }
        }
    }
}

/** An order id typed or pasted; empty, it shows the problem a launch would be refused for. */
@Composable
private fun IdField(
    form: ConfigForm,
    tag: String,
    pasteTag: String,
    caption: String,
    hint: String,
    launchChecks: List<Problem>,
) {
    val context = LocalContext.current
    val problem = form.problem(tag)
    val missing = launchChecks.find { it.field == FIELD_OF[tag] }
    Caption(caption)
    TextInput(value = form.text(tag), onValueChange = {
        form.edit(tag, it)
    }, tag = tag, isProblem = problem != null || missing != null, look = InputLook(placeholder = PLACEHOLDER, style = QaType.inputSmall)) {
        Link("Paste", pasteTag, { form.edit(tag, context.clipboardText().trim()) })
    }
    when {
        problem != null -> Help(rowProblem(problem), isProblem = true)
        missing != null -> Help(hint, isProblem = true)
    }
}

/** A text field of the order on a screen of its own: the client id, the extra parameters. */
@Composable
internal fun FieldScreen(
    form: ConfigForm,
    tag: String,
    onBack: () -> Unit,
) {
    val field = OWN_SCREEN_FIELDS[tag] ?: return
    QaScreen(field.title, onBack) {
        FormInput(form, tag, field.title, help = field.help, look = InputLook(singleLine = field.singleLine, style = QaType.inputSmall))
    }
}

private class OwnScreenField(
    val title: String,
    val help: String,
    val singleLine: Boolean,
)

private val OWN_SCREEN_FIELDS =
    mapOf(
        Tags.ORDER_CLIENT_ID to
            OwnScreenField("Client id", "Sent as clientId, as merchantCustomerId with API v2; empty sends none", singleLine = true),
        Tags.ORDER_EXTRA to
            OwnScreenField(
                "Extra parameters",
                "A JSON object added to the registration request; null leaves a parameter out",
                singleLine = false,
            ),
    )

private val SOURCES =
    listOf(
        OrderSource.REGISTER.json to "Register",
        OrderSource.MD_ORDER.json to "mdOrder",
        OrderSource.SESSION_ID.json to "Session id",
    )
private val TYPES = listOf("payment" to "Payment", "aft" to "AFT")
private val KNOWN_FEATURES = listOf("FORCE_TDS", "FORCE_SSL", "VERIFY", "FORCE_CREATE_BINDING", "AUTO_PAYMENT")
private val FIELD_OF = mapOf(Tags.ORDER_MD_ORDER to "order.mdOrder", Tags.ORDER_SESSION_ID to "order.sessionId")
private const val PLACEHOLDER = "01ea8a09-9d44-…"
private const val MD_ORDER_NOTE =
    "The app does not register this order: type, amount and features are not used. The order's status is read only " +
        "with an API v1 stand."
private const val SESSION_ID_NOTE =
    "The app does not register this session: type, amount and features are not used."
