plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.zaaam.editors"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zaaam.editors"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        vectorDrawables.useSupportLibrary = true
    }

    kotlin {
        jvmToolchain(17)
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.4.10"
    }

    signingConfigs {
        create("config") {
            val storeFilePath = System.getenv("RELEASE_STORE_FILE") ?: ""
            if (storeFilePath.isNotBlank()) {
                storeFile = file(storeFilePath)
                storePassword = System.getenv("RELEASE_STORE_PASS") ?: ""
                keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: ""
                keyPassword = System.getenv("RELEASE_KEY_PASS") ?: ""
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("config")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("config")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.bom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
    implementation(project(":core-fs"))
    implementation(project(":core-editor"))
    implementation(project(":core-preview"))

    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit4:2.4.10")
}