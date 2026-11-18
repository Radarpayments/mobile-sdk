package net.payrdr.mobile.payment.sdk.qa

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import net.payrdr.mobile.payment.sdk.qa.intake.intakeFromExtras
import net.payrdr.mobile.payment.sdk.qa.intake.intakeFromLink

/**
 * The entry point of external input: launch extras and links. It hands the input to the app's session and brings the
 * app's task to the front as it is, so a sheet that is open stays open and its run takes the input.
 *
 * The input cannot be delivered to the configuration screen itself. While the sheet is above it, Android either starts
 * a second screen, with a second run, or, when the intent equals the one that started the task, only brings the task
 * to the front and drops the extras. This activity lives in a task of its own that ends with it, so it is always
 * created and always sees its intent.
 */
internal class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = (application as QaApplication).session
        val extras =
            intakeFromExtras(
                qaConfig = intent.getStringExtra(EXTRA_CONFIG),
                qaProfile = intent.getStringExtra(EXTRA_PROFILE),
                qaLaunch = intent.getBooleanExtra(EXTRA_LAUNCH, false),
            )
        val link = intent.dataString?.let(::intakeFromLink).orEmpty()
        if (extras.isNotEmpty()) session.accept(extras)
        if (link.isNotEmpty()) session.accept(link)
        // The launcher's own intent: the task started from the launcher matches it and is brought to the front instead
        // of getting a second screen.
        packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        finish()
    }

    private companion object {
        const val EXTRA_CONFIG = "qaConfig"
        const val EXTRA_PROFILE = "qaProfile"
        const val EXTRA_LAUNCH = "qaLaunch"
    }
}
