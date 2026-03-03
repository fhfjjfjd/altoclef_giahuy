package adris.altoclef.util;

import adris.altoclef.AltoClef;
import adris.altoclef.trackers.InventoryTracker;
import adris.altoclef.util.slots.Slot;
import net.minecraft.block.BlockState;
import net.minecraft.item.*;
import net.minecraft.util.math.BlockPos;

/**
 * Automatically equips the best tool for the current action (mining/combat).
 * Called each tick from AltoClef.onClientTick().
 */
public class AutoToolEquip {

    private static BlockPos _lastEquippedForBlock = null;

    private static final Item[] WEAPONS_BY_PRIORITY = new Item[]{
            Items.NETHERITE_SWORD, Items.NETHERITE_AXE,
            Items.DIAMOND_SWORD, Items.DIAMOND_AXE,
            Items.IRON_SWORD, Items.IRON_AXE,
            Items.STONE_SWORD, Items.STONE_AXE,
            Items.WOODEN_SWORD, Items.WOODEN_AXE,
            Items.GOLDEN_SWORD, Items.GOLDEN_AXE
    };

    /**
     * Tick called from AltoClef.onClientTick().
     * Auto-equips best tool when mining a block.
     */
    public static void tick(AltoClef mod) {
        if (!AltoClef.inGame() || mod.getPlayer() == null) return;

        // Auto-equip best tool when breaking a block
        if (mod.getControllerExtras().isBreakingBlock()) {
            BlockPos breakPos = mod.getControllerExtras().getBreakingBlockPos();
            if (breakPos != null && !breakPos.equals(_lastEquippedForBlock)) {
                equipBestToolForBlock(mod, breakPos);
                _lastEquippedForBlock = breakPos;
            }
        } else {
            _lastEquippedForBlock = null;
        }
    }

    /**
     * Equip the best tool for mining the block at the given position.
     */
    public static boolean equipBestToolForBlock(AltoClef mod, BlockPos pos) {
        BlockState state = mod.getWorld().getBlockState(pos);
        if (state.isAir()) return false;

        Slot bestSlot = mod.getInventoryTracker().getBestToolSlot(state);
        if (bestSlot != null) {
            ItemStack current = mod.getPlayer().getMainHandStack();
            ItemStack best = mod.getInventoryTracker().getItemStackInSlot(bestSlot);
            // Only swap if the new tool is actually better
            if (current.getItem() != best.getItem()) {
                return mod.getSlotHandler().forceEquipItem(best.getItem());
            }
        }
        return false;
    }

    /**
     * Equip the best weapon for combat (swords + axes, ranked by damage).
     * Returns true if a weapon was equipped.
     */
    public static boolean equipBestWeapon(AltoClef mod) {
        InventoryTracker inv = mod.getInventoryTracker();
        for (Item weapon : WEAPONS_BY_PRIORITY) {
            if (inv.hasItem(weapon)) {
                return mod.getSlotHandler().forceEquipItem(weapon);
            }
        }
        // No weapon found, de-equip tools to avoid durability waste
        return mod.getSlotHandler().forceDeequipHitTool();
    }

    /**
     * Check if the player currently has a weapon equipped.
     */
    public static boolean hasWeaponEquipped(AltoClef mod) {
        Item mainHand = mod.getPlayer().getMainHandStack().getItem();
        return mainHand instanceof SwordItem || mainHand instanceof AxeItem;
    }
}
