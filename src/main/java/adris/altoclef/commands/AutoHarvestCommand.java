package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.AutoHarvestTask;

public class AutoHarvestCommand extends Command {
    public AutoHarvestCommand() throws CommandException {
        super("autoharvest", "Automatically harvest and replant mature crops nearby. Usage: @autoharvest");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        mod.log("Starting auto-harvest...");
        mod.runUserTask(new AutoHarvestTask(), this::finish);
    }
}
