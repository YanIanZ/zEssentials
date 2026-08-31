# zEssentials 1.2.1.0 — Checklist Pengujian Lengkap

> Server: Paper / fork berbasis 26.2 • Java 21+
> Gunakan jar `target/zEssentials-1.2.1.0.jar`
> Laporkan bug: section nomor + console snippet + langkah reproduksi

---

## 0. Persiapan

- [ ] Hapus SEMUA jar lama `zEssentials*.jar` dari `plugins/`
- [ ] Salin `target/zEssentials-1.2.1.0.jar` ke `plugins/`
- [ ] Hapus folder `plugins/zEssentials` untuk fresh install ATAU backup & biarkan (test config update)
- [ ] Install ProtocolLib (wajib untuk disguise system)

---

## 1. Startup & Config

| # | Aksi | Ekspektasi |
|---|------|-----------|
| S1 | Start server fresh | Plugin enable, buat config.yml + semua module configs |
| S2 | Cek console untuk error | Tidak ada `Failed to create instance from map` atau `MongoConfiguration` NPE |
| S3 | Restart server | Config self-heal berjalan, tidak ada error |
| S4 | Set `storage-type: MONGO` tanpa isi mongo-configuration | Tidak crash — default values |
| S5 | Set `storage-type: SQLITE` (default) | Plugin enable normal |

---

## 2. Nicknames

| # | Aksi | Ekspektasi |
|---|------|-----------|
| N1 | `/nick Steve` | Display name berubah jadi "Steve" |
| N2 | `/nick off` | Nickname dihapus, kembali ke real name |
| N3 | `/nick &cRedName` | Nickname berwarna merah |
| N4 | `/nick <player> &bBlue` (admin) | Nickname player lain berubah |
| N5 | `/nick` tanpa arg | Pesan usage |
| N6 | Tab-complete `/nick ` | Suggestions: "off" + online player names |
| N7 | `/nick verylongname` | Pesan "too long" |
| N8 | `/nick bad!name` | Pesan "forbidden characters" |
| N9 | `/nick` 2x dalam cooldown | Pesan cooldown dengan detik |
| N10 | Rejoin setelah /nick | Nickname tetap ada |
| N11 | `/nick &#ff0000Red` | Hex color render benar |
| N12 | Cek chat setelah /nick | Chat menampilkan nickname, bukan real name |
| N13 | Cek tab list setelah /nick | Tab list menampilkan nickname |
| N14 | Cek name above head | NameTag menampilkan nickname |
| N15 | Cek below-name objective | Menampilkan nickname |
| N16 | `/nick` tanpa arg (Hypixel style) | Random identity: nama acak + skin matching, "Randomizing..." message |
| N17 | `/nick` lagi | Identity baru berbeda (re-roll) |
| N18 | `/nick clear` | Nick + skin hilang, kembali normal |
| N19 | `/nick Notch` | Nama + skin Notch (matching skin) |
| N20 | Kosongkan random-nick-pool, `/nick` | Pesan "random nick pool is empty" |
| N21 | Matikan internet, `/nick OfflineName` | Pesan fetch failed |

---

## 3. Disguise — Player

| # | Aksi | Ekspektasi |
|---|------|-----------|
| D1 | `/disguise Notch` (Notch online) | Name + skin berubah jadi Notch |
| D2 | Lihat skin di F5 (third person) | Jika self-view: false → lihat skin asli; true → lihat disguise |
| D3 | Player lain melihat | Mereka lihat name + skin Notch |
| D4 | Tab list | Nama berubah jadi Notch |
| D5 | Name above head | Menampilkan disguise name |
| D6 | Chat setelah disguise | Chat menampilkan disguise name |
| D7 | `/disguise off` | Disguise dihapus, kembali ke real name + skin |
| D8 | `/undisguise` | Sama seperti `/disguise off` |
| D9 | Rejoin setelah disguise | Disguise tetap ada (persisted) |
| D10 | `/disguise <target> <player>` (admin) | Target di-disguise sebagai player lain |
| D11 | `/undisguise <player>` (admin) | Disguise player lain dihapus |

---

## 4. Disguise — Mob (LibsDisguises-style engine)

| # | Aksi | Ekspektasi |
|---|------|-----------|
| DM1 | `/disguise mob ZOMBIE` | Player berubah jadi zombie di mata player lain |
| DM2 | `/disguise zombie` (bare mob name, LD-style) | Sama seperti `/disguise mob ZOMBIE` |
| DM3 | `/disguise SKELETON` | Player berubah jadi skeleton |
| DM4 | `/disguise creeper` (lowercase) | Player berubah jadi creeper |
| DM5 | `/disguise mob INVALID` | Pesan "Unknown mob type" |
| DM6 | `/disguise ENDER_DRAGON` | Player berubah jadi ender dragon |
| DM7 | Equipment check saat mob disguise | Pemain lain TIDAK melihat senjata/armor di mob |
| DM8 | Tab list saat mob disguise | Disguised player HILANG dari tab list pemain lain (hide-from-tab) |
| DM9 | Tab list milik disguised player sendiri | Entry sendiri tetap ada |
| DM10 | Custom name di atas mob | Menampilkan disguise name (jika diset) |
| DM11 | `/disguise off` setelah mob disguise | Kembali ke normal, tab list pulih |
| DM12 | `/disguise mob ZOMBIE <player>` (admin) | Target berubah jadi zombie |
| DM13 | Mob disguise + rejoin | Disguise tetap (persisted), refresh otomatis 40 tick setelah join |
| DM14 | `/disguise mob BOAT` (non-living) | Ditolak — hanya living entities |
| DM15 | `/disguise player Notch` (LD-style) | Name+skin disguise via player subcommand |
| DM16 | `/disguise player <target> Notch` (admin) | Target di-disguise sebagai Notch |
| DM17 | Self-view enabled + `/disguise mob ZOMBIE` + F5 | Player melihat dirinya sebagai zombie di third-person |
| DM18 | Self-view + `/disguise off` + F5 | Player melihat dirinya kembali normal |

## 5. Disguise — Random & Skin

| # | Aksi | Ekspektasi |
|---|------|-----------|
| DR1 | `/disguise random` | Disguise sebagai random dari config pool |
| DR2 | `/disguise list` | Menampilkan list random pool |
| DR3 | Kosongkan random-pool, `/disguise random` | Pesan "pool is empty" |
| DR4 | `/disguise skin <texture>` | Skin berubah, name tetap |
| DR5 | `/disguise skin <texture> <signature>` | Skin dengan signature |
| DR6 | `/disguise` 2x dalam cooldown | Pesan cooldown |

---

## 6. /realname

| # | Aksi | Ekspektasi |
|---|------|-----------|
| RN1 | `/nick Steve` lalu `/realname` | "The real name of Steve is <realname>" |
| RN2 | `/disguise Notch` lalu `/realname Notch` | "The real name of Notch is <realname>" |
| RN3 | `/realname` tanpa disguise | "That player is not disguised" |
| RN4 | `/realname <player>` (admin, target disguised) | Reveal real name |

---

## 7. Crafting & Enderchest Block

| # | Aksi | Ekspektasi |
|---|------|-----------|
| C1 | Right-click crafting table | Custom crafting GUI terbuka (54 slot) |
| C2 | Sneak + right-click crafting table | Vanilla crafting table (bypass) |
| C3 | Disable crafting module, right-click crafting table | Pesan "module disabled", no vanilla GUI |
| C4 | `/craft` | Custom crafting GUI terbuka |
| C5 | Craft item dengan recipe valid | Item hasil muncul |
| C6 | Tutup GUI dengan item di slot | Item kembali ke inventory |
| C7 | Right-click enderchest | Ender chest overview terbuka (page selector ala Hypixel) |
| C8 | Sneak + right-click enderchest | Vanilla enderchest (bypass) |
| C9 | Disable enderchest module, right-click enderchest | Pesan "module disabled", no vanilla GUI |
| C10 | `/enderchest` | Overview page selector terbuka |
| C11 | Click page button (unlocked) | Page view terbuka dengan konten page itu |
| C12 | Click locked page button | Tidak terjadi apa-apa (locked) |
| C13 | Nav: click « First | Kembali ke page 1 |
| C14 | Nav: click Last » | Lompat ke page terakhir |
| C15 | Nav: ← Prev / Next → | Pindah page satu-satu |
| C16 | `/endersee <player>` (admin) | Lihat enderchest player lain (read-only) |

---

## 8. Chat Module

| # | Aksi | Ekspektasi |
|---|------|-----------|
| CH1 | Type `[item]` in chat | Menampilkan item hover |
| CH2 | Type `[pos]` in chat | Menampilkan koordinat dengan click-to-tp |
| CH3 | `/msg <player> hello` | Private message terkirim |
| CH4 | `/dnd` | DND toggle |
| CH5 | `/chathistory` | Chat history GUI |
| CH6 | `/poll create <s> Q? | Yes | No` | Poll dengan clickable options |
| CH7 | `/chatgames start` | Chat game dimulai |
| CH8 | `/nick &cTest` lalu chat | Chat menampilkan nickname berwarna |
| CH9 | `/disguise Notch` lalu chat | Chat menampilkan "Notch" |
| CH10 | Click `/report` di chat name | Report command pakai realname, bukan nickname |
| CH11 | Click `/msg` di chat name | Msg command pakai realname |

---

## 9. Scoreboard & Tab

| # | Aksi | Ekspektasi |
|---|------|-----------|
| TB1 | `/nick Steve` lalu cek tab | Tab menampilkan "Steve" |
| TB2 | `/nick &cSteve` lalu cek tab | Tab menampilkan "Steve" berwarna merah |
| TB3 | `/disguise Notch` lalu cek tab | Tab menampilkan "Notch" |
| TB4 | Cek below-name objective saat nick | Menampilkan nickname |
| TB5 | Cek scoreboard team prefix | Prefix group tetap, name menampilkan nickname |
| TB6 | `/nick off` lalu cek tab | Tab kembali ke real name |
| TB7 | Tab format dengan `%player%` placeholder | Resolve ke nickname |

---

## 10. Network & Social

| # | Aksi | Ekspektasi |
|---|------|-----------|
| NW1 | `/g` toggle | Global chat toggle |
| NW2 | `/friend add <player>` | Friend request |
| NW3 | `/friend accept <player>` | Friend added |
| NW4 | `/friend list` | List friends |
| NW5 | `/guild create Test` | Guild dibuat |
| NW6 | `/gc hello` | Guild chat |
| NW7 | `/party create` | Party dibuat |
| NW8 | `/pc hello` | Party chat |

---

## 11. Storage & Stash

| # | Aksi | Ekspektasi |
|---|------|-----------|
| ST1 | `/stash` | Category picker |
| ST2 | `/stash item` | Item stash GUI |
| ST3 | `/stash material` | Material stash GUI |
| ST4 | Punya nicknames.json lama, start | Migrasi ke disguises.json |
| ST5 | Cek `disguises.json` dibuat | Entry ada |

---

## 12. Vanish

| # | Aksi | Ekspektasi |
|---|------|-----------|
| V1 | `/vanish` | Vanish toggle, fake quit message |
| V2 | `/vanish <player>` (admin) | Toggle vanish player lain |
| V3 | Pickup items saat vanish | Tidak bisa pickup |
| V4 | `/vanish` lagi | Unvanish, fake join message |

---

## 13. Hex Color Verification

| # | Aksi | Ekspektasi |
|---|------|-----------|
| H1 | `/nick &#ff0000Red` | Name merah hex (bukan §#ff0000) |
| H2 | Hex di tablist config | Render benar |
| H3 | Hex di nametag config | Render benar |
| H4 | Hex di poll config | Render benar |
| H5 | Hex di chat bubbles | Render benar |

---

## 14. Config Healer

| # | Aksi | Ekspektasi |
|---|------|-----------|
| CH1 | Hapus baris dari config, restart | Config self-heal menambahkan kembali |
| CH2 | Bump config-version nicknames dari 2 → 1 | Self-heal update ke v2 |
| CH3 | Cek semua module configs punya config-version | Semua ada |

---

## 15. Folia

| # | Aksi | Ekspektasi |
|---|------|-----------|
| F1 | Enable Folia, start server | Plugin enable tanpa error |
| F2 | `/tpa` di Folia | Teleport berhasil |
| F3 | Disguise di Folia | Skin/mob refresh berhasil |
| F4 | `/vanish` di Folia | No scoreboard crash |
| F5 | Crafting/enderchest block di Folia | GUI terbuka |

---

## 16. Build & Tests

| # | Aksi | Ekspektasi |
|---|------|-----------|
| B1 | `./gradlew build -x test --console=plain` | BUILD SUCCESSFUL |
| B2 | `./gradlew test --console=plain --no-daemon` | BUILD SUCCESSFUL |
| B3 | Cek test count | 137+ root tests |
| B4 | `./gradlew :test --tests "dev.yanianz.essentials.disguise.*"` | 12 tests pass |
| B5 | `./gradlew :test --tests "dev.yanianz.essentials.nicknames.*"` | 11 tests pass |
