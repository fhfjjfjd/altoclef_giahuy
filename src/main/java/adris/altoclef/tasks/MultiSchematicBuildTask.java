package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;

public class MultiSchematicBuildTask extends Task {

    private final String[] files;
    private final int rotationSteps;
    private int index;
    private Task current;

    public MultiSchematicBuildTask(String[] files, int rotationSteps) {
        this.files = files;
        this.rotationSteps = rotationSteps;
    }

    @Override
    protected void onStart(AltoClef mod) {
        index = 0;
        current = null;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (index >= files.length) return null;
        if (current == null) {
            current = new SchematicBuildTask(files[index].trim(), rotationSteps);
        }
        if (current.isFinished(mod)) {
            index++;
            current = null;
            return null;
        }
        return current;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return index >= files.length;
    }

    @Override
    protected boolean isEqual(Task other) {
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Multi schematic build (" + index + "/" + files.length + ")";
    }
}
