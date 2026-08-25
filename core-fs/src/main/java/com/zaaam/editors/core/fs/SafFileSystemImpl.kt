package com.zaaam.editors.core.fs

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SafFileSystemImpl(private val resolver: ContentResolver) : SafFileSystem {
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
            val cursor = resolver.query(childrenUri, projection, null, null, null) ?: return@withContext FsResult.Success(emptyList())
            val list = mutableListOf<FsEntry>()
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
            cursor.close()
            FsResult.Success(list)
        } catch (e: Exception) {
            FsResult.Error(e)
        }
    }

    override suspend fun readText(uri: Uri): FsResult<String> = withContext(Dispatchers.IO) {
        try {
            val text = resolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: ""
            FsResult.Success(text)
        } catch (e: Exception) {
            FsResult.Error(e)
        }
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
            DocumentsContract.renameDocument(resolver, uri, newName)
            FsResult.Success(uri)
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