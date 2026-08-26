package com.zaaam.editors.ui.tools

import android.net.Uri
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.editors.core.fs.FsResult
import com.zaaam.editors.core.tools.BatchReplaceSummary
import com.zaaam.editors.core.tools.FileFindReport
import com.zaaam.editors.core.tools.FindReplaceEngine
import com.zaaam.editors.core.tools.MatchPreview
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.session.ToolScanPhase
import com.zaaam.editors.session.clampHighlight
import com.zaaam.editors.session.summarizeReplace
import com.zaaam.editors.ui.theme.RetroTokens
import kotlinx.coroutines.launch

// Layar Ganti Massal ala mockup: form cari/ganti + chip ABAI KAPITAL + PINDAI, hasil
// dengan highlight OliveWash, sheet konfirmasi destruktif "Tidak bisa dibatalkan",
// status per-file BERUBAH — DILEWATI dari divergence guard engine replaceVerified.
@Composable
fun FindReplaceScreen(container: AppContainer) {
    val st by container.treeScanManager.state.collectAsState()
    LaunchedEffect(Unit) { container.treeScanManager.ensureScan() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { container.treeScanManager.onTreeSelected(it) }
    }

    var query by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var ignoreCase by remember { mutableStateOf(true) }
    var scanning by remember { mutableStateOf(false) }
    var scanDone by remember { mutableStateOf(0) }
    var scanTotal by remember { mutableStateOf(0) }
    var reports by remember { mutableStateOf<List<FileFindReport>>(emptyList()) }
    // Seleksi file yang akan diganti (default: semua yang match — di-set saat scan selesai).
    var selectedUris by remember { mutableStateOf(emptySet<String>()) }
    // Hasil replace per uri setelah batch dieksekusi.
    var outcomes by remember { mutableStateOf<Map<String, BatchReplaceSummary>>(emptyMap()) }
    var confirmVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val engine = remember(container.ioDispatcher) {
        FindReplaceEngine(
            readText = { uriStr ->
                when (val r = container.fileSystem.readText(Uri.parse(uriStr))) {
                    is FsResult.Success -> r.value
                    is FsResult.Error -> null // guard teks/biner → dilaporkan sebagai skip
                }
            },
            writeText = { uriStr, text ->
                when (val r = container.fileSystem.writeText(Uri.parse(uriStr), text)) {
                    is FsResult.Success -> true
                    is FsResult.Error -> false
                }
            },
            ioDispatcher = container.ioDispatcher
        )
    }

    fun runScan() {
        val candidates = st.result?.files ?: return
        scope.launch {
            scanning = true
            outcomes = emptyMap()
            try {
                val result = engine.scan(
                    candidates = candidates,
                    query = query,
                    ignoreCase = ignoreCase,
                    onProgress = { done, total ->
                        scanDone = done
                        scanTotal = total
                    }
                )
                reports = result
                selectedUris = result.filter { (it.outcome?.totalMatches ?: 0) > 0 }.map { it.node.uri }.toSet()
            } finally {
                scanning = false
            }
        }
    }

    fun runReplace() {
        // Kontrak reviewer: jangan pernah menulis file dengan query kosong (write percuma).
        if (query.isBlank() || replacement.isEmpty()) return
        // Snapshot fase-scan dibaca SEKALI per file lalu engine verifikasi ulang
        // (replaceVerified re-read + compare) — interaksi autosave tercakup di sana.
        val targets = reports.filter {
            (it.outcome?.totalMatches ?: 0) > 0 && it.node.uri in selectedUris && it.node.uri !in outcomes.keys
        }
        if (targets.isEmpty()) return
        scope.launch {
            val running = mutableMapOf<String, BatchReplaceSummary>()
            for (report in targets) {
                val snapshot = when (val r = container.fileSystem.readText(Uri.parse(report.node.uri))) {
                    is FsResult.Success -> r.value
                    is FsResult.Error -> null
                }
                val summary = if (snapshot == null) {
                    BatchReplaceSummary.Failed
                } else {
                    engine.replaceVerified(report.node.uri, snapshot, query, replacement, ignoreCase)
                }
                running[report.node.uri] = summary
                outcomes = running.toMap()
            }
        }
    }

    val matched = reports.filter { (it.outcome?.totalMatches ?: 0) > 0 }
    val pending = matched.filter { it.node.uri in selectedUris && it.node.uri !in outcomes.keys }
    val pendingLocations = pending.sumOf { it.outcome!!.totalMatches }
    val totals = summarizeReplace(outcomes.values.toList())

    Column(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 14.dp, end = 14.dp)) {
            ToolsHeroCard(
                kicker = "ALAT / GANTI MASSAL",
                title = "Ganti Massal",
                subtitle = "file teks \u2264 2 MB \u00b7 guard biner otomatis"
            )
            ToolsField(query, { query = it }, hint = "Cari\u2026", modifier = Modifier.padding(top = 10.dp))
            ToolsField(replacement, { replacement = it }, hint = "Ganti dengan\u2026", modifier = Modifier.padding(top = 8.dp))
            Row(
                Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolsChip("ABAI KAPITAL", ignoreCase, onClick = { ignoreCase = !ignoreCase })
                Box(
                    Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (query.isNotBlank() && st.phase == ToolScanPhase.DONE && !scanning) RetroTokens.Olive else RetroTokens.OlivePress)
                        .clickable(enabled = query.isNotBlank() && st.phase == ToolScanPhase.DONE && !scanning) { runScan() }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("PINDAI", color = RetroTokens.LcdBg, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
                if (scanning) {
                    Text(
                        "Memindai\u2026 ${if (scanTotal > 0) "$scanDone/$scanTotal" else ""}",
                        color = RetroTokens.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                val label = buildString {
                    append("HASIL PINDAI")
                    if (matched.isNotEmpty()) append(" \u00b7 ${matched.size} FILE \u00b7 $pendingLocations LOKASI")
                    if (totals.changedSkipped > 0 || totals.failed > 0) {
                        append(" \u00b7 ${totals.changedSkipped} BERUBAH \u00b7 ${totals.failed} GAGAL")
                    }
                }
                ToolsSectionLabel(label)
            }
            itemsIndexed(matched, key = { _, r -> r.node.uri }) { _, report ->
                val oc = report.outcome!! // property lintas module — smart cast tak berlaku
                ReplaceReportRow(
                    name = report.node.name,
                    dir = report.node.relPath.substringBeforeLast("/", missingDelimiterValue = "(akar)"),
                    matchCount = oc.totalMatches,
                    previews = oc.previews,
                    after = outcomes[report.node.uri],
                    checked = report.node.uri in selectedUris,
                    onCheck = { checked ->
                        selectedUris = if (checked) selectedUris + report.node.uri else selectedUris - report.node.uri
                    }
                )
            }
            if (!reports.isEmpty() && matched.isEmpty() && !scanning) {
                item { ToolsNote("Tidak ada lokasi yang cocok untuk query ini.") }
            }
            if (reports.isEmpty() && st.rootUri != null && !scanning) {
                item {
                    ToolsNote("Ketik query lalu tekan PINDAI untuk memindai seluruh file teks di folder aktif.")
                }
            }
            if (st.rootUri == null) {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("(pilih folder untuk mulai)", color = RetroTokens.DimBone, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        ToolsPrimaryButton("PILIH FOLDER", { picker.launch(null) }, Modifier.padding(top = 12.dp))
                    }
                }
            }
        }

        // Kontrak reviewer: disabled kalau query/replacement kosong / tidak ada target pending.
        ToolsPrimaryButton(
            text = "GANTI DI ${pending.size} FILE ($pendingLocations LOKASI)",
            onClick = { confirmVisible = true },
            enabled = query.isNotBlank() && replacement.isNotEmpty() && pending.isNotEmpty(),
            modifier = Modifier
                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                .fillMaxWidth()
        )
    }

    ToolsSheet(
        visible = confirmVisible,
        title = "Ganti di ${pending.size} file ($pendingLocations lokasi)?",
        body = "Tidak bisa dibatalkan. Setiap file diverifikasi ulang sebelum ditulis.",
        confirmLabel = "GANTI SEKARANG",
        dismissLabel = "BATAL",
        onConfirm = {
            confirmVisible = false
            runReplace()
        },
        onDismiss = { confirmVisible = false }
    )
}

@Composable
private fun ReplaceReportRow(
    name: String,
    dir: String,
    matchCount: Int,
    previews: List<MatchPreview>,
    after: BatchReplaceSummary?,
    checked: Boolean,
    onCheck: (Boolean) -> Unit
) {
    val dimmed = after is BatchReplaceSummary.ChangedSkipped || after == BatchReplaceSummary.Failed
    Column(
        Modifier
            .fillMaxWidth()
            .alpha(if (dimmed) 0.72f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .background(RetroTokens.Card)
            .border(1.dp, RetroTokens.Border, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolsCheckSquare(checked, onToggle = { onCheck(!checked) })
            Column(Modifier.weight(1f)) {
                Text(name, color = RetroTokens.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("$matchCount match \u00b7 $dir", color = RetroTokens.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
            }
            when (after) {
                BatchReplaceSummary.ChangedSkipped -> Text(
                    "BERUBAH \u2014 DILEWATI",
                    color = RetroTokens.Brick, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold
                )
                BatchReplaceSummary.Failed -> Text(
                    "GAGAL DITULIS",
                    color = RetroTokens.Brick, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold
                )
                is BatchReplaceSummary.Success -> Text(
                    "\u2713 ${after.replacedCount} terganti",
                    color = RetroTokens.LedGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold
                )
                null -> Unit
            }
        }
        if (previews.isNotEmpty() && after == null) {
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RetroTokens.Border)
            )
            previews.take(3).forEach { p ->
                PreviewLine(p, Modifier.padding(top = 6.dp))
            }
        }
    }
}

// Baris preview mono dengan highlight OliveWash pada lokasi match (clamp kontrak reviewer:
// preview multi-baris bisa bawa koordinat melebihi lineText — jangan substring-crash).
@Composable
private fun PreviewLine(p: MatchPreview, modifier: Modifier = Modifier) {
    val (s, e) = clampHighlight(p.startInLine, p.endInLine, p.lineText.length)
    val annotated = buildAnnotatedString {
        append(p.lineText.substring(0, s))
        if (e > s) withStyle(SpanStyle(background = RetroTokens.OliveWash, fontWeight = FontWeight.Bold)) {
            append(p.lineText.substring(s, e))
        }
        append(p.lineText.substring(e))
    }
    Row(modifier) {
        Text(
            "${p.lineNumber}",
            color = RetroTokens.DimBone, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(annotated, color = RetroTokens.Muted, fontSize = 11.5.sp, fontFamily = FontFamily.Monospace, maxLines = 2)
    }
}
