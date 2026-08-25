package com.zaaam.editors.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HexSupportTest {

    @Test
    fun `byteToHex dua digit uppercase`() {
        assertEquals("00", byteToHex(0))
        assertEquals("0A", byteToHex(10))
        assertEquals("FF", byteToHex(0xFF.toByte()))
        assertEquals("80", byteToHex((-128).toByte()))
    }

    @Test
    fun `toAscii printable saja`() {
        assertEquals('A', toAscii(65))
        assertEquals('~', toAscii(126))
        assertEquals(' ', toAscii(32))
        assertNull(toAscii(0))
        assertNull(toAscii(31))
        assertNull(toAscii(127))
    }

    @Test
    fun `formatRow memecah 16 byte per baris`() {
        val bytes = ByteArray(48) { it.toByte() } // 3 baris penuh
        val row0 = formatRow(bytes, 0)
        val row2 = formatRow(bytes, 32)
        val row3 = formatRow(bytes, 48) // lewat batas → kosong

        assertEquals(16, row0.size)
        assertEquals("00", row0[0].hex)
        assertEquals('A', row2[9].ascii) // offset 32+9=41 = 'A'
        assertTrue(row3.isEmpty())
    }

    @Test
    fun `formatRow baris terakhir pendek tanpa meledak`() {
        val bytes = byteArrayOf(0x48, 0x69) // "Hi"
        val row = formatRow(bytes, 0)

        assertEquals(2, row.size)
        assertEquals("48", row[0].hex)
        assertEquals('H', row[0].ascii)
    }
}
