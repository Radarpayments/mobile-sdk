package net.payrdr.mobile.payment.sdk.payment.ssl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test

@OptIn(ExperimentalEncodingApi::class)
class SslPinningConfigTest {
    @Test fun `builder aggregates pin entries from addPin`() {
        val config =
            SslPinningConfig
                .builder()
                .addPin(hostname = "api.payrdr.net", sha256Pin = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                .addPin(hostname = "api.payrdr.net", sha256Pin = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
                .build()

        config.entries shouldHaveSize 2
        config.entries shouldContain SslPinningConfig.PinEntry("api.payrdr.net", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
    }

    @Test fun `hostname is lowercased and trailing dot stripped`() {
        val config =
            SslPinningConfig
                .builder()
                .addPin(hostname = "API.PAYRDR.NET.", sha256Pin = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                .build()

        config.entries.first().hostname shouldBe "api.payrdr.net"
    }

    @Test fun `pins apply to a host as OkHttp's CertificatePinner applies them`() {
        val a = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        val b = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
        val c = "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC="
        val config =
            SslPinningConfig
                .builder()
                .addPin("api.payrdr.net", a)
                .addPin("*.payrdr.net", b)
                .addPin("**.bank.example", c)
                .build()

        config.pinsFor("api.payrdr.net") shouldBe setOf("sha256/$a", "sha256/$b")
        config.pinsFor("API.Payrdr.net.") shouldBe setOf("sha256/$a", "sha256/$b")
        // `*.` covers exactly one label.
        config.pinsFor("pay.payrdr.net") shouldBe setOf("sha256/$b")
        config.pinsFor("payrdr.net").shouldBeEmpty()
        config.pinsFor("a.b.payrdr.net").shouldBeEmpty()
        // `**.` covers the domain itself and any depth below it.
        config.pinsFor("bank.example") shouldBe setOf("sha256/$c")
        config.pinsFor("a.b.bank.example") shouldBe setOf("sha256/$c")
        config.pinsFor("notbank.example").shouldBeEmpty()
        config.pinsFor("other.host").shouldBeEmpty()
    }

    @Test fun `addPin rejects sha256-prefixed pin`() {
        val builder = SslPinningConfig.builder()
        shouldThrow<IllegalArgumentException> {
            builder.addPin("host", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        }
    }

    @Test fun `addPin rejects wrong-length pin`() {
        val builder = SslPinningConfig.builder()
        shouldThrow<IllegalArgumentException> { builder.addPin("host", "abc") }
    }

    @Test fun `addPin rejects empty hostname`() {
        val builder = SslPinningConfig.builder()
        shouldThrow<IllegalArgumentException> {
            builder.addPin("", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        }
    }

    @Test fun `build rejects empty pin set`() {
        shouldThrow<IllegalArgumentException> { SslPinningConfig.builder().build() }
    }

    @Test fun `sha256 of empty input matches known vector`() {
        // SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        val hash = sha256(byteArrayOf())
        val hex =
            hash.joinToString("") {
                it
                    .toInt()
                    .and(0xFF)
                    .toString(16)
                    .padStart(2, '0')
            }
        hex shouldBe "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    }
}
