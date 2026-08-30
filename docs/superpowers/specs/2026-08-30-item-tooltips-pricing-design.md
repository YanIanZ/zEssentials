# Item Tooltips & Pricing — Design Spec

## §G — Goal

Show real-time sell/buy/NPC prices directly in item tooltips inside the
player's inventory, by intercepting `WINDOW_ITEMS` and `SET_SLOT` packets via
ProtocolLib and injecting extra lore lines. Prices come from multiple shop
plugins (RoyaleEconomy, EconomyShopGUI, QuickShop) via a hook system — one
Hook module per supported plugin, aggregated by a `PriceResolver`.

## §C — Context

### Existing infrastructure

- **Hook system**: `Hooks/<Name>/` Gradle modules, auto-included via
  `rootProject.subprojects.filter { it.path.startsWith(":Hooks:") }`.
  Registered in `ZEssentialsPlugin.loadHooks()` via `isPluginEnabled()` checks.
  Pattern: `createInstance("ClassName").ifPresent(obj -> ...)`.
- **ProtocolLib hook**: `Hooks/ProtocolLib/` already exists with
  `PacketListener.registerPackets()` and `PacketChatListener` + `PacketTabLayoutListener`
  using `PacketAdapter`. New packet listeners are added there.
- **Module system**: `ZModuleManager` registers modules; config at
  `modules/<name>/config.yml`. New pricing module goes under
  `dev.yanianz.essentials.pricing.*` (behavior) +
  `fr.maxlego08.essentials.module.modules.PricingModule` (scaffold).
- **Economy system**: `EconomyManager` manages named economies (money, etc.)
  with `Economy` interface. The pricing hooks query shop plugin APIs, not the
  zEssentials economy directly.
- **ColorUtil**: `dev.yanianz.essentials.util.ColorUtil.sections(text)` for
  hex/legacy color conversion.

### Key constraint

Item tooltips on 1.21+ use Adventure Components. The packet carries NBT
components that include the item's lore. Injecting extra lore lines requires
parsing the NBT, appending to the lore list, and re-serializing. ProtocolLib's
`WrapperPlayServerWindowItems` and `WrapperPlayServerSetSlot` provide access
to the ItemStack, which can be modified before the packet is sent.

## §I — Interfaces

### PriceProvider (API, new interface)

```java
package fr.maxlego08.essentials.api.pricing;

public interface PriceProvider {
    String getName();
    boolean isAvailable();

    PriceResult getPrice(org.bukkit.inventory.ItemStack item);

    record PriceResult(
        Double sellPrice,    // what shops pay you for this item
        Double buyPrice,      // what shops charge you for this item
        Double npcPrice,      // NPC vendor sell price
        String source          // "RoyaleEconomy", "EconomyShopGUI", etc.
    ) {
        public boolean hasAny() {
            return sellPrice != null || buyPrice != null || npcPrice != null;
        }
    }
}
```

### PriceResolver (main module)

```java
package dev.yanianz.essentials.pricing;

public class PriceResolverModule extends ZModule {
    List<PriceProvider> providers;  // registered hooks

    PriceProvider.PriceResult resolveBest(ItemStack item) {
        // Query all providers, return the result with the best sell price
        // (highest sell, lowest buy), or null if no provider has a price
    }
}
```

### PacketTooltipListener (ProtocolLib hook, new)

```java
package fr.maxlego08.essentials.hooks.protocollib;

public class PacketTooltipListener extends PacketAdapter implements PacketRegister {
    // Intercepts WINDOW_ITEMS and SET_SLOT packets
    // For each ItemStack in the packet, calls PriceResolver to get prices
    // Appends lore lines to the item's meta before the packet is sent
}
```

### Shop hooks (one per plugin, in Hooks/)

- `Hooks:RoyaleEconomy` — `RoyaleEconomyPriceProvider implements PriceProvider`
- `Hooks:EconomyShopGUI` — `EconomyShopGUIPriceProvider implements PriceProvider`
- `Hooks:QuickShop` — `QuickShopPriceProvider implements PriceProvider`

Each hook:
1. Checks if the plugin is enabled
2. Queries the plugin's API for the item's sell/buy price
3. Returns a `PriceResult` or null if no price found

## §V — Invariants

1. **Tooltips are modified in-transit only** — the actual ItemStack in the
   player's inventory is never modified. The packet carries a modified copy;
   the server-side item stays untouched.
2. **Price injection is idempotent** — if the same packet is processed twice
   (e.g. ProtocolLib re-send), the lore is not duplicated. A marker
   component identifies already-injected lore lines.
3. **Missing plugins degrade gracefully** — if no shop plugin is installed,
   no lore is injected. If a plugin is installed but doesn't know the item,
   that provider returns null and the resolver tries the next.
4. **Price injection is config-toggleable** — `enable: true/false` in
   `modules/pricing/config.yml`. Disabled = no packet listener registered.
5. **Per-player toggle** — players with `essentials.pricing.toggle` can
   run `/pricing` to toggle price display on/off for themselves.
6. **Injected lore uses a distinct visual style** — prefixed with a
   configurable separator line and color, so it's visually distinct from
   the item's native lore.
7. **Price lines are formatted via the economy module's formatter** —
   large numbers use compact formatting (1.5K, 2.3M).
8. **Config is self-healing** — `config-version: 1` key present.

## §T — Tasks

| # | Task | Files | Tests |
|---|------|-------|-------|
| 1 | PriceProvider API interface + PriceResult record | `API/.../api/pricing/PriceProvider.java` | Build passes |
| 2 | PricingModule + config + PriceResolver | `PricingModule.java`, `PriceResolver.java`, `modules/pricing/config.yml` | Build passes |
| 3 | PacketTooltipListener (ProtocolLib hook) | `PacketTooltipListener.java`, `PacketListener.java` | Build passes |
| 4 | RoyaleEconomy hook | `Hooks:RoyaleEconomy/` module | Build passes |
| 5 | EconomyShopGUI hook | `Hooks:EconomyShopGUI/` module | Build passes |
| 6 | QuickShop hook | `Hooks:QuickShop/` module | Build passes |
| 7 | /pricing toggle command + messages | `CommandPricing.java`, `Message.java`, `messages.yml` | Build passes |
| 8 | Changelog + final verification | `changelog.md` | All pass |

## §B — Bugs prevented

| # | Bug | Invariant |
|---|-----|-----------|
| 1 | Items modified permanently, data corruption | §V.1 — packet-only modification |
| 2 | Lore duplicated on re-send | §V.2 — idempotent marker |
| 3 | Crash when shop plugin not installed | §V.3 — graceful degradation |
| 4 | Prices shown even when feature disabled | §V.4 — config toggle |
| 5 | Player can't disable for themselves | §V.5 — per-player toggle |

## Config schema (modules/pricing/config.yml)

```yaml
config-version: 1
enable: true

# Lore lines to inject. %sell%, %buy%, %npc% are price placeholders.
# Lines with null prices are skipped.
lore-lines:
  - ""
  - "&8&m─────────────"
  - "&6Sell: &a%sell%"
  - "&6Buy: &c%buy%"
  - "&6NPC: &e%npc%"

# Separator marker (used for idempotency detection)
marker: "&8&lzE Pricing"

# Per-player toggle permission
toggle-permission: "essentials.pricing.toggle"
```

## Data flow

```
Player opens inventory
  → Server sends WINDOW_ITEMS packet
  → ProtocolLib intercepts (PacketTooltipListener)
  → For each ItemStack in packet:
    → Check: is pricing enabled? Is player's toggle on?
    → Check: does item already have injected marker? Skip if yes.
    → PriceResolver.resolveBest(item) → query all PriceProviders
    → If result has any price:
      → Clone item, append lore lines from config
      → Replace item in packet
  → Packet continues to client with modified tooltips
```

## Shop hook registration

Each shop hook is registered in `loadHooks()`:

```java
if (getServer().getPluginManager().isPluginEnabled("RoyaleEconomy")) {
    createInstance("RoyaleEconomyPriceProvider").ifPresent(obj -> {
        pricingModule.addProvider((PriceProvider) obj);
        this.getLogger().info("Register RoyaleEconomy pricing provider.");
    });
}
```

The `PriceResolver` holds a `List<PriceProvider>` and iterates all registered
providers for each item, returning the best result.
