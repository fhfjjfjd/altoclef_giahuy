package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.TimerGame;
import baritone.api.utils.input.Input;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

/**
 * Task to automatically enchant equipment.
 * Finds or crafts an enchanting table, collects lapis lazuli, navigates to the table,
 * and enchants the best available weapon/armor.
 */
public class AutoEnchantTask extends Task {

    private static final int REQUIRED_LAPIS = 3;
    private static final int REQUIRED_OBSIDIAN = 4;
    private static final int REQUIRED_DIAMONDS = 2;
    private static final int REQUIRED_BOOKS = 1;

    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(10);
    private final TimerGame _interactDelay = new TimerGame(1.0);
    private final TimerGame _enchantDelay = new TimerGame(0.5);

    private enum EnchantState {
        FIND_TABLE,
        COLLECT_LAPIS,
        ENCHANT
    }

    private EnchantState _state = EnchantState.FIND_TABLE;
    private BlockPos _tablePos = null;
    private boolean _needsCrafting = false;
    private boolean _enchantAttempted = false;

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Starting auto-enchant task...");
        _state = EnchantState.FIND_TABLE;
        _tablePos = null;
        _needsCrafting = false;
        _enchantAttempted = false;
        _interactDelay.forceElapse();
        _enchantDelay.forceElapse();
        mod.getBlockTracker().trackBlock(Blocks.ENCHANTING_TABLE);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        switch (_state) {
            case FIND_TABLE:
                return handleFindTable(mod);
            case COLLECT_LAPIS:
                return handleCollectLapis(mod);
            case ENCHANT:
                return handleEnchant(mod);
        }
        return null;
    }

    private Task handleFindTable(AltoClef mod) {
        setDebugState("Looking for enchanting table...");

        // Check if we need to craft one
        if (_needsCrafting) {
            return handleCraftTable(mod);
        }

        _tablePos = mod.getBlockTracker().getNearestTracking(Blocks.ENCHANTING_TABLE);

        if (_tablePos == null) {
            // No table found, check if we have materials to craft one
            int obsidianCount = mod.getInventoryTracker().getItemCount(Items.OBSIDIAN);
            int diamondCount = mod.getInventoryTracker().getItemCount(Items.DIAMOND);
            int bookCount = mod.getInventoryTracker().getItemCount(Items.BOOK);

            if (obsidianCount >= REQUIRED_OBSIDIAN && diamondCount >= REQUIRED_DIAMONDS && bookCount >= REQUIRED_BOOKS) {
                // We have materials, craft the table
                _needsCrafting = true;
                return handleCraftTable(mod);
            }

            // Collect materials for enchanting table
            if (obsidianCount < REQUIRED_OBSIDIAN) {
                mod.log("Collecting obsidian for enchanting table...");
                return TaskCatalogue.getItemTask("obsidian", REQUIRED_OBSIDIAN);
            }
            if (diamondCount < REQUIRED_DIAMONDS) {
                mod.log("Collecting diamonds for enchanting table...");
                return TaskCatalogue.getItemTask("diamond", REQUIRED_DIAMONDS);
            }
            if (bookCount < REQUIRED_BOOKS) {
                mod.log("Collecting book for enchanting table...");
                return TaskCatalogue.getItemTask("book", REQUIRED_BOOKS);
            }

            return _wanderTask;
        }

        // Found a table, navigate close to it
        double dist = mod.getPlayer().getBlockPos().getSquaredDistance(_tablePos);
        if (dist > 9) {
            return new GetToBlockTask(_tablePos);
        }

        // We're close to the table, move to lapis collection
        _state = EnchantState.COLLECT_LAPIS;
        return null;
    }

    private Task handleCraftTable(AltoClef mod) {
        setDebugState("Crafting enchanting table...");

        // Use TaskCatalogue to get an enchanting table
        Task craftTask = TaskCatalogue.getItemTask("enchanting_table", 1);
        if (craftTask != null) {
            if (mod.getInventoryTracker().hasItem(Items.ENCHANTING_TABLE)) {
                // Place the table near the player
                BlockPos placePos = mod.getPlayer().getBlockPos().add(2, 0, 0);
                _needsCrafting = false;
                return new PlaceBlockTask(placePos, Blocks.ENCHANTING_TABLE);
            }
            return craftTask;
        }

        mod.log("Cannot craft enchanting table.");
        _needsCrafting = false;
        return null;
    }

    private Task handleCollectLapis(AltoClef mod) {
        setDebugState("Collecting lapis lazuli...");

        int lapisCount = mod.getInventoryTracker().getItemCount(Items.LAPIS_LAZULI);
        if (lapisCount < REQUIRED_LAPIS) {
            mod.log("Need lapis lazuli for enchanting...");
            Task getTask = TaskCatalogue.getItemTask("lapis_lazuli", REQUIRED_LAPIS);
            if (getTask != null) {
                return getTask;
            }
            mod.log("Cannot obtain lapis lazuli, attempting to enchant with what we have.");
        }

        _state = EnchantState.ENCHANT;
        return null;
    }

    private Task handleEnchant(AltoClef mod) {
        setDebugState("Enchanting...");

        if (_tablePos == null) {
            _tablePos = mod.getBlockTracker().getNearestTracking(Blocks.ENCHANTING_TABLE);
            if (_tablePos == null) {
                mod.log("Lost track of enchanting table, searching again...");
                _state = EnchantState.FIND_TABLE;
                return null;
            }
        }

        // Navigate to the table if too far
        double dist = mod.getPlayer().getBlockPos().getSquaredDistance(_tablePos);
        if (dist > 9) {
            return new GetToBlockTask(_tablePos);
        }

        // Check if enchanting screen is open
        if (MinecraftClient.getInstance().currentScreen instanceof EnchantmentScreen) {
            // Enchanting screen is open, click the best enchantment (slot 2 = highest level)
            if (_enchantDelay.elapsed()) {
                mod.getInputControls().tryPress(Input.CLICK_LEFT);
                _enchantAttempted = true;
                _enchantDelay.reset();
                mod.log("Attempted enchantment!");
            }
            return null;
        }

        // Open the enchanting table
        if (_interactDelay.elapsed()) {
            mod.getInputControls().tryPress(Input.CLICK_RIGHT);
            _interactDelay.reset();
        }

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.log("Auto-enchant task stopped.");
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof AutoEnchantTask;
    }

    @Override
    protected String toDebugString() {
        return "Auto-enchanting, state=" + _state;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _enchantAttempted;
    }
}
