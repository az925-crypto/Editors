package com.zaaam.editors.session

import android.content.SharedPreferences
import android.net.Uri
import com.zaaam.editors.core.fs.FsResult
import com.zaaam.editors.core.fs.SafFileSystem
import com.zaaam.editors.core.fs.TreeAccess
import com.zaaam.editors.core.tools.DuplicateFinder
import com.zaaam.editors.core.tools.DupesOutcome
import com.zaaam.editors.core.tools.ToolNode
import com.zaaam.editors.core.tools.TreeScanResult
import com.zaaam.editors.core.tools.TreeScanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

enum class ToolScanPhase { IDLE, SCANNING, DONE, FAILED }

// SECURITY: pesan error generik — exception.message bisa bawa path provider internal.
private const val MSG_SCAN_FAILED = "Gagal memindai folder"
private const val MSG_DUPES_FAILED = "Gagal memeriksa duplikat"
private const val MSG_PERM_DENIED = "Akses ditolak — pilih folder lagi"

data class DupesState(
    val phase: ToolScanPhase = ToolScanPhase.IDLE,
    val progressDone: Int = 0,
    val progressTotal: Int = 0,
    val outcome: DupesOutcome? = null,
    val error: String? = null
)

data class TreeScanState(
    val rootUri: String? = null,
    val rootLabel: String = "",
    val includeHidden: Boolean = false,
    val phase: ToolScanPhase = ToolScanPhase.IDLE,
    val progressDone: Int = 0,
    // Kontrak UI: totalEstimate bisa 0 (root gagal) → pembagi wajib di-guard layar.
    val progressTotal: Int = 0,
    val result: TreeScanResult? = null,
    val error: String? = null,
    val dupes: DupesState = DupesState()
)

// Manager walk tree SHARED untuk Analyzer + Dupes + FindReplace: satu hasil DFS di-cache,
// tiga layar tidak pernah jalan sendiri. Pola job+generation disalin FilesViewModel (anti
// hasil basi). Progress dari engine masuk lewat MutableStateFlow.update — konflasi bawaan
// StateFlow adalah throttle sisi UI yang diminta kontrak engine (jangan ganti SharedFlow
// tanpa buffer). State HANYA disentuh dari Main (scope Main.immediate); IO murni withContext.
class TreeScanManager(
    private val fs: SafFileSystem,
    private val treeAccess: TreeAccess,
    private val prefs: SharedPreferences,
    private val ioDispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(TreeScanState())
    val state: StateFlow<TreeScanState> = _state.asStateFlow()

    private var scanJob: Job? = null
    private var scanGeneration = 0
    private var dupesJob: Job? = null
    private var dupesGeneration = 0

    init {
        restoreSavedTree()
    }

    fun onTreeSelected(uri: Uri) {
        scope.launch {
            val granted = withContext(ioDispatcher) { treeAccess.takePersistablePermission(uri) }
            if (granted is FsResult.Error) {
                _state.update { it.copy(error = MSG_PERM_DENIED) }
                return@launch
            }
            // Sama seperti FilesViewModel: persist hanya kalau read+write benar-benar
            // ter-persist — kalau tidak, restore gagal tiap cold start → dialog SAF loop.
            val restorable = withContext(ioDispatcher) { treeAccess.isPermissionValid(uri) }
            if (restorable) {
                prefs.edit().putString(PREF_TREE_URI, uri.toString()).apply()
            }
            _state.update {
                TreeScanState(
                    rootUri = uri.toString(),
                    rootLabel = displayNameOf(uri),
                    includeHidden = it.includeHidden
                )
            }
            startScan()
        }
    }

    fun clearError() { _state.update { it.copy(error = null) } }

    fun toggleHidden() {
        _state.update { it.copy(includeHidden = !it.includeHidden) }
        // Keputusan user terkunci: toggle hidden langsung memicu rescan karena cache walk
        // terikat flag includeHidden (hasil lama tidak sah untuk filter berbeda).
        if (_state.value.rootUri != null) startScan()
    }

    // Dipanggil tiap layar pemakai cache (Analyzer/Dupes/FindReplace) saat masuk:
    // DONE → cache dipakai (no-op); IDLE/FAILED + root tersedia → scan.
    fun ensureScan() {
        val s = _state.value
        if (s.phase == ToolScanPhase.DONE || s.phase == ToolScanPhase.SCANNING) return
        if (s.rootUri != null) startScan()
    }

    fun startScan() {
        val root = _state.value.rootUri ?: return
        val includeHidden = _state.value.includeHidden
        scanJob?.cancel()
        val generation = ++scanGeneration
        _state.update {
            it.copy(
                phase = ToolScanPhase.SCANNING,
                progressDone = 0,
                progressTotal = 0,
                result = null,
                error = null,
                // Hasil dupes lama otomatis basi setelah tree diganti/rescan.
                dupes = DupesState()
            )
        }
        scanJob = scope.launch {
            val scanner = TreeScanner(
                listChildren = { uriStr ->
                    mapChildren(uriStr)
                },
                ioDispatcher = ioDispatcher
            )
            try {
                val outcome = scanner.walk(root, includeHidden) { p ->
                    _state.update {
                        if (it.phase != ToolScanPhase.SCANNING || it.result != null) return@update it
                        it.copy(progressDone = p.done, progressTotal = p.totalEstimate)
                    }
                }
                if (generation != scanGeneration) return@launch
                _state.update {
                    // Root gagal dibaca ≠ folder kosong: laporkan FAILED supaya UI menawarkan
                    // rescan; skippedDirs subfolder tetap DONE (resilient by design).
                    if (outcome.stats.rootFailed) {
                        it.copy(phase = ToolScanPhase.FAILED, result = outcome, error = MSG_SCAN_FAILED)
                    } else {
                        it.copy(phase = ToolScanPhase.DONE, result = outcome)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation != scanGeneration) return@launch
                _state.update { it.copy(phase = ToolScanPhase.FAILED, error = MSG_SCAN_FAILED) }
            }
        }
    }

    fun runDupes() {
        val files = _state.value.result?.files ?: return
        if (files.isEmpty()) return
        dupesJob?.cancel()
        val generation = ++dupesGeneration
        _state.update {
            it.copy(dupes = DupesState(phase = ToolScanPhase.SCANNING, progressTotal = files.size))
        }
        dupesJob = scope.launch {
            val finder = DuplicateFinder(
                openStream = { uriStr ->
                    withContext(ioDispatcher) {
                        when (val r = fs.readStream(Uri.parse(uriStr))) {
                            is FsResult.Success -> r.value // caller (sha1Streaming use{}) yang close
                            is FsResult.Error -> null
                        }
                    }
                },
                statSize = { uriStr ->
                    when (val r = fs.statSize(Uri.parse(uriStr))) {
                        is FsResult.Success -> r.value
                        is FsResult.Error -> null
                    }
                },
                ioDispatcher = ioDispatcher
            )
            try {
                val outcome = finder.find(files) { p ->
                    _state.update {
                        if (it.dupes.phase != ToolScanPhase.SCANNING) return@update it
                        it.copy(dupes = it.dupes.copy(progressDone = p.done, progressTotal = p.totalEstimate))
                    }
                }
                if (generation != dupesGeneration) return@launch
                _state.update { it.copy(dupes = it.dupes.copy(phase = ToolScanPhase.DONE, outcome = outcome)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation != dupesGeneration) return@launch
                _state.update { it.copy(dupes = it.dupes.copy(phase = ToolScanPhase.FAILED, error = MSG_DUPES_FAILED)) }
            }
        }
    }

    private suspend fun mapChildren(uriStr: String): FsResult<List<ToolNode>> =
        when (val r = fs.listChildren(Uri.parse(uriStr))) {
            is FsResult.Success -> FsResult.Success(
                // relPath placeholder "" — TreeScanner yang menimpa dengan path sintetis.
                r.value.map { ToolNode(it.name, it.uri.toString(), "", it.isDir, it.size, it.isHidden) }
            )
            is FsResult.Error -> FsResult.Error(r.exception)
        }

    private fun restoreSavedTree() {
        val uriStr = prefs.getString(PREF_TREE_URI, null) ?: return
        val uri = Uri.parse(uriStr)
        scope.launch {
            // IPC binder — jangan di main thread saat cold start (pola FilesViewModel).
            val valid = withContext(ioDispatcher) { treeAccess.isPermissionValid(uri) }
            if (!valid) return@launch
            _state.update {
                if (it.rootUri != null) it
                else it.copy(rootUri = uriStr, rootLabel = displayNameOf(uri))
            }
        }
    }

    // docId "primary:Projects/foto.jpg" → bagian setelah ":" terakhir. Untuk ROOT tree ini
    // nama folder; fallback generik tanpa membocorkan path mentah ke log/error UI.
    private fun displayNameOf(uri: Uri): String = try {
        val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
        docId.substringAfterLast(":").ifBlank { "storage" }
    } catch (_: Exception) {
        "storage"
    }

    companion object {
        const val PREF_TREE_URI = "saf_tree_uri" // sama dengan pref Files — satu sumber folder aktif
    }
}
