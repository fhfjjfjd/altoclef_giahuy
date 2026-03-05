package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.tasks.construction.ClearRegionTask;
import adris.altoclef.util.BuildHistoryStore;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public class UndoBuildCommand extends Command {
    public UndoBuildCommand() {
        super("buildundo", "Undo last build by clearing the last schematic region");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) {
        BlockPos start = BuildHistoryStore.getLastStart();
        Vec3i size = BuildHistoryStore.getLastSize();
        if (start == null || size == null) {
            mod.logWarning("No build history to undo.");
            finish();
            return;
        }
        BlockPos end = start.add(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        mod.runUserTask(new ClearRegionTask(start, end), this::finish);
    }
}
