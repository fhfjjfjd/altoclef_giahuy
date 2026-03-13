package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.AutoExploreTask;

public class AutoExploreCommand extends Command {
    public AutoExploreCommand() throws CommandException {
        super("autoexplore", "Automatically explore new chunks. Usage: @autoexplore [radius]",
                new Arg(Integer.class, "radius", 50, 0));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        int radius = 50; // Default radius

        try {
            radius = parser.get(Integer.class);
        } catch (CommandException ignored) {
            // Use default if not provided
        }

        if (radius <= 0) {
            Debug.logError("Radius must be positive. Using default value of 50.");
            radius = 50;
        }

        Debug.logMessage("Starting auto-exploration with radius " + radius);
        mod.runUserTask(new AutoExploreTask(radius));
    }
}