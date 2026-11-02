package net.payrdr.mobile.payment.samples.android.store

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StartScreenTest {
    @get:Rule val rule = createComposeRule()
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun showsThreeCovers() {
        rule.setContent { StartScreen(onCover = {}, onSettings = {}, onCore = {}) }
        rule.onNodeWithText("Hearth & Co").assertIsDisplayed()
        rule.onNodeWithText("Roast Lane").assertExists()
        rule.onNodeWithText("Stage Pass").assertExists()
    }

    @Test fun coverTapOpensThatCover() {
        var opened: CoverId? = null
        rule.setContent { StartScreen(onCover = { opened = it }, onSettings = {}, onCore = {}) }
        rule.onNodeWithText("Roast Lane").performClick()
        assertEquals(CoverId.COFFEE, opened)
    }

    @Test fun sectionsNameBothSdks() {
        rule.setContent { StartScreen(onCover = {}, onSettings = {}, onCore = {}) }
        rule.onNodeWithText(ctx.getString(R.string.start_section_payment)).assertIsDisplayed()
        rule.onNodeWithText(ctx.getString(R.string.start_section_core)).performScrollTo().assertIsDisplayed()
    }

    @Test fun coreTileOpensSdkCore() {
        var opened = false
        rule.setContent { StartScreen(onCover = {}, onSettings = {}, onCore = { opened = true }) }
        rule.onNodeWithText(ctx.getString(R.string.core_tile_title)).performScrollTo().performClick()
        assertTrue(opened)
    }

    @Test fun gearOpensDeveloperScreen() {
        var opened = false
        rule.setContent { StartScreen(onCover = {}, onSettings = { opened = true }, onCore = {}) }
        rule.onNodeWithContentDescription(ctx.getString(R.string.start_settings)).performClick()
        assertTrue(opened)
    }
}
