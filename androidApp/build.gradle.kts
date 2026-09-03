import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "net.clahey.kinderdraw"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.clahey.kinderdraw"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"

        buildConfigField("boolean", "FAIL_SAVES", "false")
    }

    buildTypes {
        debug {
            // `./gradlew :androidApp:installDebug -PkinderdrawFailSaves=true`
            // to watch New Picture's failed-save feedback on a device. No real
            // MediaStore write can be made to fail by hand — see
            // FailingImageStorage.
            buildConfigField(
                "boolean",
                "FAIL_SAVES",
                (findProperty("kinderdrawFailSaves") == "true").toString(),
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
}
