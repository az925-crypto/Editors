package com.zaaam.editors.core.tools

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

@OptIn(ExperimentalCoroutinesApi::class)
class DuplicateFinderTest {

    private fun TestScope.finderOf(contents: Map<String, ByteArray>, sizesOverride: Map<String, Long?> = emptyMap()) =
        DuplicateFinder(
            openStream = { uri -> contents[uri]?.let(::ByteArrayInputStream) },
            statSize = { uri -> sizesOverride[uri] ?: contents[uri]?.size?.toLong() },
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

    private fun f(name: String, size: Long) = ToolNode(name, "u:$name", name, isDir = false, size = size)

    @Test
    fun `konten identik beda nama jadi satu grup`() = runTest {
        val same = "hello world dup".toByteArray()
        val finder = finderOf(mapOf(
            "u:a" to same,
            "u:b" to same,
            "u:c" to "beda total".toByteArray()
        ))
        val outcome = finder.find(listOf(f("a", same.size.toLong()), f("b", same.size.toLong()), f("c", 12)))

        assertEquals(1, outcome.groups.size)
        assertEquals(setOf("a", "b"), outcome.groups[0].nodes.map { it.name }.toSet())
        assertEquals(0, outcome.stats.changedDuringScan)
    }

    @Test
    fun `ukuran sama konten beda tidak digrup`() = runTest {
        val finder = finderOf(mapOf(
            "u:a" to "AAAABBBB".toByteArray(),
            "u:b" to "AAAACCCC".toByteArray() // head 8-byte sama? beda di tail → full hash beda
        ))
        val outcome = finder.find(listOf(f("a", 8), f("b", 8)))
        assertTrue(outcome.groups.isEmpty())
    }

    @Test
    fun `file nol byte dan oversize di-skip`() = runTest {
        val big = ByteArray(DuplicateFinder.MAX_HASH_FILE_BYTES.toInt() + 1)
        val finder = finderOf(mapOf("u:z" to ByteArray(0), "u:big" to big))
        val outcome = finder.find(listOf(f("z", 0), f("big", big.size.toLong())))

        assertTrue(outcome.groups.isEmpty())
        assertEquals(1, outcome.stats.oversizedSkipped)
    }

    @Test
    fun `file berubah saat scan dikecualikan dan terhitung`() = runTest {
        val same = "dupdup".toByteArray()
        // statSize untuk "b" melaporkan ukuran lain → dianggap berubah.
        val finder = finderOf(
            mapOf("u:a" to same, "u:b" to same),
            sizesOverride = mapOf("u:b" to 99L)
        )
        val outcome = finder.find(listOf(f("a", same.size.toLong()), f("b", same.size.toLong())))

        assertEquals(1, outcome.stats.changedDuringScan)
        assertTrue(outcome.groups.isEmpty())
    }

    @Test
    fun `head hash sama tapi full hash beda tidak digrup`() = runTest {
        val prefix = ByteArray(70 * 1024) { 7 } // > HEAD_WINDOW: fase head meloloskan keduanya
        val a = prefix + byteArrayOf(1)
        val b = prefix + byteArrayOf(2)
        val finder = finderOf(mapOf("u:a" to a, "u:b" to b))
        val outcome = finder.find(listOf(f("a", a.size.toLong()), f("b", b.size.toLong())))

        // Fase full SHA-1 harus memisahkan keduanya.
        assertTrue(outcome.groups.isEmpty())
    }

    @Test
    fun `progress hash ter-emit saat kandidat ada`() = runTest {
        val same = "x".toByteArray()
        val finder = finderOf(mapOf("u:a" to same, "u:b" to same))
        var emitted = 0
        finder.find(listOf(f("a", 1), f("b", 1))) { emitted++ }
        assertTrue(emitted >= 1)
    }
}
