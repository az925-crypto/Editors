package com.zaaam.editors.core.editor

class SoraThemeMapper(private val tokens: Any) {
    fun map(tokens: Any): Map<String, Any> = emptyMap()
    fun mapLanguage(scope: String): String = scope
    fun textMateScheme(): Map<String, Any> = emptyMap()
}