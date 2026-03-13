package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.SmeltingRecipe;
import adris.altoclef.tasks.SmeltInFurnaceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.SmeltTarget;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Task to automatically smelt raw materials in the inventory.
 */
public class AutoSmeltTask extends Task {
    
    // Define common smelting recipes
    private static final SmeltTarget[] SMELTING_RECIPES = {
        new SmeltTarget(new ItemTarget(Items.IRON_INGOT, 1), new ItemTarget(Items.RAW_IRON, 1)),
        new SmeltTarget(new ItemTarget(Items.GOLD_INGOT, 1), new ItemTarget(Items.RAW_GOLD, 1)),
        new SmeltTarget(new ItemTarget(Items.COPPER_INGOT, 1), new ItemTarget(Items.RAW_COPPER, 1)),
        new SmeltTarget(new ItemTarget(Items.COAL, 1), new ItemTarget(Items.COAL_ORE, 1)),
        new SmeltTarget(new ItemTarget(Items.DIAMOND, 1), new ItemTarget(Items.DIAMOND_ORE, 1)),
        new SmeltTarget(new ItemTarget(Items.EMERALD, 1), new ItemTarget(Items.EMERALD_ORE, 1)),
        new SmeltTarget(new ItemTarget(Items.NETHERITE_SCRAP, 1), new ItemTarget(Items.ANCIENT_DEBRIS, 1)),
        new SmeltTarget(new ItemTarget(Items.STONE, 1), new ItemTarget(Items.COBBLESTONE, 1)),
        new SmeltTarget(new ItemTarget(Items.GLASS, 1), new ItemTarget(Items.SAND, 1)),
        new SmeltTarget(new ItemTarget(Items.COOKED_PORKCHOP, 1), new ItemTarget(Items.PORKCHOP, 1)),
        new SmeltTarget(new ItemTarget(Items.COOKED_BEEF, 1), new ItemTarget(Items.BEEF, 1)),
        new SmeltTarget(new ItemTarget(Items.COOKED_CHICKEN, 1), new ItemTarget(Items.CHICKEN, 1)),
        new SmeltTarget(new ItemTarget(Items.COOKED_MUTTON, 1), new ItemTarget(Items.MUTTON, 1)),
        new SmeltTarget(new ItemTarget(Items.COOKED_RABBIT, 1), new ItemTarget(Items.RABBIT, 1)),
        new SmeltTarget(new ItemTarget(Items.BREAD, 1), new ItemTarget(Items.WHEAT, 1))
    };
    
    private SmeltTarget _currentSmeltingTarget = null;
    private Task _currentSmeltingTask = null;
    private final boolean _smeltAll; // Whether to smelt all materials or just until target counts are met

    public AutoSmeltTask() {
        this(false); // Default to smelt until targets are met
    }
    
    public AutoSmeltTask(boolean smeltAll) {
        _smeltAll = smeltAll;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Starting auto-smelt task...");
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // If we're currently smelting something, continue that task
        if (_currentSmeltingTask != null && !_currentSmeltingTask.isFinished(mod)) {
            return _currentSmeltingTask;
        }
        
        // Find the next material to smelt
        SmeltTarget nextTarget = findNextSmeltingTarget(mod);
        if (nextTarget != null) {
            mod.log("Auto-smelting: " + nextTarget.getMaterial().getMatches()[0].getName().getString() + 
                   " -> " + nextTarget.getItem().getMatches()[0].getName().getString());
            
            _currentSmeltingTarget = nextTarget;
            _currentSmeltingTask = new SmeltInFurnaceTask(_currentSmeltingTarget);
            return _currentSmeltingTask;
        }
        
        // No more materials to smelt
        mod.log("No more materials to smelt.");
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.log("Auto-smelt task stopped.");
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof AutoSmeltTask task) {
            return task._smeltAll == _smeltAll;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        if (_currentSmeltingTarget != null) {
            return "Auto-smelting " + _currentSmeltingTarget.getMaterial().getMatches()[0].getName().getString();
        } else {
            return "Searching for materials to smelt...";
        }
    }
    
    @Override
    public boolean isFinished(AltoClef mod) {
        // If we're smelting all materials, we run until stopped
        // If we're smelting to meet targets, we finish when no more materials to smelt
        return !_smeltAll && findNextSmeltingTarget(mod) == null;
    }
    
    /**
     * Find the next material to smelt based on what's available in inventory.
     */
    private SmeltTarget findNextSmeltingTarget(AltoClef mod) {
        for (SmeltTarget recipe : SMELTING_RECIPES) {
            ItemTarget material = recipe.getMaterial();
            ItemTarget result = recipe.getItem();
            
            // Check if we have the raw material in our inventory
            if (mod.getInventoryTracker().getItemCount(material.getMatches()) > 0) {
                if (_smeltAll) {
                    // If smelting all, just return this recipe if we have the material
                    return recipe;
                } else {
                    // If smelting to targets, check if we need more of the result
                    if (mod.getInventoryTracker().getItemCount(result.getMatches()) < result.getTargetCount()) {
                        return recipe;
                    }
                }
            }
        }
        
        return null; // No materials to smelt
    }
}