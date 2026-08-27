# Custom Screens & Screen Factory

Buat GUI kustom tanpa koding: daftarkan di `modules/customscreens/config.yml`
dan layout di `modules/customscreens/screens/<nama>.yml` dengan format zMenu
(items, actions, pagination, PlaceholderAPI).

Perintah: `/screen` (contoh default).

## Screen Factory (untuk developer)

```java
EssentialsScreens.get().factory().open(player, "&bTitle", 6, items);
```
