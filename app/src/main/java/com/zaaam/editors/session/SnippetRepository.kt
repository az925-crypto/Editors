package com.zaaam.editors.session

import android.content.SharedPreferences
import com.zaaam.editors.core.tools.BatchReplaceSummary
import com.zaaam.editors.core.tools.Snippet
import com.zaaam.editors.core.tools.SnippetJsonCodec
import com.zaaam.editors.core.tools.SnippetParseResult
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

// ===== Helper PURE (internal top-level, tested JVM — pola parseRecentEntry) =====

// Format ukuran ala mockup ("412.3 MB", "12.4 KB"). Negatif/korup → "0 B".
internal fun humanBytes(bytes: Long): String {
    if (bytes < 0) return "0 B"
    if (bytes < 1024) return "$bytes B"
    var v = bytes.toDouble()
    var unit = 0
    while (v >= 1024.0 && unit < UNITS.lastIndex) {
        v /= 1024.0
        unit++
    }
    return String.format(java.util.Locale.US, "%.1f %s", v, UNITS[unit])
}

private val UNITS = arrayOf("B", "KB", "MB", "GB")

// Kontrak reviewer: MatchPreview multi-baris bisa bawa endInLine > lineText.length
// (query mengandung '\n') — clamp sebelum dipakai range AnnotatedString biar UI tidak
// substring-crash. Return Pair(startInclusive, endExclusive) siap SpanRange.
internal fun clampHighlight(startInLine: Int, endInLine: Int, lineLen: Int): Pair<Int, Int> {
    val s = startInLine.coerceIn(0, lineLen)
    val e = endInLine.coerceIn(s, lineLen)
    return s to e
}

data class ReplaceTotals(
    val filesReplaced: Int,
    val replacedCount: Int,
    val changedSkipped: Int,
    val failed: Int
)

internal fun summarizeReplace(results: List<BatchReplaceSummary>): ReplaceTotals {
    var okFiles = 0
    var count = 0
    var skipped = 0
    var failed = 0
    for (r in results) {
        when (r) {
            is BatchReplaceSummary.Success -> { okFiles++; count += r.replacedCount }
            BatchReplaceSummary.ChangedSkipped -> skipped++
            BatchReplaceSummary.Failed -> failed++
        }
    }
    return ReplaceTotals(okFiles, count, skipped, failed)
}

// Merge impor: id yang sudah ada DILEWATI (bukan ditimpa — data lama user kudetakan).
// Id dobel DI DALAM file impor sendiri: yang pertama menang, sisanya dihitung skip.
// Return (merged, jumlahSkipExisting).
internal fun mergeSnippetsById(existing: List<Snippet>, incoming: List<Snippet>): Pair<List<Snippet>, Int> {
    val ids = HashSet<String>(existing.size + incoming.size)
    existing.forEach { ids.add(it.id) }
    var skipped = 0
    val fresh = mutableListOf<Snippet>()
    for (s in incoming) {
        if (!ids.add(s.id)) skipped++
        else fresh.add(s)
    }
    return (existing + fresh) to skipped
}

// Nama file ekspor "zaaam-snippets-20260826.json" — UTC supaya determinisme lintas device.
internal fun exportFileName(epochMs: Long): String {
    val day = DateTimeFormatter.ofPattern("yyyyMMdd")
        .withZone(ZoneOffset.UTC)
        .format(Instant.ofEpochMilli(epochMs))
    return "zaaam-snippets-$day.json"
}

// ===== Repository =====

sealed interface SnippetImportOutcome {
    data class Imported(val added: Int, val skippedExisting: Int, val skippedInvalid: Int) : SnippetImportOutcome
    data object BadSchema : SnippetImportOutcome
}

class SnippetRepository(private val prefs: SharedPreferences) {

    fun load(): List<Snippet> {
        val raw = prefs.getString(PREF_KEY, null) ?: return emptyList()
        // File prefs korup (diubah luar / versi lama): jangan wipe prefs — tampilkan kosong,
        // simpan berikutnya akan menimpa dengan payload valid baru.
        val parsed = (SnippetJsonCodec.parse(raw) as? SnippetParseResult.Ok)?.snippets ?: emptyList()
        // Codec TIDAK dedup id (parse sukses utk id dobel dari file luar) — LazyColumn key
        // wajib unik: pertama menang, konsisten dengan mergeSnippetsById.
        return parsed.distinctBy { it.id }
    }

    fun saveAll(list: List<Snippet>) {
        prefs.edit().putString(PREF_KEY, SnippetJsonCodec.encode(list)).apply()
    }

    fun add(name: String, language: String, tagsCsv: String, code: String): Snippet {
        val snippet = Snippet(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            language = language.trim().ifBlank { "plaintext" },
            tags = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
            code = code,
            updatedAtMs = System.currentTimeMillis()
        )
        saveAll(load() + snippet)
        return snippet
    }

    fun update(originalId: String, name: String, language: String, tagsCsv: String, code: String) {
        val updated = load().map { s ->
            if (s.id == originalId) s.copy(
                name = name.trim(),
                language = language.trim().ifBlank { "plaintext" },
                tags = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
                code = code,
                updatedAtMs = System.currentTimeMillis()
            ) else s
        }
        saveAll(updated)
    }

    fun delete(id: String) {
        saveAll(load().filterNot { it.id == id })
    }

    fun exportJson(): String = SnippetJsonCodec.encode(load())

    fun importJson(payload: String): SnippetImportOutcome {
        return when (val parsed = SnippetJsonCodec.parse(payload)) {
            is SnippetParseResult.BadSchema -> SnippetImportOutcome.BadSchema
            is SnippetParseResult.Ok -> {
                val (merged, skippedExisting) = mergeSnippetsById(load(), parsed.snippets)
                saveAll(merged)
                SnippetImportOutcome.Imported(parsed.snippets.size - skippedExisting, skippedExisting, parsed.skippedInvalid)
            }
        }
    }

    companion object {
        const val PREF_KEY = "snippets_v1" // key terkunci dari keputusan Phase 2
    }
}
