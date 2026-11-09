package net.payrdr.mobile.payment.samples.android.dev

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.payrdr.mobile.payment.samples.android.prefs.AppPrefs
import org.junit.Rule
import org.junit.Test

class DeveloperScreenTest {
    @get:Rule val rule = createComposeRule()
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    private fun show() {
        rule.setContent { DeveloperScreen(onBack = {}, onAdvanced = {}) }
        // The screen appears once DataStore has loaded the settings.
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag(PASSWORD_FIELD_TAG)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun passwordIsMasked() {
        show()
        rule.onNodeWithTag(PASSWORD_FIELD_TAG).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
    }

    @Test fun enteredCredentialsAreSaved() {
        show()
        rule.onNodeWithTag(LOGIN_FIELD_TAG).performTextReplacement("ui-test-login")
        rule.onNodeWithTag(PASSWORD_FIELD_TAG).performTextReplacement("ui-test-password")
        rule.waitUntil(5_000) {
            runBlocking {
                AppPrefs(
                    ctx,
                ).flow.first().let { it.merchantLogin == "ui-test-login" && it.merchantPassword == "ui-test-password" }
            }
        }
        runBlocking { AppPrefs(ctx).setCredentials("", "") }
    }
}
