package net.payrdr.mobile.payment.sdk.payment.support

import kotlinx.coroutines.CompletableDeferred
import net.payrdr.mobile.payment.sdk.core.model.Key
import net.payrdr.mobile.payment.sdk.payment.keyprovider.KeyProvider

internal class FakeKeyProvider(
    private val key: Key = Fixtures.TEST_KEY,
) : KeyProvider {
    /** `provideKey` waits for this before answering, when set. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun provideKey(): Key {
        gate?.await()
        return key
    }
}
