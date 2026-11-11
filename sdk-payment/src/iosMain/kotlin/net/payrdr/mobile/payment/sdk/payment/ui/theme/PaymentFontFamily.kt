package net.payrdr.mobile.payment.sdk.payment.ui.theme

/**
 * A font family the app has registered, by its family name: one listed under `UIAppFonts` or registered with Core Text.
 * A name iOS does not know draws in the system font.
 *
 * @property name the family name, as `UIFont.familyNames` lists it, e.g. `"Inter"`
 */
public actual class PaymentFontFamily(
    public val name: String,
) {
    override fun equals(other: Any?): Boolean = other is PaymentFontFamily && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "PaymentFontFamily($name)"
}
