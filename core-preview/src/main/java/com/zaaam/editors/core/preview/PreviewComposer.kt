package com.zaaam.editors.core.preview

data class ConsoleEntry(
    val level: Level,
    val message: String,
    val epochMs: Long
) {
    fun formattedTime(): String {
        val d = java.util.Date(epochMs)
        return java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(d)
    }

    fun badge(): String = when (level) {
        Level.ERROR -> "ERR"
        Level.WARN -> "WARN"
        else -> "LOG"
    }

    enum class Level {
        LOG, WARN, ERROR
    }
}

object PreviewComposer {
    fun compose(html: String, css: String?, js: String?): String {
        var doc = html
        css?.let { doc = doc.replace("<link rel=\"stylesheet\" href=\"style.css\">", "<style>" + it + "</style>") }
        js?.let { doc = doc.replace("<script src=\"main.js\"></script>", "<script>window.__zaaam_bridge = {post:function(l,m){window.ZaaamBridge.postMessage(JSON.stringify({l:l,m:m}))}};" + it + "</script>") }
        return doc
    }
}