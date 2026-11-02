package net.payrdr.mobile.payment.sdk.core.processor

import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import net.payrdr.mobile.payment.sdk.core.model.CardBindingIdIdentifier
import net.payrdr.mobile.payment.sdk.core.model.CardInfo
import net.payrdr.mobile.payment.sdk.core.model.CardPanIdentifier
import net.payrdr.mobile.payment.sdk.core.model.ExpiryDate
import net.payrdr.mobile.payment.sdk.core.model.MsdkRegisteredFrom
import kotlin.test.Test

class DefaultPaymentStringProcessorTest {
    private val processor = DefaultPaymentStringProcessor(zone = TimeZone.of("Europe/Nicosia"))

    @Test
    fun `new card full template`() {
        val s =
            processor.createPaymentString(
                order = "7f472085-399e-414e-b51c-a7b538aee497",
                timestamp = 1594009580806L,
                uuid = "fd4b1011-727a-41e8-95b4-d7092d729022",
                cardInfo =
                    CardInfo(
                        identifier = CardPanIdentifier("4532896701439077"),
                        cvv = "444",
                        expDate = ExpiryDate(2020, 12),
                        cardHolder = "CardHolderName",
                    ),
                registeredFrom = MsdkRegisteredFrom.MSDK_CORE,
            )
        s shouldBe
            "2020-07-06T07:26:20+03:00/fd4b1011-727a-41e8-95b4-d7092d729022/4532896701439077/444/202012/7f472085-399e-414e-b51c-a7b538aee497//CardHolderName/MSDK_CORE"
    }

    @Test
    fun `expiry month is zero-padded - the P2P gateway reads yyyyMM by position`() {
        val s =
            processor.createPaymentString(
                order = "7f472085-399e-414e-b51c-a7b538aee497",
                timestamp = 1594009580806L,
                uuid = "fd4b1011-727a-41e8-95b4-d7092d729022",
                cardInfo =
                    CardInfo(
                        identifier = CardPanIdentifier("4532896701439077"),
                        cvv = "444",
                        expDate = ExpiryDate(2028, 2),
                        cardHolder = "CardHolderName",
                    ),
                registeredFrom = MsdkRegisteredFrom.MSDK_CORE,
            )
        s.split("/")[4] shouldBe "202802"
    }

    @Test
    fun `new card without cvv`() {
        val s =
            processor.createPaymentString(
                order = "7f472085-399e-414e-b51c-a7b538aee497",
                timestamp = 1594009580806L,
                uuid = "fd4b1011-727a-41e8-95b4-d7092d729022",
                cardInfo =
                    CardInfo(
                        identifier = CardPanIdentifier("4532896701439077"),
                        expDate = ExpiryDate(2020, 12),
                        cardHolder = "CardHolderName",
                    ),
                registeredFrom = MsdkRegisteredFrom.MSDK_CORE,
            )
        s shouldBe
            "2020-07-06T07:26:20+03:00/fd4b1011-727a-41e8-95b4-d7092d729022/4532896701439077//202012/7f472085-399e-414e-b51c-a7b538aee497//CardHolderName/MSDK_CORE"
    }

    @Test
    fun `saved card full template`() {
        val s =
            processor.createPaymentString(
                order = "7f472085-399e-414e-b51c-a7b538aee497",
                timestamp = 1594009580806L,
                uuid = "fd4b1011-727a-41e8-95b4-d7092d729022",
                cardInfo =
                    CardInfo(
                        identifier = CardBindingIdIdentifier("47eb0336-5ad9-4e03-8a1e-b9f3656ec768"),
                        cvv = "444",
                        expDate = ExpiryDate(2020, 12),
                    ),
                registeredFrom = MsdkRegisteredFrom.MSDK_CORE,
            )
        s shouldBe
            "2020-07-06T07:26:20+03:00/fd4b1011-727a-41e8-95b4-d7092d729022//444/202012/7f472085-399e-414e-b51c-a7b538aee497/47eb0336-5ad9-4e03-8a1e-b9f3656ec768//MSDK_CORE"
    }

    @Test
    fun `new card without mdOrder`() {
        val s =
            processor.createPaymentString(
                order = "",
                timestamp = 1594009580806L,
                uuid = "fd4b1011-727a-41e8-95b4-d7092d729022",
                cardInfo =
                    CardInfo(
                        identifier = CardPanIdentifier("4532896701439077"),
                        cvv = "444",
                        expDate = ExpiryDate(2020, 12),
                        cardHolder = "CardHolderName",
                    ),
                registeredFrom = MsdkRegisteredFrom.MSDK_CORE,
            )
        s shouldBe "2020-07-06T07:26:20+03:00/fd4b1011-727a-41e8-95b4-d7092d729022/4532896701439077/444/202012///CardHolderName/MSDK_CORE"
    }

    @Test
    fun `saved card without exp date without cvv`() {
        val s =
            processor.createPaymentString(
                order = "7f472085-399e-414e-b51c-a7b538aee497",
                timestamp = 1594009580806L,
                uuid = "fd4b1011-727a-41e8-95b4-d7092d729022",
                cardInfo =
                    CardInfo(
                        identifier = CardBindingIdIdentifier("47eb0336-5ad9-4e03-8a1e-b9f3656ec768"),
                    ),
                registeredFrom = MsdkRegisteredFrom.MSDK_CORE,
            )
        s shouldBe
            "2020-07-06T07:26:20+03:00/fd4b1011-727a-41e8-95b4-d7092d729022////7f472085-399e-414e-b51c-a7b538aee497/47eb0336-5ad9-4e03-8a1e-b9f3656ec768//MSDK_CORE"
    }
}
