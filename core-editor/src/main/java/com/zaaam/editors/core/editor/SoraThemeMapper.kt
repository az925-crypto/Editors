package com.zaaam.editors.core.editor

import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.TextMateColorScheme
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

class SoraThemeMapper(private val tokens: Object) {
    fun map(tokens: Object): EditorColorScheme {
        // TODO: implement full mapping from RetroTokens to EditorColorScheme
        return EditorColorScheme()
    }

    fun mapLanguage(scope: String): Language {
        return Language(scope)
    }

    fun textMateScheme(): TextMateColorScheme {
        // TODO: map RetroTokens to TextMateColorScheme
        return TextMateColorScheme()
    }
}