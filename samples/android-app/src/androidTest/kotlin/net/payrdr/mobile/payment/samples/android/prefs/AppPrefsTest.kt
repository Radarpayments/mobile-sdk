package net.payrdr.mobile.payment.samples.android.prefs

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.model.Scenario
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AppPrefsTest {
    @Test fun default_baseUrl_loaded_when_absent() =
        runBlocking {
            val ctx = InstrumentationRegistry.getInstrumentation().targetContext
            val prefs = AppPrefs(ctx)
            val loaded = prefs.flow.first()
            assertEquals("https://dev.bpcbt.com/payment", loaded.baseUrl)
        }

    @Test fun roundtrip_mdOrder() =
        runBlocking {
            val ctx = InstrumentationRegistry.getInstrumentation().targetContext
            val prefs = AppPrefs(ctx)
            prefs.setMdOrder("custom-id-42")
            val loaded = prefs.flow.first()
            assertEquals("custom-id-42", loaded.mdOrder)
        }

    private val prefs get() = AppPrefs(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test fun credentials_have_no_built_in_defaults() {
        assertEquals("", AppPrefs.Defaults.MERCHANT_LOGIN)
        assertEquals("", AppPrefs.Defaults.MERCHANT_PASSWORD)
    }

    @Test fun credentials_roundtrip() =
        runBlocking {
            prefs.setCredentials("demo-login", "demo-password")
            val loaded = prefs.flow.first()
            assertEquals("demo-login", loaded.merchantLogin)
            assertEquals("demo-password", loaded.merchantPassword)
        }

    @Test fun clientId_stable_across_reads() =
        runBlocking {
            val first = prefs.flow.first().clientId
            assertTrue(first.isNotBlank())
            assertEquals(first, prefs.flow.first().clientId)
        }

    @Test fun clientId_is_short_like_the_ids_the_gateway_accepts() =
        runBlocking {
            val id = prefs.resetClientId()
            assertTrue(id.length <= 24, "clientId '$id' is ${id.length} chars")
        }

    @Test fun resetClientId_returns_new_persisted_id() =
        runBlocking {
            val before = prefs.flow.first().clientId
            val fresh = prefs.resetClientId()
            assertNotEquals(before, fresh)
            assertEquals(fresh, prefs.flow.first().clientId)
        }

    @Test fun scenario_roundtrip() =
        runBlocking {
            val scenario = Scenario(buyerWithCards = true, wallet = false, scanner = false, sdkResultScreen = true)
            prefs.setScenario(scenario)
            assertEquals(scenario, prefs.flow.first().scenario)
        }

    @Test fun lastCover_roundtrip() =
        runBlocking {
            prefs.setLastCover(CoverId.COFFEE)
            assertEquals(CoverId.COFFEE, prefs.flow.first().lastCover)
        }
}
