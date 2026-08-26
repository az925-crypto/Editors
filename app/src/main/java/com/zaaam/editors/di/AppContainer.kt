package com.zaaam.editors.di

import android.app.Application
import com.zaaam.editors.core.editor.EditorSession
import com.zaaam.editors.core.fs.FileKindResolver
import com.zaaam.editors.core.fs.FileOps
import com.zaaam.editors.core.fs.HiddenFiles
import com.zaaam.editors.core.fs.SafFileSystemImpl
import com.zaaam.editors.core.fs.TreeAccess
import com.zaaam.editors.session.AppScreen
import com.zaaam.editors.session.SnippetRepository
import com.zaaam.editors.session.ToolsTab
import com.zaaam.editors.session.TreeScanManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

// Fase 4: sinyal "isi file web berubah". Seq monotonic WAJIB — MutableStateFlow mengonflasi
// nilai sama, dan dua ketikan di uri yang sama akan menghasilkan value identik kalau tick
// cuma String uri (collector tidak bangun, preview basi).
data class PreviewTick(val seq: Long, val uri: String)

class AppContainer(application: Application) {
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
    val prefs = application.getSharedPreferences("zaaam_editors", android.content.Context.MODE_PRIVATE)

    val fileSystem = SafFileSystemImpl(application.contentResolver)
    val treeAccess = TreeAccess(application.contentResolver)
    val hiddenFiles = HiddenFiles()
    val fileKindResolver = FileKindResolver()
    val fileOps = FileOps()

    val editorSession = EditorSession()
    val screenState = MutableStateFlow(AppScreen.FILES)

    // Phase 2 (ALAT): sub-tab aktif + target hex editor. hexTargetUri di-set rider
    // FilesViewModel.openFile saat file BINARY dibuka — entry point hex editor.
    val toolsTab = MutableStateFlow(ToolsTab.HUB)
    val hexTargetUri = MutableStateFlow<String?>(null)

    // Shared scan (Analyzer+Dupes+FindReplace pakai satu cache walk) + repo snippet prefs.
    // Keduanya singleton container karena state-nya lintas-layar.
    val treeScanManager = TreeScanManager(fileSystem, treeAccess, prefs, ioDispatcher)
    val snippetRepository = SnippetRepository(prefs)

    // Shared antara FilesViewModel (penulis) dan EditorViewModel (pembaca) supaya isi file
    // yang baru dibuka langsung kebaca EditorViewModel ini — dua ViewModel ini instance-nya
    // beda, jadi kalau map-nya lokal isinya nggak akan ketemu. Lihat CRITICAL 3.
    val editorContents: MutableMap<String, String> = ConcurrentHashMap()

// Fase 4 live preview: EditorViewModel publish tick tiap isi file web berubah;
// PreviewViewModel collect → compose ulang (debounce 350ms di sana).
private val previewTickSeq = AtomicLong(0)
val previewTick = MutableStateFlow(PreviewTick(0, ""))

fun publishPreviewTick(uri: String) {
    previewTick.value = PreviewTick(previewTickSeq.incrementAndGet(), uri)
}

// Live preview split: editor + render web terlihat bersamaan di layar Editor.
// WHY state di container (bukan rememberSaveable lokal): harus selamat dari ganti tab
// bottom-nav & rotasi, dan dibaca lintas-layar (chip di EditorScreen saja hari ini).
// Persist prefs supaya pilihan user bertahan antar sesi; setter satu pintu agar
// key prefs hanya diketahui di sini (pola SnippetRepository).
val splitPreviewEnabled = MutableStateFlow(prefs.getBoolean(PREF_SPLIT_PREVIEW, false))

fun setSplitPreview(enabled: Boolean) {
    splitPreviewEnabled.value = enabled
    prefs.edit().putBoolean(PREF_SPLIT_PREVIEW, enabled).apply()
}

private companion object {
    const val PREF_SPLIT_PREVIEW = "split_preview_enabled"
}
}