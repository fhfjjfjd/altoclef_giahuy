package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.AutoShieldTask;

public class AutoShieldCommand extends Command {
    public AutoShieldCommand() throws CommandException {
        super("autoshield", "Automatically raise shield when hostile mobs are nearby. Usage: @autoshield");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        mod.log("Starting auto-shield...");
        mod.runUserTask(new AutoShieldTask(), this::finish);
    }
}
