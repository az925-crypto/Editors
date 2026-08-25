package com.zaaam.editors.core.editor

import org.junit.Assert.assertEquals
import org.junit.Test

// Unit test JVM murni untuk LanguageResolver (mapping ekstensi -> scope name).
// Tanpa kelas android.* karena local unit test tidak me-mock framework Android.
class LanguageResolverTest {

    private val resolver = LanguageResolver()

    @Test
    fun `html dipetakan ke text html basic`() {
        assertEquals("text.html.basic", resolver.resolve("index.html"))
    }

    @Test
    fun `css dipetakan ke source css`() {
        assertEquals("source.css", resolver.resolve("style.css"))
    }

    @Test
    fun `js dipetakan ke source js`() {
        assertEquals("source.js", resolver.resolve("main.js"))
    }

    @Test
    fun `kt dipetakan ke source kotlin`() {
        assertEquals("source.kotlin", resolver.resolve("Main.kt"))
    }

    @Test
    fun `kts dipetakan ke source kotlin`() {
        assertEquals("source.kotlin", resolver.resolve("build.gradle.kts"))
    }

    @Test
    fun `py dipetakan ke source python`() {
        assertEquals("source.python", resolver.resolve("app.py"))
    }

    @Test
    fun `json dipetakan ke source json`() {
        assertEquals("source.json", resolver.resolve("data.json"))
    }

    @Test
    fun `ekstensi tak dikenal jatuh ke text plain`() {
        assertEquals("text.plain", resolver.resolve("catatan.xyz"))
    }

    @Test
    fun `nama tanpa titik jatuh ke text plain`() {
        // substringAfterLast dengan delimiter tak ditemukan mengembalikan "" -> else branch.
        assertEquals("text.plain", resolver.resolve("Makefile"))
    }

    @Test
    fun `ekstensi uppercase dilowercase sebelum dicocokkan`() {
        assertEquals("source.kotlin", resolver.resolve("FILE.KT"))
    }
}
