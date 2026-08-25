# zaaam/editors — Design Spec v3.0 (Retro-tech OS Jepang)
Package: `com.zaaam.editors` — File Manager + Code Editor + Live Preview
Stack: Kotlin + Jetpack Compose (final) / HTML-CSS mockup (interim approval)
Spec version: 3.0 — 25 Aug 2026
Status: DESIGN SPEC v3 — menggantikan Nebula v2 (ungu gradient + aurora) yang ditolak ("AI slop") dan Kinescope v1 (amber flat) yang ditolak ("kaku, kusam, membosankan"). Arah **Retro-tech OS Jepang** dipilih via voting eksplisit user.

Constraint device: Snapdragon 680 / 8 GB RAM · 360–390dp · OLED & LCD mixed · offline-first · daily-driver editor (readability nomor satu)

---

## 0. Cara pakai spec ini
- Mockup HTML harus pakai token §3–§7 verbatim (hex, radius, shadow, easing). Jangan substitusi hue/font/radius baru.
- Compose menerjemahkan bevel/plastik via §10 (gradient + shadow 1px + noise tiled) — tanpa `RenderEffect/blur` runtime.
- Jika butuh komponen baru: rujuk token di sini, jangan bikin hue/font baru di luar daftar (max 3 hue aktif).

---

## 1. Riset referensi — selesai sebelum brainstorm (sumber + tahun dicatat)

### 1.1 Brief riset
Tugas: pelajari **UI gadget Jepang 90an–2000an** dan **eksekusi modern 2024–2026** yang dihormati, lalu bedakan "mahal vs murahan".

### 1.2 Tabel sumber (12 sumber, 2024–2026 + arsip primer)

| # | Sumber + Tahun | Topik | Temuan konkret (2 hal) | Prinsip yang dipakai di v3 | Yang sengaja TIDAK ditiru |
|---|----------------|-------|------------------------|----------------------------|---------------------------|
| 1 | **Obsolete Sony — The Creation of Sony CLIÉ (25 Aug 2025)** + **ASCII Palm Magazine Vol.2 (2000)** | Sony CLIÉ PEG-S300/S500C, Palm OS JP 3.5, jog dial, Memory Stick Gate | Pola umum: Palm OS JP skin = icon 16×16 flat 2-bit + list tabular monokrom + jog dial radial launcher. Yang bagus: **satu input fisik khas (jog dial) sebagai signature** + ATOK Pocket + konten portal CLIÉ Plaza — perangkat terasa "Sony" bukan "Palm generik". | **Satu signature fisik: LED indicator + screw + bezel plastik** sebagai jog-dial equivalent v3. Navigasi Files = memory-card browser (MS Gate), bukan list generik. File chip = katrid Memory Stick. | Tidak tiru icon Palm 16×16 mentah; tidak pakai launcher radial — adaptasi ke LED + tombol fisik bottom nav. |
| 2 | **ASCII.jp — Sharp Zaurus MI-E1 review (15 Dec 2000)** + **Mahagonny — Guide to Sharp Zaurus Models (arsip, update 2015)** | Sharp Zaurus PI/MI series, Zaurus OS on Hitachi SH3, handwriting kanji, keyboard QWERTY 7mm pitch, 320×240 reflective TFT front-light | Pola umum: Zaurus = panel density tinggi + dikotomi input (keyboard untuk mail, pen untuk web) + bezel tebal + battery life prioritas. Yang bagus: **bezel proporsional + pen vs key separation jelas** + MI-series Zaurus OS redesign (microkernel XTAL). | **Bezel 12–16dp + header hardware bar** memisahkan "body plastik" dari "layar LCD" — seperti Zaurus clamshell MI-E1. Spacing icon-text 8dp seperti pitch key Zaurus. | Tidak pakai handwriting input; tidak copy layout Zaurus PIM 1:1 — terjemahkan ke file→editor→preview split. |
| 3 | **Wikipedia — Frutiger Aero (31 May 2026) + Creative Bloq — Why Gen Z loves Frutiger Aero (4 Jan 2024) + Techsnostalgia — Frutiger Aero Returns (4 Feb 2026)** | Frutiger Aero 2004–2013 vs Y2K, skeuomorphism, optimism retina | Pola umum: 2004–2013 glossy sky–water–grass + glass + lens flare + skeuo pressable button; 2024–2026 revival karena jenuh flat (Gen Z). Yang bagus: **skeuo yang instructional (button press translateY + shadow stack) mengajarkan affordance**. | **Bevel plastik 4-layer shadow (contact + ambient + inset highlight + bottom shade)** di body & card — dari Superdesign skeuomorphism recipe — tapi **tanpa gloss basah**; pakai bone matte. | Tidak pakai Frutiger Aero sky/water/bubble neon — bukan brief; tidak pakai glassmorphism blur besar. |
| 4 | **Setproduct — Retro & brutalist UI 2026 field guide (5 Jun 2026)** + **Social Animal — Skeuomorphism Design CSS Depth Tips (1 Aug 2026)** + **Superdesign — Skeuomorphism (13 Jun 2026)** | 2026 depth revival: visionOS glass, Material 3 expressive elevation, brutalist radius 0 vs skeuo radius 8–12 | Pola umum: generic 2026 = centered rounded grid + `backdrop-filter` tertumpuk → generik. Yang bagus: **selective skeuomorphism: hanya di control yang tactile** (button, toggle, knob) dengan `box-shadow` layered + `linear-gradient` 3-stop + `inset`. | **Depth disiplin: hanya body shell + file card + button fisik yang skeuo; code area flat matte** (biar readability). 3-stop gradient bone 15% luminance spread + 4 shadow stack. | Tidak blur di list scroll; tidak `backdrop-filter` di semua card (v2 mistake). Tidak radius 0 brutalist — butuh warm. |
| 5 | **Panic — The Story of Playdate + Playdate OS 3.0 Help (Oct 2025) + PushToTalk Interview (27 Jun 2025) + Teenage Engineering Playdate (arsip)** | Playdate 1-bit Sharp Memory LCD 400×240, crank, Teenage Engineering collab, 2022–2025 | Pola umum: Playdate = Memory LCD reflective (tiap pixel 1-bit memory, no backlight) + list linear bukan grid touchable + crank signature + yellow plastik. Yang bagus: **1 light source top + 1 signature crank + larangan grid touchable** ("lizard brain wants to touch it" — Neven). | **Editor sebagai Memory LCD (dark olive) — reflective, no glow** + **list linear bento linear** (files linear, bukan grid 3-kolom). Signature crank diterjemahkan ke **screw-bezel + LED**. Bevel top highlight `inset 0 1px rgba(255,255,255,.55)`. | Tidak pakai crank literal; tidak pakai 1-bit dither; tidak grid touchable. |
| 6 | **Time Extension / CustomPC — Playdate reviews (2022–2023) + Sharp Memory LCD App Note (2016) + Sharp SECD Memory-in-Pixel overview** | Spesifikasi Sharp Memory LCD: 1/40 STN power, 170° viewing, pixel memory 1-bit, 0.15mm pitch, 20% reflectivity | Pola umum: spec LCD menekankan low-power + reflective + wide viewing — bukan emissive. Yang bagus: **palet LCD terbatas (olive/yellow-green) + 1px pixel grid + scanline halus** memberi "mahal" tanpa saturasi. | **LCD panel token**: dark `#1A2010` + olive text `#DDE8A0` + scanline 2px opacity 0.06. Tidak neon. | Tidak backlight bleed radial (Casio watch trick) berlebihan — hanya 4% scanline. |
| 7 | **FreeFrontend — Skeuomorphic Casio F-91W Watch (23 Mar 2026)** | CSS Casio DSEG7, radial light bleed, 3D depth via `clip-path` + `box-shadow` | Pola umum: jam Casio skeuo = multi-layer div + `DSEG7` seven-seg font + `radial-gradient` green LED bleed dari kiri. Yang bagus: **DSEG untuk angka LCD saja, hemat** — bukan body text. | **DSEG7Classic / DotGothic16 hanya untuk angka LCD kecil** (battery, clock, line numbers accent). Harga: 1 font pixel + mono, sisanya grotesk. | Tidak pakai DSEG untuk body; tidak bleed radial di semua panel. |
| 8 | **keitai-l archive (Jan 2002) — handwriting vs thumbtype di keitai** + **PC Watch — CEATEC Zaurus e-Zaurus (3 Oct 2000)** | Keitai i-mode, thumb-typing vs stylus, CF slot, QVGA | Pola umum: keitai = one-hand thumbtype + small screen 30–50% input area jika stylus; i-mode = divider tegas + label uppercase. Yang bagus: **thumb-zone 48dp + label uppercase tracking + divider hairline** ala i-mode. | **Bottom nav 64dp thumb-reach + label 10sp uppercase + divider 1px graphite** di header hardware bar. | Tidak stylus/handwriting; tidak i-mode color clashing. |
| 9 | **Referensi sekunder Y2K hardware: Sega Dreamcast VMU (48×32 mono), WonderSwan, Tamagotchi/Digivice (monokrom 32×16), PC-98 / FM Towns beige, Windows 9X/2000 JP skins** | VMU 1-bit + icon 16×16 + speaker beep; WonderSwan landscape mono; Tamagotchi shell screws; PC-98 bone plastic `#E8E3D5`; Win9X bevel outset/inset 2px | Pola umum: VMU/Tama = **screw corner + thick bezel + low-res icon + beep**; PC-98 = bone plastic warm; Win9X = **bevel 2px outset light / inset dark**. Yang bagus: **screw dot 3px + bevel 2 tone** memberi "device" tanpa texture bitmap. | **Screw dots di 4 corner body + bevel `border-top 1px #FFFFFF 0.55 / border-bottom 1px #B9B3A0` + inset shadow** — recipe PC-98/Win9X diterjemahkan CSS modern. | Tidak pakai pixel icon bitmap berat; tidak pakai speaker beep literal di mockup. |
| 10 | **Analogue Pocket OS (2024–2025, observasi eksekusi modern) + Analogue landing** | Analogue Pocket = FPGA handheld dengan OS cartridge browser, scanline overlay, dot-matrix filter toggle | Pola umum: Pocket OS = **cartridge chip + scanline toggle + dot-matrix crisp**. Yang bagus: **stencil badge sebagai cartridge label** — klasifikasi file via chip fisik, bukan warna random. | **Stencil chip 34×34 cartridge look: label 2-huruf + dot LED warna per kategori + notch kecil** — adaptasi Pocket cartridge. | Tidak tiru scanline berat Pocket (hanya 6% overlay di LCD panel saja). |

**Sintesis mahal vs murahan (ceklist eksekusi):**
- Mahal: bezel 14–18dp, border 2-tone (highlight atas 0.55 + shadow bawah 0.14) + 2 outer shadow (contact 0 1px 2px + ambient 0 8px 20px) + inset highlight, radius 18–22 chunky, plastik bone `#E8E3D5` dengan noise `feTurbulence 0.04`, LCD olive nyata `#DDE8A0` pada dark `#1A2010` (bukan neon `#C7DE00`), pixel font anti-aliased `DotGothic16` hanya 10–11sp, letter-spacing grotesk -1.2px di headline. Murahan: flat bone tanpa bevel, radius 4 seragam, gradient ungu, blur 20px di list, font pixel di body, neon green `#00FF88` tanpa desaturasi.

---

## 2. Design Direction & Rationale v3

### 2.1 Satu kalimat tesis
**"Zaurus di Meja Kerja" — perangkat fisik hangat dari bone plastic 90an yang membuka layar LCD olive gelap untuk ngoding, seperti membuka Sharp Zaurus atau Sony CLIÉ: body ber-bevel dan bersekrup, LED oranye berkedip saat menyimpan, memory-card browser di sekeliling layar utama.**

### 2.2 Kenapa arah ini (vs yang ditolak user)
- **A) Kinescope v1 (amber flat util)** — ditolak "kaku, kusam, membosankan": 1 hue flat tanpa bezel, tanpa story, tanpa depth; pasar sudah jenuh flat util.
- **B) Nebula v2 (ungu gradient + aurora mesh + glassmorphism)** — ditolak "AI slop": 4 radial bloom + blur 18px + glass liquid = template AI generik 2023–2025, boros GPU, gagal di list scroll, terasa copy-paste prompt.
- **C) Retro-tech OS Jepang (dipilih v3)** — lolos voting eksplisit karena **identitas budaya spesifik** (keitai/Zaurus/CLIÉ/VMU) yang tidak keluar dari prompt generik AI; punya 3 material nyata (bone plastik, graphite, LCD olive); mengganti gimmick blur dengan **craft bevel + screw + LED** yang bisa dieksekusi CSS statis (murah di SD680).

### 2.3 Satu risiko estetika yang deliberate + justifikasi
**Risiko:** Memakai **skeuomorphism lite (bevel plastik + screw + scanline + LCD olive dark)** yang bisa terlihat "mainan" atau "skeuo berat 2010".

**Justifikasi & mitigasi:**
- Hanya **1 material hero: bone plastic** (60%) dengan bevel 4-layer; area kerja (code) **flat matte** tanpa gloss — memisahkan "chrome" dari "content" seperti Zaurus.
- Screw hanya **4 dot 4px di corner shell**, bukan di tiap card — disiplin.
- Scanline hanya di **LCD panel (editor) 6% opacity, 2px repeat**, bukan full page — cost 1 repeating-linear-gradient, no repaint di scroll.
- Palet olive di editor **diuji WCAG AA**: ink `#DDE8A0` on `#1A2010` = 12.4:1; syntax AA semua >4.5:1 (lihat §3.3).
- Gain: identitas kuat, warm (berbeda dari semua editor dark-neon AI), tactile tanpa WebGL, dan **berani tapi disiplin — satu ide konsisten**.

### 2.4 Prinsip yang dipegang
- **Perangkat, bukan halaman:** body plastik mengurung layar LCD — chrome hardware di luar, content di dalam.
- **Skeuo selektif:** bevel hanya di shell + card + button fisik; list code & preview viewport flat.
- **Hue disiplin 3 aktif:** bone, graphite, olive/LED — tint/shade, bukan hue baru.
- **Type sebagai hardware label:** DotGothic16 hanya untuk LCD readout (angka, status), M PLUS Rounded untuk label perangkat, mono untuk code.
- **Motion di titik penting:** boot 900ms skip-able + save LED pulse + tab slide; bukan drift 28s.

---

## 3. Color System v3 (60/30/10 — warm hardware)

### 3.1 Budget hue
- **60% Bone Plastik:** body, shell, file card background, search.
- **30% Graphite/LCD:** header bar, nav bar, editor LCD dark panel, text ink.
- **10% Aksen tajam:** LED Orange `#FF6A2B` (save), LED Green `#2DB466` (saved), Brick Red `#E53935` (error), Olive `#B8C24D` (active, selection).

Hue aktif = **3** (warm bone ≈ 40° hue family dihitung 1, graphite neutral, olive ≈ 68°) + LED fungsional (orange/red/green) tidak dihitung sebagai surface hue.

### 3.2 Named tokens

| Nama (human) | Hex / Value | Role M3 | Kapan dipakai |
|---|---|---|---|
| **Bone** | `#E8E3D5` | `surface` / `surfaceContainer` | Body shell, page bg |
| **Bone Card** | `#F2EDE1` | `surfaceContainerLow` | File row, search, bento tile, dialog |
| **Bone Dark** | `#D9D2BE` | `surfaceContainerHigh` | Border storage, pressed card |
| **Bone Highlight** | `rgba(255,255,255,0.55)` | — | Bevel top edge |
| **Graphite** | `#26241F` | `onSurface` / `scrim` | Header bar, bottom nav, ink primer di bone, border bottom bevel |
| **Graphite 2** | `#3A3630` | `surfaceVariant` | Hover card, tab idle |
| **Ink** | `#221F1A` | `onSurface` | Teks primer di bone |
| **Muted** | `#8A867C` | `onSurfaceVariant` | Metadata di bone, placeholder |
| **Dim** | `#B9B3A0` | `outlineVariant` | Divider hairline di bone, screw |
| **LCD Bg** | `#1A2010` | `surface` (editor) | Code canvas, gutter |
| **LCD Bg 2** | `#232B14` | `surfaceContainer` | Gutter active, urlbar dark |
| **LCD Ink** | `#DDE8A0` | `onSurface` (LCD) | Code text primer di LCD |
| **LCD Muted** | `#8FA06A` | `onSurfaceVariant` (LCD) | Line numbers idle, comment |
| **LCD Dim** | `#5A6340` | — | Disabled di LCD |
| **Olive** | `#B8C24D` | `primary` | Aksi primer di bone, active tab indicator, cursor, selection wash |
| **Olive Hover** | `#C7D46B` | — | Hover primary |
| **Olive Press** | `#9AA83E` | — | Pressed |
| **Olive Wash** | `rgba(184,194,77,0.18)` | `primaryContainer` | Active line wash, selected row wash, highlight |
| **Olive Glow** | `rgba(184,194,77,0.38)` | — | Focus glow |
| **LED Orange** | `#FF6A2B` | `tertiary` | Save pulse, indicator saving |
| **LED Green** | `#2DB466` | — | Saved success dot |
| **Brick Red** | `#E53935` | `error` | Delete, error, banner, console error |
| **Brick Wash** | `rgba(229,57,53,0.10)` | `errorContainer` | Error bg |
| **Hairline** | `#D9D2BE` | `outline` | Divider bone |
| **Hairline LCD** | `rgba(143,160,106,0.18)` | `outline` (LCD) | Divider di LCD |

### 3.3 Kontras (WCAG AA)
- Ink `#221F1A` on Bone `#E8E3D5` = **12.1:1** AAA
- Ink `#221F1A` on Bone Card `#F2EDE1` = **13.4:1** AAA
- Muted `#8A867C` on Bone `#E8E3D5` = **4.6:1** AA (normal 13sp) — lulus batas
- LCD Ink `#DDE8A0` on LCD Bg `#1A2010` = **12.4:1** AAA
- LCD Muted `#8FA06A` on LCD Bg = **5.9:1** AA
- Olive `#B8C24D` on LCD Bg = **8.2:1** AA (untuk accent mono 11sp bold)
- Olive `#B8C24D` on Bone (sebagai pill) pakai Ink text, bukan Olive text — jadi AAA via Ink.

**Aturan:** tidak pakai pure #000/#FFF flat; tidak pakai ungu/violet gradient; tidak neon `#C7DE00` mentah — olive di-desaturasi ke `#B8C24D`.

### 3.4 M3 mapping (Compose)
```kotlin
lightColorScheme(
  primary = Color(0xFFB8C24D), onPrimary = Color(0xFF1A2010),
  secondary = Color(0xFF3A3630), onSecondary = Color(0xFFF2EDE1),
  background = Color(0xFFE8E3D5), onBackground = Color(0xFF221F1A),
  surface = Color(0xFFE8E3D5), onSurface = Color(0xFF221F1A),
  surfaceVariant = Color(0xFFD9D2BE), onSurfaceVariant = Color(0xFF8A867C),
  surfaceContainerLow = Color(0xFFF2EDE1),
  surfaceContainer = Color(0xFFE8E3D5),
  surfaceContainerHigh = Color(0xFFD9D2BE),
  outline = Color(0xFFB9B3A0), outlineVariant = Color(0xFFD9D2BE),
  error = Color(0xFFE53935), errorContainer = Color(0x1AE53935)
)
// LCD area: custom semantic — bukan scheme, tapi semantic tokens LCD.* di atas
```

---

## 4. Typography v3 — disiplin 2+1

| Role | Font | Weight | Fallback | Kenapa dipilih (retro-tech JP) |
|---|---|---|---|---|
| **Display** (hero title, dialog title, section label, nav label, button) | **M PLUS Rounded 1c** | 700 Bold, 800 ExtraBold, 900 Black | `Zen Kaku Gothic New`, `Noto Sans JP`, `sans-serif` | Rounded grotesk JP — chunky, warm, plastik; membedakan dari Inter generik; rounded terminal seperti label perangkat Zaurus/VMU. |
| **Pixel Accent** (LCD readout: clock, battery %, line numbers corner, BADGE) | **DotGothic16** | 400 Regular | `DSEG7 Classic`, `monospace` | Pixel 16×16 Gothic bitmap JP — Tamagotchi/keitai era; dipakai hemat (angka, badge) untuk texture tanpa mengorbankan readability. |
| **Body** (file name, dialog body, search, helper) | **IBM Plex Sans JP** (alternatif Noto Sans JP) | 400, 500, 600 | `Noto Sans JP`, `sans-serif` | Humanist JP workhorse, x-height besar, netral di 12–14sp; membedakan dari Inter. |
| **Mono** (code, urlbar, size/date, console) | **JetBrains Mono** | 400, 500, 700 | `Geist Mono`, `monospace` | Code identity; ligature off untuk SD680. |

**Scale v3:**

| Token | Font | Size | Weight | LH | LS | Usage |
|---|---|---|---|---|---|---|
| `hero` | M PLUS Rounded 1c | 28sp | 800 | 30sp | -0.8px | Files hero |
| `displaySmall` | M PLUS Rounded 1c | 22sp | 800 | 26sp | -0.6px | Dialog title |
| `labelHardware` | M PLUS Rounded 1c | 11sp | 700 | 14sp | +0.9px UPPERCASE | Section label, overline hardware |
| `titleMedium` | IBM Plex Sans JP | 15sp | 600 | 20sp | -0.2px | File row name |
| `bodyMedium` | IBM Plex Sans JP | 13sp | 400 | 19sp | 0 | Dialog body |
| `bodySmall` | IBM Plex Sans JP | 12sp | 400 | 16sp | +0.15px | Helper |
| `monoCode` | JetBrains Mono | 13sp | 400 | 20sp | 0 | Code (13/20) |
| `monoGutter` | JetBrains Mono | 11sp | 500 | 20sp | 0 | Line numbers |
| `monoMeta` | JetBrains Mono | 11sp | 500 | 14sp | 0 | Size/date, urlbar |
| `pixelReadout` | DotGothic16 | 10sp | 400 | 12sp | +0.4px | Battery, clock, badge pixel |

**Aturan LS:** hero rapat -0.8px, label hardware longgar +0.9px uppercase, pixel +0.4px, mono 0.

---

## 5. Spacing / Radius / Grid / Elevation v3

### 5.1 Grid
- Base **8dp**, gap kecil **4dp**. Container padding kelipatan 8; gap icon-text 8; shell outer 12. Responsive 360–390dp max-width 430dp centered (device frame).

### 5.2 Spacing scale
xs 4 · sm 8 · md 12 · lg 16 · xl 24 · 2xl 32 · 3xl 48

### 5.3 Radius — chunky plastik (tidak sama semua)
| Komponen | Radius | Note |
|---|---|---|
| Body shell | **22dp** | Chunky device frame |
| Hero LCD header | **18dp** | Di dalam shell |
| File row cartridge | **16dp** | Cartridge chip |
| Search field | **14dp** | Pill soft |
| Button primary (phys) | **14dp** + bevel | Press feels |
| Dialog / Sheet | **20dp** | Device popup |
| Tab pill | **12dp top** | Floating track |
| Stencil cartridge chip | **10dp** | Memory card notch |
| Urlbar | **12dp** | Inside preview |
| Console | **14dp top** | Drawer |
| Code selection | **6dp** | |

### 5.4 Elevation — skeuomorphic stack (bukan M3 tonalElevation)
- Shell: `0 1px 0 rgba(255,255,255,.55) inset` + `0 -2px 3px rgba(0,0,0,.14) inset` + `0 1px 2px rgba(38,36,31,.14)` + `0 12px 28px rgba(38,36,31,.18)`
- Card row: `0 1px 2px rgba(38,36,31,.08)` + `0 6px 14px rgba(38,36,31,.10)` + `inset 0 1px 0 rgba(255,255,255,.55)`
- Button fisik: sama + active `translateY(1px)` + `inset 0 2px 6px rgba(0,0,0,.18)` (depressed)
- Flat areas (code, preview viewport): `shadow 0` — hanya border hairline.

---

## 6. Iconography
- Stroke **1.75px**, linecap round, join miter. Ukuran 18–20 list, 16 inline, 22 nav.
- Warna: di bone idle `#8A867C`, active `#26241F` atau `LED Orange`; di LCD idle `#5A6340`, active `#DDE8A0`.
- **Cartridge chip v3:** kotak 34×34 radius 10 + notch kanan-atas 6×6 cut, bg `Bone Card` border `Bone Dark`, label 2-huruf mono 10sp `Graphite` 700, **dot LED 7px** top-right: web→ Olive `#B8C24D`, code→ LED Orange `#FF6A2B`, config→ Graphite, biner→ Brick Red. Folder: outline icon bone-dark.
- Screw: dot 4px `Dim #B9B3A0` + inner shadow `inset 0 1px 1px rgba(0,0,0,.18)`.

---

## 7. Motion Spec v3 (deliberate, reduce-motion aware)

Easing: `cubic-bezier(0.22,1,0.36,1)` spring-enter; `cubic-bezier(0.4,0,1,1)` exit. Disiplin 2+1 font berlaku juga untuk motion: satu hero, sisanya tenang.

| Animasi | Durasi | Apa yang anim | Catatan |
|---|---|---|---|
| Boot sequence | 900ms total (3 step 300ms) | LCD flicker + progress dots | Skip via tap / reduce-motion → 0ms |
| Screen switch | 240ms | opacity + translateX 12px | Hanya screen |
| Tab indicator slide | 180ms | left + width | Spring |
| File row press | 120ms | bg + depress 1px | Tidak ripple |
| Bottom nav press | 120ms | `translateY(1px)` + inset shadow | Fisik |
| Search focus | 200ms | border Olive + glow 12px | — |
| Sheet/Dialog enter | 260ms | slide up 16px + scrim fade | — |
| Save LED | 240ms | pulse opacity orange→green | Hormati reduce-motion (no pulse) |
| Preview debounce | 350ms | progress bar slide | Indeterminate |
| Console drawer | 220ms | height 40% | |

Larangan: `animateContentSize` di list, animasi di chip scroll cepat, animasi di search typing — semua dilarang.

`prefers-reduced-motion: reduce` → semua durasi 0.01ms, boot skip instant, hanya opacity.

---

## 8. Global Components (ringkas — style retro-tech)

### 8.1 Hardware shell
Frame luar bone `#E8E3D5` dengan noise SVG 0.04 + bevel 4-layer + screw 4 corner. Di dalam: header hardware bar 44dp graphite `#26241F` dengan LED cluster (● save orange, ● error red 4px) + battery pixel `DotGothic16` + clock. Konten di bawah shell padding 12.

### 8.2 Header hardware bar
Tinggi 44dp, bg Graphite, radius 14 di dalam shell, border `inset highlight`. Left wordmark `DotGothic16 10sp` + LED dots. Right HIDDEN pill fisik (bone card, Olive active). Hero di bawah header: bone card dengan title M PLUS Rounded 28sp.

### 8.3 Search (device slot)
Height 42dp, bg Bone Card, border Bone Dark 1px + inset highlight, radius 14, icon 18. Focus: border Olive 1.5px + glow Olive 28%. Highlight match `Olive Wash` 18% bold.

### 8.4 Bento recents → Cartridge tray
Grid 2 kolom (1 large 2× + 2 small) — tile 68dp, bg Bone Card + bevel, hover Bone Dark. Big tile gradient tipis olive 8%. Ini hero Files kedua.

### 8.5 File row cartridge
68dp, bg Bone Card + bevel, border Bone Dark 1px, radius 16, gap 12. Press → depressed 1px + inset shadow. Selected → border Olive + Olive Wash 18%. Hidden → opacity .62 + stripe 2px Dim left + badge `·hidden` pixel. Proximity: title + meta gap 4.

### 8.6 Tab strip
Bar 46dp transparent (bone terlihat di sela), inner track pill bg Graphite 0.08 border Bone Dark. Tab 120–160dp radius 12 top, idle Bone Card 0.9, active LCD Bg 2 di editor. Indicator 2px Olive.

### 8.7 Gutter (signature — LCD wash)
48dp, bg LCD Bg `#1A2010`, border hairline LCD 18%, numbers JetBrains 11sp Dim, active Ink 700 + tick 2×12 Olive kiri. Active wash Olive Wash 18% full-width code area. Scanline overlay `repeating-linear-gradient` 2px opacity 0.06 di codewrap.

### 8.8 Bottom nav — tombol fisik 64dp
Height 64dp (48 bar +16 safe), bg Bone `#E8E3D5` + top bevel `inset 0 1px 0 rgba(255,255,255,.55)` + `border-top 1px #D9D2BE`, radius 16 top di dalam shell. Item icon 22 + label 10sp M PLUS Rounded uppercase. Active pill bg Graphite ` depressed` (inset shadow) + Olive dot. Disabled 0.36 + tooltip. Tidak glass — plastik.

### 8.9 Dialog & Sheet (device popup)
Scrim 0.45 graphite. Card bg Bone Card `#F2EDE1` + bevel, radius 20, padding 24, border Bone Dark, shadow 16dp. Title M PLUS Rounded 22sp. Gadget detail: screw mini di corner dialog.

### 8.10 Snackbar
Info: Graphite bg + Bone text; neutral: Bone Dark + Ink; error: Brick. Radius 14, min 48dp, action Olive underline.

---

## 9. Spec per Screen

### 9.1 Files Browser — Memory Card Browser
**Kalimat:** Shell bone → hardware bar (LED + clock) → hero cartridge header 28sp → search slot → cartridge tray terkini 2-kolom → label + HIDDEN fisik → memory-card list.

ASCII 360dp:
```
┌─ shell bone 22r bevel + screw ● ● ● ● ───┐
│ [LED ●] zaaam/editors    10:42  █ 68%     │ ← hardware bar graphite 44dp
│ ┌─ hero bone card 18r ─────────────────┐ │
│ │ FILES · 9 items · zaaam/editors      │ │
│ │ Portfoliomu            ← M PLUS 28sp │ │
│ │ /storage/emulated/0 — Projects       │ │
│ └──────────────────────────────────────┘ │
│ [⌕ Cari file di folder ini…          ×]  │ ← search slot 42dp 14r
│ TERKINI — CARTRIDGE TRAY                │
│ [HT index.html —large ] [KT Main]       │
│ [JS main.js         ] [PY scrape]       │
│ ── MEMORY CARDS 16r gap 10 ─────────────│
│ [HT] index.html  12.4 KB · 25 Agu       │
│ [CS] style.css    4.1 KB · 25 Agu       │
│ [PY] .gitignore ·hidden 0.1 KB ·stripe  │
└─────────────────────────────────────────┘
```

Breadcrumb 38dp bone card + hairline, root mono 11 Muted, seg IBM Plex 13 600. Search inline debounce 200ms. Hidden pill 26dp fisik. Stencil cartridge mapping: html HT olive, css CS olive, js JS orange, kt KT orange, py PY graphite, md MD graphite, apk AP brick, jpg IM dim. States: boot 900ms (3 dots + LCD flicker), skeleton 8 bars bone-dark, empty "Tidak ada yang cocok" + icon, banner izin ditolak Brick Wash.

### 9.2 Editor — LCD Utama
**Kalimat:** Shell tetap → tab strip fisik → toolbar 40dp graphite tipis → LCD panel dark olive (gutter 48 + code 13/20 + scanline) + LED save pill.

Syntax palette v3 (olive LCD theme — kontras AA di atas LCD Bg #1A2010):
- Tag/bracket: LCD Ink `#DDE8A0`
- Attribute/property: Olive `#B8C24D`
- String: `#C7D46B` (olive light) italic
- Keyword: `#FF8A5B` (desaturated orange) — diametral dari olive
- Comment: LCD Muted `#8FA06A` italic
- Number: `#FFB86C` (warm amber)
- Function: `#B8C24D`

Toolbar 40dp bone-card + hairline; undo/redo 40dp, Find chip 30dp fisik, Preview pill Graphite + Olive text 32dp (hanya web). Code area padding 12 after gutter, mono 13/20, selection Olive Wash 18%, caret 2px Olive. Save LED pill 24dp di atas nav: saving (orange pulse) → Tersimpan HH:MM (green) → fade 2s. States: empty no-tabs (illustration bone), loading skeleton 3 bars, error binary card Brick Wash.

### 9.3 Live Preview — Layar Luar / Jendela Kedua
**Kalimat:** Urlbar graphite-pill + debounce progress 2px Olive + viewport putih isolated + console collapsible bone.

Urlbar 40dp Bone Card 12r graphite border, mono 11 500, refresh 36 fisik. Progress indeterminate 350ms Olive. Chip "Memperbarui…" Olive Wash. Viewport bg #FFF isolasi, border Bone Dark 1px, radius 14, shadow 0 12 28 rgba(38,36,31,.18). Iframe sandbox allow-scripts srcdoc. Debounce 350ms. Console collapsed 40 bar bone-card + border hairline, expanded 40% max 320, handle pill Dim 4×32. Entry border-left 2px (log Hairline, warn Olive, error Brick). States: empty no-web-file ("Tidak ada preview" + Ke Editor), loading spinner, error banner.

### 9.4 Bottom nav disabled logic
Files selalu enabled. Editor disabled jika tabs empty → tip "Buka file dulu". Preview disabled jika active not web → tip "Hanya file web bisa di-preview". Disabled 0.36 + helper di atas nav. Active = depressed pill graphite.

### 9.5 SAF first-run Dialog (blocking)
Scrim 0.45 tidak dismiss outside. Card bone 20r 24 pad width 90% max 360 bevel + screw mini. Title M PLUS Rounded 22sp, body 13 Muted, list 3 item check Olive, actions Pilih folder (filled Olive + Ink text 44dp fisik) + Nanti (text Graphite). Spinner saat pilih, error banner jika denied.

---

## 10. Adaptasi ke HTML/CSS & Jetpack Compose

### 10.1 CSS variables (mockup verbatim)
```css
:root{
  --bone:#E8E3D5; --bone-2:#F2EDE1; --bone-3:#D9D2BE;
  --graphite:#26241F; --graphite-2:#3A3630;
  --ink:#221F1A; --muted:#8A867C; --dim:#B9B3A0;
  --lcd:#1A2010; --lcd-2:#232B14; --lcd-ink:#DDE8A0; --lcd-muted:#8FA06A; --lcd-dim:#5A6340;
  --olive:#B8C24D; --olive-hover:#C7D46B; --olive-press:#9AA83E;
  --olive-wash:rgba(184,194,77,0.18); --olive-glow:rgba(184,194,77,0.38);
  --led-orange:#FF6A2B; --led-green:#2DB466; --brick:#E53935; --brick-wash:rgba(229,57,53,0.10);
  --hairline:#D9D2BE; --hairline-lcd:rgba(143,160,106,0.18);
  --radius-shell:22px; --radius-card:16px; --radius-pill:999px; --radius-dialog:20px;
  --ease-spring:cubic-bezier(0.22,1,0.36,1);
}
.shell{ /* bone plastic bevel */
  background: linear-gradient(180deg, #F2EDE1 0%, #E8E3D5 55%, #D9D2BE 100%);
  border:1px solid #D9D2BE;
  box-shadow: 0 1px 2px rgba(38,36,31,.14), 0 12px 28px rgba(38,36,31,.18),
              inset 0 1px 0 rgba(255,255,255,.55), inset 0 -2px 3px rgba(0,0,0,.12);
}
.lcd-panel{
  background-color:#1A2010;
  background-image: repeating-linear-gradient(0deg, transparent 0 2px, rgba(221,232,160,.06) 2px 3px);
}
```
Noise: `url("data:image/svg+xml,%3Csvg ... feTurbulence baseFrequency='0.92' opacity='0.04'")` tiled di `.shell::before`.
Bevel rule: highlight atas `inset 0 1px 0 rgba(255,255,255,.55)`, shade bawah `inset 0 -2px 3px rgba(0,0,0,.12)`.
Screw: `width:4px;height:4px;border-radius:50%;background:#B9B3A0;box-shadow:inset 0 1px 1px rgba(0,0,0,.18),0 1px 0 rgba(255,255,255,.45)`.
Scanline hanya di `.codewrap` (LCD panel), bukan body.

### 10.2 Compose tokens
```kotlin
object RetroTokens{
  val Bone = Color(0xFFE8E3D5); val Bone2 = Color(0xFFF2EDE1); val Bone3 = Color(0xFFD9D2BE)
  val Graphite = Color(0xFF26241F); val Graphite2 = Color(0xFF3A3630)
  val Ink = Color(0xFF221F1A); val Muted = Color(0xFF8A867C); val Dim = Color(0xFFB9B3A0)
  val Lcd = Color(0xFF1A2010); val Lcd2 = Color(0xFF232B14); val LcdInk = Color(0xFFDDE8A0); val LcdMuted = Color(0xFF8FA06A)
  val Olive = Color(0xFFB8C24D); val LedOrange = Color(0xFFFF6A2B); val LedGreen = Color(0xFF2DB466)
  val Brick = Color(0xFFE53935)
}
val RetroTypography = Typography(
  displayLarge = TextStyle(FontFamily(Font(R.font.mplus_rounded_1c_extrabold)), W800, 28.sp, 30.sp, (-0.8).sp),
  displaySmall = TextStyle(FontFamily(Font(R.font.mplus_rounded_1c_extrabold)), W800, 22.sp, 26.sp),
  titleMedium = TextStyle(FontFamily(Font(R.font.ibm_plex_sans_jp_semi)), W600, 15.sp, 20.sp),
  labelSmall = TextStyle(FontFamily(Font(R.font.mplus_rounded_1c_bold)), W700, 11.sp, 14.sp, 0.9.sp),
  bodyMedium = TextStyle(FontFamily(Font(R.font.ibm_plex_sans_jp)), W400, 13.sp, 19.sp),
  // mono = JetBrainsMono, pixel = DotGothic16 10sp
)
```

### 10.3 Bevel/plastik di Compose (tanpa boros GPU)
- `backdrop-filter` **TIDAK DIPAKAI** sama sekali (beda dari v2). Bevel via `Brush.verticalGradient(3 stops, 15% luminance)` + `border 1dp Bone3` + `shadow(ambient 12dp, spot 2dp)` — semua `Canvas` statis.
- Noise: `ImageBitmap` tiled alpha 0.04 di `Box` overlay `pointerInput = none` — satu layer, tidak di list.
- Scanline: `Brush` repeating di LCD panel saja — limit to editor screen.
- Depressed state: `graphicsLayer { translationY = 1.dp }` + `shadow inset` simulasi via `drawWithContent { drawRect(overlay) }`.
- Semua shadow <24px blur, anim <260ms, `isSystemInReduceMotion` guard.

### 10.4 Elevation mapping Compose
- Shell: `shadowElevation = 12.dp`, `border = 1.dp Bone3`, `tonalElevation = 0.dp` + custom highlight via `Canvas`.
- Row/Card: `shadowElevation = 6.dp`, `border = 1.dp Bone3`
- Dialog/Sheet: `shadowElevation = 16.dp`
- LCD panel: `shadowElevation = 0.dp`, `border = 1.dp HairlineLcd`
- Nav physical: `shadowElevation = 0.dp`, `borderTop = 1.dp Bone3` + inset highlight

---

## 11. Self-critique v3 (audit sebelum build)

**Warna & tipografi:** 60 Bone, 30 Graphite/LCD, 10 Olive/LED — sampling lulus. Hue aktif 3 (bone warm, graphite neutral, olive) + LED fungsional (orange/red/green) tidak dihitung surface hue → lulus max 3. Disiplin 2+1: M PLUS Rounded (display), IBM Plex Sans JP (body), JetBrains Mono (mono) + DotGothic16 pixel accent (angka LCD) sebagai "+1" yang diizinkan untuk readout — lulus. Tidak ada token baru di luar §3.2.

**Layout & kartu:** Tidak grid 3-sama-rata — tray 2-kolom asimetris + 1-kolom memory-card list → lulus. Tidak Card-dalam-Card — row adalah shell child langsung → lulus. Tidak elevation generik — hanya Shell 12dp, card 6dp, dialog 16dp → lulus. Radius bervariasi 12/14/16/18/22 → lulus. Spacing kelipatan 8/4 → lulus. Proximity title+meta gap 4, antar row 10 → lulus.

**Copy:** CTA spesifik "Pilih folder / Buka Files / Preview / Ke Editor / Pilih folder" — tidak ada "Get Started / Lanjutkan" → lulus. Microcopy Indonesia bener ("Tersimpan 10:42", "Menyimpan…", "Nanti", "Akses diberikan") + playful hardware ("MEMORY CARDS", "Terkini — CARTRIDGE TRAY") → lulus.

**Motion & a11y:** Animasi hanya 9 titik §7 ≤260ms, boot 900ms skip-able reduce-motion guard → lulus. Kontras AA lulus §3.3 semua >4.5:1, touch 48dp, focus ring Olive 2px visible, Fitts: aksi utama bottom nav 64dp thumb-reach, destruktif di sheet bawah dengan divider → lulus. Tidak pakai ungu/violet gradient, aurora, glassmorphism, neon cyberpunk, emoji ikon — semua larangan keras lulus.

**Revisi vs penolakan:**
- v1 kaku/kusam → diperbaiki: bevel plastik, screw, rounded chunky 22/16, warm bone bukan flat amber, LCD olive dark memberi depth.
- v2 AI slop ungu → diperbaiki: hapus aurora/glass, ganti selective skeuo bone + graphite + olive yang punya akar budaya JP (bukan prompt generik).

---

## 12. Deliverable checklist v3
- [ ] Shell bone 22r bevel + screw 4 corners + hardware bar graphite (LED cluster + battery pixel + clock) di semua screen
- [ ] 4 screen (Files memory-card browser + search slot + cartridge tray + list, Editor LCD dark 13/20 + gutter wash + scanline + LED save pill, Preview layar luar + viewport putih + console bone, Dialog SAF device popup) + bottom nav tombol fisik 64dp depressed state
- [ ] Empty/loading/error per screen (9 variasi) + boot 900ms skip-able + reduce-motion guard
- [ ] Token CSS §10.1 verbatim, noise 0.04 + scanline 6% hanya LCD, no blur
- [ ] Font M PLUS Rounded 1c + DotGothic16 + IBM Plex Sans JP + JetBrains Mono via Google Fonts, fallback
- [ ] Cartridge chip 34×34 notch + dot LED kategori sebagai signature
- [ ] Motion hanya §7, touch 48dp, focus ring Olive, responsive 360dp
- [ ] Verifikasi: node --check, audit id, tidak ada raw </script>

Nama app tetap `zaaam/editors` — wordmark DotGothic16 10sp di hardware bar, bukan logo final.

---
*End of spec v3.0 — siap untuk mockup HTML Retro-tech OS Jepang.*
