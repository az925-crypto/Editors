package com.zaaam.editors.core.editor

import io.github.rosemoe.sora.widget.EditorColorScheme

class SoraThemeMapper {
    fun applyChromeOverrides(scheme: EditorColorScheme) {
        scheme.setColor(EditorColorScheme.SCROLL_BAR, 0xFF5A6340.toInt())
        scheme.setColor(EditorColorScheme.SCROLL_BAR_THUMB, 0xFF8FA06A.toInt())
        scheme.setColor(EditorColorScheme.PANEL_BACKGROUND, 0xFF1A2010.toInt())
        scheme.setColor(EditorColorScheme.WHITESPACE, 0xFF5A6340.toInt())
    }
}
