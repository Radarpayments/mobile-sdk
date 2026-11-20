package net.payrdr.mobile.payment.sdk.qa.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

internal fun Context.copyText(
    label: String,
    text: String,
) {
    getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(label, text))
    say("$label copied")
}

internal fun Context.clipboardText(): String =
    getSystemService(ClipboardManager::class.java)
        .primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(this)
        ?.toString()
        .orEmpty()

internal fun Context.say(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
