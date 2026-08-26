package com.zaaam.editors.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.zaaam.editors.core.preview.ConsoleEntry
import com.zaaam.editors.core.preview.PreviewWebViewFactory

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

// Panel renderer WebView bersama untuk layar Preview penuh dan pane split di Editor.
//
// WHY blank handling tetap di CALLER: layar Preview butuh fallback DEMO_PREVIEW_HTML,
// sedangkan pane split butuh box teks Indonesia TANPA WebView sama sekali. Panel ini
// sengaja dumb-renderer: menerima html non-blank, tugasnya hanya memuat + guard reload.
@Composable
fun PreviewWebViewPanel(
    renderedHtml: String,
    reloadSeq: Int,
    onConsole: (ConsoleEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val lastLoadedHtml = remember { LastLoadedHtmlHolder() }
    val appliedReload = remember { ReloadHolder() }
    AndroidView(
        factory = { ctx ->
            // SECURITY: satu-satunya titik pembuatan WebView preview — hardening penuh
            // (akses file/content mati, safe browsing, semua navigasi diblok) dan bridge
            // console baru dipasang SETELAH hardening, di dalam PreviewWebViewFactory.
            PreviewWebViewFactory().create(ctx) { entry -> onConsole(entry) }.apply {
                loadDataWithBaseURL(null, renderedHtml, "text/html", "utf-8", null)
                lastLoadedHtml.value = renderedHtml
                appliedReload.value = reloadSeq
            }
        },
        update = { webView ->
            // PERF: hanya muat ulang kalau html benar-benar berubah, bukan tiap recomposition.
            // reloadSeq naik = tombol ↻ memaksa muat ulang sekali (holder di-null-kan lalu
            // guard normal jalan lagi).
            if (appliedReload.value != reloadSeq) {
                appliedReload.value = reloadSeq
                lastLoadedHtml.value = null
            }
            if (renderedHtml != lastLoadedHtml.value) {
                webView.loadDataWithBaseURL(null, renderedHtml, "text/html", "utf-8", null)
                lastLoadedHtml.value = renderedHtml
            }
        },
        onRelease = { webView ->
            // FIX leak pre-existing: selama ini WebView preview tidak pernah dihancurkan saat
            // keluar komposisi (pindah tab bottom-nav) — stopLoading + destroy membebaskan
            // native layer & referensi bridge.
            webView.stopLoading()
            webView.destroy()
        },
        modifier = modifier
    )
}
