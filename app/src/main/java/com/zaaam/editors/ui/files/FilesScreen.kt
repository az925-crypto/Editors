package com.zaaam.editors.ui.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.editors.core.fs.Kind
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.ui.theme.RetroTokens

@Composable
fun FilesScreen(container: AppContainer) {
    val vm: FilesViewModel = viewModel { FilesViewModel(container) }
    val state by vm.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RetroTokens.Shell)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Portfoliomu, siap di-edit.",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            color = RetroTokens.Graphite
        )
        Text(
            text = "/storage/emulated/0 — ${state.entries.size} items · cartridge tray",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = RetroTokens.Dim
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RetroTokens.Card)
                .padding(12.dp)
        ) {
            Text(text = "/storage/emulated/0", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = RetroTokens.Graphite)
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = { vm.onQueryChange(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari file di folder ini…") },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.recents.take(3).forEach { name ->
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = RetroTokens.Card)
                ) {
                    Text(text = name.substringAfterLast("/"), modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Terkini — cartridge tray", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RetroTokens.Dim)
            FilterChip(
                selected = state.showHidden,
                onClick = { vm.toggleHidden() },
                label = { Text(if (state.showHidden) "HIDDEN: ON" else "HIDDEN: OFF", fontSize = 11.sp) }
            )
        }
        if (state.permDenied) {
            Card(colors = CardDefaults.cardColors(containerColor = RetroTokens.Brick.copy(alpha = 0.12f)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Akses ditolak", fontWeight = FontWeight.Bold, color = RetroTokens.Brick)
                    Text(text = "File tetap tampil sebagai demo. Tap Pilih folder untuk akses penuh.", fontSize = 12.sp, color = RetroTokens.Dim)
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val filtered = vm.filteredEntries()
            if (filtered.isEmpty()) {
                item { Text(text = "Tidak ada yang cocok", modifier = Modifier.padding(16.dp), color = RetroTokens.Dim) }
            } else {
                items(filtered, key = { it.name }) { entry ->
                    FileRow(entry = entry, onClick = {})
                }
            }
        }
    }
}

@Composable
private fun FileRow(entry: com.zaaam.editors.core.fs.FsEntry, onClick: () -> Unit) {
    val isWeb = entry.kind == Kind.WEB
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (entry.isHidden) RetroTokens.Card.copy(alpha = 0.6f) else RetroTokens.Card)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(RetroTokens.Graphite),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (entry.kind) {
                    Kind.WEB -> "HT"
                    Kind.CODE -> "KT"
                    Kind.BINARY -> "AP"
                    else -> "FT"
                },
                color = RetroTokens.Olive,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (isWeb) {
                Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(RetroTokens.Olive).align(Alignment.TopEnd))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = entry.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RetroTokens.Graphite)
            Text(text = "${entry.size / 1000} KB · 25 Agu" + if (entry.isHidden) " ·hidden" else "", fontSize = 11.sp, color = RetroTokens.Dim, fontFamily = FontFamily.Monospace)
        }
        Text(text = if (entry.isDir) "›" else "", color = RetroTokens.Dim)
    }
}