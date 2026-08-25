package com.zaaam.editors.core.tools

// Model Phase 2 (core-tools). SEMUA uri disimpan sebagai String + operasi file di-inject
// lewat lambda — pola AutosaveCoordinator — supaya engine bisa di-unit-test JVM murni
// tanpa menyentuh android.net.Uri.

// relPath = path sintetis dari root ("assets/img/hero.png"; anak langsung root = nama saja).
// Dibangun TreeScanner saat walk, dipakai analyzer untuk agregat per folder top-level.
data class ToolNode(
    val name: String,
    val uri: String,
    val relPath: String,
    val isDir: Boolean,
    val size: Long = 0,
    val isHidden: Boolean = false
)

enum class ToolPhase { WALK, HASH }

data class ToolProgress(val phase: ToolPhase, val done: Int, val totalEstimate: Int)

data class FileInfoEntry(val name: String, val uri: String, val relPath: String, val bytes: Long)

data class DirAggregate(val label: String, val bytes: Long, val fileCount: Int)

data class AnalyzerReport(
    val fileCount: Int,
    val folderCount: Int,
    val skippedDirs: Int,
    val totalBytes: Long,
    val largestFiles: List<FileInfoEntry>,
    val largestDirs: List<DirAggregate>
)

data class DuplicateGroup(val sizeBytes: Long, val nodes: List<ToolNode>)

data class ScanStats(
    val skippedDirs: Int = 0,
    val changedDuringScan: Int = 0,
    val oversizedSkipped: Int = 0
)

data class TreeScanResult(
    val files: List<ToolNode>,
    val dirs: List<ToolNode>,
    val stats: ScanStats
)

data class DupesOutcome(val groups: List<DuplicateGroup>, val stats: ScanStats)

data class MatchPreview(val lineNumber: Int, val lineText: String, val startInLine: Int, val endInLine: Int)

data class FindOutcome(val totalMatches: Int, val previews: List<MatchPreview>)

data class ReplaceOutcome(val newText: String, val count: Int)

// Laporan per file saat scan find-replace; outcome null = file tak terbaca/bukan teks.
data class FileFindReport(val node: ToolNode, val outcome: FindOutcome?)

sealed interface ReplaceFileOutcome {
    data class Success(val count: Int) : ReplaceFileOutcome
    data object ChangedSkipped : ReplaceFileOutcome
    data object Failed : ReplaceFileOutcome
}
