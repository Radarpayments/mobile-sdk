package net.payrdr.mobile.payment.samples.android.core

import net.payrdr.mobile.payment.sdk.core.model.ParamField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SdkCoreViewModelTest {
    @Test fun `newCard variant params class matches CardParams`() {
        val vm = SdkCoreViewModel()
        vm.setVariant(SdkCoreVariant.NEW_CARD)
        vm.setPan("5391119268214792")
        vm.setExpiry("12/28")
        vm.setCvc("123")
        vm.setHolder("Roy Band")
        vm.setMdOrder("c400b41a-aa3d-43db-8727-ac4ca9e8f701")
        vm.setPubKeyPem("-----BEGIN PUBLIC KEY-----\ntest\n-----END PUBLIC KEY-----")
        val params = vm.buildParams()
        assertTrue(params::class.simpleName == "CardParams")
    }

    @Test fun `instant variant produces CardInstantParams`() {
        val vm = SdkCoreViewModel()
        vm.setVariant(SdkCoreVariant.INSTANT)
        vm.setPan("5391119268214792")
        vm.setExpiry("12/28")
        vm.setCvc("123")
        vm.setHolder("")
        vm.setPubKeyPem("-----BEGIN PUBLIC KEY-----\ntest\n-----END PUBLIC KEY-----")
        val params = vm.buildParams()
        assertTrue(params::class.simpleName == "CardInstantParams")
    }

    @Test fun `stored variant produces BindingParams`() {
        val vm = SdkCoreViewModel()
        vm.setVariant(SdkCoreVariant.STORED)
        vm.setBindingId("513b17f4-e32e-414f-8c74-936fd7027baa")
        vm.setCvc("")
        vm.setMdOrder("c400b41a-aa3d-43db-8727-ac4ca9e8f701")
        vm.setPubKeyPem("-----BEGIN PUBLIC KEY-----\ntest\n-----END PUBLIC KEY-----")
        val params = vm.buildParams()
        assertTrue(params::class.simpleName == "BindingParams")
    }

    @Test fun `expiry input gets its slash after the month`() {
        assertEquals("1", formatExpiryInput("1"))
        assertEquals("12", formatExpiryInput("12"))
        assertEquals("12/3", formatExpiryInput("123"))
        assertEquals("12/30", formatExpiryInput("12/305"))
    }

    @Test fun `sdk-core errors land on the form field they belong to`() {
        assertEquals(CoreField.PAN, ParamField.PAN.toCoreField())
        assertEquals(CoreField.MD_ORDER, ParamField.MD_ORDER.toCoreField())
        assertEquals(CoreField.PUB_KEY, ParamField.PUB_KEY.toCoreField())
        assertNull(ParamField.UNKNOWN.toCoreField())
    }

    @Test fun `expiry typed as digits reaches sdk-core as MM slash YY`() {
        val vm = SdkCoreViewModel()
        vm.setVariant(SdkCoreVariant.NEW_CARD)
        vm.setExpiry("1230")
        val params = vm.buildParams() as net.payrdr.mobile.payment.sdk.core.model.CardParams
        assertEquals("12/30", params.expiryMMYY)
    }

    @Test fun `the number shows in its network's groups and the cursor keeps to the digits`() {
        val visa =
            PanGroups.filter(
                androidx.compose.ui.text
                    .AnnotatedString("4111111111111111"),
            )
        assertEquals("4111 1111 1111 1111", visa.text.text)
        assertEquals(19, visa.offsetMapping.originalToTransformed(16))
        assertEquals(5, visa.offsetMapping.originalToTransformed(4))
        assertEquals(4, visa.offsetMapping.transformedToOriginal(5))
        assertEquals(
            "3782 822463 10005",
            PanGroups
                .filter(
                    androidx.compose.ui.text
                        .AnnotatedString("378282246310005"),
                ).text.text,
        )
        assertEquals(
            "",
            PanGroups
                .filter(
                    androidx.compose.ui.text
                        .AnnotatedString(""),
                ).text.text,
        )
    }
}
