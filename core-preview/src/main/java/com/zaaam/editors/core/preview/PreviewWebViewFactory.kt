package com.zaaam.editors.core.preview

import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class ConsoleBridge(private val channel: Channel<ConsoleEntry>) {
    @android.webkit.JavascriptInterface
    fun postMessage(json: String) {
        try {
            val obj = kotlinx.serialization.json.Json.decodeFromString<ConsoleMessage>(json)
            channel.trySend(ConsoleEntry(obj.level, obj.message, System.currentTimeMillis()))
        } catch (e: Exception) {
            // ignore malformed
        }
    }
}

private data class ConsoleMessage(
    val level: ConsoleEntry.Level,
    val message: String
)

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
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                }
            }
        }
        return webView
    }
}