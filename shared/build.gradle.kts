import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            // Constructing a real DrawScope in jvmTest (to assert on rendered
            // pixels) needs Skiko's native library on the runtime classpath;
            // it isn't pulled in by the Compose Multiplatform Gradle plugin
            // for a plain jvm() target the way it is for a packaged desktop
            // app. Version must track whatever skiko-awt Compose Multiplatform
            // 1.11.1 resolves.
            runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:0.144.6")
        }
    }
}

android {
    namespace = "net.clahey.kinderdraw.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
    }
}
