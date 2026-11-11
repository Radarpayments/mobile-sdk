package net.payrdr.mobile.payment.sdk.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.payrdr.mobile.payment.sdk.core.crypto.CryptogramCipher
import net.payrdr.mobile.payment.sdk.core.crypto.RsaCryptogramCipher
import net.payrdr.mobile.payment.sdk.core.model.BindingInstantParams
import net.payrdr.mobile.payment.sdk.core.model.BindingParams
import net.payrdr.mobile.payment.sdk.core.model.CardBindingIdIdentifier
import net.payrdr.mobile.payment.sdk.core.model.CardInfo
import net.payrdr.mobile.payment.sdk.core.model.CardInstantParams
import net.payrdr.mobile.payment.sdk.core.model.CardPanIdentifier
import net.payrdr.mobile.payment.sdk.core.model.CardParams
import net.payrdr.mobile.payment.sdk.core.model.Key
import net.payrdr.mobile.payment.sdk.core.model.MsdkRegisteredFrom
import net.payrdr.mobile.payment.sdk.core.model.NewPaymentMethodCardParams
import net.payrdr.mobile.payment.sdk.core.model.NewPaymentMethodStoredCardParams
import net.payrdr.mobile.payment.sdk.core.model.ParamField
import net.payrdr.mobile.payment.sdk.core.model.PaymentCardParams
import net.payrdr.mobile.payment.sdk.core.model.SdkCoreConfig
import net.payrdr.mobile.payment.sdk.core.processor.DefaultPaymentStringProcessor
import net.payrdr.mobile.payment.sdk.core.processor.PaymentStringProcessor
import net.payrdr.mobile.payment.sdk.core.utils.BindingUtils
import net.payrdr.mobile.payment.sdk.core.utils.toExpDate
import net.payrdr.mobile.payment.sdk.core.validation.BaseValidator
import net.payrdr.mobile.payment.sdk.core.validation.CardBindingIdValidator
import net.payrdr.mobile.payment.sdk.core.validation.CardCodeValidator
import net.payrdr.mobile.payment.sdk.core.validation.CardExpiryNotPastValidator
import net.payrdr.mobile.payment.sdk.core.validation.CardExpiryValidator
import net.payrdr.mobile.payment.sdk.core.validation.CardHolderValidator
import net.payrdr.mobile.payment.sdk.core.validation.CardNumberValidator
import net.payrdr.mobile.payment.sdk.core.validation.OrderNumberValidator
import net.payrdr.mobile.payment.sdk.core.validation.PubKeyValidator
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Headless token-generation entry point.
 *
 * Stateless, thread-safe; all inputs come through [generate] calls. No platform-specific
 * dependencies in the public contract — merchants do not pass an Android `Context`.
 */
@OptIn(ExperimentalUuidApi::class)
public class SdkCore internal constructor(
    private val paymentStringProcessor: PaymentStringProcessor,
    private val cryptogramCipher: CryptogramCipher,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    public constructor() : this(DefaultPaymentStringProcessor(TimeZone.currentSystemDefault()), RsaCryptogramCipher())

    private val orderValidator = OrderNumberValidator()
    private val expiryValidator = CardExpiryValidator()
    private val numberValidator = CardNumberValidator()
    private val bindingIdValidator = CardBindingIdValidator()
    private val codeValidator = CardCodeValidator()
    private val holderValidator = CardHolderValidator()
    private val pubKeyValidator = PubKeyValidator()

    /**
     * Generates a payment token for the given [config].
     *
     * Validates all fields before encryption; returns errors on the first invalid field. A new card whose expiry
     * month is before the month of [SdkCoreConfig.timestamp] (device time zone) is reported as [ParamField.EXPIRY]
     * with `invalid`.
     * On success, returns a non-null [TokenResult.token] with empty [TokenResult.errors].
     */
    public fun generate(config: SdkCoreConfig): TokenResult {
        val params = config.cardParams
        val checks =
            when (params) {
                is PaymentCardParams.StoredCardPaymentParams -> checksForStored(params)
                is PaymentCardParams.NewCardPaymentParams -> checksForNew(params, paymentDate(config.timestamp))
            }

        for (check in checks) {
            val value = check.value ?: continue
            val r = check.validator.validate(value)
            if (!r.isValid) return TokenResult.withErrors(mapOf(check.field to (r.errorCode ?: "invalid")))
        }

        val cardInfo =
            when (params) {
                is PaymentCardParams.StoredCardPaymentParams -> cardInfoForStored(params)
                is PaymentCardParams.NewCardPaymentParams -> cardInfoForNew(params)
            }

        val mdOrder =
            when (params) {
                is CardParams -> params.mdOrder
                is BindingParams -> params.mdOrder
                is NewPaymentMethodCardParams, is CardInstantParams, is BindingInstantParams, is NewPaymentMethodStoredCardParams -> ""
            }

        return buildToken(mdOrder, cardInfo, params.pubKey, config.timestamp, config.registeredFrom)
    }

    /** The SDK version, as published: `4.0.0`, `4.0.0-beta.1`. */
    public fun version(): String = VERSION

    private fun cardInfoForNew(p: PaymentCardParams.NewCardPaymentParams) =
        CardInfo(
            identifier = CardPanIdentifier(p.pan),
            expDate = p.expiryMMYY.toExpDate(),
            cardHolder = p.cardHolder,
            cvv = p.cvc,
        )

    private fun cardInfoForStored(p: PaymentCardParams.StoredCardPaymentParams): CardInfo {
        val bindingId =
            when (p) {
                is BindingParams -> p.bindingID
                is BindingInstantParams -> p.bindingID
                is NewPaymentMethodStoredCardParams -> BindingUtils.extractBindingId(p.storedPaymentId)
            }
        return CardInfo(identifier = CardBindingIdIdentifier(bindingId), cvv = p.cvc, cardHolder = null)
    }

    private fun paymentDate(timestamp: Long): LocalDate = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone).date

    /** One field to validate; null [value]s (optional fields left out) are skipped. */
    private class Check(
        val value: String?,
        val validator: BaseValidator<String>,
        val field: ParamField,
    )

    /**
     * Checks in the order a card form shows its fields. A list, not a map keyed by value: fields that
     * share a value (several empty ones) must each be checked by their own validator.
     */
    private fun checksForNew(
        p: PaymentCardParams.NewCardPaymentParams,
        today: LocalDate,
    ): List<Check> =
        buildList {
            add(Check(p.pan, numberValidator, ParamField.PAN))
            add(Check(p.expiryMMYY, expiryValidator, ParamField.EXPIRY))
            // The loop stops at the first failure, so this one sees only a well-formed MM/YY.
            add(Check(p.expiryMMYY, CardExpiryNotPastValidator(today), ParamField.EXPIRY))
            add(Check(p.cvc, codeValidator, ParamField.CVC))
            add(Check(p.cardHolder, holderValidator, ParamField.CARDHOLDER))
            if (p is CardParams) add(Check(p.mdOrder, orderValidator, ParamField.MD_ORDER))
            add(Check(p.pubKey, pubKeyValidator, ParamField.PUB_KEY))
        }

    private fun checksForStored(p: PaymentCardParams.StoredCardPaymentParams): List<Check> =
        buildList {
            when (p) {
                is BindingParams -> add(Check(p.bindingID, bindingIdValidator, ParamField.BINDING_ID))
                is BindingInstantParams -> add(Check(p.bindingID, bindingIdValidator, ParamField.BINDING_ID))
                is NewPaymentMethodStoredCardParams ->
                    add(Check(BindingUtils.extractBindingId(p.storedPaymentId), bindingIdValidator, ParamField.STORED_PAYMENT_ID))
            }
            add(Check(p.cvc, codeValidator, ParamField.CVC))
            if (p is BindingParams) add(Check(p.mdOrder, orderValidator, ParamField.MD_ORDER))
            add(Check(p.pubKey, pubKeyValidator, ParamField.PUB_KEY))
        }

    @Suppress("TooGenericExceptionCaught")
    private fun buildToken(
        mdOrder: String,
        cardInfo: CardInfo,
        pubKey: String,
        timestamp: Long,
        registeredFrom: MsdkRegisteredFrom,
    ): TokenResult {
        val paymentString =
            paymentStringProcessor.createPaymentString(
                order = mdOrder,
                timestamp = timestamp,
                uuid = Uuid.random().toString(),
                cardInfo = cardInfo,
                registeredFrom = registeredFrom,
            )
        val key = Key(value = pubKey, protocol = "RSA", expiration = Long.MAX_VALUE)
        return try {
            TokenResult.withToken(cryptogramCipher.encode(paymentString, key))
        } catch (e: IllegalArgumentException) {
            TokenResult.withErrors(mapOf(ParamField.PUB_KEY to "invalid"))
        } catch (e: Exception) {
            TokenResult.withErrors(mapOf(ParamField.UNKNOWN to "unknown"))
        }
    }

    private companion object {
        const val VERSION = SDK_VERSION
    }
}
