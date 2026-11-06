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
    alias(libs.plugins.kover)
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

// The SDK's libraries go to Maven Central and, for the Flutter and React Native plugins in this repository, to a Maven
// repository inside this build (build/maven): ./gradlew publishAndroidToRepo, or scripts/build-bridge.sh for everything
// the plugins need. A release publishes every target (the Android one as <module>-android, iOS klibs for Kotlin
// Multiplatform apps) into build/staging-deploy, signed with SIGNING_KEY; .github/workflows/release.yml uploads it.
val sdkLibraries =
    mapOf(
        "sdk-core" to "PAYRDR SDK Core: card validation and card encryption into a payment token",
        "sdk-payment" to "PAYRDR payment SDK: the checkout with cards, 3-D Secure, Google Pay and Apple Pay",
        "sdk-payment-scanner" to "PAYRDR card scanner for the checkout's card form",
        "sdk-bridge" to "PAYRDR SDK for the Flutter and React Native plugins, as a JSON contract",
    )
subprojects {
    val pomDescription = sdkLibraries[name] ?: return@subprojects
    apply(plugin = rootProject.libs.plugins.kover.get().pluginId)
    extensions.configure<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension> {
        currentProject { createVariant("sdk") { add("debug") } }
    }
    // The Android release variant is published by the payrdr.kmp.library convention.
    apply(plugin = "maven-publish")
    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = "sdkRepo"
                url = uri(rootProject.layout.buildDirectory.dir("maven"))
            }
            maven {
                name = "staging"
                url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
            }
        }
        publications.withType<MavenPublication>().configureEach {
            // Maven Central wants a javadoc jar next to every artifact; Dokka output would add nothing to the KDoc in the
            // sources jar. One jar per publication, as signing the same file for two publications clashes.
            val publication = name
            artifact(
                tasks.register<Jar>("${publication}JavadocJar") {
                    archiveClassifier.set("javadoc")
                    archiveAppendix.set(publication)
                },
            )
            pom {
                name.set(project.name)
                description.set(pomDescription)
                url.set("https://github.com/Radarpayments/mobile-sdk")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("radarpayments")
                        name.set("BPC")
                        url.set("https://github.com/Radarpayments")
                    }
                }
                scm {
                    url.set("https://github.com/Radarpayments/mobile-sdk")
                    connection.set("scm:git:https://github.com/Radarpayments/mobile-sdk.git")
                    developerConnection.set("scm:git:ssh://git@github.com/Radarpayments/mobile-sdk.git")
                }
            }
        }
    }
    val signingKey = providers.environmentVariable("SIGNING_KEY").orNull
    if (!signingKey.isNullOrBlank()) {
        apply(plugin = "signing")
        extensions.configure<SigningExtension> {
            useInMemoryPgpKeys(signingKey, providers.environmentVariable("SIGNING_PASSWORD").orNull)
            sign(the<PublishingExtension>().publications)
        }
        // Every publication of a multiplatform module shares the root module's files: Gradle wants the order explicit.
        tasks.withType<AbstractPublishToMaven>().configureEach { mustRunAfter(tasks.withType<Sign>()) }
    }
}

tasks.register("publishAndroidToRepo") {
    group = "publishing"
    description = "Publishes the SDK's Android libraries to build/maven for the Flutter and React Native plugins."
    sdkLibraries.keys.forEach { module ->
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

// Test coverage of the SDK as testDebugUnitTest runs it (commonMain and androidMain): ./gradlew koverHtmlReportSdk.
// Kotlin/Native has no coverage tool, so iosMain and what only iosTest covers do not count.
dependencies {
    sdkLibraries.keys.forEach { kover(project(":$it")) }
}

kover {
    currentProject { createVariant("sdk") {} }
    reports {
        filters {
            excludes {
                // Screens are covered on devices and in iosTest, which this report does not see; generated code is not ours.
                annotatedBy("androidx.compose.runtime.Composable")
                classes("*.generated.resources.*", "*ComposableSingletons*", "*\$\$serializer")
            }
        }
    }
}
