package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.chest.StoreInAnyChestTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.*;

/**
 * Task to automatically sort items into chests based on categories.
 */
public class ChestSortTask extends Task {
    
    // Define categories for different item types
    private static final Map<String, List<Item>> CATEGORIES = new HashMap<>();
    
    static {
        // Weapons category
        List<Item> weapons = Arrays.asList(
            Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, 
            Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD,
            Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE,
            Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE
        );
        CATEGORIES.put("weapons", weapons);
        
        // Tools category
        List<Item> tools = Arrays.asList(
            Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE,
            Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE,
            Items.WOODEN_SHOVEL, Items.STONE_SHOVEL, Items.IRON_SHOVEL,
            Items.GOLDEN_SHOVEL, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL,
            Items.WOODEN_HOE, Items.STONE_HOE, Items.IRON_HOE,
            Items.GOLDEN_HOE, Items.DIAMOND_HOE, Items.NETHERITE_HOE
        );
        CATEGORIES.put("tools", tools);
        
        // Armor category
        List<Item> armor = Arrays.asList(
            Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS,
            Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS,
            Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
            Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS,
            Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
            Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS
        );
        CATEGORIES.put("armor", armor);
        
        // Food category
        List<Item> food = Arrays.asList(
            Items.APPLE, Items.BREAD, Items.COOKED_BEEF, Items.COOKED_CHICKEN,
            Items.COOKED_PORKCHOP, Items.COOKED_MUTTON, Items.COOKED_RABBIT,
            Items.COOKED_SALMON, Items.COOKED_COD, Items.CARROT, Items.POTATO,
            Items.BAKED_POTATO, Items.MUSHROOM_STEW, Items.BEEF, Items.PORKCHOP,
            Items.CHICKEN, Items.MUTTON, Items.RABBIT, Items.SALMON, Items.COD
        );
        CATEGORIES.put("food", food);
        
        // Ores and raw materials
        List<Item> ores = Arrays.asList(
            Items.RAW_IRON, Items.RAW_GOLD, Items.RAW_COPPER,
            Items.IRON_INGOT, Items.GOLD_INGOT, Items.COPPER_INGOT,
            Items.DIAMOND, Items.EMERALD, Items.NETHERITE_SCRAP,
            Items.COAL, Items.LAPIS_LAZULI, Items.REDSTONE,
            Items.COAL_ORE, Items.IRON_ORE, Items.GOLD_ORE,
            Items.DIAMOND_ORE, Items.EMERALD_ORE, Items.NETHER_GOLD_ORE
        );
        CATEGORIES.put("ores", ores);
        
        // Building materials
        List<Item> building = Arrays.asList(
            Items.COBBLESTONE, Items.STONE, Items.DIRT, Items.GRAVEL,
            Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS,
            Items.JUNGLE_PLANKS, Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS,
            Items.SAND, Items.SANDSTONE, Items.CLAY_BALL, Items.NETHERRACK
        );
        CATEGORIES.put("building", building);
    }
    
    private final boolean _sortAllItems; // Whether to sort all items in inventory or just specific ones
    private final String[] _categoriesToSort; // Specific categories to sort
    
    public ChestSortTask() {
        this(true, new String[0]); // Sort all items by default
    }
    
    public ChestSortTask(boolean sortAllItems, String... categoriesToSort) {
        _sortAllItems = sortAllItems;
        _categoriesToSort = categoriesToSort;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Starting chest sorting task...");
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Get items to sort based on categories
        List<ItemTarget> itemsToSort = getItemsToSort(mod);
        
        if (itemsToSort.isEmpty()) {
            mod.log("No items to sort, chest management complete.");
            return null;
        }
        
        // Create a task to store these items in chests
        ItemTarget[] targets = itemsToSort.toArray(new ItemTarget[0]);
        mod.log("Storing " + itemsToSort.size() + " item types in chests...");
        
        return new StoreInAnyChestTask(targets);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.log("Chest sorting task stopped.");
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof ChestSortTask task) {
            return task._sortAllItems == _sortAllItems && 
                   Arrays.equals(task._categoriesToSort, _categoriesToSort);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        if (_sortAllItems) {
            return "Sorting all items into chests by category";
        } else {
            return "Sorting specific categories into chests: " + String.join(", ", _categoriesToSort);
        }
    }
    
    @Override
    public boolean isFinished(AltoClef mod) {
        // Check if all items to sort have been sorted
        List<ItemTarget> itemsToSort = getItemsToSort(mod);
        return itemsToSort.isEmpty();
    }
    
    /**
     * Get the list of items to sort based on categories and what's in inventory.
     */
    private List<ItemTarget> getItemsToSort(AltoClef mod) {
        List<ItemTarget> itemsToSort = new ArrayList<>();
        
        if (_sortAllItems) {
            // Sort all items in all categories
            for (Map.Entry<String, List<Item>> category : CATEGORIES.entrySet()) {
                for (Item item : category.getValue()) {
                    int itemCount = mod.getInventoryTracker().getItemCount(item);
                    if (itemCount > 0) {
                        itemsToSort.add(new ItemTarget(item, itemCount));
                    }
                }
            }
        } else {
            // Only sort specific categories
            for (String category : _categoriesToSort) {
                List<Item> items = CATEGORIES.get(category.toLowerCase());
                if (items != null) {
                    for (Item item : items) {
                        int itemCount = mod.getInventoryTracker().getItemCount(item);
                        if (itemCount > 0) {
                            itemsToSort.add(new ItemTarget(item, itemCount));
                        }
                    }
                } else {
                    Debug.logWarning("Unknown category: " + category);
                }
            }
        }
        
        return itemsToSort;
    }
}