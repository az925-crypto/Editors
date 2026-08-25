# Sora Editor - keep if needed
-keep class io.github.rosemoe.sora.** { *; }
-keep interface io.github.rosemoe.sora.** { *; }

# WebView JavaScript interface
-keepclassmembers class com.zaaam.editors.core.preview.ConsoleBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Kotlinx Serialization (for DataStore JSON)
-keep class kotlinx.serialization.** { *; }