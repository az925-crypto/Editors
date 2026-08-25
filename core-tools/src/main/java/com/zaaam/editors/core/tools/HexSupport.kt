package com.zaaam.editors.core.tools

// Guard ukuran hex editor: seluruh file dimuat ke ByteArray (SAF tidak punya random-access
// write andal — save = tulis balik utuh), jadi batas ini WAJIB. 16MB aman di SD680/8GB.
internal const val MAX_HEX_FILE_BYTES: Long = 16L * 1024 * 1024

// Stack undo byte-edit (index, nilaiLama) — cap mencegah memori membengkak.
internal const val HEX_UNDO_MAX = 32

internal const val HEX_ROW_BYTES = 16

data class ByteCell(val hex: String, val ascii: Char?)

internal fun byteToHex(b: Byte): String {
    val v = b.toInt() and 0xFF
    return "${HEX_DIGITS[v ushr 4]}${HEX_DIGITS[v and 0x0F]}"
}

// Printable ASCII saja; selain itu null → UI menampilkan "·".
internal fun toAscii(b: Byte): Char? {
    val v = b.toInt() and 0xFF
    return if (v in 0x20..0x7E) v.toChar() else null
}

internal fun formatRow(bytes: ByteArray, rowStart: Int): List<ByteCell> {
    val end = minOf(rowStart + HEX_ROW_BYTES, bytes.size)
    if (rowStart >= bytes.size) return emptyList()
    val cells = mutableListOf<ByteCell>()
    for (i in rowStart until end) cells.add(ByteCell(byteToHex(bytes[i]), toAscii(bytes[i])))
    return cells
}

private const val HEX_DIGITS = "0123456789ABCDEF"
