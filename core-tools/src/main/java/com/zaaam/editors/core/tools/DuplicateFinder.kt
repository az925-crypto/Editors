package com.zaaam.editors.core.tools

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

internal fun sha1Streaming(ins: InputStream, limitBytes: Long): ByteArray {
    val digest = MessageDigest.getInstance("SHA-1")
    val buf = ByteArray(64 * 1024)
    var consumed = 0L
    while (true) {
        if (limitBytes >= 0 && consumed >= limitBytes) break
        val want = if (limitBytes < 0) buf.size else minOf(buf.size.toLong(), limitBytes - consumed).toInt()
        val n = ins.read(buf, 0, want)
        if (n < 0) break
        digest.update(buf, 0, n)
        consumed += n
    }
    return digest.digest()
}

private fun hex(bytes: ByteArray): String = buildString(bytes.size * 2) {
    for (b in bytes) {
        val v = b.toInt() and 0xFF
        append("0123456789abcdef"[v ushr 4])
        append("0123456789abcdef"[v and 0x0F])
    }
}

// Pipeline 4 fase (planner §3): group size → head 64KB → re-stat guard → full SHA-1.
// Konservatif: file berubah saat scan DIKECUALIKAN (changedDuringScan) — tidak pernah
// mengklaim duplikat dari data basi. File 0-byte & >MAX_HASH_FILE_BYTES di-skip.
class DuplicateFinder(
    private val openStream: suspend (String) -> InputStream?,
    private val statSize: suspend (String) -> Long?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun find(
        files: List<ToolNode>,
        onProgress: suspend (ToolProgress) -> Unit = {}
    ): DupesOutcome = withContext(ioDispatcher) {
        val oversizedSkipped = files.count { it.size > MAX_HASH_FILE_BYTES }
        val candidates = files.filter { it.size in 1..MAX_HASH_FILE_BYTES }

        // Fase 1: group by size, buang singleton.
        val bySize = candidates.groupBy { it.size }.values.filter { it.size > 1 }

        // Fase 2: head 64KB hash.
        val afterHead = mutableListOf<Pair<ToolNode, String>>()
        var processed = 0
        for (group in bySize) {
            ensureActive()
            for (node in group) {
                val headHash = hashOf(node.uri, HEAD_WINDOW_BYTES) ?: continue
                afterHead.add(node to headHash)
            }
            processed += group.size
            onProgress(ToolProgress(ToolPhase.HASH, processed, candidates.size))
        }
        val headGroups = afterHead.groupBy({ it.second }, { it.first }).values.filter { it.size > 1 }

        // Fase 3+4: re-stat anti data basi, lalu full SHA-1.
        var changedDuringScan = 0
        val finalGroups = mutableListOf<DuplicateGroup>()
        for (group in headGroups) {
            ensureActive()
            val verified = mutableListOf<Pair<ToolNode, String>>()
            for (node in group) {
                val nowSize = statSize(node.uri)
                if (nowSize == null || nowSize != node.size) {
                    changedDuringScan++
                    continue
                }
                val fullHash = hashOf(node.uri, -1) ?: continue
                verified.add(node to fullHash)
            }
            verified.groupBy({ it.second }, { it.first })
                .values
                .filter { it.size > 1 }
                .forEach { same ->
                    finalGroups.add(DuplicateGroup(sizeBytes = same.first().size, nodes = same.sortedBy { it.relPath }))
                }
        }
        DupesOutcome(
            groups = finalGroups.sortedByDescending { it.sizeBytes * it.nodes.size },
            stats = ScanStats(changedDuringScan = changedDuringScan, oversizedSkipped = oversizedSkipped)
        )
    }

    private suspend fun hashOf(uri: String, limitBytes: Long): String? = try {
        val stream = openStream(uri) ?: return null
        stream.use { hex(sha1Streaming(it, limitBytes)) }
    } catch (_: Exception) {
        null
    }

    companion object {
        internal const val HEAD_WINDOW_BYTES = 64L * 1024
        internal const val MAX_HASH_FILE_BYTES = 100L * 1024 * 1024
    }
}
