plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "net.payrdr.mobile.payment.samples.android"
    compileSdk =
        libs.versions.android.compile.sdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "net.payrdr.mobile.payment.samples.android"
        minSdk =
            libs.versions.android.min.sdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.compile.sdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.toVersion(
                libs.versions.jvm.target
                    .get()
                    .toInt(),
            )
        targetCompatibility =
            JavaVersion.toVersion(
                libs.versions.jvm.target
                    .get()
                    .toInt(),
            )
    }

    buildFeatures {
        compose = true
    }

    // Lifecycle's LiveData check crashes the lint of AGP 8.7 on Kotlin 2.2 code; the app has no LiveData.
    lint {
        disable += "NullSafeMutableLiveData"
    }

    kotlin {
        jvmToolchain(
            libs.versions.jvm.target
                .get()
                .toInt(),
        )
    }
}

dependencies {
    implementation(project(":sdk-core"))
    implementation(project(":sdk-payment"))
    implementation(project(":sdk-payment-scanner"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.compose.icons.tabler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)

    // Google Pay — sdk-payment declares play-services-wallet as compileOnly, so the app (the
    // "merchant") has to ship it: without it the SDK falls back to a plain button and cannot pay.
    implementation(libs.play.services.wallet)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(kotlin("test"))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.withType<Test> { useJUnitPlatform() }
