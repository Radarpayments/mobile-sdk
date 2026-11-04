plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.mp) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.bcv) apply false
    alias(libs.plugins.skie) apply false
}

subprojects {
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)
    apply(plugin = rootProject.libs.plugins.detekt.get().pluginId)

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.3.1")
        android.set(true)
        verbose.set(true)
        filter {
            exclude("**/build/**")
            exclude("**/generated/**")
        }
    }

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        toolVersion = rootProject.libs.versions.detekt.get()
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        allRules = false
        // Known findings a module has not fixed yet (./gradlew :<module>:detektBaseline); new ones still fail.
        baseline = file("detekt-baseline.xml")
    }
}

// The SDK's Android libraries in a Maven repository inside this build (build/maven), where the Flutter and React Native
// plugins take them from: ./gradlew publishAndroidToRepo, or scripts/build-bridge.sh for everything the plugins need.
val sdkLibraries = listOf("sdk-core", "sdk-payment", "sdk-payment-scanner", "sdk-bridge")
subprojects {
    if (name !in sdkLibraries) return@subprojects
    // The Android release variant is published by the payrdr.kmp.library convention.
    apply(plugin = "maven-publish")
    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = "sdkRepo"
                url = uri(rootProject.layout.buildDirectory.dir("maven"))
            }
        }
    }
}

tasks.register("publishAndroidToRepo") {
    group = "publishing"
    description = "Publishes the SDK's Android libraries to build/maven for the Flutter and React Native plugins."
    sdkLibraries.forEach { module ->
        dependsOn(
            ":$module:publishKotlinMultiplatformPublicationToSdkRepoRepository",
            ":$module:publishAndroidReleasePublicationToSdkRepoRepository",
        )
    }
}

// The plugins write the SDK version they depend on as text (they are built in the app, outside this build), and the
// Flutter and React Native plugins share one version of their own. scripts/set-version.sh sets them all; this checks.
tasks.register("verifyVersions") {
    group = "verification"
    description = "Checks that the plugins depend on this SDK version and share one version of their own."
    val sdkVersion = version.toString()
    val sdkDependents =
        listOf("plugins/flutter/android/build.gradle.kts", "plugins/react-native/android/build.gradle").associateWith(::file)
    val pubspec = file("plugins/flutter/pubspec.yaml")
    val packageJson = file("plugins/react-native/package.json")
    inputs.files(sdkDependents.values, pubspec, packageJson)
    doLast {
        sdkDependents.forEach { (path, file) ->
            val dependsOn = Regex("""payrdrSdkVersion.*\?: "([^"]+)"""").find(file.readText())?.groupValues?.get(1)
            check(dependsOn == sdkVersion) { "$path depends on SDK $dependsOn, the SDK is $sdkVersion: run scripts/set-version.sh" }
        }
        val flutter = Regex("""(?m)^version: (\S+)""").find(pubspec.readText())?.groupValues?.get(1)
        val reactNative = Regex(""""version": "([^"]+)"""").find(packageJson.readText())?.groupValues?.get(1)
        check(flutter == reactNative) { "The Flutter plugin is $flutter, the React Native one $reactNative: run scripts/set-version.sh" }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
