# HANDOFF-PHASE2.md — Prompt eksekusi lanjutan (tempel ke sesi AI berikutnya)

> Dibuat 2026-08-26 pasca-handoff engine Phase 2. State terverifikasi saat prompt ini ditulis:
> HEAD `8dca9b2`, CI hijau, engine `:core-tools` GREEN, mockup approved, UI belum dibangun.
> Sumber kebenaran utama tetap PROGRESS.md §PHASE 2 IN-FLIGHT.

---

Kamu ambil alih repo Android zaaam/editors (branch main). WAJIB verifikasi dulu: `gh run list --limit 1`
harus GREEN sebelum sentuh apa pun.

SEBELUM APA PUN (urutan wajib):
1. Baca PROGRESS.md — khususnya §PHASE 2 IN-FLIGHT (sumber kebenaran state + keputusan user terkunci
   + urutan eksekusi + gotcha yang sudah dibayar mahal).
2. Baca STRUCTURE.md §6b (core-tools) + §3 (UI existing).
3. Baca mockup/phase2.html (APPROVED user — UI Compose wajib mengikuti ini; gerbang mockup-first
   SUDAH lewat, jangan tanya approval ulang).

MISI: tuntaskan Phase 2 PRD sampai rilis v0.2.0. Engine :core-tools sudah SELESAI & CI GREEN
(HEAD b0c104f) — kamu lanjut dari titik itu:

1. [BLOCKING] Reviewer trio PARALEL atas diff engine `git diff 45cd930..b0c104f`:
   security-reviewer (fokus: SnippetJsonCodec parser file untrusted, DuplicateFinder),
   bug-reviewer (fokus: interaksi autosave × replaceVerified, scanner cancellation),
   performance-reviewer (scanner di IO? progress emit throttled?). Ketiganya BLOCKING —
   fix-loop sampai BLOCKING: no SEBELUM mulai UI.
2. [UI commit 7-12] Bangun sesuai rencana di PROGRESS.md §PHASE 2 IN-FLIGHT:
   - Nav: AppScreen + TOOLS; session/ToolsTab.kt; container.toolsTab + hexTargetUri;
     BackHandler sub-tab → hub; bottom nav 4 item (Files·Editor·Preview·Alat).
   - TreeScanManager shared (satu walk di-cache utk Analyzer+Dupes+FindReplace), launcher SAF
     pola FilesViewModel, loadJob cancel + generation guard, pesan error generik MSG_*.
   - Layar per mockup: ToolsHub (5 kartu cartridge AN/DQ/FR/HX/SN), AnalyzerScreen (readout pixel
     + folder/file terbesar), DuplicateScreen (grup expandable, checkbox TANPA tombol hapus),
     FindReplaceScreen (form + highlight OliveWash + sheet konfirmasi destruktif "Tidak bisa
     dibatalkan" + status "BERUBAH — DILEWATI"), HexScreen (LazyColumn baris hex formatRow +
     sheet edit byte + undo capped 32 + LED save pola autosave + banner warning kalau uri juga
     tab editor + guard >16MB), SnippetsScreen (kartu + FORM MINI tambah/edit + export via
     OpenDocumentTree→createFile("application/json") + import OpenDocument sniff schema;
     repo prefs key "snippets_v1"; notice Codexa disabled).
   - Rider: FilesViewModel.openFile BINARY → set hexTargetUri + screenState TOOLS (entry hex).
   - Semua IO withContext(IO), cancellable, state UI dari main, uri String + adapter tipis.
3. Reviewer trio lagi atas diff UI + maintainability-reviewer (non-blocking) → fix-loop sampai bersih.
4. Docs sinkron: PROGRESS.md (Phase 2 pindah ke Done, backlog baru), STRUCTURE.md (§3 tools UI rows,
   §6b status final), qa-manual (skrip uji 4 fitur baru), README (fitur ALAT).
5. Rilis v0.2.0: versionCode=4 / versionName=0.2.0 → push → CI hijau → tag `v0.2.0` →
   verifikasi Release workflow HIJAU + APK muncul (`gh release list`).
   Gotcha rilis: proguard sudah ada -dontwarn kotlin.Cloneable$DefaultImpls — jangan dihapus.
6. telegram_notify hanya 4 momen: task selesai, butuh keputusan, temuan BLOCKING, temuan review
   layak diketahui (rangkum SATU pesan).

ATURAN KERJA (WAJIB):
- Build/test BERAT HANYA via GitHub Actions: edit → commit → push → `gh run list --limit 1`.
  DILARANG gradle lokal. Iterasi gagal: `gh run view <id> --log-failed | grep "e: file"`.
- Commit per logika; boleh satu push untuk satu batch CI.
- Unit test pure JVM JUnit4 — DILARANG konstruksi android.*; pure logic internal top-level;
  kotlinx-coroutines-test WAJIB dideklarasikan eksplisit di build.gradle module yang memakainya.
- Bug INDEPENDEN → delegasi bugfixer (1 panggilan per bug); cluster EditorViewModel/PreviewViewModel/
  AppContainer/nav = DEPENDENT → kerjakan langsung sendiri berurutan.
- Keputusan non-teknis/desain ambigu → tanya user (chat/Telegram), jangan asumsi. Scope Phase 2
  SUDAH TERKUNCI (lihat PROGRESS.md) — jangan buka keputusan lama.
- Update PROGRESS.md + STRUCTURE.md di AKHIR sesi.

GOTCHA KERAS (riwayat nyeri — lengkap di PROGRESS.md):
- relPath hasil DFS bawa prefix parent ("assets/img/a.jpg", bukan "a.jpg").
- Byte VALUE ≠ offset (byte[41] = ')' bukan 'A').
- Deklarasi model cuma SATU tempat (FileFindReport sempat dobel → Redeclaration).
- AGP 9: tanpa plugin kotlin-android eksplisit, tanpa kotlinOptions{}, library module tanpa
  targetSdk, signingConfigs sebelum buildTypes. JITPACK DILARANG; Sora 0.23.6 jangan di-upgrade.
- Reserved jangan dihapus: ui/theme/Shape.kt, Type.kt, FilesViewModel.navigateUp().
- Kontrak editorContents tulis-sebelum-addTab; pathStack main-only; IO withContext.
- Known limitation SENGAJA (jangan sentuh): ketikan ~900ms sebelum closeTab hilang (anti-resurrect);
  job autosave in-flight tak dicancel (anti kepotong); compose(html,null,null) identity;
  placeholder composer exact-string casing-sensitive.

DEFINISI SELESAI: reviewer engine+UI blocking no + 6 layar ALAT hidup end-to-end sesuai mockup +
docs sinkron + v0.2.0 rilis hijau + APK terverifikasi. Kalau scope kepanjangan satu sesi,
prioritaskan urutan di atas dan tinggalkan handoff bersih di PROGRESS.md.
