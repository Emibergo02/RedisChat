package dev.unnm3d.redischat.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * API to override the inventory/item/enderchest shown when a player uses the chat tags
 */
public class CustomInventoryAPI {
    private final Map<UUID, ItemStack[]> customInventories = new HashMap<>();
    private final Map<UUID, ItemStack> customItems = new HashMap<>();
    private final Map<UUID, ItemStack[]> customEnderChests = new HashMap<>();
    private final Map<UUID, Integer> customInventorySizes = new HashMap<>();

    /**
     * Set a custom inventory to be displayed when a player uses the inventory tag
     *
     * @param player    The player to set the custom inventory for
     * @param contents  The contents to display, can be null to remove the override
     */
    public void setCustomInventory(@NotNull Player player, @Nullable ItemStack[] contents) {
        if (contents == null) {
            customInventories.remove(player.getUniqueId());
            customInventorySizes.remove(player.getUniqueId());
        } else {
            customInventories.put(player.getUniqueId(), contents.clone());
            // Default size based on contents length, rounded up to nearest multiple of 9
            int size = Math.min(54, (int) Math.ceil(contents.length / 9.0) * 9);
            customInventorySizes.putIfAbsent(player.getUniqueId(), size);
        }
    }

    /**
     * Set a custom inventory with a specific size
     *
     * @param player    The player to set the custom inventory for
     * @param contents  The contents to display, can be null to remove the override
     * @param size      The size of the inventory (must be a multiple of 9, between 9 and 54)
     */
    public void setCustomInventory(@NotNull Player player, @Nullable ItemStack[] contents, int size) {
        if (contents == null) {
            customInventories.remove(player.getUniqueId());
            customInventorySizes.remove(player.getUniqueId());
            return;
        }

        // Validate size
        if (size % 9 != 0 || size < 9 || size > 54) {
            throw new IllegalArgumentException("Inventory size must be a multiple of 9 between 9 and 54");
        }

        // Create a new array with the specified size
        ItemStack[] resizedContents = new ItemStack[size];
        // Copy contents, limited by either the original array length or the new size
        System.arraycopy(contents, 0, resizedContents, 0, Math.min(contents.length, size));

        customInventories.put(player.getUniqueId(), resizedContents);
        customInventorySizes.put(player.getUniqueId(), size);
    }

    /**
     * Set an item at a specific slot in the custom inventory
     *
     * @param player  The player to set the item for
     * @param slot    The slot to set the item at (0-based index)
     * @param item    The item to set, can be null to clear the slot
     */
    public void setCustomInventoryItem(@NotNull Player player, int slot, @Nullable ItemStack item) {
        UUID playerId = player.getUniqueId();

        // If no custom inventory exists yet, create one with default size
        if (!customInventories.containsKey(playerId)) {
            int size = customInventorySizes.getOrDefault(playerId, 27);
            customInventories.put(playerId, new ItemStack[size]);
        }

        ItemStack[] contents = customInventories.get(playerId);

        // Check if the slot is within bounds
        if (slot < 0 || slot >= contents.length) {
            throw new IllegalArgumentException("Slot " + slot + " is outside the inventory bounds (0-" + (contents.length - 1) + ")");
        }

        // Set the item at the specified slot
        contents[slot] = item != null ? item.clone() : null;
    }

    /**
     * Get the custom inventory size for a player
     *
     * @param player  The player to get the inventory size for
     * @return        The custom inventory size, or 27 if not set
     */
    public int getCustomInventorySize(@NotNull Player player) {
        return customInventorySizes.getOrDefault(player.getUniqueId(), 27);
    }

    /**
     * Set the custom inventory size for a player
     *
     * @param player  The player to set the inventory size for
     * @param size    The size to set (must be a multiple of 9 between 9 and 54)
     */
    public void setCustomInventorySize(@NotNull Player player, int size) {
        if (size % 9 != 0 || size < 9 || size > 54) {
            throw new IllegalArgumentException("Inventory size must be a multiple of 9 between 9 and 54");
        }

        UUID playerId = player.getUniqueId();
        customInventorySizes.put(playerId, size);

        // If there's an existing inventory, resize it
        if (customInventories.containsKey(playerId)) {
            ItemStack[] oldContents = customInventories.get(playerId);
            ItemStack[] newContents = new ItemStack[size];

            // Copy the contents from the old array to the new one
            System.arraycopy(oldContents, 0, newContents, 0, Math.min(oldContents.length, size));

            customInventories.put(playerId, newContents);
        }
    }

    /**
     * Set a custom item to be displayed when a player uses the item tag
     *
     * @param player  The player to set the custom item for
     * @param item    The item to display, can be null to remove the override
     */
    public void setCustomItem(@NotNull Player player, @Nullable ItemStack item) {
        if (item == null) {
            customItems.remove(player.getUniqueId());
        } else {
            customItems.put(player.getUniqueId(), item.clone());
        }
    }

    /**
     * Set a custom enderchest to be displayed when a player uses the enderchest tag
     *
     * @param player    The player to set the custom enderchest for
     * @param contents  The contents to display, can be null to remove the override
     */
    public void setCustomEnderChest(@NotNull Player player, @Nullable ItemStack[] contents) {
        if (contents == null) {
            customEnderChests.remove(player.getUniqueId());
        } else {
            customEnderChests.put(player.getUniqueId(), contents.clone());
        }
    }

    /**
     * Check if a player has a custom inventory override
     *
     * @param player  The player to check
     * @return        True if the player has a custom inventory, false otherwise
     */
    public boolean hasCustomInventory(@NotNull Player player) {
        return customInventories.containsKey(player.getUniqueId());
    }

    /**
     * Check if a player has a custom item override
     *
     * @param player  The player to check
     * @return        True if the player has a custom item, false otherwise
     */
    public boolean hasCustomItem(@NotNull Player player) {
        return customItems.containsKey(player.getUniqueId());
    }

    /**
     * Check if a player has a custom enderchest override
     *
     * @param player  The player to check
     * @return        True if the player has a custom enderchest, false otherwise
     */
    public boolean hasCustomEnderChest(@NotNull Player player) {
        return customEnderChests.containsKey(player.getUniqueId());
    }

    /**
     * Get the custom inventory for a player
     *
     * @param player  The player to get the custom inventory for
     * @return        The custom inventory, or null if none is set
     */
    public @Nullable ItemStack[] getCustomInventory(@NotNull Player player) {
        ItemStack[] contents = customInventories.get(player.getUniqueId());
        return contents != null ? contents.clone() : null;
    }

    /**
     * Get the custom item for a player
     *
     * @param player  The player to get the custom item for
     * @return        The custom item, or null if none is set
     */
    public @Nullable ItemStack getCustomItem(@NotNull Player player) {
        ItemStack item = customItems.get(player.getUniqueId());
        return item != null ? item.clone() : null;
    }

    /**
     * Get the custom enderchest for a player
     *
     * @param player  The player to get the custom enderchest for
     * @return        The custom enderchest, or null if none is set
     */
    public @Nullable ItemStack[] getCustomEnderChest(@NotNull Player player) {
        ItemStack[] contents = customEnderChests.get(player.getUniqueId());
        return contents != null ? contents.clone() : null;
    }

    /**
     * Clear all custom inventories/items/enderchests for a player
     *
     * @param player  The player to clear custom content for
     */
    public void clearCustomContent(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        customInventories.remove(playerId);
        customItems.remove(playerId);
        customEnderChests.remove(playerId);
        customInventorySizes.remove(playerId);
    }

    /**
     * Clear all custom inventories/items/enderchests for all players
     */
    public void clearAllCustomContent() {
        customInventories.clear();
        customItems.clear();
        customEnderChests.clear();
        customInventorySizes.clear();
    }

    /**
     * Create a new GUI builder for a player
     *
     * @param player  The player to create the GUI for
     * @return        A new GUI builder
     */
    public GUIBuilder createGUI(@NotNull Player player) {
        return new GUIBuilder(player, this);
    }

    /**
     * Builder class for creating custom GUIs
     */
    public static class GUIBuilder {
        private final Player player;
        private final CustomInventoryAPI api;
        private final ItemStack[] contents;
        private final int size;

        private GUIBuilder(Player player, CustomInventoryAPI api) {
            this(player, api, 27); // Default size
        }

        private GUIBuilder(Player player, CustomInventoryAPI api, int size) {
            this.player = player;
            this.api = api;
            this.size = size;
            this.contents = new ItemStack[size];
        }

        /**
         * Create a new GUI builder with a specific size
         *
         * @param size  The size of the inventory (must be a multiple of 9, between 9 and 54)
         * @return      This builder for chaining
         */
        public GUIBuilder withSize(int size) {
            return new GUIBuilder(player, api, size);
        }

        /**
         * Set an item at a specific slot
         *
         * @param slot  The slot to set (0-based index)
         * @param item  The item to set
         * @return      This builder for chaining
         */
        public GUIBuilder setItem(int slot, ItemStack item) {
            if (slot < 0 || slot >= contents.length) {
                throw new IllegalArgumentException("Slot " + slot + " is outside the inventory bounds (0-" + (contents.length - 1) + ")");
            }
            contents[slot] = item != null ? item.clone() : null;
            return this;
        }

        /**
         * Fill all empty slots with the specified item
         *
         * @param item  The item to fill with
         * @return      This builder for chaining
         */
        public GUIBuilder fillEmpty(ItemStack item) {
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] == null) {
                    contents[i] = item.clone();
                }
            }
            return this;
        }

        /**
         * Fill the borders of the inventory with the specified item
         *
         * @param item  The item to fill with
         * @return      This builder for chaining
         */
        public GUIBuilder fillBorders(ItemStack item) {
            int rows = size / 9;

            // Top and bottom rows
            for (int i = 0; i < 9; i++) {
                contents[i] = item.clone(); // Top row
                contents[(rows - 1) * 9 + i] = item.clone(); // Bottom row
            }

            // Left and right columns (excluding corners that are already filled)
            for (int row = 1; row < rows - 1; row++) {
                contents[row * 9] = item.clone(); // Left column
                contents[row * 9 + 8] = item.clone(); // Right column
            }

            return this;
        }

        /**
         * Fill a row with the specified item
         *
         * @param row   The row to fill (0-based index)
         * @param item  The item to fill with
         * @return      This builder for chaining
         */
        public GUIBuilder fillRow(int row, ItemStack item) {
            if (row < 0 || row >= size / 9) {
                throw new IllegalArgumentException("Row " + row + " is outside the inventory bounds (0-" + (size / 9 - 1) + ")");
            }

            for (int i = 0; i < 9; i++) {
                contents[row * 9 + i] = item.clone();
            }

            return this;
        }

        /**
         * Fill a column with the specified item
         *
         * @param column  The column to fill (0-based index)
         * @param item    The item to fill with
         * @return        This builder for chaining
         */
        public GUIBuilder fillColumn(int column, ItemStack item) {
            if (column < 0 || column >= 9) {
                throw new IllegalArgumentException("Column " + column + " is outside the inventory bounds (0-8)");
            }

            for (int row = 0; row < size / 9; row++) {
                contents[row * 9 + column] = item.clone();
            }

            return this;
        }

        /**
         * Build the GUI and apply it to the player
         */
        public void build() {
            api.setCustomInventory(player, contents, size);
        }
    }
}