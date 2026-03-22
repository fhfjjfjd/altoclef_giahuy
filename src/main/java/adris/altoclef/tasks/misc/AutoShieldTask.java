package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.TimerGame;
import baritone.api.utils.input.Input;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.*;
import net.minecraft.item.Items;

import java.util.List;

/**
 * Task to automatically raise a shield when hostile mobs are nearby.
 * Monitors for incoming attacks and holds shield when threats are within range.
 */
public class AutoShieldTask extends Task {

    private static final double THREAT_RANGE = 5.0;
    private static final double THREAT_RANGE_SQ = THREAT_RANGE * THREAT_RANGE;

    private final TimerGame _shieldHoldTimer = new TimerGame(2.0);
    private boolean _shieldRaised = false;
    private boolean _obtainingShield = false;

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Starting auto-shield task...");
        _shieldRaised = false;
        _obtainingShield = false;
        _shieldHoldTimer.forceElapse();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Ensure we have a shield
        if (!mod.getInventoryTracker().hasItem(Items.SHIELD)) {
            if (!_obtainingShield) {
                mod.log("No shield found, obtaining one...");
                _obtainingShield = true;
            }
            setDebugState("Obtaining shield...");
            Task getTask = TaskCatalogue.getItemTask("shield", 1);
            if (getTask != null) {
                return getTask;
            }
            mod.log("Cannot obtain a shield.");
            return null;
        }
        _obtainingShield = false;

        // Check for nearby hostile mobs
        boolean threatDetected = isThreatNearby(mod);

        if (threatDetected) {
            setDebugState("Threat detected! Raising shield...");

            // Equip shield
            if (!mod.getSlotHandler().forceEquipItem(Items.SHIELD)) {
                return null;
            }

            // Hold right click to raise shield
            if (!_shieldRaised) {
                mod.getInputControls().hold(Input.CLICK_RIGHT);
                _shieldRaised = true;
                _shieldHoldTimer.reset();
            }
        } else {
            // No threats nearby
            if (_shieldRaised) {
                setDebugState("No threats, lowering shield...");
                mod.getInputControls().release(Input.CLICK_RIGHT);
                _shieldRaised = false;
            } else {
                setDebugState("Monitoring for threats...");
            }
        }

        return null;
    }

    private boolean isThreatNearby(AltoClef mod) {
        // Check tracked hostiles from EntityTracker
        List<Entity> hostiles = mod.getEntityTracker().getHostiles();
        if (!hostiles.isEmpty()) {
            for (Entity hostile : hostiles) {
                double distSq = hostile.squaredDistanceTo(mod.getPlayer());
                if (distSq <= THREAT_RANGE_SQ) {
                    return true;
                }
            }
        }

        // Also check specific mob types that might not be in hostiles list yet
        Entity closestThreat = mod.getEntityTracker().getClosestEntity(
                ZombieEntity.class,
                SkeletonEntity.class,
                CreeperEntity.class,
                SpiderEntity.class,
                WitherSkeletonEntity.class,
                VindicatorEntity.class,
                PillagerEntity.class,
                RavagerEntity.class
        );

        if (closestThreat != null) {
            double distSq = closestThreat.squaredDistanceTo(mod.getPlayer());
            return distSq <= THREAT_RANGE_SQ;
        }

        return false;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        // Make sure to release shield when stopping
        if (_shieldRaised) {
            mod.getInputControls().release(Input.CLICK_RIGHT);
            _shieldRaised = false;
        }
        mod.log("Auto-shield task stopped.");
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof AutoShieldTask;
    }

    @Override
    protected String toDebugString() {
        return "Auto-shielding, raised=" + _shieldRaised;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        // Runs indefinitely until stopped
        return false;
    }
}
