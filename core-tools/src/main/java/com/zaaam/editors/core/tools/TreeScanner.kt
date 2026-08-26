package com.zaaam.editors.core.tools

import com.zaaam.editors.core.fs.FsResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

// Walker tree SAF DFS ITERATIF (explicit stack — anti stack-overflow untuk folder dalam).
// Engine murni: daftar anak di-inject lewat lambda (String-uri → List<ToolNode>), jadi bisa
// di-unit-test JVM tanpa android.*. Resilient: subfolder gagal = skippedDirs++, walk lanjut;
// root gagal ditandai stats.rootFailed. Visited-set uri folder memutus siklus dari provider
// untrusted (child menunjuk ancestor/dirinya) sekaligus cegah dokumen reachable lewat >1
// jalur dihitung dobel.
class TreeScanner(
    private val listChildren: suspend (String) -> FsResult<List<ToolNode>>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun walk(
        rootUri: String,
        includeHidden: Boolean,
        onProgress: suspend (ToolProgress) -> Unit = {}
    ): TreeScanResult = withContext(ioDispatcher) {
        val files = mutableListOf<ToolNode>()
        val dirs = mutableListOf<ToolNode>()
        var skippedDirs = 0
        var rootFailed = false

        // (uri, relPathOfThisDir) — relPath "" khusus root virtual.
        data class Frame(val uri: String, val rel: String)

        // Anti-loop: uri folder yang sudah ditemukan tak pernah diproses kedua kali.
        val visitedDirs = HashSet<String>()
        val stack = ArrayDeque<Frame>()
        var dirsDone = 0
        var filesSeen = 0

        // Kontrak progress: engine emit per SELESAI proses satu frame folder (sukses/gagal)
        // dengan ANGKA akurat; throttle frekuensi buat UI ada di sisi StateFlow, bukan engine.
        fun emit(): ToolProgress {
            val done = dirsDone + filesSeen
            return ToolProgress(ToolPhase.WALK, done, done + stack.size)
        }

        visitedDirs.add(rootUri) // root ikut visited-set → siklus balik ke root juga putus.
        stack.addLast(Frame(rootUri, ""))

        while (stack.isNotEmpty()) {
            ensureActive()
            val frame = stack.removeLast()
            val childrenResult = listChildren(frame.uri)
            val children = (childrenResult as? FsResult.Success)?.value
            if (children == null) {
                if (frame.rel.isEmpty()) rootFailed = true else skippedDirs++
                onProgress(emit())
                continue
            }
            for (child in children) {
                if (!includeHidden && child.isHidden) continue
                // add() false = sudah dikunjungi/sedang di stack: jangan push, jangan dobel hitung.
                if (child.isDir && !visitedDirs.add(child.uri)) continue
                val rel = if (frame.rel.isEmpty()) child.name else "${frame.rel}/${child.name}"
                val node = child.copy(relPath = rel)
                if (node.isDir) {
                    dirs.add(node)
                    stack.addLast(Frame(node.uri, rel))
                } else {
                    files.add(node)
                    filesSeen++
                }
            }
            dirsDone++
            onProgress(emit())
        }
        TreeScanResult(files, dirs, ScanStats(skippedDirs = skippedDirs, rootFailed = rootFailed))
    }
}
