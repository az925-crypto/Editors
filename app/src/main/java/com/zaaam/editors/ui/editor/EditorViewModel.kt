package com.zaaam.editors.ui.editor

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.editors.core.editor.TabState
import com.zaaam.editors.core.fs.FsResult
import com.zaaam.editors.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // MEDIUM FIX (autosave real): writeText ke disk bisa gagal (permission revoke, provider
    // error) — LED harus punya state Error eksplisit, bukan diam-diam bilang "Tersimpan".
    // Pesan di UI sengaja generik, detail exception tidak dibocorkan.
    data object Error : SaveStatus
}

class EditorViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    // CRITICAL 3: pakai map bersama di AppContainer (bukan map privat lokal) supaya konten
    // yang ditulis FilesViewModel saat membuka file langsung kebaca EditorViewModel ini —
    // dua ViewModel ini instance-nya beda, jadi kalau map-nya lokal isinya nggak akan ketemu.
    private val contentMap get() = container.editorContents

    // MEDIUM FIX: dulu saveJob global tunggal — cancel() tiap keystroke membunuh markSaved
    // tab lain yang belum jalan, bikin LED "Menyimpan…" nyangkut permanen. Sekarang satu
    // Job per URI supaya debounce/autosave tiap tab independen.
    private val saveJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            container.editorSession.tabs.collect { tabs ->
                val active = container.editorSession.activeTab
                val content = active?.let { contentMap[it] } ?: ""
                _uiState.update {
                    it.copy(
                        tabs = tabs,
                        activeUri = active,
                        content = content,
                        // MEDIUM FIX: LED status tab lama tidak boleh menempel di tab baru —
                        // reset ke Idle setiap tab aktif berganti lewat addTab/closeTab.
                        saveStatus = if (it.activeUri != active) SaveStatus.Idle else it.saveStatus
                    )
                }
            }
        }
    }

    fun openTab(uri: String, name: String, content: String, isBinary: Boolean = false) {
        contentMap[uri] = content
        container.editorSession.addTab(TabState(uri, name, binary = isBinary))
    }

    fun closeTab(uri: String) {
        // MEDIUM FIX: batalkan autosave yang masih mengantre untuk tab ini — kalau dibiarkan,
        // job akan nulis balik contentMap[uri] (resurrect) setelah entry-nya dihapus.
        saveJobs.remove(uri)?.cancel()
        container.editorSession.closeTab(uri)
        contentMap.remove(uri)
    }

    fun switchTab(uri: String) {
        container.editorSession.activeTab = uri
        val content = contentMap[uri] ?: ""
        _uiState.update { it.copy(activeUri = uri, content = content, saveStatus = SaveStatus.Idle) }
    }

    // CRITICAL/MEDIUM FIX (80841fd re-audit): menerima `uri` eksplisit, bukan implisit lewat
    // _uiState.value.activeUri — caller (EditorScreen) yang flush debounce saat pindah tab
    // HARUS bisa nyasar URI tab yang ditinggalkan. Overload 1-arg (implisit activeUri) DIHAPUS:
    // itu akar race korupsi antar-tab.
    //
    // Autosave REAL: setelah debounce 900ms, isi benar-benar ditulis ke disk via
    // SafFileSystem.writeText di IO dispatcher; LED mengikuti FsResult. Tab biner tidak pernah
    // ditulis balik (editor tidak boleh menyentuh file non-teks).
    fun onContentChange(uri: String?, newContent: String) {
        val targetUri = uri ?: return
        val tab = _uiState.value.tabs.firstOrNull { it.uri == targetUri } ?: return
        if (tab.binary) return
        contentMap[targetUri] = newContent
        container.editorSession.markDirty(targetUri, true)
        if (_uiState.value.activeUri == targetUri) {
            _uiState.update { it.copy(content = newContent, saveStatus = SaveStatus.Saving) }
        }
        saveJobs.remove(targetUri)?.cancel()
        saveJobs[targetUri] = viewModelScope.launch {
            delay(900)
            saveJobs.remove(targetUri)
            when (
                withContext(container.ioDispatcher) {
                    container.fileSystem.writeText(Uri.parse(targetUri), newContent)
                }
            ) {
                is FsResult.Success -> {
                    container.editorSession.markSaved(targetUri)
                    if (_uiState.value.activeUri == targetUri) {
                        _uiState.update { it.copy(saveStatus = SaveStatus.Saved(timeNow())) }
                        delay(2000)
                        if (_uiState.value.activeUri == targetUri) {
                            _uiState.update { it.copy(saveStatus = SaveStatus.Idle) }
                        }
                    }
                }
                is FsResult.Error -> {
                    // Gagal simpan: LED Error hanya kalau tab ini masih aktif; entry contentMap
                    // TIDAK dibuang supaya teks user tidak hilang dari memori.
                    if (_uiState.value.activeUri == targetUri) {
                        _uiState.update { it.copy(saveStatus = SaveStatus.Error) }
                    }
                }
            }
        }
    }

    private fun timeNow(): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    fun isWebFile(uri: String?): Boolean {
        if (uri == null) return false
        val ext = uri.substringAfterLast(".", "").lowercase()
        return ext in setOf("html", "css", "js")
    }
}
