package com.zaaam.editors.core.fs

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SafFileSystem(private val resolver: ContentResolver) : SafFileSystem {
    override suspend fun listChildren(parentUri: Uri): Result<List<FsEntry>> = withContext(Dispatchers.IO) {
        try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentUri, DocumentsContract.getTreeDocumentId(parentUri))
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_FLAGS
            )
            val cursor = resolver.query(childrenUri, projection, null, null, null) ?: return@withContext Result.Success(emptyList())
            val list = mutableListOf<FsEntry>()
            while (cursor.moveToNext()) {
                val id = cursor.getString(0) ?: ""
                val name = cursor.getString(1) ?: ""
                val mime = cursor.getString(2) ?: ""
                val size = cursor.getLong(3)
                val modified = cursor.getLong(4)
                val flags = cursor.getInt(5)
                val childUri = DocumentsContract.buildDocumentUriUsingTree(parentUri, id)
                val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                val isHidden = name.startsWith(".")
                val kind = FileKindResolver().resolve(name)
                list.add(FsEntry(name, childUri, isDir, size, modified, isHidden, kind))
            }
            cursor.close()
            Result.Success(list)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun readText(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            resolver.openInputStream(uri)?.use { it.readText() } ?: ""
        } catch (e: Exception) {
            return@withContext Result.Error(e)
        }
    }

    override suspend fun writeText(uri: Uri, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            resolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun mkdir(parentUri: Uri, name: String): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val newDoc = DocumentsContract.createDocument(resolver, parentUri, DocumentsContract.Document.MIME_TYPE_DIR, name)
            Result.Success(newDoc)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun rename(uri: Uri, newName: String): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            DocumentsContract.renameDocument(resolver, uri, newName)
            Result.Success(uri)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun delete(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            DocumentsContract.deleteDocument(resolver, uri)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}