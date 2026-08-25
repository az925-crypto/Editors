package com.zaaam.editors.core.preview

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

// --- Bagian pure (internal top-level, tanpa android.*) agar bisa di-unit-test JVM murni. ---

// Batas panjang satu pesan console dari konten user — pesan lebih panjang dipotong,
// mencegah satu console.error raksasa memakan memori UI.
internal const val MAX_CONSOLE_MESSAGE_CHARS = 500

internal fun resolveConsoleLevel(raw: String?): ConsoleEntry.Level = when (raw?.lowercase()) {
    "warn" -> ConsoleEntry.Level.WARN
    "error" -> ConsoleEntry.Level.ERROR
    else -> ConsoleEntry.Level.LOG
}

internal fun truncateConsoleMessage(raw: String?, maxChars: Int = MAX_CONSOLE_MESSAGE_CHARS): String {
    val value = raw ?: return ""
    return if (value.length <= maxChars) value else value.substring(0, maxChars)
}

// Rate limiter sliding-window: maksimum `maxMessages` dalam jendela `windowMs`;
// kelebihan DIBUANG diam-diam. WAJIB sebelum konten user dipercaya menulis ke console UI
// (script user boleh loop console.log 1 juta kali — tanpa ini = memory/flood DoS ke UI).
internal class ConsoleRateLimiter(
    private val maxMessages: Int,
    private val windowMs: Long,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val stamps = ArrayDeque<Long>()

    @Synchronized
    fun tryAcquire(): Boolean {
        val t = now()
        while (stamps.isNotEmpty() && t - stamps.first() >= windowMs) stamps.removeFirst()
        if (stamps.size >= maxMessages) return false
        stamps.addLast(t)
        return true
    }
}

// Bridge JS→native. Kontrak dua-argumen polos (level, message) — TANPA JSON di sisi Java;
// sisi JS yang mengirim ada di PreviewComposer.CONSOLE_INSTRUMENTATION. Nama interface
// object saat addJavascriptInterface WAJIB PreviewComposer.BRIDGE_INTERFACE_NAME.
class ConsoleBridge(private val onEntry: (ConsoleEntry) -> Unit) {

    // 30 pesan/detik: jauh di atas kebutuhan debugging wajar, cukup rendah untuk membatasi banjir.
    private val limiter = ConsoleRateLimiter(maxMessages = 30, windowMs = 1000)

    @JavascriptInterface
    fun postMessage(level: String?, message: String?) {
        if (!limiter.tryAcquire()) return
        val text = truncateConsoleMessage(message)
        if (text.isEmpty()) return
        onEntry(ConsoleEntry(resolveConsoleLevel(level), text, System.currentTimeMillis()))
    }
}

class PreviewWebViewFactory {
    fun create(context: Context, onConsole: (ConsoleEntry) -> Unit): WebView {
        val bridge = ConsoleBridge(onConsole)
        return WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                safeBrowsingEnabled = true
            }
            webViewClient = object : WebViewClient() {
                // SECURITY (hardening): blokir semua navigasi keluar dari konten preview
                // (klik link, iframe, scheme intent:// dsb) — konten preview opaque-origin,
                // navigasi tidak pernah diizinkan. Tidak dipanggil untuk loadDataWithBaseURL awal.
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = true
            }
            // ORIGIN RULE Fase 4: bridge hanya terpasang SETELAH seluruh hardening di atas,
            // dan karena semua navigasi diblok total + konten hanya pernah dimuat via
            // loadDataWithBaseURL(null, …) milik compose kita sendiri, satu-satunya pihak
            // yang bisa memanggil bridge adalah dokumen hasil PreviewComposer (kode user
            // sendiri). Ditambah rate-limit + truncation di ConsoleBridge, permukaan serangan
            // bridge tertutup untuk kasus offline-first ini.
            addJavascriptInterface(bridge, PreviewComposer.BRIDGE_INTERFACE_NAME)
        }
    }
}
