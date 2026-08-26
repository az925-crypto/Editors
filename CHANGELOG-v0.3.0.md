# UPDATE zaaam/editors

> **Kalkulators versi 0.3.0 kini tersedia!** Aplikasi editor kode & file manager offline yang jalan penuh dari HP — 100% offline, tanpa akun, tanpa server.

━━━━━━━━━━━━━━━━━
🔥 *FITUR BARU UNGGULAN: LIVE PREVIEW SPLIT*
Editor dan hasil render HTML **kini tampil bersamaan** — ketik di atas, lihat hasilnya langsung di bawah, tanpa pindah tab.

━━━━━━━━━━━━━━━━━
✨ *Yang baru di versi ini:*
- **Chip "Split ◫"** — aktifkan live preview berdampingan langsung dari layar Editor
- Preview & editor berbagi satu pipeline: ketikan → debounce → render otomatis (~½ detik)
- Layout adaptif: portrait atas-bawah, landscape samping-samping — state editor (cursor/undo) tidak hilang saat rotasi
- Pilihan tersimpan antar sesi — enable sekali, split tetap nyala tiap kali buka file web
- WebView hardened penuh (akses file dimatikan, navigasi keluar diblok, bridge console rate-limited)
- Semua file web support: HTML, CSS, JS — file CSS/JS terbuka otomatis disuntikkan ke halaman

━━━━━━━━━━━━━━━━━
🔐 *PERBAIKAN KEAMANAN & STABILITAS (fix CRITICAL cold start):*
- **Fix force-close di APK release v0.2.0** — `languages.json` salah kunci (`"path"` vs `"grammar"`) yang membuat NPE saat startup; belum pernah ketahuan karena CI tidak menjalankan app
- Preload TextMate kini dijaga: gagal = editor jalan tanpa highlighting, bukan crash total
- Test regresi baru `TextMateAssetsContractTest` memastikan format asset tidak pernah berubah lagi
- WebView leak pre-existing diperbaiki — `onRelease { destroy() }` menutup native layer saat keluar komposisi
- Review loop ulang: security/blocking = clean, bug = clean (3 temuan Medium difix & re-audit bersih)

━━━━━━━━━━━━━━━━━
📦 *Total isi v0.3.0:*
- Live preview split view di layar Editor
- Fix cold start FC di APK release + test regresi asset
- Reviewer loop bersih (security + bug + performance + maintainability)
- Documentation (PROGRESS, STRUCTURE, qa-manual, README restyle)

━━━━━━━━━━━━━━━━━
⚠️ *Catatan:*
App hanya jalan offline — tanpa permission INTERNET. Data tersimpan lokal di device. Backup folder mu sendiri. Draft ulang saat rilis besar berikutnya.

━━━━━━━━━━━━━━━━━

## 🔗 Link

### 📥 Download APK
https://github.com/az925-crypto/Editors/releases/latest

### 💻 Source Code
https://github.com/az925-crypto/Editors

### ☕ Support
https://saweria.co/Zsmm

### 📢 Channel
https://whatsapp.com/channel/0029Vb7ZuEK3QxRtvlO89u0u
