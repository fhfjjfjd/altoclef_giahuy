package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.helpers.WorldHelper;
import baritone.api.utils.input.Input;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Task to automatically harvest mature crops and replant them.
 * Searches for wheat, carrots, potatoes, and beetroots that are fully grown,
 * breaks them, and replants seeds on the farmland.
 */
public class AutoHarvestTask extends Task {

    private static final int SEARCH_RADIUS = 32;

    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(10);
    private final TimerGame _replantDelay = new TimerGame(0.5);

    private enum HarvestState {
        SEARCHING,
        HARVESTING,
        REPLANTING
    }

    private HarvestState _state = HarvestState.SEARCHING;
    private BlockPos _targetCrop = null;
    private Block _targetCropBlock = null;

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Starting auto-harvest task...");
        _state = HarvestState.SEARCHING;
        _targetCrop = null;
        _targetCropBlock = null;
        _replantDelay.forceElapse();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        switch (_state) {
            case SEARCHING:
                return handleSearching(mod);
            case HARVESTING:
                return handleHarvesting(mod);
            case REPLANTING:
                return handleReplanting(mod);
        }
        return null;
    }

    private Task handleSearching(AltoClef mod) {
        setDebugState("Searching for mature crops...");

        BlockPos playerPos = mod.getPlayer().getBlockPos();
        BlockPos bestCrop = null;
        double bestDistSq = Double.MAX_VALUE;
        Block bestCropBlock = null;

        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);

                    if (mod.getWorld() == null) continue;

                    BlockState blockState = mod.getWorld().getBlockState(checkPos);
                    Block block = blockState.getBlock();

                    if (isMatureCrop(blockState, block)) {
                        double distSq = playerPos.getSquaredDistance(checkPos);
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq;
                            bestCrop = checkPos;
                            bestCropBlock = block;
                        }
                    }
                }
            }
        }

        if (bestCrop != null) {
            _targetCrop = bestCrop;
            _targetCropBlock = bestCropBlock;
            _state = HarvestState.HARVESTING;
            mod.log("Found mature crop at " + _targetCrop.toShortString());
            return null;
        }

        // No mature crops found, wander to search more
        setDebugState("No mature crops nearby, wandering...");
        return _wanderTask;
    }

    private Task handleHarvesting(AltoClef mod) {
        if (_targetCrop == null) {
            _state = HarvestState.SEARCHING;
            return null;
        }

        // Verify the crop is still there and mature
        if (mod.getWorld() == null || WorldHelper.isAir(mod, _targetCrop)) {
            // Crop already harvested, move to replanting
            _state = HarvestState.REPLANTING;
            _replantDelay.reset();
            return null;
        }

        BlockState blockState = mod.getWorld().getBlockState(_targetCrop);
        if (!isMatureCrop(blockState, blockState.getBlock())) {
            // No longer mature, search for next crop
            _state = HarvestState.SEARCHING;
            _targetCrop = null;
            return null;
        }

        setDebugState("Harvesting crop at " + _targetCrop.toShortString());
        return new DestroyBlockTask(_targetCrop);
    }

    private Task handleReplanting(AltoClef mod) {
        if (_targetCrop == null || _targetCropBlock == null) {
            _state = HarvestState.SEARCHING;
            return null;
        }

        setDebugState("Replanting at " + _targetCrop.toShortString());

        // Check the farmland is still below
        BlockPos farmlandPos = _targetCrop.down();
        if (mod.getWorld() == null) {
            _state = HarvestState.SEARCHING;
            return null;
        }

        Block belowBlock = mod.getWorld().getBlockState(farmlandPos).getBlock();
        if (belowBlock != Blocks.FARMLAND) {
            // Farmland gone, move on
            _state = HarvestState.SEARCHING;
            _targetCrop = null;
            return null;
        }

        // Navigate close to replant
        double dist = mod.getPlayer().getBlockPos().getSquaredDistance(_targetCrop);
        if (dist > 9) {
            return new GetToBlockTask(_targetCrop);
        }

        // Equip the appropriate seed and replant
        if (_replantDelay.elapsed()) {
            boolean equipped = false;
            if (_targetCropBlock == Blocks.WHEAT) {
                equipped = mod.getSlotHandler().forceEquipItem(Items.WHEAT_SEEDS);
            } else if (_targetCropBlock == Blocks.CARROTS) {
                equipped = mod.getSlotHandler().forceEquipItem(Items.CARROT);
            } else if (_targetCropBlock == Blocks.POTATOES) {
                equipped = mod.getSlotHandler().forceEquipItem(Items.POTATO);
            } else if (_targetCropBlock == Blocks.BEETROOTS) {
                equipped = mod.getSlotHandler().forceEquipItem(Items.BEETROOT_SEEDS);
            }

            if (equipped) {
                mod.getInputControls().tryPress(Input.CLICK_RIGHT);
            }

            // Move on to search for next crop
            _state = HarvestState.SEARCHING;
            _targetCrop = null;
            _targetCropBlock = null;
        }

        return null;
    }

    private boolean isMatureCrop(BlockState blockState, Block block) {
        if (block == Blocks.WHEAT || block == Blocks.CARROTS || block == Blocks.POTATOES) {
            return blockState.get(Properties.AGE_7) >= 7;
        }
        if (block == Blocks.BEETROOTS) {
            return blockState.get(Properties.AGE_3) >= 3;
        }
        return false;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.log("Auto-harvest task stopped.");
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof AutoHarvestTask;
    }

    @Override
    protected String toDebugString() {
        return "Auto-harvesting, state=" + _state;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        // Runs indefinitely until stopped
        return false;
    }
}
