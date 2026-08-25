package com.zaaam.editors.core.fs

import android.net.Uri
import kotlinx.coroutines.flow.Flow

data class FsEntry(
    val name: String,
    val uri: Uri,
    val isDir: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0,
    val isHidden: Boolean = false,
    val kind: Kind = Kind.CONFIG
)

enum class Kind {
    WEB, CODE, CONFIG, BINARY
}

interface SafFileSystem {
    suspend fun listChildren(parentUri: Uri): Result<List<FsEntry>>
    suspend fun readText(uri: Uri): Result<String>
    suspend fun writeText(uri: Uri, text: String): Result<Unit>
    suspend fun mkdir(parentUri: Uri, name: String): Result<Uri>
    suspend fun rename(uri: Uri, newName: String): Result<Uri>
    suspend fun delete(uri: Uri): Result<Unit>
}

sealed interface Result<out T> {
    data class Success<T>(val value: T) : Result<T>
    data class Error<T>(val exception: Exception) : Result<T>
}

class TreeAccess(private val contentResolver: android.content.ContentResolver) {
    suspend fun takePersistablePermission(uri: Uri) = Result.Success(Unit)
    suspend fun releasePermission(uri: Uri) = Result.Success(Unit)
    fun isPermissionValid(uri: Uri) = true
}

class HiddenFiles {
    fun isHidden(name: String): Boolean = name.startsWith(".")
    fun filter(list: List<FsEntry>, showHidden: Boolean): List<FsEntry> =
        if (showHidden) list else list.filter { !it.isHidden }
}

class FileKindResolver {
    fun resolve(name: String): Kind {
        val ext = name.substringAfterLast(".", "").lowercase()
        return when (ext) {
            "html", "css", "js" -> Kind.WEB
            "kt", "kts", "py", "json", "md", "xml", "yaml", "yml", "gradle", "java" -> Kind.CODE
            "apk", "jpg", "jpeg", "png", "gif", "webp", "pdf", "zip", "jar", "aar" -> Kind.BINARY
            else -> Kind.CONFIG
        }
    }

    fun stencilLabel(name: String): String {
        val ext = name.substringAfterLast(".", "").lowercase()
        return when (ext) {
            "html" -> "HT"
            "css" -> "CS"
            "js" -> "JS"
            "kt", "kts" -> "KT"
            "py" -> "PY"
            "json" -> "JN"
            "md" -> "MD"
            "xml" -> "XL"
            "yaml", "yml" -> "YM"
            "gradle" -> "GR"
            "java" -> "JV"
            "apk" -> "AP"
            "jpg", "jpeg" -> "IM"
            "png" -> "IM"
            "gif" -> "IM"
            "webp" -> "IM"
            "pdf" -> "PF"
            "zip" -> "ZP"
            "jar" -> "JR"
            "aar" -> "AR"
            else -> "FT"
        }
    }
}

class FileOps {
    data class UndoPayload(val uri: Uri, val oldName: String, val content: String?)
}