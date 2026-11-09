package net.payrdr.mobile.payment.samples.android.ui

private const val GREEK_STRESSED = "ΆΈΉΊΌΎΏ"
private const val GREEK_PLAIN = "ΑΕΗΙΟΥΩ"

/**
 * The text in capitals for a heading. Greek capitals carry no stress mark ("ΠΥΛΗ", not "ΠΎΛΗ"), but
 * [uppercase] keeps it.
 */
internal fun String.toCaps(): String {
    val caps = StringBuilder(uppercase())
    caps.indices.forEach { i ->
        val stressed = GREEK_STRESSED.indexOf(caps[i])
        if (stressed >= 0) caps[i] = GREEK_PLAIN[stressed]
    }
    return caps.toString()
}
