package net.payrdr.mobile.payment.samples.android.nav

import android.content.res.Configuration
import android.view.View
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import net.payrdr.mobile.payment.samples.android.AppLanguage
import net.payrdr.mobile.payment.samples.android.MainActivity
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.prefs.AppPrefs
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class SampleNavHostTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun opensStartScreenWhenNoCoverWasChosen() {
        runBlocking { AppPrefs(ctx).setLastCover(null) }
        // The texts below are read in the system language.
        AppLanguage.save(ctx, null)
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.onNodeWithText(ctx.getString(R.string.start_title)).assertExists()
        }
    }

    @Test fun reopensTheLastCover() {
        runBlocking { AppPrefs(ctx).setLastCover(CoverId.TICKETS) }
        // The texts below are read in the system language.
        AppLanguage.save(ctx, null)
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.waitUntil(5_000) { rule.onAllNodesWithTextCount(ctx.getString(R.string.tickets_live)) > 0 }
            rule.onNodeWithText(ctx.getString(R.string.start_title)).assertDoesNotExist()
        }
        runBlocking { AppPrefs(ctx).setLastCover(null) }
    }

    @Test fun opensInTheChosenLanguage() {
        runBlocking { AppPrefs(ctx).setLastCover(null) }
        AppLanguage.save(ctx, "de")
        try {
            val german = ctx.createConfigurationContext(Configuration(ctx.resources.configuration).apply { setLocale(Locale.GERMAN) })
            ActivityScenario.launch(MainActivity::class.java).use {
                rule.onNodeWithText(german.getString(R.string.start_title)).assertExists()
            }
        } finally {
            AppLanguage.save(ctx, null)
        }
    }

    @Test fun arabicIsLaidOutRightToLeft() {
        runBlocking { AppPrefs(ctx).setLastCover(null) }
        AppLanguage.save(ctx, "ar")
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { assertEquals(View.LAYOUT_DIRECTION_RTL, it.resources.configuration.layoutDirection) }
            }
        } finally {
            AppLanguage.save(ctx, null)
        }
    }

    @Test fun gearOpensDeveloperSettings() {
        runBlocking { AppPrefs(ctx).setLastCover(null) }
        // The texts below are read in the system language.
        AppLanguage.save(ctx, null)
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.onNodeWithContentDescriptionText(ctx.getString(R.string.start_settings)).performClick()
            rule.onNodeWithText(ctx.getString(R.string.dev_title)).assertExists()
        }
    }
}

private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTextCount(text: String) =
    onAllNodes(
        androidx.compose.ui.test
            .hasText(text),
    ).fetchSemanticsNodes().size

private fun androidx.compose.ui.test.junit4.ComposeTestRule.onNodeWithContentDescriptionText(text: String) =
    onNode(
        androidx.compose.ui.test
            .hasContentDescription(text),
    )
