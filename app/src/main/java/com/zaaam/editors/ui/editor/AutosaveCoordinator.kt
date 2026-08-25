package com.zaaam.editors.ui.editor

import com.zaaam.editors.core.fs.FsResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

// AUTOSAVE DEBOUNCE — dipindah utuh dari EditorViewModel.onContentChange supaya logika
// tersubtle repo ini (debounce per-uri, mutex urutan tulis, divergence check, trik fase
// self-remove) bisa diuji kotlinx-coroutines-test runTest tanpa menyentuh ViewModel/Android.
//
// Kontrak yang WAJIB dipertahankan (lihat komentar lama di EditorViewModel + PROGRESS.md):
// - Job in-flight SENGAJA tidak dibatalkan (cancel mid-write = file bisa kepotong).
//   Trik fasenya: job menghapus DIRINYA dari map tepat setelah delay debounce selesai,
//   sebelum write dimulai — sejak titik itu cancelQueued() no-op untuk job tersebut.
// - Mutex per-URI: write yang lebih baru SELALU mendarat terakhir walau ditulis paralel.
// - Divergence: Succeeded hanya diemit kalau snapshot yang barusan ditulis masih terbaru
//   menurut `isStillCurrent` — kalau user sudah mengetik lagi selama IO jalan, koordinator
//   diam (dirty tetap menyala, job berikutnya yang menuntaskan LED).
internal const val AUTOSAVE_DEBOUNCE_MS = 900L

internal class AutosaveCoordinator(
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
    private val debounceMs: Long = AUTOSAVE_DEBOUNCE_MS,
    private val write: suspend (uri: String, content: String) -> FsResult<Unit>,
    private val isStillCurrent: (uri: String, content: String) -> Boolean
) {
    sealed interface Event {
        data class Succeeded(val uri: String) : Event
        data class Failed(val uri: String) : Event
    }

    // extraBufferCapacity: emit tidak pernah suspend/dihilangkan walau collector sempat telat.
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 64)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private val jobs = HashMap<String, Job>()
    private val locks = ConcurrentHashMap<String, Mutex>()

    fun onChange(uri: String, content: String) {
        val previous = synchronized(jobs) { jobs.remove(uri) }
        previous?.cancel()
        val job = scope.launch {
            delay(debounceMs)
            // Fase queued → in-flight: lepas dari map agar tak bisa di-cancel lagi.
            synchronized(jobs) { jobs.remove(uri) }
            val result = locks.getOrPut(uri) { Mutex() }.withLock {
                withContext(ioDispatcher) { write(uri, content) }
            }
            when (result) {
                is FsResult.Success ->
                    if (isStillCurrent(uri, content)) _events.emit(Event.Succeeded(uri))
                is FsResult.Error -> _events.emit(Event.Failed(uri))
            }
        }
        synchronized(jobs) { jobs[uri] = job }
    }

    fun cancelQueued(uri: String) {
        synchronized(jobs) { jobs.remove(uri) }?.cancel()
    }
}
