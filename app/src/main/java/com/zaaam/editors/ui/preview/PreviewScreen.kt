package com.zaaam.editors.ui.preview

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.editors.core.preview.ConsoleEntry
import com.zaaam.editors.core.preview.PreviewWebViewFactory
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.ui.theme.RetroTokens

// MEDIUM FIX: holder biasa (bukan Compose State) untuk html terakhir yang benar-benar
// dimuat ke WebView — pola sama dengan JobHolder di EditorScreen. Perubahan nilainya
// murni imperatif lintas-lambda (factory & update), tidak boleh memicu invokasi ulang
// AndroidView.update{} sendiri.
private class LastLoadedHtmlHolder {
    var value: String? = null
}

@Composable
fun PreviewScreen(container: AppContainer) {
    val vm: PreviewViewModel = viewModel { PreviewViewModel(container) }
    val state by vm.uiState.collectAsState()
    val activeUri = container.editorSession.activeTab
    val isWeb = vm.isWebFile(activeUri)

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
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = state.url,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(RetroTokens.Card).padding(8.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = RetroTokens.Graphite
            )
            Text(text = "↻", modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(RetroTokens.Card).clickable { }.padding(8.dp))
        }
        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = RetroTokens.Olive)
            Text(text = "Memperbarui…", modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(6.dp)).background(RetroTokens.Olive.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, color = RetroTokens.Olive)
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(12.dp)).background(androidx.compose.ui.graphics.Color.White)) {
            val html = state.html.ifBlank {
                """
                <html><head><style>body{font-family:sans-serif;background:#faf8f4;color:#17150f;padding:24px}h1{font-size:28px}button{padding:10px 18px;background:#17150f;color:#faf8f4;border:none;border-radius:8px}</style></head>
                <body><h1>Halo, dari HP.<br>Ditulir di <span style="color:#e8930c">zaaam/editors</span>.</h1><p>Edit kode ini di tab Editor, lalu balik ke sini.</p><button onclick="console.log('diklik '+Date.now())">Diklik</button></body></html>
                """.trimIndent()
            }
            val lastLoadedHtml = remember { LastLoadedHtmlHolder() }
            AndroidView(factory = { ctx ->
                // SECURITY MEDIUM FIX: pakai PreviewWebViewFactory yang hardened penuh
                // (allowContentAccess=false, akses file dari file URL dimatikan,
                // safeBrowsingEnabled=true) alih-alih WebView manual hardening minimal.
                // addJavascriptInterface sengaja belum ditambah — reserved Fase 4.
                PreviewWebViewFactory().create(ctx) { entry -> vm.addConsole(entry) }.apply {
                    loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                    // Catat html awal supaya recomposition pertama tidak me-reload dua kali.
                    lastLoadedHtml.value = html
                }
            }, update = { webView ->
                // PERF MEDIUM FIX: hanya muat ulang kalau html benar-benar berubah,
                // bukan tiap recomposition (state lain seperti isLoading/consoleEntries
                // juga memicu update{}).
                if (html != lastLoadedHtml.value) {
                    webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                    lastLoadedHtml.value = html
                }
            }, modifier = Modifier.fillMaxSize())
        }
        Card(
            modifier = Modifier.fillMaxWidth().clickable { vm.toggleConsole() },
            colors = CardDefaults.cardColors(containerColor = RetroTokens.Card),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "CONSOLE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RetroTokens.Dim)
                Text(text = "${state.consoleEntries.size} pesan", fontSize = 11.sp, color = RetroTokens.Dim, fontFamily = FontFamily.Monospace)
                Text(text = "Clear", modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(RetroTokens.Graphite).clickable { vm.clearConsole() }.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, color = RetroTokens.Shell)
            }
            if (state.consoleExpanded) {
                LazyColumn(modifier = Modifier.height(120.dp).padding(horizontal = 8.dp)) {
                    items(state.consoleEntries) { entry ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = entry.badge(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = when (entry.level) { ConsoleEntry.Level.ERROR -> RetroTokens.Brick; ConsoleEntry.Level.WARN -> RetroTokens.Olive; else -> RetroTokens.Dim })
                            Text(text = entry.message, fontSize = 12.sp, color = RetroTokens.Graphite, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}