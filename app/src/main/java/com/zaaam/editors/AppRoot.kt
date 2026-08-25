package com.zaaam.editors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.session.AppScreen
import com.zaaam.editors.ui.editor.EditorScreen
import com.zaaam.editors.ui.files.FilesScreen
import com.zaaam.editors.ui.preview.PreviewScreen

@Composable
fun AppRoot(container: AppContainer) {
    val screen by container.screenState.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (screen) {
                AppScreen.FILES -> FilesScreen(container)
                AppScreen.EDITOR -> EditorScreen(container)
                AppScreen.PREVIEW -> PreviewScreen(container)
            }
        }
        BottomNav(container)
    }
}

@Composable
private fun BottomNav(container: AppContainer) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Files", modifier = Modifier.weight(1f))
        Text(text = "Editor", modifier = Modifier.weight(1f))
        Text(text = "Preview", modifier = Modifier.weight(1f))
    }
}