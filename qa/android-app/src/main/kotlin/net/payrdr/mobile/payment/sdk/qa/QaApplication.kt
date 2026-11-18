package net.payrdr.mobile.payment.sdk.qa

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import net.payrdr.mobile.payment.sdk.payment.scanner.provider.MlKitVisionCardScannerProvider
import net.payrdr.mobile.payment.sdk.qa.journal.Journal
import net.payrdr.mobile.payment.sdk.qa.merchant.Merchant
import net.payrdr.mobile.payment.sdk.qa.merchant.merchantHttpClient
import net.payrdr.mobile.payment.sdk.qa.storage.KeystoreSecretBox
import net.payrdr.mobile.payment.sdk.qa.storage.QaStore

/**
 * Owns the app's single store, journal, merchant and scanner provider. One instance of each per process: DataStore
 * refuses a second instance on the same file, and the checkout is keyed on configs built with the same journal and
 * scanner.
 */
internal class QaApplication : Application() {
    val store: QaStore by lazy {
        QaStore(PreferenceDataStoreFactory.create { preferencesDataStoreFile("qa") }, KeystoreSecretBox())
    }

    val journal: Journal by lazy { Journal() }

    val merchant: Merchant by lazy { Merchant(merchantHttpClient(), journal) }

    val scanner: MlKitVisionCardScannerProvider by lazy { MlKitVisionCardScannerProvider(journal) }
}
