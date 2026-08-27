# It is forbidden to fork to create an automatic build of the project! Please read the license of the project !

![info.png](https://img.groupez.dev/zessentials/zess-info.png)

# Links

* Spigot: https://www.spigotmc.org/resources/118014/
* GroupeZ: https://groupez.dev/resources/325
* BuiltByBit: https://builtbybit.com/resources/46047/
* Documentation: https://zessentials.groupez.dev/

# ToDo

Sorted by priority:

## Fixes
- [x] Compile against paper-api 26.2 with adventure 5 compatibility (ClickEvent payload/action)
- [x] Duration parser: 1s/15m/12h/7d/2w/6mo/3y and combinations like 1d12h30m (/fly add, bans...)
- [x] Economy commands accept compact numbers: 1k, 1.5m, 2b, 3t
- [x] Terms of service shown as a native mojang dialog screen on 1.21.7+ servers
- [x] Rework freeze system: separate /freeze and /unfreeze, total immobility, continuous blue particle circle and blue glow
- [x] Fix fly speed stuck at 0 after unfreeze
- [x] Fix effects module silently disabled (yaml loader overwriting runtime fields)
- [x] Fix tpa/rtp having no teleport effect (same root cause)
- [x] Scoreboard crash on modern Paper (fastboard 2.1.5 -> 2.2.1 + customScoresSupported)
- [x] Chat crash `ClickEvent.Action.RUN_COMMAND` missing on new adventure versions

## Visuals & effects
- [x] New console UI/UX: fancy startup/shutdown banners with versions, modules and timings
- [x] Particle effects on teleportation (tpa/warp/spawn/home) and key commands (/fly, /god, /gamemode, /heal...)

## Terms of service module
- [x] Accept screen on first join showing rules/terms (configurable lines, clickable buttons)
- [x] Kick players who refuse or ignore the terms (configurable timeout)
- [x] Accepted players remembered (storage + `/terms reset` admin command)

## Screens integration (custom screen everywhere)
- [ ] Framework: reusable `ScreenFactory` so every list GUI is a zMenu custom screen with configurable layout
- [ ] `/warp` and `/warps` open a custom screen (per-warp icon, category pages)
- [ ] Homes overview as a custom screen (icon per home, favorite pinning)
- [ ] Kits overview as a custom screen
- [ ] Baltop as a custom screen (paginated heads)
- [ ] Public API: addons can register their own screens from code
- [ ] `/terms` GUI variant reusing the custom screens framework

## Chat module v2
- [x] Type [inv]/[inventory] to show your inventory (hover items, click copy)
- [x] Type [ender]/[ec] to show your ender chest
- [x] Type [pos]/[position] to share coordinates (click suggests /tp)
- [ ] Polish `[item]` display, add `[inv]`, `[ender]`, `[pos]` built-in display keywords
- [ ] Custom interactive keywords: config-driven placeholder/hover/click keywords (10 defaults shipped)
- [ ] Interactive player names: hover + click actions on every player name in chat
- [ ] Player mention system: highlight @name, notify target with sound/title/actionbar/bossbar (togglable)
- [ ] Slowmode per player/server with staff bypass
- [x] Message deletion button for staff (click-to-delete own log/history)
- [ ] DND mode, quick replies, emoji shortcuts
- [x] Chat games (6 types): math race, scramble, fast-type, reverse, trivia, hot-letter — auto interval or /chatgames <type|stop|reload>
- [ ] Reputation & polls systems
- [ ] Chat color selection GUI + tags system
- [ ] Nicknames with colors (LuckPerms prefix/suffix used by default when present)
- [ ] Raid protection auto-detection
- [ ] Warning system with auto-escalation + staff notes
- [ ] Report system with GUI review
- [ ] Chat bubbles above players (text displays, culling/fade/stack options)
- [ ] DiscordSRV bridge sync
- [ ] BungeeCord/Velocity network mode

- [x] Command /tpaall
- [x] Command /list
- [x] Command /itemdb
- [x] Add bossbar to message configuration
- [x] Command /vanish
- [x] Command /tpahere
- [x] Command /ci
- [x] Command /exp give, set, show
