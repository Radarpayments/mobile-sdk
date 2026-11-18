package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.payrdr.mobile.payment.sdk.qa.config.ApiVersion
import net.payrdr.mobile.payment.sdk.qa.config.Problem
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.config.StandProfileJson

/**
 * The profile being edited. Held in memory only, never in the saved instance state, which the system may write to disk:
 * the password and the API key live nowhere but in the Keystore-sealed store.
 */
internal class ProfileEditor {
    var name by mutableStateOf("")
    var gatewayUrl by mutableStateOf("")
    var api by mutableStateOf(ApiVersion.V1)
    var login by mutableStateOf("")
    var password by mutableStateOf("")
    var apiKey by mutableStateOf("")

    var problem: Problem? by mutableStateOf(null)
        private set

    fun load(profile: StandProfile?) {
        name = profile?.name.orEmpty()
        gatewayUrl = profile?.gatewayUrl.orEmpty()
        api = profile?.api ?: ApiVersion.V1
        login = profile?.login.orEmpty()
        password = profile?.password.orEmpty()
        apiKey = profile?.apiKey.orEmpty()
        problem = null
    }

    /** The edited profile when it is valid; otherwise null, and [problem] says why. */
    fun valid(): StandProfile? {
        val profile = StandProfile(name.trim(), gatewayUrl.trim(), api, login, password, apiKey)
        problem = StandProfileJson.problemOf(profile)
        return profile.takeIf { problem == null }
    }
}
