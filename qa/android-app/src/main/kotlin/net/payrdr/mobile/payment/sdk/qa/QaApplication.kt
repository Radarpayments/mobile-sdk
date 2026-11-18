package net.payrdr.mobile.payment.sdk.qa

import android.app.Application
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.MainScope
import net.payrdr.mobile.payment.sdk.payment.scanner.provider.MlKitVisionCardScannerProvider
import net.payrdr.mobile.payment.sdk.qa.journal.Journal
import net.payrdr.mobile.payment.sdk.qa.merchant.Merchant
import net.payrdr.mobile.payment.sdk.qa.merchant.merchantHttpClient
import net.payrdr.mobile.payment.sdk.qa.storage.KeystoreSecretBox
import net.payrdr.mobile.payment.sdk.qa.storage.QaStore
import net.payrdr.mobile.payment.sdk.qa.storage.qaDataStore
import net.payrdr.mobile.payment.sdk.qa.ui.QaSession

/**
 * Owns the app's single store, journal, scanner provider and session. One instance of each per process: DataStore
 * refuses a second instance on the same file, the checkout is keyed on configs built with the same journal and
 * scanner, and every screen and every external input work on the one run.
 */
internal class QaApplication : Application() {
    val journal: Journal by lazy { Journal() }

    val scanner: MlKitVisionCardScannerProvider by lazy { MlKitVisionCardScannerProvider(journal) }

    // The run presents the sheet and takes its result on the main thread.
    val session: QaSession by lazy {
        val store = QaStore(qaDataStore { preferencesDataStoreFile("qa") }, KeystoreSecretBox())
        QaSession(store, journal, Merchant(merchantHttpClient(), journal), MainScope())
    }
}
