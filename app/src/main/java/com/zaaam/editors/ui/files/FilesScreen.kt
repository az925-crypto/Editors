package com.zaaam.editors.ui.files

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.editors.core.fs.FsEntry
import com.zaaam.editors.core.fs.Kind
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.ui.components.SafDialog
import com.zaaam.editors.ui.theme.RetroTokens

// MEDIUM: SimpleDateFormat sebelumnya dibuat baru di setiap FileRow, tiap recompose — alokasi
// tiap frame. Satu instance di-reuse untuk semua row (aman karena Compose UI cuma jalan di main
// thread, jadi tidak butuh sinkronisasi lintas-thread untuk SimpleDateFormat yang not-thread-safe).
private val fileDateFormat = java.text.SimpleDateFormat("d MMM", java.util.Locale("id"))

@Composable
fun FilesScreen(container: AppContainer) {
    val vm: FilesViewModel = viewModel { FilesViewModel(container) }
    val stateHolder = vm.uiState.collectAsState()
    val state by stateHolder

    // MEDIUM: derivedStateOf supaya filter cuma dihitung ulang kalau hasilnya beneran beda,
    // bukan setiap kali FilesUiState berubah (mis. isLoading toggle) yang sebelumnya bikin
    // filteredEntries() jalan tiap recompose dan drop frame di list panjang.
    val filtered by remember { derivedStateOf { vm.filteredEntries(stateHolder.value) } }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) vm.onTreeUriSelected(uri) else vm.onPickerCancelled()
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                text = if (state.treeUri != null) "${state.pathSegments.lastOrNull() ?: "storage"} — ${state.entries.size} items" else "Belum ada folder",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = RetroTokens.Dim
            )
            Breadcrumb(
                segments = state.pathSegments,
                onSegmentClick = { vm.navigateToSegment(it) },
                onPickFolder = { picker.launch(null) }
            )
            OutlinedTextField(
                value = state.query,
                onValueChange = { vm.onQueryChange(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari file di folder ini…") },
                singleLine = true,
                enabled = state.treeUri != null
            )
            if (state.recents.isNotEmpty()) {
                Text(
                    text = "Terkini — CARTRIDGE TRAY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RetroTokens.Dim,
                    letterSpacing = 0.9.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.recents.take(3).forEach { recent ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { vm.openRecent(recent) },
                            colors = CardDefaults.cardColors(containerColor = RetroTokens.Card),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = recent.name,
                                modifier = Modifier.padding(8.dp),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "MEMORY CARDS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RetroTokens.Dim,
                    letterSpacing = 0.9.sp
                )
                FilterChip(
                    selected = state.showHidden,
                    onClick = { vm.toggleHidden() },
                    label = {
                        Text(
                            if (state.showHidden) "HIDDEN: ON" else "HIDDEN: OFF",
                            fontSize = 11.sp
                        )
                    }
                )
            }
            if (state.permDenied && !state.showSafDialog) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RetroTokens.BrickWash),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RetroTokens.Brick.copy(alpha = 0.18f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Akses ditolak",
                                fontWeight = FontWeight.Bold,
                                color = RetroTokens.Brick,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Pilih folder lagi untuk buka file.",
                                fontSize = 12.sp,
                                color = RetroTokens.Dim
                            )
                        }
                        Text(
                            text = "Pilih folder",
                            modifier = Modifier.clickable { picker.launch(null) },
                            color = RetroTokens.Olive,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            when {
                state.isLoading -> SkeletonLoader()
                state.treeUri == null && !state.hasDismissedSaf -> EmptyState(
                    text = "Belum ada folder",
                    subtext = "Pilih folder untuk mulai browse file",
                    actionText = "Pilih folder",
                    onAction = { picker.launch(null) }
                )
                state.entries.isEmpty() && state.query.isBlank() -> EmptyState(
                    text = "Folder kosong",
                    subtext = "Belum ada file di folder ini"
                )
                else -> {
                    if (filtered.isEmpty() && state.query.isNotBlank()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Tidak ada yang cocok", color = RetroTokens.Dim, fontSize = 14.sp)
                            Text(
                                text = "Coba kata kunci lain atau aktifkan HIDDEN",
                                color = RetroTokens.Dim,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.uri.toString() }) { entry ->
                                FileRow(entry = entry, onClick = { vm.openFile(entry) })
                            }
                        }
                    }
                }
            }
        }
        if (state.showSafDialog) {
            SafDialog(
                isPicking = state.isPicking,
                error = state.safError,
                onPick = { picker.launch(null) },
                onDismiss = { vm.dismissSaf() }
            )
        }
    }
}

@Composable
private fun Breadcrumb(
    segments: List<String>,
    onSegmentClick: (Int) -> Unit,
    onPickFolder: () -> Unit
) {
    if (segments.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RetroTokens.Card)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Belum ada folder · Tap Pilih folder",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = RetroTokens.Dim
            )
            Text(
                text = "Pilih folder",
                modifier = Modifier.clickable { onPickFolder() },
                color = RetroTokens.Olive,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RetroTokens.Card)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                items(segments.size) { index ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (index > 0) {
                            Text(
                                text = "›",
                                color = RetroTokens.Dim,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                        Text(
                            text = segments[index],
                            fontFamily = if (index == 0) FontFamily.Monospace else FontFamily.Default,
                            fontSize = if (index == 0) 11.sp else 13.sp,
                            fontWeight = if (index == segments.lastIndex) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (index == 0) RetroTokens.Dim else RetroTokens.Graphite,
                            modifier = Modifier.clickable { onSegmentClick(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonLoader() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(8) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(RetroTokens.Border.copy(alpha = 0.6f))
            )
        }
    }
}

@Composable
private fun EmptyState(
    text: String,
    subtext: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = RetroTokens.Graphite)
            Text(text = subtext, fontSize = 13.sp, color = RetroTokens.Dim)
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.Button(
                    onClick = onAction,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = RetroTokens.Olive,
                        contentColor = RetroTokens.Ink
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = actionText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FileRow(entry: FsEntry, onClick: () -> Unit) {
    val isWeb = entry.kind == Kind.WEB
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
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
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(RetroTokens.Olive)
                        .align(Alignment.TopEnd)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = RetroTokens.Graphite
            )
            Text(
                text = buildString {
                    append("${entry.size / 1000} KB")
                    if (entry.lastModified > 0) {
                        append(" · ${fileDateFormat.format(java.util.Date(entry.lastModified))}")
                    }
                    if (entry.isHidden) append(" ·hidden")
                },
                fontSize = 11.sp,
                color = RetroTokens.Dim,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            text = if (entry.isDir) "›" else "",
            color = RetroTokens.Dim
        )
    }
}
