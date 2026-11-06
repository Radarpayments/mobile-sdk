package net.payrdr.mobile.payment.sdk.bridge

/** Codes of a checkout that did not open. */
internal object NotOpened {
    /** Another checkout of the same plugin is on screen. */
    const val BUSY = "busy"

    /** The app has no screen to put the checkout over (no activity, no key window). */
    const val NO_SCREEN = "no_screen"
}
