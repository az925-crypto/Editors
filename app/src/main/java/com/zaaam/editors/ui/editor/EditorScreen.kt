package com.zaaam.editors.ui.editor

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaaam.editors.core.editor.EditorEngine
import com.zaaam.editors.core.editor.LanguageResolver
import com.zaaam.editors.core.editor.SoraThemeMapper
import com.zaaam.editors.core.fs.isWebFile
import com.zaaam.editors.di.AppContainer
import com.zaaam.editors.session.AppScreen
import com.zaaam.editors.ui.preview.PreviewViewModel
import com.zaaam.editors.ui.theme.RetroTokens
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// LOW FIX (80841fd re-audit): holder biasa (bukan Compose State) untuk referensi Job debounce
// yang berubah nyaris tiap keystroke — lihat catatan di pemakaiannya di bawah. Nama field
// tetap `.value` supaya semua call site pendingChangeJob.value tidak perlu diubah.
private class JobHolder {
    var value: Job? = null
}

// MEDIUM FIX (retry storm): batas + backoff retry apply language TextMate. Dulu update{}
// mencoba ulang di SETIAP recomposition sampai preload kelar (bisa ratusan kali per detik).
private const val LANGUAGE_APPLY_MAX_ATTEMPTS = 8

private fun languageRetryDelayMs(attempt: Int): Long =
    minOf(250L shl attempt.coerceAtMost(3), 2000L)

@Composable
fun EditorScreen(container: AppContainer) {
    val vm: EditorViewModel = viewModel { EditorViewModel(container) }
    val state by vm.uiState.collectAsState()

    // Live preview split: instance PreviewViewModel yang SAMA dengan layar Preview
    // (viewModel{} activity-scoped, tanpa NavHost) — pipeline tick→debounce→compose
    // tetap satu; pane di bawah hanya konsumen render dari state yang sudah ada.
    val previewVm: PreviewViewModel = viewModel { PreviewViewModel(container) }
    val previewState by previewVm.uiState.collectAsState()
    val splitEnabled by container.splitPreviewEnabled.collectAsState()

    // Seed instan dokumen aktif ke shared VM saat tab berganti (kontrak sama dengan
    // LaunchedEffect(activeUri) milik PreviewScreen) supaya pane tidak menunggu tick
    // pertama ketika split baru dinyalakan / pindah antar file web.
    LaunchedEffect(state.activeUri) {
        if (isWebFile(state.activeUri)) previewVm.showActiveFile(state.activeUri)
    }

    if (state.tabs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().background(RetroTokens.Shell), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Tidak ada file terbuka", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = RetroTokens.Graphite)
                Text(text = "Buka file dari tab Files untuk mulai mengedit.", fontSize = 13.sp, color = RetroTokens.Dim)
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(RetroTokens.Shell)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.tabs.forEach { tab ->
                val isActive = tab.uri == state.activeUri
                Card(
                    modifier = Modifier.clickable { vm.switchTab(tab.uri) },
                    colors = CardDefaults.cardColors(containerColor = if (isActive) RetroTokens.Card else RetroTokens.Shell),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (tab.dirty) Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(RetroTokens.LedOrange))
                        Text(text = tab.displayName, fontSize = 13.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = RetroTokens.Graphite)
                        Text(text = "×", modifier = Modifier.clickable { vm.closeTab(tab.uri) }.padding(4.dp), color = RetroTokens.Dim)
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Find", modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(RetroTokens.Card).padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, color = RetroTokens.Dim)
            if (isWebFile(state.activeUri)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // WHY chip hanya untuk file web: non-web (kt/txt/biner) tidak punya
                    // html untuk di-preview — pane kosong cuma jadi noise visual.
                    val splitOn = splitEnabled
                    Text(
                        text = "Split ◫",
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (splitOn) RetroTokens.Olive else RetroTokens.Card)
                            .semantics {
                                contentDescription = if (splitOn) "Matikan pratinjau terbagi" else "Aktifkan pratinjau terbagi"
                            }
                            .clickable { container.setSplitPreview(!splitOn) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        color = if (splitOn) RetroTokens.Ink else RetroTokens.Dim,
                        fontWeight = if (splitOn) FontWeight.Bold else FontWeight.Normal
                    )
                    // Fase 4: chip kini fungsional — lompat langsung ke layar Preview.
                    Text(
                        text = "Preview ▶",
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(RetroTokens.Olive).clickable {
                            container.screenState.value = AppScreen.PREVIEW
                        }.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        color = RetroTokens.Ink,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        val activeUri = state.activeUri
        val content = state.content
        val languageResolver = remember { LanguageResolver() }
        val themeMapper = remember { SoraThemeMapper() }
        val appliedScope = remember { mutableStateOf<String?>(null) }

        // MEDIUM: cache TextMateLanguage per scope supaya pindah tab antar file berbahasa sama
        // tidak membangun ulang grammar language dari nol tiap kali (hitch ~50ms).
        val languageCache = remember { mutableMapOf<String, TextMateLanguage>() }

        // MEDIUM: hanya push `content` ke editor waktu benar-benar pindah tab, bukan tiap
        // recompose (mis. saat saveStatus berubah), supaya tidak toString() buffer besar tiap frame.
        val lastAppliedContentUri = remember { mutableStateOf<String?>(null) }
        val editorRef = remember { mutableStateOf<EditorEngine?>(null) }

        // MEDIUM FIX (retry storm): sinyal "preload TextMate sudah selesai" dari EditorsApp —
        // flip false→true memicu LaunchedEffect di bawah mencoba apply language lagi segera,
        // alih-alih nunggu recomposition apa pun.
        val textMateReady by EditorEngine.textMateReady.collectAsState()

        // LOW FIX (80841fd re-audit): sebelumnya pakai mutableStateOf<Job?>, jadi tiap kali Job
        // baru di-assign (nyaris tiap keystroke, karena event dibatalkan+dibuat ulang tiap
        // ContentChangeEvent) itu tercatat sebagai state read di dalam AndroidView.update{} —
        // bikin blok update itu di-invoke ulang oleh Compose walau activeUri/content tidak
        // berubah sama sekali. pendingChangeJob murni referensi imperatif dipakai lintas-lambda
        // (factory & update & onDispose), bukan sesuatu yang perlu memicu recomposition/update
        // kalau berubah — jadi cukup pakai holder biasa (bukan Compose State).
        val pendingChangeJob = remember { JobHolder() }
        val coroutineScope = rememberCoroutineScope()

        DisposableEffect(Unit) {
            onDispose {
                // Kalau layar ini dibongkar sebelum debounce sempat jalan, flush dulu biar
                // ketikan terakhir user tidak hilang. Sasar URI eksplisit (tab yang lagi
                // ditampilkan editor saat itu), bukan activeUri implisit dari ViewModel.
                pendingChangeJob.value?.let { job ->
                    if (job.isActive) {
                        job.cancel()
                        editorRef.value?.let { ed -> vm.onContentChange(lastAppliedContentUri.value, ed.text.toString()) }
                    }
                }
            }
        }

        // WHY SATU BoxWithConstraints sebagai parent editor+pane: node AndroidView sora
        // tidak boleh pindah parent composable saat rotasi/aktif-nonaktif split — kalau
        // pindah, factory dijalankan ulang dan state editor (cursor/undo) hilang. Posisi
        // diatur via alignment + fraction pada MODIFIER, bukan ganti Column/Row root.
        val showSplit = splitEnabled && isWebFile(state.activeUri)
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val isPortrait = maxWidth < maxHeight
            val editorModifier = when {
                !showSplit -> Modifier.fillMaxSize().background(RetroTokens.LcdBg)
                isPortrait -> Modifier.align(Alignment.TopStart).fillMaxWidth().fillMaxHeight(0.5f).background(RetroTokens.LcdBg)
                else -> Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(0.5f).background(RetroTokens.LcdBg)
            }
            AndroidView(
                modifier = editorModifier,
            factory = { ctx ->
                val editor = EditorEngine.create(ctx)
                editorRef.value = editor
                editor.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                try {
                    // CRITICAL 1: TextMate sudah di-preload di EditorsApp.onCreate (Dispatchers.IO).
                    // Di sini cuma bikin color scheme dari registry yang sudah (atau lagi) di-load,
                    // TIDAK memanggil initTextMate() lagi supaya main thread tidak nge-freeze.
                    val scheme: TextMateColorScheme = EditorEngine.createColorScheme()
                    themeMapper.applyChromeOverrides(scheme)
                    editor.setColorScheme(scheme)
                } catch (_: Exception) {
                    // Preload di background mungkin belum selesai — editor tetap jalan dengan
                    // skema default, tidak fatal.
                }
                editor.subscribeEvent(ContentChangeEvent::class.java) { event, _ ->
                    if (event.action != ContentChangeEvent.ACTION_SET_NEW_TEXT) {
                        // CRITICAL FIX (race debounce): URI sumber DI-CAPTURE saat event terjadi,
                        // bukan dibaca ulang setelah delay. Timeline korupsi lama: ketik di A →
                        // delay(200) jalan → user pindah ke tab B → job resume dan menulis teks A
                        // lewat activeUri implisit (sudah B) → contentMap[B] tertimpa teks A.
                        // Guard `lastAppliedContentUri == sourceUri`: kalau update{} sudah ganti
                        // tab, skip (flush leavingUri sudah handle); kalau job menang race, tulis
                        // EKSPLISIT ke tab sumber.
                        val sourceUri = lastAppliedContentUri.value
                        pendingChangeJob.value?.cancel()
                        pendingChangeJob.value = coroutineScope.launch {
                            delay(200)
                            if (lastAppliedContentUri.value == sourceUri) {
                                vm.onContentChange(sourceUri, editor.text.toString())
                            }
                        }
                    }
                }
                editor
            },
            update = { editor ->
                if (activeUri != lastAppliedContentUri.value) {
                    // CRITICAL FIX (80841fd re-audit): pada titik ini state.activeUri SUDAH
                    // pindah ke tab baru (B), tapi editor.text di bawah masih isi tab lama (A)
                    // karena setText(content) baru dipanggil setelah blok ini. Kalau flush
                    // pakai vm.onContentChange(text) yang lama, ViewModel baca activeUri-nya
                    // sendiri (sudah B) lalu nulis teks A ke contentMap[B] — korupsi antar-tab.
                    // Fix: sasar URI tab yang DITINGGALKAN secara eksplisit — yaitu
                    // lastAppliedContentUri.value, sebelum baris ini menimpanya jadi activeUri.
                    val leavingUri = lastAppliedContentUri.value
                    pendingChangeJob.value?.let { job ->
                        if (job.isActive) {
                            job.cancel()
                            vm.onContentChange(leavingUri, editor.text.toString())
                        }
                    }
                    lastAppliedContentUri.value = activeUri
                    if (editor.text?.toString() != content) {
                        editor.setText(content)
                    }
                }
            }
            )
            if (showSplit) {
                // WHY divider 1.dp Border (bukan shadow/elevation): bahasa visual retro-lcd
                // pakai hairline, bukan elevasi Material. Posisi = garis batas fraksi 0.5.
                if (isPortrait) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth()
                            .offset(y = maxHeight * 0.5f)
                            .height(1.dp)
                            .background(RetroTokens.Border)
                    )
                    SplitPreviewPane(
                        activeDisplayName = state.tabs.firstOrNull { it.uri == state.activeUri }?.displayName ?: "",
                        renderedHtml = previewState.html,
                        isLoading = previewState.isLoading,
                        onConsole = previewVm::addConsole,
                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().fillMaxHeight(0.5f)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .offset(x = maxWidth * 0.5f)
                            .width(1.dp)
                            .background(RetroTokens.Border)
                    )
                    SplitPreviewPane(
                        activeDisplayName = state.tabs.firstOrNull { it.uri == state.activeUri }?.displayName ?: "",
                        renderedHtml = previewState.html,
                        isLoading = previewState.isLoading,
                        onConsole = previewVm::addConsole,
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.5f)
                    )
                }
            }
        }

        // MEDIUM FIX (retry storm): apply language TextMate dipindah dari update{} ke
        // LaunchedEffect dengan backoff eksponensial. Dulu tiap recomposition mencoba ulang
        // kalau appliedScope gagal — saat preload TextMate belum kelar, itu jadi retry storm.
        // Sekarang: retry terbatas + delay naik, dan flip textMateReady (preload selesai di
        // EditorsApp) langsung memicu percobaan baru tanpa nunggu recomposition.
        LaunchedEffect(activeUri, textMateReady) {
            val ed = editorRef.value ?: return@LaunchedEffect
            if (activeUri == null || appliedScope.value == activeUri) return@LaunchedEffect
            var attempt = 0
            while (appliedScope.value != activeUri && attempt < LANGUAGE_APPLY_MAX_ATTEMPTS) {
                try {
                    val scope = languageResolver.resolve(activeUri)
                    val language = languageCache.getOrPut(scope) { TextMateLanguage.create(scope, true) }
                    ed.setEditorLanguage(language)
                    appliedScope.value = activeUri
                } catch (_: Exception) {
                    attempt++
                    if (attempt < LANGUAGE_APPLY_MAX_ATTEMPTS) delay(languageRetryDelayMs(attempt))
                }
            }
        }

        if (state.saveStatus != SaveStatus.Idle) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(when (state.saveStatus) { is SaveStatus.Saving -> RetroTokens.LedOrange; is SaveStatus.Saved -> RetroTokens.LedGreen; is SaveStatus.Error -> RetroTokens.Brick; else -> Color.Transparent }))
                Text(
                    text = when (val s = state.saveStatus) {
                        is SaveStatus.Saving -> "Menyimpan…"
                        is SaveStatus.Saved -> "Tersimpan ${s.time}"
                        // Pesan sengaja generik — detail exception tidak dibocorkan ke UI.
                        is SaveStatus.Error -> "Gagal menyimpan"
                        else -> ""
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RetroTokens.Dim
                )
            }
        }
    }
}
