package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.TimerGame;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.*;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Task to continuously monitor surroundings and warn about dangers.
 * Checks for nearby hostile mobs, low health, low food, and lava.
 */
public class AlertTask extends Task {

    private static final double CREEPER_RANGE = 10.0;
    private static final double HOSTILE_RANGE = 15.0;
    private static final float LOW_HEALTH_THRESHOLD = 6.0f;
    private static final int LOW_FOOD_THRESHOLD = 4;
    private static final int LAVA_CHECK_RADIUS = 5;

    private final TimerGame _checkTimer = new TimerGame(2.0);

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Alert system activated. Monitoring for dangers...");
        _checkTimer.forceElapse();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (!_checkTimer.elapsed()) {
            return null;
        }
        _checkTimer.reset();

        checkCreepers(mod);
        checkHostileMobs(mod);
        checkHealth(mod);
        checkFood(mod);
        checkLava(mod);

        setDebugState("Monitoring for dangers...");
        return null;
    }

    private void checkCreepers(AltoClef mod) {
        List<CreeperEntity> creepers = mod.getEntityTracker().getTrackedEntities(CreeperEntity.class);
        for (CreeperEntity creeper : creepers) {
            double dist = creeper.distanceTo(mod.getPlayer());
            if (dist <= CREEPER_RANGE) {
                String direction = getDirection(mod, creeper);
                if (creeper.getFuseSpeed() > 0) {
                    mod.log("[ALERT] CREEPER EXPLODING! " + String.format("%.1f", dist) + " blocks " + direction + "!");
                } else {
                    mod.log("[ALERT] Creeper detected " + String.format("%.1f", dist) + " blocks " + direction);
                }
            }
        }
    }

    private void checkHostileMobs(AltoClef mod) {
        checkMobType(mod, ZombieEntity.class, "Zombie");
        checkMobType(mod, SkeletonEntity.class, "Skeleton");
        checkMobType(mod, EndermanEntity.class, "Enderman");
        checkMobType(mod, WitchEntity.class, "Witch");
        checkMobType(mod, SpiderEntity.class, "Spider");
    }

    private <T extends Entity> void checkMobType(AltoClef mod, Class<T> mobClass, String name) {
        List<T> mobs = mod.getEntityTracker().getTrackedEntities(mobClass);
        for (T mob : mobs) {
            double dist = mob.distanceTo(mod.getPlayer());
            if (dist <= HOSTILE_RANGE) {
                String direction = getDirection(mod, mob);
                mod.log("[ALERT] " + name + " detected " + String.format("%.1f", dist) + " blocks " + direction);
            }
        }
    }

    private void checkHealth(AltoClef mod) {
        float health = mod.getPlayer().getHealth();
        if (health < LOW_HEALTH_THRESHOLD) {
            mod.log("[ALERT] LOW HEALTH: " + String.format("%.1f", health) + " HP!");
        }
    }

    private void checkFood(AltoClef mod) {
        int foodLevel = mod.getPlayer().getHungerManager().getFoodLevel();
        if (foodLevel < LOW_FOOD_THRESHOLD) {
            mod.log("[ALERT] LOW FOOD: " + foodLevel + " hunger points!");
        }
    }

    private void checkLava(AltoClef mod) {
        BlockPos playerPos = mod.getPlayer().getBlockPos();
        for (int x = -LAVA_CHECK_RADIUS; x <= LAVA_CHECK_RADIUS; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -LAVA_CHECK_RADIUS; z <= LAVA_CHECK_RADIUS; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    if (mod.getWorld().getBlockState(checkPos).getBlock() == Blocks.LAVA) {
                        mod.log("[ALERT] Lava nearby at " + checkPos.toShortString() + "!");
                        return;
                    }
                }
            }
        }
    }

    private String getDirection(AltoClef mod, Entity entity) {
        double dx = entity.getX() - mod.getPlayer().getX();
        double dz = entity.getZ() - mod.getPlayer().getZ();
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        if (angle < 0) angle += 360;

        if (angle >= 337.5 || angle < 22.5) return "South";
        if (angle < 67.5) return "South-West";
        if (angle < 112.5) return "West";
        if (angle < 157.5) return "North-West";
        if (angle < 202.5) return "North";
        if (angle < 247.5) return "North-East";
        if (angle < 292.5) return "East";
        return "South-East";
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.log("Alert system deactivated.");
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof AlertTask;
    }

    @Override
    protected String toDebugString() {
        return "Alert system active";
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return false;
    }
}
