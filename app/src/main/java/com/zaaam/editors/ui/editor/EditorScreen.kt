package com.zaaam.editors.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.ui.theme.RetroTokens

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
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(RetroTokens.Ink).padding(12.dp)) {
            Row(modifier = Modifier.fillMaxSize()) {
                val lines = state.content.split("\n").size
                Column(modifier = Modifier.width(36.dp), horizontalAlignment = Alignment.End) {
                    for (i in 1..lines) {
                        Text(text = "$i", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = RetroTokens.OliveDim, lineHeight = 20.sp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = state.content,
                    onValueChange = { vm.onContentChange(it) },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp, color = RetroTokens.OliveText),
                    cursorBrush = SolidColor(RetroTokens.Olive)
                )
            }
        }
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