package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.AutoTrashTask;

public class AutoTrashCommand extends Command {
    public AutoTrashCommand() throws CommandException {
        super("autotrash", "Drop junk items from inventory. Usage: @autotrash");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        mod.log("Starting auto-trash...");
        mod.runUserTask(new AutoTrashTask(), this::finish);
    }
}
