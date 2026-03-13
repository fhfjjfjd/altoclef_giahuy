package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.AutoSmeltTask;

public class AutoSmeltCommand extends Command {
    public AutoSmeltCommand() throws CommandException {
        super("autosmelt", "Automatically smelt raw materials in inventory. Usage: @autosmelt [smelt_all]",
                new Arg(Boolean.class, "smelt_all", false, 0));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        boolean smeltAll = false; // Default to smelt until targets met

        try {
            smeltAll = parser.get(Boolean.class);
        } catch (CommandException ignored) {
            // Use default if not provided
        }

        if (smeltAll) {
            mod.log("Starting auto-smelt to process all available materials...");
        } else {
            mod.log("Starting auto-smelt to meet target counts...");
        }
        
        mod.runUserTask(new AutoSmeltTask(smeltAll));
    }
}