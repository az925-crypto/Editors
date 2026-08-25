package com.zaaam.editors.core.preview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit test JVM murni untuk PreviewComposer. Tanpa kelas android.* karena
// local unit test tidak me-mock framework Android.
class PreviewComposerTest {

    private fun htmlDenganPlaceholder(): String = """
        <!DOCTYPE html>
        <html>
          <head>
            <link rel="stylesheet" href="style.css">
            <script src="main.js"></script>
          </head>
          <body><h1>Halo</h1></body>
        </html>
    """.trimIndent()

    @Test
    fun `compose dengan css mengganti link placeholder jadi style inline`() {
        val hasil = PreviewComposer.compose(htmlDenganPlaceholder(), "body { color: red; }", null)

        assertFalse(hasil.contains("""<link rel="stylesheet" href="style.css">"""))
        assertTrue(hasil.contains("<style>body { color: red; }</style>"))
    }

    @Test
    fun `compose dengan js mengganti src placeholder jadi bridge dan script inline`() {
        val hasil = PreviewComposer.compose(htmlDenganPlaceholder(), null, "console.log('halo');")

        assertFalse(hasil.contains("""<script src="main.js"></script>"""))
        assertTrue(hasil.contains("<script>window.__zaaam_bridge"))
        assertTrue(hasil.contains("console.log('halo');"))
        assertTrue(hasil.contains("</script>"))
    }

    @Test
    fun `css berisi penutup style uppercase terescape`() {
        val hasil = PreviewComposer.compose(htmlDenganPlaceholder(), "a { color: blue } </STYLE>", null)

        // Casing asli dari input user dipertahankan, hanya disisipkan backslash sebelum slash.
        // Wrapper sendiri pakai huruf kecil, jadi </STYLE> uppercase hanya mungkin dari input user.
        assertFalse(hasil.contains("</STYLE>"))
        assertTrue(hasil.contains("<\\/STYLE>"))

        // Varian lowercase dari konten user juga harus terescape. Dicek per blok hasil injeksi
        // karena penutup </style> milik wrapper sendiri sah ada di dokumen.
        val hasil2 = PreviewComposer.compose(htmlDenganPlaceholder(), """p::after { content: "</style>" }""", null)
        assertTrue(hasil2.contains("""<style>p::after { content: "<\/style>" }</style>"""))
    }

    @Test
    fun `js berisi penutup script mixed case terescape`() {
        val js = "var s = '</ScRiPt>';"
        val hasil = PreviewComposer.compose(htmlDenganPlaceholder(), null, js)

        assertFalse(hasil.contains("</ScRiPt>"))
        assertTrue(hasil.contains("<\\/ScRiPt>"))
    }

    @Test
    fun `js dengan beberapa penutup script semuanya terescape`() {
        val hasil = PreviewComposer.compose(htmlDenganPlaceholder(), null, "a();</ScRiPt>b();</SCRIPT>")

        assertFalse(hasil.contains("</ScRiPt>"))
        assertFalse(hasil.contains("</SCRIPT>"))
        // Blok persis membuktikan dua okurensi terescape dan wrapper tetap menutup normal.
        assertTrue(hasil.contains("<script>window.__zaaam_bridge"))
        assertTrue(hasil.contains("a();<\\/ScRiPt>b();<\\/SCRIPT></script>"))
    }

    @Test
    fun `compose tanpa css dan js mengembalikan html persis`() {
        val html = htmlDenganPlaceholder()
        // Html yang mengandung placeholder juga tidak boleh berubah kalau fragmen kosong.
        assertEquals(html, PreviewComposer.compose(html, null, null))
    }

    @Test
    fun `compose css dan js bersamaan mengganti kedua placeholder`() {
        val hasil = PreviewComposer.compose(
            htmlDenganPlaceholder(),
            "b { font-weight: bold; }",
            "alert('x');"
        )

        assertFalse(hasil.contains("""<link rel="stylesheet" href="style.css">"""))
        assertFalse(hasil.contains("""<script src="main.js"></script>"""))
        assertTrue(hasil.contains("<style>b { font-weight: bold; }</style>"))
        assertTrue(hasil.contains("alert('x');"))
        assertTrue(hasil.contains("window.__zaaam_bridge"))
    }
}
