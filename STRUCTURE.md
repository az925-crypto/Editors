# STRUCTURE.md — Peta Lengkap File Aplikasi

**Untuk:** AI/user berikutnya yang ambil alih development.
**Cara pakai:** baca `PROGRESS.md` dulu (state, pending fix, aturan kerja), lalu pakai file ini sebagai peta untuk navigasi kode. Setiap file dijelaskan: apa isinya, kenapa ada, dan jebakan yang perlu diketahui sebelum menyentuhnya.
**Update terakhir:** 2026-08-26 — **Phase 2 TUNTAS**: modul `:core-tools` GREEN + reviewer blocking bersih + UI 6 layar ALAT hidup end-to-end (mockup approved diikuti). Rilis v0.2.0 = bump versi → tag. Detail state: PROGRESS.md.

---

## 1. Gambaran Umum

Aplikasi Android **code editor + file manager + live preview** untuk ngoding dari HP (`com.zaaam.editors`). Multi-module Gradle, DI manual, tanpa library eksternal selain Sora editor dan Compose.

| Hal | Nilai |
|---|---|
| minSdk / targetSdk / compileSdk | 26 / 36 / 36 |
| Kotlin / AGP / Gradle / JDK(CI) | 2.4.10 / 9.3.0 / 9.5.0 / 21 |
| Compose BOM / Material3 | 2026.06.00 / 1.4.0 |
| Sora editor | 0.23.6 (Maven Central — JITPACK DILARANG, lihat PROGRESS.md) |
| Desugaring | wajib, aktif di app module (`desugar_jdk_libs:2.1.5`) |

**Alur data inti (hafalkan ini):**

```
FilesScreen ──> FilesViewModel ──> SafFileSystemImpl (SAF/IO)
                      │                     │
                      │  tulis isi file     │
                      ▼                     ▼
              AppContainer.editorContents (ConcurrentHashMap, shared)
                      │
                      ▼
        EditorViewModel.contentMap ──> EditorScreen (AndroidView CodeEditor)
                │       │                       │ ketikan (debounce 200ms,
                │       │                       │  sourceUri di-capture saat event)
                │       └─ onContentChange(uri,text) <──┘
                │               │
                │               ├─ AutosaveCoordinator: debounce 900ms → Mutex per-uri →
                │               │  writeText (IO) → events Succeeded/Failed → LED
                │               │
                │               └─ isWebFile? → publishPreviewTick(seq,uri)
                │                       │
                │                       ▼
                │           PreviewViewModel (collect tick, guard relevansi shownUri,
                │            debounce 350ms, compose off-Main via PreviewComposer)
                │                       │ state.html
                │                       ▼
                │           PreviewScreen (AndroidView WebView hardened + ConsoleBridge,
                │                          urlbar uri nyata, ↻ reloadSeq, drawer console)
                │
                └── editorSession.tabs (StateFlow) ──> tab strip + LED status

container.screenState (enum FILES/EDITOR/PREVIEW) ──> AppRoot switch layar
```

Kontrak implisit paling penting: **`editorContents` adalah jembatan antara dua ViewModel berbeda instance.** Penulisnya FilesViewModel (sebelum `addTab`), pembacanya EditorViewModel. Jangan ubah urutan tulis→addTab. Jembatan kedua: **`previewTick`** (seq monotonic — StateFlow mengonflasi nilai sama, jadi tick WAJIB bawa counter).

---

## 2. Tree Repo (dengan anotasi)

```
editors/
├── .github/workflows/
│   ├── ci.yml                  # CI: assembleDebug + testDebugUnitTest + lintDebug tiap push main
│   └── release.yml             # Release APK saat tag v* / manual dispatch
├── app/                        # MODUL UTAMA: UI Compose + wiring ViewModel (lihat §3)
├── core-editor/                # MODUL: wrapper Sora code editor (lihat §4)
├── core-fs/                    # MODUL: filesystem SAF (lihat §5) — kini ada API bytes juga
├── core-preview/               # MODUL: compose HTML utk preview (lihat §6)
├── core-tools/                 # MODUL Phase 2: engine alat + UI ALAT (lihat §6b + §3.6) — TUNTAS
├── docs/
│   ├── design-spec.md          # Spec visual RetroTokens/LCD — sumber kebenaran styling
│   └── qa-manual.md            # Skrip QA manual v0.2.0 — refresh 2026-08-26 (6 layar ALAT Phase 2)
├── gradle/
│   ├── libs.versions.toml      # SATU-SATUNYA tempat versi dependency
│   └── wrapper/                # gradle-wrapper 9.5.0
├── mockup/
│   └── index.html              # Mockup HTML full-app (workflow approval UI, notes.txt #5)
├── tmp-apk/                    # Artefak lokal (unduhan CI) — TIDAK di-track git, abaikan
├── AndroidManifest...          # (di app/) lihat §3
├── build.gradle.kts            # Deklarasi plugin root, semua `apply false`
├── settings.gradle.kts         # Daftar modul + repository
├── gradle.properties           # JVM 4GB, configuration-cache, nonTransitiveRClass
├── prd.md                      # PRD produk (draft v1.0) — visi & user story
├── PROGRESS.md                 # ★ HANDOFF UTAMA: state, pending fix, backlog, aturan kerja
├── README.md                   # Final (Divio) — sinkron kode; rilis via tag v*
└── STRUCTURE.md                # File ini
```

File historis `ah.txt` sudah DIHAPUS (snapshot basi pra-Sora, isinya digantikan PROGRESS.md).

---

## 3. Modul `:app` — UI & Wiring

Package root: `com.zaaam.editors`. Semua UI Compose, Material3, tema kustom RetroTokens. TIDAK ADA Navigation-Compose — pindah layar pakai enum + StateFlow.

### 3.1 Fondasi

| File | Isi & hal penting |
|---|---|
| `app/src/main/AndroidManifest.xml` | TANPA `<uses-permission>` apa pun (no INTERNET — keputusan security). `allowBackup=false`. Satu-satunya activity: `MainActivity`, `exported=true` (launcher), `singleTask`, `adjustResize`. |
| `EditorsApp.kt` | `Application`. Bikin `AppContainer`, expose `instance` companion. Preload TextMate di background: `appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)` → `EditorEngine.initTextMate()` (idempotent @Synchronized). Jangan pindahkan preload ke main thread. |
| `MainActivity.kt` | Tipis: `setContent { RetroTheme { AppRoot(container) } }`. |
| `AppRoot.kt` | Shell navigasi: collect `container.screenState` (StateFlow\<AppScreen\>) → when FILES/EDITOR/PREVIEW render screen masing-masing + BottomNav inline sendiri (bukan komponen Material nav). |
| `di/AppContainer.kt` | DI manual, satu class semua dependensi: `prefs` (MODE_PRIVATE), `fileSystem` (SafFileSystemImpl), `treeAccess` (TreeAccess), `hiddenFiles`, `fileKindResolver`, `fileOps` (placeholder Fase lanjutan), `editorSession` (EditorSession), `screenState`, dan **`editorContents: ConcurrentHashMap<String,String>`** — jembatan Files→Editor (kontrak tulis-sebelum-addTab, §1). |
| `session/AppScreen.kt` | Enum `FILES / EDITOR / PREVIEW`. |

### 3.2 Layar Files

| File | Isi & hal penting |
|---|---|
| `ui/files/FilesViewModel.kt` | Mesin state SAF. Kunci: `pathStack` **main-thread-only**, `loadJob.cancel() + loadGeneration` anti hasil basi lintas folder. `onTreeUriSelected` → `takePersistablePermission` (IO, fallback per-flag) → persist `prefs["saf_tree_uri"]` HANYA kalau read+write ter-persist (anti loop dialog SAF) → reset pathStack → loadChildren. `openFile`: BINARY early-return tanpa readText; sukses → tulis `editorContents` SEBELUM `addTab` → pindah layar EDITOR → upsertRecent. `openRecent`: guard scheme `content://` + guard BINARY (recents basi dari prefs aman). Pesan error UI generik (`MSG_OPEN_FAILED`/`MSG_LIST_FAILED` — exception.message tidak dibocorkan). `restoreTreeUri`: IPC `isPermissionValid` via withContext(IO). Recents: format `uri|name|kind`, parser **`parseRecentEntry` internal top-level** (tested di RecentsParserTest). |
| `ui/files/FilesScreen.kt` | Launcher `OpenDocumentTree`, **BackHandler** (pathSegments>1 → navigateUp), breadcrumb LazyRow, search debounce 200ms via `queryFlow`, FilterChip HIDDEN, banner listError + banner permDenied gaya BrickWash, SkeletonLoader 8 baris, EmptyState, recents tray max 3 kartu. `filtered` di-remember dengan key eksplisit `(entries, showHidden, query)` — JANGAN diganti derivedStateOf membaca state utuh (regresi perf, komentar di file menjelaskan). |

### 3.3 Layar Editor

| File | Isi & hal penting |
|---|---|
| `ui/editor/EditorScreen.kt` | Tab strip (Row + horizontalScroll, tombol ×), LED row Idle/Saving/Saved/**Error** (Brick dot, pesan generik), AndroidView berisi `EditorEngine`. Factory pasang `subscribeEvent(ContentChangeEvent)` → debounce 200ms via `JobHolder` — race antar-tab TERTUTUP (`sourceUri` di-capture saat event; flush explicit-uri). Apply language TextMate di LaunchedEffect backoff eksponensial max 8x + ready-signal. Chip **"Preview ▶"** (hanya web) kini FUNGSIONAL: `screenState.value = PREVIEW`. |
| `ui/editor/EditorViewModel.kt` | `EditorUiState(tabs, activeUri, content, saveStatus)`; SaveStatus Idle/Saving/Saved/Error. **Autosave didelegasikan ke `AutosaveCoordinator`** (file sebelah — debounce+mutex+divergence+trik self-remove map); VM cuma: guard tab-exists/binary → contentMap → markDirty → LED Saving → coordinator.onChange → jika web `publishPreviewTick`. Collector events: Succeeded→markSaved+LED Saved+`scheduleLedIdleReset` (job terpisah 2s, guard ganda activeUri+masih-Saved); Failed→LED Error kalau tab aktif. `closeTab` = coordinator.cancelQueued + session.closeTab + contentMap.remove. `openTab()` dan `isWebFile()` DIHAPUS (isWebFile kini top-level public di core-fs). |
| `ui/editor/AutosaveCoordinator.kt` | **BARU Fase-P2.** Logika autosave tersubtle repo, kini testable `runTest` (9 test). Kontrak WAJIB: job in-flight tak dibatalkan (anti kepotong) via trik fase job menghapus DIRI dari map setelah delay; mutex per-uri urutan tulis; Succeeded hanya jika `isStillCurrent`; `cancelQueued` cuma menyentuh job mengantre. Event SharedFlow extraBufferCapacity=64. |

### 3.4 Layar Preview

| File | Isi & hal penting |
|---|---|
| `ui/preview/PreviewScreen.kt` | Gating `isWebFile(activeUri)` (import core-fs); `LaunchedEffect(activeUri)` seed instan via `vm.showActiveFile`. **URL bar fungsional**: uri nyata `state.url`, mono 11sp ellipsis RTL ala mockup, pill 40dp; tombol ↻ 36dp → `vm.refresh()` → `reloadSeq` naik → holder imperatif reset → force reload sekali. WebView via `PreviewWebViewFactory.create()` hardened + bridge TERPASANG (Fase 4). Guard reload `LastLoadedHtmlHolder` + `ReloadHolder` (plain class — bukan Compose State). Console drawer: 40dp ↔ min(40% BoxWithConstraints, 320dp), animateDpAsState tween 220ms, LazyColumn key `entry.seq` monotonik, badge LOG/WARN/ERR + border-left level. Fallback demo HTML saat kosong. |
| `ui/preview/PreviewViewModel.kt` | `PreviewUiState(html, url, isLoading, consoleEntries, consoleExpanded, reloadSeq)`. **Rantai live**: init collect `container.previewTick` → guard relevansi (`tick.uri == shownUri`) → `loadHtml(uri)` debounced 350ms dengan stale-guard (shownUri+activeTab+contentMap dicek SETELAH delay; abort reset isLoading) → `composeAndApply`: pilih scaffold standalone (.css/.js placeholder-only) atau html mentah, fragmen = konten aktif (standalone) / tab companion lain (exclude uri aktif), js SELALU non-null "" agar instrumentasi aktif, compose di Dispatchers.Default + guard relevansi ulang pasca-resume. `addConsole` cap 200 drop tertua. `refresh()` bump reloadSeq. |

### 3.5 Komponen & Tema

| File | Isi & hal penting |
|---|---|
| `ui/components/SafDialog.kt` | Dialog SAF blocking sesuai design-spec §9.5: scrim blocking, rows checklist, error banner, tombol Pilih folder / Nanti. Satu-satunya komponen shared yang real. |
| `ui/theme/Color.kt` | `object RetroTokens` — palet LCD: Shell, Card, Graphite, Dim, Ink, Olive, Brick, BrickWash, LedOrange, LedGreen, LcdBg, Border, dst. Dipakai hampir semua screen. **Phase 2 TAMBAHAN (token lama jangan diubah):** Muted #8A867C, DimBone #B9B3A0, Graphite2, LcdDim #5A6340 (offset heks), HairlineLcd, OliveHover #C7D46B (byte ter-modifikasi), OlivePress. |
| `ui/theme/Theme.kt` | `RetroTheme`: lightColorScheme mapping RetroTokens → Material3. Dipanggil MainActivity. |
| `ui/theme/Shape.kt` | `object RetroShapes` — token radius. **BELUM ter-wire** (0 referensi) — sengaja disimpan sebagai vocabulary design-system Fase 4. |
| `ui/theme/Type.kt` | `object RetroTypography` — token text style (DisplayHero … UrlBar). **BELUM ter-wire** (0 referensi) — idem, jangan hapus. |
| ~~`ui/components/{Bevel,BootOverlay,BottomNavPhysical,HardwareBar,LedIndicator}.kt`~~ | **DIHAPUS** (bersih-bersih 2026-08-25): stub satu baris `Text("…")` tanpa logika, 0 referensi. Butuh lagi? `git log --diff-filter=D -- '**/components/'` lalu checkout dari commit sebelum penghapusan. |

### 3.6 Layar ALAT (Phase 2 — semua hidup end-to-end)

Navigasi sub-alat TIDAK pakai nav-compose: `container.toolsTab: MutableStateFlow<ToolsTab>` (enum HUB/ANALYZE/DUPES/FIND_REPLACE/HEX/SNIPPETS) + BackHandler sub-tab→HUB di wrapper. Entry hex dari Files: `container.hexTargetUri` (rider openFile/openRecent BINARY — tab editor biner dummy DIHAPUS).

| File | Isi & gotcha |
|---|---|
| `session/ToolsTab.kt` | Enum sub-tab. |
| `session/TreeScanManager.kt` | Walk tree SHARED Analyzer+Dupes+FindReplace (satu cache). Job+generation pola FilesViewModel; **rescan WAJIB cancel walk DAN dupesJob + bump dua generasi** (bug r1: outcome hash lama nyasar ke tree baru). Progress masuk via `_state.update` dari callback engine (thread-safe; conflation StateFlow = throttle sisi UI per kontrak). ScanStats.rootFailed → phase FAILED (bedakan dari folder kosong). Re-check generation DI DALAM update (TOCTOU). Restore prefs `saf_tree_uri` (satu sumber folder dengan Files; persist hanya kalau restorable). |
| `session/SnippetRepository.kt` | prefs key `snippets_v1`; load() **dedup by id** (prefs luar bisa dobel → LazyColumn key crash); merge impor first-wins; helper PURE internal top-level tested: `humanBytes`, `clampHighlight` (kontrak MatchPreview multi-baris), `summarizeReplace`, `mergeSnippetsById`, `exportFileName`. Semua IO repo dipanggil off-main dari SnippetsScreen. |
| `ui/tools/ToolsScreen.kt` | Wrapper: pill ttab sub-nav + switch pane + BackHandler(enabled = tab != HUB). |
| `ui/tools/ToolsComponents.kt` | Komponen shared ala mockup: ToolsHeroCard/SectionLabel/Stencil(ledColor)/CartridgeRow/Chip/CheckSquare(tick rotasi)/Field/ProgressBar(guard total≤0)/BarRow/LedPill(blink infiniteTransition)/TagPill/BannerBrick/Note/Primary+SecondaryButton/Sheet(scrim pointerInput + kartu bawah). Tanpa shadow berat — border+background saja. |
| `ui/tools/ToolsHubScreen.kt` | 5 cartridge AN/DQ/FR/HX/SN → set toolsTab. Key LazyColumn = enum name (it.third). |
| `ui/tools/AnalyzerScreen.kt` | ensureScan saat masuk; gate body `phase==DONE` (rootFailed=FAILED jangan render body kosong); aggregateAnalysis di remember(result); chip hidden toggle → auto rescan. |
| `ui/tools/DuplicatesScreen.kt` | runDupes otomatis via LaunchedEffect(st.result) saat DONE && dupes IDLE; grup expandable (key uri node pertama), checkbox kosmetik **TANPA hapus**; BUKA › → openInEditor (**skip-overwrite editorContents kalau uri sudah terbuka/dirty**); footer stats changedDuringScan. |
| `ui/tools/FindReplaceScreen.kt` | Engine instance remember dengan lambda injected; scan→reports; selectedUris default semua match; tombol GANTI butuh query.isNotBlank + replacement non-empty + pending>0 (kontrak reviewer); sheet destruktif; replace per file = baca snapshot sekali → replaceVerified; status per file Success/BERUBAH—DILEWATI/GAGAL; highlight PreviewLine via clampHighlight. |
| `ui/tools/HexScreen.kt` | Kontrak perf: formatRow HANYA per row visible `remember(version,rowStart)`; ByteArray mutasi in-place + version bump (JANGAN copy 16MB/edit); `modified: Map<Int,Byte>` nilai original utk highlight & undo restore; undo cap 32; LED owner-guard (`ledOwner` uri — timer reset tak boleh menyentuh file lain); banner file-juga-di-editor dari editorSession.tabs; guard >16MB (oversize) & branch file 0-byte; lompat offset IME Search parse hex. Save = writeBytes seluruh isi (SAF tanpa random-access). |
| `ui/tools/SnippetsScreen.kt` | Load/mutasi SEMUA withContext(IO); form mini tambah/edit/hapus; EKSPOR OpenDocumentTree→createFile("application/json", exportFileName)→writeText, nama lapor = getDocumentId aktual; IMPOR OpenDocument(json/octet-stream/plain)→readText(guard 2MB)→importJson→sheet hasil; CODEXA disabled α.45. |

---

## 4. Modul `:core-editor` — Wrapper Sora

Namespace `com.zaaam.editors.core.editor`. Expose sora via `api()` (app butuh classpath supertype CodeEditor — JANGAN diturunkan ke implementation).

| File | Isi & hal penting |
|---|---|
| `EditorEngine.kt` | `class EditorEngine : CodeEditor`. `create(ctx)` factory. `initTextMate(app)` @Synchronized idempotent + **`textMateReady: StateFlow<Boolean>` ready-signal** (di-set true akhir initTextMate; dipakai EditorScreen untuk retry apply language segera). `createColorScheme()` = `TextMateColorScheme.create(ThemeRegistry.getInstance())`. Gotcha import Sora: `EditorColorScheme` di `widget.schemes`, `IThemeSource` di `org.eclipse.tm4e.core.registry` (detail lengkap di PROGRESS.md §5). |
| `EditorSession.kt` | Source of truth tab: `TabState(uri, displayName, dirty, lastSavedAt, binary)`, `_tabs: MutableStateFlow<List<TabState>>`, `activeTab: String?` (var biasa). `addTab` dedupe by uri + set active; `markDirty/markSaved` map-copy list. |
| `LanguageResolver.kt` | Ekstensi → scope TextMate: html/css/js/kotlin/python/json, fallback `"text.plain"` — kini grammar plain TERDAFTAR di assets (`plain.tmLanguage.json`, patterns kosong) sehingga apply language untuk ekstensi tak-kenal langsung sukses tanpa backoff. Tested: LanguageResolverTest. |
| `SoraThemeMapper.kt` | Override warna chrome scrollbar/completion/action window dari RetroTokens ke ID `EditorColorScheme` yang sudah diverifikasi. |

---

## 5. Modul `:core-fs` — SAF Filesystem

Namespace `com.zaaam.editors.core.fs`. Semua operasi IO murni SAF (DocumentsContract), tanpa path filesystem langsung.

| File | Isi & hal penting |
|---|---|
| `FileKindResolver.kt` | Multi-class dalam satu file: • `FsEntry(name, uri, isDir, size, lastModified, isHidden, kind)` • `enum Kind { WEB, CODE, CONFIG, BINARY }` • `sealed FsResult<T>` Success/Error(exception) — pola error handling seluruh repo • `interface SafFileSystem` (listChildren/readText/writeText/mkdir/rename/delete) • `TreeAccess(contentResolver)`: `takePersistablePermission` coba READ\|WRITE lalu **fallback per-flag** catch SecurityException; `isPermissionValid` **wajib read DAN write** ter-persist • `HiddenFiles.isHidden/filter` • `FileKindResolver.resolve(ext→Kind)` & `stencilLabel(ext→2 huruf)` — pure function, tested • `FileOps(UndoPayload)` placeholder Fase lanjutan. |
| `SafFileSystemImpl.kt` | Impl SafFileSystem, semua `withContext(Dispatchers.IO)` + try/catch → FsResult. `listChildren`: projection DocumentsContract, `cursor.use{}`. `readText`: precheck size >2MB tolak; provider tanpa COLUMN_SIZE (null/-1) → **streaming bounded manual** (`readBounded`, loop buffer 64KB setara readNBytes(MAX+1) — API 33-only jadi tidak dipakai langsung); hasil dicek **`isUsableAsText`** (internal top-level, tested: NUL byte / UTF-8 invalid → Error) supaya file biner tidak pernah masuk editor dan autosave tidak bisa merusaknya. `writeText`: stream null → **Error** (bukan Success palsu). `mkdir/rename/delete`: DocumentsContract standar. **Phase 2:** + `statSize/readBytes(maxBytes)/writeBytes/createFile(parent,mime,name)` (additive, dipakai core-tools/hex/snippet) + SEMUA catch kini rethrow `CancellationException` dulu (cancel bukan error operasi). |

---

## 6. Modul `:core-preview` — Composer Preview

Namespace `com.zaaam.editors.core.preview`. Modul kecil, bergantung `:core-fs`.

| File | Isi & hal penting |
|---|---|
| `PreviewComposer.kt` | `ConsoleEntry(level, message, epochMs, seq monotonik utk key LazyColumn)` + badge/formattedTime. `object PreviewComposer.compose(html, css?, js?)`: replace placeholder exact-string casing-sensitive (kontrak dikunci test) **atau fallback APPEND di akhir dokumen kalau placeholder absen**. JS payload: def `window.__zaaam_bridge` → `window.ZaaamBridge.postMessage(String(l),String(m))` dua-arg polos + interceptor console.log/warn/error + window.onerror + sinyal "preview siap"; fragmen user di-escape TERAKHIR. **Invariant single-injection**: satu-satunya titik suntik konten/instrumentasi — scaffold standalone cuma placeholder sehingga user JS tereksekusi tepat 1x. Escape close-tag regex `</\s*(style|script)\b[^>]*>` IGNORE_CASE (varian spasi/tab/end-tag ber-atribut; tidak over-match "</scripts>"). `wrapStandaloneDocument(kind)` scaffold .css/.js. Nama bridge konstanta internal tunggal (`BRIDGE_JS_OBJECT`/`BRIDGE_INTERFACE_NAME`). Tested: PreviewComposerTest. |
| `PreviewWebViewFactory.kt` | `ConsoleBridge(@JavascriptInterface postMessage(level,message))` — rate-limit sliding window 30 msg/s (`ConsoleRateLimiter`, now() injectable), truncate 500 char (`truncateConsoleMessage`), level whitelist (`resolveConsoleLevel`); bagian pure = top-level internal tanpa android.*, tested ConsoleBridgeSupportTest. Factory: hardening penuh dulu (jsEnabled, file/content access OFF, safeBrowsing, shouldOverrideUrlLoading SELALU true) → BARU `addJavascriptInterface(bridge, "ZaaamBridge")`. Origin rule: konten hanya pernah dimuat via loadDataWithBaseURL milik compose sendiri + navigasi total diblok → hanya dokumen compose yang bisa memanggil bridge. DIPAKAI PreviewScreen sejak batch 2026-08-25. |

---

## 6b. Modul `:core-tools` — Phase 2 Engine (GREEN; reviewer trio blocking-no; UI hidup)

Namespace `com.zaaam.editors.core.tools`. Depend `:core-fs`. SEMUA engine murni: uri String + lambda injected (pola AutosaveCoordinator) supaya test JVM murni jalan. API publik untuk :app: TreeScanner/DuplicateFinder/FindReplaceEngine, seluruh ToolModels.kt (ReplaceFileOutcome SUDAH DIHAPUS — mati sejak UI pakai BatchReplaceSummary), aggregateAnalysis, HexSupport penuh, SnippetJsonCodec, Snippet/SnippetParseResult. JsonMini & JSON_MAX_DEPTH tetap internal.

| File | Isi & gotcha |
|---|---|
| `ToolModels.kt` | ToolNode(name, uri, **relPath**, isDir, size, isHidden) — relPath sintetis dari root DIBANGUN scanner (nested = bawa prefix: "assets/img/a.jpg"). Juga ToolProgress/AnalyzerReport/DuplicateGroup/DupesOutcome/MatchPreview/FileFindReport/ReplaceFileOutcome. Deklarasi model CUMA di sini (sempat dobel → Redeclaration). |
| `TreeScanner.kt` | DFS ITERATIF explicit stack (anti stack-overflow); folder gagal = skippedDirs++, lanjut sibling; ensureActive() per iterasi; hidden difilter saat walk kalau includeHidden=false (tidak turun ke dalamnya); onProgress(ToolProgress(WALK,…)). |
| `StorageAnalyzer.kt` | `aggregateAnalysis()` pure internal — largestFiles top-N + largestDirs agregat TOP-LEVEL via relPath.substringBefore("/"); anak langsung root = label "(akar)". |
| `DuplicateFinder.kt` | 4 fase: group size (1..100MB) → head SHA-1 64KB → **re-statSize guard** (beda = changedDuringScan, exclude — anti klaim duplikat dari data basi, termasuk korban autosave sendiri) → full SHA-1. sha1Streaming manual buffer 64KB. File 0-byte & oversize skip (oversizedSkipped). TIDAK ada aksi hapus di v0.2 (keputusan user). |
| `FindReplaceEngine.kt` | findMatches/replaceLiteral LITERAL `indexOf(ignoreCase=true)` — BUKAN lowercase() (desink indeks 'İ' — bug CRITICAL sudah difix; regionMatches cocokkan İ↔i via simple mapping, indeks selalu koordinat asli); non-overlapping; **preview WINDOW ±PREVIEW_CONTEXT_CHARS=64 dalam baris** + kursor baris amortized O(n) (dulu substring baris penuh = ~100MB heap utk minified ×50), default maxPreviewsPerFile=12. Engine.scan → FileFindReport (outcome null = file tak terbaca/ditolak guard teks). **replaceVerified = re-read + bandingkan snapshot scan** → ChangedSkipped kalau beda (interaksi autosave!). BatchReplaceSummary Success/ChangedSkipped/Failed. |
| `HexSupport.kt` | MAX_HEX_FILE_BYTES=16MB (save=tulis balik utuh karena SAF tak punya random-access write — seluruh isi WAJIB di memori saat simpan), HEX_UNDO_MAX=32, formatRow 16 byte/baris → ByteCell(hex uppercase, ascii printable-only else null). formatRow PUBLIC + guard rowStart<0/≥size → emptyList. **Kontrak UI: panggil per row visible LazyColumn + remember keyed (version,rowStart) — DILARANG precompute seluruh file.** |
| `Snippet.kt` | Snippet(id,name,language,tags,code,updatedAtMs); `SnippetJsonCodec` PUBLIC encode/parse schema `zaaam-snippets` v1 — escaper ketat (", \\, \n\r\t, kontrol <0x20 → \uXXXX); parser JsonMini strict internal (**depth-guard JSON_MAX_DEPTH=128** anti SOE dari file untrusted; JSON null sah, field asing diabaikan, entry rusak = skippedInvalid, schema/version mismatch = BadSchema). Interface `SnippetExchange` = adapter point Codexa (**BLOCKED menunggu spec eksternal** — jangan tulis kode Codexa sebelum spec ada). |

Test (semua GREEN): TreeScannerTest (+siklus/rootFailed/progress-file), StorageAnalyzerTest, DuplicateFinderTest, FindReplaceEngineTest (+pure fns +regresi İ +window preview), HexSupportTest (+guard offset), SnippetJsonCodecTest (+nesting patologis). coroutines-test WAJIB dideklarasikan eksplisit di build.gradle modul. App module tambah: FormatHelpersTest, SnippetsSupportTest.

---

## 7. Assets, Res, Build Files

| Path | Isi |
|---|---|
| `app/src/main/assets/textmate/languages.json` | Registry 7 bahasa custom (html/css/js/kotlin/python/json/**plain**). `scopeName` HARUS persis sama dgn `scopeName` di tmLanguage masing-masing (registry throw mismatch). |
| `app/src/main/assets/textmate/*.tmLanguage.json` | 7 grammar minimal buatan sendiri (bukan dump grammar resmi); `plain.tmLanguage.json` = patterns kosong utk fallback text.plain. |
| `app/src/main/assets/textmate/themes/retro-lcd.json` | Theme format VSCode, palet LCD RetroTokens. |
| `app/src/main/res/` | Launcher adaptive (mipmap + drawable vector), `values/colors.xml`, `strings.xml`, `themes.xml` (Theme.Zaaam). `res/font` kosong — fonts bundling masih backlog. |
| `settings.gradle.kts` | `FAIL_ON_PROJECT_REPOS`; repos: **google() + mavenCentral() saja** (jitpack.io DIHAPUS — supply-chain risk). Modul: app, core-fs, core-editor, core-preview, **core-tools** (Phase 2). TIDAK ADA core-tools versi lama/core-tools aspirasi lain. |
| `build.gradle.kts` (root) | 4 plugin alias `apply false`: android-application, android-library, kotlin-android, kotlin-compose. |
| `gradle.properties` | -Xmx4096m, caching + configuration-cache on, androidX, nonTransitiveRClass. |
| `gradle/libs.versions.toml` | Satu-satunya sumber versi. AGP 9.3.0, Kotlin 2.4.10, composeBom 2026.06.00, material3 1.4.0 (eksplisit), coroutines 1.10.2 (+ `kotlinx-coroutines-test` kini DIPAKAI :app untuk AutosaveCoordinatorTest), sora 0.23.6, desugar 2.1.5, junit 4.13.2. |
| `app/src/test/`, `core-*/src/test/` | **Test suite pure JVM (enforced CI `testDebugUnitTest`):** RecentsParserTest, LanguageResolverTest, FileKindResolverTest (+isWebFile, +HiddenFiles), BinaryGuardTest, BoundedReadTest, PreviewComposerTest (escape varian + single-injection), ConsoleBridgeSupportTest, AutosaveCoordinatorTest (runTest), **+ core-tools: TreeScanner / StorageAnalyzer / DuplicateFinder / FindReplaceEngine / HexSupport / SnippetJsonCodec tests**. Aturan: JANGAN konstruksi android.* di test; pure function internal top-level agar testable; coroutines-test WAJIB dideklarasikan eksplisit di build.gradle modul. |
| `.github/workflows/ci.yml` | push/PR main → JDK21 temurin + gradle cache → assembleDebug → testDebugUnitTest → lintDebug → upload reports & debug-apk (retention 7 hari). Ini SATU-SATUNYA cara verifikasi build (dilarang gradle lokal di Termux). |
| `.github/workflows/release.yml` | Trigger tag `v*` + workflow_dispatch; `contents: write`; decode keystore opsional dari secret `RELEASE_KEYSTORE_B64` (fallback debug signing by design sampai keystore real dipasang); secrets via step-level `env:` (JANGAN `${{ secrets.* }}` inline di `run:`); nama APK dari `GITHUB_REF_NAME#v` **disanitasi whitelist `[A-Za-z0-9._-]`** (fix 2026-08-25); upload via softprops/action-gh-release v2. |

---

## 8. Dokumen Root

| File | Status & isi |
|---|---|
| `PROGRESS.md` | ★ Handoff utama: aturan wajib (CI-only build, urutan workflow agent, quirks AGP9/Sora), state done/pending, daftar bug terverifikasi + snippet fix, backlog berprioritas, riwayat commit, command verifikasi CI. SELALU update setelah sesi kerja. |
| `STRUCTURE.md` | File ini — peta struktur + penjelasan tiap file. |
| `prd.md` | PRD draft v1.0: code editor + file manager + live preview utk developer HP. |
| `README.md` | FINAL (Divio: tutorial/how-to/reference/explanation) — klaim sinkron kode (Sora 0.23.6, 5 modul termasuk :core-tools). |
| `docs/design-spec.md` | Spec visual lengkap (RetroTokens LCD, layout per layar, dialog SAF §9.5). Sumber kebenaran styling. |
| `docs/qa-manual.md` | Skrip QA manual v0.2.0 — refresh 2026-08-26 pasca-Phase 2 (ALAT: analisa/dupes/ganti/heks/snippet + regresi lama). |
| `mockup/index.html` | Mockup full-app v3 (approved) — sumber token & pola layar. |
| `mockup/phase2.html` | Mockup Phase 2 (APPROVED user 2026-08-25): 6 pane ALAT — hub, analisa, duplikat, ganti massal, heks, snippet. UI Compose wajib mengikuti ini. |
| `notes.txt` | Instruksi tetap dari user: build via CI saja, package naming, selalu rilis APK ke Releases, mockup-first untuk UI. |
| ~~`ah.txt`~~ | Dihapus 2026-08-25 — snapshot handoff pra-Sora yang sudah digantikan PROGRESS.md (file untracked, tidak ada di git). |

---

## 9. Status Cepat Masalah (state pasca-Phase 2 tuntas, 2026-08-26)

Semua fitur besar selesai & reviewer blocking bersih: Fase 1-5 infra/core/autosave/hardening, **Fase 4 preview live end-to-end**, P2 (AutosaveCoordinator dll), **PHASE 2 penuh — engine `:core-tools` + UI 6 layar ALAT** (dua putaran reviewer trio: engine r1 BLOCKING yes→fix→r2 no; UI r1 bug+perf yes→fix→r2 no; security & maintainability catatan backlog). Sisa kerja: rilis v0.2.0.

**Sisa terbuka non-blocking (detail: PROGRESS.md §Backlog Aktif):**

| Masalah | Lokasi | Prioritas |
|---|---|---|
| Hex SIMPAN tanpa divergence-guard vs autosave | HexScreen.kt | P2 perf |
| aggregateAnalysis di main compose (remember cukup utk tree normal) | AnalyzerScreen.kt | P3 perf |
| Triplikasi buka-file + displayNameOf ×3; scaffold sheet ×3 | FilesViewModel/Duplicates/Hex/Snippets | P3 maint |
| Query diedit pasca-scan → write percuma Success(0) | FindReplaceScreen.kt | P3 |
| Recompose Preview per console; editorContents tanpa eviksi; bottom-nav disabled §9.4 | Preview/AppContainer/AppRoot | P4 |
| SHA-1→256 dupes; MAX_WALK_NODES; writeBytes truncate non-atomik (limitasi SAF) | core-tools/core-fs | backlog |

Known limitation SENGAJA (jangan "dibenerin" tanpa paham trade-off): ketikan ~900ms terakhir sebelum closeTab tidak tersimpan (anti-resurrect); job autosave in-flight tidak dicancel (anti file kepotong); placeholder composer exact-string casing-sensitive; `compose(html,null,null)` identity; hex highlight tetap menyala setelah undo ke nilai asli persis.

---

## 10. Aturan Main Singkat (rem bagi AI berikutnya)

1. Build/test HANYA via GitHub Actions (push → `gh run list`). Termux tidak untuk gradle berat.
2. Urutan kerja agent wajib (explore → planner → design → build↔test → reviewers → docs → commit): lihat PROGRESS.md §BACA INI DULU.
3. `pathStack` main-thread-only; state UI update dari main; IO lewat `withContext`.
4. Kontrak `editorContents`: tulis SEBELUM `addTab`.
5. Jangan tambah plugin `kotlin-android`, jangan `kotlinOptions{}`, library module tanpa targetSdk, signingConfig sebelum buildTypes.
6. Jitpack dilarang; Sora 0.23.6 dari Central; jangan upgrade sembarangan (0.24.x prerelease).
7. Import Sora punya jebakan path — cek PROGRESS.md §5 sebelum refactor import.
