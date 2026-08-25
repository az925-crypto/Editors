# STRUCTURE.md — Peta Lengkap File Aplikasi

**Untuk:** AI/user berikutnya yang ambil alih development.
**Cara pakai:** baca `PROGRESS.md` dulu (state, pending fix, aturan kerja), lalu pakai file ini sebagai peta untuk navigasi kode. Setiap file dijelaskan: apa isinya, kenapa ada, dan jebakan yang perlu diketahui sebelum menyentuhnya.
**Update terakhir:** 2026-08-25, HEAD sesi ini setelah commit bersih-bersih dead code.

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
                     │                              │ ketikan (debounce 200ms)
                     └────────── onContentChange <──┘   (MASIH memori-only, lihat §6)

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
| `ui/files/FilesViewModel.kt` | Mesin state SAF. Kunci: `pathStack` **main-thread-only**, `loadJob.cancel() + loadGeneration` anti hasil basi lintas folder. `onTreeUriSelected` → `takePersistablePermission` (IO) → simpan `prefs["saf_tree_uri"]` → reset pathStack → loadChildren. `openFile`: BINARY early-return tanpa readText; sukses → tulis `editorContents` SEBELUM `addTab` → pindah layar EDITOR → upsertRecent. `openRecent`: BELUM punya guard BINARY/validasi scheme (backlog). `navigateUp()` ada tapi belum dipasang BackHandler (dead code sengaja disimpan). `restoreTreeUri` di init: cek permission via IPC **masih di main thread** (perf issue terbuka). Recents: format `uri|name|kind` di StringSet, parser `parseRecentEntry` aman nama-file-berpipe. |
| `ui/files/FilesScreen.kt` | Launcher `OpenDocumentTree`, breadcrumb LazyRow (items tanpa key stabil — low), search debounce 200ms via `queryFlow`, FilterChip HIDDEN, banner permDenied gaya BrickWash, SkeletonLoader 8 baris, EmptyState, recents tray max 3 kartu. `filtered` di-remember dengan key eksplisit `(entries, showHidden, query)` — JANGAN diganti derivedStateOf membaca state utuh (regresi perf, komentar di file menjelaskan). `fileDateFormat` top-level reuse. `listError` ADA di state tapi BELUM dirender UI mana pun (backlog #5). |

### 3.3 Layar Editor

| File | Isi & hal penting |
|---|---|
| `ui/editor/EditorScreen.kt` | Tab strip (Row + horizontalScroll, tombol ×), LED row saveStatus, dan AndroidView berisi `EditorEngine`. Bagian paling sensitif di repo: factory pasang `subscribeEvent(ContentChangeEvent)` → debounce 200ms via `JobHolder` (holder biasa, bukan Compose State — alasan di komentar). **RACE KRITIKAL MASIH OPEN di ~baris 158-166**: job debounce memanggil `onContentChange(text)` implisit-activeUri; kalau user ganti tab di tengah delay, teks tab lama nimpa slot tab baru. `update{}` hanya bekerja saat pindah tab: flush eksplisit `leavingUri` dulu, gate `lastAppliedContentUri`, setText kalau konten beda. `languageCache` per-scope; blok language apply set `appliedScope` SETELAH sukses (retry tiap recomposition — bisa storm saat preload belum siap, perf low terbuka). |
| `ui/editor/EditorViewModel.kt` | `EditorUiState(tabs, activeUri, content, saveStatus)`; `SaveStatus` sealed: Idle/Saving/Saved(time) — BELUM ada varian Error. Collector init ikuti `editorSession.tabs` (**belum reset saveStatus saat ganti tab aktif** — bug M). `saveJob` masih global tunggal (`cancel()` tiap keystroke — bug M). **`onContentChange(uri, text)` SAVE MASIH FAKE**: cuma tulis `contentMap` + markDirty/markSaved lokal, TIDAK PERNAH `writeText` ke disk (backlog prioritas #1 — edit hilang saat restart). `closeTab` hapus `contentMap[uri]`; flush/debounce bisa me-resurrect entry (guard belum ada). Overload `onContentChange(text)` 1-arg (implisit activeUri) adalah API lama penyebab race — calon penghapusan saat fix. |

### 3.4 Layar Preview

| File | Isi & hal penting |
|---|---|
| `ui/preview/PreviewScreen.kt` | Gating `isWebFile(activeTab)`; URL bar teks statis + tombol ↻ tanpa aksi (placeholder); WebView **dibuat manual di sini dengan hardening minimal** (`javaScriptEnabled=true`, `allowFileAccess=false` saja) PADAHAL `PreviewWebViewFactory` hardened tersedia di core-preview — security M terbuka: samakan saat wiring preview live. `update{}` reload HTML TIAP recomposition (perf M terbuka — butuh guard last-loaded). Fallback demo HTML hardcoded. Console card expand/collapse 120dp + Clear. |
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
| `EditorEngine.kt` | `class EditorEngine : CodeEditor`. `create(ctx)` factory. `initTextMate(app)` @Synchronized idempotent: `FileProviderRegistry.addFileProvider(AssetsFileResolver)` → `ThemeRegistry.loadTheme(ThemeModel(IThemeSource.fromInputStream(...retro-lcd)))` → `GrammarRegistry.loadGrammars("textmate/languages.json")`. `createColorScheme()` = `TextMateColorScheme.create(ThemeRegistry.getInstance())`. Gotcha import Sora: `EditorColorScheme` di `widget.schemes`, `IThemeSource` di `org.eclipse.tm4e.core.registry` (detail lengkap di PROGRESS.md §5). |
| `EditorSession.kt` | Source of truth tab: `TabState(uri, displayName, dirty, lastSavedAt, binary)`, `_tabs: MutableStateFlow<List<TabState>>`, `activeTab: String?` (var biasa). `addTab` dedupe by uri + set active; `markDirty/markSaved` map-copy list. |
| `LanguageResolver.kt` | Ekstensi → scope TextMate: html/css/js/kotlin/python/json, fallback `"text.plain"`. Gotcha: `text.plain` TIDAK terdaftar di languages.json → catch pemanggil harus fallback `setEditorLanguage(null)` (belum dilakukan — backlog #3). |
| `SoraThemeMapper.kt` | Override warna chrome scrollbar/completion/action window dari RetroTokens ke ID `EditorColorScheme` yang sudah diverifikasi. |

---

## 5. Modul `:core-fs` — SAF Filesystem

Namespace `com.zaaam.editors.core.fs`. Semua operasi IO murni SAF (DocumentsContract), tanpa path filesystem langsung.

| File | Isi & hal penting |
|---|---|
| `FileKindResolver.kt` | Multi-class dalam satu file: • `FsEntry(name, uri, isDir, size, lastModified, isHidden, kind)` • `enum Kind { WEB, CODE, CONFIG, BINARY }` • `sealed FsResult<T>` Success/Error(exception) — pola error handling seluruh repo • `interface SafFileSystem` (listChildren/readText/writeText/mkdir/rename/delete) • `TreeAccess(contentResolver)`: `takePersistablePermission(uri)` **selalu minta READ\|WRITE** tanpa cek flag aktual (security M terbuka), `isPermissionValid` **cuma cek isReadPermission** (security M terbuka) • `HiddenFiles.isHidden/filter` • `FileKindResolver.resolve(ext→Kind)` & `stencilLabel(ext→2 huruf)` — pure function, kandidat unit test termudah • `FileOps(UndoPayload)` placeholder Fase lanjutan. |
| `SafFileSystemImpl.kt` | Impl SafFileSystem, semua `withContext(Dispatchers.IO)` + try/catch → FsResult. `listChildren`: projection DocumentsContract, `cursor.use{}`. `readText`: guard `MAX_TEXT_FILE_BYTES` (2MB) via `querySize` — **bypass kalau provider tidak isi COLUMN_SIZE (null/-1)** → file besar tetap dimuat utuh (security/perf M terbuka; fix: streaming readNBytes). `writeText`: `openOutputStream?.use{write}` — ⚠️ stream null tetap return Success (sharp edge, perhatikan saat implement autosave real: cek null → Error). `mkdir/rename/delete`: DocumentsContract standar. |

---

## 6. Modul `:core-preview` — Composer Preview

Namespace `com.zaaam.editors.core.preview`. Modul kecil, bergantung `:core-fs`.

| File | Isi & hal penting |
|---|---|
| `PreviewComposer.kt` | `ConsoleEntry(level, message, epochMs)` + badge/formattedTime. `object PreviewComposer.compose(html, css?, js?)`: inline `<link stylesheet href="style.css">` → `<style>…` dan `<script src="main.js">` → inline + bridge `window.__zaaam_bridge`. **BELUM escape `</script>`/`</style>`** dari konten user (security M terbuka — fix: replace case-insensitive ke `<\/script`). Pure string → kandidat unit test termudah. |
| `PreviewWebViewFactory.kt` | **RESERVED — jangan hapus.** `ConsoleBridge(@JavascriptInterface postMessage)` + `PreviewWebViewFactory.create(context){}`: WebView hardened penuh (allowFileAccess/allowContentAccess/FileAccessFromFileURLs/UniversalAccessFromFileURLs=false, safeBrowsingEnabled=true, WebViewClient). Saat ini 0 pemakaian karena PreviewScreen bikin WebView sendiri — wiring-nya adalah bagian fix security + Fase 4 (console drawer). Catatan desain: jangan sembarang `addJavascriptInterface` bridge ke konten user tanpa rate-limit/origin rule. |

---

## 7. Assets, Res, Build Files

| Path | Isi |
|---|---|
| `app/src/main/assets/textmate/languages.json` | Registry 6 bahasa custom (html/css/js/kotlin/python/json). `scopeName` HARUS persis sama dgn `scopeName` di tmLanguage masing-masing (registry throw mismatch). Belum ada `text.plain`. |
| `app/src/main/assets/textmate/*.tmLanguage.json` | 6 grammar minimal buatan sendiri (bukan dump grammar resmi). |
| `app/src/main/assets/textmate/themes/retro-lcd.json` | Theme format VSCode, palet LCD RetroTokens. |
| `app/src/main/res/` | Launcher adaptive (mipmap + drawable vector), `values/colors.xml`, `strings.xml`, `themes.xml` (Theme.Zaaam). `res/font` kosong — fonts bundling masih backlog. |
| `settings.gradle.kts` | `FAIL_ON_PROJECT_REPOS`; repos: google(), mavenCentral(), **jitpack.io** (dicatat reviewer sbg supply-chain risk — deps aktual semuanya Central; kandidat dihapus). Modul: app, core-fs, core-editor, core-preview. TIDAK ADA core-tools (yang di README = aspirasi). |
| `build.gradle.kts` (root) | 4 plugin alias `apply false`: android-application, android-library, kotlin-android, kotlin-compose. |
| `gradle.properties` | -Xmx4096m, caching + configuration-cache on, androidX, nonTransitiveRClass. |
| `gradle/libs.versions.toml` | Satu-satunya sumber versi. AGP 9.3.0, Kotlin 2.4.10, composeBom 2026.06.00, material3 1.4.0 (eksplisit), coroutines 1.10.2 (+ artifacts `kotlinx-coroutines-test` TERDAFTAR tapi belum direferensi modul mana pun — siap dipakai infra test), sora 0.23.6, desugar 2.1.5, junit 4.13.2. |
| `.github/workflows/ci.yml` | push/PR main → JDK21 temurin + gradle cache → assembleDebug → testDebugUnitTest → lintDebug → upload reports & debug-apk (retention 7 hari). Ini SATU-SATUNYA cara verifikasi build (dilarang gradle lokal di Termux). |
| `.github/workflows/release.yml` | Trigger tag `v*` + workflow_dispatch; `contents: write`; decode keystore opsional dari secret `RELEASE_KEYSTORE_B64` (fallback debug signing by design sampai keystore real dipasang); secrets via step-level `env:` (JANGAN `${{ secrets.* }}` inline di `run:`); nama APK dari `GITHUB_REF_NAME#v` (sanitasi karakter belum ada — security L terbuka); upload via softprops/action-gh-release v2. |

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

## 9. Status Cepat Masalah Terbuka (ringkas)

Detail lengkap + snippet fix: **PROGRESS.md**. Lokasi kodenya:

| Masalah | Lokasi | Severity |
|---|---|---|
| Race debounce implicit-uri (korupsi antar-tab) | `EditorScreen.kt` ~158-166 | Critical |
| Save FAKE — tak pernah `writeText` ke disk | `EditorViewModel.onContentChange` | High (backlog #1) |
| saveJob global tunggal | `EditorViewModel.kt` :39/:88 | Medium |
| Collector tak reset saveStatus | `EditorViewModel.kt` :41-49 | Medium |
| Resurrect contentMap entry setelah closeTab | `EditorViewModel.closeTab/onContentChange` | Low |
| Over-grant persistable permission READ\|WRITE | `FileKindResolver.kt` :34-44 | Medium |
| isPermissionValid cuma cek read | `FileKindResolver.kt` :58-66 | Medium |
| WebView mentah vs factory hardened | `PreviewScreen.kt` :77-85 vs `PreviewWebViewFactory.kt` | Medium |
| Escape `</script>`/`</style>` | `PreviewComposer.kt` :27-28 | Medium |
| Guard 2MB bypass (size null/-1) | `SafFileSystemImpl.readText` :51-66 | Medium |
| Reload WebView tiap recomposition | `PreviewScreen.kt` update{} | Medium (perf) |
| Copy buffer 2MB di main tiap debounce | `EditorScreen.kt` subscribeEvent | Medium (perf) |
| IPC permission di main saat cold start | `FilesViewModel.restoreTreeUri` | Medium (perf) |
| Retry storm appliedScope saat preload belum siap | `EditorScreen.kt` blok language | Low (perf) |
| BackHandler hilang (navigateUp dead) | `FilesScreen.kt` + `FilesViewModel.navigateUp` | Backlog #2 |
| Fallback `text.plain` tak terdaftar | `languages.json` + `LanguageResolver` | Backlog #3 |
| openRecent tanpa guard BINARY/scheme | `FilesViewModel.openRecent` | Backlog #4 |
| listError tak pernah dirender | `FilesScreen.kt` | Backlog #5 |
| Test suite kosong total (0 src/test) | semua modul | Maintainability High |

---

## 10. Aturan Main Singkat (rem bagi AI berikutnya)

1. Build/test HANYA via GitHub Actions (push → `gh run list`). Termux tidak untuk gradle berat.
2. Urutan kerja agent wajib (explore → planner → design → build↔test → reviewers → docs → commit): lihat PROGRESS.md §BACA INI DULU.
3. `pathStack` main-thread-only; state UI update dari main; IO lewat `withContext`.
4. Kontrak `editorContents`: tulis SEBELUM `addTab`.
5. Jangan tambah plugin `kotlin-android`, jangan `kotlinOptions{}`, library module tanpa targetSdk, signingConfig sebelum buildTypes.
6. Jitpack dilarang; Sora 0.23.6 dari Central; jangan upgrade sembarangan (0.24.x prerelease).
7. Import Sora punya jebakan path — cek PROGRESS.md §5 sebelum refactor import.
