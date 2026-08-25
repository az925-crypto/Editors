package com.zaaam.editors.ui.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.editors.core.fs.isWebFile
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
    // URL bar menampilkan uri NYATA tab aktif (dulu hardcode path palsu). Kosong = belum ada seed.
    val url: String = "",
    val isLoading: Boolean = false,
    val consoleEntries: List<ConsoleEntry> = emptyList(),
    val consoleExpanded: Boolean = false,
    // Naik tiap tombol ↻ — PreviewScreen memakainya untuk memaksa loadDataWithBaseURL ulang
    // melewati guard "html berubah" (guard itu tetap dipertahankan untuk recomposition biasa).
    val reloadSeq: Int = 0
)

// Cap anti-flood lapis kedua di UI — bridge sudah rate-limited 30 msg/s + truncate 500 char,
// tapi list console juga tidak boleh tumbuh tanpa batas sepanjang sesi.
private const val MAX_CONSOLE_UI_ENTRIES = 200

class PreviewViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    private var debounceJob: Job? = null

    // Dokumen yang sedang ditampilkan preview — kunci stale-guard (R2/R3): job debounce yang
    // selesai SETELAH user ganti dokumen tidak boleh menimpa state dokumen baru.
    private var shownUri: String? = null

    init {
        viewModelScope.launch {
            container.previewTick.collect { tick ->
                // Relevansi: hanya compose ulang kalau yang berubah memang dokumen terpilih
                // dan masih ada di contentMap (belum closeTab).
                if (tick.uri == shownUri && container.editorContents.containsKey(tick.uri)) {
                    loadHtml(tick.uri)
                }
            }
        }
    }

    // Dipanggil PreviewScreen saat layar masuk / activeUri berganti. Seed INSTAN dari
    // contentMap (tanpa debounce) supaya konten langsung terlihat; debounce 350ms hanya
    // untuk jalur push ketikan (spec §7 "Preview debounce 350ms").
    fun showActiveFile(uri: String?) {
        val target = uri?.takeIf { isWebFile(it) } ?: return
        val content = container.editorContents[target] ?: return
        shownUri = target
        debounceJob?.cancel()
        composeAndApply(target, content)
    }

    fun loadHtml(uri: String) {
        if (!isWebFile(uri)) return
        val capturedUri = uri
        debounceJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        debounceJob = viewModelScope.launch {
            delay(350)
            // Stale-guard: selama window debounce user bisa pindah tab/dokumen atau closeTab.
            if (capturedUri != shownUri || capturedUri != container.editorSession.activeTab) return@launch
            val content = container.editorContents[capturedUri] ?: return@launch
            composeAndApply(capturedUri, content)
        }
    }

    fun refresh() {
        _uiState.update { it.copy(reloadSeq = it.reloadSeq + 1) }
    }

    private fun composeAndApply(uri: String, content: String) {
        // File .css/.js yang dibuka langsung dibungkus scaffold doc; .html dipakai apa adanya.
        val docHtml = when (uri.substringAfterLast(".", "").lowercase()) {
            "css" -> PreviewComposer.wrapStandaloneDocument(PreviewComposer.StandaloneKind.CSS, content)
            "js" -> PreviewComposer.wrapStandaloneDocument(PreviewComposer.StandaloneKind.JS, content)
            else -> content
        }
        val composed = PreviewComposer.compose(docHtml, companionCss(uri), companionJs())
        shownUri = uri
        _uiState.update { it.copy(html = composed, url = uri, isLoading = false) }
    }

    // Companion files: isi tab .css/.js lain yang TERBUKA digabung jadi satu fragmen — cukup
    // untuk pola umum index.html + style.css + main.js (lihat mockup portfolio). Penyuntikan
    // tetap mengikuti kontrak composer: replace placeholder exact-string, fallback append.
    private fun companionCss(activeUri: String): String? =
        container.editorSession.tabs.value
            .filter { !it.binary && it.uri != activeUri && it.uri.endsWith(".css", ignoreCase = true) }
            .mapNotNull { container.editorContents[it.uri] }
            .joinToString("\n")
            .ifBlank { null }

    // js sengaja SELALU non-null ("") — instrumentasi console harus terpasang untuk SEMUA
    // preview, sementara kontrak identity compose(html, null, null) milik unit test tetap utuh.
    private fun companionJs(): String =
        container.editorSession.tabs.value
            .filter { !it.binary && it.uri.endsWith(".js", ignoreCase = true) }
            .mapNotNull { container.editorContents[it.uri] }
            .joinToString("\n")
            .ifBlank { "" }

    fun addConsole(entry: ConsoleEntry) {
        _uiState.update { st ->
            val trimmed = if (st.consoleEntries.size >= MAX_CONSOLE_UI_ENTRIES) {
                st.consoleEntries.takeLast(MAX_CONSOLE_UI_ENTRIES - 1)
            } else {
                st.consoleEntries
            }
            st.copy(consoleEntries = trimmed + entry)
        }
    }

    fun clearConsole() {
        _uiState.update { it.copy(consoleEntries = emptyList()) }
    }

    fun toggleConsole() {
        _uiState.update { it.copy(consoleExpanded = !it.consoleExpanded) }
    }
}
