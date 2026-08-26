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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.editors.core.tools.aggregateAnalysis
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.session.ToolScanPhase
import com.zaaam.editors.session.humanBytes
import com.zaaam.editors.ui.theme.RetroTokens

// Layar Analisa Penyimpanan ala mockup: readout total tree, folder terbesar (bar),
// file terbesar (kartu), catatan folder yang dilewati. Data dari cache TreeScanManager.
@Composable
fun AnalyzerScreen(container: AppContainer) {
    val st by container.treeScanManager.state.collectAsState()
    LaunchedEffect(Unit) { container.treeScanManager.ensureScan() }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { container.treeScanManager.onTreeSelected(it) } }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 14.dp, end = 14.dp)) {
            ToolsHeroCard(
                kicker = "ALAT / ANALISA PENYIMPANAN",
                title = "Analisa Penyimpanan",
                subtitle = st.rootLabel.ifBlank { "belum ada folder" }
            )
            if (st.phase == ToolScanPhase.SCANNING) {
                // Kontrak engine: progressTotal bisa 0 saat root gagal → jangan bagi nol.
                ToolsProgressBar(st.progressDone, st.progressTotal, Modifier.padding(top = 10.dp))
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolsChip("SERTAKAN HIDDEN", st.includeHidden, onClick = { container.treeScanManager.toggleHidden() })
                Box(
                    modifier = Modifier
                        .padding(top = 0.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (st.rootUri != null && st.phase != ToolScanPhase.SCANNING) RetroTokens.Olive else RetroTokens.OlivePress)
                        .clickable(enabled = st.rootUri != null && st.phase != ToolScanPhase.SCANNING) {
                            container.treeScanManager.startScan()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("PINDAI ULANG", color = RetroTokens.LcdBg, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 14.dp))
                }
            }
            if (st.error != null) {
                Text(
                    text = st.error ?: "",
                    color = RetroTokens.Brick,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
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
            st.phase == ToolScanPhase.FAILED && st.result == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Pindai gagal \u2014 coba PINDAI ULANG.", color = RetroTokens.Muted, fontSize = 13.sp)
                }
            }
            st.result != null -> AnalysisBody(st.result!!, container)
        }
    }
}

@Composable
private fun AnalysisBody(result: com.zaaam.editors.core.tools.TreeScanResult, container: AppContainer) {
    // aggregateAnalysis murni & murah (satu pass atas hasil walk yang sudah di-cache).
    val report = remember(result) {
        aggregateAnalysis(result.files, result.dirs, result.stats.skippedDirs)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(RetroTokens.Card)
                    .border(1.dp, RetroTokens.Border, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(humanBytes(report.totalBytes), fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = RetroTokens.Graphite)
                    Text(
                        "${report.fileCount} file \u00b7 ${report.folderCount} folder" +
                            if (report.skippedDirs > 0) " \u00b7 ${report.skippedDirs} dilewati" else "",
                        color = RetroTokens.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace
                    )
                }
                ToolsChip("SELESAI", selected = true, onClick = {})
            }
        }
        if (report.fileCount > 0) {
            item { ToolsSectionLabel("FOLDER TERBESAR") }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(RetroTokens.Card)
                        .border(1.dp, RetroTokens.Border, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val max = report.largestDirs.maxOfOrNull { it.bytes }?.coerceAtLeast(1L) ?: 1L
                    report.largestDirs.forEach { dir ->
                        ToolsBarRow(dir.label, humanBytes(dir.bytes), dir.bytes.toFloat() / max)
                    }
                }
            }
            item { ToolsSectionLabel("FILE TERBESAR") }
            items(report.largestFiles, key = { it.uri }) { entry ->
                val stencil = remember(entry.name) { container.fileKindResolver.stencilLabel(entry.name) }
                val parent = entry.relPath.substringBeforeLast("/", missingDelimiterValue = "(akar)")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(RetroTokens.Card)
                        .border(1.dp, RetroTokens.Border, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ToolsStencil(stencil, RetroTokens.Olive)
                    Column(Modifier.weight(1f)) {
                        Text(entry.name, color = RetroTokens.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text("$parent \u00b7 ${humanBytes(entry.bytes)}", color = RetroTokens.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                    }
                }
            }
        } else {
            item { ToolsNote("Folder kosong atau semua file tersembunyi difilter.") }
        }
        if (report.skippedDirs > 0) {
            item {
                ToolsNote("${report.skippedDirs} folder tidak bisa dibaca (izin provider) \u2014 dilewati saat pindaian.")
            }
        }
    }
}
