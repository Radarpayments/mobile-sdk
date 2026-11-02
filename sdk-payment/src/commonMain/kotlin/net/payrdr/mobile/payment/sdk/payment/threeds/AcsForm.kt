package net.payrdr.mobile.payment.sdk.payment.threeds

/**
 * The form posted to the bank's ACS page (`application/x-www-form-urlencoded`). Every value is escaped except RFC 3986's
 * unreserved characters: PaReq is base64, and a `+` left as is reads as a space on the bank's side ("bad pareq").
 */
internal object AcsForm {
    fun body(
        md: String,
        paReq: String,
        termUrl: String,
    ): String = "MD=${encode(md)}&PaReq=${encode(paReq)}&TermUrl=${encode(termUrl)}"

    fun encode(value: String): String =
        buildString {
            for (byte in value.encodeToByteArray()) {
                val char = byte.toInt().toChar()
                if (byte >= 0 && (char.isLetterOrDigit() || char in UNRESERVED_MARKS)) {
                    append(char)
                } else {
                    append('%').append(HEX[(byte.toInt() shr 4) and 0xF]).append(HEX[byte.toInt() and 0xF])
                }
            }
        }

    private const val UNRESERVED_MARKS = "-._~"
    private const val HEX = "0123456789ABCDEF"
}
