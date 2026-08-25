# Progress & Handoff — zaaam/editors

**Tanggal:** 2026-08-25 (update: Fase 4 + rilis v0.1.2 tuntas; **Phase 2 IN-FLIGHT — engine GREEN, UI belum dibangun**)
**HEAD:** `b0c104f` (main) — CI hijau (run `32912277278`); WAJIB cek ulang (`gh run list --limit 1`) sebelum lanjut apapun
**Repo:** https://github.com/az925-crypto/Editors.git
**Package:** `com.zaaam.editors` — minSdk 26 / targetSdk 36 / compileSdk 36 / Kotlin 2.4.10 / AGP 9.3.0 / Gradle 9.5.0 / JDK 21
**Tag rilis stabil:** `v0.1.2` (`cad2d29`) — Release HIJAU (run `32871666496`, APK `zaaam-editors-0.1.2.apk`). Tag v0.2.0 BELUM ada — jangan tag sampai Phase 2 tuntas + reviewer bersih.

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

## 🚧 PHASE 2 IN-FLIGHT (baca ini sebelum sentuh apapun!)

**Status keputusan user (TERKUNCI via chat+Telegram, 2026-08-25):**
- Scope: **SEMUA Phase 2 PRD**, KECUALI root access/libsu → **SKIP, jangan kerjakan, PRD tidak diubah**.
- Hapus duplikat dari finder: **TIDAK masuk v0.2** (cari + buka di editor saja).
- Form mini tambah snippet: **MASUK** (tanpa itu fitur mati karena Codexa belum ada spec).
- Guard hex editor: **16 MB per file** (`MAX_HEX_FILE_BYTES`).
- Hidden files: **default DIKECUALIKAN** di analyzer/duplikat/ganti-massal, ada toggle.
- Mockup 6 layar `mockup/phase2.html` sudah **DI-APPROVE** user (commit `0b53669`, pernah dilayani localhost:8031). Gerbang mockup-first SUDAH lewat — UI boleh langsung dibangun mengikuti mockup itu.

**Sudah selesai (engine batch, CI GREEN di HEAD):**
- `core-fs`: API additive `statSize/readBytes(maxBytes)/writeBytes/createFile(parent,mime,name)` + **rethrow CancellationException** di SEMUA catch (rider backlog #3 lama — cancel coroutine tak lagi jadi FsResult.Error palsu).
- Modul baru **`:core-tools`** (build file kloning core-fs; dep core-fs + coroutines-test untuk test). Engine murni testable — uri String + lambda injected (pola AutosaveCoordinator):
  - `ToolModels.kt`: ToolNode(name,uri,**relPath**,isDir,size,isHidden) — relPath sintetis dari root dibangun scanner ("assets/img/a.jpg"); progress/report/dupes/replace models.
  - `TreeScanner.kt`: DFS ITERATIF (explicit stack), resilient (folder gagal = skippedDirs++, lanjut sibling), ensureActive() per iterasi, filter hidden saat walk, onProgress WALK.
  - `StorageAnalyzer.kt`: `aggregateAnalysis()` pure — largestFiles top-N + agregat folder TOP-LEVEL (relPath.substringBefore("/"), root-level = label "(akar)").
  - `DuplicateFinder.kt`: 4 fase — group size(1..100MB) → head SHA-1 64KB → **re-statSize guard** (beda = changedDuringScan, exclude konservatif) → full SHA-1. File 0-byte & oversize skip. SHA-1 streaming manual buffer 64KB.
  - `FindReplaceEngine.kt`: `findMatches/replaceLiteral` LITERAL indexOf (BUKAN Regex — metachar aman); engine scan→FileFindReport; **replaceVerified = re-read + bandingkan snapshot** (interaksi autosave! beda sedikit → ChangedSkipped).
  - `HexSupport.kt`: MAX_HEX_FILE_BYTES=16MB, HEX_UNDO_MAX=32, formatRow 16 byte/baris + toAscii printable-only.
  - `Snippet.kt`: model Snippet + `SnippetJsonCodec` encode/parse schema `zaaam-snippets` v1 (escaper ketat \uXXXX utk kontrol; parser mini strict JsonMini — JSON null sah, field asing diabaikan, entry rusak dihitung skippedInvalid) + interface `SnippetExchange` = adapter point Codexa (**BLOCKED menunggu spec eksternal**).
- Test suite core-tools GREEN (38 tests modul itu): TreeScannerTest, StorageAnalyzerTest, DuplicateFinderTest, FindReplaceEngineTest(+pure), HexSupportTest, SnippetJsonCodecTest.

**BELUM DIBANGUN — urutan wajib sesi berikutnya:**
1. ⚠️ **Reviewer trio BLOCKING atas diff engine** (`git diff 45cd930..b0c104f`) — security fokus ke SnippetJsonCodec (parser file untrusted!) + DuplicateFinder; bug cek interaksi autosave×replaceVerified; perf cek scanner Main-thread. Fix-loop sampai bersih SEBELUM mulai UI.
2. UI commit 7–12 (rencana detail dari planner, ikuti mockup approved):
   - Nav: enum AppScreen + `TOOLS`; file baru `session/ToolsTab.kt` (HUB/ANALYZE/DUPES/FIND_REPLACE/HEX/SNIPPETS); `container.toolsTab: MutableStateFlow<ToolsTab>` + `container.hexTargetUri`; BackHandler sub-tab → hub; bottom nav 4 item.
   - Shared scan: `TreeScanManager` (app layer) — satu walk di-cache untuk Analyzer+Dupes+FindReplace (state Idle/Scanning/Done/Failed + launcher SAF tree dari FilesViewModel pattern).
   - Layar: ToolsHub (5 kartu cartridge), AnalyzerScreen, DuplicateScreen (grup expandable, checkbox TANPA aksi hapus), FindReplaceScreen (form+preview highlight OliveWash+sheet konfirmasi destruktif "Tidak bisa dibatalkan"), HexScreen (LazyColumn baris hex dari formatRow + sheet edit byte + undo stack capped + LED save pola autosave + banner warning kalau uri juga tab editor), SnippetsScreen (kartu + form mini + export OpenDocumentTree/createFile("application/json") + import OpenDocument sniff schema; repo simpan prefs key `snippets_v1`).
   - Rider: FilesViewModel.openFile BINARY → alih-alih diam, set hexTargetUri + screenState TOOLS (entry hex editor).
3. Reviewer trio lagi setelah UI + maintainability non-blocking.
4. Docs sinkron (STRUCTURE §core-tools UI rows, qa-manual Phase 2, README) → rilis **v0.2.0** (versionCode=4).

**Gotcha yang sudah bayar mahal sesi ini (jangan ulang):**
- relPath hasil DFS SELALU bawa prefix parent ("assets/hero.png", bukan "hero.png").
- Byte VALUE ≠ offset: byte[41] nilainya 41 = ')' — jangan ketukar waktu nge-test ascii.
- `::java.io.ByteArrayInputStream` INVALID — harus import dulu, `::ByteArrayInputStream`.
- kotlinx-coroutines-test WAJIB didaftarkan eksplisit di build.gradle module yang mau runTest (catalog alias sudah ada).
- Deklarasi model cuma di SATU tempat (FileFindReport sempat dobel → Redeclaration error).

## ⚠️ BACKLOG AKTIF (urutan prioritas, 2026-08-25 pasca-Phase-2-engine)

Sisa kerja Phase 2 ada di §PHASE 2 IN-FLIGHT di atas (sumber kebenaran). Backlog non-Phase-2 yang tersisa:

1. **[P3 perf] Recompose seluruh PreviewScreen per pesan console** — split collect html vs consoleEntries kalau terasa jank.
2. **[P3 maintainability] Routing composeAndApply belum pure/testable** + duplikasi cek ekstensi `.endsWith` di PreviewViewModel (harusnya pakai `isWebFile` core-fs).
3. **[P4] Bottom nav disabled logic §9.4 spec belum ada**; html kosong menampilkan demo fallback (kosmetik).
4. **[Backlog tetap]** fonts bundling (`res/font` kosong); full-copy `editor.text.toString()` ~2MB per window debounce di Main (Low); asumsi LaunchedEffect(activeUri) aman krn screen-swap komposisi.
5. **[Codexa]** import/export snippet menunggu spesifikasi eksternal — adapter `SnippetExchange` sudah disiapkan di core-tools.
6. **[Known limitation — ANGKA DIREVISI]:** ketikan dalam window ~900ms (debounce autosave) sebelum closeTab tidak tersimpan (catatan lama salah tulis ≤200ms) + job autosave in-flight tidak dicancel — dua-duanya sengaja, jangan "dibenerin" tanpa paham trade-off di komentarnya.

---

## FILE MAP

Dipindah ke **STRUCTURE.md** (root) — peta lengkap per file beserta penjelasan, gotcha, dan lokasi masalah terbuka. Bagian ini tidak lagi dipelihara supaya tidak ada dua sumber kebenaran.

## RIWAYAT COMMIT SESI INI (baru)
Batch Phase 2 engine 2026-08-25/26 (CI GREEN di HEAD): `0b53669` mockup phase2 approved → `0fa46b0` core-fs api bytes+cancellation → `06aec6f` core-tools scaffold+scanner → `4fe4986` core-tools engines (analyzer/dupes/find-replace/hex/snippet)+test → `70087b0`,`9605fa9`,`1bbc0f1` fix compile/test iterasi CI → `b0c104f` fix ekspektasi test (DFS prefix + ascii ')').
Batch sebelumnya — Fase 4 + rilis v0.1.2: `4c1c0f3` readBounded+text.plain → `1409e6c` composer+bridge hardened → `6830d11` autosave coordinator+isWebFile+openTab+tick → `a3b2f64` fase 4 preview live → `5c81b2f`,`630ee8b`,`9511afe` reviewer fixes → `40d88cb`,`17c17e0` api rapi+bump v0.1.2 → `cad2d29` proguard dontwarn R8.
Riwayat lama: `ec37762` editor race+autosave real+ready-signal → `3e20f64` core-fs permission/stream jujur → `b3be056` files BackHandler+guards → `873d0b8` preview hardened+escape → `5d1b7c2` test infra → `c4e3b97` release.yml sanitasi → `458d081` anti-korupsi biner → `6822091` fixture ELF → `6e10067` LED jujur divergence guard → lebih lama lihat git log.

## VERIFIKASI CEPAT
```
git push origin main && gh run list --limit 1          # CI
gh run view <id> --log-failed | grep -E "e: file"      # error compile
gh release list                                        # APK releases
```
