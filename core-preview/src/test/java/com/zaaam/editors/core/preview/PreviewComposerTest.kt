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

    // --- Fase 4: instrumentasi console ---

    @Test
    fun `payload js memakai bridge dua argumen tanpa json stringify`() {
        val hasil = PreviewComposer.compose(htmlDenganPlaceholder(), null, "x();")

        assertTrue(hasil.contains("<script>window.__zaaam_bridge"))
        assertTrue(hasil.contains("ZaaamBridge.postMessage(String(l),String(m))"))
        assertFalse(hasil.contains("JSON.stringify"))
    }

    @Test
    fun `instrumentasi mengoverride log warn error dan listener error`() {
        val hasil = PreviewComposer.compose(htmlDenganPlaceholder(), null, "")

        assertTrue(hasil.contains("console.log=function(){send(\"log\",arguments)"))
        assertTrue(hasil.contains("console.warn=function(){send(\"warn\",arguments)"))
        assertTrue(hasil.contains("console.error=function(){send(\"error\",arguments)"))
        assertTrue(hasil.contains("window.addEventListener(\"error\""))
        assertTrue(hasil.contains("\"preview siap\""))
    }

    @Test
    fun `js user tetap segmen terakhir sebelum penutup script`() {
        val hasil = PreviewComposer.compose(htmlDenganPlaceholder(), null, "a();")

        val posUser = hasil.indexOf("a();")
        val posTutup = hasil.lastIndexOf("</script>")
        assertTrue(posUser in 0 until posTutup)
        // Tidak ada kode instrumentasi SETELAH js user.
        assertFalse(hasil.substring(posUser).contains("__zaaam_bridge = {"))
    }

    // --- Fase 4: fallback append saat placeholder absen ---

    @Test
    fun `css tanpa placeholder di-append di akhir dokumen`() {
        val htmlPolos = "<html><body><h1>Halo</h1></body></html>"
        val hasil = PreviewComposer.compose(htmlPolos, "p { margin: 0 }", null)

        assertFalse(hasil.contains("""<link rel="""))
        assertTrue(hasil.endsWith("<style>p { margin: 0 }</style>"))
    }

    @Test
    fun `js tanpa placeholder di-append di akhir dokumen`() {
        val htmlPolos = "<html><body><h1>Halo</h1></body></html>"
        val hasil = PreviewComposer.compose(htmlPolos, null, "doIt();")

        assertTrue(hasil.endsWith("doIt();</script>"))
        assertTrue(hasil.contains("window.__zaaam_bridge"))
        assertTrue(hasil.contains("doIt();"))
    }

    @Test
    fun `fallback append tidak aktif kalau placeholder ketemu`() {
        val hasil = PreviewComposer.compose(htmlDenganPlaceholder(), "a { color: red }", "b();")

        // Hanya SATU blok style/script hasil replace — tidak ada duplikat append.
        assertEquals(1, Regex("<style>").findAll(hasil).count())
        assertEquals(1, Regex("<script>").findAll(hasil).count())
        assertEquals(1, Regex("</style>").findAll(hasil).count())
    }

    @Test
    fun `compose tanpa keduanya tetap identitas termasuk tanpa fallback`() {
        val htmlPolos = "<html><body>x</body></html>"
        assertEquals(htmlPolos, PreviewComposer.compose(htmlPolos, null, null))
    }

    // --- Fase 4: dokumen standalone untuk file .css / .js langsung ---

    @Test
    fun `wrapStandaloneDocument css membungkus style dan terescape`() {
        val hasil = PreviewComposer.wrapStandaloneDocument(
            PreviewComposer.StandaloneKind.CSS,
            """p::after { content: "</STYLE>" }"""
        )

        assertTrue(hasil.startsWith("<!DOCTYPE html>"))
        assertTrue(hasil.contains("<style>"))
        assertFalse(hasil.contains("</STYLE>"))
        assertTrue(hasil.contains("<\\/STYLE>"))
    }

    @Test
    fun `wrapStandaloneDocument js membawa bridge dan terescape`() {
        val hasil = PreviewComposer.wrapStandaloneDocument(
            PreviewComposer.StandaloneKind.JS,
            "var s = '</ScRiPt>';"
        )

        assertTrue(hasil.startsWith("<!DOCTYPE html>"))
        assertTrue(hasil.contains("window.__zaaam_bridge"))
        assertFalse(hasil.contains("</ScRiPt>"))
        assertTrue(hasil.contains("<\\/ScRiPt>"))
    }
}
