package com.zaaam.editors.core.editor

class LanguageResolver {
    fun resolve(name: String): String {
        val ext = name.substringAfterLast(".", "").lowercase()
        return when (ext) {
            "html" -> "text.html.basic"
            "css" -> "source.css"
            "js" -> "source.js"
            "kt", "kts" -> "source.kotlin"
            "py" -> "source.python"
            "json" -> "source.json"
            else -> "text.plain"
        }
    }
}