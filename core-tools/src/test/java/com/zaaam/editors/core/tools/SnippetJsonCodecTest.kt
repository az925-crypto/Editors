package com.zaaam.editors.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnippetJsonCodecTest {

    private fun sample() = Snippet(
        id = "id-1",
        name = "Retrofit \"builder\"",
        language = "kotlin",
        tags = listOf("network", "ret\"ro"),
        code = "val x = 1\nval s = \"back\\slash\"\t<tab>",
        updatedAtMs = 1756108920000
    )

    @Test
    fun `roundtrip mempertahankan semua field termasuk escape sulit`() {
        val json = SnippetJsonCodec.encode(listOf(sample()), exportedAtMs = 1756108920000)
        val result = SnippetJsonCodec.parse(json)

        assertTrue(result is SnippetParseResult.Ok)
        val parsed = (result as SnippetParseResult.Ok).snippets.single()
        assertEquals(sample(), parsed)
        assertEquals(0, result.skippedInvalid)
    }

    @Test
    fun `karakter kontrol di-escape jadi unicode`() {
        val json = SnippetJsonCodec.encode(listOf(Snippet("i", "n", "plaintext", emptyList(), "ab", 1L)))
        assertTrue(json.contains("\\u001f"))
        assertTrue(SnippetJsonCodec.parse(json) is SnippetParseResult.Ok)
    }

    @Test
    fun `schema salah ditolak`() {
        assertEquals(
            SnippetParseResult.BadSchema,
            SnippetJsonCodec.parse("""{"schema":"lain","version":1,"snippets":[]}""")
        )
        assertEquals(
            SnippetParseResult.BadSchema,
            SnippetJsonCodec.parse("""{"schema":"zaaam-snippets","version":2,"snippets":[]}""")
        )
        assertEquals(SnippetParseResult.BadSchema, SnippetJsonCodec.parse("bukan json"))
        assertEquals(SnippetParseResult.BadSchema, SnippetJsonCodec.parse("""{"schema":"zaaam-snippets"}"""))
    }

    @Test
    fun `entry invalid dihitung skipped tanpa merusak lainnya`() {
        val json = """
            {"schema":"zaaam-snippets","version":1,"exportedAt":"x","snippets":[
              {"name":"ok banget","code":"abc"},
              {"name":""},
              {"code":"tanpa nama"},
              "bukan objek",
              {"name":"dua","language":"","tags":["a",5,true],"code":"def"}
            ]}
        """.trimIndent()
        val result = SnippetJsonCodec.parse(json)

        assertTrue(result is SnippetParseResult.Ok)
        val ok = result as SnippetParseResult.Ok
        assertEquals(2, ok.snippets.size)
        assertEquals(3, ok.skippedInvalid)
        assertEquals("plaintext", ok.snippets[0].language) // language kosong → default
        assertEquals(listOf("a"), ok.snippets[1].tags.filterIsInstance<String>())
    }

    @Test
    fun `field tak dikenal diabaikan forward-compat`() {
        val json = """{"schema":"zaaam-snippets","version":1,"extra":{"nested":[1,2]},"snippets":[{"name":"n","code":"c","unknownField":123}]}"""
        val result = SnippetJsonCodec.parse(json)
        assertTrue(result is SnippetParseResult.Ok)
        assertEquals(1, (result as SnippetParseResult.Ok).snippets.size)
    }

    @Test
    fun `json null literal dan nested object tidak bikin parser nyangkut`() {
        // JSON null sah — codec menandai entry tanpa name/code sebagai skipped, bukan crash.
        val json = """{"schema":"zaaam-snippets","version":1,"snippets":[null,{"name":"n","code":"c"}]}"""
        val result = SnippetJsonCodec.parse(json)
        assertTrue(result is SnippetParseResult.Ok)
        assertEquals(1, (result as SnippetParseResult.Ok).snippets.size)
    }
}
