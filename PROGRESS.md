# Progress & Handoff — zaaam/editors

**Tanggal:** 2026-08-25 (update: batch eksekusi pending fix tuntas + reviewer loop bersih)
**HEAD:** `6e10067+` (main) — WAJIB cek CI hijau dulu (`gh run list --limit 1`) sebelum lanjut fix apapun
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
- **Fase 2 core-fs REAL:** `SafFileSystemImpl` (listChildren/readText/writeText/mkdir/rename/delete, cursor `use{}`, readText guard 2MB via querySize + fallback streaming bounded), `TreeAccess.takePersistableUriPermission/release/isPermissionValid` real (fallback per-flag, validasi read+write), rename return new URI.
- **Fase 2 UI real SAF:** `FilesViewModel` (OpenDocumentTree flow, pathStack main-only, loadJob cancel + generation guard, recents SharedPreferences parse first/last-pipe-safe (`parseRecentEntry` internal top-level + tested), restore tree uri via IO, breadcrumb segments) + `FilesScreen` (launcher, BackHandler naik-folder, banner listError + permDenied gaya BrickWash, skeleton 8 baris, empty states S0-S4b, FileRow cartridge) + `SafDialog` full spec §9.5.
- **Fase 3 core-editor REAL:** `EditorEngine extends CodeEditor`, preload TextMate di `EditorsApp.onCreate` via IO scope (idempotent @Synchronized + ready-signal `textMateReady: StateFlow<Boolean>`), factory hanya createColorScheme+applyChromeOverrides, subscribeEvent ContentChangeEvent → debounce 200ms dengan capture `sourceUri` saat event + guard `lastAppliedContentUri` (race antar-tab TERTUTUP), flush explicit-uri saat pindah tab/dispose, apply language via LaunchedEffect backoff eksponensial max 8x (retry storm mati), languageCache per-scope. Assets textmate lengkap.
- **Fase 3 wiring:** `AppContainer.editorContents: ConcurrentHashMap` shared; EditorScreen AndroidView(CodeEditor); LED row Idle/Saving/Saved/Error (Error = Brick dot "Gagal menyimpan", pesan generik).
- **AUTOSAVE REAL (2026-08-25):** debounce 900ms per-uri → `fileSystem.writeText(Uri.parse(uri))` di IO → LED dari FsResult. Guard: tab binary tidak pernah ditulis balik, uri tak ada di tabs = return (anti-resurrect). `saveJobs: Map<String,Job>` per-uri + `saveLocks: Mutex` per-uri (urutan tulis dijamin, job in-flight tidak di-cancel). markSaved/LED Saved hanya kalau snapshot tulis masih terbaru di contentMap.
- **Batch security/perf/backlog (2026-08-25):** TreeAccess flags aktual + fallback per-flag; isPermissionValid wajib read+write; saf_tree_uri hanya di-persist kalau restorable (anti loop dialog SAF); writeText null stream → Error; readText tolak non-teks (`isUsableAsText`: NUL byte / UTF-8 invalid — anti korupsi file biner oleh autosave); PreviewScreen pakai `PreviewWebViewFactory.create()` hardened + guard reload html berubah; PreviewComposer escape `</style>`/`</script>` case-insensitive; shouldOverrideUrlLoading blokir navigasi keluar; openRecent guard scheme content:// + BINARY; pesan error UI generik (MSG_*); release.yml sanitasi VERSION; jitpack.io dihapus.
- **Infra test:** 6 kelas test pure JVM enforced CI — LanguageResolverTest, FileKindResolverTest (+HiddenFiles), BinaryGuardTest, PreviewComposerTest, RecentsParserTest. junit saja (kotlin-test dead dep dihapus).
- **Reviewer loop 2026-08-25:** security BLOCKING no; performance BLOCKING no; bug-reviewer round-1 BLOCKING yes (Critical korupsi file biner via autosave) → fix `isUsableAsText` guard → round-2 BLOCKING no. maintainability-reviewer SEHAT (backlog di bawah).

## ⚠️ BACKLOG AKTIF (urutan prioritas, hasil review 2026-08-25)

Semua item PENDING FIX lama (race debounce, save fake, saveJob global, collector reset, resurrect, over-grant permission, WebView mentah, escape composer, bypass 2MB, IPC main thread, retry storm, BackHandler, openRecent guard, listError, sanitasi VERSION, jitpack) sudah TUNTAS dan diverifikasi reviewer. Sisa:

1. **[P2] Fase 4 preview live:** wire `vm.loadHtml()` dari Editor (rantai masih putus; composer escape + factory hardened sudah siap). Saat wiring: placeholder match exact-string casing-sensitive di `PreviewComposer`, pasangan nama bridge `__zaaam_bridge` (JS) ↔ `ZaaamBridge` (interface), tandai param `onConsole` sebagai reserved sampai ConsoleBridge benar-benar di-addJavascriptInterface.
2. **[P2] Testability autosave coordinator:** ekstrak logika `onContentChange` (debounce+mutex+divergence) jadi class terpisah yang bisa dites `kotlinx-coroutines-test runTest` (dep sudah ada di catalog, belum dipakai). Ini bagian paling subtle repo — sekarang diverifikasi manual/reviewer doang.
3. **[P2] Unifikasi `isWebFile`** (duplikat identik di EditorViewModel + PreviewViewModel; pengetahuan sama juga ada di FileKindResolver→Kind.WEB).
4. **[P2] `openTab()` dead code** dengan default `isBinary=false` — hapus atau jadikan satu-satunya jalur open; jangan biarkan jadi pintu kedua kontrak tulis-sebelum-addTab.
5. **[P2] `readBounded` internal + boundary test** (tepat limit lolos, limit+1 throw, stream kosong) via ByteArrayInputStream.
6. **[Low] Known limitation:** ketikan dalam window ≤200ms terakhir sebelum closeTab dibuang tanpa simpan (konsekuensi sengaja guard anti-resurrect — alternatifnya resurrect entry mati).
7. **[Low] perf amortized:** `editor.text.toString()` full-copy ~2MB di Main tiap window debounce saat edit file besar (max 5x/detik, jank 1-2 frame low-end). Mitigasi ideal: diff incremental / Editable slice — v0.1 acceptable.
8. **[Backlog lama tetap]** fallback language `text.plain` tidak terdaftar (catch kini di LaunchedEffect; tetap baik daftarkan plain grammar atau setEditorLanguage(null)); fonts bundling (`res/font` kosong).
9. **Release v0.1.2:** bump versionCode=3/versionName=0.1.2 → tag v0.1.2 → verify Release workflow hijau + APK (sanitasi VERSION sudah aktif).
10. README final (Divio) + qa-manual §6 refresh; normalisasi prefiks komentar audit lama (`CRITICAL 3`, `(80841fd re-audit)` merujuk revisi PROGRESS lama).

---

## FILE MAP

Dipindah ke **STRUCTURE.md** (root) — peta lengkap per file beserta penjelasan, gotcha, dan lokasi masalah terbuka. Bagian ini tidak lagi dipelihara supaya tidak ada dua sumber kebenaran.

## RIWAYAT COMMIT SESI INI (baru)
Batch eksekusi pending fix 2026-08-26 (semuanya CI GREEN): `ec37762` editor race+autosave real+ready-signal → `3e20f64` core-fs permission/stream jujur → `b3be056` files BackHandler+guards+listError+RecentsParserTest → `873d0b8` preview hardened+escape+PreviewComposerTest → `5d1b7c2` test infra core-editor/core-fs → `c4e3b97` release.yml sanitasi+jitpack hapus → `458d081` anti-korupsi biner+mutex persist-guard (review round-1) → `6822091` test fixture ELF → `6e10067` LED jujur divergence guard (review round-2).
Riwayat lama: `df17cb5` SAF files real → `86d682c` Sora+assets+desugaring → `93ec18f` schemes import → `56077c0` api deps → `0fcb385` docs → `80841fd` reviewer loop 1 → `37b9678` reaudit fix 2 → `7d23b56` cleanup dead code.

## VERIFIKASI CEPAT
```
git push origin main && gh run list --limit 1          # CI
gh run view <id> --log-failed | grep -E "e: file"      # error compile
gh release list                                        # APK releases
```
