package net.payrdr.mobile.payment.flutter

import android.content.Intent
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import net.payrdr.mobile.payment.sdk.bridge.PayrdrBridge
import net.payrdr.mobile.payment.sdk.bridge.PayrdrBridgeCheckout

/**
 * Passes the Dart side's JSON to the SDK's bridge and its answers back; all mapping lives in sdk-bridge. The sheet opens
 * over the current activity with startActivityForResult, so the app's MainActivity stays a plain FlutterActivity.
 */
class PayrdrSdkPlugin :
    FlutterPlugin,
    MethodChannel.MethodCallHandler,
    ActivityAware,
    PluginRegistry.ActivityResultListener {
    private lateinit var channel: MethodChannel
    private var activity: ActivityPluginBinding? = null
    private val checkout = PayrdrBridgeCheckout()

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "net.payrdr.sdk")
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(
        call: MethodCall,
        result: MethodChannel.Result,
    ) {
        val request = call.arguments as? String ?: return result.error("invalid_request", "Expected a JSON string", null)
        when (call.method) {
            "present" -> checkout.start(activity?.activity, request) { result.success(it) }
            "coreToken" -> result.success(PayrdrBridge.coreToken(request))
            else -> result.notImplemented()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ): Boolean = checkout.handleActivityResult(requestCode, resultCode, data)

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        activity?.removeActivityResultListener(this)
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) = onAttachedToActivity(binding)

    override fun onDetachedFromActivityForConfigChanges() = onDetachedFromActivity()
}
