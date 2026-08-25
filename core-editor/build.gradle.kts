plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.zaaam.editors.core.editor"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}