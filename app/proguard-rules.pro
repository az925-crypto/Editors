# Sora Editor - keep if needed
-keep class io.github.rosemoe.sora.** { *; }
-keep interface io.github.rosemoe.sora.** { *; }

# Sora 0.23.6 (ShareableData$DefaultImpls) mereferensikan kelas yang tidak ada di
# kotlin-stdlib baru — hanya referensi metadata kompilasi lama, aman di-dontwarn.
-dontwarn kotlin.Cloneable$DefaultImpls

# WebView JavaScript interface
-keepclassmembers class com.zaaam.editors.core.preview.ConsoleBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Kotlinx Serialization (for DataStore JSON)
-keep class kotlinx.serialization.** { *; }