package com.zaaam.editors.ui.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.editors.core.editor.TabState
import com.zaaam.editors.core.fs.FsResult
import com.zaaam.editors.core.tools.DuplicateGroup
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.session.ToolScanPhase
import com.zaaam.editors.session.humanBytes
import com.zaaam.editors.ui.theme.RetroTokens
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MSG_OPEN_FAILED = "Gagal membuka file"

// Layar Cari Duplikat ala mockup: chip SERTAKAN HIDDEN + PINDAI ULANG, grup expandable
// dengan checkbox TANPA aksi hapus (keputusan user v0.2: cari + buka di editor saja).
@Composable
fun DuplicatesScreen(container: AppContainer) {
    val st by container.treeScanManager.state.collectAsState()
    LaunchedEffect(Unit) { container.treeScanManager.ensureScan() }

    // Hash duplikat jalan sekali per hasil walk baru — bukan tiap masuk tab.
    LaunchedEffect(st.result) {
        if (st.phase == ToolScanPhase.DONE && st.dupes.phase == ToolScanPhase.IDLE) {
            container.treeScanManager.runDupes()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { container.treeScanManager.onTreeSelected(it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 14.dp, end = 14.dp)) {
            ToolsHeroCard(
                kicker = "ALAT / CARI DUPLIKAT",
                title = "Cari Duplikat",
                subtitle = st.rootLabel.ifBlank { "belum ada folder" }
            )
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolsChip("SERTAKAN HIDDEN", st.includeHidden, onClick = { container.treeScanManager.toggleHidden() })
                Box(
                    Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (st.rootUri != null && st.phase == ToolScanPhase.DONE) RetroTokens.Olive else RetroTokens.OlivePress)
                        .clickable(enabled = st.rootUri != null && st.phase == ToolScanPhase.DONE && st.result != null) {
                            container.treeScanManager.runDupes()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("PINDAI ULANG", color = RetroTokens.LcdBg, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 14.dp))
                }
            }
            when {
                st.phase == ToolScanPhase.SCANNING -> Text(
                    "Memindai tree\u2026",
                    color = RetroTokens.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp)
                )
                st.dupes.phase == ToolScanPhase.SCANNING -> Text(
                    "Menghitung hash\u2026 ${st.dupes.progressDone}/${st.dupes.progressTotal}",
                    color = RetroTokens.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp)
                )
                st.error != null || st.dupes.error != null -> Text(
                    st.error ?: st.dupes.error ?: "",
                    color = RetroTokens.Brick, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        when {
            st.rootUri == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("(pilih folder untuk mulai)", color = RetroTokens.DimBone, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        ToolsPrimaryButton("PILIH FOLDER", { picker.launch(null) }, Modifier.padding(top = 16.dp))
                    }
                }
            }
            else -> DupesBody(
                container,
                st.dupes.outcome?.groups ?: emptyList(),
                changedDuringScan = st.dupes.outcome?.stats?.changedDuringScan ?: 0
            )
        }
    }
}

@Composable
private fun DupesBody(container: AppContainer, groups: List<DuplicateGroup>, changedDuringScan: Int) {
    // Grup expandable: key pakai uri node pertama (stabil antar-recompose).
    val expanded = remember { mutableStateOf(emptySet<String>()) }
    val scope = rememberCoroutineScope()
    var openError by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(groups, key = { _, g -> g.nodes.firstOrNull()?.uri ?: g.hashCode().toString() }) { _, group ->
            val key = group.nodes.firstOrNull()?.uri ?: return@itemsIndexed
            var checked by remember(key) { mutableStateOf(true) }
            val isOpen = key in expanded.value
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(RetroTokens.Card)
                    .border(1.dp, RetroTokens.Border, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ToolsStencil("\u00d7${group.nodes.size}", RetroTokens.Olive)
                    Column(Modifier.weight(1f)) {
                        Text(
                            group.nodes.firstOrNull()?.name ?: "?",
                            color = RetroTokens.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
                        )
                        Text(
                            "${group.nodes.size} file \u00b7 ${humanBytes(group.sizeBytes)} tiap salinan",
                            color = RetroTokens.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace
                        )
                    }
                    // Checkbox grup kosmetik — v0.2 TIDAK ada aksi hapus.
                    ToolsCheckSquare(checked, onToggle = { checked = !checked })
                }
                if (isOpen) {
                    Column(Modifier.padding(top = 10.dp)) {
                        group.nodes.forEach { node ->
                            val dir = node.relPath.substringBeforeLast("/", missingDelimiterValue = "(akar)")
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch { openInEditor(container, node.uri, node.name, onFail = { openError = true }) }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(node.name, color = RetroTokens.Ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(dir, color = RetroTokens.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                                }
                                Text("BUKA \u203a", color = RetroTokens.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clickable {
                            expanded.value = if (isOpen) expanded.value - key else expanded.value + key
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isOpen) "\u25b4 tutup" else "\u25be buka", color = RetroTokens.DimBone, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            if (groups.isEmpty()) {
                ToolsNote("Tidak ada grup duplikat yang terdeteksi di folder ini.")
            } else {
                val wasted = groups.sumOf { it.sizeBytes * (it.nodes.size - 1) }
                ToolsNote(
                    "${groups.size} grup \u00b7 total ${humanBytes(wasted)} boros \u00b7 file yang berubah saat pindai otomatis dikecualikan." +
                        if (changedDuringScan > 0) " ($changedDuringScan file dikecualikan)" else "",
                    solid = true
                )
            }
        }
        if (openError) {
            item {
                ToolsBannerBrick(MSG_OPEN_FAILED, "File tidak bisa dibuka sebagai teks di editor.")
            }
        }
    }
}

// Entry "BUKA" duplikat → editor teks (kontrak tulis editorContents SEBELUM addTab).
// Duplikasi kecil dari FilesViewModel.openFile; refaktor gabung = backlog.
internal suspend fun openInEditor(container: AppContainer, uriStr: String, name: String, onFail: () -> Unit) {
    val result = withContext(container.ioDispatcher) {
        container.fileSystem.readText(android.net.Uri.parse(uriStr))
    }
    when (result) {
        is FsResult.Success -> {
            container.editorContents[uriStr] = result.value
            container.editorSession.addTab(TabState(uriStr, name))
            container.screenState.value = com.zaaam.editors.session.AppScreen.EDITOR
        }
        is FsResult.Error -> onFail()
    }
}
