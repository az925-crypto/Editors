package com.zaaam.editors.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// Kontrak assets TextMate vs pembaca sora 0.23.6 (LanguageDefinitionReader):
// tiap entri WAJIB memakai kunci "grammar" (+ name, scopeName) — BUKAN "path" — dan
// nilainya path asset valid TANPA prefix "./" (AssetManager.open tidak normalisasi).
//
// Regresi v0.2.0: salah kunci membuat NPE di COLD START hanya di device (CI tidak pernah
// menjalankan app) → seluruh proses mati sebelum UI. Test ini menjaga formatnya di CI.
class TextMateAssetsContractTest {

    private val assetsDir = File("src/main/assets")

    private fun languagesJson(): String = File(assetsDir, "textmate/languages.json").readText()

    @Test
    fun `languages json memakai kunci grammar bukan path`() {
        val text = languagesJson()
        assertFalse("kunci 'path' dilarang — sora membaca 'grammar'", text.contains("\"path\""))
        assertTrue("harus ada kunci 'grammar'", text.contains("\"grammar\""))
        assertTrue("harus dibungkus objek {\"languages\": [...]}", text.contains("\"languages\""))
    }

    @Test
    fun `tiap grammar path valid dan benar-benar menunjuk file asset`() {
        val paths = Regex("\"grammar\"\\s*:\\s*\"([^\"]+)\"")
            .findAll(languagesJson())
            .map { it.groupValues[1] }
            .toList()
        assertTrue("minimal 7 bahasa terdaftar, aktual=${paths.size}", paths.size >= 7)
        paths.forEach { p ->
            assertFalse("path tidak boleh berprefix ./ atau / : $p", p.startsWith("./") || p.startsWith("/"))
            assertTrue("file grammar tidak ditemukan: $p", File(assetsDir, p).isFile)
        }
    }

    @Test
    fun `scopeName unik antar entri`() {
        // GrammarRegistry mendaftar by scopeName — dobel bikin apply language ambigu.
        val scopes = Regex("\"scopeName\"\\s*:\\s*\"([^\"]+)\"")
            .findAll(languagesJson())
            .map { it.groupValues[1] }
            .toList()
        assertEquals(scopes.size, scopes.toSet().size)
    }

    @Test
    fun `theme retro-lcd tersedia`() {
        assertTrue(File(assetsDir, "textmate/themes/retro-lcd.json").isFile)
    }
}
