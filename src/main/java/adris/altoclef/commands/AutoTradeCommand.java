package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.AutoVillagerTradeTask;
import adris.altoclef.util.ItemTarget;

public class AutoTradeCommand extends Command {
    public AutoTradeCommand() throws CommandException {
        super("autotrade", "Automatically trade with villagers. Usage: @autotrade <item> <count>",
                new Arg(String.class, "item", "", 0),
                new Arg(Integer.class, "count", 1, 1));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String itemStr = "";
        int count = 1;

        try {
            itemStr = parser.get(String.class);
        } catch (CommandException e) {
            Debug.logError("Usage: @autotrade <item> <count>");
            return;
        }

        try {
            count = parser.get(Integer.class);
        } catch (CommandException ignored) {
            // Use default if not provided
        }

        if (count <= 0) {
            Debug.logError("Count must be positive.");
            return;
        }

        // Try to get the item target
        ItemTarget itemTarget = TaskCatalogue.getItemTarget(itemStr);
        if (itemTarget == null) {
            Debug.logError("Unknown item: " + itemStr);
            return;
        }

        Debug.logMessage("Starting auto-trading for " + count + " " + itemStr);
        mod.runUserTask(new AutoVillagerTradeTask(itemTarget, count));
    }
}