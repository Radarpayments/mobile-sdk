package net.payrdr.mobile.payment.sdk.payment.ui.payer

private class PlainValue<T>(
    override var value: T,
) : ObservableValue<T>

internal actual fun <T> observableValueOf(value: T): ObservableValue<T> = PlainValue(value)

internal actual fun <K, V> observableMapOf(): MutableMap<K, V> = mutableMapOf()
