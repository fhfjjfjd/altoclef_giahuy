package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.UndoBuildTask;

public class UndoBuildCommand extends Command {
    public UndoBuildCommand() throws CommandException {
        super("undobuild", "Undo the last build operation. Usage: @undobuild [build_index]",
                new Arg(Integer.class, "build_index", 0, 0));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        int buildIndex = 0;

        try {
            buildIndex = parser.get(Integer.class);
        } catch (CommandException ignored) {
            // Use default index 0 (most recent build) if not specified
        }

        if (buildIndex < 0) {
            Debug.logError("Build index cannot be negative. Use 0 for most recent build, 1 for second most recent, etc.");
            return;
        }
        
        if (buildIndex >= mod.getBuildHistory().getBuildCount()) {
            Debug.logError("Build index " + buildIndex + " is out of range. Only " + 
                          mod.getBuildHistory().getBuildCount() + " builds in history.");
            return;
        }

        BuildHistory.BuildOperation buildOp = mod.getBuildHistory().getBuild(buildIndex);
        if (buildOp == null) {
            Debug.logError("No build found at index " + buildIndex);
            return;
        }

        Debug.logMessage("Undoing build: " + buildOp._schematicName + " (" + buildOp.getBlockCount() + " blocks)");
        mod.runUserTask(new UndoBuildTask(buildIndex));
    }
}