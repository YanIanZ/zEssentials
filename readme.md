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
- [x] Framework: reusable `ScreenFactory` with pagination and slot-bound clicks (dev.yanianz.essentials.screens)
- [x] Baltop as a custom screen (/baltopgui [economy], paginated heads)
- [x] Warps as a custom screen (/warpgui, permission filtered)
- [x] Public API: addons reuse ScreenFactory through EssentialsScreens.get().factory()
- [/] Homes and kits already ship zMenu inventory layouts, migrate them on top of the factory later
- [x] Category support: openCategorized(player, title, rows, LinkedHashMap<category, items>) with picker page + back navigation

## Next release candidates (parity inspirations)

Inspired by CMILib (https://github.com/Zrips/CMILib):
- [ ] `/cmi itemname` style advanced item editing (glow, unbreakable, custom model data)
- [ ] Anvil text input reuse for more prompts (home rename, note title)
- [ ] More player placeholders (statistics deep dive, timed commands)
- [ ] `/cmiKit` style kit claim-all button in the preview screen

Inspired by TAB (https://github.com/NEZNAMY/TAB) — parity 1:1 long term:
- [x] Tab list header & footer per world with PlaceholderAPI + refresh interval
- [ ] Tab-list scoreboard system: per world/group headers, footers and layouts
- [x] Nametag prefix/suffix above head via scoreboard teams (per group rules with priority)
- [x] Team based sorting of the tab list (zero padded priority team names, lower priority first)
- [ ] Team based sorting of the tab list (group -> priority ladder)
- [x] Header/footer animation frames cycled on the refresh timer with named `%anim_<name>%` tokens

Inspired by FancyHolograms (https://github.com/FancyMcPlugins/FancyHolograms) — migration of the hologram module:
- [x] Text/item holograms built on display entities (background, shadow, billboard, scale, translation, brightness, visibility)
- [x] Per line editing, rotation, scale, brightness controls
- [x] Autosave + reload without flicker

Small quality features:
- [x] Sleep through night acceleration (percentage based, smooth time speed up)
- [x] `/eat` fills hunger, saturation and stops burning instantly
- [x] `/near <radius>` optional radius argument (1-200 blocks, clamped)
- [x] `/xyz` copies coordinates to clipboard with hover preview
- [ ] `/feed all` alias already exists through `*`
- [ ] `/near <radius>` optional radius argument
- [ ] `/xyz` copies coordinates to clipboard with formatted colors
- [ ] `/rules` alias into terms screen after acceptance flow

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
- [x] Reputation system (/rep <player>, cooldown per giver) & interactive polls (/poll create <s> question | opt1 | opt2, clickable options with % bars)
- [ ] Chat color selection GUI + tags system
- [x] Nicknames with colors (/nick, impersonation guard, per player cooldown, persisted and re-applied on join)
- [x] Raid protection auto-detection (identical spam from N players in a window, staff alert + actions)
- [x] Warning system with auto-escalation (configurable thresholds running console commands)
- [x] Staff notes: /note add <player> <text>, /notes <player>, /notes clear <player> persisted in json
- [x] Report system: /report <player> <reason> with cooldown, staff alerts with teleport click, /reports list with resolve buttons
- [x] Chat bubbles above players: text displays with configurable duration, y offset, stacking, background ARGB and scale
- [x] DiscordSRV outbound bridge (chat forwarded to the main discord channel via reflection, zero hard dependency)
- [x] BungeeCord/Velocity chat relay (modules/bungeechat/config.yml): public chat broadcast through the plugin messaging channel, both ends need the module enabled — messages from other servers keep their colors and display the origin server name

- [x] Command /tpaall
- [x] Command /list
- [x] Command /itemdb
- [x] Add bossbar to message configuration
- [x] Command /vanish
- [x] Command /tpahere
- [x] Command /ci
- [x] Command /exp give, set, show
