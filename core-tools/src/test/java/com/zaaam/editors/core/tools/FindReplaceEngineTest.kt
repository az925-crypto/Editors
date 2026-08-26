package com.zaaam.editors.core.tools

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FindReplacePureTest {

    @Test
    fun `findMatches hitung semua okurensi non-overlapping`() {
        val outcome = findMatches("aaaa", "aa", ignoreCase = false, maxPreviews = 10)
        assertEquals(2, outcome.totalMatches)
    }

    @Test
    fun `findMatches ignoreCase`() {
        val outcome = findMatches("Halo halo HALO", "halo", ignoreCase = true, maxPreviews = 10)
        assertEquals(3, outcome.totalMatches)
    }

    @Test
    fun `findMatches query kosong aman`() {
        assertEquals(0, findMatches("abc", "", false, 10).totalMatches)
    }

    @Test
    fun `preview membawa nomor baris dan posisi dalam baris`() {
        val text = "baris satu\nbaris dua target\nbaris tiga"
        val outcome = findMatches(text, "target", false, 10)

        assertEquals(1, outcome.totalMatches)
        val p = outcome.previews.single()
        assertEquals(2, p.lineNumber)
        assertEquals("baris dua target", p.lineText)
        assertEquals(10, p.startInLine)
        assertEquals(16, p.endInLine)
    }

    @Test
    fun `maxPreviews membatasi preview tanpa bohong soal total`() {
        val outcome = findMatches("ab ab ab ab", "ab", false, maxPreviews = 2)
        assertEquals(4, outcome.totalMatches)
        assertEquals(2, outcome.previews.size)
    }

    @Test
    fun `replaceLiteral mengganti semua dan menjaga sisanya`() {
        val result = replaceLiteral("satu dua satu", "satu", "eno", ignoreCase = false)
        assertEquals("eno dua eno", result.newText)
        assertEquals(2, result.count)
    }

    @Test
    fun `replaceLiteral ignoreCase memakai casing replacement`() {
        val result = replaceLiteral("Halo HALO halO", "halo", "hai", ignoreCase = true)
        assertEquals("hai hai hai", result.newText)
    }

    @Test
    fun `replaceLiteral metachar diperlakukan literal`() {
        val result = replaceLiteral("a.b aXb", ".", "-", ignoreCase = false)
        assertEquals("a-b aXb", result.newText)
    }

    @Test
    fun `replaceLiteral from kosong tidak mengubah apa pun`() {
        val result = replaceLiteral("tetap", "", "x", false)
        assertEquals("tetap", result.newText)
        assertEquals(0, result.count)
    }

    @Test
    fun `regresi char lowercase memanjang tidak bikin crash`() {
        // 'İ' U+0130 kalau dilowercase() memanjang jadi 2 char ("i" + combining dot).
        // Implementasi lama pakai lowercase() sehingga indeks match lepas dari text asli.
        val outcome = findMatches("İx İx", "ix", ignoreCase = true, maxPreviews = 10)
        assertEquals(0, outcome.totalMatches) // İ tidak fold ke 'i': aman, bukan false positive
        val result = replaceLiteral("İx İx", "ix", "R", ignoreCase = true)
        assertEquals("İx İx", result.newText) // teks utuh, tanpa exception
        assertEquals(0, result.count)
    }

    @Test
    fun `indeks match presisi walau lowercase memanjang panjangnya`() {
        // Bukti indeks ASLI yang dipakai: match kedua mulai di 'İ' kedua (index 3),
        // bukan di koordinat versi lowercase yang sudah bergeser +1 per 'İ'.
        val text = "İA İA"
        val outcome = findMatches(text, "İa", ignoreCase = true, maxPreviews = 10)
        assertEquals(2, outcome.totalMatches)
        assertEquals(listOf(0, 3), outcome.previews.map { it.startInLine })
        assertEquals(listOf(2, 5), outcome.previews.map { it.endInLine })

        val result = replaceLiteral(text, "İa", "Z", ignoreCase = true)
        assertEquals("Z Z", result.newText)
        assertEquals(2, result.count)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FindReplaceEngineTest {

    private fun TestScope.engineOf(
        files: MutableMap<String, String>,
        reject: Set<String> = emptySet()
    ) = FindReplaceEngine(
        readText = { uri -> if (uri in reject) null else files[uri] },
        writeText = { uri, text ->
            if (uri in reject) false else {
                files[uri] = text
                true
            }
        },
        ioDispatcher = StandardTestDispatcher(testScheduler)
    )

    private fun node(name: String) = ToolNode(name, "u:$name", name, isDir = false, size = 0L)

    @Test
    fun `scan menghasilkan laporan per file termasuk yang tak terbaca`() = runTest {
        val files = mutableMapOf("u:a" to "carrot soup", "u:b" to "no match")
        val engine = engineOf(files, reject = setOf("u:bin"))

        val reports = engine.scan(listOf(node("a"), node("b"), node("bin")), "carrot", ignoreCase = false)

        assertEquals(3, reports.size)
        assertEquals(1, reports[0].outcome?.totalMatches)
        assertEquals(0, reports[1].outcome?.totalMatches)
        assertEquals(null, reports[2].outcome)
    }

    @Test
    fun `replaceVerified menulis kalau snapshot masih segar`() = runTest {
        val files = mutableMapOf("u:a" to "merah dan merah")
        val engine = engineOf(files)

        val summary = engine.replaceVerified("u:a", "merah dan merah", "merah", "biru", ignoreCase = false)

        assertTrue(summary is BatchReplaceSummary.Success)
        assertEquals(2, (summary as BatchReplaceSummary.Success).replacedCount)
        assertEquals("biru dan biru", files["u:a"])
    }

    @Test
    fun `replaceVerified skip kalau konten berubah di antara scan dan replace`() = runTest {
        // Simulasi autosave menyelip: isi berubah SETELAH user lihat hasil scan.
        val files = mutableMapOf("u:a" to "merah dan merah PLUS tambahan")
        val engine = engineOf(files)

        val summary = engine.replaceVerified("u:a", "merah dan merah", "merah", "biru", ignoreCase = false)

        assertTrue(summary is BatchReplaceSummary.ChangedSkipped)
        assertEquals("merah dan merah PLUS tambahan", files["u:a"])
    }

    @Test
    fun `replaceVerified failed kalau file tak terbaca`() = runTest {
        val files = mutableMapOf<String, String>()
        val engine = engineOf(files, reject = setOf("u:x"))
        val summary = engine.replaceVerified("u:x", "apa saja", "a", "b", ignoreCase = false)
        assertTrue(summary is BatchReplaceSummary.Failed)
    }
}
