package net.payrdr.mobile.payment.sdk.payment.presentation

/**
 * Marks what the SDK's own SwiftUI layer reads from Kotlin: the checkout's state, its steps and its texts. Swift sees
 * only public declarations, so these are public — but they are not API: any of them may change or go in any release.
 * Do not use them from an app.
 */
@RequiresOptIn(
    message = "Exists for the Payrdr SDK's own SwiftUI layer; not API, may change in any release.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class PayrdrUiApi
