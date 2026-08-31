package dev.yanianz.essentials.crafting;

import org.bukkit.inventory.ItemStack;

/**
 * Per-player crafting state used by the zMenu crafting GUI. The grid keeps
 * raw ItemStacks placed by the player; the result is computed on demand
 * through the shared recipe matcher.
 */
public class CraftingSession {

    private final boolean quickCraftAllowed;
    private final ItemStack[] grid = new ItemStack[9];

    public CraftingSession(boolean quickCraftAllowed) {
        this.quickCraftAllowed = quickCraftAllowed;
    }

    public boolean isQuickCraftAllowed() {
        return quickCraftAllowed;
    }

    public ItemStack getGrid(int index) {
        return grid[index];
    }

    public void setGrid(int index, ItemStack item) {
        grid[index] = item == null || item.getType().isAir() ? null : item;
    }

    public ItemStack[] gridSnapshot() {
        return grid.clone();
    }

    public ItemStack computeResult() {
        return RecipeMatcher.matchRecipe(gridSnapshot());
    }

    public int maxCraftable() {
        return RecipeMatcher.maxCraftable(gridSnapshot());
    }
}
