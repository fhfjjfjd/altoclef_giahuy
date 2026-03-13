package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import net.minecraft.item.Item;

/**
 * Task to craft items when durability is low.
 */
public class CraftDurabilityTask extends Task {
    private final Item _itemToCraft;
    private final int _count;

    public CraftDurabilityTask(Item itemToCraft) {
        this(itemToCraft, 1);
    }

    public CraftDurabilityTask(Item itemToCraft, int count) {
        _itemToCraft = itemToCraft;
        _count = count;
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