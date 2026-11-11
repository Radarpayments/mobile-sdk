import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("payrdr.kmp.library")
    id("payrdr.kmp.compose.android")
    alias(libs.plugins.bcv)
    alias(libs.plugins.skie)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":sdk-core"))
            api(project(":sdk-payment"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(libs.turbine)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.mlkit.text.recognition)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.androidx.startup.runtime)
        }
    }
}

val generateScannerStrings =
    tasks.register<GeneratePaymentStringsTask>("generateScannerStrings") {
        stringFiles.from(fileTree("src/commonMain/strings") { include("values*/strings.xml") })
        packageName.set("net.payrdr.mobile.payment.sdk.payment.scanner.presentation")
        outputDir.set(layout.buildDirectory.dir("generated/scannerStrings/kotlin"))
        resourcesPackage.set("net.payrdr.mobile.payment.sdk.sdk.payment.scanner.generated.resources")
        resourcesOutputDir.set(layout.buildDirectory.dir("generated/scannerStrings/android"))
    }

kotlin {
    sourceSets.all { languageSettings.optIn("net.payrdr.mobile.payment.sdk.payment.presentation.PayrdrUiApi") }
    // Both scanner screens read their texts from Kotlin tables written from the XML files; Android adds `Res.string`.
    sourceSets.commonMain {
        kotlin.srcDir(generateScannerStrings.flatMap { it.outputDir })
    }
    sourceSets.androidMain {
        kotlin.srcDir(generateScannerStrings.flatMap { it.resourcesOutputDir })
    }
}

kotlin {
    val xcf = XCFramework("RadarpaymentsSDKPayment")
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "RadarpaymentsSDKPayment"
            isStatic = false
            binaryOption("bundleId", "net.payrdr.mobile.payment.sdk.payment")
            // Kotlin/Native builds for iOS 12.0 by default; 14.0 is the SDK minimum.
            freeCompilerArgs +=
                "-Xoverride-konan-properties=osVersionMin.ios_arm64=14.0;osVersionMin.ios_simulator_arm64=14.0;osVersionMin.ios_x64=14.0"
            // This is the framework apps link for the checkout. Both modules are exported so that their types have
            // plain Swift names, the same ones RadarpaymentsSDKCore gives sdk-core: an app moves from one framework
            // to the other by changing the import. An app links one of the two, never both: each Kotlin/Native
            // framework has its own runtime and its own Swift type for the same Kotlin class.
            export(project(":sdk-payment"))
            export(project(":sdk-core"))
            xcf.add(this)
        }
    }
}

@OptIn(kotlinx.validation.ExperimentalBCVApi::class)
apiValidation {
    nonPublicMarkers += "net.payrdr.mobile.payment.sdk.payment.presentation.PayrdrUiApi"
    klib {
        enabled = true
    }
}
