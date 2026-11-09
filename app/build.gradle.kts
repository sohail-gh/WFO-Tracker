import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val signingProps = Properties().apply {
    val keystorePropsFile = rootProject.file("keystore.properties")
    if (keystorePropsFile.exists()) {
        FileInputStream(keystorePropsFile).use { load(it) }
    } else {
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            FileInputStream(localPropsFile).use { load(it) }
        }
    }
}

fun getSigningProp(key: String, envKey: String): String? {
    return System.getenv(envKey) ?: signingProps.getProperty(key)
}

android {
    namespace = "com.wfotracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wfotracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = getSigningProp("release.storeFile", "RELEASE_STORE_FILE")
                ?: "C:/Users/khans/.android/wfo-tracker-release.jks"
            val storePasswordProp = getSigningProp("release.storePassword", "RELEASE_STORE_PASSWORD")
            val keyAliasProp = getSigningProp("release.keyAlias", "RELEASE_KEY_ALIAS") ?: "wfo-tracker"
            val keyPasswordProp = getSigningProp("release.keyPassword", "RELEASE_KEY_PASSWORD")

            val storeFileObj = File(storeFilePath).let { if (it.isAbsolute) it else rootProject.file(storeFilePath) }
            if (storeFileObj.exists() && !storePasswordProp.isNullOrBlank() && !keyPasswordProp.isNullOrBlank()) {
                storeFile = storeFileObj
                storePassword = storePasswordProp
                keyAlias = keyAliasProp
                keyPassword = keyPasswordProp
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose (BOM)
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Jetpack Glance App Widget
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Preferences DataStore
    implementation(libs.androidx.datastore.preferences)

    // Kotlinx Serialization JSON
    implementation(libs.kotlinx.serialization.json)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
