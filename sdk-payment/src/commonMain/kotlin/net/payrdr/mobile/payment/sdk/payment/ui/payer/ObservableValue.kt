package net.payrdr.mobile.payment.sdk.payment.ui.payer

import kotlin.reflect.KProperty

/**
 * A field of [PayerFieldsState]. On Android it is Compose snapshot state, so the screens recompose when it changes;
 * on iOS a plain value, as the SwiftUI form reads the state again after each of its own edits.
 */
internal interface ObservableValue<T> {
    var value: T
}

internal operator fun <T> ObservableValue<T>.getValue(
    thisRef: Any?,
    property: KProperty<*>,
): T = value

internal operator fun <T> ObservableValue<T>.setValue(
    thisRef: Any?,
    property: KProperty<*>,
    value: T,
) {
    this.value = value
}

internal expect fun <T> observableValueOf(value: T): ObservableValue<T>

/** A map with the same platform behaviour as [ObservableValue]. */
internal expect fun <K, V> observableMapOf(): MutableMap<K, V>
