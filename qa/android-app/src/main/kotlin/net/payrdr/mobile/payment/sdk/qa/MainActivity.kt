package net.payrdr.mobile.payment.sdk.qa

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import net.payrdr.mobile.payment.sdk.qa.intake.intakeFromExtras
import net.payrdr.mobile.payment.sdk.qa.intake.intakeFromLink
import net.payrdr.mobile.payment.sdk.qa.ui.QaNavGraph
import net.payrdr.mobile.payment.sdk.qa.ui.QaSession

internal class MainActivity : ComponentActivity() {
    private val session: QaSession by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A recreated activity got its intent before; applying it again would undo what QA changed since.
        if (savedInstanceState == null) take(intent)
        setContent { QaNavGraph(session) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        take(intent)
    }

    private fun take(intent: Intent) {
        val extras =
            intakeFromExtras(
                qaConfig = intent.getStringExtra(EXTRA_CONFIG),
                qaProfile = intent.getStringExtra(EXTRA_PROFILE),
                qaLaunch = intent.getBooleanExtra(EXTRA_LAUNCH, false),
            )
        val link = intent.dataString?.let(::intakeFromLink).orEmpty()
        if (extras.isNotEmpty()) session.accept(extras)
        if (link.isNotEmpty()) session.accept(link)
    }

    private companion object {
        const val EXTRA_CONFIG = "qaConfig"
        const val EXTRA_PROFILE = "qaProfile"
        const val EXTRA_LAUNCH = "qaLaunch"
    }
}
