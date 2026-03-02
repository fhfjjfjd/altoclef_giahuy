package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.InventoryTracker;
import adris.altoclef.util.CubeBounds;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.SchematicBlockMapper;
import adris.altoclef.util.Utils;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.schematic.ISchematic;
import baritone.process.BuilderProcess;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.io.File;
import java.util.*;

public class SchematicBuildTask extends Task {

    public enum BuildState {
        BUILDING,
        SOURCING,
        RECOVERING
    }

    private boolean finished;
    private BuilderProcess builder;
    private String schematicFileName;
    private BlockPos startPos;
    private int allowedResourceStackCount;
    private Vec3i schemSize;
    private CubeBounds bounds;
    private Map<BlockState, Integer> missing;
    private boolean addedAvoidance;
    private boolean clearRunning = false;
    private String name;
    private ISchematic schematic;

    private BuildState buildState = BuildState.BUILDING;
    private List<BlockState> sourcingBatch;
    private int sourcingIndex;

    private static final int FOOD_UNITS = 80;
    private static final int MIN_FOOD_UNITS = 10;
    private static final int STUCK_TIMEOUT_SECONDS = 120;

    private final TimerGame _clickTimer = new TimerGame(STUCK_TIMEOUT_SECONDS);
    private final MovementProgressChecker _moveChecker = new MovementProgressChecker(4, 0.1, 4, 0.01);
    private Task walkAroundTask;

    public SchematicBuildTask(final String schematicFileName) {
        this(schematicFileName, new BlockPos(MinecraftClient.getInstance().player.getPos()));
    }

    public SchematicBuildTask(final String schematicFileName, final BlockPos startPos) {
        this(schematicFileName, startPos, 3);
    }

    public SchematicBuildTask(final String schematicFileName, final BlockPos startPos, final int allowedResourceStackCount) {
        this();
        this.schematicFileName = schematicFileName;
        this.startPos = startPos;
        this.allowedResourceStackCount = allowedResourceStackCount;
    }

    public SchematicBuildTask() {
        this.addedAvoidance = false;
    }

    public SchematicBuildTask(String name, ISchematic schematic, final BlockPos startPos) {
        this();
        this.name = name;
        this.schematic = schematic;
        this.startPos = startPos;
    }

    public BuildState getBuildState() {
        return buildState;
    }

    @Override
    protected void onStart(AltoClef mod) {
        this.finished = false;
        this.buildState = BuildState.BUILDING;
        this.sourcingBatch = null;
        this.sourcingIndex = 0;

        if (builder == null) {
            builder = mod.getClientBaritone().getBuilderProcess();
        }

        if (schematicFileName != null) {
            final File file = new File("schematics/" + schematicFileName);
            if (!file.exists()) {
                Debug.logMessage("Could not locate schematic file. Terminating...");
                this.finished = true;
                return;
            }
        }

        builder.clearState();

        if (Utils.isNull(this.schematic)) {
            builder.build(schematicFileName, startPos, true);
        } else {
            builder.build(this.name, this.schematic, startPos);
        }

        if (schemSize == null) {
            this.schemSize = builder.getSchemSize();
        }

        if (schemSize != null && builder.isFromAltoclef() && !this.addedAvoidance) {
            this.bounds = new CubeBounds(
                    mod.getPlayer().getBlockPos(),
                    this.schemSize.getX(), this.schemSize.getY(), this.schemSize.getZ(),
                    mod.getCurrentDimension()
            );
            this.addedAvoidance = true;
            mod.addToAvoidanceFile(this.bounds);
            mod.reloadAvoidanceFile();
            mod.unsetAvoidanceOf(this.bounds);
        }

        _moveChecker.reset();
        _clickTimer.reset();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (clearRunning && builder.isActive()) {
            return null;
        }
        clearRunning = false;

        this.missing = builder.getMissing();

        switch (buildState) {
            case BUILDING:
                return tickBuilding(mod);
            case SOURCING:
                return tickSourcing(mod);
            case RECOVERING:
                return tickRecovering(mod);
        }
        return null;
    }

    private Task tickBuilding(AltoClef mod) {
        // Unset avoidance so bot can enter build area
        if (bounds != null) {
            mod.unsetAvoidanceOf(bounds);
        }

        // Check if we need materials
        boolean needsMaterials = missing != null && !missing.isEmpty()
                && (builder.isPaused() || !builder.isFromAltoclef() || !builder.isActive());

        if (needsMaterials) {
            transitionToSourcing(mod);
            return tickSourcing(mod);
        }

        // Stuck detection
        if (_moveChecker.check(mod)) {
            _clickTimer.reset();
        }
        if (_clickTimer.elapsed()) {
            transitionToRecovering(mod);
            return tickRecovering(mod);
        }

        return null;
    }

    private void transitionToSourcing(AltoClef mod) {
        buildState = BuildState.SOURCING;
        Debug.logMessage("State -> SOURCING");

        // Enable avoidance to protect build area while sourcing
        if (bounds != null && !mod.inAvoidance(bounds)) {
            mod.setAvoidanceOf(bounds);
        }

        // Snapshot the entire batch of needed materials (sourcing hysteresis)
        sourcingBatch = computeSourcingBatch(mod, missing);
        sourcingIndex = 0;
    }

    private Task tickSourcing(AltoClef mod) {
        // Food check first
        if (mod.getInventoryTracker().totalFoodScore() < MIN_FOOD_UNITS) {
            return new CollectFoodTask(FOOD_UNITS);
        }

        // Refresh missing list to see if we've gathered enough
        this.missing = builder.getMissing();

        // Work through the sourcing batch
        while (sourcingIndex < sourcingBatch.size()) {
            BlockState state = sourcingBatch.get(sourcingIndex);
            // Check if we still need this block type
            if (missing != null && missing.containsKey(state)) {
                Item item = SchematicBlockMapper.getObtainableItem(state);
                if (item == null) {
                    // Unobtainable block (piston_head, fire, etc.), skip
                    sourcingIndex++;
                    continue;
                }
                int have = mod.getInventoryTracker().getItemCount(item);
                int need = missing.get(state);
                if (have < need) {
                    return TaskCatalogue.getItemTask(item, need);
                }
            }
            // Already have enough of this type, move to next
            sourcingIndex++;
        }

        // All materials sourced, transition back to building
        transitionToBuilding(mod);
        return null;
    }

    private void transitionToBuilding(AltoClef mod) {
        buildState = BuildState.BUILDING;
        Debug.logMessage("State -> BUILDING (all materials sourced)");

        // Disable avoidance so bot can enter build area
        if (bounds != null) {
            mod.unsetAvoidanceOf(bounds);
        }

        // Resume builder
        if (builder.isPaused()) {
            builder.resume();
        }

        sourcingBatch = null;
        sourcingIndex = 0;
        _moveChecker.reset();
        _clickTimer.reset();
    }

    private void transitionToRecovering(AltoClef mod) {
        buildState = BuildState.RECOVERING;
        Debug.logMessage("State -> RECOVERING (stuck detected)");
        walkAroundTask = new RandomRadiusGoalTask(mod.getPlayer().getBlockPos(), 5d)
                .next(mod.getPlayer().getBlockPos());
    }

    private Task tickRecovering(AltoClef mod) {
        if (walkAroundTask != null && !walkAroundTask.isFinished(mod)) {
            return walkAroundTask;
        }

        // Recovery complete, go back to building
        walkAroundTask = null;
        builder.popStack();
        buildState = BuildState.BUILDING;
        Debug.logMessage("State -> BUILDING (recovery complete)");
        _clickTimer.reset();
        _moveChecker.reset();
        return null;
    }

    /**
     * Compute the batch of block types to collect, respecting allowedResourceStackCount.
     * This is the "sourcing hysteresis" - we collect ALL needed types before returning to building.
     */
    private List<BlockState> computeSourcingBatch(AltoClef mod, Map<BlockState, Integer> missing) {
        final InventoryTracker inventory = mod.getInventoryTracker();
        int finishedStacks = 0;
        final List<BlockState> batch = new ArrayList<>();

        for (final BlockState state : missing.keySet()) {
            if (finishedStacks >= this.allowedResourceStackCount) break;

            final Item item = SchematicBlockMapper.getObtainableItem(state);
            if (item == null) continue; // skip unobtainable blocks
            final int count = inventory.getItemCount(item);
            final int maxCount = item.getMaxCount();
            final int needed = missing.get(state);

            if (count >= needed) {
                finishedStacks++;
            } else if (count >= maxCount) {
                finishedStacks += (int) Math.ceil((double) count / maxCount);
                if (finishedStacks < this.allowedResourceStackCount) {
                    batch.add(state);
                }
            } else {
                batch.add(state);
            }
        }

        return batch;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        builder.pause();
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof SchematicBuildTask;
    }

    @Override
    protected String toDebugString() {
        return "SchematicBuilderTask[" + buildState + "]";
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        if ((builder != null && builder.isFromAltoclefFinished()) || this.finished) {
            mod.loadAvoidanceFile();
            return true;
        }
        return false;
    }
}
