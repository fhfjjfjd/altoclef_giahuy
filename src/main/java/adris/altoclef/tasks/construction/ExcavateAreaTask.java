package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

/**
 * Excavate (clear) an underground region before building.
 * Adds 1-block work buffer around the build area.
 * Handles hazards (lava, water) by sealing them before clearing.
 */
public class ExcavateAreaTask extends Task {

    private final BlockPos buildOrigin;
    private final int sizeX, sizeY, sizeZ;
    private final int buffer;
    private boolean hazardsSealed = false;

    private static final int MIN_TOOLS = 1;

    public ExcavateAreaTask(BlockPos buildOrigin, int sizeX, int sizeY, int sizeZ) {
        this(buildOrigin, sizeX, sizeY, sizeZ, 1);
    }

    public ExcavateAreaTask(BlockPos buildOrigin, int sizeX, int sizeY, int sizeZ, int buffer) {
        this.buildOrigin = buildOrigin;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.buffer = buffer;
    }

    @Override
    protected void onStart(AltoClef mod) {
        hazardsSealed = false;
        Debug.logMessage("ExcavateAreaTask: clearing " + sizeX + "x" + sizeY + "x" + sizeZ + " at " + buildOrigin.toShortString());
    }

    @Override
    protected Task onTick(AltoClef mod) {
        Dimension dim = Dimension.current();

        // Phase 1: Scan for and seal hazards (lava/water on boundary)
        if (!hazardsSealed) {
            BlockPos hazard = findBoundaryHazard(mod);
            if (hazard != null) {
                setDebugState("Sealing hazard at " + hazard.toShortString());
                return new PlaceStructureBlockTask(hazard);
            }
            hazardsSealed = true;
            Debug.logMessage("ExcavateAreaTask: all boundary hazards sealed");
        }

        // Phase 2: Clear the region using Baritone's clearArea
        BlockPos from = buildOrigin.add(-buffer, -buffer, -buffer);
        BlockPos to = buildOrigin.add(sizeX + buffer, sizeY + buffer, sizeZ + buffer);

        // Clamp to dimension limits
        int fromY = Math.max(from.getY(), dim.getMinY());
        int toY = Math.min(to.getY(), dim.getMaxY());
        from = new BlockPos(from.getX(), fromY, from.getZ());
        to = new BlockPos(to.getX(), toY, to.getZ());

        setDebugState("Excavating region...");
        return new ClearRegionTask(from, to);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        // Check if the core build area is clear
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    BlockPos pos = buildOrigin.add(x, y, z);
                    if (mod.getWorld() != null && !mod.getWorld().isAir(pos)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof ExcavateAreaTask task) {
            return task.buildOrigin.equals(buildOrigin)
                    && task.sizeX == sizeX && task.sizeY == sizeY && task.sizeZ == sizeZ;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "ExcavateArea " + sizeX + "x" + sizeY + "x" + sizeZ + " at " + buildOrigin.toShortString();
    }

    /**
     * Find a lava/water block on the boundary of the excavation zone.
     * These must be sealed before clearing to prevent flooding.
     */
    private BlockPos findBoundaryHazard(AltoClef mod) {
        if (mod.getWorld() == null) return null;

        int bx = buildOrigin.getX() - buffer;
        int by = buildOrigin.getY() - buffer;
        int bz = buildOrigin.getZ() - buffer;
        int ex = buildOrigin.getX() + sizeX + buffer;
        int ey = buildOrigin.getY() + sizeY + buffer;
        int ez = buildOrigin.getZ() + sizeZ + buffer;

        Dimension dim = Dimension.current();
        by = Math.max(by, dim.getMinY());
        ey = Math.min(ey, dim.getMaxY());

        // Scan boundary faces for liquid
        for (int x = bx; x <= ex; x++) {
            for (int y = by; y <= ey; y++) {
                for (int z = bz; z <= ez; z++) {
                    // Only check boundary blocks
                    boolean isBoundary = (x == bx || x == ex || y == by || y == ey || z == bz || z == ez);
                    if (!isBoundary) continue;

                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mod.getWorld().getBlockState(pos);
                    if (state.isOf(Blocks.LAVA) || state.isOf(Blocks.WATER)) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }
}
