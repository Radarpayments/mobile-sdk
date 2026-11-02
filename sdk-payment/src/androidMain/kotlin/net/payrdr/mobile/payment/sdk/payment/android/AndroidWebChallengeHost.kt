package net.payrdr.mobile.payment.sdk.payment.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import net.payrdr.mobile.payment.sdk.payment.threeds.ThreeDsChallenge
import net.payrdr.mobile.payment.sdk.payment.threeds.ThreeDsResult
import net.payrdr.mobile.payment.sdk.payment.threeds.WebChallengeHost
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

/**
 * Android implementation of [WebChallengeHost]. Launches an internal
 * [ThreeDsWebActivity] that displays a `WebView`, POSTs the ACS form, and intercepts the redirect.
 *
 * The 5-min timeout is enforced by `withTimeout(challenge.timeoutMs)`. When the timeout fires, the
 * Activity is finished and [ThreeDsResult.TimedOut] is returned. If the user hits Back, the
 * Activity finishes with `RESULT_CANCELED` → [ThreeDsResult.Cancelled].
 *
 * Instantiate once per `ComponentActivity` that hosts the checkout (e.g. inside `SdkPaymentActivity.onCreate`).
 *
 * @param activity the hosting activity — used to register the `ActivityResultLauncher` at start-up
 */
public class AndroidWebChallengeHost(
    private val activity: ComponentActivity,
) : WebChallengeHost {
    private var pending: ((ThreeDsResult) -> Unit)? = null

    // ComponentActivity is the host — the Fragment-version check does not apply here.
    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val launcher =
        activity.registerForActivityResult(ThreeDsWebContract) { result ->
            pending?.invoke(result)
            pending = null
        }

    override suspend fun performChallenge(challenge: ThreeDsChallenge): ThreeDsResult =
        try {
            withTimeout(challenge.timeoutMs) {
                suspendCancellableCoroutine<ThreeDsResult> { cont ->
                    pending = { result ->
                        if (cont.isActive) cont.resume(result)
                    }
                    cont.invokeOnCancellation { pending = null }
                    // The checkout has already opened its sheet to the whole screen: the bank page just appears over it.
                    launcher.launch(challenge, ActivityOptionsCompat.makeCustomAnimation(activity, 0, 0))
                }
            }
        } catch (_: TimeoutCancellationException) {
            pending = null
            // The page must not stay open: the buyer could still confirm on it after the checkout moved on.
            ThreeDsWebActivity.shown?.get()?.finish()
            ThreeDsResult.TimedOut
        }
}

internal object ThreeDsWebContract : ActivityResultContract<ThreeDsChallenge, ThreeDsResult>() {
    internal const val EXTRA_CHALLENGE = "challenge"
    internal const val EXTRA_RESULT = "result"
    internal const val RESULT_KEY_SUCCESS = "success"
    internal const val RESULT_KEY_REASON = "reason"
    internal const val RESULT_KEY_INTERRUPTED = "interrupted"
    internal const val RESULT_KEY_QUERY = "query"

    override fun createIntent(
        context: Context,
        input: ThreeDsChallenge,
    ): Intent =
        Intent(context, ThreeDsWebActivity::class.java).apply {
            putExtra("acsUrl", input.acsUrl)
            putExtra("paReq", input.paReq)
            putExtra("md", input.md)
            putExtra("termUrl", input.termUrl)
            putExtra("mdOrder", input.mdOrder)
            putExtra("returnUrlPrefix", input.returnUrlPrefix)
            putExtra("sessionId", input.sessionId)
        }

    @Suppress("ReturnCount")
    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): ThreeDsResult {
        if (resultCode != Activity.RESULT_OK || intent == null) return ThreeDsResult.Cancelled
        val success = intent.getBooleanExtra(RESULT_KEY_SUCCESS, false)
        if (!success) {
            val reason = intent.getStringExtra(RESULT_KEY_REASON) ?: "unknown"
            return if (intent.getBooleanExtra(
                    RESULT_KEY_INTERRUPTED,
                    false,
                )
            ) {
                ThreeDsResult.Interrupted(reason)
            } else {
                ThreeDsResult.Failed(reason)
            }
        }
        val queryEntries: Map<String, String> =
            intent
                .getStringArrayExtra(RESULT_KEY_QUERY)
                ?.toList()
                ?.chunked(2) { pair -> pair[0] to (pair.getOrNull(1) ?: "") }
                ?.toMap()
                ?: emptyMap()
        return ThreeDsResult.Succeeded(queryEntries)
    }
}

/**
 * Internal Activity that hosts the `WebView` and POSTs the ACS form. Not exported; launched only
 * via [ThreeDsWebContract].
 */
internal class ThreeDsWebActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Recreated after Android ended the process: posting the same one-time request to the bank again only hangs.
        // Closing as interrupted lets the checkout ask the gateway how the payment ended.
        if (savedInstanceState != null) return finishWithFailure("bank page restored", interrupted = true)
        shown = WeakReference(this)
        // Appears and goes without a transition: the checkout's full-screen sheet under it is the motion.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        }

        val acsUrl = requireNotNull(intent.getStringExtra("acsUrl")) { "acsUrl missing" }
        val paReq = requireNotNull(intent.getStringExtra("paReq")) { "paReq missing" }
        val md = requireNotNull(intent.getStringExtra("md")) { "md missing" }
        val termUrl = requireNotNull(intent.getStringExtra("termUrl")) { "termUrl missing" }
        val returnUrlPrefix = intent.getStringExtra("returnUrlPrefix") ?: "sdk://done"
        // Only decides which pages end the bank page (the return prefix, or the session's own success/failure pages).
        val challenge =
            ThreeDsChallenge(
                mdOrder = intent.getStringExtra("mdOrder").orEmpty(),
                acsUrl = acsUrl,
                paReq = paReq,
                md = md,
                termUrl = termUrl,
                returnUrlPrefix = returnUrlPrefix,
                sessionId = intent.getStringExtra("sessionId"),
            )

        val webView =
            WebView(this).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                // The bank's one-time code is not a password: without this Android offers to save it to the password
                // manager the moment the page takes it.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                }
                webViewClient =
                    object : WebViewClient() {
                        override fun onPageStarted(
                            view: WebView?,
                            url: String?,
                            favicon: Bitmap?,
                        ) {
                            if (url != null && challenge.isReturnUrl(url)) {
                                handleRedirect(url)
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            val url = request.url.toString()
                            return if (challenge.isReturnUrl(url)) {
                                handleRedirect(url)
                                true
                            } else {
                                false
                            }
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError,
                        ) {
                            if (request.isForMainFrame) {
                                // The page did not load (no network, DNS, TLS): not the bank's answer.
                                finishWithFailure("web-resource-error: ${error.errorCode}", interrupted = true)
                            }
                        }
                    }
            }
        // Bank pages are white: light, edge-to-edge bars over the page instead of the host app's
        // bars; the page itself keeps clear of the bars and of the keyboard (OTP input).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        webView.setBackgroundColor(Color.WHITE)
        // The insets go on a container: a WebView draws the page from its own top edge whatever its padding, so padded
        // itself the page would still slide under the status bar.
        val page =
            FrameLayout(this).apply {
                setBackgroundColor(Color.WHITE)
                addView(webView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            }
        ViewCompat.setOnApplyWindowInsetsListener(page) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        setContentView(page)

        val formBody =
            buildString {
                append("MD=")
                append(Uri.encode(md))
                append("&PaReq=")
                append(Uri.encode(paReq))
                append("&TermUrl=")
                append(Uri.encode(termUrl))
            }.toByteArray()
        webView.postUrl(acsUrl, formBody)
    }

    private fun handleRedirect(url: String) {
        val query =
            Uri.parse(url).queryParameterNames.flatMap { key ->
                val values = Uri.parse(url).getQueryParameters(key)
                values.map { key to it }
            }
        // Flatten to Array<String> = [k, v, k, v, ...] for parcel transport.
        val flat = query.flatMap { listOf(it.first, it.second) }.toTypedArray()
        // Detect error parameter from ACS convention.
        val hasError = query.any { it.first.equals("error", ignoreCase = true) }
        val intent =
            Intent().apply {
                putExtra(ThreeDsWebContract.RESULT_KEY_SUCCESS, !hasError)
                if (hasError) {
                    putExtra(
                        ThreeDsWebContract.RESULT_KEY_REASON,
                        query.firstOrNull { it.first.equals("error", true) }?.second ?: "unknown",
                    )
                }
                putExtra(ThreeDsWebContract.RESULT_KEY_QUERY, flat)
            }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onDestroy() {
        if (shown?.get() === this) shown = null
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun finishWithFailure(
        reason: String,
        interrupted: Boolean = false,
    ) {
        val intent =
            Intent().apply {
                putExtra(ThreeDsWebContract.RESULT_KEY_SUCCESS, false)
                putExtra(ThreeDsWebContract.RESULT_KEY_REASON, reason)
                putExtra(ThreeDsWebContract.RESULT_KEY_INTERRUPTED, interrupted)
            }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    internal companion object {
        /** The bank page on screen, so the host can close it when the challenge times out. */
        var shown: WeakReference<ThreeDsWebActivity>? = null
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }
}
