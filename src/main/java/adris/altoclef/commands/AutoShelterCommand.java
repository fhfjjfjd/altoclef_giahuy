package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.AutoShelterTask;

public class AutoShelterCommand extends Command {
    public AutoShelterCommand() throws CommandException {
        super("autoshelter", "Automatically build a cobblestone shelter at night. Usage: @autoshelter");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        mod.log("Starting auto-shelter...");
        mod.runUserTask(new AutoShelterTask(), this::finish);
    }
}
