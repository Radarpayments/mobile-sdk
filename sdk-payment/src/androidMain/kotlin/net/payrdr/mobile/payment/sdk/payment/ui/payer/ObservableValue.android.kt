package net.payrdr.mobile.payment.sdk.payment.ui.payer

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf

private class SnapshotValue<T>(
    initial: T,
) : ObservableValue<T> {
    private val state = mutableStateOf(initial)

    override var value: T
        get() = state.value
        set(value) {
            state.value = value
        }
}

internal actual fun <T> observableValueOf(value: T): ObservableValue<T> = SnapshotValue(value)

internal actual fun <K, V> observableMapOf(): MutableMap<K, V> = mutableStateMapOf()
