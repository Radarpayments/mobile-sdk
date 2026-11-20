package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import net.payrdr.mobile.payment.sdk.qa.config.Problem
import net.payrdr.mobile.payment.sdk.qa.config.namesFromText
import net.payrdr.mobile.payment.sdk.qa.config.namesToText
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Caption
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Chip
import net.payrdr.mobile.payment.sdk.qa.ui.kit.ChipFlow
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Help
import net.payrdr.mobile.payment.sdk.qa.ui.kit.InputLook
import net.payrdr.mobile.payment.sdk.qa.ui.kit.ListGroup
import net.payrdr.mobile.payment.sdk.qa.ui.kit.MarkRow
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Segment
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Segmented
import net.payrdr.mobile.payment.sdk.qa.ui.kit.SwitchRow
import net.payrdr.mobile.payment.sdk.qa.ui.kit.TextInput

/**
 * A field of the form under its caption: the input, then its problem or [help]. [problem] is a problem the form does
 * not hold, such as a required field a launch would be refused for; the form's own problem comes first.
 */
@Composable
internal fun FormInput(
    form: ConfigForm,
    tag: String,
    caption: String,
    help: String? = null,
    problem: Problem? = null,
    look: InputLook = InputLook(),
) {
    val shown = form.problem(tag) ?: problem
    Caption(caption)
    TextInput(form.text(tag), { form.edit(tag, it) }, tag, isProblem = shown != null, look = look)
    when {
        shown != null -> Help(rowProblem(shown), isProblem = true)
        help != null -> Help(help)
    }
}

/** A boolean field as a row with a switch; the row carries the field's tag. */
@Composable
internal fun FormSwitch(
    form: ConfigForm,
    tag: String,
    label: String,
) {
    SwitchRow(label, form.text(tag) == "true", tag) { form.edit(tag, it.toString()) }
}

/** A field of two to four values as segments; [labels] maps each JSON value to its label. */
@Composable
internal fun FormSegments(
    form: ConfigForm,
    tag: String,
    caption: String,
    labels: List<Pair<String, String>>,
) {
    Caption(caption)
    Segmented(
        segments = labels.map { (value, label) -> Segment(value, label, Tags.option(tag, value)) },
        selected = form.text(tag),
        tag = tag,
        onSelect = { form.edit(tag, it) },
    )
    form.problem(tag)?.let { Help(rowProblem(it), isProblem = true) }
}

/**
 * A one-of field as a list of marks; each row carries [Tags.option] of its value. With [listTag] the list carries that
 * tag and reads as the JSON value, `""` for none.
 */
@Composable
internal fun FormMarks(
    form: ConfigForm,
    tag: String,
    options: List<Pair<String, String>>,
    listTag: String? = tag,
    onChosen: () -> Unit = {},
) {
    val selected = form.text(tag)
    val modifier = if (listTag != null) Modifier.semantics { text = AnnotatedString(selected) }.testTag(listTag) else Modifier
    ListGroup(modifier) {
        options.forEach { (value, label) ->
            MarkRow(label, value == selected, Tags.option(tag, value)) {
                form.edit(tag, value)
                onChosen()
            }
        }
    }
}

/**
 * A set field as chips: [known] first in their order, then the chosen names that are not among them. The chips' frame
 * carries the field's tag and reads as the names joined by `,`, as the field's text does.
 */
@Composable
internal fun FormChips(
    form: ConfigForm,
    tag: String,
    known: List<String>,
    chipTag: (String) -> String = { Tags.option(tag, it) },
    extra: @Composable () -> Unit = {},
) {
    val text = form.text(tag)
    val chosen = namesFromText(text)
    Column(Modifier.semantics { this.text = AnnotatedString(text) }.testTag(tag)) {
        ChipFlow {
            (known + chosen.filter { it !in known }).forEach { name ->
                val on = name in chosen
                Chip(name, on, chipTag(name)) {
                    form.edit(tag, namesToText(if (on) chosen - name else chosen + name))
                }
            }
            extra()
        }
    }
    form.problem(tag)?.let { Help(rowProblem(it), isProblem = true) }
}
