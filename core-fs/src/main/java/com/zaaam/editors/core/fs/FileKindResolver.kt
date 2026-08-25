package com.zaaam.editors.core.fs

import android.net.Uri

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

sealed interface FsResult<out T> {
    data class Success<T>(val value: T) : FsResult<T>
    data class Error<T>(val exception: Exception) : FsResult<T>
}

interface SafFileSystem {
    suspend fun listChildren(parentUri: Uri): FsResult<List<FsEntry>>
    suspend fun readText(uri: Uri): FsResult<String>
    suspend fun writeText(uri: Uri, text: String): FsResult<Unit>
    suspend fun mkdir(parentUri: Uri, name: String): FsResult<Uri>
    suspend fun rename(uri: Uri, newName: String): FsResult<Uri>
    suspend fun delete(uri: Uri): FsResult<Unit>
}

class TreeAccess(private val contentResolver: android.content.ContentResolver) {
    // SECURITY FIX: dulu selalu minta READ|WRITE sekaligus walau grant aktual dari picker cuma
    // satu flag — provider bisa menolak seluruh permintaan (SecurityException) padahal read
    // saja cukup. Sekarang: coba dua-duanya, kalau ditolak fallback per-flag.
    suspend fun takePersistablePermission(uri: Uri): FsResult<Unit> {
        val bothFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        return try {
            contentResolver.takePersistableUriPermission(uri, bothFlags)
            FsResult.Success(Unit)
        } catch (e: SecurityException) {
            tryTake(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ?: tryTake(uri, android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                ?: FsResult.Error(e)
        }
    }

    private fun tryTake(uri: Uri, flags: Int): FsResult<Unit>? = try {
        contentResolver.takePersistableUriPermission(uri, flags)
        FsResult.Success(Unit)
    } catch (_: SecurityException) {
        null
    }

    suspend fun releasePermission(uri: Uri): FsResult<Unit> {
        return try {
            contentResolver.releasePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            FsResult.Success(Unit)
        } catch (e: SecurityException) {
            FsResult.Error(e)
        }
    }

    fun isPermissionValid(uri: Uri): Boolean {
        return try {
            // SECURITY FIX: dulu cuma cek isReadPermission — revoke write tidak terdeteksi dan
            // autosave gagal diam-diam. Editor butuh baca+tulis, jadi keduanya wajib persisted.
            val perms = contentResolver.persistedUriPermissions.filter { it.uri == uri }
            perms.any { it.isReadPermission } && perms.any { it.isWritePermission }
        } catch (e: Exception) {
            false
        }
    }
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

// Sumber tunggal "uri ini preview-able?" — dulu duplikat identik di EditorViewModel dan
// PreviewViewModel. Kontrak sengaja sama dengan versi lama: ambil substring setelah titik
// terakhir lalu lowercase; uri tanpa query/fragment (SAF content:// memang tidak membawanya).
fun isWebFile(uri: String?): Boolean {
    if (uri == null) return false
    val ext = uri.substringAfterLast(".", "").lowercase()
    return ext in setOf("html", "css", "js")
}

class FileOps {
    data class UndoPayload(val uri: Uri, val oldName: String, val content: String?)
}