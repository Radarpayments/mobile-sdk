package net.payrdr.mobile.payment.samples.android.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class CapsTest {
    @Test fun `greek capitals lose the stress mark`() {
        assertEquals("ΠΥΛΗ ΠΛΗΡΩΜΩΝ", "Πύλη πληρωμών".toCaps())
    }

    @Test fun `other accents stay`() {
        assertEquals("ÉTAPE ÎLE", "étape île".toCaps())
    }
}
