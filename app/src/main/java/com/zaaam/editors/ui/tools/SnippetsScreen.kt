package com.zaaam.editors.ui.tools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.editors.core.fs.FsResult
import com.zaaam.editors.core.tools.Snippet
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.session.SnippetImportOutcome
import com.zaaam.editors.session.exportFileName
import com.zaaam.editors.ui.theme.RetroTokens
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MSG_SNIPPET_IO = "Operasi file gagal"

// Layar Snippet ala mockup: kartu stencil+tagpill, chip EKSPOR/IMPOR/CODEXA (disabled —
// menunggu spec), FORM MINI tambah/edit (keputusan user: masuk v0.2), prefs key snippets_v1.
@Composable
fun SnippetsScreen(container: AppContainer) {
    val repo = container.snippetRepository
    // Load TIDAK di main thread (parse JSON linear — 1000 snippet bisa puluhan ms):
    // mulai kosong, isi via LaunchedEffect + withContext(IO).
    var snippets by remember { mutableStateOf(emptyList<Snippet>()) }
    var editing by remember { mutableStateOf<Snippet?>(null) } // null = tidak ngedit; Snippet = edit; Snippet kosong = baru
    var showForm by remember { mutableStateOf(false) }
    var importReport by remember { mutableStateOf<String?>(null) }
    var ioError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        snippets = withContext(container.ioDispatcher) { repo.load() }
    }

    fun reload() {
        scope.launch { snippets = withContext(container.ioDispatcher) { repo.load() } }
    }

    fun importFrom(uriStr: String) {
        scope.launch {
            when (val r = withContext(container.ioDispatcher) { container.fileSystem.readText(Uri.parse(uriStr)) }) {
                is FsResult.Error -> ioError = true
                is FsResult.Success -> {
                    when (val outcome = withContext(container.ioDispatcher) { repo.importJson(r.value) }) {
                        is SnippetImportOutcome.Imported -> {
                            importReport = "${outcome.added} snippet ditambah \u00b7 " +
                                "${outcome.skippedExisting} dilewati (sudah ada) \u00b7 " +
                                "${outcome.skippedInvalid} gagal parse."
                            reload()
                        }
                        SnippetImportOutcome.BadSchema -> importReport = "File bukan skema zaaam-snippets yang sah."
                    }
                }
            }
        }
    }

    fun exportTo(parentUri: Uri) {
        scope.launch {
            val name = exportFileName(System.currentTimeMillis())
            val created = withContext(container.ioDispatcher) {
                container.fileSystem.createFile(parentUri, "application/json", name)
            }
            when (created) {
                is FsResult.Error -> ioError = true
                is FsResult.Success -> {
                    val written = withContext(container.ioDispatcher) {
                        container.fileSystem.writeText(created.value, repo.exportJson())
                    }
                    if (written is FsResult.Success) {
                        // Nama AKTUAL dari provider (bisa di-rename jadi " (1)" kalau bentrok).
                        val actualName = try {
                            android.provider.DocumentsContract.getDocumentId(created.value)
                                .substringAfterLast(":")
                        } catch (_: Exception) {
                            name
                        }
                        importReport = "Terekspor ke $actualName."
                    } else ioError = true
                }
            }
        }
    }

    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(::exportTo)
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importFrom(it.toString()) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 14.dp, end = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    ToolsHeroCard(
                        kicker = "ALAT / SNIPPET",
                        title = "Snippet",
                        subtitle = "${snippets.size} tersimpan \u00b7 skema zaaam-snippets v1"
                    )
                }
                ToolsPrimaryButton("+ BARU", onClick = {
                    editing = null
                    showForm = true
                })
            }
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolsChip("EKSPOR JSON", selected = false, onClick = { treePicker.launch(null) }, showDot = false)
                ToolsChip("IMPOR FILE", selected = false, onClick = { filePicker.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }, showDot = false)
                Box(Modifier.alpha(0.45f)) {
                    ToolsChip("CODEXA", selected = false, onClick = {}, showDot = false)
                }
            }
            if (ioError) {
                Text(
                    MSG_SNIPPET_IO,
                    color = RetroTokens.Brick, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(snippets, key = { it.id }) { s ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(RetroTokens.Card)
                        .border(1.dp, RetroTokens.Border, RoundedCornerShape(16.dp))
                        .clickable {
                            editing = s
                            showForm = true
                        }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ToolsStencil(s.language.take(2).uppercase().ifBlank { "??" }, RetroTokens.Olive)
                        Column(Modifier.weight(1f)) {
                            Text(s.name, color = RetroTokens.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            if (s.tags.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    s.tags.take(3).forEach { tag -> ToolsTagPill(tag) }
                                }
                            }
                        }
                        Text("\u22ee", color = RetroTokens.Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                ToolsNote("Impor Codexa menunggu spesifikasi format dari developer \u2014 adapter disiapkan, tinggal colok.")
            }
        }
    }

    if (showForm) {
        SnippetFormSheet(
            original = editing,
            onSave = { name, language, tagsCsv, code ->
                scope.launch {
                    withContext(container.ioDispatcher) {
                        if (editing == null) repo.add(name, language, tagsCsv, code)
                        else repo.update(editing!!.id, name, language, tagsCsv, code)
                    }
                    reload()
                    showForm = false
                }
            },
            onDelete = editing?.let { s ->
                {
                    scope.launch {
                        withContext(container.ioDispatcher) { repo.delete(s.id) }
                        reload()
                        showForm = false
                    }
                }
            },
            onDismiss = { showForm = false }
        )
    }

    ToolsSheet(
        visible = importReport != null,
        title = if (ioError) "Gagal" else "Selesai",
        body = importReport ?: "",
        confirmLabel = null,
        dismissLabel = null,
        onConfirm = null,
        onDismiss = {
            importReport = null
            ioError = false
        }
    )
}

// Form mini tambah/edit snippet (keputusan user terkunci: MASUK v0.2).
@Composable
private fun SnippetFormSheet(
    original: Snippet?,
    onSave: (name: String, language: String, tagsCsv: String, code: String) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var name by remember(original) { mutableStateOf(original?.name ?: "") }
    var language by remember(original) { mutableStateOf(original?.language ?: "") }
    var tags by remember(original) { mutableStateOf(original?.tags?.joinToString(", ") ?: "") }
    var code by remember(original) { mutableStateOf(original?.code ?: "") }

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
            Text(
                if (original == null) "Snippet baru" else "Edit snippet",
                color = RetroTokens.Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
            )
            FormField(value = name, onValue = { name = it }, hint = "Nama", modifier = Modifier.padding(top = 12.dp))
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(value = language, onValue = { language = it }, hint = "bahasa", modifier = Modifier.weight(1f), mono = true)
                FormField(value = tags, onValue = { tags = it }, hint = "tag1, tag2", modifier = Modifier.weight(2f), mono = true)
            }
            CodeField(value = code, onValue = { code = it }, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolsSecondaryButton("BATAL", onDismiss, Modifier.weight(1f))
                if (onDelete != null) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(RetroTokens.BrickWash)
                            .border(1.dp, RetroTokens.Brick.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                            .clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("HAPUS", color = RetroTokens.Brick, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                ToolsPrimaryButton(
                    "SIMPAN",
                    onClick = { onSave(name, language, tags, code) },
                    enabled = name.isNotBlank() && code.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FormField(value: String, onValue: (String) -> Unit, hint: String, modifier: Modifier = Modifier, mono: Boolean = false) {
    BasicTextField(
        value = value,
        onValueChange = onValue,
        singleLine = true,
        textStyle = TextStyle(color = RetroTokens.Ink, fontSize = 13.sp, fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default),
        cursorBrush = SolidColor(RetroTokens.Olive),
        decorationBox = { inner ->
            Row(
                Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(RetroTokens.Shell)
                    .border(1.dp, RetroTokens.Border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (value.isEmpty()) Text(hint, color = RetroTokens.Muted, fontSize = 13.sp)
                inner()
            }
        },
        modifier = modifier
    )
}

@Composable
private fun CodeField(value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = onValue,
        textStyle = TextStyle(color = RetroTokens.Ink, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
        cursorBrush = SolidColor(RetroTokens.Olive),
        decorationBox = { inner ->
            Column(
                Modifier
                    .height(110.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(RetroTokens.Shell)
                    .border(1.dp, RetroTokens.Border, RoundedCornerShape(14.dp))
                    .padding(10.dp)
            ) {
                if (value.isEmpty()) Text("isi kode\u2026", color = RetroTokens.Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                inner()
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}
