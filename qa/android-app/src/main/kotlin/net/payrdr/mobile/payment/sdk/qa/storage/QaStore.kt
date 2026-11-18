package net.payrdr.mobile.payment.sdk.qa.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import net.payrdr.mobile.payment.sdk.qa.config.Decoded
import net.payrdr.mobile.payment.sdk.qa.config.MAX_DOCUMENT_DEPTH
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.config.QaConfigJson
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.config.StandProfileJson
import net.payrdr.mobile.payment.sdk.qa.config.limitProblem
import java.io.IOException

/**
 * The current configuration (as its JSON text) and the stand profiles (one sealed JSON array, since they carry the
 * credentials). Nothing stored is trusted: what cannot be read — written by another build, sealed by a Keystore key that
 * is gone — reads as the default config and no profiles, so the app starts instead of crashing.
 */
internal class QaStore(
    private val dataStore: DataStore<Preferences>,
    private val box: SecretBox,
    // Opening the profiles is a Keystore call; a Compose collector would otherwise make it on the main thread.
    private val readDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    // An unreadable file ends the flow with the defaults instead of failing the screen that collects it.
    private val preferences: Flow<Preferences> =
        dataStore.data.catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }

    // Every write emits all preferences again; only a change of the stored text is read again.
    val config: Flow<QaConfig> =
        preferences
            .map { it[CONFIG] }
            .distinctUntilChanged()
            .map(::readConfig)
            .flowOn(readDispatcher)

    /** Sorted by name; the profiles of a stored list that are invalid are left out, the others are kept. */
    val profiles: Flow<List<StandProfile>> =
        preferences
            .map { it[PROFILES] }
            .distinctUntilChanged()
            .map(::readProfiles)
            .flowOn(readDispatcher)

    /** Stores [config] as it is: the caller validates it first, since one that is invalid reads back as the default. */
    suspend fun saveConfig(config: QaConfig) {
        dataStore.edit { it[CONFIG] = QaConfigJson.encode(config) }
    }

    /**
     * Replaces the profile of the same name; a stored list that cannot be read is started again with this profile. The
     * caller validates [profile] first: an invalid one is stored and then left out when the list is read.
     */
    suspend fun saveProfile(profile: StandProfile) {
        dataStore.edit { preferences ->
            writeProfiles(preferences, readProfiles(preferences[PROFILES]).filterNot { it.name == profile.name } + profile)
        }
    }

    /**
     * Leaves the config alone: one that names the deleted profile is reported by the launch check. A list that cannot be
     * read, or holds no profile of that name, is left as it is stored.
     */
    suspend fun deleteProfile(name: String) {
        dataStore.edit { preferences ->
            val stored = readProfiles(preferences[PROFILES])
            if (stored.any { it.name == name }) writeProfiles(preferences, stored.filterNot { it.name == name })
        }
    }

    private fun readConfig(stored: String?): QaConfig = ((stored?.let(QaConfigJson::decode)) as? Decoded.Ok)?.value ?: QaConfig()

    private fun readProfiles(stored: String?): List<StandProfile> {
        val plain = stored?.let(box::open) ?: return emptyList()
        // The stored array holds every profile: one level above a document, and as large as the profiles are many.
        if (limitProblem(plain, maxBytes = Int.MAX_VALUE, maxDepth = MAX_DOCUMENT_DEPTH + 1) != null) return emptyList()
        val array =
            try {
                Json.parseToJsonElement(plain) as? JsonArray
            } catch (_: SerializationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            } ?: return emptyList()
        return array
            .mapNotNull { element ->
                ((element as? JsonObject)?.let { StandProfileJson.decode(it.toString()) } as? Decoded.Ok)?.value
            }.associateBy { it.name }
            .values
            .sortedBy { it.name }
    }

    private fun writeProfiles(
        preferences: MutablePreferences,
        profiles: List<StandProfile>,
    ) {
        if (profiles.isEmpty()) {
            preferences.remove(PROFILES)
        } else {
            val array = JsonArray(profiles.map { Json.parseToJsonElement(StandProfileJson.encode(it)) })
            preferences[PROFILES] = box.seal(array.toString())
        }
    }

    private companion object {
        val CONFIG = stringPreferencesKey("config")
        val PROFILES = stringPreferencesKey("profiles")
    }
}
