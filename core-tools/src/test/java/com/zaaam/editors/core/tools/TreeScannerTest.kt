package com.zaaam.editors.core.tools

import com.zaaam.editors.core.fs.FsResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit test TreeScanner — fake tree berbasis map uri→children, murni JVM.
@OptIn(ExperimentalCoroutinesApi::class)
class TreeScannerTest {

    private fun TestScope.scannerOf(tree: Map<String, List<ToolNode>>) = TreeScanner(
        listChildren = { uri ->
            tree[uri]?.let { FsResult.Success(it) } ?: FsResult.Error(Exception("folder tidak tersedia"))
        },
        ioDispatcher = StandardTestDispatcher(testScheduler)
    )

    private fun f(name: String) = ToolNode(name, "u:$name", "", isDir = false, size = 10L)
    private fun d(name: String) = ToolNode(name, "u:$name", "", isDir = true)
    private fun h(name: String) = ToolNode(name, "u:$name", "", isDir = false, isHidden = true)
    private fun hd(name: String) = ToolNode(name, "u:$name", "", isDir = true, isHidden = true)

    @Test
    fun `walk membangun relPath bertingkat`() = runTest {
        val tree = mapOf(
            "root" to listOf(f("readme.md"), d("assets")),
            "u:assets" to listOf(f("hero.png"), d("img")),
            "u:img" to listOf(f("a.jpg"))
        )
        val result = scannerOf(tree).walk("root", includeHidden = true)

        assertEquals(setOf("readme.md", "assets/hero.png", "assets/img/a.jpg"), result.files.map { it.relPath }.toSet())
        assertEquals(setOf("assets", "assets/img"), result.dirs.map { it.relPath }.toSet())
        assertEquals(0, result.stats.skippedDirs)
    }

    @Test
    fun `hidden dikecualikan beserta isinya kalau includeHidden false`() = runTest {
        val tree = mapOf(
            "root" to listOf(h(".env"), hd(".git"), f("main.kt")),
            "u:.git" to listOf(f("config"))
        )
        val result = scannerOf(tree).walk("root", includeHidden = false)

        assertEquals(listOf("main.kt"), result.files.map { it.relPath })
        assertTrue(result.dirs.isEmpty())
    }

    @Test
    fun `hidden tetap masuk kalau includeHidden true`() = runTest {
        val tree = mapOf(
            "root" to listOf(h(".env"), f("main.kt")),
            "u:.env" to emptyList()
        )
        val result = scannerOf(tree).walk("root", includeHidden = true)

        assertEquals(2, result.files.size)
    }

    @Test
    fun `folder gagal dihitung skipped dan sibling tetap jalan`() = runTest {
        // "locked" tidak punya entri di map → listChildren Error.
        val tree = mapOf(
            "root" to listOf(d("locked"), f("ok.txt")),
            "u:ok.txt" to emptyList()
        )
        val result = scannerOf(tree).walk("root", includeHidden = true)

        assertEquals(1, result.stats.skippedDirs)
        assertEquals(listOf("ok.txt"), result.files.map { it.relPath })
    }

    @Test
    fun `progress ter-emit minimal sekali per folder`() = runTest {
        val tree = mapOf(
            "root" to listOf(f("a.txt"), d("sub")),
            "u:sub" to listOf(f("b.txt"))
        )
        var calls = 0
        scannerOf(tree).walk("root", includeHidden = true) { calls++ }
        assertTrue(calls >= 2)
    }

    @Test
    fun `root gagal ditandai rootFailed bukan dianggap folder kosong`() = runTest {
        // Map sengaja tanpa entri "root" → listChildren(root) Error.
        val tree = mapOf("u:lain" to emptyList<ToolNode>())
        val result = scannerOf(tree).walk("root", includeHidden = true)

        assertTrue(result.stats.rootFailed)
        assertEquals(0, result.stats.skippedDirs) // root gagal ≠ skipped biasa
        assertTrue(result.files.isEmpty() && result.dirs.isEmpty())
    }

    @Test
    fun `siklus folder terminate dan tidak dobel hitung`() = runTest {
        // Provider jahat/buggy: folder menunjuk dirinya sendiri → walk harus berhenti sendiri;
        // selesainya test ini adalah bukti anti infinite loop.
        val tree = mapOf(
            "root" to listOf(d("loop"), f("a.txt")),
            "u:loop" to listOf(d("loop"))
        )
        val result = scannerOf(tree).walk("root", includeHidden = true)

        assertEquals(listOf("loop"), result.dirs.map { it.relPath }) // sekali saja
        assertEquals(listOf("a.txt"), result.files.map { it.relPath })

        // Siklus antar-folder: a → b → a juga harus putus lewat visited-set.
        val mutual = mapOf(
            "root" to listOf(d("a"), f("x.txt")),
            "u:a" to listOf(d("b")),
            "u:b" to listOf(d("a"))
        )
        val result2 = scannerOf(mutual).walk("root", includeHidden = true)

        assertEquals(listOf("a", "a/b"), result2.dirs.map { it.relPath })
        assertEquals(listOf("x.txt"), result2.files.map { it.relPath })
    }

    @Test
    fun `progress membawa hitungan file dan estimasi monotonik`() = runTest {
        // 1 root berisi 2 file + 1 subfolder; subfolder berisi 1 file.
        val tree = mapOf(
            "root" to listOf(f("a.txt"), f("b.txt"), d("sub")),
            "u:sub" to listOf(f("c.txt"))
        )
        val emissions = mutableListOf<ToolProgress>()
        scannerOf(tree).walk("root", includeHidden = true) { emissions.add(it) }

        // done = folder selesai + file terlihat: setelah root = 1+2, setelah sub = 2+3.
        assertEquals(3, emissions.first().done)
        val last = emissions.last()
        assertEquals(5, last.done)
        assertTrue(last.totalEstimate >= last.done)
        // totalEstimate tetap monotonik walau tumbuh saat discovery.
        assertTrue(emissions.zipWithNext().all { (prev, next) -> next.totalEstimate >= prev.totalEstimate })
    }
}
