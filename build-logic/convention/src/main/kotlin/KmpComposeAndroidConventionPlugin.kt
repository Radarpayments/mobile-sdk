import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/**
 * Adds the Compose UI of the Android target on top of `payrdr.kmp.library`:
 *
 *   plugins {
 *       id("payrdr.kmp.library")
 *       id("payrdr.kmp.compose.android")
 *   }
 *
 * Compose stays out of the iOS targets: the compiler plugin runs for Android only and the Compose libraries are
 * `androidMain` dependencies, so the iOS frameworks carry neither Compose nor Skia. The iOS UI is SwiftUI.
 */
class KmpComposeAndroidConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<ComposeCompilerGradlePluginExtension> {
            targetKotlinPlatforms.set(setOf(KotlinPlatformType.androidJvm))
        }

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.apply {
                androidMain.get().dependencies {
                    implementation(libs.library("compose-mp-runtime"))
                    implementation(libs.library("compose-mp-foundation"))
                    implementation(libs.library("compose-mp-material3"))
                    implementation(libs.library("compose-mp-material-icons-extended"))
                    implementation(libs.library("compose-mp-ui"))
                }
            }
        }

        extensions.configure<LibraryExtension> {
            buildFeatures {
                compose = true
            }
        }
    }
}
