import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("payrdr.kmp.library")
    id("payrdr.kmp.compose.android")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.bcv)
    alias(libs.plugins.skie)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":sdk-core"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        commonTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.ktor.client.mock)
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            // api: PayrdrCheckout takes ComponentActivity and ActivityResultCaller in its public signatures.
            api(libs.androidx.activity.compose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.webkit)
            // Ktor OkHttp engine + OkHttp for SSL pinning via CertificatePinner.
            implementation(libs.ktor.client.okhttp)
            implementation(libs.okhttp)
            // Google Pay SDK: compileOnly so merchants who don't enable GPay don't pay the size cost.
            compileOnly(libs.play.services.wallet)
            // Task<T>.await() bridge — used by GooglePayTokenProvider; matches play-services-wallet use.
            // Exclude stale play-services-tasks transitive (strictly 16.0.1 drags in android.arch.lifecycle:1.0.0
            // that isn't served by the filtered google() repo); merchants get a modern tasks via play-services-wallet.
            implementation(
                libs.kotlinx.coroutines.play.services
                    .get()
                    .toString(),
            ) {
                exclude(group = "com.google.android.gms", module = "play-services-tasks")
            }
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.compose.ui.test.junit4)
                implementation(libs.androidx.compose.ui.test.manifest)
                implementation(libs.kotlinx.coroutines.test)
                implementation(kotlin("test"))
                implementation(libs.androidx.test.espresso.core)
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.core)
            }
        }
    }

    val xcf = XCFramework("sdk_payment")
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "sdk_payment"
            isStatic = false
            // CFBundleIdentifier must not contain underscores; override so Xcode codesign
            // + validate accept the framework when embedded into an app bundle.
            binaryOption("bundleId", "net.payrdr.mobile.payment.sdk.sdkpayment")
            // Kotlin/Native builds for iOS 12.0 by default; 14.0 is the SDK minimum.
            freeCompilerArgs +=
                "-Xoverride-konan-properties=osVersionMin.ios_arm64=14.0;osVersionMin.ios_simulator_arm64=14.0;osVersionMin.ios_x64=14.0"
            xcf.add(this)
        }
    }
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
}

val generatePaymentStrings =
    tasks.register<GeneratePaymentStringsTask>("generatePaymentStrings") {
        stringFiles.from(fileTree("src/commonMain/strings") { include("values*/strings.xml") })
        packageName.set("net.payrdr.mobile.payment.sdk.payment.presentation")
        outputDir.set(layout.buildDirectory.dir("generated/paymentStrings/kotlin"))
        resourcesPackage.set("net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources")
        resourcesOutputDir.set(layout.buildDirectory.dir("generated/paymentStrings/android"))
    }

kotlin {
    sourceSets.all { languageSettings.optIn("net.payrdr.mobile.payment.sdk.payment.presentation.PayrdrUiApi") }
    // Both UIs read their texts from Kotlin tables written from the XML files; Android adds the `Res.string` keys.
    sourceSets.commonMain {
        kotlin.srcDir(generatePaymentStrings.flatMap { it.outputDir })
    }
    sourceSets.androidMain {
        kotlin.srcDir(generatePaymentStrings.flatMap { it.resourcesOutputDir })
    }
}

@OptIn(kotlinx.validation.ExperimentalBCVApi::class)
apiValidation {
    nonPublicMarkers += "net.payrdr.mobile.payment.sdk.payment.presentation.PayrdrUiApi"
    ignoredClasses += "net.payrdr.mobile.payment.sdk.payment.presentation.PayrdrUiApi"
    klib {
        enabled = true
    }
}
