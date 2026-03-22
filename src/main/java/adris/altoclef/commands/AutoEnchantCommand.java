package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.AutoEnchantTask;

public class AutoEnchantCommand extends Command {
    public AutoEnchantCommand() throws CommandException {
        super("autoenchant", "Automatically find or craft an enchanting table and enchant gear. Usage: @autoenchant");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        mod.log("Starting auto-enchant...");
        mod.runUserTask(new AutoEnchantTask(), this::finish);
    }
}
