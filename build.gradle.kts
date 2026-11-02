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

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
