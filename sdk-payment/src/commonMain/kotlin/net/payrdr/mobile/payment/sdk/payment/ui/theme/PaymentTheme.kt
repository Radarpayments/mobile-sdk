package net.payrdr.mobile.payment.sdk.payment.ui.theme

/**
 * Selects the theme used by the checkout UI.
 */
public sealed interface PaymentTheme {
    /** Follow the system light/dark setting. Default. */
    public data object System : PaymentTheme

    /** Force the bundled light palette. */
    public data object Light : PaymentTheme

    /** Force the bundled dark palette. */
    public data object Dark : PaymentTheme

    /** Merchant-provided fully custom theme. Any of the three knobs may use defaults by passing the companion constants. */
    public data class Custom(
        val colors: PaymentColors,
        val typography: PaymentTypography = PaymentTypography.Default,
        val shapes: PaymentShapes = PaymentShapes.Default,
    ) : PaymentTheme
}
