package com.zaaam.editors.core.preview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit test JVM murni untuk bagian pure ConsoleBridge (level map, truncate, rate limiter).
// Tanpa konstruksi class android.* — @JavascriptInterface hanya annotation di class bridge,
// yang tidak dikonstruksi di sini.
class ConsoleBridgeSupportTest {

    // --- resolveConsoleLevel ---

    @Test
    fun `level log warn error dipetakan benar`() {
        assertEquals(ConsoleEntry.Level.LOG, resolveConsoleLevel("log"))
        assertEquals(ConsoleEntry.Level.WARN, resolveConsoleLevel("warn"))
        assertEquals(ConsoleEntry.Level.ERROR, resolveConsoleLevel("error"))
    }

    @Test
    fun `level case insensitive`() {
        assertEquals(ConsoleEntry.Level.WARN, resolveConsoleLevel("WARN"))
        assertEquals(ConsoleEntry.Level.ERROR, resolveConsoleLevel("Error"))
        assertEquals(ConsoleEntry.Level.LOG, resolveConsoleLevel("LOG"))
    }

    @Test
    fun `level asing atau null jatuh ke log`() {
        assertEquals(ConsoleEntry.Level.LOG, resolveConsoleLevel(null))
        assertEquals(ConsoleEntry.Level.LOG, resolveConsoleLevel(""))
        assertEquals(ConsoleEntry.Level.LOG, resolveConsoleLevel("info"))
        assertEquals(ConsoleEntry.Level.LOG, resolveConsoleLevel("debug; evil()"))
    }

    // --- truncateConsoleMessage ---

    @Test
    fun `pesan null jadi string kosong`() {
        assertEquals("", truncateConsoleMessage(null))
    }

    @Test
    fun `pesan tepat batas lolos utuh`() {
        val pesan = "x".repeat(MAX_CONSOLE_MESSAGE_CHARS)
        assertEquals(pesan, truncateConsoleMessage(pesan))
    }

    @Test
    fun `pesan lebih dari batas dipotong tanpa throw`() {
        val pesan = "y".repeat(MAX_CONSOLE_MESSAGE_CHARS + 1)
        val hasil = truncateConsoleMessage(pesan)
        assertEquals(MAX_CONSOLE_MESSAGE_CHARS, hasil.length)
        assertTrue(hasil.startsWith("yyy"))
    }

    // --- ConsoleRateLimiter ---

    @Test
    fun `maksimum pesan dalam window lolos lalu dibuang`() {
        var t = 0L
        val limiter = ConsoleRateLimiter(maxMessages = 30, windowMs = 1000, now = { t })
        repeat(30) { assertTrue("pesan ke-${it + 1} harus lolos", limiter.tryAcquire()) }
        assertFalse(limiter.tryAcquire())
    }

    @Test
    fun `kuota pulih setelah window bergeser`() {
        var t = 0L
        val limiter = ConsoleRateLimiter(maxMessages = 30, windowMs = 1000, now = { t })
        repeat(30) { assertTrue(limiter.tryAcquire()) }
        assertFalse(limiter.tryAcquire())
        t += 1001
        assertTrue(limiter.tryAcquire())
    }

    @Test
    fun `sliding window bukan fixed bucket`() {
        // 20 pesan di t=0, lalu 15 usaha di t=600 → hanya 10 lolos (20 masih dalam window).
        var t = 0L
        val limiter = ConsoleRateLimiter(maxMessages = 30, windowMs = 1000, now = { t })
        repeat(20) { assertTrue(limiter.tryAcquire()) }
        t = 600
        repeat(10) { assertTrue(limiter.tryAcquire()) }
        assertFalse(limiter.tryAcquire())
        // Di t=1001, 20 pesan pertama keluar dari window → kuota kembali.
        t = 1001
        assertTrue(limiter.tryAcquire())
    }

    @Test
    fun `limiter dengan nol kuota selalu menolak`() {
        val limiter = ConsoleRateLimiter(maxMessages = 0, windowMs = 1000)
        assertFalse(limiter.tryAcquire())
    }
}
