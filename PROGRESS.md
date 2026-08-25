# Progress — zaaam/editors

**Tanggal:** 2026-08-25
**Tag terakhir:** `v0.1.1` (commit `4383466`) — CI hijau (32814059540) + Release hijau (32814065578)
**Branch:** `main` @ `56077c0` — https://github.com/az925-crypto/Editors.git
**Package:** `com.zaaam.editors` — minSdk 26 / targetSdk 36 / compileSdk 36 / Kotlin 2.4.10 / AGP 9.3.0 / Gradle 9.5.0 / JDK 21

## Done
- Mockup HTML full-app v3 "Zaurus di Meja Kerja" approved (2026-08-25) — live `http://127.0.0.1:8080/mockup/` — spec `docs/design-spec.md` v3
- Git init + remote `az925-crypto/Editors` + `main` 17 commit
- Gradle wrapper bootstrap dari `gradle/gradle` v9.5.0 raw
- CI `.github/workflows/ci.yml` hijau: `assembleDebug` ✓ `testDebugUnitTest` ✓ `lintDebug` ✓ (run `32819652242` terbaru)
- Theme minimal: `RetroTokens` (bone `#E8E3D5` / graphite `#26241F` / olive `#B8C24D` / lcd `#DDE8A0` on `#1A2010`), `colors.xml`, launcher adaptive `drawable/ic_launcher_background+foreground` + `mipmap-anydpi-v26`
- App skeleton: `EditorsApp`, `MainActivity` (`RetroTheme { Text }`), `AppRoot` (`collectAsState` nav Files/Editor/Preview), `AppContainer` + `SharedPreferences` `zaaam_editors`
- `core-fs` real: `SafFileSystemImpl` (DocumentsContract `buildChildDocumentsUriUsingTree` + `readText`/`writeText`/`mkdir`/`delete`) + `TreeAccess.takePersistableUriPermission` real + `isPermissionValid` + `HiddenFiles` + `FileKindResolver`/`FileOps` — fix rename return new URI + cursor leak noted
- `core-editor` real: `EditorEngine` extends `CodeEditor` (Sora) + `SoraThemeMapper` (schemes.EditorColorScheme chrome overrides) + `LanguageResolver` — desugaring enabled (`desugar_jdk_libs:2.1.5`)
- `core-preview` stub: `PreviewComposer`/`ConsoleEntry`/`PreviewWebViewFactory` (hardened) — belum di-wire
- `FilesScreen` + `FilesViewModel` real SAF (ganti dummy `loadDummy()`): `OpenDocumentTree` launcher + `takePersistablePermission` + `buildChildDocumentsUriUsingTree` + breadcrumb dinamis 38dp + `SafDialog` blocking + recents `SharedPreferences` (StringSet `uri|name|kind`) + `SearchSlot` debounce 200ms + `PermBanner` — CI hijau `56077c0`
- `EditorScreen` Sora: `AndroidView(CodeEditor)` + `TextMateColorScheme.create(ThemeRegistry)` + `TextMateLanguage.create(scope)` + theme `retro-lcd.json` + grammars `html/css/js/kt/py/json` di `assets/textmate/` + `SoraThemeMapper` — CI hijau
- `docs/qa-manual.md` checklist 8 poin
- **Hardening round (pre-80841fd, verified saat re-audit):** `EditorsApp.onCreate` preload TextMate via `Dispatchers.IO` (fix freeze main thread 400-1200ms) + `FilesViewModel` pathStack main-thread-only & `loadChildren` Job+generasi (fix race breadcrumb) + `AppContainer.editorContents` shared map (fix handoff `openFile`→tab kosong) + `SafFileSystemImpl.readText` size guard 2MB (fix OOM) + `cursor.use{}` (fix leak) + `parseRecentEntry` first/last `|` split (fix delimiter di nama file) + `AndroidManifest allowBackup="false"`
- **Re-audit `80841fd` — Bug (1 Critical + 2 Medium), semua fixed:**
  - `EditorScreen.kt` flush debounce saat pindah tab nimpa file salah → sekarang flush nyasar `leavingUri` eksplisit (URI tab yang ditinggalkan), bukan `activeUri` implisit yang sudah keburu pindah
  - `FilesViewModel.navigateToSegment` guard `index < 0` (sebelumnya cuma cek batas atas → crash `IndexOutOfBoundsException`)
  - `EditorViewModel` saveStatus race → tiap update `saveStatus`/`content` sekarang dijaga cek `activeUri == targetUri` lewat overload `onContentChange(uri, content)` eksplisit
- **Re-audit `80841fd` — Performance (3 Low), semua fixed:**
  - `pendingChangeJob` diganti dari `mutableStateOf<Job?>` ke `JobHolder` biasa (bukan Compose State) — sebelumnya tiap assign Job baru (nyaris tiap keystroke) memicu `AndroidView.update{}` re-invoke tanpa perlu
  - `appliedScope.value` di `EditorScreen` sekarang cuma ditandai "applied" SETELAH `setEditorLanguage` sukses — sebelumnya ditandai duluan sebelum try, jadi kalau race sama preload TextMate (paling gampang kena tab pertama cold start) hasilnya macet permanen plain-text tanpa retry
  - `FilesScreen` filter list diganti dari `derivedStateOf { vm.filteredEntries(stateHolder.value) }` (masih baca whole state object, recompute tiap field apa pun berubah) ke `remember(state.entries, state.showHidden, state.query) { ... }` (recompute cuma kalau 3 field itu beneran berubah)

## CI Fix Loop (14 commit)
1. `c787705` remove `kotlin-android` (AGP 9 built-in)
2. `e278acb` `kotlinOptions` → `kotlin { jvmToolchain(17) }`
3. `ce00325` `signingConfigs` before `buildTypes`
4. `8931384` conditional `storeFile`
5. `8e499f2` remove `targetSdk` from library
6. `e0ea599`/`d9f38f9`/`5e917b9` JitPack Sora → disable (JitPack timeout `Could not find ...` + `SocketTimeoutException`)
7. `1debb03` EditText fallback
8. `28443dd`→`4321284` `strings.xml` `&` → `&amp;` + corruption fix (`ET.parse` OK)
9. `4313464` `FsResult` rename + `EditorEngine` fix
10. `6457064` signing fallback debug + `colors.xml` + launcher + remove `windowLightNavigationBar` API27
11. `4383466` fix `release.yml` secrets `env:` + `workflow_dispatch` + bump `0.1.1` → Release hijau `32814065578`
12. `df17cb5` feat `FilesScreen` real SAF (6 files, `BackHandler` bug ditemukan)
13. `86d682c` feat `Sora 0.23.6` TextMate + `retro-lcd.json` + `languages.json` + desugaring + fix `FilesScreen` back-handling
14. `93ec18f` fix `EditorColorScheme` import `widget.schemes` + verified color IDs
15. `56077c0` fix `api` expose sora deps (supertype classpath) → CI hijau `32819652242`

## Belum Done — Review BLOCKING (Fase 2/3 hardening sebelum v0.1.2)
- **Performance BLOCKING no** — semua fixed (lihat `## Done`), termasuk 3 Low dari re-audit `80841fd`.
- **Bug BLOCKING no** — semua fixed (lihat `## Done`), termasuk 1 Critical + 2 Medium dari re-audit `80841fd`.
- **Security BLOCKING no (3 Medium tersisa dari 6):**
  - `TreeAccess.takePersistablePermission` masih over-grant `FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION` sekaligus di semua kondisi
  - `TreeAccess.isPermissionValid` cek `isReadPermission` doang, tidak cek write permission-nya juga
  - `PreviewScreen.kt:78` masih instansiasi `WebView(ctx)` langsung, belum lewat `PreviewWebViewFactory` yang sudah di-hardening
  - ✅ sudah fixed: cursor leak `use{}`, delimiter `|` di recents, `allowBackup="false"`
- **Fase 4 — core-preview real:** `PreviewComposer` inline css/js beneran + `ConsoleBridge` `@JavascriptInterface` JSON + debounce 350ms + `UrlBar`/`ConsolePanel` 40dp↔40% + `WebView` `loadDataWithBaseURL` lewat `PreviewWebViewFactory` (stub belum di-wire, sekalian bereskan bypass Security di atas)
- **Lain:** font bundling `res/font` (M PLUS Rounded 1c / IBM Plex Sans JP / DotGothic16), `maintainability-reviewer` (non-blocking), `README` final, `release v0.1.2` tag + APK

## Artifact
- Debug APK: CI artifact `app-debug-apk` run `32819652242` (hijau terbaru, `gh run download`)
- Release APK: `v0.1.1` hijau di `Releases` (`zaaam-editors-0.1.1.apk`, run `32814065578`, fallback debug signing) — `v0.1.2` pending setelah fix BLOCKING

## Next Session
1. Bereskan 3 Medium Security tersisa: scope grant `TreeAccess` (read-only vs read+write sesuai kebutuhan), `isPermissionValid` cek write juga, wire `PreviewScreen` ke `PreviewWebViewFactory`
2. Re-run `security` reviewer sampai `BLOCKING: no` penuh (Performance & Bug sudah `no` sejak re-audit `80841fd`)
3. Fase 4 — wire `core-preview` real (`PreviewComposer`, `ConsoleBridge`, debounce 350ms)
4. `maintainability-reviewer` + `docs` + bump `0.1.2` + `git tag v0.1.2` → `gh release create` (workflow `workflow_dispatch` siap)
