package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.construction.ExcavateAreaTask;
import adris.altoclef.tasks.construction.ScaffoldTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import baritone.api.schematic.ISchematic;
import baritone.api.schematic.format.ISchematicFormat;
import baritone.utils.schematic.SchematicSystem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.io.File;
import java.io.FileInputStream;
import java.util.Optional;

/**
 * Dimension-aware build task: auto-detects build environment and handles
 * elevated (scaffolding) and underground (excavation) construction.
 *
 * Build modes:
 *   AUTO      — detect from target Y vs surface Y
 *   ELEVATED  — force scaffold mode (building in the air)
 *   UNDERGROUND — force excavation mode (building below surface)
 *   SURFACE   — default, no pre-processing
 */
public class DimensionAwareBuildTask extends Task {

    public enum BuildMode {
        AUTO,
        ELEVATED,
        UNDERGROUND,
        SURFACE
    }

    public enum Phase {
        PREPARING,   // scaffold or excavate
        BUILDING,    // delegate to SchematicBuildTask
        FINISHED
    }

    private final String schematicFileName;
    private final BlockPos startPos;
    private final int rotationSteps;
    private final BuildMode requestedMode;

    private BuildMode resolvedMode;
    private Phase phase;
    private Dimension buildDimension;
    private Vec3i schematicSize;

    private Task prepareTask;
    private SchematicBuildTask buildTask;

    public DimensionAwareBuildTask(String schematicFileName, BlockPos startPos, int rotationSteps, BuildMode mode) {
        this.schematicFileName = schematicFileName;
        this.startPos = startPos;
        this.rotationSteps = rotationSteps;
        this.requestedMode = mode;
    }

    public DimensionAwareBuildTask(String schematicFileName, BlockPos startPos, int rotationSteps) {
        this(schematicFileName, startPos, rotationSteps, BuildMode.AUTO);
    }

    public DimensionAwareBuildTask(String schematicFileName, int rotationSteps, BuildMode mode) {
        this(schematicFileName, null, rotationSteps, mode);
    }

    public Phase getPhase() { return phase; }
    public BuildMode getResolvedMode() { return resolvedMode; }
    public Dimension getBuildDimension() { return buildDimension; }

    @Override
    protected void onStart(AltoClef mod) {
        phase = Phase.PREPARING;
        buildDimension = Dimension.current();

        // Load schematic to get size
        schematicSize = loadSchematicSize();

        BlockPos pos = (startPos != null) ? startPos : mod.getPlayer().getBlockPos();

        // Resolve build mode
        resolvedMode = resolveMode(mod, pos);

        Debug.logMessage("DimensionAwareBuild: dim=" + buildDimension
                + " mode=" + resolvedMode
                + " pos=" + pos.toShortString()
                + (schematicSize != null ? " size=" + schematicSize.getX() + "x" + schematicSize.getY() + "x" + schematicSize.getZ() : ""));

        // Validate Y range
        if (!buildDimension.isValidBuildY(pos.getY())) {
            Debug.logMessage("WARNING: Build Y=" + pos.getY() + " is outside " + buildDimension + " range ["
                    + buildDimension.getMinY() + ", " + buildDimension.getMaxY() + ")");
        }

        // Nether ceiling warning
        if (buildDimension == Dimension.NETHER && buildDimension.hasCeiling() && pos.getY() > 120) {
            Debug.logMessage("WARNING: Building near Nether ceiling (Y=" + pos.getY() + "), limited space!");
        }

        // Set up prepare task based on mode
        switch (resolvedMode) {
            case ELEVATED:
                prepareTask = new ScaffoldTask(pos, 2);
                break;
            case UNDERGROUND:
                if (schematicSize != null) {
                    prepareTask = new ExcavateAreaTask(pos, schematicSize.getX(), schematicSize.getY(), schematicSize.getZ());
                } else {
                    // Default excavation size if schematic size unknown
                    prepareTask = new ExcavateAreaTask(pos, 16, 8, 16);
                }
                break;
            case SURFACE:
            default:
                // No preparation needed, go straight to building
                prepareTask = null;
                phase = Phase.BUILDING;
                break;
        }

        // Create the actual build task
        BlockPos buildPos = (startPos != null) ? startPos : mod.getPlayer().getBlockPos();
        buildTask = new SchematicBuildTask(schematicFileName, buildPos, 3, rotationSteps);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Check if dimension changed mid-build
        Dimension currentDim = Dimension.current();
        if (currentDim != buildDimension) {
            Debug.logMessage("DimensionAwareBuild: dimension changed from " + buildDimension + " to " + currentDim + "! Pausing...");
            return null;
        }

        switch (phase) {
            case PREPARING:
                return tickPreparing(mod);
            case BUILDING:
                return tickBuilding(mod);
            default:
                return null;
        }
    }

    private Task tickPreparing(AltoClef mod) {
        if (prepareTask == null || prepareTask.isFinished(mod)) {
            Debug.logMessage("DimensionAwareBuild: preparation complete, starting build");
            phase = Phase.BUILDING;
            return tickBuilding(mod);
        }
        setDebugState("Preparing: " + resolvedMode.name());
        return prepareTask;
    }

    private Task tickBuilding(AltoClef mod) {
        if (buildTask.isFinished(mod)) {
            phase = Phase.FINISHED;
            return null;
        }
        setDebugState("Building [" + buildDimension + "/" + resolvedMode + "]");
        return buildTask;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return phase == Phase.FINISHED || (buildTask != null && buildTask.isFinished(mod));
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof DimensionAwareBuildTask task) {
            return task.schematicFileName.equals(schematicFileName)
                    && task.rotationSteps == rotationSteps
                    && task.requestedMode == requestedMode;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "DimensionAwareBuild[" + phase + " " + resolvedMode + " " + buildDimension + "]";
    }

    private BuildMode resolveMode(AltoClef mod, BlockPos pos) {
        if (requestedMode != BuildMode.AUTO) {
            return requestedMode;
        }

        // Auto-detect based on environment
        int surfaceY = getSurfaceY(mod, pos);

        if (buildDimension.isUnderground(pos.getY(), surfaceY)) {
            return BuildMode.UNDERGROUND;
        }
        if (buildDimension.isElevated(pos.getY(), surfaceY)) {
            return BuildMode.ELEVATED;
        }
        return BuildMode.SURFACE;
    }

    private int getSurfaceY(AltoClef mod, BlockPos pos) {
        if (mod.getWorld() == null) return 64;

        // Scan down from max height to find first solid block at this X/Z
        int maxY = buildDimension.getMaxY();
        if (buildDimension == Dimension.NETHER) maxY = 127;

        for (int y = maxY; y >= buildDimension.getMinY(); y--) {
            BlockPos check = new BlockPos(pos.getX(), y, pos.getZ());
            if (WorldHelper.isSolid(mod, check)) {
                return y;
            }
        }
        return buildDimension.getMinY();
    }

    private Vec3i loadSchematicSize() {
        try {
            File file = new File("schematics/" + schematicFileName);
            if (!file.exists()) return null;
            Optional<ISchematicFormat> format = SchematicSystem.INSTANCE.getByFile(file);
            if (!format.isPresent()) return null;
            ISchematic schem = format.get().parse(new FileInputStream(file));
            return new Vec3i(schem.widthX(), schem.heightY(), schem.lengthZ());
        } catch (Exception e) {
            return null;
        }
    }
}
