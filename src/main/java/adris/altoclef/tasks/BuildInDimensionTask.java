package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;

import java.util.Objects;

/**
 * Goes to a target dimension (if specified) then starts schematic build.
 */
public class BuildInDimensionTask extends Task {

    private final String schematicFileName;
    private final int rotationSteps;
    private final Dimension targetDimension;

    private SchematicBuildTask buildTask;

    public BuildInDimensionTask(String schematicFileName, int rotationSteps, Dimension targetDimension) {
        this.schematicFileName = schematicFileName;
        this.rotationSteps = rotationSteps;
        this.targetDimension = targetDimension;
    }

    @Override
    protected void onStart(AltoClef mod) {
        buildTask = null;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (targetDimension != null && mod.getCurrentDimension() != targetDimension) {
            setDebugState("Moving to " + targetDimension + " before building");
            return new DefaultGoToDimensionTask(targetDimension);
        }

        if (buildTask == null) {
            setDebugState("Starting build in current dimension");
            buildTask = new SchematicBuildTask(schematicFileName, rotationSteps);
        }

        return buildTask;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return buildTask != null && buildTask.isFinished(mod);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof BuildInDimensionTask task) {
            return Objects.equals(task.schematicFileName, schematicFileName)
                    && task.rotationSteps == rotationSteps
                    && task.targetDimension == targetDimension;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Build schematic in dimension task";
    }
}
