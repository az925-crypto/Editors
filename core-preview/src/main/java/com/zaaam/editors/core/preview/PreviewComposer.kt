package com.zaaam.editors.core.preview

data class ConsoleEntry(
    val level: Level,
    val message: String,
    val epochMs: Long,
    // Seq monotonik untuk key LazyColumn yang stabil saat list tercapai cap dan bergeser
    // (tanpa key, shift index bikin seluruh row recompose tiap pesan baru).
    val seq: Long = nextConsoleEntrySeq()
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

private val consoleEntrySeq = java.util.concurrent.atomic.AtomicLong(0)
private fun nextConsoleEntrySeq(): Long = consoleEntrySeq.incrementAndGet()

object PreviewComposer {
    // Konten user yang mengandung penutup tag (apapun casing) bisa menutup wrapper
    // <style>/<script> lebih awal dan menyuntik markup/eksekusi di luar kendali,
    // jadi fragmen yang disuntikkan di-escape dulu dengan menyisipkan backslash.
    // \s* menutup varian valid per HTML parser seperti "</style >" / "</STYLE\t>"; [^>]*>
    // menutup end-tag ber-atribute ("</script foo>") yang juga valid menutup elemen.
    // \b mencegah over-match kata lain ("</scripts>" bukan penutup, tidak diescape).
    private val closeStyleTag = Regex("</\\s*style\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val closeScriptTag = Regex("</\\s*script\\b[^>]*>", RegexOption.IGNORE_CASE)

    // Placeholder replace sengaja EXACT-STRING casing-sensitive — perilaku lama yang
    // dikunci PreviewComposerTest; jangan diubah jadi regex/case-insensitive.
    private const val CSS_PLACEHOLDER = "<link rel=\"stylesheet\" href=\"style.css\">"
    private const val JS_PLACEHOLDER = "<script src=\"main.js\"></script>"

    // Pasangan nama bridge Fase 4: namespace JS `__zaaam_bridge` (didefinisikan di bawah)
    // meneruskan panggilan ke `ZaaamBridge` — nama object yang di-addJavascriptInterface
    // oleh PreviewWebViewFactory. Mengubah salah satunya = console mati diam-diam.
    internal const val BRIDGE_JS_OBJECT = "__zaaam_bridge"
    internal const val BRIDGE_INTERFACE_NAME = "ZaaamBridge"

    private fun escapeCloseTag(value: String, pola: Regex): String =
        pola.replace(value) { m -> "<\\/" + m.value.substring(2) }

    // Instrumentasi console: override log/warn/error + listener window.onerror meneruskan
    // pesan ke native lewat bridge dua-argumen polos (tanpa JSON.stringify — menghapus
    // seluruh kelas bug parsing/injection JSON di sisi Java). Native console tetap dipanggil.
    private const val CONSOLE_INSTRUMENTATION =
        "window.$BRIDGE_JS_OBJECT = {post:function(l,m){window.$BRIDGE_INTERFACE_NAME.postMessage(String(l),String(m));}};" +
            "(function(){" +
            "var send=function(l,a){var s=[],i;for(i=0;i<a.length;i++)s.push(String(a[i]));window.$BRIDGE_JS_OBJECT.post(l,s.join(\" \"));};\n" +
            "var _l=console.log,_w=console.warn,_e=console.error;\n" +
            "console.log=function(){send(\"log\",arguments);_l.apply(console,arguments);};\n" +
            "console.warn=function(){send(\"warn\",arguments);_w.apply(console,arguments);};\n" +
            "console.error=function(){send(\"error\",arguments);_e.apply(console,arguments);};\n" +
            "window.addEventListener(\"error\",function(ev){send(\"error\",[String(ev.message)]);});\n" +
            "window.$BRIDGE_JS_OBJECT.post(\"log\",\"preview siap\");\n" +
            "})();"

    fun compose(html: String, css: String?, js: String?): String {
        var doc = html
        css?.let { fragmen ->
            val wrapped = "<style>" + escapeCloseTag(fragmen, closeStyleTag) + "</style>"
            // Jalur lama: placeholder ketemu → replace exact-string (dikunci unit test).
            // Fallback baru: placeholder tidak ada → append di akhir dokumen supaya css/js
            // tab companion tetap ikut ter-preview pada HTML buatan user tanpa placeholder.
            doc = if (doc.contains(CSS_PLACEHOLDER)) doc.replace(CSS_PLACEHOLDER, wrapped)
            else doc + "\n" + wrapped
        }
        js?.let { fragmen ->
            val wrapped = "<script>" + CONSOLE_INSTRUMENTATION +
                escapeCloseTag(fragmen, closeScriptTag) + "</script>"
            doc = if (doc.contains(JS_PLACEHOLDER)) doc.replace(JS_PLACEHOLDER, wrapped)
            else doc + "\n" + wrapped
        }
        return doc
    }

    enum class StandaloneKind { CSS, JS }

    // Dokumen scaffold untuk preview file .css/.js yang dibuka langsung (tanpa index.html).
    // Sengaja HANYA berisi placeholder — satu-satunya titik injeksi konten/instrumentasi
    // adalah compose() lewat placeholder ini, sehingga user JS tereksekusi TEPAT SATU kali
    // dan instrumentasi console terpasang TEPAT SATU kali (fix review: dulu scaffold
    // menyuntik sendiri + compose append ulang = double eksekusi + log dobel).
    fun wrapStandaloneDocument(kind: StandaloneKind): String = when (kind) {
        StandaloneKind.CSS ->
            "<!DOCTYPE html><html><head>" + CSS_PLACEHOLDER + "</head><body></body></html>"
        StandaloneKind.JS ->
            "<!DOCTYPE html><html><head></head><body>" + JS_PLACEHOLDER + "</body></html>"
    }
}
