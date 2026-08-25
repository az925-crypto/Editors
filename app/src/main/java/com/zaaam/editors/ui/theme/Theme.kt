package com.zaaam.editors.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val lightScheme = lightColorScheme(
    primary = RetroTokens.Olive,
    onPrimary = RetroTokens.Ink,
    background = RetroTokens.Shell,
    onBackground = RetroTokens.Graphite,
    surface = RetroTokens.Card,
    onSurface = RetroTokens.Graphite,
    error = RetroTokens.Brick,
    onError = RetroTokens.Ink,
)

@Composable
fun RetroTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightScheme, content = content)
}