# zEssentials 1.2.1.0 — Checklist Pengujian Lengkap

> Server: Paper / fork berbasis 26.2 • Java 21+
> Gunakan jar `target/zEssentials-1.2.1.0.jar`
> Laporkan bug: section nomor + console snippet + langkah reproduksi

---

## 0. Persiapan

- [ ] Hapus SEMUA jar lama `zEssentials*.jar` dari `plugins/`
- [ ] Salin `target/zEssentials-1.2.1.0.jar` ke `plugins/`
- [ ] Hapus folder `plugins/zEssentials` untuk fresh install ATAU backup & biarkan (test config update)

---

## 1. Startup & Config

| # | Aksi | Ekspektasi |
|---|------|-----------|
| S1 | Start server fresh (no plugins/zEssentials folder) | Plugin enable, buat config.yml + semua module configs |
| S2 | Cek console untuk error | Tidak ada `Failed to create instance from map` atau `MongoConfiguration` NPE |
| S3 | Restart server | Config self-heal berjalan, tidak ada error |
| S4 | Set `storage-type: MONGO` tanpa isi mongo-configuration | Tidak crash — default values (port=0, host=null) |
| S5 | Set `storage-type: SQLITE` (default) | Plugin enable normal |

---

## 2. Nicknames Module

| # | Aksi | Ekspektasi |
|---|------|-----------|
| N1 | `/nick Steve` | Display name berubah jadi "Steve", pesan konfirmasi |
| N2 | `/nick off` | Nickname dihapus, kembali ke real name |
| N3 | `/nick &cRedName` | Nickname berwarna merah (jika allow-colors: true) |
| N4 | `/nick <player> &bBlue` (admin) | Nickname player lain berubah |
| N5 | `/nick` tanpa arg | Pesan usage |
| N6 | Tab-complete `/nick ` | Suggestions: "off" + online player names |
| N7 | `/nick verylongnamethatiswaytoolong` | Pesan error "too long" (max-length) |
| N8 | `/nick bad!name` | Pesan error "forbidden characters" |
| N9 | `/nick` cepat 2x dalam cooldown | Pesan cooldown dengan detik tersisa |
| N10 | Rejoin setelah /nick | Nickname tetap ada (persisted) |
| N11 | Cek `/nick` dengan hex color `&#ff0000Red` | Hex color render benar (bukan §#ff0000) |

---

## 3. Disguise System

### 3.1 Basic Disguise

| # | Aksi | Ekspektasi |
|---|------|-----------|
| D1 | `/disguise Notch` (Notch online) | Name + skin berubah jadi Notch, pesan konfirmasi |
| D2 | Lihat skin di F5 (third person) | Jika self-view: false → tetap lihat skin asli. Jika true → lihat skin disguise |
| D3 | Player lain melihat | Mereka lihat name + skin Notch |
| D4 | Tab list | Nama berubah jadi Notch |
| D5 | Name above head (NameTag) | Menampilkan disguise name |
| D6 | Chat | Name di chat menampilkan disguise name |
| D7 | `/disguise off` | Disguise dihapus, kembali ke real name + skin |
| D8 | `/undisguise` | Sama seperti `/disguise off` |
| D9 | Rejoin setelah disguise | Disguise tetap ada (persisted di disguises.json) |

### 3.2 Admin Disguise

| # | Aksi | Ekspektasi |
|---|------|-----------|
| D10 | `/disguise <target> <player>` (admin) | Target di-disguise sebagai player lain |
| D11 | `/undisguise <player>` (admin) | Disguise player lain dihapus |
| D12 | `/disguise <target> off` (admin) | Sama seperti /undisguise <target> |

### 3.3 Random Disguise

| # | Aksi | Ekspektasi |
|---|------|-----------|
| D13 | `/disguise random` | Disguise sebagai random dari config pool |
| D14 | `/disguise list` | Menampilkan list random pool dari config |
| D15 | Kosongkan random-pool di config, `/disguise random` | Pesan "pool is empty" |

### 3.4 Custom Skin

| # | Aksi | Ekspektasi |
|---|------|-----------|
| D16 | `/disguise skin <texture-value>` | Skin berubah, name tetap |
| D17 | `/disguise skin <texture> <signature>` | Skin berubah dengan signature |
| D18 | `/disguise skin <texture>` tanpa perm ESSENTIALS_DISGUISE_SKIN | No permission |

### 3.5 Cooldown

| # | Aksi | Ekspektasi |
|---|------|-----------|
| D19 | `/disguise` 2x dalam cooldown | Pesan cooldown dengan detik |
| D20 | Bypass dengan perm ESSENTIALS_DISGUISE_BYPASS_COOLDOWN | Tidak ada cooldown |

### 3.6 Offline Player Skin

| # | Aksi | Ekspektasi |
|---|------|-----------|
| D21 | `/disguise <offline-player>` | Pesan "Fetching skin...", lalu skin diambil dari Mojang API |
| D22 | Disconnect internet, `/disguise <offline-player>` | Pesan "Failed to fetch skin" |
| D23 | `/disguise <offline-player>` 2x (cache hit) | Instan (dari cache, no API call) |

---

## 4. Storage Migration

| # | Aksi | Ekspektasi |
|---|------|-----------|
| M1 | Punya nicknames.json lama, start server | Migrasi ke disguises.json, nicknames tetap |
| M2 | Cek file `disguises.json` dibuat | Entry ada dengan disguiseName, textureValue=null |
| M3 | Hapus nicknames.json, restart | Data tetap dari disguises.json |

---

## 5. Chat Module

| # | Aksi | Ekspektasi |
|---|------|-----------|
| C1 | Type `[item]` in chat | Menampilkan item hover |
| C2 | Type `[pos]` in chat | Menampilkan koordinat dengan click-to-tp |
| C3 | `/msg <player> hello` | Private message terkirim |
| C4 | `/dnd` | DND toggle, ping sound suppressed |
| C5 | `/chathistory` | Chat history GUI terbuka |
| C6 | `/poll create <s> Question? | Yes | No` | Poll dibuat dengan clickable options |
| C7 | `/chatgames start` | Chat game dimulai |
| C8 | `/nick &cTest` lalu chat | Chat name menampilkan nickname berwarna |

---

## 6. Network & Social

| # | Aksi | Ekspektasi |
|---|------|-----------|
| NW1 | `/g` toggle | Global chat toggle on/off |
| NW2 | `/friend add <player>` | Friend request terkirim |
| NW3 | `/friend accept <player>` | Friend ditambah |
| NW4 | `/friend list` | List friends |
| NW5 | `/guild create TestGuild` | Guild dibuat, jadi leader |
| NW6 | `/guild invite <player>` | Invite terkirim |
| NW7 | `/gc hello` | Guild chat message |
| NW8 | `/party create` | Party dibuat |
| NW9 | `/party invite <player>` | Invite terkirim |
| NW10 | `/pc hello` | Party chat message |

---

## 7. Enderchest & Stash

| # | Aksi | Ekspektasi |
|---|------|-----------|
| E1 | `/enderchest` | Enderchest GUI terbuka (54 slot, paginated) |
| E2 | `/endersee <player>` (admin) | Lihat enderchest player lain (read-only) |
| E3 | `/stash` | Category picker (Item Stash / Material Stash) |
| E4 | `/stash item` | Item stash GUI terbuka |
| E5 | `/stash material` | Material stash GUI terbuka |

---

## 8. Pricing & Tooltips

| # | Aksi | Ekspektasi |
|---|------|-----------|
| P1 | `/pricing` toggle | Price display on/off |
| P2 | Buka inventory dengan item yang ada di shop | Tooltip menampilkan harga |
| P3 | Install RoyaleEconomy, cek tooltip | Harga dari RoyaleEconomy muncul |

---

## 9. Crafting

| # | Aksi | Ekspektasi |
|---|------|-----------|
| CR1 | `/craft` atau buka crafting table | Custom crafting GUI (54 slot) |
| CR2 | Craft item dengan recipe valid | Item hasil muncul |
| CR3 | Tutup GUI dengan item di slot | Item kembali ke inventory |

---

## 10. Vanish

| # | Aksi | Ekspektasi |
|---|------|-----------|
| V1 | `/vanish` | Vanish toggle, fake quit message |
| V2 | `/vanish <player>` (admin) | Toggle vanish player lain |
| V3 | Cek pickup items saat vanish | Tidak bisa pickup |
| V4 | `/vanish` lagi | Unvanish, fake join message |

---

## 11. Effects & Visuals

| # | Aksi | Ekspektasi |
|---|------|-----------|
| EF1 | `/tpa <player>`, accept | Particle ring effect di lokasi asal & tujuan |
| EF2 | `/warp <name>` | Particle ring effect |
| EF3 | `/fly` | Particle effect |
| EF4 | `/gamemode creative` | Burst effect |

---

## 12. Terms & Rules

| # | Aksi | Ekspektasi |
|---|------|-----------|
| T1 | Fresh player join | Terms dialog muncul |
| T2 | Klik Accept | Terms diterima, bisa main |
| T3 | Klik Deny | Kick |
| T4 | `/terms reset <player>` (admin) | Terms reset, player harus accept lagi |

---

## 13. Reports & Notes

| # | Aksi | Ekspektasi |
|---|------|-----------|
| R1 | `/report <player> hacking` | Report terkirim ke staff |
| R2 | `/reports` (staff) | Reports screen GUI terbuka |
| R3 | Click teleport di report | Teleport ke reported player |
| R4 | `/note add <player> <text>` (admin) | Note ditambah |
| R5 | `/notes <player>` (admin) | List notes |

---

## 14. Hex Color Verification

| # | Aksi | Ekspektasi |
|---|------|-----------|
| H1 | `/nick &#ff0000Red` | Name merah hex (bukan broken §#ff0000) |
| H2 | Cek tablist dengan hex color di config | Hex render benar |
| H3 | Cek nametag dengan hex color | Hex render benar |
| H4 | Cek poll dengan hex color | Hex render benar |
| H5 | Cek chat bubbles dengan hex color | Hex render benar |

---

## 15. Config Healer

| # | Aksi | Ekspektasi |
|---|------|-----------|
| CH1 | Hapus baris dari module config, restart | Config self-heal menambahkan kembali |
| CH2 | Tambah unknown key di config | Config self-heal tidak hapus |
| CH3 | Bump config-version di nicknames/config.yml dari 2 → 1 | Config self-heal update ke v2 |
| CH4 | Cek semua 49 module configs punya config-version | Semua ada config-version: 1 atau 2 |

---

## 16. Performance & Folia

| # | Aksi | Ekspektasi |
|---|------|-----------|
| F1 | Enable Folia, start server | Plugin enable tanpa error |
| F2 | `/tpa` di Folia | Teleport berhasil (teleportAsync) |
| F3 | Disguise di Folia | Skin refresh berhasil |
| F4 | 20 players online, semua disguise | Tidak ada lag spike |
| F5 | Toggle vanish di Folia | Scoreboard ops di runNextTick, no crash |

---

## 17. Build & Tests

| # | Aksi | Ekspektasi |
|---|------|-----------|
| B1 | `./gradlew build -x test --console=plain` | BUILD SUCCESSFUL, jar di target/ |
| B2 | `./gradlew test --console=plain --no-daemon` | BUILD SUCCESSFUL |
| B3 | Cek test count | 137+ root tests, 892+ API tests |
| B4 | `./gradlew :test --tests "dev.yanianz.essentials.disguise.DisguiseDataTest"` | 5 tests pass |
| B5 | `./gradlew :test --tests "dev.yanianz.essentials.disguise.SkinCacheTest"` | 7 tests pass |
| B6 | `./gradlew :test --tests "dev.yanianz.essentials.nicknames.NicknamesModuleTest"` | 11 tests pass |
