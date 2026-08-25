package com.zaaam.editors.ui.files

import com.zaaam.editors.core.fs.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Menjaga parser recents dari regresi: format prefs "uri|name|kind" dengan nama file yang
// boleh mengandung "|" (parser first/last-pipe, bukan split).
class RecentsParserTest {

    @Test
    fun `format standar terurai benar`() {
        val r = parseRecentEntry("content://x/y/catatan.md|catatan.md|CODE")
        assertEquals("content://x/y/catatan.md", r?.uri)
        assertEquals("catatan.md", r?.name)
        assertEquals(Kind.CODE, r?.kind)
    }

    @Test
    fun `nama berpipe tidak merusak uri dan kind`() {
        val r = parseRecentEntry("content://x/y|na|me.md|WEB")
        assertEquals("content://x/y", r?.uri)
        assertEquals("na|me.md", r?.name)
        assertEquals(Kind.WEB, r?.kind)
    }

    @Test
    fun `tanpa separator null`() {
        assertNull(parseRecentEntry("content://x/y"))
    }

    @Test
    fun `satu separator null`() {
        assertNull(parseRecentEntry("content://x/y|nama"))
    }

    @Test
    fun `string kosong null`() {
        assertNull(parseRecentEntry(""))
    }

    @Test
    fun `kind tak dikenal fallback CONFIG`() {
        assertEquals(Kind.CONFIG, parseRecentEntry("content://x/y|file|WADUH")?.kind)
    }

    @Test
    fun `semua nilai kind valid terurai`() {
        Kind.entries.forEach { k ->
            assertEquals(k, parseRecentEntry("u|n|$k")?.kind)
        }
    }
}
