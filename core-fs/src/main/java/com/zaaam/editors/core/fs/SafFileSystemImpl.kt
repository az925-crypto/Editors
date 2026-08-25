package com.zaaam.editors.core.fs

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

// CRITICAL FIX (review): guard "boleh dibuka sebagai teks" — NUL byte hampir selalu tanda
// biner, dan byte sequence yang tidak valid UTF-8 berarti decode akan menghasilkan replacement
// char (mangling). File seperti ini DITOLAK sebelum masuk editor: kalau sampai dibuka lalu
// diedit, autosave real akan menulis balik hasil mangling dan merusak file permanen di disk.
// INTERNAL: diekspos untuk unit test (BinaryGuardTest).
internal fun isUsableAsText(bytes: ByteArray): Boolean {
    if (bytes.indexOf(0.toByte()) >= 0) return false
    return try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
        true
    } catch (_: CharacterCodingException) {
        false
    }
}

class SafFileSystemImpl(private val resolver: ContentResolver) : SafFileSystem {

    companion object {
        // CRITICAL 4: batas aman ukuran file teks yang dibuka di editor. Di atas ini,
        // resolver.openInputStream(...).readText() bisa bikin OOM/ANR (mis. file 30MB).
        private const val MAX_TEXT_FILE_BYTES = 2L * 1024 * 1024
    }

    override suspend fun listChildren(parentUri: Uri): FsResult<List<FsEntry>> = withContext(Dispatchers.IO) {
        try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentUri, DocumentsContract.getTreeDocumentId(parentUri))
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )
            val list = mutableListOf<FsEntry>()
            // MEDIUM (cursor leak): pakai use{} supaya cursor selalu ke-close, termasuk kalau
            // ada exception di tengah while-loop — sebelumnya cursor.close() manual di akhir
            // bisa kelewat kalau ada throw duluan.
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0) ?: ""
                    val name = cursor.getString(1) ?: ""
                    val mime = cursor.getString(2) ?: ""
                    val size = cursor.getLong(3)
                    val modified = cursor.getLong(4)
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(parentUri, id)
                    val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                    val isHidden = name.startsWith(".")
                    val kind = FileKindResolver().resolve(name)
                    list.add(FsEntry(name, childUri, isDir, size, modified, isHidden, kind))
                }
            }
            FsResult.Success(list)
        } catch (e: Exception) {
            FsResult.Error(e)
        }
    }

    override suspend fun readText(uri: Uri): FsResult<String> = withContext(Dispatchers.IO) {
        try {
            // CRITICAL 4: cek COLUMN_SIZE dulu sebelum baca isi file. Kalau lebih dari 2MB,
            // tolak dengan pesan error yang jelas alih-alih langsung baca semua ke memori.
            val size = querySize(uri)
            if (size != null && size >= 0 && size > MAX_TEXT_FILE_BYTES) {
                return@withContext FsResult.Error(
                    Exception("File terlalu besar untuk dibuka (${size / (1024 * 1024)}MB, maksimal 2MB)")
                )
            }
            val stream = resolver.openInputStream(uri)
                ?: return@withContext FsResult.Error(Exception("Tidak bisa membuka file"))
            stream.use { ins ->
                // SECURITY/PERF FIX: kalau provider tidak mengisi COLUMN_SIZE (null/negatif),
                // jangan baca buta — baca streaming berbatas: stop di MAX+1 byte lalu tolak.
                // Setara readNBytes(MAX+1), tapi loop manual karena readNBytes baru ada di API 33
                // sedangkan minSdk 26.
                val bytes = readBounded(ins, MAX_TEXT_FILE_BYTES)
                if (!isUsableAsText(bytes)) {
                    return@use FsResult.Error(Exception("File bukan teks — tidak dibuka di editor"))
                }
                FsResult.Success(String(bytes, Charsets.UTF_8))
            }
        } catch (e: Exception) {
            FsResult.Error(e)
        }
    }

    // Bounded read setara InputStream.readNBytes(limit+1): melempar Exception begitu total
    // byte melewati batas, supaya file raksasa tidak pernah termuat utuh ke memori.
    private fun readBounded(stream: java.io.InputStream, limitBytes: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val n = stream.read(buf)
            if (n < 0) break
            total += n
            if (total > limitBytes) {
                throw Exception("File terlalu besar untuk dibuka (maksimal ${limitBytes / (1024 * 1024)}MB)")
            }
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private fun querySize(uri: Uri): Long? {
        val projection = arrayOf(DocumentsContract.Document.COLUMN_SIZE)
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                if (idx >= 0 && !cursor.isNull(idx)) return cursor.getLong(idx)
            }
        }
        return null
    }

    override suspend fun writeText(uri: Uri, text: String): FsResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // HIGH FIX (autosave real): stream null berarti provider menolak menulis — TIDAK
            // boleh dilaporkan Success; caller memakai FsResult ini untuk LED Saved/Error.
            val stream = resolver.openOutputStream(uri)
                ?: return@withContext FsResult.Error(Exception("Tidak bisa membuka file untuk ditulis"))
            stream.use { it.write(text.toByteArray()) }
            FsResult.Success(Unit)
        } catch (e: Exception) {
            FsResult.Error(e)
        }
    }

    override suspend fun mkdir(parentUri: Uri, name: String): FsResult<Uri> = withContext(Dispatchers.IO) {
        try {
            val newDoc = DocumentsContract.createDocument(resolver, parentUri, DocumentsContract.Document.MIME_TYPE_DIR, name)
            if (newDoc != null) FsResult.Success(newDoc) else FsResult.Error(Exception("createDocument returned null"))
        } catch (e: Exception) {
            FsResult.Error(e)
        }
    }

    override suspend fun rename(uri: Uri, newName: String): FsResult<Uri> = withContext(Dispatchers.IO) {
        try {
            val newUri = DocumentsContract.renameDocument(resolver, uri, newName)
            if (newUri != null) FsResult.Success(newUri) else FsResult.Success(uri)
        } catch (e: Exception) {
            FsResult.Error(e)
        }
    }

    override suspend fun delete(uri: Uri): FsResult<Unit> = withContext(Dispatchers.IO) {
        try {
            DocumentsContract.deleteDocument(resolver, uri)
            FsResult.Success(Unit)
        } catch (e: Exception) {
            FsResult.Error(e)
        }
    }
}
