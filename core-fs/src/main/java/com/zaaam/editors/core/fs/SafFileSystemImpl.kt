package com.zaaam.editors.core.fs

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            if (size != null && size > MAX_TEXT_FILE_BYTES) {
                return@withContext FsResult.Error(
                    Exception("File terlalu besar untuk dibuka (${size / (1024 * 1024)}MB, maksimal 2MB)")
                )
            }
            val text = resolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: ""
            FsResult.Success(text)
        } catch (e: Exception) {
            FsResult.Error(e)
        }
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
            resolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
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
