package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.AutoFishTask;

public class AutoFishCommand extends Command {
    public AutoFishCommand() throws CommandException {
        super("autofish", "Automatically fish. Usage: @autofish");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        mod.log("Starting auto-fishing...");
        mod.runUserTask(new AutoFishTask(), this::finish);
    }
}
