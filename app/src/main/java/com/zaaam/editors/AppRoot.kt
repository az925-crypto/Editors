package com.zaaam.editors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.session.AppScreen
import com.zaaam.editors.ui.editor.EditorScreen
import com.zaaam.editors.ui.files.FilesScreen
import com.zaaam.editors.ui.preview.PreviewScreen
import com.zaaam.editors.ui.tools.ToolsScreen
import com.zaaam.editors.ui.theme.RetroTokens

@Composable
fun AppRoot(container: AppContainer) {
    val screen by container.screenState.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (screen) {
                AppScreen.FILES -> FilesScreen(container)
                AppScreen.EDITOR -> EditorScreen(container)
                AppScreen.PREVIEW -> PreviewScreen(container)
                AppScreen.TOOLS -> ToolsScreen(container)
            }
        }
        BottomNav(container)
    }
}

@Composable
private fun BottomNav(container: AppContainer) {
    val screen by container.screenState.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RetroTokens.Card)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem("Files", screen == AppScreen.FILES, Modifier.weight(1f)) { container.screenState.value = AppScreen.FILES }
        NavItem("Editor", screen == AppScreen.EDITOR, Modifier.weight(1f)) { container.screenState.value = AppScreen.EDITOR }
        NavItem("Preview", screen == AppScreen.PREVIEW, Modifier.weight(1f)) { container.screenState.value = AppScreen.PREVIEW }
        NavItem("Alat", screen == AppScreen.TOOLS, Modifier.weight(1f)) { container.screenState.value = AppScreen.TOOLS }
    }
}

@Composable
private fun NavItem(label: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) RetroTokens.Graphite else RetroTokens.Card)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = if (active) RetroTokens.Olive else RetroTokens.Dim, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp)
    }
}