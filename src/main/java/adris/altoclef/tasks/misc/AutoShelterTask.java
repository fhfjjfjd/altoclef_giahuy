package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Task to build a 5x5x4 cobblestone shelter when it's nighttime.
 * Collects cobblestone if needed, then places walls, roof, and a door opening.
 */
public class AutoShelterTask extends Task {

    private static final int SHELTER_WIDTH = 5;
    private static final int SHELTER_DEPTH = 5;
    private static final int SHELTER_HEIGHT = 4;
    private static final int REQUIRED_COBBLESTONE = 100;

    private BlockPos _origin = null;
    private List<BlockPos> _blocksToPlace = null;
    private int _currentBlockIndex = 0;
    private boolean _collectingMaterials = false;

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Starting auto-shelter task...");
        _origin = null;
        _blocksToPlace = null;
        _currentBlockIndex = 0;
        _collectingMaterials = false;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Check if it's nighttime
        long timeOfDay = mod.getWorld().getTimeOfDay() % 24000;
        if (timeOfDay < 13000 && _origin == null) {
            setDebugState("Waiting for nighttime to build shelter...");
            mod.log("It's still daytime. Waiting for night to build shelter.");
            return null;
        }

        // Ensure we have enough cobblestone
        int cobbleCount = mod.getInventoryTracker().getItemCount(Items.COBBLESTONE);
        if (cobbleCount < REQUIRED_COBBLESTONE && _origin == null) {
            setDebugState("Collecting cobblestone (" + cobbleCount + "/" + REQUIRED_COBBLESTONE + ")...");
            _collectingMaterials = true;
            Task gatherTask = TaskCatalogue.getItemTask("cobblestone", REQUIRED_COBBLESTONE);
            if (gatherTask != null) {
                return gatherTask;
            }
            return null;
        }
        _collectingMaterials = false;

        // Pick a build origin near the player
        if (_origin == null) {
            _origin = mod.getPlayer().getBlockPos().add(2, 0, 2);
            _blocksToPlace = generateShelterBlocks(_origin);
            _currentBlockIndex = 0;
            mod.log("Building shelter at " + _origin.toShortString());
        }

        // Place blocks one at a time
        if (_currentBlockIndex < _blocksToPlace.size()) {
            BlockPos target = _blocksToPlace.get(_currentBlockIndex);

            // Skip if block is already placed
            if (mod.getWorld().getBlockState(target).getBlock() == Blocks.COBBLESTONE) {
                _currentBlockIndex++;
                return onTick(mod);
            }

            // Check if we still have cobblestone to place
            if (mod.getInventoryTracker().getItemCount(Items.COBBLESTONE) < 1) {
                setDebugState("Ran out of cobblestone, collecting more...");
                Task gatherTask = TaskCatalogue.getItemTask("cobblestone", REQUIRED_COBBLESTONE);
                if (gatherTask != null) {
                    return gatherTask;
                }
                return null;
            }

            setDebugState("Placing block " + (_currentBlockIndex + 1) + "/" + _blocksToPlace.size());
            return new PlaceBlockTask(target, Blocks.COBBLESTONE);
        }

        mod.log("Shelter construction complete!");
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.log("Auto-shelter task stopped.");
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof AutoShelterTask;
    }

    @Override
    protected String toDebugString() {
        if (_collectingMaterials) {
            return "Collecting shelter materials";
        }
        if (_blocksToPlace != null) {
            return "Building shelter (" + _currentBlockIndex + "/" + _blocksToPlace.size() + ")";
        }
        return "Auto-shelter waiting";
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _blocksToPlace != null && _currentBlockIndex >= _blocksToPlace.size();
    }

    /**
     * Generate the list of block positions for a 5x5x4 cobblestone shelter.
     * Leaves a 1x2 opening in the front wall for a door.
     */
    private List<BlockPos> generateShelterBlocks(BlockPos origin) {
        List<BlockPos> blocks = new ArrayList<>();

        for (int y = 0; y < SHELTER_HEIGHT; y++) {
            for (int x = 0; x < SHELTER_WIDTH; x++) {
                for (int z = 0; z < SHELTER_DEPTH; z++) {
                    boolean isWall = x == 0 || x == SHELTER_WIDTH - 1 || z == 0 || z == SHELTER_DEPTH - 1;
                    boolean isRoof = y == SHELTER_HEIGHT - 1;
                    boolean isFloor = y == 0;

                    // Door opening: front wall (z == 0), center column (x == 2), bottom two blocks (y == 0, 1)
                    boolean isDoorOpening = z == 0 && x == SHELTER_WIDTH / 2 && y < 2;

                    if ((isWall || isRoof) && !isFloor && !isDoorOpening) {
                        blocks.add(origin.add(x, y, z));
                    }
                }
            }
        }

        return blocks;
    }
}
