package com.zaaam.editors.core.fs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// CRITICAL FIX (review): menjaga guard anti-korupsi — file non-teks tidak boleh lolos masuk
// editor, karena autosave real akan menulis balik hasil decode yang ter-mangling.
class BinaryGuardTest {

    @Test
    fun `ascii valid diterima`() {
        assertTrue(isUsableAsText("hello world\n".toByteArray()))
    }

    @Test
    fun `utf8 multibyte valid diterima`() {
        assertTrue(isUsableAsText("kód ünïcode 日本語 ✓".toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun `file kosong diterima`() {
        assertTrue(isUsableAsText(ByteArray(0)))
    }

    @Test
    fun `nul byte ditolak`() {
        assertFalse(isUsableAsText(byteArrayOf(0x68, 0x65, 0x00, 0x6C)))
    }

    @Test
    fun `elf magic ditolak`() {
        val elf = byteArrayOf(0x7F.toByte(), 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte())
        assertFalse(isUsableAsText(elf))
    }

    @Test
    fun `byte invalid utf8 ditolak`() {
        // 0xFF dan 0xE9 (latin-1 é) bukan sequence UTF-8 valid.
        assertFalse(isUsableAsText(byteArrayOf(0xFF.toByte())))
        assertFalse(isUsableAsText(byteArrayOf(0xE9.toByte())))
    }

    @Test
    fun `utf16 le dengan nul interleave ditolak`() {
        val utf16 = "teks".toByteArray(Charsets.UTF_16LE)
        assertFalse(isUsableAsText(utf16))
    }
}
