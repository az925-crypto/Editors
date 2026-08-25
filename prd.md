# PRD: [Nama Aplikasi — TBD]
### Code Editor + File Manager Mobile (dengan Live Preview)

**Versi:** 1.0 (Draft)
**Tanggal:** 25 Agustus 2026
**Status:** Draft — siap direview / di-feed ke planner agent

---

## 1. Ringkasan

Aplikasi Android yang menggabungkan file manager dan code editor dalam satu app, dilengkapi live preview untuk file web (HTML/CSS/JS). Ditujukan untuk developer yang ngoding langsung dari HP tanpa laptop.

---

## 2. Masalah

Developer yang ngoding dari HP saat ini harus pakai minimal 3 aplikasi terpisah untuk satu alur kerja:

1. File manager — buat browse/cari file project, termasuk file hidden
2. Code editor terpisah — buat edit kode
3. Browser/app lain — buat preview hasil HTML/CSS/JS

Bolak-balik antar app ini bikin alur kerja lambat dan terputus, apalagi kalau file harus di-share manual antar aplikasi.

**User story:**
Seorang Android developer yang ngoding cuma pakai HP karena belum punya laptop. Dia harus install file manager terpisah buat cari file project, pindah ke app editor buat edit, lalu kalau mau lihat hasil HTML/CSS/JS-nya harus export atau share ke browser dulu.

---

## 3. Target User

- Developer (Android/web) yang koding dari mobile sebagai device utama, bukan sekunder
- Terbiasa utak-atik file system Android, kadang butuh akses ke file/folder yang di-hide default
- Device kelas menengah ke bawah (contoh target: Snapdragon 680, RAM 8GB) — bukan flagship

---

## 4. Tujuan & Metrik Sukses

| Tujuan | Metrik |
|---|---|
| Kurangi app-switching | User bisa buka file → edit → preview tanpa keluar app |
| Jadi daily driver | User balik tiap hari buat ngoding, bukan sekali pakai lalu di-uninstall |
| Ringan di device low-end | Tetap responsif di RAM 8GB, ga freeze pas file tree besar |

---

## 5. Scope

### MVP (v1) — wajib ada
- File browser dengan akses ke hidden files (dotfiles, folder `.thumbnails`, `.trashed`, dll)
- Code editor dengan syntax highlighting (minimal: HTML, CSS, JS, Kotlin, Python, JSON)
- Live preview untuk file HTML/CSS/JS, render di dalam app
- Multi-tab editing (buka beberapa file sekaligus)
- Auto-save

### Phase 2 — nice-to-have, bukan blocker rilis awal
- Storage analyzer (cari file duplikat/besar)
- Batch find & replace lintas file
- Hex editor untuk file binary
- Root-level access (device yang di-root, pakai libsu)
- Import/export snippet ke Codexa

### Eksplisit di luar scope (v1)
- Kolaborasi real-time / multi-user
- Cloud sync (offline-first dulu)
- Compile & run bahasa terkompilasi (v1 cukup edit + syntax highlight, bukan compiler)

---

## 6. Alur Utama (User Flow)

1. Buka app → langsung liat file tree (root storage + recent files)
2. Tap folder/file → kalau file kode, langsung kebuka di editor
3. Edit file → auto-save jalan di background
4. Kalau file HTML/CSS/JS → muncul tab/tombol **Preview** → render langsung tanpa pindah app
5. (Opsional) Toggle "show hidden files" buat lihat dotfiles/folder tersembunyi

**Catatan penting:** task pertama yang HARUS bisa diselesaikan user di kunjungan pertama adalah **buka file kode**. Ini fondasi — fitur lain (preview, cari hidden file) baru kepake setelah step ini berhasil.

---

## 7. Kebutuhan Teknis

**Stack:** Kotlin + Jetpack Compose, offline-first

**Modul arsitektur (modular, bukan monolitik):**

| Modul | Tanggung Jawab |
|---|---|
| `core-fs` | Akses file via Storage Access Framework (SAF), fallback opsional ke `MANAGE_EXTERNAL_STORAGE`, hidden file scanner |
| `core-editor` | Wrapper editor (kandidat: Sora Editor), syntax highlighting, multi-tab |
| `core-preview` | WebView/`iframe sandbox` dengan `srcdoc`, debounce render (~300–500ms), console log capture |
| `core-tools` | *(Phase 2)* storage analyzer, duplicate finder, batch find-replace |

**Constraint performa (target device: RAM 8GB, Snapdragon 680):**
- File tree lazy-loaded — jangan scan seluruh storage di awal
- Preview engine cuma aktif kalau file type-nya web, ga jalan terus di background

---

## 8. Keputusan Terbuka

Hal-hal ini masih perlu diputuskan sebelum atau selama development:

1. **SAF vs `MANAGE_EXTERNAL_STORAGE`** — SAF lebih aman & Play Store-compliant tapi lebih terbatas; permission penuh butuh review ketat kalau mau publish ke Play Store. Ini nentuin arah distribusi (Play Store vs sideload/APK langsung).
2. **Nama & positioning app** — belum ditentukan, perlu dibedakan jelas dari Acode/MT Manager.
3. **Editor engine final** — Sora Editor vs CodeMirror (WebView-based) vs opsi lain, tergantung trade-off performa vs kemudahan integrasi.

---

## 9. Diferensiasi

Dibanding cara sekarang (app terpisah) atau kompetitor (Acode, MT Manager):

- **Semua dalam satu app** — ga perlu share/export file antar aplikasi
- **Live preview langsung** — Acode punya editor bagus tapi preview-nya lemah/ga smooth; ini jadi selling point utama
- **Akses file lebih dalam** — termasuk hidden files, bukan cuma file manager biasa
