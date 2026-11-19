package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.payrdr.mobile.payment.sdk.qa.config.Problem
import net.payrdr.mobile.payment.sdk.qa.intake.Intake
import net.payrdr.mobile.payment.sdk.qa.intake.intakeFromText
import net.payrdr.mobile.payment.sdk.qa.intake.trimInput

/**
 * The text typed or pasted on the Import from text screen, read as clipboard text is: on a cloud device the clipboard is
 * not the tester's, but keyboard input is. Held in memory only, never in the saved instance state, which the system may
 * write to disk: a profile's text carries its password.
 */
internal class TextImport {
    var text by mutableStateOf("")
        private set

    /** Why the last import was rejected, until the text changes. */
    var problem: Problem? by mutableStateOf(null)
        private set

    /** Off while the text is empty after the trimming of every way in, which would only be the `$` problem. */
    val canImport: Boolean get() = trimInput(text).isNotEmpty()

    fun edit(value: String) {
        text = value
        problem = null
    }

    fun clear() = edit("")

    /**
     * Hands the text's intake to [accept], as the clipboard import does. True when the screen is done; on a rejection the
     * text stays for a fix and [problem] says what is wrong.
     */
    fun submit(accept: (List<Intake>) -> Unit): Boolean {
        if (!canImport) return false
        val steps = intakeFromText(text)
        accept(steps)
        problem = steps.firstNotNullOfOrNull { (it as? Intake.Rejected)?.problem }
        if (problem == null) text = ""
        return problem == null
    }
}
