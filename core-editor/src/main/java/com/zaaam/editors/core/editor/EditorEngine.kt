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

@Keep
class EditorEngine private constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CodeEditor(context, attrs, defStyleAttr) {

    companion object {
        @Volatile
        private var textMateInitialized = false

        @JvmStatic
        fun create(context: Context): EditorEngine = EditorEngine(context)

        @JvmStatic
        @Synchronized
        fun initTextMate(context: Context) {
            if (textMateInitialized) return
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
        }

        @JvmStatic
        fun createColorScheme(): TextMateColorScheme =
            TextMateColorScheme.create(ThemeRegistry.getInstance())
    }
}
