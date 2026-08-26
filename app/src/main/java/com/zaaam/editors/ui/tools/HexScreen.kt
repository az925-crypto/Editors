package com.zaaam.editors.ui.tools

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.editors.core.fs.FsResult
import com.zaaam.editors.core.tools.MAX_HEX_FILE_BYTES
import com.zaaam.editors.core.tools.formatRow
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.session.humanBytes
import com.zaaam.editors.ui.theme.RetroTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MSG_HEX_LOAD = "Gagal memuat file"
private const val UNDO_LIMIT = 32

// Layar Editor Heks ala mockup: header stencil+LED, banner warning kalau file juga terbuka
// di editor, tabel LCD (offset | 16 byte | ASCII), bar bawah lompat-offset + UNDO + SIMPAN.
//
// Kontrak perf-reviewer: formatRow HANYA untuk baris visible (LazyColumn) dan di-remember
// keyed (version, rowStart). DILARANG precompute seluruh file (1M baris > 100MB heap).
// ByteArray dimutasi IN-PLACE + version bump (jangan copy 16MB per edit byte).
@Composable
fun HexScreen(container: AppContainer) {
    val target by container.hexTargetUri.collectAsState()
    val tabs by container.editorSession.tabs.collectAsState()

    var bytes by remember { mutableStateOf(ByteArray(0)) }
    var version by remember { mutableIntStateOf(0) }
    // index → nilai ORIGINAL saat pertama kali diedit; dipakai highlight .mod dan dibersihkan saat simpan sukses.
    var modified by remember { mutableStateOf(mapOf<Int, Byte>()) }
    val undo = remember { mutableStateListOf<Pair<Int, Byte>>() }
    var fileName by remember { mutableStateOf("") }
    var fileSize by remember { mutableStateOf(0L) }
    var oversize by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var led by remember { mutableStateOf(LedState.IDLE) }
    var ledOwner by remember { mutableStateOf<String?>(null) } // uri pemilik LED — anti timer lintas-file
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var jumpText by remember { mutableStateOf("0x00000000") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(target) {
        bytes = ByteArray(0)
        version++
        modified = emptyMap()
        undo.clear()
        oversize = false
        loadError = false
        led = LedState.IDLE
        fileSize = 0
        fileName = ""
        val uriStr = target ?: return@LaunchedEffect
        val uri = Uri.parse(uriStr)
        // Rider mengirim DOCUMENT uri file (bukan tree) — pakai getDocumentId.
        fileName = try {
            android.provider.DocumentsContract.getDocumentId(uri)
                .substringAfterLast(":").ifBlank { uriStr.takeLast(12) }
        } catch (_: Exception) {
            uriStr.takeLast(12)
        }
        when (val sz = withContext(container.ioDispatcher) { container.fileSystem.statSize(uri) }) {
            is FsResult.Error -> loadError = true
            is FsResult.Success -> {
                fileSize = sz.value
                if (sz.value > MAX_HEX_FILE_BYTES) {
                    oversize = true // keputusan terkunci: guard 16MB per file
                } else {
                    when (val rb = withContext(container.ioDispatcher) {
                        container.fileSystem.readBytes(uri, MAX_HEX_FILE_BYTES)
                    }) {
                        is FsResult.Error -> loadError = true
                        is FsResult.Success -> {
                            bytes = rb.value
                            version++
                        }
                    }
                }
            }
        }
    }

    fun applyEdit(index: Int, newValue: Byte) {
        if (index !in bytes.indices) return
        val old = bytes[index]
        if (old == newValue) return
        // Simpan nilai ORIGINAL sekali saja supaya undo beruntun tetap bisa balik ke disk.
        if (!modified.containsKey(index)) {
            modified = modified + (index to old)
        }
        bytes[index] = newValue
        undo.add(index to old)
        while (undo.size > UNDO_LIMIT) undo.removeAt(0) // cap kontrak HEX_UNDO_MAX
        version++
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 14.dp, end = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    ToolsHeroCard(
                        kicker = "EDITOR HEKS",
                        title = fileName.ifBlank { "belum ada file" },
                        subtitle = humanBytes(fileSize) + " \u00b7 " +
                            if (oversize) "> 16 MB \u2014 ditolak" else "${bytes.size} byte termuat"
                    )
                }
                ToolsLedPill(led)
            }
            val alsoInEditor = target != null && tabs.any { it.uri == target }
            if (alsoInEditor) {
                ToolsBannerBrick(
                    "FILE INI JUGA TERBUKA DI EDITOR",
                    "Dua penulis pada satu file berisiko \u2014 tutup tab editornya dulu kalau ragu.",
                    Modifier.padding(top = 10.dp)
                )
            }
        }
        when {
            target == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Buka file biner dari tab Files \u2014 otomatis mendarat di sini.",
                        color = RetroTokens.DimBone, fontSize = 12.sp, fontFamily = FontFamily.Monospace
                    )
                }
            }
            oversize -> {
                ToolsBannerBrick(
                    "FILE TERLALU BESAR",
                    "Editor heks memuat seluruh isi ke memori (SAF tanpa random-access write) \u2014 batas 16 MB per file.",
                    Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp)
                )
                Spacer(Modifier.weight(1f))
            }
            loadError -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(MSG_HEX_LOAD, color = RetroTokens.Muted, fontSize = 13.sp)
                }
            }
            bytes.isEmpty() -> {
                // File 0-byte SAH — bedakan dari gagal muat; simpan tetap bisa menulis isi baru.
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("(file kosong \u2014 edit byte pertama lewat offset 0 setelah ada isi)",
                        color = RetroTokens.DimBone, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
            else -> {
                val rowCount = (bytes.size + 15) / 16
                fun scrollToOffset() {
                    val off = jumpText.removePrefix("0x").removePrefix("0X").toLongOrNull(16) ?: return
                    if (off < 0) return
                    val row = (off / 16).toInt()
                    if (row in 0 until rowCount) {
                        scope.launch { listState.animateScrollToItem(row) }
                    }
                }
                // Panel LCD ala .hexwrap: bg gelap + border hairline lcd.
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp, end = 14.dp, top = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(RetroTokens.LcdBg)
                        .border(1.dp, RetroTokens.HairlineLcd, RoundedCornerShape(14.dp))
                        .padding(vertical = 6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                ) {
                    items(count = rowCount, key = { it }) { rowStart ->
                        val cells = remember(version, rowStart) { formatRow(bytes, rowStart) }
                        HexRow(
                            rowStart = rowStart,
                            cells = cells,
                            modifiedKeys = modified.keys,
                            onCellTap = { editingIndex = rowStart + it }
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = jumpText,
                        onValueChange = { jumpText = it },
                        singleLine = true,
                        textStyle = TextStyle(color = RetroTokens.Ink, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        cursorBrush = SolidColor(RetroTokens.Olive),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { scrollToOffset() }),
                        decorationBox = { inner ->
                            Row(
                                Modifier
                                    .height(36.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(RetroTokens.Card)
                                    .border(1.dp, RetroTokens.Border, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) { inner() }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ToolsSecondaryButton("UNDO", onClick = {
                        val last = undo.removeLastOrNull()
                        if (last != null) {
                            val (idx, old) = last
                            bytes[idx] = old
                            if (modified[idx] == bytes[idx]) modified = modified - idx
                            version++
                        }
                    }, enabled = undo.isNotEmpty())
                    ToolsPrimaryButton(if (led == LedState.SAVING) "MENYIMPAN\u2026" else "SIMPAN", onClick = {
                        val uriStr = target ?: return@ToolsPrimaryButton
                        scope.launch {
                            led = LedState.SAVING
                            ledOwner = uriStr // guard: timer reset lama tidak boleh menyentuh file lain
                            val result = withContext(container.ioDispatcher) {
                                container.fileSystem.writeBytes(Uri.parse(uriStr), bytes)
                            }
                            if (ledOwner != uriStr) return@launch // target sudah berganti saat menulis
                            led = if (result is FsResult.Success) LedState.SAVED else LedState.ERROR
                            if (result is FsResult.Success) modified = emptyMap()
                            delay(2000)
                            if (ledOwner == uriStr && led == LedState.SAVED) led = LedState.IDLE
                        }
                    }, enabled = led != LedState.SAVING)
                }
            }
        }
    }

    editingIndex?.let { idx ->
        HexEditSheet(
            currentValue = bytes.getOrNull(idx),
            onApply = { newByte ->
                applyEdit(idx, newByte)
                editingIndex = null
            },
            onDismiss = { editingIndex = null }
        )
    }
}

private fun String?.ifBlankCompat(): String? = if (this.isNullOrBlank()) null else this

@Composable
private fun HexRow(
    rowStart: Int,
    cells: List<com.zaaam.editors.core.tools.ByteCell>,
    modifiedKeys: Set<Int>,
    onCellTap: (Int) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "%06X".format(rowStart),
            color = RetroTokens.LcdDim,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(46.dp)
        )
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            cells.forEachIndexed { i, cell ->
                val absIdx = rowStart + i
                val isMod = absIdx in modifiedKeys
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isMod) RetroTokens.OliveWash else Color.Transparent)
                        .clickable { onCellTap(i) }
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        cell.hex,
                        color = if (isMod) RetroTokens.OliveHover else RetroTokens.OliveText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isMod) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        Text(
            cells.joinToString("") { c -> c.ascii?.toString() ?: "\u00b7" },
            color = RetroTokens.OliveDim,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            modifier = Modifier.width(84.dp)
        )
    }
}

// Sheet edit satu byte: tampilkan nilai lama, input 2 digit hex, validasi 00..FF.
@Composable
private fun HexEditSheet(currentValue: Byte?, onApply: (Byte) -> Unit, onDismiss: () -> Unit) {
    var input by remember(currentValue) {
        mutableStateOf(currentValue?.let { "%02X".format(it) } ?: "")
    }
    val parsed = input.trim().toUIntOrNull(16)?.takeIf { it <= 0xFFu }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
            .background(RetroTokens.Graphite.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 10.dp, end = 10.dp, bottom = 74.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(RetroTokens.Card)
                .border(1.dp, RetroTokens.Border, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Text("Edit byte", color = RetroTokens.Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                currentValue?.let { "Nilai sekarang: %02X (%d)".format(it, it.toInt() and 0xFF) } ?: "-",
                color = RetroTokens.Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )
            BasicTextField(
                value = input,
                onValueChange = { s ->
                    if (s.length <= 2 && s.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) input = s
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = RetroTokens.Ink, fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                ),
                cursorBrush = SolidColor(RetroTokens.Olive),
                decorationBox = { inner ->
                    Row(
                        Modifier
                            .padding(top = 12.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(RetroTokens.Shell)
                            .border(1.dp, RetroTokens.Border, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        inner()
                        Spacer(Modifier.width(4.dp))
                        Text("hex 00\u2013FF", color = RetroTokens.DimBone, fontSize = 11.sp)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolsSecondaryButton("BATAL", onDismiss, Modifier.weight(1f))
                ToolsPrimaryButton(
                    "TERAPKAN",
                    onClick = { parsed?.toInt()?.let { onApply(it.toByte()) } },
                    enabled = parsed != null,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
