import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.android")

        extensions.configure<LibraryExtension> {
            compileSdk = libs.intVersion("android-compile-sdk")
            namespace = "net.payrdr.mobile.payment.sdk.${project.name.replace("-", ".")}"

            defaultConfig {
                minSdk = libs.intVersion("android-min-sdk")
            }

            compileOptions {
                sourceCompatibility = JavaVersion.toVersion(libs.intVersion("jvm-target"))
                targetCompatibility = JavaVersion.toVersion(libs.intVersion("jvm-target"))
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            jvmToolchain(libs.intVersion("jvm-target"))
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget(libs.version("jvm-target")))
            }
        }
    }

}
