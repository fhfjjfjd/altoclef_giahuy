package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.TimerGame;
import baritone.api.utils.input.Input;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Task to build a bridge in a specified direction.
 * Places blocks under the player's feet while walking forward.
 */
public class AutoBridgeTask extends Task {

    private final String _direction;
    private final int _length;
    private int _blocksPlaced = 0;
    private boolean _collectingMaterials = false;
    private final TimerGame _placeTimer = new TimerGame(0.5);

    public AutoBridgeTask(String direction, int length) {
        _direction = direction.toLowerCase();
        _length = length;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Starting auto-bridge: " + _direction + " for " + _length + " blocks");
        _blocksPlaced = 0;
        _collectingMaterials = false;
        _placeTimer.forceElapse();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Check if we have building materials
        boolean hasCobblestone = mod.getInventoryTracker().hasItem(Items.COBBLESTONE);
        boolean hasDirt = mod.getInventoryTracker().hasItem(Items.DIRT);
        boolean hasNetherrack = mod.getInventoryTracker().hasItem(Items.NETHERRACK);

        if (!hasCobblestone && !hasDirt && !hasNetherrack) {
            if (!_collectingMaterials) {
                mod.log("No building materials, collecting cobblestone...");
                _collectingMaterials = true;
            }
            setDebugState("Collecting building materials...");
            Task collectTask = TaskCatalogue.getItemTask("cobblestone", 64);
            if (collectTask != null) {
                return collectTask;
            }
            mod.log("Cannot obtain building materials.");
            return null;
        }
        _collectingMaterials = false;

        // Equip building material
        if (hasCobblestone) {
            mod.getSlotHandler().forceEquipItem(Items.COBBLESTONE);
        } else if (hasDirt) {
            mod.getSlotHandler().forceEquipItem(Items.DIRT);
        } else {
            mod.getSlotHandler().forceEquipItem(Items.NETHERRACK);
        }

        // Calculate the next block position to place
        BlockPos playerPos = mod.getPlayer().getBlockPos();
        Direction dir = getDirectionFromString(_direction);
        if (dir == null) {
            mod.log("Invalid direction: " + _direction + ". Use north/south/east/west.");
            return null;
        }

        BlockPos belowPlayer = playerPos.down();
        BlockPos targetPos = belowPlayer.offset(dir, 1);

        // Check if the target position needs a block
        Block targetBlock = mod.getWorld().getBlockState(targetPos).getBlock();
        if (targetBlock == Blocks.AIR || targetBlock == Blocks.CAVE_AIR || targetBlock == Blocks.VOID_AIR) {
            if (_placeTimer.elapsed()) {
                setDebugState("Placing block " + (_blocksPlaced + 1) + "/" + _length);

                // Sneak and move forward for safe bridging
                mod.getInputControls().hold(Input.SNEAK);
                mod.getInputControls().hold(Input.MOVE_FORWARD);

                // Place block at target position
                Block placeBlock = Blocks.COBBLESTONE;
                if (hasDirt && !hasCobblestone) placeBlock = Blocks.DIRT;
                if (hasNetherrack && !hasCobblestone && !hasDirt) placeBlock = Blocks.NETHERRACK;

                _placeTimer.reset();
                return new PlaceBlockTask(targetPos, placeBlock);
            }
        } else {
            // Block already exists at target, move forward
            mod.getInputControls().hold(Input.SNEAK);
            mod.getInputControls().hold(Input.MOVE_FORWARD);
            _blocksPlaced++;
            mod.log("Bridge progress: " + _blocksPlaced + "/" + _length + " blocks placed");
        }

        setDebugState("Bridging " + _direction + ": " + _blocksPlaced + "/" + _length);
        return null;
    }

    private Direction getDirectionFromString(String dir) {
        switch (dir) {
            case "north": return Direction.NORTH;
            case "south": return Direction.SOUTH;
            case "east": return Direction.EAST;
            case "west": return Direction.WEST;
            default: return null;
        }
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getInputControls().release(Input.SNEAK);
        mod.getInputControls().release(Input.MOVE_FORWARD);
        mod.log("Auto-bridge stopped. Placed " + _blocksPlaced + " blocks.");
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof AutoBridgeTask task) {
            return task._direction.equals(_direction) && task._length == _length;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Auto-bridge " + _direction + " " + _blocksPlaced + "/" + _length;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _blocksPlaced >= _length;
    }
}
