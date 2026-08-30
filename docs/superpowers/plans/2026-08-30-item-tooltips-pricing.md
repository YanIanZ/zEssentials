# Item Tooltips & Pricing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show real-time sell/buy/NPC prices from shop plugins directly in inventory item tooltips via ProtocolLib packet interception.

**Architecture:** A `PriceProvider` API interface lets each shop plugin hook report prices. A `PriceResolver` in the main module aggregates all providers and returns the best result. A `PacketTooltipListener` in the ProtocolLib hook intercepts `WINDOW_ITEMS`/`SET_SLOT` packets, queries the resolver, and injects lore lines into item tooltips before they reach the client. The actual server-side item is never modified.

**Tech Stack:** Java 21, ProtocolLib (packet interception), Bukkit/Paper API, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-08-30-item-tooltips-pricing-design.md`

## Global Constraints

- Java 21 bytecode target. Paper API is `compileOnly`.
- No comments in code unless explicitly requested.
- Folia-supported: use `plugin.getScheduler()` for async work.
- Config self-healing: `config-version: 1` key in module config.
- Package convention: feature logic in `dev.yanianz.essentials.pricing.*`, module scaffold in `fr.maxlego08.essentials.module.modules.PricingModule`.
- Hook modules go in `Hooks/<Name>/` with `compileOnly` dependencies on the target plugin.
- Build: `./gradlew build -x test --console=plain`
- Test: `./gradlew test --console=plain --no-daemon`
- Working directory: `/Users/rheninxy/Sourby/zEssentials`
- Bazaar prices are OUT OF SCOPE (postponed). Only local shop plugin prices.

---

## File Structure

| File | Responsibility |
|------|---------------|
| `API/.../api/pricing/PriceProvider.java` | Interface for shop price providers + `PriceResult` record |
| `src/main/java/dev/yanianz/essentials/pricing/PriceResolver.java` | Aggregates providers, returns best price |
| `src/main/java/fr/maxlego08/essentials/module/modules/PricingModule.java` | ZModule scaffold, config, provider list |
| `src/main/resources/modules/pricing/config.yml` | Config: enable, lore lines, marker, toggle permission |
| `Hooks/ProtocolLib/.../PacketTooltipListener.java` | Intercepts WINDOW_ITEMS/SET_SLOT, injects lore |
| `Hooks/ProtocolLib/.../PacketListener.java` | Register the new tooltip listener |
| `Hooks/RoyaleEconomy/` | RoyaleEconomy price provider hook |
| `Hooks/EconomyShopGUI/` | EconomyShopGUI price provider hook |
| `Hooks/QuickShop/` | QuickShop price provider hook |
| `src/main/java/dev/yanianz/essentials/pricing/CommandPricing.java` | /pricing toggle command |
| `API/.../messages/Message.java` | New messages |
| `src/main/resources/messages/messages.yml` | New message keys |
| `settings.gradle.kts` | Include new hook modules |
| `src/main/java/fr/maxlego08/essentials/module/ZModuleManager.java` | Register PricingModule |
| `src/main/java/fr/maxlego08/essentials/commands/CommandLoader.java` | Register /pricing command |
| `src/main/java/fr/maxlego08/essentials/ZEssentialsPlugin.java` | Register shop hooks in loadHooks() |

---

### Task 1: PriceProvider API Interface

**Files:**
- Create: `API/src/main/java/fr/maxlego08/essentials/api/pricing/PriceProvider.java`

**Interfaces:**
- Produces: `PriceProvider` interface, `PriceProvider.PriceResult` record

- [ ] **Step 1: Create PriceProvider interface**

```java
package fr.maxlego08.essentials.api.pricing;

import org.bukkit.inventory.ItemStack;

public interface PriceProvider {

    String getName();

    boolean isAvailable();

    PriceResult getPrice(ItemStack item);

    record PriceResult(
        Double sellPrice,
        Double buyPrice,
        Double npcPrice,
        String source
    ) {
        public boolean hasAny() {
            return sellPrice != null || buyPrice != null || npcPrice != null;
        }
    }
}
```

- [ ] **Step 2: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add API/src/main/java/fr/maxlego08/essentials/api/pricing/PriceProvider.java
git commit -m "feat(pricing): PriceProvider API interface + PriceResult record"
```

---

### Task 2: PricingModule + Config + PriceResolver

**Files:**
- Create: `src/main/resources/modules/pricing/config.yml`
- Create: `src/main/java/dev/yanianz/essentials/pricing/PriceResolver.java`
- Create: `src/main/java/fr/maxlego08/essentials/module/modules/PricingModule.java`
- Modify: `src/main/java/fr/maxlego08/essentials/module/ZModuleManager.java`

**Interfaces:**
- Consumes: `PriceProvider`, `PriceResult`
- Produces: `PricingModule.addProvider(PriceProvider)`, `PricingModule.resolvePrice(ItemStack)`, `PricingModule.isEnabled()`, `PricingModule.isToggleEnabled(UUID)`, `PricingModule.togglePlayer(UUID)`, config getters

- [ ] **Step 1: Create config file**

```yaml
########################################################################################################################
#
# zEssentials - Item Tooltips & Pricing
# Shows real-time sell/buy/NPC prices from shop plugins in item tooltips.
# Requires ProtocolLib for packet interception.
#
########################################################################################################################

# Config schema version — do not edit
config-version: 1

enable: true

# Lore lines to inject into item tooltips.
# %sell% = best sell price, %buy% = best buy price, %npc% = NPC sell price
# Lines with null prices are skipped (the line is not shown).
lore-lines:
  - ""
  - "&8&m─────────────"
  - "&6Sell: &a%sell%"
  - "&6Buy: &c%buy%"
  - "&6NPC: &e%npc%"

# Separator marker used for idempotency (prevents lore duplication on re-send)
marker: "&8&lzE Pricing"

# Per-player toggle permission
toggle-permission: "essentials.pricing.toggle"
```

- [ ] **Step 2: Create PriceResolver**

```java
package dev.yanianz.essentials.pricing;

import fr.maxlego08.essentials.api.pricing.PriceProvider;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PriceResolver {

    private final List<PriceProvider> providers;

    public PriceResolver(List<PriceProvider> providers) {
        this.providers = providers;
    }

    public PriceProvider.PriceResult resolveBest(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;

        PriceProvider.PriceResult best = null;
        double bestSell = Double.MIN_VALUE;
        double bestBuy = Double.MAX_VALUE;
        Double npcPrice = null;

        for (PriceProvider provider : providers) {
            if (!provider.isAvailable()) continue;
            PriceProvider.PriceResult result = provider.getPrice(item);
            if (result == null || !result.hasAny()) continue;

            if (result.sellPrice() != null && result.sellPrice() > bestSell) {
                bestSell = result.sellPrice();
            }
            if (result.buyPrice() != null && result.buyPrice() < bestBuy) {
                bestBuy = result.buyPrice();
            }
            if (result.npcPrice() != null) {
                npcPrice = result.npcPrice();
            }
        }

        Double sell = bestSell == Double.MIN_VALUE ? null : bestSell;
        Double buy = bestBuy == Double.MAX_VALUE ? null : bestBuy;

        if (sell == null && buy == null && npcPrice == null) return null;

        return new PriceProvider.PriceResult(sell, buy, npcPrice, "resolved");
    }
}
```

- [ ] **Step 3: Create PricingModule**

```java
package fr.maxlego08.essentials.module.modules;

import dev.yanianz.essentials.pricing.PriceResolver;
import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.configuration.NonLoadable;
import fr.maxlego08.essentials.api.pricing.PriceProvider;
import fr.maxlego08.essentials.module.ZModule;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PricingModule extends ZModule {

    private boolean enabled = true;
    private List<String> loreLines = new ArrayList<>();
    private String marker = "&8&lzE Pricing";
    private String togglePermission = "essentials.pricing.toggle";
    private PriceResolver resolver;

    @NonLoadable
    private final List<PriceProvider> providers = new ArrayList<>();

    @NonLoadable
    private final Set<UUID> disabledPlayers = new HashSet<>();

    public PricingModule(ZEssentialsPlugin plugin) {
        super(plugin, "pricing");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        YamlConfiguration config = getConfiguration();
        this.enabled = config.getBoolean("enable", true);
        this.loreLines = config.getStringList("lore-lines");
        this.marker = config.getString("marker", "&8&lzE Pricing");
        this.togglePermission = config.getString("toggle-permission", "essentials.pricing.toggle");

        this.resolver = new PriceResolver(this.providers);
    }

    public void addProvider(PriceProvider provider) {
        this.providers.add(provider);
        this.resolver = new PriceResolver(this.providers);
    }

    public PriceProvider.PriceResult resolvePrice(ItemStack item) {
        if (!this.enabled || this.resolver == null) return null;
        return this.resolver.resolveBest(item);
    }

    public boolean isEnabled() {
        return this.enabled && this.isEnable;
    }

    public boolean isToggleEnabled(UUID playerId) {
        return !this.disabledPlayers.contains(playerId);
    }

    public boolean togglePlayer(UUID playerId) {
        if (this.disabledPlayers.contains(playerId)) {
            this.disabledPlayers.remove(playerId);
            return true;
        } else {
            this.disabledPlayers.add(playerId);
            return false;
        }
    }

    public List<String> getLoreLines() { return loreLines; }
    public String getMarker() { return marker; }
    public String getTogglePermission() { return togglePermission; }
    public List<PriceProvider> getProviders() { return providers; }
}
```

- [ ] **Step 4: Register module in ZModuleManager**

In `ZModuleManager.loadModules()`, add before `this.loadConfigurations()`:

```java
        this.modules.put(PricingModule.class, new PricingModule(this.plugin));
```

Add import: `import fr.maxlego08.essentials.module.modules.PricingModule;`

- [ ] **Step 5: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/modules/pricing/config.yml \
  src/main/java/dev/yanianz/essentials/pricing/PriceResolver.java \
  src/main/java/fr/maxlego08/essentials/module/modules/PricingModule.java \
  src/main/java/fr/maxlego08/essentials/module/ZModuleManager.java
git commit -m "feat(pricing): module scaffold, config, price resolver"
```

---

### Task 3: PacketTooltipListener (ProtocolLib hook)

**Files:**
- Create: `Hooks/ProtocolLib/src/main/java/fr/maxlego08/essentials/hooks/protocollib/PacketTooltipListener.java`
- Modify: `Hooks/ProtocolLib/src/main/java/fr/maxlego08/essentials/hooks/protocollib/PacketListener.java`

**Interfaces:**
- Consumes: `PricingModule.resolvePrice()`, `PricingModule.getLoreLines()`, `PricingModule.getMarker()`, `PricingModule.isToggleEnabled()`, `PriceResult`
- Produces: `PacketTooltipListener` registered in `PacketListener`

- [ ] **Step 1: Create PacketTooltipListener**

```java
package fr.maxlego08.essentials.hooks.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.pricing.PriceProvider;
import fr.maxlego08.essentials.api.packet.PacketRegister;
import fr.maxlego08.essentials.module.modules.PricingModule;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PacketTooltipListener extends PacketAdapter implements PacketRegister {

    private final EssentialsPlugin plugin;

    public PacketTooltipListener(EssentialsPlugin plugin) {
        super(PacketAdapter.params()
                .plugin(plugin)
                .listenerPriority(ListenerPriority.NORMAL)
                .types(PacketType.Play.Server.WINDOW_ITEMS, PacketType.Play.Server.SET_SLOT));
        this.plugin = plugin;
    }

    @Override
    public void addPacketListener() {
        com.comphenix.protocol.ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PricingModule module = plugin.getModuleManager().getModule(PricingModule.class);
        if (module == null || !module.isEnabled()) return;

        Player player = event.getPlayer();
        if (!module.isToggleEnabled(player.getUniqueId())) return;

        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            List<ItemStack> items = event.getPacket().getItemListModifier().read(0);
            if (items == null) return;
            for (int i = 0; i < items.size(); i++) {
                ItemStack modified = injectPrice(items.get(i), module, player);
                if (modified != null) items.set(i, modified);
            }
        } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            ItemStack item = event.getPacket().getItemModifier().read(0);
            if (item == null) return;
            ItemStack modified = injectPrice(item, module, player);
            if (modified != null) event.getPacket().getItemModifier().write(0, modified);
        }
    }

    private ItemStack injectPrice(ItemStack item, PricingModule module, Player player) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        String marker = module.getMarker();
        if (lore.stream().anyMatch(line -> line.contains(marker))) return null;

        PriceProvider.PriceResult result = module.resolvePrice(item);
        if (result == null || !result.hasAny()) return null;

        List<String> injectedLines = buildLoreLines(module.getLoreLines(), result);
        if (injectedLines.isEmpty()) return null;

        lore.addAll(injectedLines);
        meta.setLore(lore);
        ItemStack clone = item.clone();
        clone.setItemMeta(meta);
        return clone;
    }

    private List<String> buildLoreLines(List<String> template, PriceProvider.PriceResult result) {
        List<String> lines = new ArrayList<>();
        for (String line : template) {
            String formatted = line;
            boolean hasPrice = false;

            if (result.sellPrice() != null) {
                formatted = formatted.replace("%sell%", formatPrice(result.sellPrice()));
                if (line.contains("%sell%")) hasPrice = true;
            } else {
                if (line.contains("%sell%")) continue;
            }

            if (result.buyPrice() != null) {
                formatted = formatted.replace("%buy%", formatPrice(result.buyPrice()));
                if (line.contains("%buy%")) hasPrice = true;
            } else {
                if (line.contains("%buy%")) continue;
            }

            if (result.npcPrice() != null) {
                formatted = formatted.replace("%npc%", formatPrice(result.npcPrice()));
                if (line.contains("%npc%")) hasPrice = true;
            } else {
                if (line.contains("%npc%")) continue;
            }

            formatted = formatted.replace("&", "§");
            lines.add(formatted);
        }
        return lines;
    }

    private String formatPrice(double price) {
        if (price >= 1_000_000_000) return String.format(java.util.Locale.US, "%.1fB", price / 1_000_000_000);
        if (price >= 1_000_000) return String.format(java.util.Locale.US, "%.1fM", price / 1_000_000);
        if (price >= 1_000) return String.format(java.util.Locale.US, "%.1fK", price / 1_000);
        return String.format(java.util.Locale.US, "%.2f", price);
    }
}
```

- [ ] **Step 2: Register in PacketListener**

In `PacketListener.registerPackets()`, add:

```java
        this.register(new PacketTooltipListener(plugin));
```

Add import: `import fr.maxlego08.essentials.hooks.protocollib.PacketTooltipListener;`

- [ ] **Step 3: Fix ProtocolLib hook paper-api version**

In `Hooks/ProtocolLib/build.gradle.kts`, change `1.21.5-R0.1-SNAPSHOT` to `26.2.build.119-stable`:

```kotlin
    compileOnly("io.papermc.paper:paper-api:26.2.build.119-stable")
```

- [ ] **Step 4: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add Hooks/ProtocolLib/src/main/java/fr/maxlego08/essentials/hooks/protocollib/PacketTooltipListener.java \
  Hooks/ProtocolLib/src/main/java/fr/maxlego08/essentials/hooks/protocollib/PacketListener.java \
  Hooks/ProtocolLib/build.gradle.kts
git commit -m "feat(pricing): ProtocolLib tooltip injection listener"
```

---

### Task 4: Shop Hook Stubs (RoyaleEconomy, EconomyShopGUI, QuickShop)

This task creates the three hook modules as stubs. Each hook has a `PriceProvider` implementation that returns null for now (the actual price querying logic depends on each plugin's specific API, which may need runtime testing to verify). The stubs register in `loadHooks()` and build successfully.

**Files:**
- Create: `Hooks/RoyaleEconomy/build.gradle.kts`
- Create: `Hooks/RoyaleEconomy/src/main/java/fr/maxlego08/essentials/hooks/pricing/RoyaleEconomyPriceProvider.java`
- Create: `Hooks/EconomyShopGUI/build.gradle.kts`
- Create: `Hooks/EconomyShopGUI/src/main/java/fr/maxlego08/essentials/hooks/pricing/EconomyShopGUIPriceProvider.java`
- Create: `Hooks/QuickShop/build.gradle.kts`
- Create: `Hooks/QuickShop/src/main/java/fr/maxlego08/essentials/hooks/pricing/QuickShopPriceProvider.java`
- Modify: `settings.gradle.kts`
- Modify: `src/main/java/fr/maxlego08/essentials/ZEssentialsPlugin.java`

- [ ] **Step 1: Add hook modules to settings.gradle.kts**

Add after the existing Hooks includes:

```kotlin
include("Hooks:RoyaleEconomy")
include("Hooks:EconomyShopGUI")
include("Hooks:QuickShop")
```

- [ ] **Step 2: Create RoyaleEconomy hook**

`Hooks/RoyaleEconomy/build.gradle.kts`:
```kotlin
group = "Hooks:RoyaleEconomy"

dependencies {
    compileOnly(project(":API"))
    compileOnly("io.papermc.paper:paper-api:26.2.build.119-stable")
}
```

`Hooks/RoyaleEconomy/src/main/java/fr/maxlego08/essentials/hooks/pricing/RoyaleEconomyPriceProvider.java`:
```java
package fr.maxlego08.essentials.hooks.pricing;

import fr.maxlego08.essentials.api.pricing.PriceProvider;
import org.bukkit.inventory.ItemStack;

public class RoyaleEconomyPriceProvider implements PriceProvider {

    @Override
    public String getName() {
        return "RoyaleEconomy";
    }

    @Override
    public boolean isAvailable() {
        return org.bukkit.Bukkit.getPluginManager().isPluginEnabled("RoyaleEconomy");
    }

    @Override
    public PriceResult getPrice(ItemStack item) {
        return null;
    }
}
```

- [ ] **Step 3: Create EconomyShopGUI hook**

`Hooks/EconomyShopGUI/build.gradle.kts`:
```kotlin
group = "Hooks:EconomyShopGUI"

dependencies {
    compileOnly(project(":API"))
    compileOnly("io.papermc.paper:paper-api:26.2.build.119-stable")
}
```

`Hooks/EconomyShopGUI/src/main/java/fr/maxlego08/essentials/hooks/pricing/EconomyShopGUIPriceProvider.java`:
```java
package fr.maxlego08.essentials.hooks.pricing;

import fr.maxlego08.essentials.api.pricing.PriceProvider;
import org.bukkit.inventory.ItemStack;

public class EconomyShopGUIPriceProvider implements PriceProvider {

    @Override
    public String getName() {
        return "EconomyShopGUI";
    }

    @Override
    public boolean isAvailable() {
        return org.bukkit.Bukkit.getPluginManager().isPluginEnabled("EconomyShopGUI");
    }

    @Override
    public PriceResult getPrice(ItemStack item) {
        return null;
    }
}
```

- [ ] **Step 4: Create QuickShop hook**

`Hooks/QuickShop/build.gradle.kts`:
```kotlin
group = "Hooks:QuickShop"

dependencies {
    compileOnly(project(":API"))
    compileOnly("io.papermc.paper:paper-api:26.2.build.119-stable")
}
```

`Hooks/QuickShop/src/main/java/fr/maxlego08/essentials/hooks/pricing/QuickShopPriceProvider.java`:
```java
package fr.maxlego08.essentials.hooks.pricing;

import fr.maxlego08.essentials.api.pricing.PriceProvider;
import org.bukkit.inventory.ItemStack;

public class QuickShopPriceProvider implements PriceProvider {

    @Override
    public String getName() {
        return "QuickShop";
    }

    @Override
    public boolean isAvailable() {
        return org.bukkit.Bukkit.getPluginManager().isPluginEnabled("QuickShop")
                || org.bukkit.Bukkit.getPluginManager().isPluginEnabled("QuickShop-Hikari");
    }

    @Override
    public PriceResult getPrice(ItemStack item) {
        return null;
    }
}
```

- [ ] **Step 5: Register hooks in loadHooks()**

In `ZEssentialsPlugin.loadHooks()`, add after the existing hooks:

```java
        fr.maxlego08.essentials.module.modules.PricingModule pricingModule =
                this.moduleManager.getModule(fr.maxlego08.essentials.module.modules.PricingModule.class);

        if (pricingModule != null && pricingModule.isEnabled()) {
            if (getServer().getPluginManager().isPluginEnabled("RoyaleEconomy")) {
                createInstance("RoyaleEconomyPriceProvider").ifPresent(obj -> {
                    pricingModule.addProvider((fr.maxlego08.essentials.api.pricing.PriceProvider) obj);
                    this.getLogger().info("Register RoyaleEconomy pricing provider.");
                });
            }
            if (getServer().getPluginManager().isPluginEnabled("EconomyShopGUI")) {
                createInstance("EconomyShopGUIPriceProvider").ifPresent(obj -> {
                    pricingModule.addProvider((fr.maxlego08.essentials.api.pricing.PriceProvider) obj);
                    this.getLogger().info("Register EconomyShopGUI pricing provider.");
                });
            }
            if (getServer().getPluginManager().isPluginEnabled("QuickShop")
                    || getServer().getPluginManager().isPluginEnabled("QuickShop-Hikari")) {
                createInstance("QuickShopPriceProvider").ifPresent(obj -> {
                    pricingModule.addProvider((fr.maxlego08.essentials.api.pricing.PriceProvider) obj);
                    this.getLogger().info("Register QuickShop pricing provider.");
                });
            }
        }
```

- [ ] **Step 6: Build to verify compilation**

Run: `./gradlew build -x test --console=plain`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Run all tests**

Run: `./gradlew test --console=plain --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add Hooks/RoyaleEconomy/ Hooks/EconomyShopGUI/ Hooks/QuickShop/ \
  settings.gradle.kts src/main/java/fr/maxlego08/essentials/ZEssentialsPlugin.java
git commit -m "feat(pricing): shop hook stubs (RoyaleEconomy, EconomyShopGUI, QuickShop)"
```

---

### Task 5: /pricing Toggle Command + Messages + Changelog

**Files:**
- Create: `src/main/java/dev/yanianz/essentials/pricing/CommandPricing.java`
- Modify: `API/src/main/java/fr/maxlego08/essentials/api/messages/Message.java`
- Modify: `src/main/resources/messages/messages.yml`
- Modify: `src/main/java/fr/maxlego08/essentials/commands/CommandLoader.java`
- Modify: `changelog.md`

- [ ] **Step 1: Add Message enum entries**

In `Message.java`, add:

```java
    COMMAND_PRICING_ENABLED("<success>Item pricing tooltips are now enabled."),
    COMMAND_PRICING_DISABLED("<error>Item pricing tooltips are now disabled."),
    DESCRIPTION_PRICING("Toggle item pricing tooltips"),
```

- [ ] **Step 2: Add messages.yml entries**

```yaml
command-pricing-enabled: "<success>Item pricing tooltips are now enabled."
command-pricing-disabled: "<error>Item pricing tooltips are now disabled."
description-pricing: "Toggle item pricing tooltips"
```

- [ ] **Step 3: Create CommandPricing**

```java
package dev.yanianz.essentials.pricing;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.module.modules.PricingModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

public class CommandPricing extends VCommand {

    public CommandPricing(EssentialsPlugin plugin) {
        super(plugin);
        this.setModule(PricingModule.class);
        this.setPermission(Permission.ESSENTIALS_USE);
        this.setDescription(Message.DESCRIPTION_PRICING);
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        PricingModule module = plugin.getModuleManager().getModule(PricingModule.class);
        if (module == null) return CommandResultType.SUCCESS;

        boolean enabled = module.togglePlayer(this.player.getUniqueId());
        message(this.sender, enabled ? Message.COMMAND_PRICING_ENABLED : Message.COMMAND_PRICING_DISABLED);
        return CommandResultType.SUCCESS;
    }
}
```

- [ ] **Step 4: Register command in CommandLoader**

Add:

```java
        register("pricing", CommandPricing.class);
```

Add import: `import dev.yanianz.essentials.pricing.CommandPricing;`

- [ ] **Step 5: Build + test**

Run: `./gradlew build --console=plain --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Add changelog entry**

In `changelog.md`, under `# 1.2.0.0`, add:

```markdown
## Item Tooltips & Pricing

- **Real-time price tooltips** — sell/buy/NPC prices from shop plugins shown directly in inventory item tooltips via ProtocolLib packet interception
- **Multi-shop support** — separate hooks for RoyaleEconomy, EconomyShopGUI, QuickShop; PriceResolver aggregates all active providers and returns the best price
- **Packet-only modification** — actual server-side items are never modified; tooltips are injected in-transit only
- **Idempotent injection** — marker component prevents lore duplication on packet re-send
- **Per-player toggle** — `/pricing` command lets players turn price display on/off for themselves
- **Config-driven** — lore line templates, marker, toggle permission all in `modules/pricing/config.yml`
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/dev/yanianz/essentials/pricing/CommandPricing.java \
  API/src/main/java/fr/maxlego08/essentials/api/messages/Message.java \
  src/main/resources/messages/messages.yml \
  src/main/java/fr/maxlego08/essentials/commands/CommandLoader.java \
  changelog.md
git commit -m "feat(pricing): /pricing toggle command, messages, changelog"
```
