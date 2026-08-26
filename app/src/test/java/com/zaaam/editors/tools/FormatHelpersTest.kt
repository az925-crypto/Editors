package com.zaaam.editors.tools

import com.zaaam.editors.session.clampHighlight
import com.zaaam.editors.session.exportFileName
import com.zaaam.editors.session.humanBytes
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatHelpersTest {

    @Test
    fun `humanBytes skala penuh`() {
        assertEquals("0 B", humanBytes(-5))
        assertEquals("0 B", humanBytes(0))
        assertEquals("512 B", humanBytes(512))
        assertEquals("1.5 KB", humanBytes(1536))
        assertEquals("1.0 MB", humanBytes(1024L * 1024))
        assertEquals("250.0 MB", humanBytes(250L * 1024 * 1024))
        // 5 GB → tidak naik unit lagi (cap GB)
        assertEquals("5.0 GB", humanBytes(5L * 1024 * 1024 * 1024))
    }

    @Test
    fun `clampHighlight jaga range di dalam lineText`() {
        assertEquals(10 to 16, clampHighlight(10, 16, 16))
        // Multi-baris: endInLine bisa > panjang baris → dipatok ke len.
        assertEquals(1 to 3, clampHighlight(1, 9, 3))
        // Start di luar (korup) → dipatok; end tak pernah < start.
        assertEquals(2 to 2, clampHighlight(7, 9, 2))
        assertEquals(0 to 0, clampHighlight(-3, -1, 0))
    }
}
