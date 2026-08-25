package com.zaaam.editors.ui.preview

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.editors.core.preview.ConsoleEntry
import com.zaaam.editors.core.preview.PreviewWebViewFactory
import com.zaaam.editors.core.fs.isWebFile
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.ui.theme.RetroTokens

// MEDIUM FIX: holder biasa (bukan Compose State) untuk html terakhir yang benar-benar
// dimuat ke WebView — pola sama dengan JobHolder di EditorScreen. Perubahan nilainya
// murni imperatif lintas-lambda (factory & update), tidak boleh memicu invokasi ulang
// AndroidView.update{} sendiri.
private class LastLoadedHtmlHolder {
    var value: String? = null
}

private class ReloadHolder {
    var value: Int = -1
}

private const val CONSOLE_COLLAPSED_DP = 40
private const val CONSOLE_EXPANDED_MAX_DP = 320

@Composable
fun PreviewScreen(container: AppContainer) {
    val vm: PreviewViewModel = viewModel { PreviewViewModel(container) }
    val state by vm.uiState.collectAsState()
    val activeUri = container.editorSession.activeTab
    val isWeb = isWebFile(activeUri)

    // Fase 4: seed konten aktif tiap layar masuk / uri berganti (instan, tanpa debounce).
    LaunchedEffect(activeUri) { vm.showActiveFile(activeUri) }

    if (!isWeb) {
        Box(modifier = Modifier.fillMaxSize().background(RetroTokens.Shell), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Tidak ada preview", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = RetroTokens.Graphite)
                Text(text = "Buka file HTML/CSS/JS di Editor, lalu tap Preview", fontSize = 13.sp, color = RetroTokens.Dim)
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(RetroTokens.Shell)) {
        // URL bar fungsional: uri nyata tab aktif (ellipsis RTL seperti mockup — ujung nama
        // file yang terlihat) + tombol ↻ memaksa reload WebView.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RetroTokens.Card)
                    .border(1.dp, RetroTokens.Border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.url.ifBlank { "—" },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = RetroTokens.Graphite,
                        textDirection = TextDirection.Rtl,
                        textAlign = TextAlign.Left
                    )
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RetroTokens.Card)
                    .clickable { vm.refresh() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "↻", fontSize = 16.sp, color = RetroTokens.Dim)
            }
        }
        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = RetroTokens.Olive)
            Text(text = "Memperbarui…", modifier = Modifier.padding(horizontal = 14.dp).clip(RoundedCornerShape(6.dp)).background(RetroTokens.OliveWash).padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, color = RetroTokens.Olive)
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp).clip(RoundedCornerShape(12.dp)).background(Color.White)) {
            val renderedHtml = state.html.ifBlank {
                DEMO_PREVIEW_HTML
            }
            val lastLoadedHtml = remember { LastLoadedHtmlHolder() }
            val appliedReload = remember { ReloadHolder() }
            AndroidView(factory = { ctx ->
                // SECURITY MEDIUM FIX: pakai PreviewWebViewFactory yang hardened penuh
                // (allowContentAccess=false, akses file dari file URL dimatikan,
                // safeBrowsingEnabled=true); Fase 4 menambah addJavascriptInterface di dalam
                // factory SETELAH seluruh hardening + rate-limit bridge (lihat file itu).
                PreviewWebViewFactory().create(ctx) { entry -> vm.addConsole(entry) }.apply {
                    loadDataWithBaseURL(null, renderedHtml, "text/html", "utf-8", null)
                    lastLoadedHtml.value = renderedHtml
                    appliedReload.value = state.reloadSeq
                }
            }, update = { webView ->
                // PERF MEDIUM FIX: hanya muat ulang kalau html benar-benar berubah,
                // bukan tiap recomposition. Tombol ↻ menaikkan reloadSeq → holder di-reset
                // sekali → muat paksa satu kali lalu guard normal jalan lagi.
                if (appliedReload.value != state.reloadSeq) {
                    appliedReload.value = state.reloadSeq
                    lastLoadedHtml.value = null
                }
                if (renderedHtml != lastLoadedHtml.value) {
                    webView.loadDataWithBaseURL(null, renderedHtml, "text/html", "utf-8", null)
                    lastLoadedHtml.value = renderedHtml
                }
            }, modifier = Modifier.fillMaxSize())
        }
        ConsoleDrawer(state = state, onToggle = vm::toggleConsole, onClear = vm::clearConsole)
    }
}

@Composable
private fun ConsoleDrawer(
    state: PreviewUiState,
    onToggle: () -> Unit,
    onClear: () -> Unit
) {
    // Spec §9.3/§7: collapsed 40dp bar, expanded min(40% area tersedia, 320dp), transisi 220ms.
    // BoxWithConstraints mengukur ruang BENAR-BENAR tersedia (bukan tinggi layar penuh).
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val expandedHeight = minOf(maxHeight * 0.4f, CONSOLE_EXPANDED_MAX_DP.dp)
        val targetHeight = if (state.consoleExpanded) expandedHeight else CONSOLE_COLLAPSED_DP.dp
        val animatedHeight by animateDpAsState(
            targetValue = targetHeight,
            animationSpec = tween(durationMillis = 220),
            label = "consoleDrawer"
        )
        Card(
            modifier = Modifier.fillMaxWidth().height(animatedHeight),
            colors = CardDefaults.cardColors(containerColor = RetroTokens.Card),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CONSOLE_COLLAPSED_DP.dp)
                        .clickable(onClick = onToggle)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "CONSOLE", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.9.sp, color = RetroTokens.Dim)
                    Text(text = "${state.consoleEntries.size} pesan", fontSize = 11.sp, color = RetroTokens.Dim, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "Clear",
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RetroTokens.Graphite)
                            .clickable(onClick = onClear)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = RetroTokens.Shell
                    )
                }
                if (state.consoleExpanded) {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp)) {
                        // Key seq monotonik — tanpa ini, saat list tercapai cap dan bergeser
                        // seluruh row recompose tiap pesan baru (jank di low-end).
                        items(state.consoleEntries, key = { it.seq }) { entry ->
                            ConsoleEntryRow(entry)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(0.dp))
                }
            }
        }
    }
}

@Composable
private fun ConsoleEntryRow(entry: ConsoleEntry) {
    val levelColor = when (entry.level) {
        ConsoleEntry.Level.ERROR -> RetroTokens.Brick
        ConsoleEntry.Level.WARN -> RetroTokens.Olive
        else -> RetroTokens.Border
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Border-left 2dp sebagai encoder level (log hairline/warn olive/error brick).
                drawRect(color = levelColor, size = Size(width = 2.dp.toPx(), height = size.height))
            }
            .padding(start = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = entry.badge(), modifier = Modifier.size(width = 30.dp, height = 14.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = when (entry.level) {
            ConsoleEntry.Level.ERROR -> RetroTokens.Brick
            ConsoleEntry.Level.WARN -> RetroTokens.Olive
            else -> RetroTokens.Dim
        })
        Text(
            text = entry.message,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            color = RetroTokens.Graphite,
            fontFamily = FontFamily.Monospace
        )
        Text(text = entry.formattedTime(), fontSize = 10.sp, color = RetroTokens.Dim, fontFamily = FontFamily.Monospace)
    }
}

private const val DEMO_PREVIEW_HTML = """<html><head><style>body{font-family:sans-serif;background:#faf8f4;color:#17150f;padding:24px}h1{font-size:28px}button{padding:10px 18px;background:#17150f;color:#faf8f4;border:none;border-radius:8px}</style></head>
<body><h1>Halo, dari HP.<br>Ditulir di <span style="color:#e8930c">zaaam/editors</span>.</h1><p>Edit kode ini di tab Editor, lalu balik ke sini.</p><button onclick="console.log('diklik '+Date.now())">Diklik</button></body></html>"""
