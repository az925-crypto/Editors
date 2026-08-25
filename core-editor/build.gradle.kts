plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.zaaam.editors.core.editor"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.sora.editor)
    implementation(libs.sora.textmate)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}