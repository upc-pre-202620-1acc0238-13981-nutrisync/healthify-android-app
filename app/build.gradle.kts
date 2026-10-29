import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing: keystore.properties at the root locally, environment variables in CI.
val keystoreProperties = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

fun signingValue(key: String): String? =
    keystoreProperties.getProperty(key) ?: providers.environmentVariable(key).orNull

android {
    namespace = "pe.edu.upc.healthify"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "pe.edu.upc.healthify"
        minSdk = 24
        targetSdk = 37
        // CI passes -PversionCode with the run number: each distributed build needs a higher one.
        versionCode = providers.gradleProperty("versionCode").orNull?.toInt() ?: 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Without a keystore the release build stays unsigned instead of failing.
        signingValue("KEYSTORE_FILE")?.let { keystoreFile ->
            create("release") {
                storeFile = rootProject.file(keystoreFile)
                storePassword = signingValue("KEYSTORE_PASSWORD")
                keyAlias = signingValue("KEY_ALIAS")
                keyPassword = signingValue("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Port of the healthify-platform "http" profile (Properties/launchSettings.json).
            // 10.0.2.2 is the host as seen from the emulator.
            buildConfigField("String", "BASE_URL", "\"https://platform.healthify.lat/api/v1/\"")
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            buildConfigField("String", "BASE_URL", "\"https://platform.healthify.lat/api/v1/\"")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time (clinical and consultation dates) with minSdk 24.
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // Versions are pinned on purpose to EasySneaker's.
        // They are bumped together and by hand, not on a lint warning.
        disable += setOf("AndroidGradlePluginVersion", "GradleDependency")
    }
    androidResources {
        // values/ = Spanish (see src/main/res/resources.properties), values-en/ = English.
        generateLocaleConfig = true
    }
    bundle {
        // The in-app language switch needs both languages in the base APK.
        language {
            enableSplit = false
        }
    }
}

ksp {
    // Versioned Room schemas for writing migrations (AppDatabase, core/database).
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation (type-safe @Serializable routes)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    debugImplementation(libs.okhttp.logging)

    // Persistence
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.tink.android)

    // Images
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Camera, QR and background work
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.zxing.core)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
