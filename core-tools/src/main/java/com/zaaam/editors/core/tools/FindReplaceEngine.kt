package com.zaaam.editors.core.tools

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Pencarian LITERAL murni (loop indexOf — sengaja BUKAN Regex, supaya query berisi
// metacharacter tidak bikin perilaku aneh). Non-overlapping, maju sepanjang panjang match.
internal fun findMatches(text: String, query: String, ignoreCase: Boolean, maxPreviews: Int): FindOutcome {
    if (query.isEmpty()) return FindOutcome(0, emptyList())
    val hay = if (ignoreCase) text.lowercase() else text
    val needle = if (ignoreCase) query.lowercase() else query
    var total = 0
    val previews = mutableListOf<MatchPreview>()
    var idx = hay.indexOf(needle)
    while (idx >= 0) {
        total++
        if (previews.size < maxPreviews) previews.add(previewAt(text, idx, needle.length))
        idx = hay.indexOf(needle, idx + needle.length)
    }
    return FindOutcome(total, previews)
}

private fun previewAt(text: String, matchStart: Int, matchLen: Int): MatchPreview {
    var lineNumber = 0
    var lineStart = 0
    for (i in 0 until matchStart) {
        if (text[i] == '\n') {
            lineNumber++
            lineStart = i + 1
        }
    }
    var lineEnd = text.indexOf('\n', matchStart)
    if (lineEnd < 0) lineEnd = text.length
    return MatchPreview(
        lineNumber = lineNumber + 1, // 1-based untuk tampilan
        lineText = text.substring(lineStart, lineEnd),
        startInLine = matchStart - lineStart,
        endInLine = matchStart - lineStart + matchLen
    )
}

internal fun replaceLiteral(text: String, from: String, to: String, ignoreCase: Boolean): ReplaceOutcome {
    if (from.isEmpty()) return ReplaceOutcome(text, 0)
    val hay = if (ignoreCase) text.lowercase() else text
    val needle = if (ignoreCase) from.lowercase() else from
    val out = StringBuilder(text.length)
    var count = 0
    var cursor = 0
    var idx = hay.indexOf(needle)
    while (idx >= 0) {
        out.append(text, cursor, idx).append(to)
        cursor = idx + needle.length
        count++
        idx = hay.indexOf(needle, cursor)
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
        maxPreviewsPerFile: Int = 50,
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
