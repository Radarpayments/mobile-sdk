package net.payrdr.mobile.payment.sdk.payment.support

import net.payrdr.mobile.payment.sdk.core.model.Key

internal object Fixtures {
    /** 2048-bit RSA public key — test-only. Copied from sdk-core RsaCryptogramCipherTest. */
    const val TEST_PUBLIC_KEY_PEM: String = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvjG7qd50WLuBmw7rUpQo
xN44zA/XUddLxYAN22fJE9GbLMwKcT0fVE//KjAUDDy3s8p4QuNuxLfUsINM4DlV
IbQZx3ZOXUaQILYCqmqqvbibEk4ulITdMS6QjEsHXdcKXpr+1ZyVG90RI7oxyQf8
YKTxqOQF7lYqicEb6ucPJwYxLA1IoPu9K5PAysLrFiEwuq+z6dNFrvVMXSDAefry
Hc/MnjXhGLc3qcfH9sJczKQ8we6I7zIDgRcIjsZ8xI8ME+X36UJpYPvrU41Xssi1
6MCObHbZpZTEjShPnfmwTIDMbjnqwitXKaC0C+vJ+DvnL5+iqsHN+HT+z8NcQXco
xQIDAQAB
-----END PUBLIC KEY-----"""

    val TEST_KEY: Key = Key(value = TEST_PUBLIC_KEY_PEM, protocol = "RSA", expiration = Long.MAX_VALUE)

    const val TEST_MD_ORDER: String = "7f472085-399e-414e-b51c-a7b538aee497"
    const val TEST_PAN: String = "4532896701439077"
    const val TEST_EXPIRY: String = "12/28"
    const val TEST_CVC: String = "444"
    const val TEST_CARDHOLDER: String = "JOHN SMITH"
    const val TEST_BINDING_ID: String = "47eb0336-5ad9-4e03-8a1e-b9f3656ec768"
}
