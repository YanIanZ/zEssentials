# zEssentials 1.0.4.0 — Checklist Pengujian Lengkap

> Server: Paper / fork berbasis 26.2 • Java 21+ (test bench memakai Java 25)
> Tandai ✔ setelah setiap item lolos. Laporkan deviasi console ke developer.

## 0. Persiapan

- [ ] Backup folder `plugins/zEssentials` lama
- [ ] Salin `target/zEssentials-1.0.4.0.jar` ke `plugins/`
- [ ] Hapus `plugins/zMenu-*.jar` & `plugins/PlaceholderAPI-*.jar` (untuk test auto-install), ATAU biarkan terpasang (skip bagian auto-install)

## 1. Startup & Dependency

| # | Test | Ekspektasi |
|---|------|-----------|
| 1.1 | Server start tanpa zMenu/PAPI | Console: "Missing required plugins ... trying to install them" → PlaceholderAPI ter-download dari Hangar + langsung enable; zMenu ter-download dari Modrinth lalu warning "cannot be hot-loaded" |
| 1.2 | `dependency-loader.auto-restart: true` di config.yml | Setelah install, server shutdown sendiri dengan countdown broadcast 10s (butuh host auto-restart untuk hidup lagi) |
| 1.3 | Start kedua | Tidak ada download ulang; PAPI aktif; tinggal restart sekali utk zMenu bila belum |
| 1.4 | Start ketiga (semua plugin lengkap) | Banner gradient `zEssentials` muncul, tidak ada ERROR/WARN dependency, jumlah commands/modules tampil |
| 1.5 | Shutdown | Banner "disabled, see you soon!" tanpa NPE |

## 2. Perintah Baru

| # | Command | Ekspektasi |
|---|---------|-----------|
| 2.1 | `/list` | Nama pemain online urut abjad + counter `%amount%/%max%`; vanished tersembunyi bagi non-staff |
| 2.2 | `/itemdb` (pegang item) | Material, key (`minecraft:...`), amount, stack size |
| 2.3 | `/itemdb` (tangan kosong) | Pesan error ramah, tidak error console |
| 2.4 | `/tpaall` | Semua pemain online terima request teleport; 1 pesan ringkas ke sender; skip pemain yang ignore/DND-disabled |
| 2.5 | `/essentials` | Membuka command utama plugin (alias `/zessentials`, `/ess` masih jalan) |

## 3. Freeze / Unfreeze

| # | Test | Ekspektasi |
|---|------|-----------|
| 3.1 | `/freeze <player>` | Target: tidak bisa gerak (boleh putar kepala), glow biru, partikel salju mengorbit (2 titik × 3 level), chat & command diblokir, walk/fly speed = 0 |
| 3.2 | `/freeze <player>` (ulang) | Idempotent — tetap frozen, tidak toggle |
| 3.3 | `/unfreeze <player>` | Bebas gerak, glow hilang, partikel berhenti, speed kembali normal (walk 0.2 / fly 0.1) |
| 3.4 | Setelah unfreeze: `/fly` on → gerak bebas | **Regression test bug lama** |
| 3.5 | Reconnect saat masih frozen (dengan persist default false) | Auto-release — tidak ada pemain stuck |
| 3.6 | GUI sanction tombol freeze | Masih bertindak sebagai toggle |
| 3.7 | Pesan/chat pemain frozen | Diblokir + notifikasi |

## 4. Effects Module

| # | Test | Ekspektasi |
|---|------|-----------|
| 4.1 | `/tpa <p>` → accept | Ring portal di titik asal & tujuan + suara enderman |
| 4.2 | `/rtp` atau `/warp` atau `/spawn` atau `/home` | Efek teleport sama |
| 4.3 | `/gamemode creative` → survival | Burst HAPPY_VILLAGER + levelup |
| 4.4 | `/fly on/off` (double jump space ×2 juga) | Burst CLOUD |
| 4.5 | `/heal <p>` dan `/god <p>` (on) | Sparkle totem |

## 5. Terms of Service

| # | Test | Ekspektasi |
|---|------|-----------|
| 5.1 | Join dengan player baru | ±0.25s muncul **dialog Mojang asli** (bukan chest): pertanyaan + rules + 2 tombol |
| 5.2 | Klik ACCEPT | Dialog tertutup, pesan accepted, chat bisa dipakai |
| 5.3 | Tutup dengan ESC | Dialog terbuka ulang otomatis (masih pending) |
| 5.4 | Ketik command selain `/terms*` saat pending | Diblokir dengan pesan |
| 5.5 | Chat saat pending | Diblokir |
| 5.6 | Diam melewati timeout (default 60s) | Kick dengan pesan timeout |
| 5.7 | Klik REFUSE | Kick dengan pesan refuse |
| 5.8 | Rejoin setelah accept | Tidak ada dialog lagi (persisted) |
| 5.9 | Fallback (server < 1.21.7, simulasi skip) | Chest GUI 45-slot setara |
| 5.10 | `/terms reset <player>` (console/staff) | Acceptance hilang → join berikutnya dapat dialog lagi |

## 6. Screen Framework

| # | Test | Ekspektasi |
|---|------|-----------|
| 6.1 | `/warpgui` | Semua warp yang boleh diakses; klik = teleport |
| 6.2 | `/baltopgui` | Player head paginated + saldo; tombol next/prev/close jalan |
| 6.3 | Item > isi satu halaman | Pagination arrow muncul & berfungsi |

## 7. Chat Module v2

### Keywords
| # | Ketik | Ekspektasi |
|---|-------|-----------|
| 7.1 | `[item]` atau `[i]` | Nama item berwarna ×jumlah, hover tooltip item, klik menjalankan `/showitem` |
| 7.2 | `[inv]` / `[inventory]` | Badge [INV]; hover = daftar semua item; klik = copy list ke clipboard |
| 7.3 | `[ender]` / `[ec]` | Sama seperti atas, sumber enderchest |
| 7.4 | `[pos]` / `[position]` | `[x, y, z]`; hover detail; klik suggest `/tp x y z` |
| 7.5 | `[health]`, `[food]`, `[ip]`, `[store]`, `[ping]`, `[money]`, `[coins]`, `[playtime]`, `[block]` | Sesuai config |

### Mention
| # | Test | Ekspektasi |
|---|------|-----------|
| 7.6 | A ketik "hai @B coba lihat" | Di layar B nama @B gold bold + hover "Someone mentioned you!"; pemain lain lihat versi hover berbeda; B dengar sound |
| 7.7 | B punya DND aktif | Sound tidak bunyi, highlight tetap ada |
| 7.8 | Klik mention | Men-suggest `/msg B ` di chat box |

### Lainnya
| # | Test | Ekspektasi |
|---|------|-----------|
| 7.9 | Emoji `:heart:` dsb | Berubah jadi simbol sesuai map config |
| 7.10 | `/chatslowmode 5` → spam chat | Pesan ke-2 ditolak dengan countdown; bypass perm bebas; `/chatslowmode 0` mematikan |
| 7.11 | `/dnd` lalu dimention orang lain | Tidak ada sound |
| 7.12 | `/chathistory <p>` (sebagai moderator) | Tiap baris ada tombol ✖ merah; klik = pesan hilang dari list & DB, page re-render |
| 7.13 | Pesan mengandung caps/link/flood/sama | Filter existing masih jalan (regression) |

## 8. Customization GUI

| # | Test | Ekspektasi |
|---|------|-----------|
| 8.1 | `/chatcolor` | GUI 16 warna + slot BOLD/ITALIC (bila punya perm decorations) |
| 8.2 | Pilih warna → chat | Pesan kamu berwarna itu; bold/italic toggle live |
| 8.3 | `/tags` | Daftar tag; tag berperm tampil barrier; pilih → prefix muncul sebelum pesan berikutnya |
| 8.4 | Rejoin | Warna/tag masih dipakai (persisted json) |

## 9. Sanctions

| # | Test | Ekspektasi |
|---|------|-----------|
| 9.1 | `/warn <p> <reason>` | Target terima MESSAGE_WARN; staff notify; tersimpan |
| 9.2 | Capai threshold escalation (mis. warn ke-3) | Console log escalation + command ban dieksekusi |
| 9.3 | `/warnings <p>` | Header + daftar reason & tanggal |
| 9.4 | Clear expired sanctions (SQLite: majukan tanggal, start ulang) | Tidak ada error `expired_at` |

## 10. Notes

| # | Test | Ekspektasi |
|---|------|-----------|
| 10.1 | `/note add <p> "jangan lupa review"` | Note added message |
| 10.2 | `/notes <p>` | Header + tiap note: tanggal, staff, isi |
| 10.3 | `/notes clear <p>` | Count removed; `/notes <p>` kosong |

## 11. Reputation

| # | Test | Ekspektasi |
|---|------|-----------|
| 11.1 | `/rep <p2>` | +1 given, broadcast opsional muncul |
| 11.2 | `/rep <p2>` lagi (≤24 jam) | Pesan ALREADY |
| 11.3 | `/rep <self>` | Pesan SELF |
| 11.4 | `/reputation <p2>` | Skor benar |

## 12. Polls

| # | Test | Ekspektasi |
|---|------|-----------|
| 12.1 | `/poll create 30 Makanan favorit? | Pizza | Bakso` (pipe!) | Kartu poll broadcast; tiap opsi clickable |
| 12.2 | Vote dua pemain beda opsi | Counter naik, tandan ✔ pindah ke opsi baru (revote) |
| 12.3 | Vote dua kali opsi sama | Tetap 1 vote |
| 12.4 | Tunggu habis | Bar hasil % + winner/tie/no-vote announcement |
| 12.5 | `/poll stop` | Poll langsung berakhir |
| 12.6 | `/poll vote` tanpa poll | Pesan "no open poll" |

## 13. Chat Games

| # | Test | Ekspektasi |
|---|------|-----------|
| 13.1 | `/chatgames math` | Soal matematika broadcast |
| 13.2 | Jawab benar di chat | Jawaban tidak ikut ke chat; announce winner; reward command jalan |
| 13.3 | Ulangi untuk scramble/fast-type/reverse/trivia/hot-letter | Semua tayang & bisa dijawab (hot-letter: kata apa pun awalan X ≥ min length) |
| 13.4 | Mulai game saat game aktif | Ditolak "already running" |
| 13.5 | `/chatgames stop` | Batal + pengumuman |
| 13.6 | `auto-interval-minutes: 1` → tunggu | Random round otomatis mulai |

## 14. Nicknames

| # | Test | Ekspektasi |
|---|------|-----------|
| 14.1 | `/nick &b&lSpeedyBoi` | Display name + tab list berubah |
| 14.2 | `/nick` > max chars | Error TOO LONG |
| 14.3 | Nick berisi simbol aneh | Error INVALID |
| 14.4 | Nick sama dg nama player online lain | IMPERSONATION error |
| 14.5 | `/nick` ganti lagi ≤60s | Cooldown (kecuali bypass perm / admin target) |
| 14.6 | `/nick off` | Kembali ke nama asli |
| 14.7 | `/nick <player> <nama>` sebagai admin via console→player arg | Berlaku untuk player tsb |
| 14.8 | Rejoin | Nick tetap terpasang |

## 15. Reports

| # | Test | Ekspektasi |
|---|------|-----------|
| 15.1 | `/report <p> griefing base` | Moderator online: alert merah + pling sound, clickable tp |
| 15.2 | Report kedua dalam cooldown | Pesan wait + detik tersisa |
| 15.3 | `/report <self>` | Pesan SELF |
| 15.4 | `/reports` | List open reports + tombol [✔] resolve dan [➤] tp per baris |
| 15.5 | Klik [✔] | Mark resolved; hilang dari open list |
| 15.6 | Join staff saat ada open report | Reminder count |

## 16. Integrasi Eksternal

| # | Test | Ekspektasi |
|---|------|-----------|
| 16.1 | DiscordSRV terpasang + bridge enabled | Chat MC muncul di main channel Discord sesuai format |
| 16.2 | DiscordSRV tidak ada + bridge enabled | Log "stays idle", tidak crash |
| 16.3 | Network relay enabled di 2 server (BungeeCord/Velocity modern forwarding) | Chat server A muncul di server B dengan format `[serverName] player » msg` |
| 16.4 | ItemsAdder/Oraxen/LuckPerms terpasang | Tidak ada regression pada modul existing (memakai softdepend path masing-masing) |

## 17. Storage & Parser

| # | Test | Ekspektasi |
|---|------|-----------|
| 17.1 | `/fly add <p> 1d12h30m` | Durasi terakumulasi benar (1 hari 12 jam 30 menit) |
| 17.2 | `/fly info` | Menampilkan sisa waktu tepat |
| 17.3 | `/eco give <p> 1k` | Saldo +1000; `1.5m` = +1.500.000; `2b`, `3t` idem |
| 17.4 | `/eco give <p> 1,000.5` | Koma tetap didukung |
| 17.5 | MySQL/MariaDB storage type | Driver resolve status ALREADY_AVAILABLE; koneksi valid; tidak ada download |

## 18. Regression Sweep

- [ ] `/chatclear`, `/broadcast`, `/spawn`, `/home`, `/warp <name>`, `/sethome`
- [ ] `/gamemode` berbagai mode, `/heal`, `/feed`, `/god`
- [ ] `/vanish` toggle & efeknya pada `/list` milik pemain lain
- [ ] Private message `/msg` + `/r` balas (dua arah)
- [ ] Reply tanpa pernah DM → pesan REPLY_NO_TARGET
- [ ] Ignore system: ignored player tak bisa DM/tpa
- [ ] Economy: pay, balance, transactions di baltop
- [ ] Restart server — no crash, no NPE, banner disable rapi

---

**Laporan bug:** sertakan snippet console + langkah reproduksi + build number.
