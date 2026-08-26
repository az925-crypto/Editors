package com.zaaam.editors.core.tools

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class Snippet(
    val id: String,
    val name: String,
    val language: String,
    val tags: List<String>,
    val code: String,
    val updatedAtMs: Long
)

sealed interface SnippetParseResult {
    data class Ok(val snippets: List<Snippet>, val skippedInvalid: Int) : SnippetParseResult
    data object BadSchema : SnippetParseResult
}

const val SNIPPET_SCHEMA = "zaaam-snippets"
const val SNIPPET_VERSION = 1

// Codec JSON hand-rolled (skema cuma 5 field — plugin serialization tidak layak risikonya
// di repo sensitif-AGP-9 ini). Adapter point Codexa: ganti/implement SnippetExchange nanti.
// PUBLIC sejak UI Phase 2: SnippetsScreen (:app) encode/decode via repo prefs "snippets_v1".
object SnippetJsonCodec {

    fun encode(snippets: List<Snippet>, exportedAtMs: Long = System.currentTimeMillis()): String {
        val sb = StringBuilder()
        sb.append("{\"schema\":\"").append(SNIPPET_SCHEMA).append("\",\"version\":").append(SNIPPET_VERSION)
        sb.append(",\"exportedAt\":\"").append(isoUtc(exportedAtMs)).append("\",\"snippets\":[")
        snippets.forEachIndexed { i, s ->
            if (i > 0) sb.append(',')
            sb.append("{\"id\":\"").append(escape(s.id))
                .append("\",\"name\":\"").append(escape(s.name))
                .append("\",\"language\":\"").append(escape(s.language))
                .append("\",\"tags\":[")
            s.tags.forEachIndexed { j, t -> if (j > 0) sb.append(','); sb.append('"').append(escape(t)).append('"') }
            sb.append("],\"code\":\"").append(escape(s.code))
                .append("\",\"updatedAtMs\":").append(s.updatedAtMs)
                .append('}')
        }
        sb.append("]}")
        return sb.toString()
    }

    fun parse(json: String): SnippetParseResult {
        val root = JsonMini.parseOrNull(json) ?: return SnippetParseResult.BadSchema
        if (root !is Map<*, *>) return SnippetParseResult.BadSchema
        if (root["schema"] != SNIPPET_SCHEMA || root["version"] != 1L) return SnippetParseResult.BadSchema
        val arr = root["snippets"] as? List<*> ?: return SnippetParseResult.BadSchema
        var skipped = 0
        val out = mutableListOf<Snippet>()
        for (entry in arr) {
            val m = entry as? Map<*, *>
            val name = m?.get("name") as? String
            val code = m?.get("code") as? String
            if (m == null || name.isNullOrBlank() || code == null) {
                skipped++
                continue
            }
            val language = ((m["language"] as? String)?.takeIf { it.isNotBlank() }) ?: "plaintext"
            val tags = (m["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val id = (m["id"] as? String)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
            val updated = (m["updatedAtMs"] as? Long) ?: 0L
            out.add(Snippet(id, name, language, tags, code, updated))
        }
        return SnippetParseResult.Ok(out, skipped)
    }

    private fun escape(raw: String): String {
        val sb = StringBuilder(raw.length + 8)
        for (ch in raw) {
            when {
                ch == '"' -> sb.append("\\\"")
                ch == '\\' -> sb.append("\\\\")
                ch == '\n' -> sb.append("\\n")
                ch == '\r' -> sb.append("\\r")
                ch == '\t' -> sb.append("\\t")
                ch < ' ' -> sb.append("\\u").append(String.format("%04x", ch.code))
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun isoUtc(ms: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(ms))
    }
}

// Batas nesting JSON utk input untrusted (import user): tanpa ini rekursi parser bisa
// StackOverflowError — Error yang lolos dari catch(Exception) di parseOrNull → crash proses.
internal const val JSON_MAX_DEPTH = 128

// Parser JSON minimal STRICT untuk kebutuhan codec: object/array/string/number(long)/bool/null.
// Cukup ketat: string tanpa escape valid ditolak, trailing garbage ditolak.
internal object JsonMini {

    fun parseOrNull(json: String): Any? = try {
        val p = Parser(json)
        val v = p.parseValue(depth = 0)
        p.skipWs()
        if (!p.atEnd() || p.failed) null else v
    } catch (_: Exception) {
        null
    }

    private class Parser(val s: String) {
        var pos = 0
        var failed = false

        fun atEnd() = pos >= s.length
        fun fail(): Nothing? { failed = true; return null }

        fun skipWs() { while (pos < s.length && s[pos].isWhitespace()) pos++ }

        fun parseValue(depth: Int): Any? {
            if (depth > JSON_MAX_DEPTH) return fail() // stop rekursi sebelum stack habis
            skipWs()
            if (atEnd()) return fail()
            return when (s[pos]) {
                '{' -> parseObject(depth)
                '[' -> parseArray(depth)
                '"' -> parseString()
                else -> parseLiteral()
            }
        }

        fun parseObject(depth: Int): Any? {
            pos++ // {
            val map = LinkedHashMap<String, Any?>()
            skipWs()
            if (!atEnd() && s[pos] == '}') { pos++; return map }
            while (true) {
                skipWs()
                if (atEnd() || s[pos] != '"') return fail()
                val key = parseString() ?: return null
                skipWs()
                if (atEnd() || s[pos] != ':') return fail()
                pos++
                val value = parseValue(depth + 1) ?: if (failed) return null else null
                if (failed) return null
                map[key] = value
                skipWs()
                if (atEnd()) return fail()
                when (s[pos]) {
                    ',' -> pos++
                    '}' -> { pos++; return map }
                    else -> return fail()
                }
            }
        }

        fun parseArray(depth: Int): Any? {
            pos++ // [
            val list = mutableListOf<Any?>()
            skipWs()
            if (!atEnd() && s[pos] == ']') { pos++; return list }
            while (true) {
                val item = parseValue(depth + 1) ?: if (failed) return null else null
                if (failed) return null
                list.add(item)
                skipWs()
                if (atEnd()) return fail()
                when (s[pos]) {
                    ',' -> pos++
                    ']' -> { pos++; return list }
                    else -> return fail()
                }
            }
        }

        fun parseString(): String? {
            pos++ // "
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) return fail()
                when (val c = s[pos]) {
                    '"' -> { pos++; return sb.toString() }
                    '\\' -> {
                        pos++
                        if (atEnd()) return fail()
                        when (val e = s[pos]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 >= s.length) return fail()
                                val hexPart = s.substring(pos + 1, pos + 5)
                                val code = hexPart.toIntOrNull(16) ?: return fail()
                                sb.append(code.toChar())
                                pos += 4
                            }
                            else -> return fail()
                        }
                        pos++
                    }
                    else -> {
                        if (c.code < 0x20) return fail() // raw control char = invalid
                        sb.append(c)
                        pos++
                    }
                }
            }
        }

        fun parseLiteral(): Any? {
            val start = pos
            while (!atEnd() && ",]} \n\r\t".indexOf(s[pos]) < 0) pos++
            when (val token = s.substring(start, pos)) {
                "null" -> return null
                "true" -> return true
                "false" -> return false
                else -> return token.toLongOrNull() ?: token.toDoubleOrNull()?.toLong() ?: fail()
            }
        }
    }
}

// Adapter point ekstensi masa depan (mis. Codexa): implement interface ini dengan mapper
// format eksternal ↔ Snippet; UI cukup tahu interface ini.
interface SnippetExchange {
    fun exportAll(snippets: List<Snippet>): String
    fun import(payload: String): SnippetImportReport
}

data class SnippetImportReport(val added: List<Snippet>, val skippedExisting: Int, val skippedInvalid: Int)
