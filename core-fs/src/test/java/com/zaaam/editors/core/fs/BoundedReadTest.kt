package com.zaaam.editors.core.fs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream

// CRITICAL FIX (review): boundary test baca streaming berbatas — file yang ukurannya persis
// di batas harus lolos utuh, dan yang 1 byte lebih harus ditolak SEBELUM byte-nya ditulis,
// supaya file raksasa tidak pernah termuat utuh ke memori (OOM/ANR guard di readText).
// Murni JVM: pakai ByteArrayInputStream, tidak ada konstruksi class android.* di sini.
class BoundedReadTest {

    @Test
    fun `stream kosong menghasilkan array kosong`() {
        val hasil = readBounded(ByteArrayInputStream(ByteArray(0)), limitBytes = 1000L)
        assertEquals(0, hasil.size)
    }

    @Test
    fun `tepat limit lolos utuh`() {
        val data = ByteArray(1000) { it.toByte() }
        val hasil = readBounded(ByteArrayInputStream(data), limitBytes = 1000L)
        assertEquals(1000, hasil.size)
        assertTrue(hasil.contentEquals(data))
    }

    @Test
    fun `limit plus satu melempar exception`() {
        val data = ByteArray(1001)
        try {
            readBounded(ByteArrayInputStream(data), limitBytes = 1000L)
            fail("Harusnya lempar Exception karena total > limitBytes")
        } catch (e: Exception) {
            // Harus kena cabang limit, bukan error lain dari stream.
            assertTrue(e.message!!.contains("terlalu besar"))
        }
    }
}
