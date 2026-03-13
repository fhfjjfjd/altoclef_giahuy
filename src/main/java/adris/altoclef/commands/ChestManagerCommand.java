package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.ChestSortTask;

public class ChestManagerCommand extends Command {
    public ChestManagerCommand() throws CommandException {
        super("chestmanager", "Automatically sort items into chests by category. Usage: @chestmanager [category1] [category2]...",
                new Arg(String.class, "categories", "", 0));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        // Get all remaining arguments as categories
        String[] categories = parser.getRemainingArgs(String.class);
        
        if (categories.length == 0) {
            // No categories specified, sort all items
            mod.log("Starting chest management to sort all items by category...");
            mod.runUserTask(new ChestSortTask());
        } else {
            // Sort specific categories
            mod.log("Starting chest management for categories: " + String.join(", ", categories));
            mod.runUserTask(new ChestSortTask(false, categories));
        }
    }
}