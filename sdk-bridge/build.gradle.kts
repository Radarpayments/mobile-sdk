import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("payrdr.kmp.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.bcv)
    // Bundles the SDK's SwiftUI checkout into the framework; without it the plugins' checkout cannot open on iOS.
    alias(libs.plugins.skie)
}

// The SDK for apps that are not written in Kotlin or Swift: the Flutter and React Native plugins take the checkout and
// SDK Core through it, with a JSON contract (see README.md) instead of Kotlin types.

// The contract's sample messages (contract/*.json), which the plugins' tests read too, as constants for commonTest:
// tests on iOS have no file of the project to read.
val contractFixtures =
    tasks.register("generateContractFixtures") {
        val contract = layout.projectDirectory.dir("contract")
        val out = layout.buildDirectory.dir("generated/contract")
        inputs.dir(contract)
        outputs.dir(out)
        doLast {
            val constants =
                contract.asFile
                    .listFiles { file -> file.extension == "json" }!!
                    .sortedBy { it.name }
                    .joinToString("\n\n") { file ->
                        val name = file.nameWithoutExtension.uppercase().replace('-', '_')
                        val text =
                            file
                                .readText()
                                .replace("\\", "\\\\")
                                .replace("\"", "\\\"")
                                .replace("$", "\\$")
                                .replace("\n", "\\n")
                        "    const val $name: String = \"$text\""
                    }
            val source = out.get().file("net/payrdr/mobile/payment/sdk/bridge/ContractFixtures.kt").asFile
            source.parentFile.mkdirs()
            source.writeText(
                "@file:Suppress(\"ktlint\")\n\npackage net.payrdr.mobile.payment.sdk.bridge\n\n" +
                    "/** Generated from sdk-bridge/contract by generateContractFixtures. */\n" +
                    "internal object ContractFixtures {\n$constants\n}\n",
            )
        }
    }

kotlin {
    // The camera contract of the scanner, which the plugins' Swift sources implement.
    sourceSets.all { languageSettings.optIn("net.payrdr.mobile.payment.sdk.payment.presentation.PayrdrUiApi") }
    sourceSets {
        commonTest.get().kotlin.srcDir(contractFixtures)
        commonMain.dependencies {
            implementation(project(":sdk-core"))
            implementation(project(":sdk-payment"))
            implementation(project(":sdk-payment-scanner"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            // sdk-payment leaves Google Pay to the app (compileOnly); a plugin user gets it without touching Gradle.
            implementation(libs.play.services.wallet)
        }
        iosMain.dependencies {
            // Exported below.
            api(project(":sdk-payment"))
            api(project(":sdk-payment-scanner"))
        }
    }

    // One framework with all of the SDK inside: an app can hold only one Kotlin/Native runtime that shares objects.
    val xcf = XCFramework("PayrdrSdkBridge")
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "PayrdrSdkBridge"
            isStatic = false
            binaryOption("bundleId", "net.payrdr.mobile.payment.sdk.bridge")
            // The bundled SwiftUI checkout and the plugins' scanner sources refer to the SDK's types by their own names,
            // as in RadarpaymentsSDKPayment.
            export(project(":sdk-payment"))
            export(project(":sdk-payment-scanner"))
            // 14.0 is the SDK minimum; see sdk-payment.
            freeCompilerArgs +=
                "-Xoverride-konan-properties=osVersionMin.ios_arm64=14.0;osVersionMin.ios_simulator_arm64=14.0;osVersionMin.ios_x64=14.0"
            xcf.add(this)
        }
    }
}

@OptIn(kotlinx.validation.ExperimentalBCVApi::class)
apiValidation {
    klib {
        enabled = true
    }
}
