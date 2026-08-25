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

    // CRITICAL 3: pakai map bersama di AppContainer (bukan map privat lokal) supaya konten
    // yang ditulis FilesViewModel saat membuka file langsung kebaca EditorViewModel ini —
    // dua ViewModel ini instance-nya beda, jadi kalau map-nya lokal isinya nggak akan ketemu.
    private val contentMap get() = container.editorContents
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
        onContentChange(_uiState.value.activeUri, newContent)
    }

    // CRITICAL/MEDIUM FIX (80841fd re-audit): sekarang menerima `uri` eksplisit, bukan cuma
    // implisit lewat _uiState.value.activeUri. Caller (EditorScreen) yang flush debounce saat
    // pindah tab HARUS bisa nyasar URI tab yang sedang ditinggalkan, karena pada saat flush itu
    // jalan, activeUri di state sudah keburu pindah ke tab baru — kalau dibaca implisit di sini,
    // konten tab lama ketulis ke slot tab baru (EditorScreen.kt:163 bug).
    //
    // Sekalian membenahi bug saveStatus (EditorViewModel.kt:73 lama): saveJob yang dijadwalkan
    // untuk tab A tidak boleh menimpa saveStatus kalau user sudah pindah ke tab B sebelum delay
    // 900ms/2000ms selesai — makanya tiap update UI state di bawah dijaga dengan cek
    // `_uiState.value.activeUri == uri` dulu.
    fun onContentChange(uri: String?, newContent: String) {
        val targetUri = uri ?: return
        contentMap[targetUri] = newContent
        container.editorSession.markDirty(targetUri, true)
        if (_uiState.value.activeUri == targetUri) {
            _uiState.update { it.copy(content = newContent, saveStatus = SaveStatus.Saving) }
        }
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(900)
            container.editorSession.markSaved(targetUri)
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            if (_uiState.value.activeUri == targetUri) {
                _uiState.update { it.copy(saveStatus = SaveStatus.Saved(time)) }
                delay(2000)
                if (_uiState.value.activeUri == targetUri) {
                    _uiState.update { it.copy(saveStatus = SaveStatus.Idle) }
                }
            }
        }
    }

    fun isWebFile(uri: String?): Boolean {
        if (uri == null) return false
        val ext = uri.substringAfterLast(".", "").lowercase()
        return ext in setOf("html", "css", "js")
    }
}
