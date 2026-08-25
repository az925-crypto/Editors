package com.zaaam.editors.core.editor

import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

class SoraThemeMapper {
    fun applyChromeOverrides(scheme: EditorColorScheme) {
        scheme.setColor(EditorColorScheme.SCROLL_BAR_TRACK, 0xFF5A6340.toInt())
        scheme.setColor(EditorColorScheme.SCROLL_BAR_THUMB, 0xFF8FA06A.toInt())
        scheme.setColor(EditorColorScheme.SCROLL_BAR_THUMB_PRESSED, 0xFFB8C24D.toInt())
        scheme.setColor(EditorColorScheme.LINE_NUMBER_PANEL, 0xF01A2010.toInt())
        scheme.setColor(EditorColorScheme.LINE_NUMBER_PANEL_TEXT, 0xFF8FA06A.toInt())
        scheme.setColor(EditorColorScheme.COMPLETION_WND_BACKGROUND, 0xFF232B14.toInt())
        scheme.setColor(EditorColorScheme.COMPLETION_WND_CORNER, 0xFF5A6340.toInt())
        scheme.setColor(EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY, 0xFFDDE8A0.toInt())
        scheme.setColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY, 0xFF8FA06A.toInt())
        scheme.setColor(EditorColorScheme.TEXT_ACTION_WINDOW_BACKGROUND, 0xFF232B14.toInt())
    }
}
