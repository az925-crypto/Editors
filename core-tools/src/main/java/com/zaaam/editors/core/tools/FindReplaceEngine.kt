package com.zaaam.editors.core.tools

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Pencarian LITERAL murni (loop indexOf — sengaja BUKAN Regex, supaya query berisi
// metacharacter tidak bikin perilaku aneh). Non-overlapping, maju sepanjang panjang match.
// ignoreCase lewat indexOf(ignoreCase=true) (regionMatches per-char), BUKAN lowercase():
// lowercase bisa mengubah panjang string ('İ' → "i̇" 2 char) sehingga indeks match
// lepas dari text asli → crash saat splice / teks korup. regionMatches membandingkan
// window sepanjang query DI text asli — indeks selalu valid, splice selalu aman.
// Trade-off (disengaja): bukan full Unicode case-folding ('ß' tak match 'ss') —
// tapi 'İ'↔'i' tetap cocok via simple lowercase mapping (terverifikasi CI).
internal fun findMatches(text: String, query: String, ignoreCase: Boolean, maxPreviews: Int): FindOutcome {
    if (query.isEmpty()) return FindOutcome(0, emptyList())
    var total = 0
    val previews = mutableListOf<MatchPreview>()
    // Kursor baris berjalan: maju SEKALI melintasi teks (amortized O(n)) — dulu tiap
    // preview rescan dari index 0 dan substring SATU BARIS PENUH; file minified 2MB
    // satu-baris × 50 preview = ~100MB heap + ratusan juta iterasi.
    var curLine = 0 // 0-based
    var curLineStart = 0
    var scannedTo = 0
    fun advance(to: Int) {
        if (to <= scannedTo) return
        for (i in scannedTo until to) {
            if (text[i] == '\n') {
                curLine++
                curLineStart = i + 1
            }
        }
        scannedTo = to
    }
    var idx = text.indexOf(query, 0, ignoreCase)
    while (idx >= 0) {
        total++
        if (previews.size < maxPreviews) {
            advance(idx)
            // Window konteks terbatas dalam BARIS yang sama — bukan baris penuh.
            val lineEndRaw = text.indexOf('\n', idx)
            val lineEnd = if (lineEndRaw < 0) text.length else lineEndRaw
            val ws = maxOf(curLineStart, idx - PREVIEW_CONTEXT_CHARS)
            val we = minOf(lineEnd, idx + query.length + PREVIEW_CONTEXT_CHARS)
            previews.add(
                MatchPreview(
                    lineNumber = curLine + 1, // 1-based untuk tampilan
                    lineText = text.substring(ws, we),
                    startInLine = idx - ws,
                    endInLine = idx - ws + query.length
                )
            )
        }
        idx = text.indexOf(query, idx + query.length, ignoreCase)
    }
    return FindOutcome(total, previews)
}

internal const val PREVIEW_CONTEXT_CHARS = 64

internal fun replaceLiteral(text: String, from: String, to: String, ignoreCase: Boolean): ReplaceOutcome {
    if (from.isEmpty()) return ReplaceOutcome(text, 0)
    // indexOf ignoreCase menjaga indeks tetap di koordinat text asli (lihat catatan atas),
    // jadi cursor selalu <= text.length dan splice StringBuilder aman.
    val out = StringBuilder(text.length)
    var count = 0
    var cursor = 0
    var idx = text.indexOf(from, 0, ignoreCase)
    while (idx >= 0) {
        out.append(text, cursor, idx).append(to)
        cursor = idx + from.length
        count++
        idx = text.indexOf(from, cursor, ignoreCase)
    }
    out.append(text, cursor, text.length)
    return ReplaceOutcome(out.toString(), count)
}

sealed interface BatchReplaceSummary {
    data class Success(val replacedCount: Int) : BatchReplaceSummary
    data object ChangedSkipped : BatchReplaceSummary
    data object Failed : BatchReplaceSummary
}

class FindReplaceEngine(
    // null = file tak terbaca / ditolak guard teks; false = gagal tulis.
    private val readText: suspend (String) -> String?,
    private val writeText: suspend (String, String) -> Boolean,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun scan(
        candidates: List<ToolNode>,
        query: String,
        ignoreCase: Boolean,
        maxPreviewsPerFile: Int = 12,
        onProgress: suspend (Int, Int) -> Unit = { _, _ -> }
    ): List<FileFindReport> = withContext(ioDispatcher) {
        val reports = mutableListOf<FileFindReport>()
        candidates.forEachIndexed { i, node ->
            val content = readText(node.uri)
            reports.add(
                FileFindReport(
                    node = node,
                    outcome = content?.let { findMatches(it, query, ignoreCase, maxPreviewsPerFile) }
                )
            )
            onProgress(i + 1, candidates.size)
        }
        reports
    }

    // Divergence guard interaksi-autosave: konten di-re-read dan dibandingkan dengan snapshot
    // fase scan. Beda sedikit pun → ChangedSkipped (file bisa saja baru ditulis autosave).
    suspend fun replaceVerified(
        uri: String,
        expectedContent: String,
        query: String,
        replacement: String,
        ignoreCase: Boolean
    ): BatchReplaceSummary = withContext(ioDispatcher) {
        val fresh = readText(uri) ?: return@withContext BatchReplaceSummary.Failed
        if (fresh != expectedContent) return@withContext BatchReplaceSummary.ChangedSkipped
        val result = replaceLiteral(fresh, query, replacement, ignoreCase)
        if (writeText(uri, result.newText)) BatchReplaceSummary.Success(result.count)
        else BatchReplaceSummary.Failed
    }
}
