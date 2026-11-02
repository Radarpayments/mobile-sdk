package net.payrdr.mobile.payment.sdk.payment.scanner.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.startup.Initializer
import java.lang.ref.WeakReference

public object ContextHolder {
    @Volatile internal var appContext: Context? = null

    public fun current(): Context =
        appContext ?: error(
            "sdk-payment-scanner: application context not initialised. Ensure androidx.startup is enabled " +
                "for your app, or call ContextHolder.init(context) manually from Application.onCreate.",
        )

    public fun init(context: Context) {
        val app = context.applicationContext
        if (appContext === app) return
        appContext = app
        (app as? Application)?.registerActivityLifecycleCallbacks(ActivityTracker)
    }

    /** The activity on screen: the camera permission dialog is asked for from it. */
    internal val resumedActivity = LatestResumed<ComponentActivity>()

    private object ActivityTracker : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            if (activity is ComponentActivity) resumedActivity.resumed(activity)
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (activity is ComponentActivity) resumedActivity.destroyed(activity)
        }

        override fun onActivityCreated(
            activity: Activity,
            savedInstanceState: Bundle?,
        ) = Unit

        override fun onActivityStarted(activity: Activity) = Unit

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) = Unit

        override fun onActivitySaveInstanceState(
            activity: Activity,
            outState: Bundle,
        ) = Unit
    }
}

/** The [T] that came on screen last, until it is destroyed; held weakly, so it never keeps a finished screen alive. */
internal class LatestResumed<T : Any> {
    @Volatile private var latest: WeakReference<T>? = null

    fun resumed(item: T) {
        latest = WeakReference(item)
    }

    fun destroyed(item: T) {
        if (latest?.get() === item) latest = null
    }

    fun current(): T? = latest?.get()
}

public class ScannerInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        ContextHolder.init(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

/** The activity on screen, to ask for the camera from. The application context the scanner holds is never one. */
internal fun requireScannerActivity(): ComponentActivity =
    ContextHolder.resumedActivity.current()
        ?: error("sdk-payment-scanner: no ComponentActivity on screen; the scanner runs in an app built on androidx.activity")
