# Progress — zaaam/editors

**Tanggal:** 2026-08-25
**Tag terakhir:** `v0.1.0` (commit `1992f61`) — CI hijau tapi release workflow belum trigger
**Branch:** `main` — https://github.com/az925-crypto/Editors.git
**Package:** `com.zaaam.editors` — minSdk 26 / targetSdk 36 / compileSdk 36 / Kotlin 2.4.10 / AGP 9.3.0 / Gradle 9.5.0 / JDK 21

## Done
- Mockup HTML full-app v3 "Zaurus di Meja Kerja" approved (2026-08-25) — live `http://127.0.0.1:8080/mockup/` — spec `docs/design-spec.md` v3
- Git init + remote `az925-crypto/Editors` + `main` 13 commit
- Gradle wrapper bootstrap dari `gradle/gradle` v9.5.0 raw
- CI `.github/workflows/ci.yml` hijau: `assembleDebug` ✓ `testDebugUnitTest` ✓ `lintDebug` ✓ (run `32810623511`, `32810398160`)
- Theme minimal: `RetroTokens` (bone `#E8E3D5` / graphite `#26241F` / olive `#B8C24D` / lcd `#DDE8A0` on `#1A2010`), `colors.xml`, launcher adaptive `drawable/ic_launcher_background+foreground` + `mipmap-anydpi-v26`
- App skeleton: `EditorsApp`, `MainActivity` (`RetroTheme { Text }`), `AppRoot` (`collectAsState` nav Files/Editor/Preview), `AppContainer` minimal (tanpa DataStore)
- `core-fs` stub: `FsEntry`/`Kind`/`FsResult`/`SafFileSystem` interface + `SafFileSystemImpl` (DocumentsContract) + `TreeAccess`/`HiddenFiles`/`FileKindResolver`/`FileOps`
- `core-editor` stub: `EditorEngine` (EditText fallback, Sora disabled) + `EditorSession`/`TabState`/`AutoSaveController`
- `core-preview` stub: `PreviewComposer`/`ConsoleEntry`/`PreviewWebViewFactory` (tanpa serialization)
- `FilesScreen` + `FilesViewModel` dummy (filter, HIDDEN toggle, recents click → open tab) + `EditorScreen` + `EditorViewModel` (tabs, BasicTextField, line numbers, save indicator) + `PreviewScreen` + `PreviewViewModel` (WebView dummy) — semua hijau di CI
- `docs/qa-manual.md` checklist 8 poin

## CI Fix Loop (9 commit)
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

## Belum Done (Fase 2-5 Real)
- **Fase 2 — core-fs real:** SAF `ACTION_OPEN_DOCUMENT_TREE` + `takePersistableUriPermission` + `buildChildDocumentsUriUsingTree` lazy + `HiddenFiles` dotfile + breadcrumb + `FileOps` undo + `CartridgeTray` bento + `FileRowCartridge` 68dp + `SearchSlot` debounce 200ms + `PermBanner`
- **Fase 3 — core-editor real:** Sora balik (`com.github.Rosemoe:Sora-Editor` JitPack coords benar atau submodule `Editor` 0.24.6) + `language-textmate` + `TextMateRegistry` assets + `SoraThemeMapper` RetroTokens → `EditorColorScheme` + `LanguageResolver` scope
- **Fase 4 — core-preview real:** `PreviewComposer` inline css/js beneran + `ConsoleBridge` `@JavascriptInterface` JSON + `PreviewWebViewFactory` hardened (`allowFileAccess=false` etc) + debounce 350ms + `UrlBar`/`ConsolePanel` 40dp↔40% + `WebView` `loadDataWithBaseURL`
- **Fase 5 — release:** `app/build.gradle` fallback debug → ganti keystore real (via `gh secret set RELEASE_KEYSTORE_B64`), `release.yml` tag trigger tidak jalan (push `v0.1.0` tidak buat run) → tambah `workflow_dispatch` atau fix `on: push: tags`, lalu `gh release create v0.1.0 zaaam-editors-v0.1.0.apk`
- **Lain:** font bundling `res/font` (M PLUS Rounded 1c / IBM Plex Sans JP / DotGothic16), `core-tools` Phase 2 skip, reviewers BLOCKING (`security-reviewer`/`bug-reviewer`/`performance-reviewer`) + `maintainability-reviewer`, `README` final

## Artifact
- Debug APK: CI artifact `app-debug-apk` run `32810623511` (bisa `gh run download`)
- Release APK: belum di `Releases` (workflow belum trigger) — fallback debug signing siap, tinggal fix trigger lalu re-tag `v0.1.1` atau manual `gh release create`

## Next Session
1. Fix `release.yml` trigger + test `assembleRelease` lokal via CI (push tag `v0.1.1`)
2. Kembalikan Sora (cek `https://jitpack.io/#Rosemoe/Sora-Editor` versi terbaru, atau vendor via `git submodule`)
3. Implementasi `FilesScreen` real SAF (ganti dummy `loadDummy()` dengan `SafFileSystemImpl.listChildren`)
