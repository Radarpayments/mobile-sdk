package net.payrdr.mobile.payment.sdk.qa.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.payrdr.mobile.payment.sdk.qa.config.ApiVersion
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.config.QaConfigJson
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.config.StandProfileJson
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QaStoreTest {
    private val configKey = stringPreferencesKey("config")
    private val profilesKey = stringPreferencesKey("profiles")
    private val sandbox =
        StandProfile("sandbox", "https://dev.bpcbt.com/payment", login = "login-1", password = "password-1", apiKey = "key-1")

    private class PrefixBox(
        private val opens: Boolean = true,
    ) : SecretBox {
        // Reversed, so that a test can tell that the credentials are not stored as they are.
        override fun seal(plain: String) = "sealed:" + plain.reversed()

        override fun open(sealed: String): String? = if (opens) sealed.removePrefix("sealed:").reversed() else null
    }

    private fun TestScope.dataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(createTempDirectory("qa-store").toFile(), "qa.preferences_pb")
        }

    // The test dispatcher instead of Dispatchers.Default keeps the reads on the test's scheduler.
    private fun TestScope.qaStore(
        dataStore: DataStore<Preferences>,
        box: SecretBox,
    ) = QaStore(dataStore, box, StandardTestDispatcher(testScheduler))

    private fun TestScope.store(box: SecretBox = PrefixBox()) = qaStore(dataStore(), box)

    @Test
    fun `a fresh store holds the default config and no profiles`() =
        runTest {
            val store = store()
            store.config.first() shouldBe QaConfig()
            store.profiles.first() shouldBe emptyList()
        }

    @Test
    fun `a saved config is read back`() =
        runTest {
            val store = store()
            val config = QaConfig(profile = "sandbox").let { it.copy(order = it.order.copy(amount = 4_200, features = listOf("VERIFY"))) }
            store.saveConfig(config)
            store.config.first() shouldBe config
        }

    @Test
    fun `saving a profile with an existing name replaces it`() =
        runTest {
            val store = store()
            store.saveProfile(sandbox)
            store.saveProfile(sandbox.copy(gatewayUrl = "https://dev.bpcbt.com/second", api = ApiVersion.V2))
            store.profiles.first() shouldBe listOf(sandbox.copy(gatewayUrl = "https://dev.bpcbt.com/second", api = ApiVersion.V2))
        }

    @Test
    fun `profiles come back sorted by name with their credentials`() =
        runTest {
            val store = store()
            store.saveProfile(sandbox.copy(name = "zeta"))
            store.saveProfile(sandbox.copy(name = "alpha"))
            store.profiles.first() shouldBe listOf(sandbox.copy(name = "alpha"), sandbox.copy(name = "zeta"))
        }

    @Test
    fun `a deleted profile is gone and the config still names it`() =
        runTest {
            val store = store()
            store.saveProfile(sandbox)
            store.saveConfig(QaConfig(profile = "sandbox"))
            store.deleteProfile("sandbox")
            store.profiles.first() shouldBe emptyList()
            store.config.first().profile shouldBe "sandbox"
        }

    @Test
    fun `deleting from a list that cannot be opened leaves it stored`() =
        runTest {
            val dataStore = dataStore()
            dataStore.edit { it[profilesKey] = "garbage" }
            qaStore(dataStore, PrefixBox(opens = false)).deleteProfile("sandbox")
            dataStore.data.first()[profilesKey] shouldBe "garbage"
        }

    @Test
    fun `deleting a name that is not stored leaves the list as it was`() =
        runTest {
            val dataStore = dataStore()
            // Spaced unlike what the store writes, so that a rewrite would show in the raw value.
            val stored = PrefixBox().seal("[ ${StandProfileJson.encode(sandbox)} ]")
            dataStore.edit { it[profilesKey] = stored }
            qaStore(dataStore, PrefixBox()).deleteProfile("other")
            dataStore.data.first()[profilesKey] shouldBe stored
        }

    @Test
    fun `a change to the config does not open the profiles again`() =
        runTest {
            var opened = 0
            val box =
                object : SecretBox by PrefixBox() {
                    override fun open(sealed: String): String? {
                        opened++
                        return PrefixBox().open(sealed)
                    }
                }
            val sealed = PrefixBox().seal("[${StandProfileJson.encode(sandbox)}]")
            val data = MutableStateFlow(preferencesOf(profilesKey to sealed))
            val dataStore =
                object : DataStore<Preferences> {
                    override val data: Flow<Preferences> = data

                    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                        transform(data.value).also { data.value = it }
                }
            val seen = mutableListOf<List<StandProfile>>()
            // Not in backgroundScope: advanceUntilIdle does not run background work, and the read runs as a child of this job.
            val collecting = launch { qaStore(dataStore, box).profiles.collect { seen += it } }
            advanceUntilIdle()
            data.value = preferencesOf(profilesKey to sealed, configKey to QaConfigJson.encode(QaConfig(profile = "sandbox")))
            advanceUntilIdle()
            collecting.cancel()
            seen shouldBe listOf(listOf(sandbox))
            opened shouldBe 1
        }

    @Test
    fun `profiles are stored sealed`() =
        runTest {
            val dataStore = dataStore()
            qaStore(dataStore, PrefixBox()).saveProfile(sandbox)
            val raw = dataStore.data.first()[profilesKey].orEmpty()
            raw shouldNotContain "password-1"
            raw shouldNotContain "key-1"
            raw.startsWith("sealed:") shouldBe true
        }

    @Test
    fun `a config written by another build reads as the default`() =
        runTest {
            val dataStore = dataStore()
            dataStore.edit { it[configKey] = "{\"version\":2,\"profile\":\"sandbox\"}" }
            qaStore(dataStore, PrefixBox()).config.first() shouldBe QaConfig()
        }

    @Test
    fun `a config that is not a document reads as the default`() =
        runTest {
            val dataStore = dataStore()
            dataStore.edit { it[configKey] = "<<<" }
            qaStore(dataStore, PrefixBox()).config.first() shouldBe QaConfig()
        }

    @Test
    fun `profiles that cannot be opened read as none`() =
        runTest {
            val dataStore = dataStore()
            qaStore(dataStore, PrefixBox()).saveProfile(sandbox)
            qaStore(dataStore, PrefixBox(opens = false)).profiles.first() shouldBe emptyList()
        }

    @Test
    fun `stored text nested too deep reads as the default and no profiles`() =
        runTest {
            val dataStore = dataStore()
            val deep = "[".repeat(100_000) + "]".repeat(100_000)
            dataStore.edit {
                it[configKey] = "{\"a\":$deep}"
                it[profilesKey] = PrefixBox().seal(deep)
            }
            val store = qaStore(dataStore, PrefixBox())
            store.config.first() shouldBe QaConfig()
            store.profiles.first() shouldBe emptyList()
        }

    @Test
    fun `sealed text that is not a list of profiles reads as none`() =
        runTest {
            val dataStore = dataStore()
            dataStore.edit { it[profilesKey] = PrefixBox().seal("{\"name\":\"sandbox\"}") }
            qaStore(dataStore, PrefixBox()).profiles.first() shouldBe emptyList()
        }

    @Test
    fun `an invalid profile among valid ones is dropped and the others are kept`() =
        runTest {
            val dataStore = dataStore()
            val broken = """{"name":"Not A Name","gatewayUrl":"https://dev.bpcbt.com/payment"}"""
            dataStore.edit { it[profilesKey] = PrefixBox().seal("[$broken,7,${StandProfileJson.encode(sandbox)}]") }
            qaStore(dataStore, PrefixBox()).profiles.first() shouldBe listOf(sandbox)
        }

    @Test
    fun `saving a profile over unreadable ones starts the list again`() =
        runTest {
            val dataStore = dataStore()
            dataStore.edit { it[profilesKey] = "garbage" }
            val store = qaStore(dataStore, PrefixBox(opens = false))
            store.saveProfile(sandbox)
            qaStore(dataStore, PrefixBox()).profiles.first() shouldBe listOf(sandbox)
        }

    @Test
    fun `a store that cannot be read yields the defaults`() =
        runTest {
            val broken =
                object : DataStore<Preferences> {
                    override val data: Flow<Preferences> = flow { throw IOException("disk") }

                    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                        throw IOException("disk")
                }
            val store = qaStore(broken, PrefixBox())
            store.config.first() shouldBe QaConfig()
            store.profiles.first() shouldBe emptyList()
        }

    @Test
    fun `the stored config is the contract's JSON text`() =
        runTest {
            val dataStore = dataStore()
            val config = QaConfig(profile = "sandbox")
            qaStore(dataStore, PrefixBox()).saveConfig(config)
            dataStore.data.first()[configKey] shouldBe QaConfigJson.encode(config)
        }
}
