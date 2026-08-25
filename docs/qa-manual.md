# QA Manual — zaaam/editors v0.1.0

Checklist manual untuk setiap rilis (jalankan di device target Snapdragon 680 / 8GB RAM).

## 1. Install
- [ ] Download APK dari GitHub Releases (zaaam-editors-v0.1.0.apk)
- [ ] `adb install -r zaaam-editors-v0.1.0.apk` sukses (atau sideload via file manager)
- [ ] App launch < 2 detik cold start, tidak white-flash (bone_shel windowBackground)

## 2. Files screen
- [ ] Buka app → langsung lihat file tree + breadcrumb `/storage/emulated/0`
- [ ] Tap folder → expand/collapse, chevron rotate
- [ ] Filter search → hasil filter inline, highlight
- [ ] Toggle HIDDEN: ON/OFF → dotfiles muncul/hilang (opacity, stripe)
- [ ] Tap file (index.html) → pindah ke Editor, tab baru muncul

## 3. Editor screen
- [ ] Tab strip: tap file lain → tab baru, dirty dot muncul saat edit
- [ ] Edit teks → chip "Menyimpan…" → "Tersimpan HH:MM" → fade 2s
- [ ] Undo/redo via toolbar (jika ada)
- [ ] Preview button hanya muncul untuk html/css/js, tidak untuk .kt/.py
- [ ] File biner (apk/jpg) → card error "File tidak bisa dibuka sebagai teks"

## 4. Preview screen
- [ ] Buka html → Preview → urlbar `file://` + progress 350ms + viewport putih render
- [ ] Edit html di Editor → kembali ke Preview → render update (LIVE)
- [ ] Console drawer: tap header → expand 40%, log/warn/error badge, counter
- [ ] Refresh → progress + chip "Memperbarui…"
- [ ] File non-web → Preview show empty state "Tidak ada preview"

## 5. Navigation
- [ ] Bottom nav: Files selalu enabled, Editor disabled jika 0 tab, Preview disabled jika bukan web
- [ ] Tap disabled → tooltip "Buka file dulu" / "Hanya file web bisa di-preview"
- [ ] Back dari Editor/Preview → kembali ke Files

## 6. Permissions
- [ ] First launch → dialog SAF blocking → Pilih folder → grant → list muncul
- [ ] First launch → Nanti → banner Brick Wash "Akses ditolak" + Files tetap demo

## 7. Performance
- [ ] Scroll file tree besar tidak jank
- [ ] Preview hanya aktif saat file web aktif (cek di profiler, WebView tidak jalan di background)
- [ ] APK size < 15 MB (debug)

## 8. Release
- [ ] `git tag v0.1.0 && git push --tags` → GitHub Release terbuat + APK ter-upload
- [ ] Update-over-install dari v0.0.x (jika ada) tidak perlu uninstall