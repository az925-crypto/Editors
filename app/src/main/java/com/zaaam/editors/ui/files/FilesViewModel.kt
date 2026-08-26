package com.zaaam.editors.ui.files

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.editors.core.editor.TabState
import com.zaaam.editors.core.fs.FsEntry
import com.zaaam.editors.core.fs.FsResult
import com.zaaam.editors.core.fs.Kind
import com.zaaam.editors.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RecentFile(
    val uri: String,
    val name: String,
    val kind: Kind
)

// SECURITY: pesan error untuk UI dibuat generik — exception.message bisa berisi path internal
// provider atau detail sistem yang tidak boleh tampil ke user.
private const val MSG_OPEN_FAILED = "Gagal membuka file"
private const val MSG_LIST_FAILED = "Gagal memuat folder"

data class FilesUiState(
    val treeUri: Uri? = null,
    val currentUri: Uri? = null,
    val pathSegments: List<String> = emptyList(),
    val entries: List<FsEntry> = emptyList(),
    val query: String = "",
    val showHidden: Boolean = false,
    val isLoading: Boolean = false,
    val isPicking: Boolean = false,
    val permDenied: Boolean = false,
    val safError: String? = null,
    val showSafDialog: Boolean = false,
    val hasDismissedSaf: Boolean = false,
    val recents: List<RecentFile> = emptyList(),
    val listError: String? = null
)

@OptIn(FlowPreview::class)
class FilesViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    // CRITICAL 2: pathStack HANYA boleh disentuh dari Main thread. Semua fungsi di bawah
    // memodifikasinya secara sinkron di badan fungsi (dijalankan Main lewat viewModelScope
    // default dispatcher), dan IO cuma dipakai lewat withContext di dalam loadChildren.
    private val pathStack = mutableListOf<Uri>()

    // CRITICAL 2: generasi + job dipakai supaya hasil loadChildren yang basi (folder yang
    // sudah ditinggalkan user) tidak pernah ke-apply ke state, biar entries tidak ketuker.
    private var loadJob: Job? = null
    private var loadGeneration = 0

    init {
        viewModelScope.launch {
            queryFlow.debounce(200).collect { q ->
                _uiState.update { it.copy(query = q) }
            }
        }
        loadRecents()
        restoreTreeUri()
    }

    fun onQueryChange(q: String) {
        queryFlow.value = q
    }

    fun toggleHidden() {
        _uiState.update { it.copy(showHidden = !it.showHidden) }
    }

    fun onTreeUriSelected(uri: Uri) {
        _uiState.update { it.copy(isPicking = true, safError = null) }
        viewModelScope.launch {
            when (val result = withContext(Dispatchers.IO) { container.treeAccess.takePersistablePermission(uri) }) {
                is FsResult.Success -> {
                    // MEDIUM FIX (review): persist uri hanya kalau read+write benar-benar
                    // ter-persist. Grant read-only hasil fallback per-flag memang bisa lanjut
                    // dipakai sesi ini, tapi kalau ikut disimpan, isPermissionValid (yang butuh
                    // read+write untuk autosave) akan gagal tiap cold start — dialog SAF muncul
                    // berulang tanpa pernah bisa restore.
                    val restorable = withContext(Dispatchers.IO) { container.treeAccess.isPermissionValid(uri) }
                    if (restorable) {
                        container.prefs.edit().putString("saf_tree_uri", uri.toString()).apply()
                    }
                    // Main thread saja.
                    pathStack.clear()
                    pathStack.add(uri)
                    _uiState.update {
                        it.copy(
                            treeUri = uri,
                            currentUri = uri,
                            pathSegments = listOf(getDisplayName(uri)),
                            isPicking = false,
                            permDenied = false,
                            showSafDialog = false,
                            hasDismissedSaf = false,
                            safError = null
                        )
                    }
                    loadChildren(uri)
                }
                is FsResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isPicking = false,
                            safError = "Akses ditolak — coba pilih folder lagi"
                        )
                    }
                }
            }
        }
    }

    fun onPickerCancelled() {
        _uiState.update { it.copy(isPicking = false) }
    }

    fun dismissSaf() {
        _uiState.update {
            it.copy(showSafDialog = false, hasDismissedSaf = true, permDenied = true)
        }
    }

    fun showSafDialog() {
        _uiState.update { it.copy(showSafDialog = true) }
    }

    fun navigateInto(entry: FsEntry) {
        if (!entry.isDir) return
        // Main thread saja — tidak ada lagi launch(Dispatchers.IO) yang membungkus mutasi ini.
        pathStack.add(entry.uri)
        _uiState.update {
            it.copy(
                currentUri = entry.uri,
                pathSegments = it.pathSegments + entry.name,
                isLoading = true
            )
        }
        loadChildren(entry.uri)
    }

    fun navigateToSegment(index: Int) {
        // MEDIUM FIX (80841fd re-audit): index < 0 (mis. indexOf yang gagal match) sebelumnya
        // lolos guard lalu pathStack[-1] -> IndexOutOfBoundsException crash.
        if (index < 0 || index >= pathStack.size) return
        val uri = pathStack[index]
        pathStack.subList(index + 1, pathStack.size).clear()
        _uiState.update {
            it.copy(
                currentUri = uri,
                pathSegments = it.pathSegments.take(index + 1),
                isLoading = true
            )
        }
        loadChildren(uri)
    }

    fun navigateUp(): Boolean {
        if (pathStack.size <= 1) return false
        pathStack.removeAt(pathStack.lastIndex)
        val uri = pathStack.last()
        _uiState.update {
            it.copy(
                currentUri = uri,
                pathSegments = it.pathSegments.dropLast(1),
                isLoading = true
            )
        }
        loadChildren(uri)
        return true
    }

    fun openFile(entry: FsEntry) {
        if (entry.isDir) {
            navigateInto(entry)
            return
        }
        viewModelScope.launch {
            if (entry.kind == Kind.BINARY) {
                // Rider Phase 2: file biner TIDAK lagi bikin tab editor dummy — masuk
                // hex editor (container.hexTargetUri di-consume HexScreen). Autosave tetap
                // aman karena tab biner tidak pernah dibuat.
                container.hexTargetUri.value = entry.uri.toString()
                container.toolsTab.value = com.zaaam.editors.session.ToolsTab.HEX
                container.screenState.value = com.zaaam.editors.session.AppScreen.TOOLS
                return@launch
            }
            when (val result = container.fileSystem.readText(entry.uri)) {
                is FsResult.Success -> {
                    // CRITICAL 3: tulis isi file ke map bersama SEBELUM addTab, supaya waktu
                    // EditorViewModel baca tab yang baru dibuka, isinya sudah ada — bukan tab kosong.
                    container.editorContents[entry.uri.toString()] = result.value
                    container.editorSession.addTab(
                        TabState(entry.uri.toString(), entry.name)
                    )
                    container.screenState.value = com.zaaam.editors.session.AppScreen.EDITOR
                    upsertRecent(entry)
                }
                is FsResult.Error -> {
                    // SECURITY: jangan bocorkan exception.message (bisa berisi path/internal
                    // provider) — cukup pesan generik.
                    _uiState.update { it.copy(listError = MSG_OPEN_FAILED) }
                }
            }
        }
    }

    fun openRecent(recent: RecentFile) {
        viewModelScope.launch {
            // BACKLOG #4: recents dibaca dari SharedPreferences yang bisa basi/diubah luar —
            // validasi scheme sebelum URI dipakai, dan tab BINARY tidak pernah readText.
            if (!recent.uri.startsWith("content://")) {
                _uiState.update { it.copy(listError = MSG_OPEN_FAILED) }
                return@launch
            }
            if (recent.kind == Kind.BINARY) {
                // Rider Phase 2: sama seperti openFile — biner ke hex editor.
                container.hexTargetUri.value = recent.uri
                container.toolsTab.value = com.zaaam.editors.session.ToolsTab.HEX
                container.screenState.value = com.zaaam.editors.session.AppScreen.TOOLS
                return@launch
            }
            when (val result = container.fileSystem.readText(Uri.parse(recent.uri))) {
                is FsResult.Success -> {
                    // CRITICAL 3: sama seperti openFile — isi map bersama dulu baru addTab.
                    container.editorContents[recent.uri] = result.value
                    container.editorSession.addTab(
                        TabState(recent.uri, recent.name)
                    )
                    container.screenState.value = com.zaaam.editors.session.AppScreen.EDITOR
                }
                is FsResult.Error -> {
                    _uiState.update { it.copy(listError = MSG_OPEN_FAILED) }
                }
            }
        }
    }

    fun clearListError() {
        _uiState.update { it.copy(listError = null) }
    }

    fun setPermDenied(v: Boolean) {
        _uiState.update { it.copy(permDenied = v) }
    }

    // MEDIUM: sekarang menerima state secara eksplisit supaya bisa dipanggil dari dalam
    // derivedStateOf di FilesScreen (baca lewat State snapshot, bukan _uiState.value mentah),
    // jadi hasilnya cuma dihitung ulang saat entries/showHidden/query beneran berubah.
    fun filteredEntries(state: FilesUiState = _uiState.value): List<FsEntry> {
        var list = state.entries
        if (!state.showHidden) list = list.filter { !it.isHidden }
        if (state.query.isNotBlank()) {
            val q = state.query.lowercase()
            list = list.filter { it.name.lowercase().contains(q) }
        }
        return list
    }

    private fun loadChildren(parentUri: Uri) {
        // CRITICAL 2: batalkan request folder sebelumnya yang masih jalan, dan tandai generasi
        // baru supaya kalaupun request lama sempat selesai duluan, hasilnya tetap dibuang.
        loadJob?.cancel()
        val generation = ++loadGeneration
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, listError = null) }
            val result = withContext(Dispatchers.IO) { container.fileSystem.listChildren(parentUri) }

            // Kalau user sudah pindah folder lagi sebelum request ini kelar, buang hasilnya.
            if (generation != loadGeneration || _uiState.value.currentUri != parentUri) {
                return@launch
            }

            when (result) {
                is FsResult.Success -> {
                    val sorted = result.value.sortedWith(
                        compareByDescending<FsEntry> { it.isDir }.thenBy { it.name.lowercase() }
                    )
                    _uiState.update {
                        it.copy(entries = sorted, isLoading = false, permDenied = false)
                    }
                }
                is FsResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            permDenied = true,
                            listError = MSG_LIST_FAILED
                        )
                    }
                }
            }
        }
    }

    private fun upsertRecent(entry: FsEntry) {
        val current = _uiState.value.recents.toMutableList()
        current.removeAll { it.uri == entry.uri.toString() }
        current.add(0, RecentFile(entry.uri.toString(), entry.name, entry.kind))
        val trimmed = current.take(5)
        _uiState.update { it.copy(recents = trimmed) }
        saveRecents(trimmed)
    }

    private fun saveRecents(recents: List<RecentFile>) {
        val set = recents.map { "${it.uri}|${it.name}|${it.kind}" }.toSet()
        container.prefs.edit().putStringSet("recent_files", set).apply()
    }

    private fun loadRecents() {
        val set = container.prefs.getStringSet("recent_files", emptySet()) ?: emptySet()
        val recents = set.mapNotNull { entry -> parseRecentEntry(entry) }
        _uiState.update { it.copy(recents = recents) }
    }

    private fun restoreTreeUri() {
        val uriStr = container.prefs.getString("saf_tree_uri", null)
        if (uriStr == null) {
            _uiState.update { it.copy(showSafDialog = true) }
            return
        }
        val uri = Uri.parse(uriStr)
        viewModelScope.launch {
            // PERF FIX: persistedUriPermissions adalah IPC binder ke system server — jangan
            // dieksekusi di main thread saat cold start.
            val valid = withContext(Dispatchers.IO) { container.treeAccess.isPermissionValid(uri) }
            if (valid) {
                onTreeUriSelected(uri)
            } else {
                _uiState.update { it.copy(showSafDialog = true) }
            }
        }
    }

    // MEDIUM: docId formatnya "primary:Projects/sub" — ambil bagian SETELAH ":" terakhir
    // ("Projects"), bukan yang pertama ("primary"), supaya breadcrumb tidak selalu nampilin
    // nama root storage.
    private fun getDisplayName(uri: Uri): String {
        return try {
            val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
            docId.substringAfterLast(":").ifBlank { "storage" }
        } catch (_: Exception) {
            "storage"
        }
    }
}

// MEDIUM: nama file bisa mengandung "|", jadi split("|") biasa bisa pecah jadi lebih dari
// 3 bagian dan bikin field ketuker. Ambil bagian PERTAMA sebagai uri dan bagian TERAKHIR
// sebagai kind (keduanya dijamin tidak mengandung "|"), sisanya di tengah adalah nama file
// apa adanya — termasuk kalau ada "|" di dalamnya.
//
// INTERNAL (bukan private): sengaja diekspos untuk unit test — parser ini punya edge case
// yang gampang rusak kalau di-refactor (RecentsParserTest menjaganya).
internal fun parseRecentEntry(entry: String): RecentFile? {
    val firstSep = entry.indexOf('|')
    val lastSep = entry.lastIndexOf('|')
    if (firstSep < 0 || lastSep <= firstSep) return null
    val uri = entry.substring(0, firstSep)
    val name = entry.substring(firstSep + 1, lastSep)
    val kindRaw = entry.substring(lastSep + 1)
    val kind = try { Kind.valueOf(kindRaw) } catch (_: Exception) { Kind.CONFIG }
    return RecentFile(uri, name, kind)
}
