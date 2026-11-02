import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkTask

plugins {
    id("payrdr.kmp.library")
    id("payrdr.kmp.compose.library")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.bcv)
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
    }

    // One framework with all of the SDK inside: an app can hold only one Kotlin/Native runtime that shares objects.
    val xcf = XCFramework("PayrdrSdkBridge")
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "PayrdrSdkBridge"
            isStatic = false
            binaryOption("bundleId", "net.payrdr.mobile.payment.sdk.bridge")
            // 14.0 is the SDK minimum; see sdk-payment.
            freeCompilerArgs +=
                "-Xoverride-konan-properties=osVersionMin.ios_arm64=14.0;osVersionMin.ios_simulator_arm64=14.0;osVersionMin.ios_x64=14.0"
            xcf.add(this)
        }
    }
}

// The checkout's strings and images (Compose resources of every module) go inside each framework of the XCFramework,
// where Compose Multiplatform 1.9+ looks for them: the app needs no build phase to copy them, whatever brings the
// framework in (Swift Package Manager, CocoaPods or Xcode).
tasks.withType<XCFrameworkTask>().configureEach {
    dependsOn("assembleIosArm64MainResources", "assembleIosSimulatorArm64MainResources")
    val resources = layout.buildDirectory.dir("kotlin-multiplatform-resources/aggregated-resources")
    doLast {
        outputs.files.asFileTree.matching { include("**/*.framework/Info.plist") }.forEach { plist ->
            val framework = plist.parentFile
            val target = if (framework.parentFile.name.endsWith("simulator")) "iosSimulatorArm64" else "iosArm64"
            copy {
                from(resources.get().dir("$target/composeResources"))
                into(framework.resolve("composeResources"))
            }
        }
    }
}

apiValidation {
    klib {
        enabled = true
    }
}
