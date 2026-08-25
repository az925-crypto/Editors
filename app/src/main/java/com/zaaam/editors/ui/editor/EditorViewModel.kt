package com.zaaam.editors.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.editors.core.editor.TabState
import com.zaaam.editors.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class EditorUiState(
    val tabs: List<TabState> = emptyList(),
    val activeUri: String? = null,
    val content: String = "",
    val saveStatus: SaveStatus = SaveStatus.Idle
)

sealed interface SaveStatus {
    object Idle : SaveStatus
    object Saving : SaveStatus
    data class Saved(val time: String) : SaveStatus
}

class EditorViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val contentMap = mutableMapOf<String, String>()
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            container.editorSession.tabs.collect { tabs ->
                val active = container.editorSession.activeTab
                val content = active?.let { contentMap[it] } ?: ""
                _uiState.update { it.copy(tabs = tabs, activeUri = active, content = content) }
            }
        }
    }

    fun openTab(uri: String, name: String, content: String, isBinary: Boolean = false) {
        contentMap[uri] = content
        container.editorSession.addTab(TabState(uri, name, binary = isBinary))
    }

    fun closeTab(uri: String) {
        container.editorSession.closeTab(uri)
        contentMap.remove(uri)
    }

    fun switchTab(uri: String) {
        container.editorSession.activeTab = uri
        val content = contentMap[uri] ?: ""
        _uiState.update { it.copy(activeUri = uri, content = content, saveStatus = SaveStatus.Idle) }
    }

    fun onContentChange(newContent: String) {
        val uri = _uiState.value.activeUri ?: return
        contentMap[uri] = newContent
        container.editorSession.markDirty(uri, true)
        _uiState.update { it.copy(content = newContent, saveStatus = SaveStatus.Saving) }
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(900)
            container.editorSession.markSaved(uri)
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            _uiState.update { it.copy(saveStatus = SaveStatus.Saved(time)) }
            delay(2000)
            _uiState.update { it.copy(saveStatus = SaveStatus.Idle) }
        }
    }

    fun isWebFile(uri: String?): Boolean {
        if (uri == null) return false
        val ext = uri.substringAfterLast(".", "").lowercase()
        return ext in setOf("html", "css", "js")
    }
}