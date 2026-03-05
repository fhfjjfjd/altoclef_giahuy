package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasksystem.Task;

public class AutoSmeltCommand extends Command {
    public AutoSmeltCommand() throws CommandException {
        super("autosmelt", "Auto smelt by target output item: @autosmelt <item_name> [count]",
                new Arg(String.class, "item"),
                new Arg(Integer.class, "count", 1, 1));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String item = parser.get(String.class);
        int count = parser.get(Integer.class);
        Task task = TaskCatalogue.getItemTask(item, count);
        if (task == null) throw new CommandException("Unknown smelt target: " + item);
        mod.runUserTask(task, this::finish);
    }
}
