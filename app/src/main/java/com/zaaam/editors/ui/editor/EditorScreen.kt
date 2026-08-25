package com.zaaam.editors.ui.editor

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.editors.core.editor.EditorEngine
import com.zaaam.editors.core.editor.LanguageResolver
import com.zaaam.editors.core.editor.SoraThemeMapper
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.ui.theme.RetroTokens
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme

@Composable
fun EditorScreen(container: AppContainer) {
    val vm: EditorViewModel = viewModel { EditorViewModel(container) }
    val state by vm.uiState.collectAsState()

    if (state.tabs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(RetroTokens.Shell), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Tidak ada file terbuka", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = RetroTokens.Graphite)
                Text(text = "Buka file dari tab Files untuk mulai mengedit.", fontSize = 13.sp, color = RetroTokens.Dim)
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(RetroTokens.Shell)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.tabs.forEach { tab ->
                val isActive = tab.uri == state.activeUri
                Card(
                    modifier = Modifier.clickable { vm.switchTab(tab.uri) },
                    colors = CardDefaults.cardColors(containerColor = if (isActive) RetroTokens.Card else RetroTokens.Shell),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (tab.dirty) Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(RetroTokens.LedOrange))
                        Text(text = tab.displayName, fontSize = 13.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = RetroTokens.Graphite)
                        Text(text = "×", modifier = Modifier.clickable { vm.closeTab(tab.uri) }.padding(4.dp), color = RetroTokens.Dim)
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Find", modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(RetroTokens.Card).padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, color = RetroTokens.Dim)
            if (vm.isWebFile(state.activeUri)) {
                Text(text = "Preview ▶", modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(RetroTokens.Olive).padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, color = RetroTokens.Ink, fontWeight = FontWeight.Bold)
            }
        }
        val activeUri = state.activeUri
        val content = state.content
        val languageResolver = remember { LanguageResolver() }
        val themeMapper = remember { SoraThemeMapper() }
        val appliedScope = remember { mutableStateOf<String?>(null) }

        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(RetroTokens.LcdBg),
            factory = { ctx ->
                val editor = EditorEngine.create(ctx)
                editor.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                try {
                    EditorEngine.initTextMate(ctx)
                    val scheme: TextMateColorScheme = EditorEngine.createColorScheme()
                    themeMapper.applyChromeOverrides(scheme)
                    editor.setColorScheme(scheme)
                } catch (_: Exception) {
                }
                editor.subscribeEvent(ContentChangeEvent::class.java) { event, _ ->
                    if (event.action != ContentChangeEvent.ACTION_SET_NEW_TEXT) {
                        vm.onContentChange(editor.text.toString())
                    }
                }
                editor
            },
            update = { editor ->
                if (editor.text?.toString() != content) {
                    editor.setText(content)
                }
                if (activeUri != null && appliedScope.value != activeUri) {
                    appliedScope.value = activeUri
                    try {
                        val scope = languageResolver.resolve(activeUri)
                        editor.setEditorLanguage(TextMateLanguage.create(scope, true))
                    } catch (_: Exception) {
                    }
                }
            }
        )

        if (state.saveStatus != SaveStatus.Idle) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(when (state.saveStatus) { is SaveStatus.Saving -> RetroTokens.LedOrange; is SaveStatus.Saved -> RetroTokens.LedGreen; else -> Color.Transparent }))
                Text(
                    text = when (val s = state.saveStatus) { is SaveStatus.Saving -> "Menyimpan…"; is SaveStatus.Saved -> "Tersimpan ${s.time}"; else -> "" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RetroTokens.Dim
                )
            }
        }
    }
}
