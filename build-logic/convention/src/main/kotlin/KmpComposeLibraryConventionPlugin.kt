import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Adds Compose Multiplatform + Kotlin Compose compiler plugin on top of `payrdr.kmp.library`.
 *
 * Apply BOTH to a module that needs Compose UI:
 *
 *   plugins {
 *       id("payrdr.kmp.library")
 *       id("payrdr.kmp.compose.library")
 *   }
 *
 * Configures `composeResources` default namespace so the generated `Res` accessor lives under
 * the module's main package, and enables the Android compose build feature.
 */
class KmpComposeLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<ComposeExtension> {
            extensions.configure<ResourcesExtension> {
                // package where the generated `Res` accessor and string table go
                packageOfResClass = "net.payrdr.mobile.payment.sdk.${project.name.replace("-", ".")}.generated.resources"
                publicResClass = false
                generateResClass = ResourcesExtension.ResourceClassGeneration.Auto
            }
        }

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.apply {
                commonMain.get().dependencies {
                    implementation(libs.library("compose-mp-runtime"))
                    implementation(libs.library("compose-mp-foundation"))
                    implementation(libs.library("compose-mp-material3"))
                    implementation(libs.library("compose-mp-material-icons-extended"))
                    implementation(libs.library("compose-mp-ui"))
                    implementation(libs.library("compose-mp-components-resources"))
                    implementation(libs.library("compose-mp-components-ui-tooling-preview"))
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
