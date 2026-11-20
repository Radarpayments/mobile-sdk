package net.payrdr.mobile.payment.sdk.qa.report

/** What the report says about the app and the device it ran on. */
internal data class AppInfo(
    val appVersion: String,
    val sdkVersion: String,
    val device: String,
    val os: String,
)
