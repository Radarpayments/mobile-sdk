package net.payrdr.mobile.payment.sdk.payment.support

import net.payrdr.mobile.payment.sdk.core.model.Key
import net.payrdr.mobile.payment.sdk.payment.keyprovider.KeyProvider

internal class FakeKeyProvider(
    private val key: Key = Fixtures.TEST_KEY,
) : KeyProvider {
    override suspend fun provideKey(): Key = key
}
