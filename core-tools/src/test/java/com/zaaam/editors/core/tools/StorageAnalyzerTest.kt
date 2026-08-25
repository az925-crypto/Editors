package com.zaaam.editors.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageAnalyzerTest {

    private fun f(relPath: String, size: Long, name: String = relPath.substringAfterLast('/')) =
        ToolNode(name, "u:${name}#$relPath", relPath, isDir = false, size = size)

    @Test
    fun `agregat total file folder bytes`() {
        val report = aggregateAnalysis(
            files = listOf(f("a.txt", 100), f("sub/b.txt", 50)),
            dirs = listOf(ToolNode("sub", "u:sub", "sub", isDir = true)),
            skippedDirs = 2
        )

        assertEquals(2, report.fileCount)
        assertEquals(1, report.folderCount)
        assertEquals(150L, report.totalBytes)
        assertEquals(2, report.skippedDirs)
    }

    @Test
    fun `largestFiles urut menurun dan ter-capat`() {
        val files = (1..15).map { f("f$it.txt", it.toLong() * 10) }
        val report = aggregateAnalysis(files, emptyList(), 0, maxLists = 10)

        assertEquals(10, report.largestFiles.size)
        assertEquals(150L, report.largestFiles.first().bytes) // f15 → 150
        assertEquals(listOf(150L, 140L, 130L), report.largestFiles.take(3).map { it.bytes })
    }

    @Test
    fun `largestDirs diagregasi per folder top-level`() {
        val files = listOf(
            f("assets/img/hero.png", 90),
            f("assets/img/logo.png", 10),
            f("assets/js/x.js", 40),
            f("readme.md", 5)
        )
        val report = aggregateAnalysis(files, emptyList(), 0)

        assertEquals("(akar)", report.largestDirs.first { it.label == "(akar)" }.label)
        val assets = report.largestDirs.first { it.label == "assets" }
        assertEquals(140L, assets.bytes)
        assertEquals(3, assets.fileCount)
        assertEquals("assets", report.largestDirs.first().label) // terbesar di depan
    }

    @Test
    fun `tree kosong tidak meledak`() {
        val report = aggregateAnalysis(emptyList(), emptyList(), 0)
        assertEquals(0L, report.totalBytes)
        assertTrue(report.largestFiles.isEmpty())
        assertTrue(report.largestDirs.isEmpty())
    }
}
