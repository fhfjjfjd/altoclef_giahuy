package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

/**
 * Build scaffolding (pillar up + bridge) to reach an elevated build position.
 * Automatically selects scaffold blocks based on current dimension.
 */
public class ScaffoldTask extends Task {

    private final BlockPos targetPos;
    private final int scaffoldBuffer;  // extra blocks above target for workspace
    private boolean materialsCollected = false;
    private boolean pillarComplete = false;
    private BlockPos currentPillarTop;

    private static final int MIN_SCAFFOLD_MATERIALS = 16;
    private static final int PREFERRED_SCAFFOLD_MATERIALS = 64;

    public ScaffoldTask(BlockPos targetPos) {
        this(targetPos, 2);
    }

    public ScaffoldTask(BlockPos targetPos, int scaffoldBuffer) {
        this.targetPos = targetPos;
        this.scaffoldBuffer = scaffoldBuffer;
    }

    @Override
    protected void onStart(AltoClef mod) {
        materialsCollected = false;
        pillarComplete = false;
        currentPillarTop = mod.getPlayer().getBlockPos();
        Debug.logMessage("ScaffoldTask: building scaffold to Y=" + targetPos.getY());
    }

    @Override
    protected Task onTick(AltoClef mod) {
        BlockPos playerPos = mod.getPlayer().getBlockPos();
        Dimension dim = Dimension.current();

        // Validate Y range
        if (!dim.isValidBuildY(targetPos.getY())) {
            Debug.logMessage("ScaffoldTask: target Y=" + targetPos.getY() + " is out of range for " + dim.name());
            return null;
        }

        // Phase 1: Collect scaffold materials if needed
        int scaffoldCount = getScaffoldMaterialCount(mod);
        if (scaffoldCount < MIN_SCAFFOLD_MATERIALS) {
            setDebugState("Collecting scaffold materials...");
            return TaskCatalogue.getSquashedItemTask(
                    new ItemTarget(Items.COBBLESTONE, PREFERRED_SCAFFOLD_MATERIALS),
                    new ItemTarget(Items.DIRT, PREFERRED_SCAFFOLD_MATERIALS)
            );
        }

        // Phase 2: Pillar up to target Y
        if (playerPos.getY() < targetPos.getY()) {
            setDebugState("Pillaring up to Y=" + targetPos.getY() + " (current Y=" + playerPos.getY() + ")");
            // Place block below feet and jump
            BlockPos below = playerPos.down();
            if (!WorldHelper.isSolid(mod, below)) {
                return new PlaceStructureBlockTask(below);
            }
            // Need to pillar: place block at feet level then jump
            return new PlaceStructureBlockTask(playerPos);
        }

        // Phase 3: Bridge horizontally to target X/Z if needed
        int dx = targetPos.getX() - playerPos.getX();
        int dz = targetPos.getZ() - playerPos.getZ();
        if (Math.abs(dx) > 1 || Math.abs(dz) > 1) {
            setDebugState("Bridging to target position...");
            // Bridge one block at a time towards target
            int stepX = Integer.signum(dx);
            int stepZ = Integer.signum(dz);
            BlockPos nextBridge = new BlockPos(
                    playerPos.getX() + stepX,
                    targetPos.getY(),
                    playerPos.getZ() + stepZ
            );
            BlockPos belowBridge = nextBridge.down();
            if (!WorldHelper.isSolid(mod, belowBridge)) {
                return new PlaceStructureBlockTask(belowBridge);
            }
        }

        // Scaffold complete
        pillarComplete = true;
        setDebugState("Scaffold complete at target position");
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        if (pillarComplete) return true;
        BlockPos playerPos = mod.getPlayer().getBlockPos();
        return playerPos.getY() >= targetPos.getY()
                && Math.abs(playerPos.getX() - targetPos.getX()) <= 1
                && Math.abs(playerPos.getZ() - targetPos.getZ()) <= 1;
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof ScaffoldTask) {
            return ((ScaffoldTask) other).targetPos.equals(targetPos);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "ScaffoldTask -> " + targetPos.toShortString();
    }

    private int getScaffoldMaterialCount(AltoClef mod) {
        return mod.getInventoryTracker().getItemCount(Items.COBBLESTONE, Items.DIRT, Items.NETHERRACK);
    }
}
