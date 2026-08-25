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
    // Konten user yang mengandung penutup tag (apapun casing) bisa menutup wrapper
    // <style>/<script> lebih awal dan menyuntik markup/eksekusi di luar kendali,
    // jadi fragmen yang disuntikkan di-escape dulu dengan menyisipkan backslash.
    private val closeStyleTag = Regex("</style>", RegexOption.IGNORE_CASE)
    private val closeScriptTag = Regex("</script>", RegexOption.IGNORE_CASE)

    private fun escapeCloseTag(value: String, pola: Regex): String =
        pola.replace(value) { m -> "<\\/" + m.value.substring(2) }

    fun compose(html: String, css: String?, js: String?): String {
        var doc = html
        css?.let { doc = doc.replace("<link rel=\"stylesheet\" href=\"style.css\">", "<style>" + escapeCloseTag(it, closeStyleTag) + "</style>") }
        js?.let { doc = doc.replace("<script src=\"main.js\"></script>", "<script>window.__zaaam_bridge = {post:function(l,m){window.ZaaamBridge.postMessage(JSON.stringify({l:l,m:m}))}};" + escapeCloseTag(it, closeScriptTag) + "</script>") }
        return doc
    }
}