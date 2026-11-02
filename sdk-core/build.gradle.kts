plugins {
    id("payrdr.kmp.library")
    alias(libs.plugins.bcv)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotest.assertions.core)
            implementation(kotlin("test"))
        }
    }
}

apiValidation {
    klib {
        enabled = true
    }
}
