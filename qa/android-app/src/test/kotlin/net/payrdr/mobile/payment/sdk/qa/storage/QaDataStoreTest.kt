package net.payrdr.mobile.payment.sdk.qa.storage

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QaDataStoreTest {
    @Test
    fun `a corrupted store file is replaced by an empty one`() =
        runTest {
            val file = File(createTempDirectory("qa-store").toFile(), "qa.preferences_pb")
            file.writeBytes(byteArrayOf(0x0A, 0x7F, 0x01, 0x02))
            val box =
                object : SecretBox {
                    override fun seal(plain: String) = plain

                    override fun open(sealed: String) = sealed
                }
            val store = QaStore(qaDataStore(backgroundScope) { file }, box, StandardTestDispatcher(testScheduler))

            store.saveConfig(QaConfig(profile = "sandbox"))

            store.config.first() shouldBe QaConfig(profile = "sandbox")
        }
}
