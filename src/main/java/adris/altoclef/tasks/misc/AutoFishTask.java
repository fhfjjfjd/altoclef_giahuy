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
 */
public class AutoFishTask extends Task {

    private final TimerGame _castDelay = new TimerGame(1.0);
    private final TimerGame _reelDelay = new TimerGame(0.5);
    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(10);

    private enum FishingState {
        GETTING_ROD,
        FINDING_WATER,
        CASTING,
        WAITING_FOR_BITE,
        REELING
    }

    private FishingState _state = FishingState.GETTING_ROD;
    private BlockPos _waterTarget = null;

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Starting auto-fish task...");
        _state = FishingState.GETTING_ROD;
        _castDelay.forceElapse();
        _reelDelay.forceElapse();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Ensure we have a fishing rod
        if (!mod.getInventoryTracker().hasItem(Items.FISHING_ROD)) {
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
                setDebugState("Casting fishing rod...");
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
                    _castDelay.reset();
                }
                break;

            case WAITING_FOR_BITE:
                setDebugState("Waiting for fish to bite...");
                // Make sure rod is still equipped
                mod.getSlotHandler().forceEquipItem(Items.FISHING_ROD);

                FishingBobberEntity bobber = mod.getPlayer().fishHook;
                if (bobber == null) {
                    // Bobber disappeared without us reeling - recast
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
                setDebugState("Reeling in fish...");
                mod.getSlotHandler().forceEquipItem(Items.FISHING_ROD);
                if (_reelDelay.elapsed()) {
                    mod.getInputControls().tryPress(Input.CLICK_RIGHT);
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
        return "Auto-fishing, state=" + _state;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        // Runs indefinitely until stopped
        return false;
    }
}
