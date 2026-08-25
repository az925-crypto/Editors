plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.zaaam.editors.core.fs"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}