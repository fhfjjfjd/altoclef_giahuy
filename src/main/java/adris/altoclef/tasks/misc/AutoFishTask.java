package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.helpers.LookHelper;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import net.minecraft.block.Blocks;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Task to automatically fish.
 * Equips a fishing rod, finds water, casts the line, and reels in when a fish bites.
 * Tracks fish caught, auto-crafts rod if needed, detects rain bonus, and recasts on timeout.
 */
public class AutoFishTask extends Task {

    private static final double BITE_TIMEOUT_SECONDS = 45.0;

    private final TimerGame _castDelay = new TimerGame(1.0);
    private final TimerGame _reelDelay = new TimerGame(0.5);
    private final TimerGame _biteTimeout = new TimerGame(BITE_TIMEOUT_SECONDS);
    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(10);

    private enum FishingState {
        CRAFTING_ROD,
        GETTING_ROD,
        FINDING_WATER,
        CASTING,
        WAITING_FOR_BITE,
        REELING
    }

    private FishingState _state = FishingState.GETTING_ROD;
    private BlockPos _waterTarget = null;
    private int _fishCaughtCount = 0;
    private boolean _isRaining = false;

    public int getFishCaughtCount() {
        return _fishCaughtCount;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Starting auto-fish task...");
        _state = FishingState.GETTING_ROD;
        _fishCaughtCount = 0;
        _castDelay.forceElapse();
        _reelDelay.forceElapse();
        _biteTimeout.forceElapse();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Detect rain for bonus logging
        _isRaining = mod.getWorld().isRaining();

        // Ensure we have a fishing rod - try crafting first if we have materials
        if (!mod.getInventoryTracker().hasItem(Items.FISHING_ROD)) {
            boolean hasSticks = mod.getInventoryTracker().getItemCount(Items.STICK) >= 3;
            boolean hasString = mod.getInventoryTracker().getItemCount(Items.STRING) >= 2;

            if (hasSticks && hasString) {
                setDebugState("Crafting fishing rod...");
                _state = FishingState.CRAFTING_ROD;
                Task craftTask = TaskCatalogue.getItemTask("fishing_rod", 1);
                if (craftTask != null) {
                    return craftTask;
                }
            }

            setDebugState("Obtaining fishing rod...");
            _state = FishingState.GETTING_ROD;
            Task getTask = TaskCatalogue.getItemTask("fishing_rod", 1);
            if (getTask != null) {
                return getTask;
            }
            mod.log("Cannot obtain a fishing rod.");
            return null;
        }

        switch (_state) {
            case CRAFTING_ROD:
            case GETTING_ROD:
                _state = FishingState.FINDING_WATER;
                // Fall through to FINDING_WATER

            case FINDING_WATER:
                setDebugState("Looking for water...");
                _waterTarget = mod.getBlockTracker().getNearestTracking(Blocks.WATER);
                if (_waterTarget == null) {
                    // Wander to find water
                    return _wanderTask;
                }
                // Get close to the water
                double dist = mod.getPlayer().getBlockPos().getSquaredDistance(_waterTarget);
                if (dist > 16) {
                    return new GetToBlockTask(_waterTarget);
                }
                _state = FishingState.CASTING;
                _castDelay.reset();
                break;

            case CASTING:
                setDebugState("Casting fishing rod..." + (_isRaining ? " (rain bonus!)" : ""));
                // Equip fishing rod
                if (!mod.getSlotHandler().forceEquipItem(Items.FISHING_ROD)) {
                    break;
                }
                // Look at water
                if (_waterTarget != null) {
                    Vec3d waterCenter = new Vec3d(_waterTarget.getX() + 0.5, _waterTarget.getY() + 0.5, _waterTarget.getZ() + 0.5);
                    LookHelper.lookAt(mod, waterCenter);
                }
                // Cast after delay
                if (_castDelay.elapsed()) {
                    mod.getInputControls().tryPress(Input.CLICK_RIGHT);
                    _state = FishingState.WAITING_FOR_BITE;
                    _biteTimeout.reset();
                    _castDelay.reset();
                    if (_isRaining) {
                        mod.log("Fishing in rain - expect faster bites!");
                    }
                }
                break;

            case WAITING_FOR_BITE:
                setDebugState("Waiting for bite... (caught: " + _fishCaughtCount + ")" + (_isRaining ? " [rain]" : ""));
                // Make sure rod is still equipped
                mod.getSlotHandler().forceEquipItem(Items.FISHING_ROD);

                FishingBobberEntity bobber = mod.getPlayer().fishHook;
                if (bobber == null) {
                    // Bobber disappeared without us reeling - recast
                    _state = FishingState.CASTING;
                    _castDelay.reset();
                    break;
                }

                // Timeout: if no bite in 45 seconds, reel in and recast
                if (_biteTimeout.elapsed()) {
                    mod.log("No bite after " + BITE_TIMEOUT_SECONDS + "s, recasting...");
                    mod.getInputControls().tryPress(Input.CLICK_RIGHT);
                    _state = FishingState.CASTING;
                    _castDelay.reset();
                    break;
                }

                // Check if a fish is biting (bobber velocity goes negative Y rapidly)
                Vec3d velocity = bobber.getVelocity();
                if (velocity.y < -0.04) {
                    mod.log("Fish on the hook!");
                    _state = FishingState.REELING;
                    _reelDelay.reset();
                }
                break;

            case REELING:
                setDebugState("Reeling in fish #" + (_fishCaughtCount + 1) + "...");
                mod.getSlotHandler().forceEquipItem(Items.FISHING_ROD);
                if (_reelDelay.elapsed()) {
                    mod.getInputControls().tryPress(Input.CLICK_RIGHT);
                    _fishCaughtCount++;
                    mod.log("Fish caught! Total: " + _fishCaughtCount);
                    // Go back to casting
                    _state = FishingState.CASTING;
                    _castDelay.reset();
                }
                break;
        }

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.log("Auto-fish task stopped.");
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof AutoFishTask;
    }

    @Override
    protected String toDebugString() {
        return "Auto-fishing, state=" + _state + ", caught=" + _fishCaughtCount + (_isRaining ? " [rain]" : "");
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        // Runs indefinitely until stopped
        return false;
    }
}
