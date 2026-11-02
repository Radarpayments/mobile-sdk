package net.payrdr.mobile.payment.reactnative

import android.app.Activity
import android.content.Intent
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.UiThreadUtil
import net.payrdr.mobile.payment.sdk.bridge.PayrdrBridge
import net.payrdr.mobile.payment.sdk.bridge.PayrdrBridgeCheckout

/**
 * Passes the JavaScript side's JSON to the SDK's bridge and its answers back; all mapping lives in sdk-bridge. The sheet
 * opens over the current activity with startActivityForResult, so the app's MainActivity needs no changes.
 */
class PayrdrSdkModule(
  reactContext: ReactApplicationContext,
) : NativePayrdrSdkSpec(reactContext),
  ActivityEventListener {
  private val checkout = PayrdrBridgeCheckout()

  init {
    reactContext.addActivityEventListener(this)
  }

  override fun present(
    requestJson: String,
    promise: Promise,
  ) {
    UiThreadUtil.runOnUiThread { checkout.start(reactApplicationContext.currentActivity, requestJson) { promise.resolve(it) } }
  }

  override fun coreToken(
    requestJson: String,
    promise: Promise,
  ) {
    promise.resolve(PayrdrBridge.coreToken(requestJson))
  }

  override fun onActivityResult(
    activity: Activity,
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
  ) {
    checkout.handleActivityResult(requestCode, resultCode, data)
  }

  override fun onNewIntent(intent: Intent) = Unit

  override fun invalidate() {
    reactApplicationContext.removeActivityEventListener(this)
    super.invalidate()
  }

  companion object {
    const val NAME = NativePayrdrSdkSpec.NAME
  }
}
