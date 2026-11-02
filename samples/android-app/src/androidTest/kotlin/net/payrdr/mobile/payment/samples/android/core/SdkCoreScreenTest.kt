package net.payrdr.mobile.payment.samples.android.core

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.prefs.AppPrefs
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SdkCoreScreenTest {
    @get:Rule val rule = createComposeRule()
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun newCardWithTheExampleKey() {
        runBlocking {
            AppPrefs(ctx).apply {
                setSdkCoreVariant(SdkCoreVariant.NEW_CARD.storageKey)
                setMdOrder(AppPrefs.MD_ORDER)
                setPubKeyPem(AppPrefs.PUB_KEY_PEM)
            }
        }
    }

    private fun show() {
        rule.setContent { SdkCoreScreen(onBack = {}) }
        // The form appears once the stored mdOrder and key are read.
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag(CORE_GENERATE_TAG)).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun noOrderVariantHasNoMdOrderField() {
        show()
        rule.onNodeWithTag(coreFieldTag(CoreField.MD_ORDER)).assertExists()
        rule.onNodeWithText(ctx.getString(R.string.core_variant_instant)).performClick()
        rule.onNodeWithTag(coreFieldTag(CoreField.MD_ORDER)).assertDoesNotExist()
    }

    @Test fun emptyCardShowsWhatIsMissingNextToTheField() {
        show()
        rule.onNodeWithTag(CORE_GENERATE_TAG).performScrollTo().performClick()
        rule.onNodeWithTag(coreErrorTag(CoreField.PAN)).performScrollTo().assertIsDisplayed()
        rule.onNode(hasTestTag(CORE_TOKEN_TAG)).assertDoesNotExist()
    }

    @Test fun cardDataTurnIntoAToken() {
        show()
        rule.onNodeWithTag(coreFieldTag(CoreField.PAN)).performTextInput("4111111111111111")
        rule.onNodeWithTag(coreFieldTag(CoreField.EXPIRY)).performTextInput("1230")
        rule.onNodeWithTag(coreFieldTag(CoreField.CVC)).performTextInput("123")
        rule.onNodeWithTag(CORE_GENERATE_TAG).performScrollTo().performClick()
        rule.onNodeWithTag(CORE_TOKEN_TAG).assertIsDisplayed()
        rule.onNode(hasText(ctx.getString(R.string.core_token_ok))).assertIsDisplayed()
        // "Again" goes back to the form with the card still filled in.
        rule.onNodeWithText(ctx.getString(R.string.core_again)).performScrollTo().performClick()
        rule.onNodeWithTag(coreFieldTag(CoreField.PAN)).assert(hasText("4111111111111111"))
    }

    @Test fun numberAndCvcTakeTheNetworksLength() {
        show()
        // Amex: 15 digits at most, a 4-digit CVC; then Visa cuts the CVC to 3.
        rule.onNodeWithTag(coreFieldTag(CoreField.PAN)).performTextInput("378282246310005999")
        rule.onNodeWithTag(coreFieldTag(CoreField.PAN)).assert(hasText("378282246310005"))
        rule.onNodeWithTag(coreFieldTag(CoreField.CVC)).performTextInput("12345")
        rule.onNodeWithTag(coreFieldTag(CoreField.CVC)).assert(hasText("1234"))
        rule.onNodeWithTag(coreFieldTag(CoreField.PAN)).performTextClearance()
        rule.onNodeWithTag(coreFieldTag(CoreField.PAN)).performTextInput("4111111111111111")
        rule.onNodeWithTag(coreFieldTag(CoreField.CVC)).assert(hasText("123"))
    }
}
