package dev.yanianz.essentials.crafting;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.Iterator;

public final class RecipeMatcher {

    private RecipeMatcher() {
    }

    public static ItemStack matchRecipe(ItemStack[] grid) {
        if (grid == null || grid.length != 9) return null;

        boolean hasItem = false;
        for (ItemStack item : grid) {
            if (item != null && !item.getType().isAir()) {
                hasItem = true;
                break;
            }
        }
        if (!hasItem) return null;

        ItemStack[][] matrix = new ItemStack[3][3];
        for (int i = 0; i < 9; i++) {
            matrix[i / 3][i % 3] = grid[i];
        }

        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof org.bukkit.inventory.ShapedRecipe shaped) {
                ItemStack result = tryShaped(shaped, matrix);
                if (result != null) return result;
            } else if (recipe instanceof org.bukkit.inventory.ShapelessRecipe shapeless) {
                ItemStack result = tryShapeless(shapeless, matrix);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static ItemStack tryShaped(org.bukkit.inventory.ShapedRecipe recipe, ItemStack[][] grid) {
        String[] shape = recipe.getShape();
        var map = recipe.getIngredientMap();

        for (int rowOffset = 0; rowOffset <= 3 - shape.length; rowOffset++) {
            for (int colOffset = 0; colOffset <= 3 - shape[0].length(); colOffset++) {
                boolean match = true;
                for (int r = 0; r < shape.length && match; r++) {
                    for (int c = 0; c < shape[r].length() && match; c++) {
                        char key = shape[r].charAt(c);
                        ItemStack expected = map.get(key);
                        ItemStack actual = grid[rowOffset + r][colOffset + c];
                        if (!itemsMatch(expected, actual)) match = false;
                    }
                }
                if (match) {
                    for (int r = 0; r < shape.length; r++) {
                        for (int c = 0; c < shape[r].length(); c++) {
                            if (shape[r].charAt(c) != ' ') continue;
                            if (grid[rowOffset + r][colOffset + c] != null
                                    && !grid[rowOffset + r][colOffset + c].getType().isAir()) {
                                match = false;
                            }
                        }
                    }
                    if (match) return recipe.getResult().clone();
                }
            }
        }
        return null;
    }

    private static ItemStack tryShapeless(org.bukkit.inventory.ShapelessRecipe recipe, ItemStack[][] grid) {
        var ingredients = new java.util.ArrayList<>(recipe.getIngredientList());
        java.util.List<ItemStack> gridItems = new java.util.ArrayList<>();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (grid[r][c] != null && !grid[r][c].getType().isAir()) {
                    gridItems.add(grid[r][c]);
                }
            }
        }
        if (gridItems.size() != ingredients.size()) return null;

        boolean[] used = new boolean[ingredients.size()];
        for (ItemStack gridItem : gridItems) {
            boolean found = false;
            for (int i = 0; i < ingredients.size(); i++) {
                if (used[i]) continue;
                if (itemsMatch(ingredients.get(i), gridItem)) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return null;
        }
        return recipe.getResult().clone();
    }

    private static boolean itemsMatch(ItemStack expected, ItemStack actual) {
        if (expected == null || expected.getType() == Material.AIR) {
            return actual == null || actual.getType().isAir();
        }
        if (actual == null || actual.getType().isAir()) return false;
        return expected.getType() == actual.getType();
    }

    public static int maxCraftable(ItemStack[] grid) {
        ItemStack result = matchRecipe(grid);
        if (result == null) return 0;

        int minCount = Integer.MAX_VALUE;
        for (ItemStack item : grid) {
            if (item == null || item.getType().isAir()) continue;
            minCount = Math.min(minCount, item.getAmount());
        }
        return minCount == Integer.MAX_VALUE ? 0 : minCount;
    }
}
