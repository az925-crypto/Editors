# Progress & Handoff — zaaam/editors

**Tanggal:** 2026-08-26 (update: **fitur LIVE PREVIEW SPLIT tuntas + reviewer blocking bersih**; sebelumnya v0.2.1 fix CRITICAL FC cold start)
**HEAD:** lihat `git log -1` — WAJIB cek CI (`gh run list --limit 1`) sebelum lanjut apapun
**Repo:** https://github.com/az925-crypto/Editors.git
**Package:** `com.zaaam.editors` — minSdk 26 / targetSdk 36 / compileSdk 36 / Kotlin 2.4.10 / AGP 9.3.0 / Gradle 9.5.0 / JDK 21
**Tag rilis stabil:** `v0.1.2` (`cad2d29`). Tag `v0.2.0` = bump versionCode=4/versionName=0.2.0 → tag setelah docs commit ini.

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
13. **CRITICAL (v0.2.1 — FC cold start di device):** sora 0.23.6 `LanguageDefinitionReader` membaca kunci **`"grammar"`** di tiap entri `languages.json` (BUKAN `"path"`) + path asset TANPA prefix `./`. Salah format = NPE di preload startup → seluruh proses mati sebelum UI. Tidak pernah ketahuan karena CI tidak menjalankan app dan APK release baru pertama kali dipasang device saat v0.2.0. Pengaman: `TextMateAssetsContractTest` (CI) + initTextMate try-catch (degradasi tanpa highlighting, tidak bunuh proses). **PELAJARI: unit test JVM hijau ≠ app jalan di device; smoke-test APK RELEASE di device adalah bagian dari definisi selesai rilis.**
14. **INVARIANT live preview split (jangan dirusak):**
    - `PreviewViewModel` DI-SHARE satu instance antara layar Preview & pane Editor via `viewModel { PreviewViewModel(container) }` — sah karena app TANPA NavHost (ViewModelStoreOwner = Activity). Kalau nanti migrasi ke Navigation-Compose, factory/scope ini berubah makna → double-instance → double-compose.
    - Collector tick di VM DIGERBANGI konsumen (`screenState==PREVIEW || (EDITOR && splitPreviewEnabled)`). **Menambah konsumen preview ketiga = WAJIB update gate itu + ritual seed `LaunchedEffect(activeUri, flag) { vm.showActiveFile(...) }` di konsumen baru**, kalau tidak preview diam-diam basi tanpa error.
    - `showActiveFile()` me-RESET state bila dokumen berganti (anti render basi di bawah header baru); same-doc re-seed sengaja tidak reset (anti flicker).
    - Node AndroidView sora HARUS tetap SATU call-site dalam BoxWithConstraints (posisi via modifier alignment/fraction, BUKAN pindah parent Column↔Row) — kalau reparenting, editor dibuat ulang saat rotasi (cursor/undo hilang).

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

## ✅ PHASE 2 — TUNTAS (2026-08-26; detail teknis di STRUCTURE.md §3.6 + §6b)

**Keputusan user terkunci (terlaksana sesuai rencana):** semua Phase 2 PRD kecuali root access/libsu (SKIP); duplikat TANPA hapus (cari+buka saja); form mini snippet MASUK; hex guard 16MB (`MAX_HEX_FILE_BYTES`); hidden default DIKECUALIKAN + toggle; mockup `mockup/phase2.html` approved → UI mengikuti mockup.

**Engine (batch awal, CI GREEN):** core-fs additive `statSize/readBytes/writeBytes/createFile/readStream` + rethrow CancellationException semua catch; modul `:core-tools` murni-JVM (uri String + lambda injected): TreeScanner DFS iteratif anti-siklus (visited-set), StorageAnalyzer, DuplicateFinder SHA-1 4 fase, FindReplaceEngine literal + replaceVerified divergence-guard, HexSupport guard 16MB/undo-32/formatRow, SnippetJsonCodec parser ketat depth-guard 128.

**Reviewer engine round-1 (BLOCKING yes→fix→round-2 BLOCKING no):**
- CRITICAL: ignoreCase pakai `lowercase()` desink indeks ('İ' memanjang) → crash/korupsi saat replace → diganti `indexOf(ignoreCase=true)` (regionMatches, indeks koordinat asli; 'İ'↔'i' tetap cocok via simple lowercase — terverifikasi CI).
- HIGH: JsonMini rekursi tanpa batas → SOE dari file untrusted → depth-guard 128.
- MED: CancellationException tertelan hashOf; progress WALK tak hitung file; fase full-hash tanpa emit; root gagal ≡ folder kosong.
- Fix murah: visited-set scanner, formatRow guard negatif, hoist FileKindResolver, gen re-check dalam `_state.update`.

**UI (commit c7–c13):**
- Nav: `AppScreen+TOOLS`; `session/ToolsTab.kt` (HUB/ANALYZE/DUPES/FIND_REPLACE/HEX/SNIPPETS); `container.toolsTab` + `container.hexTargetUri`; BackHandler sub-tab→HUB; bottom nav 4 item.
- `session/TreeScanManager.kt`: satu walk di-cache utk Analyzer+Dupes+FindReplace (job+generation pola FilesViewModel; rescan = cancel walk **dan** dupesJob + bump dua generasi); progress via StateFlow.update (conflation = throttle sisi UI per kontrak engine).
- `session/SnippetRepository.kt`: prefs key `snippets_v1`, load dedup by id (prefs luar bisa id dobel), merge impor first-wins, helper pure tested: humanBytes/clampHighlight/summarizeReplace/mergeSnippetsById/exportFileName.
- Layar ala mockup: ToolsHub (5 cartridge), AnalyzerScreen (readout+bar folder terbesar+file terbesar), DuplicatesScreen (grup expandable + checkbox TANPA hapus + BUKA ke editor), FindReplaceScreen (literal scan, highlight OliveWash windowed, sheet destruktif "Tidak bisa dibatalkan", status BERUBAH—DILEWATI), HexScreen (LazyColumn formatRow visible-only keyed version, sheet edit byte, undo cap 32, LED owner-guard, banner editor, guard >16MB, lompat offset), SnippetsScreen (kartu + form mini + ekspor/impor JSON + notice Codexa disabled).
- Rider: FilesViewModel.openFile/openRecent BINARY → `hexTargetUri` + TOOLS/HEX (tab biner dummy dihapus).
- Komponen shared `ui/tools/ToolsComponents.kt` (hero/section/cartridge/stencil/chip/checksquare/field/progress/barrow/ledpill/tagpill/banner/note/buttons/sheet) + token baru Color.kt (Muted/DimBone/Graphite2/LcdDim/HairlineLcd/OliveHover/OlivePress — token lama tidak disentuh).

**Reviewer UI round-1 (BLOCKING yes→fix→round-2 BLOCKING no):**
- CRITICAL (bug): startScan tak cancel dupesJob/gen → outcome hash lama menempel tree baru → kini dicancel + gen dinaikkan.
- CRITICAL (perf): previewAt substring baris penuh — minified 2MB ×50 preview ≈100MB heap → kini kursor baris amortized O(n) + window konteks ±64 char (PREVIEW_CONTEXT_CHARS), default maxPreviews 12, test regresi window.
- MED/LOW difix: tombol GANTI butuh query non-blank; snippets load/mutasi pindah IO; openInEditor skip-overwrite tab dirty; LED owner guard anti timer lintas-file; file 0-byte bukan error; AnalysisBody digate DONE; nama ekspor dari dokumen aktual.
- Security UI: BLOCKING no sejak r1 (error generik, tanpa Regex user-input, prefs aman, permission path konsisten).
- maintainability non-blocking: catatan backlog di bawah.

## ✅ LIVE PREVIEW SPLIT (2026-08-26, semua CI GREEN + reviewer blocking bersih)

Fitur: editor dan render web TERLIHAT BERSAMAAN di layar Editor — chip **"Split ◫"** (hanya file web) toggle pane; hasil update otomatis saat mengetik (debounce 200ms editor + 350ms preview). Keputusan desain (planner+design):
- **Reuse `PreviewViewModel` yang sama** (activity-scoped, tanpa NavHost) — TIDAK membuat composer/collector kedua; pipeline tick→debounce→compose tetap satu, double-compose mustahil by construction. Ekstraksi "PreviewDocumentAssembler" DIBATALKAN (YAGNI).
- `PreviewWebViewPanel` diekstraksi dari PreviewScreen (dipakai dua call site) + `onRelease { stopLoading(); destroy() }` — sekalian fix leak WebView pre-existing.
- Flag `container.splitPreviewEnabled` persist prefs key `split_preview_enabled` (setter satu pintu), default OFF.
- Layout: SATU `BoxWithConstraints` — portrait editor atas 50% / pane bawah; landscape side-by-side; divider hairline 1dp Border digambar SETELAH pane (z-order). Pane blank = box teks Indonesia (BUKAN demo HTML); tanpa console drawer (fitur lengkap tetap di tab Preview).
- Fix pasca-review: reset render basi saat ganti dokumen (`showActiveFile`), gate konsumen untuk collector tick (hemat CPU saat split OFF & bukan layar Preview), seed keyed `(activeUri, splitEnabled)`.

## 🚧 STATE SEKARANG (pasca-Live-Preview-Split)

Semua fitur besar Fase 1-5 + Phase 2 + Live Preview Split SELESAI & reviewer blocking bersih. Rilis terakhir v0.2.1. Langkah berikutnya bila mau rilis fitur ini: bump versionCode=6/versionName=0.3.0 → tag → verifikasi APK release di device (smoke-test wajib per gotcha #13).

## ⚠️ BACKLOG AKTIF (urutan prioritas, 2026-08-26 pasca-Phase-2-tuntas)

Sisa dari review UI/engine (semua non-blocking, tidak menghalangi rilis):

1. **[P2 perf] HexScreen SIMPAN tanpa divergence-guard** — tulis balik seluruh buffer bisa menimpa perubahan autosave editor yang terjadi setelah load (banner warning sudah ada). Fix ideal: re-stat size/compare sebelum writeBytes (pola replaceVerified). Lokasi: ui/tools/HexScreen.kt.
2. **[P3 perf] aggregateAnalysis di main saat compose** — remember(result) sudah cukup utk tree ≤50k file (~10–30ms sekali per rescan); kalau mau hardening: hitung report di manager off-main. Lokasi: AnalyzerScreen.kt.
3. **[P3 maintainability] Triplikasi kontrak buka-file** (FilesViewModel.openFile / openRecent / openInEditor DuplicatesScreen) + displayNameOf ×3 → gabungkan jadi satu helper shared. Lokasi: FilesViewModel.kt, DuplicatesScreen.kt, TreeScanManager.kt.
4. **[P3 maintainability] Scaffold sheet duplikat 3×** (ToolsSheet / HexEditSheet / SnippetFormSheet) → ekstrak base sheet composable.
5. **[P3] FindReplace: query diedit pasca-scan** → hasil tampil basi vs replaceVerified (lindungi data, tapi write percuma Success(0)); reset reports saat query berubah.
6. **[P4 perf] Recompose PreviewScreen per pesan console** (split collect); **editorContents tanpa eviksi** (clear on closeTab?); **matched/pending FindReplace per recompose** (remember keys); **dupes expanded Set** (derivedStateOf) — semua kosmetik/terukur kecil.
7. **[P4] Bottom nav disabled logic §9.4 spec belum ada; html kosong demo fallback (kosmetik).**
8. **[Backlog tetap]** fonts bundling (`res/font` kosong); SHA-1→SHA-256 dupes (LOW: v0.2 tanpa aksi hapus); preview multi-baris label "…"; MAX_WALK_NODES cap walk; writeBytes/writeText truncate non-atomik (dokumentasi limitasi SAF).
9. **[Codexa]** import/export snippet menunggu spesifikasi eksternal — adapter `SnippetExchange` siap di core-tools.
10. **[Known limitation — SENGAJA]:** ketikan ~900ms sebelum closeTab hilang (anti-resurrect); job autosave in-flight tak dicancel; compose(html,null,null) identity; placeholder composer exact-string casing-sensitive; hex highlight "pernah diedit" tetap menyala setelah undo (nilai asli dipulihkan tapi map original tak dihapus kecuali match persis).
11. **[Split preview] delta-update WebView** pengganti full-reload per burst (~550ms idle) — hilangkan scroll-reset tiap update; plus preserve scroll via evaluateJavascript. Lokasi: PreviewWebViewPanel.kt.
12. **[Split preview] IME memakan setengah area** (adjustResize) — auto-collapse pane saat keyboard visible (`WindowInsets.isImeVisible`). JANGAN ganti softInputMode global.
13. **[Split preview] Draggable divider** pengganti rasio fixed 50/50; rotasi mid-split me-remount pane (WebView baru) — bisa dioptimalkan kalau keluhannya muncul.
14. **[Maintainability split] Kopling gate lintas-file** (`screenState`+flag dibaca VM): konsumen ketiga wajib update gate + seed ritual (gotcha #14); literal 0.5f ×6 titik → ekstrak konstanta; duplikasi styling canvas putih pane vs layar Preview → pertimbangkan token shared; displayName lookup ×2 → hoist satu kali.

---

## FILE MAP

Dipindah ke **STRUCTURE.md** (root) — peta lengkap per file beserta penjelasan, gotcha, dan lokasi masalah terbuka. Bagian ini tidak lagi dipelihara supaya tidak ada dua sumber kebenaran.

## RIWAYAT COMMIT SESI INI (baru)
Batch live preview split 2026-08-26 (CI GREEN di HEAD): `4d623ae` ekstraksi PreviewWebViewPanel+onRelease destroy → `2c07ed6` feat split view (chip persist, pane reuse VM shared, BoxWithConstraints) → `81cd73f` fix review (reset render basi ganti dokumen, gate konsumen tick, divider z-order).
Batch v0.2.1: `82d422f` fix CRITICAL FC cold start — languages.json kunci "grammar" + initTextMate try-catch + TextMateAssetsContractTest + docs gotcha/qa.
Batch Phase 2 tuntas 2026-08-26 (CI GREEN di HEAD): engine `0b53669` mockup approved → `0fa46b0` core-fs api bytes+cancellation → `06aec6f` core-tools scaffold+scanner → `4fe4986` engines+test → `70087b0`,`9605fa9`,`1bbc0f1`,`017b3f7`,`b0c104f` fix iterasi CI. Reviewer engine: `70b4372` trio r1 fixes (desink ignoreCase, depth JSON, cancel dupes/scanner) + `f07088e`,`ba9432d` fix nama test/ekspektasi İ (regionMatches cocokkan İ↔i). UI: `8c09605` c7 nav+ToolsTab → `5aad570` c8 widening visibilitas → `aa0d049` c9 TreeScanManager+SnippetRepo+readStream+test → `f933b47` c10 komponen+hub+analisa → `3dcf2f6` c11 dupes+findreplace → `f069dac` c12 hex+rider → `73f794f` c13 snippets → `0a09dfe` fix import compile → `c801d5e` reviewer UI r1 fixes (stale dupes outcome, preview window anti-OOM, guard query kosong, dedup snippet id, IO off-main, skip-overwrite dirty, LED owner, 0-byte, gate analyzer).
Batch sebelumnya — Fase 4 + rilis v0.1.2: `4c1c0f3` readBounded+text.plain → `1409e6c` composer+bridge hardened → `6830d11` autosave coordinator+isWebFile+openTab+tick → `a3b2f64` fase 4 preview live → `5c81b2f`,`630ee8b`,`9511afe` reviewer fixes → `40d88cb`,`17c17e0` api rapi+bump v0.1.2 → `cad2d29` proguard dontwarn R8.
Riwayat lama: `ec37762` editor race+autosave real+ready-signal → `3e20f64` core-fs permission/stream jujur → `b3be056` files BackHandler+guards → `873d0b8` preview hardened+escape → `5d1b7c2` test infra → `c4e3b97` release.yml sanitasi → `458d081` anti-korupsi biner → `6822091` fixture ELF → `6e10067` LED jujur divergence guard → lebih lama lihat git log.

## VERIFIKASI CEPAT
```
git push origin main && gh run list --limit 1          # CI
gh run view <id> --log-failed | grep -E "e: file"      # error compile
gh release list                                        # APK releases
```
