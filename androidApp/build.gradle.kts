import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
}

// Release signing credentials, read from ~/.gradle/gradle.properties (outside
// this repository — the committed gradle.properties must never hold them) or
// from ORG_GRADLE_PROJECT_-prefixed environment variables. Names avoid dots so
// the environment form stays expressible.
//
// Absent entirely, the release build is simply unsigned rather than broken, so
// a checkout without the keystore still builds. Pass them with -P only if you
// don't mind the password appearing in `ps`.
val keystorePath: String? = findProperty("KINDERDRAW_KEYSTORE") as String?
val keystorePassword: String? = findProperty("KINDERDRAW_KEYSTORE_PASSWORD") as String?

android {
    namespace = "net.clahey.kinderdraw"
    compileSdk = 36

    signingConfigs {
        if (keystorePath != null && keystorePassword != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = (findProperty("KINDERDRAW_KEY_ALIAS") as String?) ?: "kinderdraw"
                // PKCS12 keystores — what keytool produces by default, whatever
                // the file is named — require these to match, so the key
                // password falls back to the store password.
                keyPassword = (findProperty("KINDERDRAW_KEY_PASSWORD") as String?) ?: keystorePassword
            }
        }
    }

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

        release {
            // Nothing to obfuscate in a public-source app, and no keep rules to
            // get wrong — see the Publishing LLD's Decisions.
            isMinifyEnabled = false

            // Null when the credentials above are absent, which leaves the
            // bundle unsigned rather than failing the build.
            signingConfig = signingConfigs.findByName("release")
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
