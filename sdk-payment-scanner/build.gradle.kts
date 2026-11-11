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
    val xcf = XCFramework("sdk_payment_scanner")
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.compilations.getByName("main").cinterops.create("scannervision") {
            defFile(project.file("src/nativeInterop/cinterop/scannervision.def"))
            packageName("net.payrdr.mobile.payment.sdk.payment.scanner.vision")
        }
        target.binaries.framework {
            baseName = "sdk_payment_scanner"
            isStatic = false
            binaryOption("bundleId", "net.payrdr.mobile.payment.sdk.sdkpaymentscanner")
            // Kotlin/Native builds for iOS 12.0 by default. Below 12.2 (before Swift shipped with iOS) the
            // linker binds CoreMedia functions to the libswiftCoreMedia overlay instead of CoreMedia, and
            // on devices whose overlay lacks them the first camera frame crashed. 14.0 is the SDK minimum.
            freeCompilerArgs +=
                "-Xoverride-konan-properties=osVersionMin.ios_arm64=14.0;osVersionMin.ios_simulator_arm64=14.0;osVersionMin.ios_x64=14.0"
            // Re-export sdk-payment + sdk-core so merchants who use the scanner framework get a
            // single-module Swift import surface (`import sdk_payment_scanner` brings every
            // public type into scope). Consumers MUST NOT also link `sdk_payment.xcframework`
            // alongside this framework — doing so creates dual Swift types for the same Kotlin
            // class (`sdk_payment_scanner.SdkPayment` vs `sdk_payment.SdkPayment`) and breaks
            // type-check. The SampleApp's Xcode project is configured accordingly: it links
            // only this one XCFramework.
            export(project(":sdk-payment"))
            // sdk-core intentionally NOT re-exported: leaving it as an implicit transitive
            // module keeps its Swift names under the `Sdk_core*` prefix (matching the shape
            // sdk_payment.xcframework exposed before scanner was added), so existing merchant
            // code that references e.g. `Sdk_coreTokenResult` keeps compiling.
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
