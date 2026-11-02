package net.payrdr.mobile.payment.sdk.payment.ios

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import net.payrdr.mobile.payment.sdk.payment.threeds.AcsForm
import net.payrdr.mobile.payment.sdk.payment.threeds.ThreeDsChallenge
import net.payrdr.mobile.payment.sdk.payment.threeds.ThreeDsResult
import net.payrdr.mobile.payment.sdk.payment.threeds.WebChallengeHost
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.addValue
import platform.Foundation.dataUsingEncoding
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIBarButtonSystemItem
import platform.UIKit.UIColor
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.UINavigationController
import platform.UIKit.UIScrollViewContentInsetAdjustmentBehavior
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController
import platform.UIKit.bottomAnchor
import platform.UIKit.leadingAnchor
import platform.UIKit.navigationItem
import platform.UIKit.safeAreaLayoutGuide
import platform.UIKit.topAnchor
import platform.UIKit.trailingAnchor
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import kotlin.coroutines.resume

/**
 * iOS implementation of [WebChallengeHost]. Presents a `WKWebView` inside a modal
 * `UINavigationController` with a Cancel bar button.
 *
 * The 5-min timeout is enforced by `withTimeout(challenge.timeoutMs)`. When the timeout fires, the
 * presented VC is dismissed and [ThreeDsResult.TimedOut] is returned.
 *
 * The form body is built by [AcsForm]: Foundation's query character set leaves `+` unescaped, which the bank reads
 * as a space in base64 PaReq.
 *
 * @param presentingViewControllerProvider lazily resolves the presenting VC at challenge time; lets
 *        the host survive owner-VC recreation.
 */
@OptIn(ExperimentalForeignApi::class)
public class IosWebChallengeHost(
    private val presentingViewControllerProvider: () -> UIViewController,
) : WebChallengeHost {
    override suspend fun performChallenge(challenge: ThreeDsChallenge): ThreeDsResult =
        try {
            withTimeout(challenge.timeoutMs) {
                suspendCancellableCoroutine<ThreeDsResult> { cont ->
                    val presenter = presentingViewControllerProvider()
                    val vc =
                        ThreeDsWebViewController(
                            challenge = challenge,
                            onComplete = { outcome ->
                                presenter.dismissViewControllerAnimated(false, completion = null)
                                if (cont.isActive) cont.resume(outcome)
                            },
                        )
                    val nav = UINavigationController(rootViewController = vc)
                    nav.modalPresentationStyle = UIModalPresentationFullScreen
                    // Bank pages are white: keep the bar light too, whatever the system theme is.
                    nav.overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleLight
                    // The checkout has already opened its sheet to the whole screen: the bank page just appears over it,
                    // and goes the same way.
                    presenter.presentViewController(nav, animated = false, completion = null)
                    cont.invokeOnCancellation {
                        presenter.dismissViewControllerAnimated(false, completion = null)
                    }
                }
            }
        } catch (_: TimeoutCancellationException) {
            ThreeDsResult.TimedOut
        }
}

/**
 * Hosts the WKWebView + navigation bar. Builds the POST body, performs the request, and routes
 * redirects back via [onComplete]. Cancelled when the user taps the Cancel button.
 */
@OptIn(ExperimentalForeignApi::class)
private class ThreeDsWebViewController(
    private val challenge: ThreeDsChallenge,
    private val onComplete: (ThreeDsResult) -> Unit,
) : UIViewController(nibName = null, bundle = null),
    WKNavigationDelegateProtocol {
    // Renamed from `webView` to avoid clash with the WKNavigationDelegateProtocol override method.
    private lateinit var wkWebView: WKWebView

    override fun viewDidLoad() {
        super.viewDidLoad()
        // Without a colour the screen is black until the bank page draws its first frame.
        view.backgroundColor = UIColor.whiteColor
        val config = WKWebViewConfiguration()
        wkWebView = WKWebView(frame = view.bounds, configuration = config)
        wkWebView.backgroundColor = UIColor.whiteColor
        wkWebView.navigationDelegate = this
        view.addSubview(wkWebView)
        // The page starts below the bars and stays there when scrolled: the navigation bar is see-through (iOS 26), so a
        // full-screen page would scroll under the status bar and the close button.
        wkWebView.scrollView.contentInsetAdjustmentBehavior =
            UIScrollViewContentInsetAdjustmentBehavior.UIScrollViewContentInsetAdjustmentNever
        wkWebView.translatesAutoresizingMaskIntoConstraints = false
        val safeArea = view.safeAreaLayoutGuide
        NSLayoutConstraint.activateConstraints(
            listOf(
                wkWebView.topAnchor.constraintEqualToAnchor(safeArea.topAnchor),
                wkWebView.bottomAnchor.constraintEqualToAnchor(safeArea.bottomAnchor),
                wkWebView.leadingAnchor.constraintEqualToAnchor(safeArea.leadingAnchor),
                wkWebView.trailingAnchor.constraintEqualToAnchor(safeArea.trailingAnchor),
            ),
        )

        // `navigationItem` is an ObjC extension property imported explicitly from platform.UIKit.
        navigationItem.leftBarButtonItem =
            UIBarButtonItem(
                // UIBarButtonSystemItemCancel is a CEnum entry, not a top-level constant.
                barButtonSystemItem = UIBarButtonSystemItem.UIBarButtonSystemItemCancel,
                target = this,
                action = NSSelectorFromString("cancelTapped"),
            )

        submitForm()
    }

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    @Suppress("unused")
    fun cancelTapped() {
        onComplete(ThreeDsResult.Cancelled)
    }

    private fun submitForm() {
        val url = NSURL(string = challenge.acsUrl)
        val request = NSMutableURLRequest(uRL = url)
        // setHTTPMethod / setHTTPBody are ObjC category methods imported as K/N extension fns.
        request.setHTTPMethod("POST")
        val body = AcsForm.body(md = challenge.md, paReq = challenge.paReq, termUrl = challenge.termUrl)
        request.setHTTPBody((body as NSString).dataUsingEncoding(NSUTF8StringEncoding))
        // addValue avoids Kotlin MutableMap.setValue delegate ambiguity with setValue.
        request.addValue("application/x-www-form-urlencoded", forHTTPHeaderField = "Content-Type")
        wkWebView.loadRequest(request)
    }

    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
        val url = decidePolicyForNavigationAction.request.URL?.absoluteString
        if (url != null && challenge.isReturnUrl(url)) {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            handleRedirect(url)
        } else {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
        }
    }

    private fun handleRedirect(urlString: String) {
        val components =
            NSURLComponents(
                uRL = NSURL(string = urlString),
                resolvingAgainstBaseURL = false,
            )
        val query: List<NSURLQueryItem> =
            @Suppress("UNCHECKED_CAST")
            (components?.queryItems as? List<NSURLQueryItem> ?: emptyList())
        val map = mutableMapOf<String, String>()
        for (item in query) {
            val name = item.name
            val itemValue = item.value ?: ""
            map[name] = itemValue
        }
        val hasError = map.keys.any { it.equals("error", ignoreCase = true) }
        if (hasError) {
            onComplete(ThreeDsResult.Failed(map["error"] ?: "unknown"))
        } else {
            onComplete(ThreeDsResult.Succeeded(map))
        }
    }
}
