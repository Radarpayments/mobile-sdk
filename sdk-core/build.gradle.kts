import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("payrdr.kmp.library")
    alias(libs.plugins.bcv)
    alias(libs.plugins.skie)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(kotlin("test"))
        }
    }
}

kotlin {
    // The framework of an app that only tokenizes cards. An app with the checkout links RadarpaymentsSDKPayment
    // instead, which has these types under the same Swift names.
    val xcf = XCFramework("RadarpaymentsSDKCore")
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "RadarpaymentsSDKCore"
            isStatic = false
            binaryOption("bundleId", "net.payrdr.mobile.payment.sdk.core")
            // Kotlin/Native builds for iOS 12.0 by default; 14.0 is the SDK minimum.
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
