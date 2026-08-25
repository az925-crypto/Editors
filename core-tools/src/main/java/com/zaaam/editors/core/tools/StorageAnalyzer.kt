package com.zaaam.editors.core.tools

// Agregat murni dari hasil walk — tanpa IO, tested langsung.
internal fun aggregateAnalysis(
    files: List<ToolNode>,
    dirs: List<ToolNode>,
    skippedDirs: Int,
    maxLists: Int = 10
): AnalyzerReport {
    val totalBytes = files.sumOf { it.size }
    val largestFiles = files.asSequence()
        .sortedByDescending { it.size }
        .take(maxLists)
        .map { FileInfoEntry(it.name, it.uri, it.relPath, it.size) }
        .toList()

    // Agregat per folder TOP-LEVEL di bawah root: "assets/img/x.png" → label "assets".
    // File langsung di root dikelompokkan "(akar)".
    data class Acc(var bytes: Long = 0, var count: Int = 0)
    val byTop = LinkedHashMap<String, Acc>()
    for (f in files) {
        val top = f.relPath.substringBefore("/", missingDelimiterValue = "")
        val key = if (top.isEmpty()) "(akar)" else top
        val acc = byTop.getOrPut(key) { Acc() }
        acc.bytes += f.size
        acc.count++
    }
    val largestDirs = byTop.entries.asSequence()
        .sortedByDescending { it.value.bytes }
        .take(5)
        .map { DirAggregate(it.key, it.value.bytes, it.value.count) }
        .toList()

    return AnalyzerReport(
        fileCount = files.size,
        folderCount = dirs.size,
        skippedDirs = skippedDirs,
        totalBytes = totalBytes,
        largestFiles = largestFiles,
        largestDirs = largestDirs
    )
}
