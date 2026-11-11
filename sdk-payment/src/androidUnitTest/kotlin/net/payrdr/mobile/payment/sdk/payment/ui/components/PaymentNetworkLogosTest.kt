package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.addPathNodes
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PaymentNetworkLogosTest {
    private val logos =
        mapOf(
            "Visa" to PaymentNetworkLogos.Visa,
            "Mastercard" to PaymentNetworkLogos.Mastercard,
            "Amex" to PaymentNetworkLogos.Amex,
            "UnionPay" to PaymentNetworkLogos.UnionPay,
        )

    @Test
    fun `every generated path parses`() {
        logos.forEach { (_, logo) ->
            logo.layers.shouldNotBeEmpty()
            logo.layers.forEach { addPathNodes(it.pathData).shouldNotBeEmpty() }
        }
    }

    @Test
    fun `bounds are positive and only UnionPay has a knockout layer`() {
        logos.forEach { (name, logo) ->
            (logo.width > 0f && logo.height > 0f) shouldBe true
            logo.layers.any { it.knockout } shouldBe (name == "UnionPay")
        }
    }

    @Test
    fun `Visa arc keeps its flags and end point`() {
        // Simple Icons packs arc flags without separators ("0 01.894"), which Compose parsed into a broken arc
        // and a crooked logo; the data is stored normalised, so the arc must come out exactly as drawn.
        val arc = addPathNodes(PaymentNetworkLogos.Visa.layers[0].pathData).filterIsInstance<PathNode.RelativeArcTo>().first()
        arc.isMoreThanHalf shouldBe false
        arc.isPositiveArc shouldBe true
        arc.arcStartDx shouldBe 0.894f
        arc.arcStartDy shouldBe 0.764f
    }
}
