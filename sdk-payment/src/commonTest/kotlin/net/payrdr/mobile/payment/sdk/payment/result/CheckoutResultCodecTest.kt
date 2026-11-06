package net.payrdr.mobile.payment.sdk.payment.result

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.payrdr.mobile.payment.sdk.payment.exception.SdkApplePayException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkCryptogramException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkDeclinedException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkGooglePayException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkInvalidSessionIdException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkNetworkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkNotConfiguredException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkOrderNotFoundException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkPaymentApiException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkSessionExpiredException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkSessionNotFoundException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkThreeDsChallengeFailedException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkThreeDsTimeoutException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkTransactionException
import kotlin.test.Test

class CheckoutResultCodecTest {
    private fun roundTrip(result: CheckoutResult): CheckoutResult = CheckoutResultCodec.decode(CheckoutResultCodec.encode(result))

    private fun failedWith(error: SdkException): SdkException =
        roundTrip(CheckoutResult.Failed("md-1", error))
            .shouldBeInstanceOf<CheckoutResult.Failed>()
            .also { it.orderId shouldBe "md-1" }
            .error

    @Test fun `completed and canceled keep their order id`() {
        roundTrip(CheckoutResult.Completed("md-1")) shouldBe CheckoutResult.Completed("md-1")
        roundTrip(CheckoutResult.Canceled("md-1")) shouldBe CheckoutResult.Canceled("md-1")
    }

    @Test fun `unknown keeps its order id and the type of its cause`() {
        val unknown =
            roundTrip(CheckoutResult.Unknown("md-1", SdkNetworkException("offline")))
                .shouldBeInstanceOf<CheckoutResult.Unknown>()
        unknown.orderId shouldBe "md-1"
        unknown.cause.shouldBeInstanceOf<SdkNetworkException>().message shouldBe "offline"
        roundTrip(CheckoutResult.Unknown("md-1")).shouldBeInstanceOf<CheckoutResult.Unknown>().cause shouldBe null
    }

    @Test fun `a decline keeps its type and code and message`() {
        val declined = SdkDeclinedException(declineCode = "DATA_INPUT_ERROR", message = "Declined")
        val error = failedWith(declined).shouldBeInstanceOf<SdkDeclinedException>()
        error.declineCode shouldBe "DATA_INPUT_ERROR"
        error.message shouldBe "Declined"
    }

    @Test fun `an API error keeps its http status and code`() {
        val api = SdkPaymentApiException(httpStatus = 502, errorCode = "5", message = "Bad gateway")
        val error = failedWith(api).shouldBeInstanceOf<SdkPaymentApiException>()
        error.httpStatus shouldBe 502
        error.errorCode shouldBe "5"
        error.message shouldBe "Bad gateway"
    }

    @Test fun `a 3DS failure keeps its reason`() {
        failedWith(SdkThreeDsChallengeFailedException("ACS error"))
            .shouldBeInstanceOf<SdkThreeDsChallengeFailedException>()
            .reason shouldBe "ACS error"
    }

    @Test fun `an expired session keeps its order`() {
        failedWith(SdkSessionExpiredException("md-1"))
            .shouldBeInstanceOf<SdkSessionExpiredException>()
            .mdOrder shouldBe "md-1"
    }

    @Test fun `every SDK error crosses the activity result as itself with its message`() {
        listOf(
            SdkNetworkException("offline"),
            SdkSessionNotFoundException("ps_1"),
            SdkOrderNotFoundException("md-1"),
            SdkInvalidSessionIdException("ps_?"),
            SdkCryptogramException("no cryptogram"),
            SdkNotConfiguredException("no gateway"),
            SdkTransactionException("failed"),
            SdkThreeDsTimeoutException(),
            SdkGooglePayException("unavailable"),
            SdkApplePayException("unavailable"),
        ).forEach { error ->
            val decoded = failedWith(error)
            decoded::class shouldBe error::class
            decoded.message shouldBe error.message
        }
    }

    @Test fun `errors that name a session or an order keep it`() {
        failedWith(SdkSessionNotFoundException("ps_1")).shouldBeInstanceOf<SdkSessionNotFoundException>().sessionId shouldBe "ps_1"
        failedWith(SdkOrderNotFoundException("md-1")).shouldBeInstanceOf<SdkOrderNotFoundException>().mdOrder shouldBe "md-1"
        failedWith(SdkInvalidSessionIdException("ps_?")).shouldBeInstanceOf<SdkInvalidSessionIdException>().sessionId shouldBe "ps_?"
    }

    @Test fun `an unrecognised exception arrives as a plain SdkException with its message`() {
        val error = failedWith(object : SdkException("custom") {})
        error::class shouldBe SdkException::class
        error.message shouldBe "custom"
    }

    @Test fun `closed without a result keeps the order id when it is known`() {
        CheckoutResultCodec.closedWithoutResult(orderId = "md-1") shouldBe CheckoutResult.Unknown("md-1")
    }

    @Test fun `closed without a result is unknown and not a cancel`() {
        CheckoutResultCodec.closedWithoutResult().shouldBeInstanceOf<CheckoutResult.Unknown>()
    }
}
