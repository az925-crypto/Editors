package com.zaaam.editors.core.tools

import com.zaaam.editors.core.fs.FsResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

// Walker tree SAF DFS ITERATIF (explicit stack — anti stack-overflow untuk folder dalam).
// Engine murni: daftar anak di-inject lewat lambda (String-uri → List<ToolNode>), jadi bisa
// di-unit-test JVM tanpa android.*. Resilient: satu folder gagal = skippedDirs++, walk lanjut.
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

        // (uri, relPathOfThisDir, nodeDir?) — relPath "" khusus root virtual.
        data class Frame(val uri: String, val rel: String)

        val stack = ArrayDeque<Frame>()
        stack.addLast(Frame(rootUri, ""))
        var visited = 0

        while (stack.isNotEmpty()) {
            ensureActive()
            val frame = stack.removeLast()
            val childrenResult = listChildren(frame.uri)
            val children = (childrenResult as? FsResult.Success)?.value
            if (children == null) {
                if (frame.rel.isNotEmpty()) skippedDirs++ // root gagal dilaporkan caller sebagai error total
                onProgress(ToolProgress(ToolPhase.WALK, ++visited, visited + stack.size))
                continue
            }
            for (child in children) {
                if (!includeHidden && child.isHidden) continue
                val rel = if (frame.rel.isEmpty()) child.name else "${frame.rel}/${child.name}"
                val node = child.copy(relPath = rel)
                if (node.isDir) {
                    dirs.add(node)
                    stack.addLast(Frame(node.uri, rel))
                } else {
                    files.add(node)
                }
            }
            onProgress(ToolProgress(ToolPhase.WALK, ++visited, visited + stack.size))
        }
        TreeScanResult(files, dirs, ScanStats(skippedDirs = skippedDirs))
    }
}
