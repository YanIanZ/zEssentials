op# zEssentials 1.1.0.0 — Checklist Pengujian Lengkap

> Server: Paper / fork berbasis 26.2 • Java 21+
> Gunakan jar `target/zEssentials-1.1.0.0.jar`
> Laporkan bug: section nomor + console snippet + langkah reproduksi

---

## 0. Persiapan

- [ ] Hapus SEMUA jar lama `zEssentials*.jar` dari `plugins/`
- [ ] Salin `target/zEssentials-1.1.0.0.jar` ke `plugins/`
- [ ] Hapus `plugins/zMenu-*.jar` & `plugins/PlaceholderAPI-*.jar` (test auto-install) ATAU biarkan (skip S1–S3)
- [ ] Hapus folder `plugins/zEssentials` untuk fresh install ATAU backup & biarkan (test config update)

---

## 1. Auto Install & Startup

| #   | Aksi                                                                       | Ekspektasi                                                                                                                                                                                                                                |
|-----|----------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1.1 | Start TANPA zMenu & PAPI                                                   | Console: "Missing required plugins... trying to install them" → PAPI download dari Hangar & **enable otomatis tanpa restart**; zMenu ter-download dari Modrinth ke `plugins/` tapi **warning "cannot be hot-loaded"** (Paper restriction) |
| 1.2 | `dependency-loader.auto-restart: true` di `plugins/zEssentials/config.yml` | Setelah install, server shutdown sendiri + broadcast countdown 10s                                                                                                                                                                        |
| 1.3 | Start kedua                                                                | zMenu load normal dari `plugins/zMenu-*.jar`; tidak ada download ulang                                                                                                                                                                    |
| 1.4 | Start ketiga                                                               | Banner gradient `zEssentials ◆ 1.1.0.0 » ✔ enabled`; jumlah commands & modules tampil; tidak ada ERROR/WARN                                                                                                                               |
| 1.5 | `/essentials`                                                              | Command utama terbuka; alias `/ess` & `/zessentials` tetap jalan                                                                                                                                                                          |
| 1.6 | Stop server                                                                | Banner "✘ disabled" tanpa NPE/crash                                                                                                                                                                                                       |

---

## 2. Effects Module

| #   | Aksi                                 | Ekspektasi                                                                                         |
|-----|--------------------------------------|----------------------------------------------------------------------------------------------------|
| 2.1 | `/tpa <p>` → target accept           | Ring portal di titik asal & tujuan + suara enderman                                                |
| 2.2 | `/rtp`                               | Ring portal + suara                                                                                |
| 2.3 | `/warp <name>`                       | Ring portal + suara                                                                                |
| 2.4 | `/spawn`                             | Ring portal + suara                                                                                |
| 2.5 | `/gamemode creative` lalu `survival` | Burst HAPPY_VILLAGER + levelup sound                                                               |
| 2.6 | `/fly on` lalu `/fly off`            | Burst CLOUD                                                                                        |
| 2.7 | `/heal <p>`                          | Sparkle TOTEM_OF_UNDYING + levelup                                                                 |
| 2.8 | `/god <p>` (on saja)                 | Sparkle totem                                                                                      |
| 2.9 | Console log startup                  | Line `Effects loaded: teleport=PORTAL gamemode=HAPPY_VILLAGER fly=CLOUD blessing=TOTEM_OF_UNDYING` |

---

## 3. Terms of Service

| #    | Aksi                           | Ekspektasi                                                              |
|------|--------------------------------|-------------------------------------------------------------------------|
| 3.1  | Join dengan player baru        | ±0.3s muncul **dialog Mojang asli** (bukan chest) berisi rules + tombol |
| 3.2  | ESC untuk tutup dialog         | Dialog terbuka ulang otomatis (masih pending)                           |
| 3.3  | Ketik command selain `/terms*` | Diblokir + pesan "Accept the terms first"                               |
| 3.4  | Ketik pesan di chat            | Diblokir                                                                |
| 3.5  | Klik tombol **ACCEPT**         | Dialog tertutup, pesan "Terms accepted"; tersimpan permanen             |
| 3.6  | Rejoin setelah accept          | Tidak ada dialog lagi                                                   |
| 3.7  | Klik tombol **REFUSE**         | Kick dengan layar terms refuse (hex color bekerja)                      |
| 3.8  | Diam > timeout (default 60s)   | Kick dengan layar timeout                                               |
| 3.9  | `/terms reset <p>` (console)   | Reset; join berikutnya dapat dialog lagi                                |
| 3.10 | `/terms reload`                | Config di-reload tanpa restart                                          |

---

## 4. Freeze / Unfreeze

| #   | Aksi                                          | Ekspektasi                                                                                                                                  |
|-----|-----------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| 4.1 | `/freeze <p>`                                 | Target: TIDAK bisa gerak (kepala boleh diputar), glow biru, partikel salju mengorbit (3 level), walk+fly speed = 0, chat & command diblokir |
| 4.2 | `/freeze <p>` (ulang)                         | Idempotent — tetap frozen, tidak toggle                                                                                                     |
| 4.3 | `/unfreeze <p>`                               | Bebas gerak, glow hilang, partikel berhenti, walk 0.2 / fly 0.1 dipulihkan                                                                  |
| 4.4 | Setelah unfreeze: `/fly on` → jalan & terbang | **Regression test**: bebas gerak, tidak stuck                                                                                               |
| 4.5 | Reconnect (persist=false default)             | Auto-release, tidak stuck                                                                                                                   |
| 4.6 | GUI sanction tombol freeze                    | Toggle behavior tetap bekerja                                                                                                               |

---

## 5. Effects — Regression

| #   | Aksi                                                     | Ekspektasi                                      |
|-----|----------------------------------------------------------|-------------------------------------------------|
| 5.1 | Console startup: tidak ada "An error with loading field" | Semua field @NonLoadable bekerja                |
| 5.2 | `/fly add <p> 1d12h30m`                                  | Durasi terakumulasi: 1 hari + 12 jam + 30 menit |
| 5.3 | `/fly add <p> 10d`                                       | 10 hari ditambahkan                             |
| 5.4 | `/fly info`                                              | Sisa waktu terformat benar                      |
| 5.5 | `/eco give <p> 1k`                                       | Saldo +1,000                                    |
| 5.6 | `/eco give <p> 1.5m`                                     | Saldo +1,500,000                                |
| 5.7 | `/eco give <p> 2b`                                       | Saldo +2,000,000,000                            |

---

## 6. Chat Module v2

### 6a. Display Keywords

| #    | Ketik di chat              | Ekspektasi                                                          |
|------|----------------------------|---------------------------------------------------------------------|
| 6.1  | `[item]` atau `[i]`        | Nama item ×jumlah, hover tooltip item, klik menjalankan `/showitem` |
| 6.2  | `[inv]` atau `[inventory]` | Badge [INV]; hover = semua item inventory; klik = copy ke clipboard |
| 6.3  | `[ender]` atau `[ec]`      | Badge [ENDER CHEST]; hover = isi ender chest                        |
| 6.4  | `[pos]` atau `[position]`  | `[x, y, z]`; klik suggest `/tp x y z`                               |
| 6.5  | `[health]`                 | `❤ x/y`                                                             |
| 6.6  | `[food]`                   | `🍖 x/20`                                                           |
| 6.7  | `[money]`                  | Saldo terformat                                                     |
| 6.8  | `[coins]`                  | Saldo coins terformat                                               |
| 6.9  | `[store]`                  | Klik open_url; warna sesuai config                                  |
| 6.10 | `[ip]`                     | Text server address                                                 |

### 6b. Mention

| #    | Aksi                                | Ekspektasi                                                               |
|------|-------------------------------------|--------------------------------------------------------------------------|
| 6.11 | A ketik `hai @B`                    | Di layar B: `@B` gold bold + hover "Someone mentioned you!"; sound pling |
| 6.12 | Di layar A: hover `@B`              | Hover "Click to message them"                                            |
| 6.13 | Klik `@B` di layar A                | Suggest `/msg B ` di chat box                                            |
| 6.14 | B punya `/dnd` aktif → A ketik `@B` | Tidak ada sound untuk B, highlight tetap ada                             |

### 6c. Emoji & Slowmode

| #    | Aksi                                           | Ekspektasi                          |
|------|------------------------------------------------|-------------------------------------|
| 6.15 | Ketik `:heart:`                                | Berubah jadi ❤                      |
| 6.16 | Ketik `:100:`                                  | Berubah jadi 💯                     |
| 6.17 | `/chatslowmode 5` → spam 2 pesan               | Pesan ke-2 ditolak dengan countdown |
| 6.18 | `/chatslowmode 0`                              | Slowmode mati                       |
| 6.19 | Staff dengan `essentials.chat.bypass.slowmode` | Tidak terkena slowmode              |

### 6d. Message Deletion

| #    | Aksi                           | Ekspektasi                                                            |
|------|--------------------------------|-----------------------------------------------------------------------|
| 6.20 | `/chathistory <p>` (moderator) | Tiap baris ada tombol ✖ merah                                         |
| 6.21 | Klik ✖                         | Pesan hilang dari DB & list, page re-render dengan "resolved" message |

### 6e. DND

| #    | Aksi                    | Ekspektasi                                |
|------|-------------------------|-------------------------------------------|
| 6.22 | `/dnd`                  | "Do not disturb enabled"                  |
| 6.23 | Orang lain mention Anda | Highlight tetap ada, TAPI tidak ada sound |
| 6.24 | `/dnd` lagi             | "Do not disturb disabled"                 |

---

## 7. Chat Customization

### 7a. `/chatcolor`

| #   | Aksi                          | Ekspektasi                                                               |
|-----|-------------------------------|--------------------------------------------------------------------------|
| 7.1 | `/chatcolor`                  | GUI 45 slot terbuka: 16 warna di 2 baris + RESET + BOLD + ITALIC + CLOSE |
| 7.2 | Klik warna (mis. Gold)        | Chat color tersimpan; pesan konfirmasi; berlaku di chat berikutnya       |
| 7.3 | Klik BOLD                     | Toggle bold ON/OFF (glint saat ON); berlaku di chat                      |
| 7.4 | Klik ITALIC                   | Toggle italic ON/OFF; berlaku di chat                                    |
| 7.5 | Klik RESET                    | Semua kembali default                                                    |
| 7.6 | Player tanpa perm decorations | Slot BOLD/ITALIC tidak berfungsi                                         |

### 7b. `/tags`

| #    | Aksi                  | Ekspektasi                                                              |
|------|-----------------------|-------------------------------------------------------------------------|
| 7.7  | `/tags`               | GUI tag terbuka; tag "None" di depan; tag terkunci tampil gray dye      |
| 7.8  | Klik tag (mis. ✦ PRO) | Tag tersimpan; pesan konfirmasi; muncul sebelum rank di chat berikutnya |
| 7.9  | Klik "None"           | Tag dihapus                                                             |
| 7.10 | Rejoin                | Tag masih dipakai (persisted)                                           |

### 7c. Regresi

| #    | Aksi                                     | Ekspektasi                                                                        |
|------|------------------------------------------|-----------------------------------------------------------------------------------|
| 7.11 | Nickname aktif + tag aktif + color aktif | Ketiga fitur bekerja BERSAMAAN di chat: `[tag] [rank] NickName: <colored>message` |

---

## 8. Chat Bubbles

| #   | Aksi                                   | Ekspektasi                                                                     |
|-----|----------------------------------------|--------------------------------------------------------------------------------|
| 8.1 | Ketik pesan di chat                    | Bubble muncul di atas kepala, mengikuti gerakan player (passenger mount)       |
| 8.2 | Jalan/lari/terbang dengan bubble aktif | Bubble mengikuti mulus, TIDAK tertinggal, TIDAK error console                  |
| 8.3 | Kirim 3 pesan berturut-turut           | Bubble tumpuk: pesan TERBARU di bawah (dekat kepala), yang LAMA terdorong NAIK |
| 8.4 | Tunggu `duration-seconds` (default 6s) | Bubble hilang otomatis; bubble di atasnya turun ke posisi baru                 |
| 8.5 | Emoji `:heart:` dalam pesan            | Emoji tampil di bubble                                                         |
| 8.6 | Quit saat bubble aktif                 | Bubble hilang, tidak ada orphan entity                                         |

---

## 9. TAB List Parity

### 9a. Header/Footer

| #   | Aksi                                                   | Ekspektasi                                              |
|-----|--------------------------------------------------------|---------------------------------------------------------|
| 9.1 | Login                                                  | Header & footer tab list tampil dengan warna hex + PAPI |
| 9.2 | Tunggu `refresh-seconds` (default 5s)                  | Placeholders seperti `%server_online%` ter-update       |
| 9.3 | `%anim_gradient-title%` di header (dengan anim config) | Frame berganti tiap refresh                             |

### 9b. Belowname

| #   | Aksi                                       | Ekspektasi                                                              |
|-----|--------------------------------------------|-------------------------------------------------------------------------|
| 9.4 | `mode: HEALTH`                             | Angka HP tampil di bawah nametag semua pemain + display name `♥ Health` |
| 9.5 | `mode: PLACEHOLDER` + `refresh-seconds: 3` | Score dari PAPI placeholder ter-update tiap 3 detik                     |

### 9c. Nametags & Sorting

| #   | Aksi                               | Ekspektasi                                                        |
|-----|------------------------------------|-------------------------------------------------------------------|
| 9.6 | Join (dengan LuckPerms grup admin) | Prefix `⚔ ADMIN` muncul di atas kepala & di tab list sebelum nama |
| 9.7 | Tab list                           | Admin teratas, lalu mod, lalu vip, lalu default (urut priority)   |
| 9.8 | Player SPECTATOR mode              | Tab name berubah "&8&oSpectator"                                  |
| 9.9 | Kembali ke survival                | Tab name kembali normal                                           |

---

## 10. Nicknames

| #    | Aksi                              | Ekspektasi                               |
|------|-----------------------------------|------------------------------------------|
| 10.1 | `/nick &b&lSpeedyBoi`             | Tab list name berubah; chat name berubah |
| 10.2 | `/nick` > 16 chars                | Error TOO LONG                           |
| 10.3 | Nick sama dengan nama pemain lain | IMPERSONATION error                      |
| 10.4 | `/nick` ganti lagi ≤ 60s          | Cooldown error                           |
| 10.5 | `/nick off`                       | Nama asli kembali                        |
| 10.6 | Rejoin dengan nick aktif          | Nick tetap terpasang (persisted)         |

---

## 11. Reports

| #    | Aksi                            | Ekspektasi                                                          |
|------|---------------------------------|---------------------------------------------------------------------|
| 11.1 | `/report <p> griefing base`     | Moderator online: alert merah + pling sound, klik teleport          |
| 11.2 | Report kedua ≤ 60s              | Pesan cooldown + detik tersisa                                      |
| 11.3 | `/reports`                      | **Screen GUI** terbuka (bukan chat): tiap laporan = paper clickable |
| 11.4 | Left click paper                | Resolve; screen refresh tanpa report tsb                            |
| 11.5 | Right click paper               | Teleport ke target (jika online)                                    |
| 11.6 | Join staff saat ada open report | Reminder count                                                      |

---

## 12. Polls

| #    | Aksi                              | Ekspektasi                                     |
|------|-----------------------------------|------------------------------------------------|
| 12.1 | `/poll create 30 Makanan favorit? | Pizza                                          | Bakso` | Kartu poll broadcast; opsi clickable |
| 12.2 | Vote pizza                        | Counter +1; ✔ pada pilihan; revote pindah opsi |
| 12.3 | Tunggu habis 30s                  | Bar hasil % + winner announcement              |
| 12.4 | `/poll vote 1` (tanpa poll)       | "There is no open poll"                        |
| 12.5 | `/poll stop`                      | Poll berakhir                                  |

## 13. Reputation

| #    | Aksi                   | Ekspektasi                                     |
|------|------------------------|------------------------------------------------|
| 13.1 | `/rep <p2>`            | +1 given; broadcast dengan hex color `#a7ff33` |
| 13.2 | `/rep <p2>` lagi ≤ 24h | Pesan ALREADY                                  |
| 13.3 | `/rep <self>`          | Pesan SELF                                     |
| 13.4 | `/reputation <p2>`     | Skor benar                                     |

## 14. Chat Games

| #    | Aksi                    | Ekspektasi                                                       |
|------|-------------------------|------------------------------------------------------------------|
| 14.1 | `/chatgames math`       | Soal matematika broadcast; jawab benar → winner + reward command |
| 14.2 | `/chatgames scramble`   | Kata diacak; jawab asli                                          |
| 14.3 | `/chatgames fast-type`  | Kalimat; ketik persis                                            |
| 14.4 | `/chatgames reverse`    | Kata dibalik; ketik asli                                         |
| 14.5 | `/chatgames trivia`     | Q&A dari config                                                  |
| 14.6 | `/chatgames hot-letter` | Ketik kata awalan X ≥ min length                                 |
| 14.7 | `/chatgames stop`       | Batal + pengumuman                                               |
| 14.8 | Jawaban salah           | Tidak terjadi apa-apa (message tetap jalan normal)               |

## 15. Staff Notes

| #    | Aksi                           | Ekspektasi                                     |
|------|--------------------------------|------------------------------------------------|
| 15.1 | `/note add <p> "Review build"` | Note added                                     |
| 15.2 | `/notes <p>`                   | Header + tiap note dengan tanggal & staff name |
| 15.3 | `/notes clear <p>`             | Count removed; list kosong                     |

## 16. Raid Protection

| #    | Aksi                                | Ekspektasi                                                                           |
|------|-------------------------------------|--------------------------------------------------------------------------------------|
| 16.1 | 3 pemain ketik pesan sama dalam 10s | Pesan ke-3+ di-cancel; moderator alert ⚠ merah; action command jalan (jika dikonfig) |
| 16.2 | 1 pemain spam pesan sama            | TIDAK memicu raid (butuh ≥ 2 pemain berbeda)                                         |

## 17. Sleep

| #    | Aksi                            | Ekspektasi                                                          |
|------|---------------------------------|---------------------------------------------------------------------|
| 17.1 | Player tidur (50% dari online)  | Broadcast "night is moving faster"; waktu bergeser +100 ticks/detik |
| 17.2 | Cukup pemain tidur sampai fajar | Broadcast "Good morning"; waktu = pagi; tidak instan                |
| 17.3 | Player bangun sebelum fajar     | Akselerasi berhenti; waktu freeze di posisi terakhir                |
| 17.4 | Nether/End tidak terpengaruh    | Hanya overworld                                                     |

## 18. Custom Screens

| #    | Aksi                  | Ekspektasi                                         |
|------|-----------------------|----------------------------------------------------|
| 18.1 | `/screen`             | Contoh screen terbuka (dari `screens/example.yml`) |
| 18.2 | Klik tombol di screen | Aksi berjalan (console_command, message, dsb)      |

## 19. Warnings

| #    | Aksi                 | Ekspektasi                                           |
|------|----------------------|------------------------------------------------------|
| 19.1 | `/warn <p> <reason>` | Target terima warning; tersimpan; notify broadcast   |
| 19.2 | Capai 3 warnings     | Console: escalation command dieksekusi (mis. ban 1d) |
| 19.3 | `/warnings <p>`      | List semua warnings dengan tanggal & reason          |

---

## ✅ Selesai Testing

Jika SEMUA item lulus → rilis **1.1.0.0** siap produksi!

**Lapor bug:** section nomor + console snippet + langkah reproduksi.
