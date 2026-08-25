# zaaam/editors

Mobile code editor + file manager + live preview for Android. Built for developers coding from phone.

## Status
Pre-alpha — MVP in development. Design spec v3 approved: "Zaurus di Meja Kerja" (retro-tech Japanese gadget aesthetic).

## Build
All Gradle operations run in GitHub Actions (CI-only development per `notes.txt`).

```bash
# Local: not supported (no Android SDK/gradle in Termux)
# CI: push to main → Actions build debug APK
# Release: push tag v* → Actions build release APK → GitHub Release
```

## Tech Stack
- Kotlin 2.4.10 + AGP 9.3.0 + Gradle 9.5.0
- Jetpack Compose (BOM 2026.06.00)
- Sora Editor 0.24.6 (LGPL-2.1) for syntax highlighting
- Offline-first: Storage Access Framework (SAF) for file access
- Modules: `:app` + `:core-fs` + `:core-editor` + `:core-preview`

## Architecture (PRD §7)
| Module | Responsibility |
|--------|----------------|
| `core-fs` | SAF file access, hidden file scanner, tree persistence |
| `core-editor` | Sora Editor wrapper, syntax highlighting (HTML/CSS/JS/Kotlin/Python/JSON), multi-tab, auto-save |
| `core-preview` | WebView sandbox + `srcdoc` composition + console capture |
| `core-tools` | Phase 2 (storage analyzer, batch find/replace, hex editor) |

## License
App code: MIT. Sora Editor: LGPL-2.1 (dynamic linking via Gradle dependency, attribution in About screen).

## Attribution
- Sora Editor by Rosemoe (LGPL-2.1)
- TextMate grammars (MIT)
- Fonts: M PLUS Rounded 1c, IBM Plex Sans JP, JetBrains Mono, DotGothic16 (OFL)