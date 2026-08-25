# Progress & Handoff — zaaam/editors

**Tanggal:** 2026-08-25 (update: batch Fase 4 preview LIVE + P2 tuntas + rilis v0.1.2)
**HEAD:** `cad2d29` (main) — CI hijau (run `32871532097`); WAJIB cek ulang (`gh run list --limit 1`) sebelum lanjut
**Repo:** https://github.com/az925-crypto/Editors.git
**Package:** `com.zaaam.editors` — minSdk 26 / targetSdk 36 / compileSdk 36 / Kotlin 2.4.10 / AGP 9.3.0 / Gradle 9.5.0 / JDK 21
**Tag terakhir:** `v0.1.2` (`cad2d29`) — Release workflow HIJAU (run `32871666496`), APK `zaaam-editors-0.1.2.apk` ~3.17MB di Releases

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
12. `ah.txt` sudah DIHAPUS (snapshot basi pra-Sora). Sejak Fase 4, `PreviewWebViewFactory.kt` + `ConsoleBridge` BENAR-BENAR TERPAKAI (bridge terpasang + rate-limited) — jangan dihapus. Yang benar-benar sudah dibuang: 5 stub UI komponen + stub AutoSaveController (commit `7d23b56`).

---

## STATE SEKARANG

### Done (Fase 1-5 besar)
- **Fase 1 infra:** CI hijau stabil (assembleDebug+testDebugUnitTest+lintDebug), release.yml fixed+hijau v0.1.1, theme RetroTokens, launcher adaptive, app shell nav Files/Editor/Preview.
- **Fase 2 core-fs REAL:** `SafFileSystemImpl` (listChildren/readText/writeText/mkdir/rename/delete, cursor `use{}`, readText guard 2MB via querySize + fallback streaming bounded), `TreeAccess.takePersistableUriPermission/release/isPermissionValid` real (fallback per-flag, validasi read+write), rename return new URI.
- **Fase 2 UI real SAF:** `FilesViewModel` (OpenDocumentTree flow, pathStack main-only, loadJob cancel + generation guard, recents SharedPreferences parse first/last-pipe-safe (`parseRecentEntry` internal top-level + tested), restore tree uri via IO, breadcrumb segments) + `FilesScreen` (launcher, BackHandler naik-folder, banner listError + permDenied gaya BrickWash, skeleton 8 baris, empty states S0-S4b, FileRow cartridge) + `SafDialog` full spec §9.5.
- **Fase 3 core-editor REAL:** `EditorEngine extends CodeEditor`, preload TextMate di `EditorsApp.onCreate` via IO scope (idempotent @Synchronized + ready-signal `textMateReady: StateFlow<Boolean>`), factory hanya createColorScheme+applyChromeOverrides, subscribeEvent ContentChangeEvent → debounce 200ms dengan capture `sourceUri` saat event + guard `lastAppliedContentUri` (race antar-tab TERTUTUP), flush explicit-uri saat pindah tab/dispose, apply language via LaunchedEffect backoff eksponensial max 8x (retry storm mati), languageCache per-scope. Assets textmate lengkap.
- **Fase 3 wiring:** `AppContainer.editorContents: ConcurrentHashMap` shared; EditorScreen AndroidView(CodeEditor); LED row Idle/Saving/Saved/Error (Error = Brick dot "Gagal menyimpan", pesan generik).
- **AUTOSAVE REAL (2026-08-25):** debounce 900ms per-uri → `fileSystem.writeText(Uri.parse(uri))` di IO → LED dari FsResult. Guard: tab binary tidak pernah ditulis balik, uri tak ada di tabs = return (anti-resurrect). `saveJobs: Map<String,Job>` per-uri + `saveLocks: Mutex` per-uri (urutan tulis dijamin, job in-flight tidak di-cancel). markSaved/LED Saved hanya kalau snapshot tulis masih terbaru di contentMap.
- **Batch security/perf/backlog (2026-08-25):** TreeAccess flags aktual + fallback per-flag; isPermissionValid wajib read+write; saf_tree_uri hanya di-persist kalau restorable (anti loop dialog SAF); writeText null stream → Error; readText tolak non-teks (`isUsableAsText`: NUL byte / UTF-8 invalid — anti korupsi file biner oleh autosave); PreviewScreen pakai `PreviewWebViewFactory.create()` hardened + guard reload html berubah; PreviewComposer escape `</style>`/`</script>` case-insensitive; shouldOverrideUrlLoading blokir navigasi keluar; openRecent guard scheme content:// + BINARY; pesan error UI generik (MSG_*); release.yml sanitasi VERSION; jitpack.io dihapus.
- **Infra test:** 6 kelas test pure JVM enforced CI — LanguageResolverTest, FileKindResolverTest (+HiddenFiles), BinaryGuardTest, PreviewComposerTest, RecentsParserTest. junit saja (kotlin-test dead dep dihapus).
- **Reviewer loop 2026-08-25:** security BLOCKING no; performance BLOCKING no; bug-reviewer round-1 BLOCKING yes (Critical korupsi file biner via autosave) → fix `isUsableAsText` guard → round-2 BLOCKING no. maintainability-reviewer SEHAT (backlog di bawah).
- **FASE 4 PREVIEW LIVE (2026-08-25, semua CI GREEN):**
  - Rantai wired end-to-end: `EditorViewModel.onContentChange` → `container.publishPreviewTick(uri)` (**PreviewTick seq monotonic** — StateFlow mengonflasi nilai sama, tick String polos tidak akan membangunkan collector) → PreviewViewModel collect (guard relevansi `tick.uri == shownUri`) → compose **off-Main** (`withContext(Dispatchers.Default)`, guard relevansi ulang setelah resume) → `state.html` → WebView reload terguard `LastLoadedHtmlHolder`; tombol ↻ = `reloadSeq` naik → holder di-reset sekali → force reload.
  - Seed instan: `LaunchedEffect(activeUri)` → `vm.showActiveFile()` baca contentMap tanpa debounce; debounce 350ms hanya jalur push ketikan (spec §7).
  - Companion css/js dari tab TERBUKA lain (exclude uri aktif); file .css/.js dibuka langsung discaffold **placeholder-only** — INVARIANT SINGLE-INJECTION: compose() satu-satunya titik suntik konten/instrumentasi, user JS tereksekusi tepat 1x.
  - URL bar menampilkan uri nyata (ellipsis RTL ala mockup), chip "Preview ▶" kini navigasi ke PREVIEW, console drawer 40dp ↔ min(40%,320dp) tween 220ms (BoxWithConstraints, bukan LocalConfiguration).
- **Console bridge REAL hardened (Fase 4):** `ConsoleBridge.postMessage(level, message)` dua-argumen polos — TANPA JSON di sisi Java (menghapus kelas bug parsing/injection). Terpasang via `addJavascriptInterface(bridge, "ZaaamBridge")` SETELAH seluruh hardening; origin rule: semua navigasi diblok total + konten cuma pernah dimuat via loadDataWithBaseURL milik compose sendiri. Rate-limit sliding window 30 msg/s (`now()` injectable, tested), truncate 500 char, level whitelist log/warn/error→enum else LOG. Instrumentasi JS di PreviewComposer: override console.log/warn/error + window.onerror + sinyal "preview siap"; escape close-tag kini `\b[^>]*>` (varian spasi/tab/end-tag ber-atribut, tidak over-match "</scripts>"). UI cap 200 entri + LazyColumn key seq monotonik.
- **P2 tuntas (2026-08-25):** `AutosaveCoordinator` ekstraksi testable `runTest` (9 test virtual-time: debounce boundary, coalesce keystroke, cancelQueued queued-vs-in-flight, mutex ordering, divergence membungkam Succeeded, Failed emit walau divergen, independen antar-uri); LED idle-reset pindah job terpisah (head-of-line blocking fix); `isWebFile` public top-level di core-fs single source (internal TIDAK KELIHATAN lintas module — jebakan yang sudah lolos planner); `openTab()` dead code dihapus; `readBounded` internal top-level + BoundedReadTest boundary; grammar `text.plain` terdaftar (assets plain.tmLanguage.json patterns kosong).
- **Reviewer loop Fase 4 (2026-08-25):** security BLOCKING no (Low escape varian → difix); performance BLOCKING no (Medium key LazyColumn + compose off-Main → difix; sisanya backlog); bug round-1 **BLOCKING yes** (High: standalone JS double-execution karena scaffold menyuntik sendiri + companionJs tidak exclude aktif) → scaffold placeholder-only → round-2 **BLOCKING no**, dua Medium ikutannya langsung difix. maintainability non-blocking sehat.
- **Rilis v0.1.2:** bump versionCode=3/versionName=0.1.2 → tag `v0.1.2` → Release HIJAU. Gotcha BARU: R8 minify gagal `Missing class kotlin.Cloneable$DefaultImpls` (metadata kompilasi lama sora) → `-dontwarn kotlin.Cloneable$DefaultImpls` di proguard-rules.pro.

## ⚠️ BACKLOG AKTIF (urutan prioritas, hasil review 2026-08-25 pasca-Fase 4)

Semua item Fase 4 + P2 (preview live wiring, console bridge, autosave coordinator, isWebFile unifikasi, openTab dead code, readBounded boundary test, text.plain grammar) sudah TUNTAS + CI hijau + reviewer blocking bersih. Sisa:

1. **[P3 perf] Recompose seluruh PreviewScreen per pesan console** — `state.uiState` dibaca di scope paling atas; tiap `addConsole` (maks 30/s) recompose Column + AndroidView.update{} (guard lastLoadedHtml mencegah reload WebView, tapi layout tetap diukur). Fix murah kalau terasa: split collect html vs consoleEntries per blok.
2. **[P3 maintainability] Routing composeAndApply belum pure/testable** (pemilihan fragmen standalone/companion inline di VM) + duplikasi cek ekstensi web via `.endsWith` di VM padahal sudah ada `isWebFile` core-fs. Satukan saat sentuh fitur preview berikutnya.
3. **[P4] CancellationException tertelan `catch (e: Exception)` di SafFileSystemImpl** — benign sekarang (caller buang hasil), rethrow saat sentuh file itu lagi.
4. **[P4] Bottom nav disabled logic §9.4 spec belum diimplementasikan** (semua tombol selalu aktif); file .html kosong menampilkan demo fallback (kosmetik ambigu).
5. **[Backlog lama tetap]** fonts bundling (`res/font` kosong); full-copy `editor.text.toString()` ~2MB per window debounce di Main (Low amortized).
6. **[Asumsi terdokumentasi]** `LaunchedEffect(activeUri)` di PreviewScreen aman HANYA karena AppRoot membuang komposisi tiap pindah layar — revisi kalau migrasi Navigation-Compose.
7. **[Low] Known limitation — ANGKA DIREVISI:** ketikan dalam window ~900ms (debounce autosave) terakhir sebelum closeTab tidak tersimpan (catatan lama bilang ≤200ms — itu window debounce editor event, bukan autosave). Konsekuensi sengaja guard anti-resurrect + anti file kepotong; jangan "dibenerin" tanpa paham trade-off di komentarnya.

---

## FILE MAP

Dipindah ke **STRUCTURE.md** (root) — peta lengkap per file beserta penjelasan, gotcha, dan lokasi masalah terbuka. Bagian ini tidak lagi dipelihara supaya tidak ada dua sumber kebenaran.

## RIWAYAT COMMIT SESI INI (baru)
Batch Fase 4 + rilis 2026-08-25 (semuanya CI GREEN): `4c1c0f3` readBounded+text.plain assets → `1409e6c` composer instrumentasi+bridge hardened → `6830d11` autosave coordinator+isWebFile+openTab+tick → `a3b2f64` fase 4 preview live UI/wiring → `5c81b2f` review round-1 fix (High single-injection dll) → `630ee8b` compile listOfNotNull → `9511afe` review-2 medium (guard relevansi+regex atribut) → `40d88cb` Event.Succeeded api rapi → `17c17e0` bump v0.1.2 → `cad2d29` proguard dontwarn R8.
Riwayat lama: `ec37762` editor race+autosave real+ready-signal → `3e20f64` core-fs permission/stream jujur → `b3be056` files BackHandler+guards → `873d0b8` preview hardened+escape → `5d1b7c2` test infra → `c4e3b97` release.yml sanitasi → `458d081` anti-korupsi biner → `6822091` fixture ELF → `6e10067` LED jujur divergence guard → lebih lama lihat git log.

## VERIFIKASI CEPAT
```
git push origin main && gh run list --limit 1          # CI
gh run view <id> --log-failed | grep -E "e: file"      # error compile
gh release list                                        # APK releases
```
