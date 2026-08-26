# QA Manual — zaaam/editors v0.2.0

Checklist manual untuk setiap rilis (jalankan di device target Snapdragon 680 / 8GB RAM).
Di-refresh 2026-08-26 pasca-Phase 2 (6 layar ALAT: analisa, duplikat, ganti massal, heks, snippet).

## 1. Install
- [ ] Download APK dari GitHub Releases (`zaaam-editors-0.2.0.apk`)
- [ ] `adb install -r zaaam-editors-0.2.0.apk` sukses (atau sideload via file manager)
- [ ] App launch < 2 detik cold start, tidak white-flash

## 2. Files screen
- [ ] First launch → dialog SAF blocking → **Pilih folder** → grant read+write → list muncul; **Nanti** → banner "Akses ditolak" gaya BrickWash
- [ ] Tap folder masuk; tombol back sistem naik folder (breadcrumb ikut); breadcrumb tap segmen lompat folder
- [ ] Search debounce 200ms filter inline
- [ ] Toggle HIDDEN: dotfiles muncul/hilang
- [ ] Tray recents max 3 kartu; tap recents buka file (guard uri basi aman)
- [ ] Tap file web/kode → pindah Editor + tab baru; tap file biner (jpg/png/zip/apk) → langsung masuk **Editor Heks** (rider Phase 2)

## 3. Editor screen
- [ ] Tab strip multi-file; dirty dot oranye saat edit; × menutup tab
- [ ] Ketik → LED "Menyimpan…" → "Tersimpan HH:MM" → hilang ~2 detik; syntax highlighting sesuai bahasa; ekstensi tak dikenal tetap rapi (grammar text.plain)
- [ ] Autosave benar-benar menulis ke disk: edit di app → cek file via file manager lain
- [ ] Pindah tab cepat saat ngetik → isi kedua tab tidak tertukar (race guard)
- [ ] Chip **Preview ▶** hanya muncul untuk html/css/js; tap langsung lompat ke layar Preview
- [ ] Chip **Split ◫** hanya muncul untuk html/css/js; toggle → editor atas + pratinjau bawah (landscape: samping-samping); ketik → render ikut terbaru (<~1 detik)
- [ ] Split ON → ganti dokumen web lain → pane menampilkan dokumen BARU (tidak ada render basi dokumen lama di bawah header baru)
- [ ] Split ON → tutup tab web aktif / pindah ke file non-web → editor full kembali, tanpa crash; buka file web lagi → pane muncul dengan isi benar
- [ ] Rotasi saat split ON → posisi cursor/undo editor tidak hilang; layout flip portrait↔landscape benar
- [ ] Matikan Split → buka app ulang dari launcher → chip masih ON (persist prefs)

## 4. Preview screen
- [ ] Masuk Preview dari chip/nav → konten aktif langsung tampil (seed instan), urlbar menampilkan uri `content://…` nyata
- [ ] Edit HTML di Editor → balik ke Preview → render terbaru (push tick, debounce 350ms); konsol histori tetap ada karena VM di-share dengan pane split
- [ ] Trio klasik: buka index.html + style.css + main.js sebagai tab → preview menyuntik keduanya ke placeholder `<link rel="stylesheet" href="style.css">` / `<script src="main.js"></script>`
- [ ] Buka file .css langsung → scaffold style; file .js langsung → dieksekusi TEPAT SATU kali (cek log tidak dobel)
- [ ] Console drawer: collapsed bar 40dp ↔ expanded 40% (maks 320dp) animasi halus; badge LOG/WARN/ERR + timestamp; Clear mengosongkan
- [ ] Flood test: loop `console.log` 1000x → UI tetap responsif (rate-limit 30/s, cap 200 entri)
- [ ] Klik link keluar / navigasi dari halaman → DIBLOK (tidak ada navigasi keluar)

## 5. ALAT — Hub & Analisa Penyimpanan (Phase 2)
- [ ] Nav **Alat** → hub 5 kartu cartridge AN/DQ/FR/HX/SN; tap kartu pindah sub-tab; pill sub-nav ikut aktif
- [ ] Back sistem dari sub-tab → balik ke HUB (tidak keluar app); back di HUB → perilaku normal
- [ ] Analisa: belum pilih folder → CTA PILIH FOLDER (SAF); setelah pilih → progress "Memindai…" dengan angka berjalan (bukan beku)
- [ ] Hasil: readout total MB + jumlah file/folder/dilewati; bar FOLDER TERBESAR proporsional; FILE TERBESAR dengan stencil kode ekstensi
- [ ] Toggle SERTAKAN HIDDEN → auto rescan (dotfiles masuk/hilang); PINDAI ULANG jalan ulang
- [ ] Folder dengan subfolder tak-bisa-dibaca → catatan "N folder … dilewati" tanpa crash

## 6. ALAT — Cari Duplikat
- [ ] Masuk tab → hash otomatis jalan setelah walk selesai ("Menghitung hash… n/m"); grup duplikat muncul berurut boros-terbesar
- [ ] Grup expand/collapse (tulis ▾/▴) menampilkan tiap salinan + foldernya; checkbox grup bisa on/off — TIDAK ADA tombol hapus (by design v0.2)
- [ ] BUKA › pada satu salinan → file terbuka di Editor (file teks); file biner → banner gagal buka generik
- [ ] Footer: "N grup · total X boros · file yang berubah saat pindai otomatis dikecualikan"
- [ ] PINDAI ULANG saat hash masih jalan → hasil lama TIDAK nyasar ke tree baru (guard generasi)

## 7. ALAT — Ganti Massal
- [ ] Query kosong/spasi → tombol PINDAI & GANTI mati; query biasa → PINDAI → label "HASIL PINDAI · N FILE · M LOKASI"
- [ ] Preview per match: highlight OliveWash tepat pada kata; baris panjang (minified) tetap ringan, nomor baris benar
- [ ] Checkbox per file memilih/melepas target; tombol "GANTI DI N FILE (M LOKASI)" hitung ulang live
- [ ] GANTI → sheet konfirmasi "Tidak bisa dibatalkan…" → BATAL batal bersih; GANTI SEKARANG → status ✓ N terganti per file
- [ ] File yang berubah saat scan (mis. kena autosave) → baris redup + "BERUBAH — DILEWATI", isinya TIDAK tersentuh
- [ ] Verifikasi disk: hasil ganti benar dibaca editor lain; replacement berisi query sendiri ("a"→"aa") tidak infinite

## 8. ALAT — Editor Heks
- [ ] Tap biner dari Files → mendarat di HEKS dengan nama+ukuran benar; LED IDLE
- [ ] File juga terbuka sebagai tab editor → banner "FILE INI JUGA TERBUKA DI EDITOR"
- [ ] Tap byte → sheet edit (nilai sekarang hex+desimal); input 3 karakter ditolak; TERAPKAN → cell ter-highlight wash + ascii kolom ikut berubah
- [ ] UNDO ≤32 langkah mundur persis; langkah ke-33 membuang yang tertua
- [ ] Lompat offset `0x…` (IME Search) → scroll ke baris itu
- [ ] SIMPAN → LED MENYIMPAN→TERSIMPAN→idle; verifikasi via file manager lain isi berubah; edit lagi → hanya cell berubah yang highlight
- [ ] File >16MB → banner penolakan, tabel tak dimuat; file 0-byte → pesan "(file kosong)" bukan error

## 9. ALAT — Snippet
- [ ] + BARU → form mini (nama/bahasa/tag/kode); SIMPAN butuh nama+kode; kartu muncul dengan stencil bahasa + tagpill
- [ ] Tap kartu → form terisi untuk edit; HAPUS menghapus permanen
- [ ] EKSPOR JSON → SAF folder → file `zaaam-snippets-YYYYMMDD.json` berisi semua snippet; sheet sukses menyebut nama aktual
- [ ] IMPOR FILE → pilih JSON sah → sheet "N ditambah · M dilewati · K gagal parse"; id sama TIDAK menimpa data lama
- [ ] IMPOR file ngawur (bukan skema) → pesan "bukan skema zaaam-snippets"; JSON nesting >128 level ditolak tanpa crash
- [ ] Chip CODEXA abu-abu disabled + catatan menunggu spec
- [ ] Kill app → buka lagi → snippet masih ada (prefs snippets_v1)

## 10. Navigation
- [ ] Bottom nav 4 item (Files·Editor·Preview·Alat) selalu bisa ditap
- [ ] Files ↔ Editor ↔ Preview ↔ Alat bolak-balik tanpa kehilangan state tab/isi/pindai

## 11. Performance (SD680)
- [ ] Scroll file tree besar tidak jank
- [ ] Ketik panjang di html besar (~1MB): tidak freeze; compose preview max ~3x/detik
- [ ] Ganti Massal atas file minified besar (single-line ~1MB+) tidak OOM/lag parah (preview windowed)
- [ ] APK release < 5 MB

## 12. Release
- [ ] Bump versionCode/versionName → push main → CI hijau → tag `vX.Y.Z` → Release workflow hijau + APK muncul di Releases
- [ ] **Smoke-test APK RELEASE (bukan debug) di device:** buka app → tidak force close saat cold start (preload TextMate), dialog SAF muncul, buka file teks → highlighting jalan
- [ ] Update-over-install dari v0.1.x/v0.2.x tanpa uninstall (data prefs SAF tree uri + recents kebaca lagi; snippets mulai kosong = normal)
