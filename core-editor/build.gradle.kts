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
    api(libs.sora.editor)
    api(libs.sora.editor.language.textmate)

    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.4.10")
}