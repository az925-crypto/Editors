package com.zaaam.editors.ui.editor

import com.zaaam.editors.core.fs.FsResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit test AutosaveCoordinator — logika autosave tersubtle repo ini (dulu diverifikasi
// manual/reviewer doang). Virtual time runTest: debounce 900ms tidak benar-benar menunggu.
// Kontrak yang dikunci di sini (lihat komentar AutosaveCoordinator):
// debounce per-uri, coalesce keystroke, cancelQueued hanya menyentuh job mengantre,
// mutex menjaga urutan tulis, divergence membungkam Succeeded, gagal → Failed.
@OptIn(ExperimentalCoroutinesApi::class)
class AutosaveCoordinatorTest {

    @Test
    fun `debounce menahan penulisan sampai lewat batas`() = runTest {
        val writes = mutableListOf<Pair<String, String>>()
        val coord = AutosaveCoordinator(
            scope = backgroundScope,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            write = { u, c -> writes += u to c; FsResult.Success(Unit) },
            isStillCurrent = { _, _ -> true }
        )
        coord.onChange("u", "v1")
        advanceTimeBy(AUTOSAVE_DEBOUNCE_MS - 1)
        runCurrent()
        assertEquals(0, writes.size)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("u" to "v1"), writes)
    }

    @Test
    fun `ketikan cepat digabung jadi satu tulis konten terbaru`() = runTest {
        val writes = mutableListOf<Pair<String, String>>()
        val coord = AutosaveCoordinator(
            scope = backgroundScope,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            write = { u, c -> writes += u to c; FsResult.Success(Unit) },
            isStillCurrent = { _, _ -> true }
        )
        coord.onChange("u", "v1")
        coord.onChange("u", "v2")
        advanceTimeBy(100)
        coord.onChange("u", "v3")
        advanceTimeBy(AUTOSAVE_DEBOUNCE_MS + 10)
        runCurrent()
        assertEquals(1, writes.size)
        assertEquals("u" to "v3", writes.single())
    }

    @Test
    fun `cancelQueued sebelum in-flight membatalkan tulis`() = runTest {
        val writes = mutableListOf<Pair<String, String>>()
        val coord = AutosaveCoordinator(
            scope = backgroundScope,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            write = { u, c -> writes += u to c; FsResult.Success(Unit) },
            isStillCurrent = { _, _ -> true }
        )
        coord.onChange("u", "v1")
        advanceTimeBy(AUTOSAVE_DEBOUNCE_MS - 1)
        runCurrent()
        coord.cancelQueued("u")
        advanceTimeBy(2000)
        runCurrent()
        assertEquals(0, writes.size)
    }

    @Test
    fun `cancelQueued setelah in-flight tidak membatalkan tulis anti kepotong`() = runTest {
        val writes = mutableListOf<Pair<String, String>>()
        val gate = CompletableDeferred<Unit>()
        val coord = AutosaveCoordinator(
            scope = backgroundScope,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            write = { u, c -> gate.await(); writes += u to c; FsResult.Success(Unit) },
            isStillCurrent = { _, _ -> true }
        )
        coord.onChange("u", "v1")
        advanceTimeBy(AUTOSAVE_DEBOUNCE_MS)
        runCurrent() // job sudah lewat delay & self-remove; sekarang menunggu IO di gate
        coord.cancelQueued("u") // no-op sesuai kontrak anti file kepotong
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf("u" to "v1"), writes)
    }

    @Test
    fun `mutex menjaga urutan tulis uri sama`() = runTest {
        val order = mutableListOf<String>()
        val firstWriteGate = CompletableDeferred<Unit>()
        var callCount = 0
        val coord = AutosaveCoordinator(
            scope = backgroundScope,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            write = { _, c ->
                callCount++
                if (callCount == 1) {
                    firstWriteGate.await() // write pertama "lambat"
                }
                order += c
                FsResult.Success(Unit)
            },
            isStillCurrent = { _, _ -> true }
        )
        coord.onChange("u", "c1")
        advanceTimeBy(AUTOSAVE_DEBOUNCE_MS)
        runCurrent() // #1 memegang mutex, sedang diblok gate
        coord.onChange("u", "c2")
        advanceTimeBy(AUTOSAVE_DEBOUNCE_MS + 10)
        runCurrent() // #2 debounce selesai tapi menunggu mutex
        assertTrue(order.isEmpty())
        firstWriteGate.complete(Unit)
        runCurrent()
        assertEquals(listOf("c1", "c2"), order)
    }

    @Test
    fun `divergence membungkam event succeeded`() = runTest {
        val writes = mutableListOf<Pair<String, String>>()
        val events = mutableListOf<AutosaveCoordinator.Event>()
        val coord = AutosaveCoordinator(
            scope = backgroundScope,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            write = { u, c -> writes += u to c; FsResult.Success(Unit) },
            isStillCurrent = { _, _ -> false } // contentMap sudah berubah saat IO jalan
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            coord.events.collect { events += it }
        }
        coord.onChange("u", "v1")
        advanceTimeBy(AUTOSAVE_DEBOUNCE_MS + 10)
        runCurrent()
        // Tulis tetap jalan (data sampai ke disk), tapi TIDAK ada klaim Succeeded.
        assertEquals(1, writes.size)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `tulis berhasil mengemit succeeded dengan uri`() = runTest {
        val events = mutableListOf<AutosaveCoordinator.Event>()
        val coord = AutosaveCoordinator(
            scope = backgroundScope,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            write = { _, _ -> FsResult.Success(Unit) },
            isStillCurrent = { _, _ -> true }
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            coord.events.collect { events += it }
        }
        coord.onChange("uri-a", "isi")
        advanceTimeBy(AUTOSAVE_DEBOUNCE_MS + 10)
        runCurrent()
        assertEquals(listOf<AutosaveCoordinator.Event>(AutosaveCoordinator.Event.Succeeded("uri-a")), events)
    }

    @Test
    fun `tulis gagal mengemit failed dan tetap emit walau divergen`() = runTest {
        val events = mutableListOf<AutosaveCoordinator.Event>()
        val coord = AutosaveCoordinator(
            scope = backgroundScope,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            write = { _, _ -> FsResult.Error(Exception("provider tolak")) },
            isStillCurrent = { _, _ -> false }
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            coord.events.collect { events += it }
        }
        coord.onChange("uri-x", "isi")
        advanceTimeBy(AUTOSAVE_DEBOUNCE_MS + 10)
        runCurrent()
        assertEquals(listOf<AutosaveCoordinator.Event>(AutosaveCoordinator.Event.Failed("uri-x")), events)
    }

    @Test
    fun `uri beda punya debounce independen`() = runTest {
        val writes = mutableListOf<Pair<String, String>>()
        val coord = AutosaveCoordinator(
            scope = backgroundScope,
            ioDispatcher = StandardTestDispatcher(testScheduler),
            write = { u, c -> writes += u to c; FsResult.Success(Unit) },
            isStillCurrent = { _, _ -> true }
        )
        coord.onChange("a", "va")
        advanceTimeBy(500)
        coord.onChange("b", "vb")
        advanceTimeBy(400) // a sudah 900, b baru 400
        runCurrent()
        assertTrue(writes.contains("a" to "va"))
        assertTrue(!writes.any { it.first == "b" })
        advanceTimeBy(500) // b genap 900
        runCurrent()
        assertTrue(writes.contains("b" to "vb"))
    }
}
