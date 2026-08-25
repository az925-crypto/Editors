package com.zaaam.editors.core.preview

import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.channels.Channel

class ConsoleBridge(private val channel: Channel<ConsoleEntry>) {
    @android.webkit.JavascriptInterface
    fun postMessage(json: String) {
        try {
            channel.trySend(ConsoleEntry(ConsoleEntry.Level.LOG, json, System.currentTimeMillis()))
        } catch (e: Exception) {
        }
    }
}

class PreviewWebViewFactory {
    fun create(context: android.content.Context, onConsole: (ConsoleEntry) -> Unit): WebView {
        val webView = WebView(context).apply {
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

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                }
            }
        }
        return webView
    }
}