package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.AlertTask;

public class AlertCommand extends Command {
    public AlertCommand() throws CommandException {
        super("alert", "Toggle danger alert system on/off. Usage: @alert");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        mod.log("Starting alert system...");
        mod.runUserTask(new AlertTask(), this::finish);
    }
}
