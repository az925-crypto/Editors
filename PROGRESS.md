# Progress & Handoff — zaaam/editors

**Tanggal:** 2026-08-25 (update: bersih-bersih dead code + STRUCTURE.md)
**HEAD:** `7d23b56` (main) — WAJIB cek CI hijau dulu (`gh run list --limit 1`) sebelum lanjut fix apapun
**Repo:** https://github.com/az925-crypto/Editors.git
**Package:** `com.zaaam.editors` — minSdk 26 / targetSdk 36 / compileSdk 36 / Kotlin 2.4.10 / AGP 9.3.0 / Gradle 9.5.0 / JDK 21
**Tag terakhir:** `v0.1.1` (`4383466`) — Release workflow HIJAU (run `32814065578`, APK debug-signed di Releases)

---

## BACA INI DULU (untuk AI/user berikutnya)

1. **Build/test BERAT WAJIB lewat GitHub Actions CI** — JANGAN gradle lokal di Termux (instruksi user, notes.txt #1). Iterasi: edit → commit → `git push origin main` → `gh run list --limit 1` → kalau gagal `gh run view <id> --log-failed | grep "e: file"`.
2. **Workflow wajib (AGENTS.md):** explore → planner (>1 file) → design (kalau sentuh UI) → build↔test loop sampai CI GREEN → migration-guard (n/a, belum ada Room) → security-reviewer + bug-reviewer + performance-reviewer PARALEL (semuanya BLOCKING, fix-loop sampai BLOCKING: no) → maintainability-reviewer (non-blocking) → docs → commit.
3. **AGP 9 quirks (riwayat 15 fix):** jangan tambah plugin `kotlin-android` (built-in), jangan pakai `kotlinOptions{}` (pakai `kotlin { jvmToolchain(17) }`), library modules TANPA `targetSdk`, `signingConfigs` sebelum `buildTypes`.
4. **JITPACK DILARANG** — riwayat timeout 3x sia-sia. Sora dari Maven Central: `io.github.Rosemoe.sora-editor:{editor,language-textmate}:0.23.6` (0.24.x hanya prerelease GitHub).
5. **Sora API gotchas (sudah diverifikasi ke source 0.23.6):**
   - `EditorColorScheme` ada di `io.github.rosemoe.sora.widget.schemes` (BUKAN `widget`)
   - `IThemeSource` ada di `org.eclipse.tm4e.core.registry` (BUKAN package sora)
   - `ThemeModel.setDark(true)` — `isDark` bukan field publik
   - Setup pattern: `FileProviderRegistry.addFileProvider(AssetsFileResolver)` → `ThemeRegistry.loadTheme(ThemeModel(IThemeSource.fromInputStream(stream,path,null),"name"))` → `setTheme(name)` → `GrammarRegistry.loadGrammars("textmate/languages.json")` → `editor.setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()))`
   - scope name di languages.json HARUS persis sama dengan `scopeName` di dalam tmLanguage.json (registry throw mismatch)
6. **Desugaring WAJIB** untuk language-textmate dengan minSdk<33: sudah aktif (`isCoreLibraryDesugaringEnabled=true` + `desugar_jdk_libs:2.1.5` di app module).
7. **core-editor expose sora via `api()` bukan `implementation`** — app butuh classpath supertype CodeEditor.
8. **Keystore release belum ada** → APK release pakai fallback debug signing (by design dulu). Real keystore via `gh secret set RELEASE_KEYSTORE_B64` + RELEASE_STORE_PASS/KEY_ALIAS/KEY_PASS.
9. **release.yml sudah fixed** (commit `4383466`): secrets via step `env:` (jangan `${{ secrets.* }}` langsung di `run:` = command injection risk; `secrets` context ilegal di step-level `if`), trigger `push: tags v*` + `workflow_dispatch`. Uji: bump versionName/versionCode → push tag `vX.Y.Z`.
10. **User sering benerin sendiri via zip** (`~/git/Editors-main-fixed*.zip`, export GitHub tanpa `.github`/dotfiles). Cara apply: `unzip -d /tmp/opencode/x`, `diff -rq` vs working tree (exclude `.git/.github/.gitignore/tmp-apk/`), review diff, copy file yang relevan, jangan overwrite `.github`.
11. **Peta lengkap seluruh file + penjelasan per file: `STRUCTURE.md` (root)** — baca SEBELUM sentuh kode. Berisi: alur data inti, kontrak `editorContents`, isi/gotcha tiap file, lokasi tiap masalah terbuka, dan status file (aktif/reserved/dihapus).
12. `ah.txt` sudah DIHAPUS (snapshot basi pra-Sora). `PreviewWebViewFactory.kt` + `addConsole()` DIPERTAHANKAN sebagai reserved fix security/Fase 4 — jangan dihapus lagi. Yang benar-benar sudah dibuang: 5 stub UI komponen + stub AutoSaveController (commit `7d23b56`).

---

## STATE SEKARANG

### Done (Fase 1-5 besar)
- **Fase 1 infra:** CI hijau stabil (assembleDebug+testDebugUnitTest+lintDebug), release.yml fixed+hijau v0.1.1, theme RetroTokens, launcher adaptive, app shell nav Files/Editor/Preview.
- **Fase 2 core-fs REAL:** `SafFileSystemImpl` (listChildren/readText/writeText/mkdir/rename/delete, cursor `use{}`, readText guard 2MB via querySize), `TreeAccess.takePersistableUriPermission/release/isPermissionValid` real, rename return new URI.
- **Fase 2 UI real SAF:** `FilesViewModel` (OpenDocumentTree flow, pathStack main-only, loadJob cancel + generation guard, recents SharedPreferences parse first/last-pipe-safe, restore tree uri, breadcrumb segments) + `FilesScreen` (launcher, breadcrumb dinamis, skeleton 8 bar, banner denied, empty states S0-S4b, FileRow cartridge + tanggal real via top-level SimpleDateFormat) + `SafDialog` full spec §9.5 (scrim blocking, check rows, error banner, Pilih folder/Nanti).
- **Fase 3 core-editor REAL:** `EditorEngine extends CodeEditor`, preload TextMate di `EditorsApp.onCreate` via `CoroutineScope(SupervisorJob()+Dispatchers.IO)` (idempotent @Synchronized), factory hanya createColorScheme+applyChromeOverrides, subscribeEvent ContentChangeEvent → debounce 200ms JobHolder + flush explicit-uri saat pindah tab/dispose, `lastAppliedContentUri` gate setText hanya saat pindah tab, `appliedScope` set-setelah-sukses (auto-retry highlight), languageCache per-scope. `SoraThemeMapper` chrome overrides (scrollbar/completion window/action window). Assets: `app/src/main/assets/textmate/` — themes/retro-lcd.json (VSCode format, palet LCD RetroTokens) + languages.json + 6 grammar custom minimal (html/css/js/kotlin/python/json).
- **Fase 3 wiring:** `AppContainer.editorContents: ConcurrentHashMap` shared antara FilesViewModel (penulis, SEBELUM addTab) dan EditorViewModel (contentMap get() = editorContents). EditorScreen AndroidView(CodeEditor), save LED pill.
- **Reviewer round 1+2:** security BLOCKING no; performance BLOCKING no (semua fix verified); bug-reviewer round 2 masih BLOCKING yes (lihat bawah).
- **Cleanup dead code (`7d23b56`):** hapus 5 stub UI komponen (`ui/components/{Bevel,BootOverlay,BottomNavPhysical,HardwareBar,LedIndicator}.kt`) + stub `AutoSaveController` — semua verifikasi grep nol referensi. `PreviewWebViewFactory.kt`, `ConsoleBridge`, dan `addConsole()` DIPERTAHANKAN (reserved fix security WebView + Fase 4). `ah.txt` dihapus dari working tree. Peta file lengkap: **STRUCTURE.md**.

### ⚠️ PENDING FIX — bug-reviewer round 3 (VERIFIED REAL, bukan false positive)

Semua sudah ditrace timeline, tinggal eksekusi. Setelah ini re-run bug-reviewer.

**[Critical] Race debounce implicit-uri — `app/src/main/java/com/zaaam/editors/ui/editor/EditorScreen.kt:158-166`**
Timeline korupsi: ketik di A t=0 → job delay(200) dibuat → switchTab(B) t=190 (activeUri=B) → job resume t=200 SEBELUM update{} cancel → baris 163 `vm.onContentChange(editor.text.toString())` implisit → contentMap[B] tertimpa teks A.
Fix: capture uri + text guard saat event:
```kotlin
editor.subscribeEvent(ContentChangeEvent::class.java) { event, _ ->
    if (event.action != ContentChangeEvent.ACTION_SET_NEW_TEXT) {
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
```
Guard `lastAppliedContentUri == sourceUri`: kalau update{} sudah ganti tab, skip (flush leavingUri sudah handle); kalau job menang race, tulis eksplisit ke A (onContentChange(uri,...) tidak lagi butuh activeUri match untuk contentMap).

**[Medium] saveJob single global — `app/src/main/java/com/zaaam/editors/ui/editor/EditorViewModel.kt:~88`**
`saveJob?.cancel()` tiap keystroke membunuh markSaved(A) yang belum jalan → dirty LED A nyangkut. Fix: per-uri:
```kotlin
private val saveJobs = mutableMapOf<String, Job>()
// di onContentChange(uri, newContent):
saveJobs.remove(targetUri)?.cancel()
saveJobs[targetUri] = viewModelScope.launch {
    delay(900)
    container.editorSession.markSaved(targetUri)
    saveJobs.remove(targetUri)
    ...update saveStatus dengan guard activeUri==targetUri...
    delay(2000)
    if (_uiState.value.activeUri == targetUri) { ...Idle }
}
```

**[Medium] Collector tabs tidak reset saveStatus — `EditorViewModel.kt:43`**
Buka file baru dari Files → addTab → collector update tanpa reset → "Menyimpan…" milik tab lama nempel di tab baru. Fix di init collector:
```kotlin
_uiState.update {
    it.copy(
        tabs = tabs, activeUri = active, content = content,
        saveStatus = if (it.activeUri != active) SaveStatus.Idle else it.saveStatus
    )
}
```

**[Low] closeTab resurrect map entry**
closeTab hapus contentMap[uri], lalu flush onDispose/update nulis balik. Fix guard di awal `onContentChange(uri, newContent)`:
```kotlin
if (_uiState.value.tabs.none { it.uri == targetUri }) return
```

### Security Medium OPEN (round 1, belum difix)
- Over-grant: `takePersistableUriPermission(READ or WRITE)` selalu minta dua-duanya (`core-fs/FileKindResolver.kt:36`). Ideal: cek flags yang benar-benar di-grant dari intent result.
- `isPermissionValid` cuma cek `isReadPermission` (`FileKindResolver.kt:60`) — write revoke tak terdeteksi, save gagal diam-diam.
- `PreviewScreen` bikin WebView sendiri tanpa hardening (`ui/preview/PreviewScreen.kt:78`) padahal `PreviewWebViewFactory` sudah hardened (allowFileAccess=false etc + safeBrowsing). Fase 4 akan rombak — samakan saat itu.
- Catatan mitigasi existing: no INTERNET permission, allowBackup sudah false, MODE_PRIVATE ok.

### Backlog lain (urutan prioritas)
1. **Save FAKE (penting!):** `EditorViewModel.onContentChange` hanya markDirty/markSaved lokal — TIDAK PERNAH panggil `fileSystem.writeText`. Edit hilang saat restart app. Implement autosave real via debounce + `container.fileSystem.writeText(Uri.parse(uri), content)` di IO, LED status dari FsResult.
2. **BackHandler hilang:** `navigateUp()` dead code — system Back keluar app, harusnya naik folder. Tambah `BackHandler(enabled = state.pathSegments.size > 1) { vm.navigateUp() }` di FilesScreen (import androidx.activity.compose.BackHandler — hati-hati versi OnBackPressedCallback yang dulu gagal compile; BackHandler composable lebih aman).
3. **Fallback language `text.plain` tidak terdaftar** di assets/textmate/languages.json → txt/md keep highlight file sebelumnya (catch di setEditorLanguage tidak reset). Fix: daftarkan plain grammar atau `editor.setEditorLanguage(null)` di catch.
4. **openRecent tidak cek BINARY** (mitigated: binary tak pernah masuk recents karena openFile early-return sebelum upsertRecent; stale prefs bisa lolos — tambah guard kind==BINARY → addTab binary=true tanpa readText).
5. **listError tidak pernah ditampilkan** di FilesScreen (state ada, UI tidak baca).
6. **Fase 4 preview real:** wire `PreviewComposer` (inline css/js, escape `</script>`) + `PreviewWebViewFactory` hardened + ConsoleBridge @JavascriptInterface + debounce 350ms + UrlBar/ConsolePanel 40dp↔40%.
7. **Fonts bundling:** dir `res/font` kosong — M PLUS Rounded 1c / IBM Plex Sans JP / DotGothic16 / JetBrains Mono.
8. **maintainability-reviewer** belum pernah jalan (wajib sebelum docs final per AGENTS.md).
9. **Release v0.1.2:** setelah semua fix → bump versionCode=3/versionName=0.1.2 → commit → tag v0.1.2 → push → verify Release workflow hijau + APK di Releases.
10. README final (Divio system), qa-manual §6 update (SAF flow sudah real).

---

## FILE MAP

Dipindah ke **STRUCTURE.md** (root) — peta lengkap per file beserta penjelasan, gotcha, dan lokasi masalah terbuka. Bagian ini tidak lagi dipelihara supaya tidak ada dua sumber kebenaran.

## RIWAYAT COMMIT SESI INI (baru)
`df17cb5` SAF files real → `86d682c` Sora+assets+desugaring → `93ec18f` schemes import → `56077c0` api deps (CI GREEN pertama full feature) → `0fcb385` docs → `80841fd` reviewer loop 1 (user zip fix) → `37b9678` reaudit fix (user zip fix 2) — semuanya CI GREEN.

## VERIFIKASI CEPAT
```
git push origin main && gh run list --limit 1          # CI
gh run view <id> --log-failed | grep -E "e: file"      # error compile
gh release list                                        # APK releases
```
