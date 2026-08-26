package com.zaaam.editors.tools

import com.zaaam.editors.core.tools.BatchReplaceSummary
import com.zaaam.editors.core.tools.Snippet
import com.zaaam.editors.session.exportFileName
import com.zaaam.editors.session.mergeSnippetsById
import com.zaaam.editors.session.summarizeReplace
import org.junit.Assert.assertEquals
import org.junit.Test

class SnippetsSupportTest {

    private fun s(id: String, name: String = "n") = Snippet(id, name, "kotlin", emptyList(), "code", 1L)

    @Test
    fun `merge skip id existing dan id dobel dalam impor`() {
        val existing = listOf(s("a"), s("b"))
        val incoming = listOf(s("b", "timpa? jangan"), s("c"), s("c", "dobel"), s("d"))
        val (merged, skipped) = mergeSnippetsById(existing, incoming)

        assertEquals(listOf("a", "b", "c", "d"), merged.map { it.id })
        assertEquals("n", merged.first { it.id == "b" }.name) // data lama menang
        assertEquals(2, skipped)
    }

    @Test
    fun `merge dengan existing kosong masuk semua`() {
        val incoming = listOf(s("x"), s("y"))
        val (merged, skipped) = mergeSnippetsById(emptyList(), incoming)
        assertEquals(2, merged.size)
        assertEquals(0, skipped)
    }

    @Test
    fun `nama ekspor deterministik UTC`() {
        // 2026-08-26 00:00:00 UTC
        val ms = java.time.LocalDate.of(2026, 8, 26)
            .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals("zaaam-snippets-20260826.json", exportFileName(ms))
    }

    @Test
    fun `summarize replace rollup semua outcome`() {
        val totals = summarizeReplace(
            listOf(
                BatchReplaceSummary.Success(3),
                BatchReplaceSummary.Success(2),
                BatchReplaceSummary.ChangedSkipped,
                BatchReplaceSummary.ChangedSkipped,
                BatchReplaceSummary.Failed
            )
        )
        assertEquals(2, totals.filesReplaced)
        assertEquals(5, totals.replacedCount)
        assertEquals(2, totals.changedSkipped)
        assertEquals(1, totals.failed)
    }
}
