package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.*;

/**
 * Task to craft items when durability is low.
 * Enhanced with priority system and configurable threshold.
 */
public class CraftDurabilityTask extends Task {
    private final Item _itemToCraft;
    private final int _count;
    
    // Priority map for equipment (higher = more important)
    private static final Map<Item, Integer> PRIORITY_MAP = new HashMap<>();
    static {
        // Armor priority (Netherite > Diamond > Iron > Gold > Leather)
        PRIORITY_MAP.put(Items.NETHERITE_HELMET, 50);
        PRIORITY_MAP.put(Items.NETHERITE_CHESTPLATE, 55);
        PRIORITY_MAP.put(Items.NETHERITE_LEGGINGS, 52);
        PRIORITY_MAP.put(Items.NETHERITE_BOOTS, 48);
        PRIORITY_MAP.put(Items.DIAMOND_HELMET, 40);
        PRIORITY_MAP.put(Items.DIAMOND_CHESTPLATE, 45);
        PRIORITY_MAP.put(Items.DIAMOND_LEGGINGS, 42);
        PRIORITY_MAP.put(Items.DIAMOND_BOOTS, 38);
        PRIORITY_MAP.put(Items.IRON_HELMET, 30);
        PRIORITY_MAP.put(Items.IRON_CHESTPLATE, 35);
        PRIORITY_MAP.put(Items.IRON_LEGGINGS, 32);
        PRIORITY_MAP.put(Items.IRON_BOOTS, 28);
        
        // Weapon priority
        PRIORITY_MAP.put(Items.NETHERITE_SWORD, 60);
        PRIORITY_MAP.put(Items.DIAMOND_SWORD, 50);
        PRIORITY_MAP.put(Items.IRON_SWORD, 40);
        PRIORITY_MAP.put(Items.STONE_SWORD, 30);
        PRIORITY_MAP.put(Items.WOODEN_SWORD, 20);
        
        // Tool priority
        PRIORITY_MAP.put(Items.NETHERITE_PICKAXE, 58);
        PRIORITY_MAP.put(Items.DIAMOND_PICKAXE, 48);
        PRIORITY_MAP.put(Items.IRON_PICKAXE, 38);
        PRIORITY_MAP.put(Items.NETHERITE_AXE, 56);
        PRIORITY_MAP.put(Items.DIAMOND_AXE, 46);
        PRIORITY_MAP.put(Items.IRON_AXE, 36);
        PRIORITY_MAP.put(Items.NETHERITE_SHOVEL, 54);
        PRIORITY_MAP.put(Items.DIAMOND_SHOVEL, 44);
        PRIORITY_MAP.put(Items.IRON_SHOVEL, 34);
    }

    public CraftDurabilityTask(Item itemToCraft) {
        this(itemToCraft, 1);
    }

    public CraftDurabilityTask(Item itemToCraft, int count) {
        _itemToCraft = itemToCraft;
        _count = count;
    }
    
    /**
     * Get priority score for an item based on material and slot
     */
    public static int getPriorityScore(Item item) {
        return PRIORITY_MAP.getOrDefault(item, 10);
    }
    
    /**
     * Calculate durability percentage of an item stack
     */
    public static int getDurabilityPercent(ItemStack stack) {
        if (!stack.isDamageable()) return 100;
        int maxDurability = stack.getMaxDamage();
        int currentDurability = maxDurability - stack.getDamage();
        return (currentDurability * 100) / maxDurability;
    }
    
    /**
     * Check if an item should be repaired based on settings threshold
     */
    public static boolean shouldRepair(ItemStack stack, AltoClef mod) {
        if (!stack.isDamageable()) return false;
        int threshold = mod.getModSettings().getMinDurabilityThreshold();
        int durabilityPercent = getDurabilityPercent(stack);
        return durabilityPercent < threshold;
    }
    
    /**
     * Get all damaged items in player inventory that need repair
     */
    public static List<ItemStack> getDamagedItems(AltoClef mod) {
        List<ItemStack> damaged = new ArrayList<>();
        // Iterate through player inventory slots
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mod.getPlayer().getInventory().getStack(slot);
            if (stack != null && stack.isDamageable() && shouldRepair(stack, mod)) {
                damaged.add(stack);
            }
        }
        return damaged;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Starting durability crafting task for: " + _itemToCraft.getName().getString());
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Create an ItemTarget for the item we want to craft
        ItemTarget target = new ItemTarget(_itemToCraft, _count);

        // Use the existing crafting system to craft the item
        return TaskCatalogue.getItemTask(target);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.log("Stopped durability crafting task for: " + _itemToCraft.getName().getString());
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof CraftDurabilityTask task) {
            return task._itemToCraft == _itemToCraft && task._count == _count;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Crafting " + _count + " " + _itemToCraft.getName().getString() + " due to low durability.";
    }
}