package com.zaaam.editors.ui.files

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.editors.core.fs.FsEntry
import com.zaaam.editors.core.fs.Kind
import com.zaaam.editors.di.AppContainer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FilesUiState(
    val entries: List<FsEntry> = emptyList(),
    val query: String = "",
    val showHidden: Boolean = false,
    val isLoading: Boolean = false,
    val permDenied: Boolean = false,
    val expandedDirs: Set<String> = setOf("/storage/emulated/0", "/storage/emulated/0/Projects", "/storage/emulated/0/Projects/portfolio"),
    val recents: List<String> = listOf("Projects/portfolio/index.html", "Projects/zedit-app/MainActivity.kt", "Projects/scripts/scrape.py")
)

@OptIn(FlowPreview::class)
class FilesViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            queryFlow.debounce(200).collect { q ->
                _uiState.update { it.copy(query = q) }
            }
        }
        loadDummy()
    }

    fun onQueryChange(q: String) {
        queryFlow.value = q
    }

    fun toggleHidden() {
        _uiState.update { it.copy(showHidden = !it.showHidden) }
    }

    fun toggleDir(path: String) {
        _uiState.update {
            val expanded = it.expandedDirs.toMutableSet()
            if (path in expanded) expanded.remove(path) else expanded.add(path)
            it.copy(expandedDirs = expanded)
        }
    }

    fun setPermDenied(v: Boolean) {
        _uiState.update { it.copy(permDenied = v) }
    }

    private fun loadDummy() {
        val dummy = listOf(
            FsEntry("Documents", Uri.parse("content://dummy/Documents"), true),
            FsEntry("Download", Uri.parse("content://dummy/Download"), true),
            FsEntry("Projects", Uri.parse("content://dummy/Projects"), true),
            FsEntry(".thumbnails", Uri.parse("content://dummy/.thumbnails"), true, isHidden = true),
            FsEntry(".trashed", Uri.parse("content://dummy/.trashed"), true, isHidden = true),
            FsEntry(".editorconfig", Uri.parse("content://dummy/.editorconfig"), false, isHidden = true),
            FsEntry("index.html", Uri.parse("content://dummy/index.html"), false, size = 12400, kind = Kind.WEB),
            FsEntry("style.css", Uri.parse("content://dummy/style.css"), false, size = 4100, kind = Kind.WEB),
            FsEntry("MainActivity.kt", Uri.parse("content://dummy/MainActivity.kt"), false, size = 8700, kind = Kind.CODE),
            FsEntry("scrape.py", Uri.parse("content://dummy/scrape.py"), false, size = 1100, kind = Kind.CODE),
        )
        _uiState.update { it.copy(entries = dummy) }
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
}