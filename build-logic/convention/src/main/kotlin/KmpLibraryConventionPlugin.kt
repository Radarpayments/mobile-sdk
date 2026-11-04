import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/**
 * Applies the standard KMP library setup: Kotlin Multiplatform plugin, Android library plugin,
 * three iOS targets + Android target, source-set hierarchy, and default JVM toolchain.
 *
 * Consuming module only needs to add its own dependencies and declare itself via:
 *   plugins { id("payrdr.kmp.library") }
 */
class KmpLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.library")

        extensions.configure<KotlinMultiplatformExtension> {
            jvmToolchain(libs.intVersion("jvm-target"))
            explicitApi()

            androidTarget {
                // What maven-publish publishes for Android (Flutter and React Native plugins consume it; see the root build).
                publishLibraryVariants("release")
                compilations.all {
                    compileTaskProvider.configure {
                        compilerOptions {
                            jvmTarget.set(JvmTarget.fromTarget(libs.version("jvm-target")))
                        }
                    }
                }
                // Instrumented tests are only androidInstrumentedTest: commonTest already runs on the JVM and iOS, and its
                // names with spaces cannot be dexed for the device below DEX 040 (API 30), and minSdk is lower.
                @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
                unitTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
            }

            iosX64()
            iosArm64()
            iosSimulatorArm64()

            sourceSets.apply {
                commonMain.get().dependencies {
                    implementation(libs.findLibrary("kotlin.stdlib").get())
                }
                commonTest.get().dependencies {
                    implementation(libs.findLibrary("kotest.assertions.core").get())
                    implementation(kotlin("test"))
                }
            }

            // kotlinx-datetime 0.7 (Compose Multiplatform 1.9) moved Instant and Clock to kotlin.time, which stays
            // experimental until Kotlin 2.3.
            sourceSets.all { languageSettings.optIn("kotlin.time.ExperimentalTime") }
            // What the SDK modules share with each other and nobody else.
            sourceSets.all { languageSettings.optIn("net.payrdr.mobile.payment.sdk.core.InternalPayrdrApi") }

            // The version is written only in gradle.properties; the code and the iOS frameworks take it from there.
            val generateSdkVersion =
                tasks.register<GenerateSdkVersionTask>("generateSdkVersion") {
                    sdkVersion.set(project.version.toString())
                    packageName.set("net.payrdr.mobile.payment.sdk.${project.name.removePrefix("sdk-").replace("-", ".")}")
                    outputDir.set(layout.buildDirectory.dir("generated/sdkVersion/kotlin"))
                }
            sourceSets.commonMain.get().kotlin.srcDir(generateSdkVersion.flatMap { it.outputDir })
            targets.withType<KotlinNativeTarget>().configureEach {
                binaries.withType<Framework>().configureEach {
                    // Apple takes at most three integers here, in an embedded framework too: 4.0.0 for 4.0.0-beta.1.
                    val release = project.version.toString().substringBefore('-')
                    binaryOption("bundleShortVersionString", release)
                    binaryOption("bundleVersion", release)
                }
            }

            applyDefaultHierarchyTemplate()
        }

        extensions.configure<LibraryExtension> {
            compileSdk = libs.intVersion("android-compile-sdk")
            namespace = "net.payrdr.mobile.payment.sdk.${project.name.replace("-", ".")}"

            defaultConfig {
                minSdk = libs.intVersion("android-min-sdk")
            }

            // The instrumented-test APK otherwise targets minSdk, and recent Android puts its permission review for
            // apps that old over the test activity: Compose UI tests then find no hierarchy to test.
            testOptions {
                targetSdk = libs.intVersion("android-compile-sdk")
            }

            // Lifecycle's LiveData check crashes the lint of AGP 8.7 on Kotlin 2.2 code (IncompatibleClassChangeError),
            // and the SDK has no LiveData for it to check.
            lint {
                disable += "NullSafeMutableLiveData"
            }

            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(libs.intVersion("jvm-target"))
                targetCompatibility = JavaVersion.toVersion(libs.intVersion("jvm-target"))
            }
        }
    }

}
