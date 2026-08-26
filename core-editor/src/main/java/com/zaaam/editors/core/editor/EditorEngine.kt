package com.zaaam.editors.core.editor

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.Keep
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Keep
class EditorEngine private constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CodeEditor(context, attrs, defStyleAttr) {

    companion object {
        @Volatile
        private var textMateInitialized = false

        // MEDIUM FIX (retry storm): sinyal publik "preload TextMate selesai". UI (EditorScreen)
        // memakai ini untuk mencoba apply language tepat setelah preload kelar, alih-alih
        // menebak-nebak lewat recomposition.
        private val _textMateReady = MutableStateFlow(false)
        val textMateReady: StateFlow<Boolean> = _textMateReady.asStateFlow()

        @JvmStatic
        fun create(context: Context): EditorEngine = EditorEngine(context)

        @JvmStatic
        @Synchronized
        fun initTextMate(context: Context) {
            if (textMateInitialized) return
            try {
                val fileProvider = FileProviderRegistry.getInstance()
                fileProvider.addFileProvider(AssetsFileResolver(context.applicationContext.assets))
                val themePath = "textmate/themes/retro-lcd.json"
                val model = ThemeModel(
                    org.eclipse.tm4e.core.registry.IThemeSource.fromInputStream(
                        fileProvider.tryGetInputStream(themePath),
                        themePath,
                        null
                    ),
                    "retro-lcd"
                )
                model.setDark(true)
                ThemeRegistry.getInstance().loadTheme(model)
                ThemeRegistry.getInstance().setTheme("retro-lcd")
                GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
                textMateInitialized = true
                _textMateReady.value = true
            } catch (e: Exception) {
                // CRITICAL (v0.2.0 device crash): preload jalan di Application.onCreate — exception
                // di sini MEMBUNUH PROSES sebelum UI muncul. Degradasi anggun: editor tetap hidup
                // tanpa highlighting, ready-signal tetap false supaya UI tidak apply language.
                // Jangan rethrow; textMateInitialized tetap false agar bisa retry manual.
                textMateInitialized = false
            }
        }

        @JvmStatic
        fun createColorScheme(): TextMateColorScheme =
            TextMateColorScheme.create(ThemeRegistry.getInstance())
    }
}
