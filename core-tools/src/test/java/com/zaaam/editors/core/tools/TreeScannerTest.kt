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

        assertEquals(setOf("readme.md", "hero.png", "a.jpg"), result.files.map { it.relPath }.toSet())
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
}
