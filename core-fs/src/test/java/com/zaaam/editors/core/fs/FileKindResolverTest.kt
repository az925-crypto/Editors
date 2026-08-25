package com.zaaam.editors.core.fs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit test JVM murni untuk FileKindResolver dan HiddenFiles.isHidden.
// HiddenFiles.filter sengaja tidak dites karena butuh FsEntry (android.net.Uri)
// yang tidak boleh dikonstruksi di local unit test JVM.
class FileKindResolverTest {

    private val resolver = FileKindResolver()
    private val hiddenFiles = HiddenFiles()

    // --- FileKindResolver.resolve ---

    @Test
    fun `resolve memetakan html css js ke web`() {
        assertEquals(Kind.WEB, resolver.resolve("index.html"))
        assertEquals(Kind.WEB, resolver.resolve("style.css"))
        assertEquals(Kind.WEB, resolver.resolve("main.js"))
    }

    @Test
    fun `resolve memetakan semua ekstensi kode ke code`() {
        assertEquals(Kind.CODE, resolver.resolve("Main.kt"))
        assertEquals(Kind.CODE, resolver.resolve("build.gradle.kts"))
        assertEquals(Kind.CODE, resolver.resolve("app.py"))
        assertEquals(Kind.CODE, resolver.resolve("data.json"))
        assertEquals(Kind.CODE, resolver.resolve("README.md"))
        assertEquals(Kind.CODE, resolver.resolve("layout.xml"))
        assertEquals(Kind.CODE, resolver.resolve("config.yaml"))
        assertEquals(Kind.CODE, resolver.resolve("config.yml"))
        assertEquals(Kind.CODE, resolver.resolve("build.gradle"))
        assertEquals(Kind.CODE, resolver.resolve("App.java"))
    }

    @Test
    fun `resolve memetakan semua ekstensi biner ke binary`() {
        assertEquals(Kind.BINARY, resolver.resolve("app.apk"))
        assertEquals(Kind.BINARY, resolver.resolve("foto.jpg"))
        assertEquals(Kind.BINARY, resolver.resolve("foto.jpeg"))
        assertEquals(Kind.BINARY, resolver.resolve("logo.png"))
        assertEquals(Kind.BINARY, resolver.resolve("anim.gif"))
        assertEquals(Kind.BINARY, resolver.resolve("wallpaper.webp"))
        assertEquals(Kind.BINARY, resolver.resolve("dokumen.pdf"))
        assertEquals(Kind.BINARY, resolver.resolve("arsip.zip"))
        assertEquals(Kind.BINARY, resolver.resolve("lib.jar"))
        assertEquals(Kind.BINARY, resolver.resolve("lib.aar"))
    }

    @Test
    fun `resolve ekstensi lain jatuh ke config`() {
        assertEquals(Kind.CONFIG, resolver.resolve("notes.txt"))
        assertEquals(Kind.CONFIG, resolver.resolve("arsip.7z"))
    }

    @Test
    fun `resolve ekstensi uppercase readme md jadi code`() {
        assertEquals(Kind.CODE, resolver.resolve("README.MD"))
    }

    // --- FileKindResolver.stencilLabel ---

    @Test
    fun `stencilLabel mengembalikan singkatan untuk web dan kode`() {
        assertEquals("HT", resolver.stencilLabel("index.html"))
        assertEquals("CS", resolver.stencilLabel("style.css"))
        assertEquals("JS", resolver.stencilLabel("main.js"))
        assertEquals("KT", resolver.stencilLabel("Main.kt"))
        assertEquals("PY", resolver.stencilLabel("app.py"))
        assertEquals("JN", resolver.stencilLabel("data.json"))
        assertEquals("MD", resolver.stencilLabel("README.md"))
    }

    @Test
    fun `stencilLabel mengembalikan singkatan untuk binary`() {
        assertEquals("AP", resolver.stencilLabel("app.apk"))
        assertEquals("IM", resolver.stencilLabel("foto.jpg"))
        assertEquals("PF", resolver.stencilLabel("dokumen.pdf"))
        assertEquals("ZP", resolver.stencilLabel("arsip.zip"))
    }

    @Test
    fun `stencilLabel ekstensi tak dikenal mengembalikan ft`() {
        assertEquals("FT", resolver.stencilLabel("notes.txt"))
        assertEquals("FT", resolver.stencilLabel("Makefile"))
    }

    // --- HiddenFiles.isHidden ---

    @Test
    fun `isHidden mendeteksi nama diawali titik`() {
        assertTrue(hiddenFiles.isHidden(".gitignore"))
        assertTrue(hiddenFiles.isHidden(".env"))
    }

    @Test
    fun `isHidden nama biasa tidak dianggap tersembunyi`() {
        assertFalse(hiddenFiles.isHidden("file.txt"))
        assertFalse(hiddenFiles.isHidden("a.b.c"))
    }

    // --- isWebFile (top-level) ---

    @Test
    fun `isWebFile mendeteksi ekstensi web`() {
        assertTrue(isWebFile("content://x/yy/index.html"))
        assertTrue(isWebFile("content://x/yy/style.css"))
        assertTrue(isWebFile("content://x/yy/main.js"))
    }

    @Test
    fun `isWebFile case insensitive`() {
        assertTrue(isWebFile("INDEX.HTML"))
        assertTrue(isWebFile("Style.CSS"))
    }

    @Test
    fun `isWebFile uri null dan non-web false`() {
        assertFalse(isWebFile(null))
        assertFalse(isWebFile("notes.txt"))
        assertFalse(isWebFile("Main.kt"))
        assertFalse(isWebFile("tanpa-ekstensi"))
    }
}
