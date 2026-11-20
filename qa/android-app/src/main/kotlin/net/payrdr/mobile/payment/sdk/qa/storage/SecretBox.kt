package net.payrdr.mobile.payment.sdk.qa.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Seals a text so that it can be kept next to the app's data and opened only by this app on this device. */
internal interface SecretBox {
    /** Throws when the text cannot be sealed (the Keystore failed); there is no fallback to storing it as it is. */
    fun seal(plain: String): String

    /** Returns the text [seal] was given, or null when [sealed] cannot be opened (another key, damaged, key gone). */
    fun open(sealed: String): String?
}

/**
 * AES-256/GCM with a key that never leaves the AndroidKeyStore; the output is base64 of the IV followed by the ciphertext.
 * `androidx.security:security-crypto` is deprecated, hence the platform API.
 */
internal class KeystoreSecretBox : SecretBox {
    override fun seal(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, existingOrNewKey())
        return Base64.encodeToString(cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    // The key is not created here: after a restore to another device it is gone, and a fresh one would only fail to open
    // what the old one sealed. The caller then starts without profiles.
    // RuntimeException covers bad base64 and ProviderException, and also the IllegalStateException or NullPointerException
    // that AndroidKeyStore on some OEM builds throws from the keystore service: any of them would crash the screen that
    // reads the profiles at startup.
    @Suppress("TooGenericExceptionCaught")
    override fun open(sealed: String): String? =
        try {
            val bytes = Base64.decode(sealed, Base64.NO_WRAP)
            val key = existingKey()
            if (key == null || bytes.size <= IV_BYTES) {
                null
            } else {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
                String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
            }
        } catch (_: GeneralSecurityException) {
            null
        } catch (_: IOException) {
            null
        } catch (_: RuntimeException) {
            null
        }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    private fun existingKey(): SecretKey? = keyStore().getKey(KEY_ALIAS, null) as? SecretKey

    // Two seals racing with no key would each generate one under the alias, and the first ciphertext could not be opened.
    @Synchronized
    private fun existingOrNewKey(): SecretKey = existingKey() ?: createKey()

    private fun createKey(): SecretKey {
        val spec =
            KeyGenParameterSpec
                .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build()
        return KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
            .apply { init(spec) }
            .generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "qa_profiles"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val TAG_BITS = 128
        const val IV_BYTES = 12
    }
}
