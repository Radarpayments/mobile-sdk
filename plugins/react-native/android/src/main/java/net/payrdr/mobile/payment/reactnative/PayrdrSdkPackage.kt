package net.payrdr.mobile.payment.reactnative

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

class PayrdrSdkPackage : BaseReactPackage() {
  override fun getModule(
    name: String,
    reactContext: ReactApplicationContext,
  ): NativeModule? = if (name == PayrdrSdkModule.NAME) PayrdrSdkModule(reactContext) else null

  override fun getReactModuleInfoProvider() =
    ReactModuleInfoProvider {
      mapOf(
        PayrdrSdkModule.NAME to
          ReactModuleInfo(
            name = PayrdrSdkModule.NAME,
            className = PayrdrSdkModule.NAME,
            canOverrideExistingModule = false,
            needsEagerInit = false,
            isCxxModule = false,
            isTurboModule = true,
          ),
      )
    }
}
