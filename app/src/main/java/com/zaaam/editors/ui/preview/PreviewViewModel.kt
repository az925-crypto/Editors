package com.zaaam.editors.ui.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.editors.core.preview.ConsoleEntry
import com.zaaam.editors.core.preview.PreviewComposer
import com.zaaam.editors.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PreviewUiState(
    val html: String = "",
    val url: String = "file:///storage/emulated/0/Projects/portfolio/index.html",
    val isLoading: Boolean = false,
    val consoleEntries: List<ConsoleEntry> = emptyList(),
    val consoleExpanded: Boolean = false
)

class PreviewViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    private var debounceJob: Job? = null

    fun loadHtml(html: String, css: String? = null, js: String? = null) {
        debounceJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        debounceJob = viewModelScope.launch {
            delay(350)
            val composed = PreviewComposer.compose(html, css, js)
            _uiState.update { it.copy(html = composed, isLoading = false) }
        }
    }

    fun addConsole(entry: ConsoleEntry) {
        _uiState.update { it.copy(consoleEntries = it.consoleEntries + entry) }
    }

    fun clearConsole() {
        _uiState.update { it.copy(consoleEntries = emptyList()) }
    }

    fun toggleConsole() {
        _uiState.update { it.copy(consoleExpanded = !it.consoleExpanded) }
    }

    fun isWebFile(uri: String?): Boolean {
        if (uri == null) return false
        val ext = uri.substringAfterLast(".", "").lowercase()
        return ext in setOf("html", "css", "js")
    }
}