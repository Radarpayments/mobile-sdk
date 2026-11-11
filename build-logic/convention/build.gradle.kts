plugins {
    `kotlin-dsl`
}

group = "net.payrdr.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        // Gradle 8.10 compiles build logic with Kotlin 1.9; the Kotlin and Compose Multiplatform plugins are built with 2.x.
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "payrdr.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "payrdr.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("kmpComposeAndroid") {
            id = "payrdr.kmp.compose.android"
            implementationClass = "KmpComposeAndroidConventionPlugin"
        }
    }
}
