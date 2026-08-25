# zaaam/editors

Mobile code editor + file manager + live preview untuk Android. Dibuat untuk developer yang ngoding dari HP: buka folder via SAF, edit dengan syntax highlighting Sora, lihat hasilnya langsung di WebView preview dengan console capture.

**Status:** v0.1.2 (pre-alpha, MVP berfungsi). Design spec v3 disetujui user: "Zaurus di Meja Kerja" (estetika retro-tech gadget Jepang).

---

## Tutorial — mulai dari nol

1. Install APK dari [GitHub Releases](https://github.com/az925-crypto/Editors/releases).
2. Buka app → dialog SAF muncul → **Pilih folder** project-mu (grant read+write).
3. Tab **Files**: browse file, tap `index.html` → terbuka di Editor.
4. Tab **Editor**: ngoding dengan highlighting; autosave jalan otomatis (debounce ~900ms, LED status di bawah tab strip).
5. Tap chip **Preview ▶** (muncul untuk html/css/js) atau nav **Preview**: render live dokumenmu; file `style.css`/`main.js` yang ikut terbuka sebagai tab ikut tersuntik ke placeholder `<link rel="stylesheet" href="style.css">` / `<script src="main.js"></script>`.
6. Console drawer di bawah preview menampilkan `console.log/warn/error` + error runtime halaman (`preview siap` = instrumentasi aktif).

## How-to — tugas umum

| Tugas | Cara |
|---|---|
| Build debug | Push ke `main` → GitHub Actions CI (`assembleDebug`) → unduh artifact APK |
| Rilis versi baru | Bump `versionCode`/`versionName` di `app/build.gradle.kts` → commit → push → tunggu CI hijau → `git tag vX.Y.Z && git push origin vX.Y.Z` → Release workflow upload APK |
| Verifikasi CI | `gh run list --limit 1`; kalau gagal `gh run view <id> --log-failed \| grep "e: file"` |
| Jalankan unit test | Ikut CI (`testDebugUnitTest`) — build/test lokal di Termux tidak didukung |

## Reference — arsitektur

| Modul | Tanggung jawab |
|---|---|
| `:app` | UI Compose (Files/Editor/Preview), ViewModel, DI manual (`AppContainer`), tema RetroTokens |
| `:core-fs` | SAF filesystem (`SafFileSystemImpl`), permission tree (`TreeAccess`), resolver jenis file |
| `:core-editor` | Wrapper Sora 0.23.6 (`EditorEngine`), TextMate preload, sesi tab, `LanguageResolver` |
| `:core-preview` | `PreviewComposer` (komposisi HTML + instrumentasi console), `PreviewWebViewFactory` (WebView hardened + `ConsoleBridge` rate-limited) |

Jembatan data inti: `AppContainer.editorContents` (Files menulis sebelum `addTab`, Editor membaca/menulis saat edit) dan `AppContainer.previewTick` (Editor memberi tahu Preview isi file web berubah). Peta lengkap per file: [STRUCTURE.md](STRUCTURE.md). State & riwayat kerja: [PROGRESS.md](PROGRESS.md).

**Keputusan teknis penting** (jangan dilanggar tanpa paham trade-off-nya):

- **Build/test hanya via GitHub Actions** — tidak ada Android SDK/Gradle di Termux.
- **Tanpa permission INTERNET**, WebView hardened penuh (akses file/content mati, semua navigasi keluar diblok) — preview hanya merender dokumen komposisi sendiri, offline-first.
- **Autosave anti-korupsi:** job in-flight tidak dibatalkan (anti file kepotong), mutex per-uri menjaga urutan, guard divergence mencegah klaim "Tersimpan" palsu, file biner tidak pernah ditulis balik.
- **Sora 0.23.6 dari Maven Central** (0.24.x masih prerelease); JITPACK DILARANG (riwayat timeout).

## Explanation — kenapa begini

Desain mengikuti spec v3 (lihat `docs/design-spec.md`): bone plastic + graphite + LCD olive, skeuomorphism selektif (bevel/screw/LED) tanpa blur — dipilih lewat voting eksplisit setelah dua arah ditolak user. Arsitektur sengaja tanpa library eksternal selain Sora + Compose: DI manual, navigasi enum + StateFlow, tanpa Room/Navigation-Compose — memperkecil permukaan bug untuk aplikasi satu-developer, dengan konsekuensi beberapa pola harus dijaga manual (lihat gotcha di PROGRESS.md).

## License

App code: MIT. Sora Editor: LGPL-2.1 (dynamic linking via Gradle dependency).
