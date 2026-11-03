package net.payrdr.mobile.payment.samples.android.merchant

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

/** The HTTP client of the "your server" calls, which this sample makes to the gateway itself. */
internal fun merchantHttpClient(): HttpClient = HttpClient(OkHttp)
