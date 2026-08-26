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
- [x] Scoreboard crash on modern Paper (fastboard 2.1.5 -> 2.2.1 + customScoresSupported)
- [x] Chat crash `ClickEvent.Action.RUN_COMMAND` missing on new adventure versions

## Visuals & effects
- [x] New console UI/UX: fancy startup/shutdown banners with versions, modules and timings
- [x] Particle effects on teleportation (tpa/warp/spawn/home) and key commands (/fly, /god, /gamemode, /heal...)

## Terms of service module
- [x] Accept screen on first join showing rules/terms (configurable lines, clickable buttons)
- [x] Kick players who refuse or ignore the terms (configurable timeout)
- [x] Accepted players remembered (storage + `/terms reset` admin command)

## Chat module v2
- [ ] Polish `[item]` display, add `[inv]`, `[ender]`, `[pos]` built-in display keywords
- [ ] Custom interactive keywords: config-driven placeholder/hover/click keywords (10 defaults shipped)
- [ ] Interactive player names: hover + click actions on every player name in chat
- [ ] Player mention system: highlight @name, notify target with sound/title/actionbar/bossbar (togglable)
- [ ] Slowmode per player/server with staff bypass
- [ ] Message deletion button for staff (click-to-delete own log/history)
- [ ] DND mode, quick replies, emoji shortcuts
- [ ] Chat games (6 types) with rewards
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
