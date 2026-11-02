package net.payrdr.mobile.payment.samples.android.store.invest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TopUpAmountTest {
    private fun typed(text: String) = text.fold(TopUpAmount("")) { amount, c -> amount.type(c) }

    @Test fun `default is 100 euros and valid`() {
        assertEquals(10000L, TopUpAmount().minor)
        assertTrue(TopUpAmount().valid)
    }

    @Test fun `zero is not valid`() {
        assertEquals("0", typed("0").text)
        assertFalse(typed("0").valid)
    }

    @Test fun `a separator first reads as zero point`() {
        assertEquals("0.", typed(".").text)
        assertFalse(typed(".").valid)
    }

    @Test fun `a trailing separator still counts`() {
        assertEquals(1000L, typed("10.").minor)
        assertTrue(typed("10.").valid)
    }

    @Test fun `a third decimal is ignored`() {
        assertEquals("12.34", typed("12.345").text)
        assertEquals("12.3", typed("12.3.").text)
    }

    @Test fun `limits are 10 to 1000 euros`() {
        assertFalse(typed("9.99").valid)
        assertTrue(typed("10").valid)
        assertTrue(typed("1000").valid)
        assertFalse(typed("1000.01").valid)
    }

    @Test fun `no leading zeros`() {
        assertEquals("0", typed("00").text)
        assertEquals("5", typed("05").text)
    }

    @Test fun `integer part is capped at six digits`() {
        assertEquals("123456", typed("1234567").text)
    }

    @Test fun `backspace to empty`() {
        val empty = typed("5").backspace()
        assertEquals("", empty.text)
        assertNull(empty.minor)
        assertFalse(empty.valid)
        assertEquals("", empty.backspace().text)
    }

    @Test fun `preset sets whole euros`() {
        assertEquals("250", TopUpAmount.preset(25000).text)
        assertEquals("12.5", TopUpAmount.preset(1250).text)
    }

    @Test fun `unconfirmed blocks continue`() {
        assertFalse(TopUpStatus.Unconfirmed.canContinue(TopUpAmount()))
        assertTrue(TopUpStatus.Declined(null).canContinue(TopUpAmount()))
        assertTrue(TopUpStatus.Editing.canContinue(TopUpAmount()))
        assertFalse(TopUpStatus.Editing.canContinue(TopUpAmount("5")))
        assertFalse(TopUpStatus.Registering.canContinue(TopUpAmount()))
    }

    @Test fun `text from the system keyboard is cleaned the same way`() {
        assertEquals("12.34", TopUpAmount.parse("12,345").text)
        assertEquals("12.34", TopUpAmount.parse("12.3.4").text)
        assertEquals("5", TopUpAmount.parse("0005").text)
        assertEquals("0.5", TopUpAmount.parse(",5").text)
        assertEquals("", TopUpAmount.parse("-").text)
        assertEquals(99900L, TopUpAmount.parse("999").minor)
    }

    @Test fun `parse reads the Arabic decimal separator`() {
        assertEquals("12.5", TopUpAmount.parse("12\u066B5").text)
    }

    @Test fun `parse reads Arabic-Indic and Persian digits as ASCII`() {
        assertEquals("12.5", TopUpAmount.parse("\u0661\u0662\u066B\u0665").text)
        assertEquals("30", TopUpAmount.parse("\u06F3\u06F0").text)
    }
}
