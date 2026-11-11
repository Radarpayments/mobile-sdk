group = "net.payrdr.mobile.payment.flutter"
// The plugin's version is the one in pubspec.yaml.
version = file("../pubspec.yaml").readLines().first { it.startsWith("version:") }.substringAfter(":").trim()

buildscript {
    val kotlinVersion = "2.4.0"
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:9.1.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

// The SDK's Android libraries. In this repository they are built into build/maven by scripts/build-bridge.sh; an app
// that takes the plugin from elsewhere points payrdrMavenUrl (gradle.properties) at the repository that serves them.
val payrdrMavenUrl: String =
    (findProperty("payrdrMavenUrl") as String?) ?: file("../../../build/maven").toURI().toString()
val payrdrSdkVersion: String = (findProperty("payrdrSdkVersion") as String?) ?: "4.0.0-beta.4"

// The app resolves the SDK as well (the plugin's dependency is part of its build), so it needs the repository too.
rootProject.allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri(payrdrMavenUrl) }
    }
}

plugins {
    id("com.android.library")
}

android {
    namespace = "net.payrdr.mobile.payment.flutter"

    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
        getByName("test") {
            java.srcDirs("src/test/kotlin")
        }
    }

    defaultConfig {
        minSdk = 24
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()

                it.outputs.upToDateWhen { false }

                it.testLogging {
                    events("passed", "skipped", "failed", "standardOut", "standardError")
                    showStandardStreams = true
                }
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation("io.github.radarpayments:sdk-bridge-android:$payrdrSdkVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.mockito:mockito-core:5.0.0")
}
