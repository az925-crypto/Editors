# STRUCTURE.md — Peta Lengkap File Aplikasi

**Untuk:** AI/user berikutnya yang ambil alih development.
**Cara pakai:** baca `PROGRESS.md` dulu (state, pending fix, aturan kerja), lalu pakai file ini sebagai peta untuk navigasi kode. Setiap file dijelaskan: apa isinya, kenapa ada, dan jebakan yang perlu diketahui sebelum menyentuhnya.
**Update terakhir:** 2026-08-25, setelah batch eksekusi pending fix (autosave real, hardening security/perf, test suite) + reviewer loop bersih.

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
                      │                              │ ketikan (debounce 200ms,
                      │                              │  sourceUri di-capture saat event)
                      └──────── onContentChange(uri,text) <──┘
                      │
                      └── debounce 900ms → fileSystem.writeText (IO, Mutex per-uri)
                                              → LED Idle/Saving/Saved/Error dari FsResult

EditorSession.tabs (StateFlow) ──> tab strip + LED status
container.screenState (enum FILES/EDITOR/PREVIEW) ──> AppRoot switch layar
```

Kontrak implisit paling penting: **`editorContents` adalah jembatan antara dua ViewModel berbeda instance.** Penulisnya FilesViewModel (sebelum `addTab`), pembacanya EditorViewModel. Jangan ubah urutan tulis→addTab.

---

## 2. Tree Repo (dengan anotasi)

```
editors/
├── .github/workflows/
│   ├── ci.yml                  # CI: assembleDebug + testDebugUnitTest + lintDebug tiap push main
│   └── release.yml             # Release APK saat tag v* / manual dispatch
├── app/                        # MODUL UTAMA: UI Compose + wiring ViewModel (lihat §3)
├── core-editor/                # MODUL: wrapper Sora code editor (lihat §4)
├── core-fs/                    # MODUL: filesystem SAF (lihat §5)
├── core-preview/               # MODUL: compose HTML utk preview (lihat §6)
├── docs/
│   ├── design-spec.md          # Spec visual RetroTokens/LCD — sumber kebenaran styling
│   └── qa-manual.md            # Skrip uji manual era v0.1.0 — SEBAGIAN STALE (lihat catatan §9)
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
├── README.md                   # ⚠️ STALE: tulis Sora 0.24.6 (aktual 0.23.6), sebut modul core-tools yg tak ada
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
| `ui/editor/EditorScreen.kt` | Tab strip (Row + horizontalScroll, tombol ×), LED row Idle/Saving/Saved/**Error** (Brick dot, pesan generik), dan AndroidView berisi `EditorEngine`. Factory pasang `subscribeEvent(ContentChangeEvent)` → debounce 200ms via `JobHolder` — **race antar-tab TERTUTUP**: `sourceUri` di-capture saat event, job hanya menulis kalau `lastAppliedContentUri == sourceUri`; flush pindah-tab/dispose selalu explicit-uri. Apply language TextMate kini di **LaunchedEffect(activeUri, textMateReady)** dengan backoff eksponensial max 8x (retry storm mati; ready-signal dari EditorEngine memicu retry segera setelah preload kelar). |
| `ui/editor/EditorViewModel.kt` | `EditorUiState(tabs, activeUri, content, saveStatus)`; `SaveStatus`: Idle/Saving/Saved(time)/**Error**. Collector init reset saveStatus saat activeUri berganti. **Autosave REAL**: `onContentChange(uri, text)` (overload 1-arg implisit DIHAPUS — akar race) → guard tab-existence (anti-resurrect) + guard binary → `saveJobs: Map<String,Job>` per-uri, debounce 900ms → `writeText` di IO dalam **Mutex per-uri** (`saveLocks`, urutan tulis dijamin; job in-flight sengaja tidak di-cancel agar file tidak kepotong) → LED dari FsResult; markSaved/Saved hanya kalau snapshot tulis masih terbaru di contentMap. `closeTab` cancel job antre + hapus contentMap. `openTab()` ada tapi nol caller (backlog: hapus/pakai). |

### 3.4 Layar Preview

| File | Isi & hal penting |
|---|---|
| `ui/preview/PreviewScreen.kt` | Gating `isWebFile(activeTab)`; URL bar teks statis + tombol ↻ tanpa aksi (placeholder); WebView dibuat via **`PreviewWebViewFactory.create()` hardened** + `vm.addConsole` wired ke callback (bridge JS sendiri masih reserved Fase 4). Guard reload via `LastLoadedHtmlHolder`: `loadDataWithBaseURL` hanya kalau html benar-benar berubah. Fallback demo HTML hardcoded. Console card expand/collapse 120dp + Clear. |
| `ui/preview/PreviewViewModel.kt` | `PreviewUiState(html, url, isLoading, consoleEntries, consoleExpanded)`. `loadHtml(html, css?, js?)`: debounce 350ms → `PreviewComposer.compose` → state.html. `addConsole/clearConsole/toggleConsole`. Catatan: `loadHtml` belum punya caller dari Editor (rantai live-preview putus; Fase 4). `addConsole` juga belum ada produsernya (reserved console drawer). |

### 3.5 Komponen & Tema

| File | Isi & hal penting |
|---|---|
| `ui/components/SafDialog.kt` | Dialog SAF blocking sesuai design-spec §9.5: scrim blocking, rows checklist, error banner, tombol Pilih folder / Nanti. Satu-satunya komponen shared yang real. |
| `ui/theme/Color.kt` | `object RetroTokens` — palet LCD: Shell, Card, Graphite, Dim, Ink, Olive, Brick, BrickWash, LedOrange, LedGreen, LcdBg, Border, dst. Dipakai hampir semua screen. |
| `ui/theme/Theme.kt` | `RetroTheme`: lightColorScheme mapping RetroTokens → Material3. Dipanggil MainActivity. |
| `ui/theme/Shape.kt` | `object RetroShapes` — token radius. **BELUM ter-wire** (0 referensi) — sengaja disimpan sebagai vocabulary design-system Fase 4. |
| `ui/theme/Type.kt` | `object RetroTypography` — token text style (DisplayHero … UrlBar). **BELUM ter-wire** (0 referensi) — idem, jangan hapus. |
| ~~`ui/components/{Bevel,BootOverlay,BottomNavPhysical,HardwareBar,LedIndicator}.kt`~~ | **DIHAPUS** (bersih-bersih 2026-08-25): stub satu baris `Text("…")` tanpa logika, 0 referensi. Butuh lagi? `git log --diff-filter=D -- '**/components/'` lalu checkout dari commit sebelum penghapusan. |

---

## 4. Modul `:core-editor` — Wrapper Sora

Namespace `com.zaaam.editors.core.editor`. Expose sora via `api()` (app butuh classpath supertype CodeEditor — JANGAN diturunkan ke implementation).

| File | Isi & hal penting |
|---|---|
| `EditorEngine.kt` | `class EditorEngine : CodeEditor`. `create(ctx)` factory. `initTextMate(app)` @Synchronized idempotent + **`textMateReady: StateFlow<Boolean>` ready-signal** (di-set true akhir initTextMate; dipakai EditorScreen untuk retry apply language segera). `createColorScheme()` = `TextMateColorScheme.create(ThemeRegistry.getInstance())`. Gotcha import Sora: `EditorColorScheme` di `widget.schemes`, `IThemeSource` di `org.eclipse.tm4e.core.registry` (detail lengkap di PROGRESS.md §5). |
| `EditorSession.kt` | Source of truth tab: `TabState(uri, displayName, dirty, lastSavedAt, binary)`, `_tabs: MutableStateFlow<List<TabState>>`, `activeTab: String?` (var biasa). `addTab` dedupe by uri + set active; `markDirty/markSaved` map-copy list. |
| `LanguageResolver.kt` | Ekstensi → scope TextMate: html/css/js/kotlin/python/json, fallback `"text.plain"`. Gotcha: `text.plain` TIDAK terdaftar di languages.json — apply language kini di LaunchedEffect backoff (retry terbatas), tapi tetap baik daftarkan plain grammar atau `setEditorLanguage(null)` (backlog). Tested: LanguageResolverTest. |
| `SoraThemeMapper.kt` | Override warna chrome scrollbar/completion/action window dari RetroTokens ke ID `EditorColorScheme` yang sudah diverifikasi. |

---

## 5. Modul `:core-fs` — SAF Filesystem

Namespace `com.zaaam.editors.core.fs`. Semua operasi IO murni SAF (DocumentsContract), tanpa path filesystem langsung.

| File | Isi & hal penting |
|---|---|
| `FileKindResolver.kt` | Multi-class dalam satu file: • `FsEntry(name, uri, isDir, size, lastModified, isHidden, kind)` • `enum Kind { WEB, CODE, CONFIG, BINARY }` • `sealed FsResult<T>` Success/Error(exception) — pola error handling seluruh repo • `interface SafFileSystem` (listChildren/readText/writeText/mkdir/rename/delete) • `TreeAccess(contentResolver)`: `takePersistablePermission` coba READ\|WRITE lalu **fallback per-flag** catch SecurityException; `isPermissionValid` **wajib read DAN write** ter-persist • `HiddenFiles.isHidden/filter` • `FileKindResolver.resolve(ext→Kind)` & `stencilLabel(ext→2 huruf)` — pure function, tested • `FileOps(UndoPayload)` placeholder Fase lanjutan. |
| `SafFileSystemImpl.kt` | Impl SafFileSystem, semua `withContext(Dispatchers.IO)` + try/catch → FsResult. `listChildren`: projection DocumentsContract, `cursor.use{}`. `readText`: precheck size >2MB tolak; provider tanpa COLUMN_SIZE (null/-1) → **streaming bounded manual** (`readBounded`, loop buffer 64KB setara readNBytes(MAX+1) — API 33-only jadi tidak dipakai langsung); hasil dicek **`isUsableAsText`** (internal top-level, tested: NUL byte / UTF-8 invalid → Error) supaya file biner tidak pernah masuk editor dan autosave tidak bisa merusaknya. `writeText`: stream null → **Error** (bukan Success palsu). `mkdir/rename/delete`: DocumentsContract standar. |

---

## 6. Modul `:core-preview` — Composer Preview

Namespace `com.zaaam.editors.core.preview`. Modul kecil, bergantung `:core-fs`.

| File | Isi & hal penting |
|---|---|
| `PreviewComposer.kt` | `ConsoleEntry(level, message, epochMs)` + badge/formattedTime. `object PreviewComposer.compose(html, css?, js?)`: inline `<link stylesheet href="style.css">` → `<style>…` dan `<script src="main.js">` → inline + bridge `window.__zaaam_bridge`. Fragmen css/js di-escape dulu: **`</style>`/`</script>` case-insensitive → `<\/style>`/`<\/script>`** (tested di PreviewComposerTest). Catatan wiring Fase 4: placeholder match exact-string casing-sensitive; pasangan nama bridge JS↔interface (`__zaaam_bridge`↔`ZaaamBridge`) hanya terdokumentasi di string JS. |
| `PreviewWebViewFactory.kt` | **DIPAKAI PreviewScreen sejak batch 2026-08-25** (tetap jangan dihapus). `ConsoleBridge(@JavascriptInterface postMessage)` + `PreviewWebViewFactory.create(context){}`: WebView hardened penuh (allowFileAccess/allowContentAccess/FileAccessFromFileURLs/UniversalAccessFromFileURLs=false, safeBrowsingEnabled=true, WebViewClient + **shouldOverrideUrlLoading selalu true** — navigasi keluar diblokir). Param `onConsole` sudah ter-wire ke vm.addConsole, tapi ConsoleBridge BELUM di-addJavascriptInterface (reserved Fase 4 — jangan sembarang tambah tanpa rate-limit/origin rule). |

---

## 7. Assets, Res, Build Files

| Path | Isi |
|---|---|
| `app/src/main/assets/textmate/languages.json` | Registry 6 bahasa custom (html/css/js/kotlin/python/json). `scopeName` HARUS persis sama dgn `scopeName` di tmLanguage masing-masing (registry throw mismatch). Belum ada `text.plain`. |
| `app/src/main/assets/textmate/*.tmLanguage.json` | 6 grammar minimal buatan sendiri (bukan dump grammar resmi). |
| `app/src/main/assets/textmate/themes/retro-lcd.json` | Theme format VSCode, palet LCD RetroTokens. |
| `app/src/main/res/` | Launcher adaptive (mipmap + drawable vector), `values/colors.xml`, `strings.xml`, `themes.xml` (Theme.Zaaam). `res/font` kosong — fonts bundling masih backlog. |
| `settings.gradle.kts` | `FAIL_ON_PROJECT_REPOS`; repos: **google() + mavenCentral() saja** (jitpack.io DIHAPUS 2026-08-25 — supply-chain risk, deps semua resolve dari dua repo itu). Modul: app, core-fs, core-editor, core-preview. TIDAK ADA core-tools (yang di README = aspirasi). |
| `build.gradle.kts` (root) | 4 plugin alias `apply false`: android-application, android-library, kotlin-android, kotlin-compose. |
| `gradle.properties` | -Xmx4096m, caching + configuration-cache on, androidX, nonTransitiveRClass. |
| `gradle/libs.versions.toml` | Satu-satunya sumber versi. AGP 9.3.0, Kotlin 2.4.10, composeBom 2026.06.00, material3 1.4.0 (eksplisit), coroutines 1.10.2 (+ artifacts `kotlinx-coroutines-test` TERDAFTAR tapi belum direferensi modul mana pun — siap dipakai test autosave coordinator), sora 0.23.6, desugar 2.1.5, junit 4.13.2 (satu-satunya dep test — kotlin-test dead dep sudah dihapus dari 4 modul). |
| `app/src/test/`, `core-*/src/test/` | **Test suite pure JVM (enforced CI `testDebugUnitTest`):** RecentsParserTest (parser recents pipe-safe), LanguageResolverTest, FileKindResolverTest (+HiddenFiles), BinaryGuardTest (guard anti-korupsi biner), PreviewComposerTest (escape close-tag). Aturan: JANGAN konstruksi android.* di test (not mocked); pure function dibuat `internal` top-level agar testable. |
| `.github/workflows/ci.yml` | push/PR main → JDK21 temurin + gradle cache → assembleDebug → testDebugUnitTest → lintDebug → upload reports & debug-apk (retention 7 hari). Ini SATU-SATUNYA cara verifikasi build (dilarang gradle lokal di Termux). |
| `.github/workflows/release.yml` | Trigger tag `v*` + workflow_dispatch; `contents: write`; decode keystore opsional dari secret `RELEASE_KEYSTORE_B64` (fallback debug signing by design sampai keystore real dipasang); secrets via step-level `env:` (JANGAN `${{ secrets.* }}` inline di `run:`); nama APK dari `GITHUB_REF_NAME#v` **disanitasi whitelist `[A-Za-z0-9._-]`** (fix 2026-08-25); upload via softprops/action-gh-release v2. |

---

## 8. Dokumen Root

| File | Status & isi |
|---|---|
| `PROGRESS.md` | ★ Handoff utama: aturan wajib (CI-only build, urutan workflow agent, quirks AGP9/Sora), state done/pending, daftar bug terverifikasi + snippet fix, backlog berprioritas, riwayat commit, command verifikasi CI. SELALU update setelah sesi kerja. |
| `STRUCTURE.md` | File ini — peta struktur + penjelasan tiap file. |
| `prd.md` | PRD draft v1.0: code editor + file manager + live preview utk developer HP. |
| `README.md` | ⚠️ STALE: sebut "Sora 0.24.6" (aktual 0.23.6) dan modul `core-tools` yang tidak ada. Perlu dirapikan saat fase docs. |
| `docs/design-spec.md` | Spec visual lengkap (RetroTokens LCD, layout per layar, dialog SAF §9.5). Sumber kebenaran styling. |
| `docs/qa-manual.md` | Skrip QA manual era v0.1.0 — SEBAGIAN STALE (deskripsi breadcrumb/back-nav/live-preview belum sesuai kode sekarang); §6 dialog SAF masih valid. Perlu refresh saat fase docs. |
| `mockup/index.html` | Mockup full-app HTML. Workflow notes.txt #5: mockup + approval Telegram SEBELUM kerja UI baru. |
| `notes.txt` | Instruksi tetap dari user: build via CI saja, package naming, selalu rilis APK ke Releases, mockup-first untuk UI. |
| ~~`ah.txt`~~ | Dihapus 2026-08-25 — snapshot handoff pra-Sora yang sudah digantikan PROGRESS.md (file untracked, tidak ada di git). |

---

## 9. Status Cepat Masalah (state pasca-batch 2026-08-25)

Semua item di daftar lama (race debounce, save fake, saveJob global, collector reset, resurrect, over-grant permission, isPermissionValid read-only, WebView mentah, escape composer, bypass 2MB, reload WebView, IPC main thread, retry storm, BackHandler, openRecent guard, listError, sanitasi VERSION, jitpack, test suite kosong) sudah **FIXED + diverifikasi reviewer** (security no / bug round-2 no / perf no / maintainability sehat). Detail: PROGRESS.md §Backlog Aktif.

Sisa terbuka:

| Masalah | Lokasi | Prioritas |
|---|---|---|
| Preview live belum wired (loadHtml tak ada caller; bridge reserved) | `PreviewViewModel`/`EditorScreen` | P2 (Fase 4) |
| Autosave coordinator tak bisa di-unit-test (tangled ke VM) | `EditorViewModel.onContentChange` | P2 |
| `isWebFile` duplikat 2 ViewModel | EditorViewModel/PreviewViewModel | P2 |
| `openTab()` dead code trap (`isBinary=false` default) | `EditorViewModel.kt` | P2 |
| `readBounded` private, boundary test belum ada | `SafFileSystemImpl.kt` | P2 |
| Ketikan ≤200ms sebelum closeTab dibuang (by design anti-resurrect) | EditorScreen×EditorViewModel | Low |
| Full-copy buffer ~2MB per window debounce di Main (amortized) | `EditorScreen.kt` subscribeEvent | Low |
| Fallback language `text.plain` tidak terdaftar | languages.json | Backlog |
| Fonts bundling (`res/font` kosong) | res/font | Backlog |
| Release v0.1.2 + README final + qa-manual refresh | root | Rilis |

---

## 10. Aturan Main Singkat (rem bagi AI berikutnya)

1. Build/test HANYA via GitHub Actions (push → `gh run list`). Termux tidak untuk gradle berat.
2. Urutan kerja agent wajib (explore → planner → design → build↔test → reviewers → docs → commit): lihat PROGRESS.md §BACA INI DULU.
3. `pathStack` main-thread-only; state UI update dari main; IO lewat `withContext`.
4. Kontrak `editorContents`: tulis SEBELUM `addTab`.
5. Jangan tambah plugin `kotlin-android`, jangan `kotlinOptions{}`, library module tanpa targetSdk, signingConfig sebelum buildTypes.
6. Jitpack dilarang; Sora 0.23.6 dari Central; jangan upgrade sembarangan (0.24.x prerelease).
7. Import Sora punya jebakan path — cek PROGRESS.md §5 sebelum refactor import.
