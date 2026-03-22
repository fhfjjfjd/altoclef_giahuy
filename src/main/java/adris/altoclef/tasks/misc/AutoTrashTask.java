package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;

/**
 * Task to drop/throw away junk items from inventory.
 * Removes rotten flesh, poisonous potatoes, excess dirt/cobblestone/gravel/seeds.
 */
public class AutoTrashTask extends Task {

    private static final int DIRT_EXCESS = 64;
    private static final int COBBLESTONE_EXCESS = 128;
    private static final int SEEDS_EXCESS = 32;

    private final TimerGame _actionTimer = new TimerGame(0.3);
    private boolean _finished = false;
    private int _itemsTrashed = 0;

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Starting auto-trash: cleaning up inventory...");
        _finished = false;
        _itemsTrashed = 0;
        _actionTimer.forceElapse();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (!_actionTimer.elapsed()) {
            return null;
        }

        // Check and trash pure junk items (drop all)
        if (trashAllOf(mod, Items.ROTTEN_FLESH, "Rotten Flesh")) return null;
        if (trashAllOf(mod, Items.POISONOUS_POTATO, "Poisonous Potato")) return null;
        if (trashAllOf(mod, Items.GRAVEL, "Gravel")) return null;

        // Check and trash excess items (keep some)
        if (trashExcess(mod, Items.DIRT, "Dirt", DIRT_EXCESS)) return null;
        if (trashExcess(mod, Items.COBBLESTONE, "Cobblestone", COBBLESTONE_EXCESS)) return null;
        if (trashExcess(mod, Items.WHEAT_SEEDS, "Seeds", SEEDS_EXCESS)) return null;

        // No more junk found
        if (_itemsTrashed > 0) {
            mod.log("Auto-trash complete. Trashed " + _itemsTrashed + " item stacks.");
        } else {
            mod.log("Auto-trash: no junk items found in inventory.");
        }
        _finished = true;
        return null;
    }

    private boolean trashAllOf(AltoClef mod, Item item, String name) {
        List<Slot> slots = mod.getInventoryTracker().getInventorySlotsWithItem(item);
        if (!slots.isEmpty()) {
            Slot slot = slots.get(0);
            mod.log("Trashing: " + name);
            // Use THROW with button 1 to throw the entire stack
            mod.getSlotHandler().clickSlot(slot, 1, SlotActionType.THROW);
            mod.getSlotHandler().registerSlotAction();
            _actionTimer.reset();
            _itemsTrashed++;
            return true;
        }
        return false;
    }

    private boolean trashExcess(AltoClef mod, Item item, String name, int keepAmount) {
        int count = mod.getInventoryTracker().getItemCount(item);
        if (count > keepAmount) {
            List<Slot> slots = mod.getInventoryTracker().getInventorySlotsWithItem(item);
            if (!slots.isEmpty()) {
                Slot slot = slots.get(slots.size() - 1);
                mod.log("Trashing excess " + name + " (have " + count + ", keeping " + keepAmount + ")");
                // Use THROW with button 1 to throw the entire stack
                mod.getSlotHandler().clickSlot(slot, 1, SlotActionType.THROW);
                mod.getSlotHandler().registerSlotAction();
                _actionTimer.reset();
                _itemsTrashed++;
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.log("Auto-trash stopped. Trashed " + _itemsTrashed + " item stacks.");
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof AutoTrashTask;
    }

    @Override
    protected String toDebugString() {
        return "Auto-trashing junk items, trashed " + _itemsTrashed + " stacks";
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _finished;
    }
}
