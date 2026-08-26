# zaaam/editors

Mobile code editor + file manager + live preview + toolkit untuk Android. Dibuat untuk developer yang ngoding dari HP: buka folder via SAF, edit dengan syntax highlighting Sora, lihat hasilnya langsung di WebView preview dengan console capture — plus kotak alat analisa/duplikat/ganti-massal/heks/snippet yang semuanya jalan offline.

**Status:** v0.2.0 (pre-alpha). Design spec v3 disetujui user: "Zaurus di Meja Kerja" (estetika retro-tech gadget Jepang).

---

## Tutorial — mulai dari nol

1. Install APK dari [GitHub Releases](https://github.com/az925-crypto/Editors/releases).
2. Buka app → dialog SAF muncul → **Pilih folder** project-mu (grant read+write).
3. Tab **Files**: browse file, tap `index.html` → terbuka di Editor. Tap file biner (jpg/zip/apk) → langsung masuk **Editor Heks**.
4. Tab **Editor**: ngoding dengan highlighting; autosave jalan otomatis (debounce ~900ms, LED status di bawah tab strip).
5. Tap chip **Split ◫** untuk pratinjau live berdampingan saat mengetik, atau chip **Preview ▶** / nav **Preview** (muncul untuk html/css/js): render live dokumenmu; file `style.css`/`main.js` yang ikut terbuka sebagai tab ikut tersuntik ke placeholder `<link rel="stylesheet" href="style.css">` / `<script src="main.js"></script>`.
6. Console drawer di bawah preview menampilkan `console.log/warn/error` + error runtime halaman (`preview siap` = instrumentasi aktif).
7. Tab **Alat**: lima perkakas offline — Analisa Penyimpanan, Cari Duplikat, Ganti Massal, Editor Heks, Snippet. Semua berbagi satu pindaian tree (pilih folder sekali, dipakai bersama).

## How-to — tugas umum

| Tugas | Cara |
|---|---|
| Build debug | Push ke `main` → GitHub Actions CI (`assembleDebug`) → unduh artifact APK |
| Rilis versi baru | Bump `versionCode`/`versionName` di `app/build.gradle.kts` → commit → push → tunggu CI hijau → `git tag vX.Y.Z && git push origin vX.Y.Z` → Release workflow upload APK |
| Verifikasi CI | `gh run list --limit 1`; kalau gagal `gh run view <id> --log-failed \| grep "e: file"` |
| Jalankan unit test | Ikut CI (`testDebugUnitTest`) — build/test lokal di Termux tidak didukung |
| Cari file/folder terbesar | Alat → **Analisa Penyimpanan** → pindai; readout total + bar folder terbesar + daftar file terbesar |
| Bereskan duplikat | Alat → **Cari Duplikat** → hash SHA-1 bertahap (aman utk file berubah saat pindai); buka salinannya di editor. Tanpa tombol hapus (by design) |
| Ganti teks lintas file | Alat → **Ganti Massal** → cari literal (bukan regex) → PINDAI → review highlight → GANTI SEKARANG (konfirmasi destruktif; tiap file diverifikasi ulang sebelum ditulis) |
| Intip/edit file biner | Alat → **Editor Heks** (atau tap biner dari Files) — batas 16 MB/file; undo 32 langkah; simpan = tulis balik utuh |
| Simpan potongan kode | Alat → **Snippet** → + BARU; ekspor/impor JSON skema `zaaam-snippets` v1 |

## Reference — arsitektur

| Modul | Tanggung jawab |
|---|---|
| `:app` | UI Compose (Files/Editor/Preview/Alat), ViewModel + manager shared (`TreeScanManager`, `SnippetRepository`), DI manual (`AppContainer`), tema RetroTokens |
| `:core-fs` | SAF filesystem (`SafFileSystemImpl`: teks, bytes, stream), permission tree (`TreeAccess`), resolver jenis file |
| `:core-editor` | Wrapper Sora 0.23.6 (`EditorEngine`), TextMate preload, sesi tab, `LanguageResolver` |
| `:core-preview` | `PreviewComposer` (komposisi HTML + instrumentasi console), `PreviewWebViewFactory` (WebView hardened + `ConsoleBridge` rate-limited) |
| `:core-tools` | Engine alat murni-JVM: `TreeScanner` (DFS iteratif anti-siklus), `StorageAnalyzer`, `DuplicateFinder` (SHA-1 4 fase + guard file berubah), `FindReplaceEngine` (literal + replaceVerified anti-autosave-race), `HexSupport` (guard 16MB), `SnippetJsonCodec` (parser ketat depth-guarded) |

Jembatan data inti: `AppContainer.editorContents` (Files menulis sebelum `addTab`, Editor membaca/menulis saat edit), `AppContainer.previewTick` (Editor memberi tahu Preview isi file web berubah), dan `AppContainer.toolsTab`/`hexTargetUri` (navigasi sub-alat + entry hex dari Files). Peta lengkap per file: [STRUCTURE.md](STRUCTURE.md). State & riwayat kerja: [PROGRESS.md](PROGRESS.md).

**Keputusan teknis penting** (jangan dilanggar tanpa paham trade-off-nya):

- **Build/test hanya via GitHub Actions** — tidak ada Android SDK/Gradle di Termux.
- **Tanpa permission INTERNET**, WebView hardened penuh (akses file/content mati, semua navigasi keluar diblok) — preview hanya merender dokumen komposisi sendiri, offline-first.
- **Autosave anti-korupsi:** job in-flight tidak dibatalkan (anti file kepotong), mutex per-uri menjaga urutan, guard divergence mencegah klaim "Tersimpan" palsu, file biner tidak pernah ditulis balik oleh autosave.
- **Engine alat murni JVM** (uri String + lambda injected): semua IO lewat dispatcher yang di-inject caller; progress dikonflasi via StateFlow; parser JSON untrusted depth-guard 128.
- **Sora 0.23.6 dari Maven Central** (0.24.x masih prerelease); JITPACK DILARANG (riwayat timeout).

## Explanation — kenapa begini

Desain mengikuti spec v3 (lihat `docs/design-spec.md`): bone plastic + graphite + LCD olive, skeuomorphism selektif (bevel/screw/LED) tanpa blur — dipilih lewat voting eksplisit setelah dua arah ditolak user. Arsitektur sengaja tanpa library eksternal selain Sora + Compose: DI manual, navigasi enum + StateFlow, tanpa Room/Navigation-Compose — memperkecil permukaan bug untuk aplikasi satu-developer, dengan konsekuensi beberapa pola harus dijaga manual (lihat gotcha di PROGRESS.md). Engine alat dipisah dari UI supaya logika berat (hashing, scanning, parsing) bisa di-unit-test JVM murni di CI tanpa emulator.

## License

App code: MIT. Sora Editor: LGPL-2.1 (dynamic linking via Gradle dependency).
