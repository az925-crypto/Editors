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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecentFile(
    val uri: String,
    val name: String,
    val kind: Kind
)

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
    private val pathStack = mutableListOf<Uri>()

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
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = container.treeAccess.takePersistablePermission(uri)) {
                is FsResult.Success -> {
                    container.prefs.edit().putString("saf_tree_uri", uri.toString()).apply()
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
                    pathStack.clear()
                    pathStack.add(uri)
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
        viewModelScope.launch(Dispatchers.IO) {
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
    }

    fun navigateToSegment(index: Int) {
        if (index >= pathStack.size) return
        val uri = pathStack[index]
        pathStack.subList(index + 1, pathStack.size).clear()
        _uiState.update {
            it.copy(
                currentUri = uri,
                pathSegments = it.pathSegments.take(index + 1),
                isLoading = true
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            loadChildren(uri)
        }
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
        viewModelScope.launch(Dispatchers.IO) {
            loadChildren(uri)
        }
        return true
    }

    fun openFile(entry: FsEntry) {
        if (entry.isDir) {
            navigateInto(entry)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (entry.kind == Kind.BINARY) {
                container.editorSession.addTab(
                    TabState(entry.uri.toString(), entry.name, binary = true)
                )
                container.screenState.value = com.zaaam.editors.session.AppScreen.EDITOR
                return@launch
            }
            when (val result = container.fileSystem.readText(entry.uri)) {
                is FsResult.Success -> {
                    container.editorSession.addTab(
                        TabState(entry.uri.toString(), entry.name)
                    )
                    container.screenState.value = com.zaaam.editors.session.AppScreen.EDITOR
                    upsertRecent(entry)
                }
                is FsResult.Error -> {
                    _uiState.update { it.copy(listError = result.exception.message) }
                }
            }
        }
    }

    fun openRecent(recent: RecentFile) {
        viewModelScope.launch(Dispatchers.IO) {
            val uri = Uri.parse(recent.uri)
            when (val result = container.fileSystem.readText(uri)) {
                is FsResult.Success -> {
                    container.editorSession.addTab(
                        TabState(recent.uri, recent.name)
                    )
                    container.screenState.value = com.zaaam.editors.session.AppScreen.EDITOR
                }
                is FsResult.Error -> {
                    _uiState.update { it.copy(listError = result.exception.message) }
                }
            }
        }
    }

    fun setPermDenied(v: Boolean) {
        _uiState.update { it.copy(permDenied = v) }
    }

    fun filteredEntries(): List<FsEntry> {
        val s = _uiState.value
        var list = s.entries
        if (!s.showHidden) list = list.filter { !it.isHidden }
        if (s.query.isNotBlank()) {
            val q = s.query.lowercase()
            list = list.filter { it.name.lowercase().contains(q) }
        }
        return list
    }

    private fun loadChildren(parentUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, listError = null) }
            when (val result = container.fileSystem.listChildren(parentUri)) {
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
                            listError = result.exception.message
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
        val recents = set.mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 3) {
                val kind = try { Kind.valueOf(parts[2]) } catch (_: Exception) { Kind.CONFIG }
                RecentFile(parts[0], parts[1], kind)
            } else null
        }
        _uiState.update { it.copy(recents = recents) }
    }

    private fun restoreTreeUri() {
        val uriStr = container.prefs.getString("saf_tree_uri", null)
        if (uriStr != null) {
            val uri = Uri.parse(uriStr)
            if (container.treeAccess.isPermissionValid(uri)) {
                onTreeUriSelected(uri)
            } else {
                _uiState.update { it.copy(showSafDialog = true) }
            }
        } else {
            _uiState.update { it.copy(showSafDialog = true) }
        }
    }

    private fun getDisplayName(uri: Uri): String {
        return try {
            val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
            docId.split(":").firstOrNull() ?: "storage"
        } catch (_: Exception) {
            "storage"
        }
    }
}
