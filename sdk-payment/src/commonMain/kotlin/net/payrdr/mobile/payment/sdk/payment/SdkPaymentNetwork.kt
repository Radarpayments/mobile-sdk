package net.payrdr.mobile.payment.sdk.payment

import net.payrdr.mobile.payment.sdk.payment.api.PaymentApi
import net.payrdr.mobile.payment.sdk.payment.keyprovider.KeyProvider

/**
 * Replaces the network layer the SDK builds from its config: for tests and special cases (a custom HTTP stack, a
 * recorded gateway). Most apps never need it — `SdkPayment.create(config)` is the whole setup.
 *
 * @property api the gateway calls the checkout makes
 * @property keyProvider source of the RSA public key the card data is encrypted with
 */
public class SdkPaymentNetwork(
    public val api: PaymentApi,
    public val keyProvider: KeyProvider,
)
