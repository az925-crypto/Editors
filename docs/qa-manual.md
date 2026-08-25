# QA Manual — zaaam/editors v0.1.2

Checklist manual untuk setiap rilis (jalankan di device target Snapdragon 680 / 8GB RAM).
Di-refresh 2026-08-25 pasca-Fase 4 (preview live + console bridge).

## 1. Install
- [ ] Download APK dari GitHub Releases (`zaaam-editors-0.1.2.apk`)
- [ ] `adb install -r zaaam-editors-0.1.2.apk` sukses (atau sideload via file manager)
- [ ] App launch < 2 detik cold start, tidak white-flash

## 2. Files screen
- [ ] First launch → dialog SAF blocking → **Pilih folder** → grant read+write → list muncul; **Nanti** → banner "Akses ditolak" gaya BrickWash
- [ ] Tap folder masuk; tombol back sistem naik folder (breadcrumb ikut); breadcrumb tap segmen lompat folder
- [ ] Search debounce 200ms filter inline
- [ ] Toggle HIDDEN: dotfiles muncul/hilang
- [ ] Tray recents max 3 kartu; tap recents buka file (guard uri basi aman)
- [ ] Tap file web/kode → pindah Editor + tab baru; tap file biner → tidak dibuka (guard anti-korupsi)

## 3. Editor screen
- [ ] Tab strip multi-file; dirty dot oranye saat edit; × menutup tab
- [ ] Ketik → LED "Menyimpan…" → "Tersimpan HH:MM" → hilang ~2 detik; syntax highlighting sesuai bahasa; ekstensi tak dikenal tetap rapi (grammar text.plain)
- [ ] Autosave benar-benar menulis ke disk: edit di app → cek file via file manager lain
- [ ] Pindah tab cepat saat ngetik → isi kedua tab tidak tertukar (race guard)
- [ ] Chip **Preview ▶** hanya muncul untuk html/css/js; tap langsung lompat ke layar Preview

## 4. Preview screen (Fase 4)
- [ ] Masuk Preview dari chip/nav → konten aktif langsung tampil (seed instan), urlbar menampilkan uri `content://…` nyata
- [ ] Edit HTML di Editor → balik ke Preview → render terbaru (push tick, debounce 350ms)
- [ ] Trio klasik: buka index.html + style.css + main.js sebagai tab → preview menyuntik keduanya ke placeholder `<link rel="stylesheet" href="style.css">` / `<script src="main.js"></script>`
- [ ] Buka file .css langsung → scaffold style; file .js langsung → dieksekusi TEPAT SATU kali (cek log tidak dobel)
- [ ] Console drawer: collapsed bar 40dp ↔ expanded 40% (maks 320dp) animasi halus; badge LOG/WARN/ERR + timestamp; Clear mengosongkan
- [ ] `console.log/warn/error` + error runtime halaman masuk console; pesan "preview siap" muncul sekali per muat = instrumentasi aktif
- [ ] Flood test: loop `console.log` 1000x → UI tetap responsif (rate-limit 30/s, cap 200 entri)
- [ ] Tombol ↻ memaksa reload ulang dokumen yang sama
- [ ] Klik link keluar / navigasi dari halaman → DIBLOK (tidak ada navigasi keluar)
- [ ] File non-web → empty state "Tidak ada preview"

## 5. Navigation
- [ ] Bottom nav 3 tombol selalu bisa ditap (logika disabled §9.4 spec belum diimplementasikan — known limitation v0.1.2)
- [ ] Files ↔ Editor ↔ Preview bolak-balik tanpa kehilangan state tab/isi

## 6. Performance (SD680)
- [ ] Scroll file tree besar tidak jank
- [ ] Ketik panjang di html besar (~1MB): tidak freeze; compose preview max ~3x/detik
- [ ] APK release < 5 MB (aktual ~3.2 MB)

## 7. Release
- [ ] Bump versionCode/versionName → push main → CI hijau → tag `vX.Y.Z` → Release workflow hijau + APK muncul di Releases
- [ ] Update-over-install dari v0.1.x tanpa uninstall (data prefs SAF tree uri kebaca lagi)
