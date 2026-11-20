package net.payrdr.mobile.payment.sdk.qa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import net.payrdr.mobile.payment.sdk.qa.ui.QaNavGraph

/** The app's screens. External input arrives through [MainActivity]; this one only shows the app's one session. */
internal class QaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as QaApplication
        setContent { QaNavGraph(app.session, app.scanner) }
    }
}
