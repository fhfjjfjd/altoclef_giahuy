package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasksystem.Task;

public class AutoMineCommand extends Command {
    public AutoMineCommand() throws CommandException {
        super("automine", "Auto mine resource veins using catalogue name: @automine <resource> [count]",
                new Arg(String.class, "resource"),
                new Arg(Integer.class, "count", 1, 1));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String resource = parser.get(String.class);
        int count = parser.get(Integer.class);
        Task task = TaskCatalogue.getItemTask(resource, count);
        if (task == null) throw new CommandException("Unknown resource: " + resource);
        mod.runUserTask(task, this::finish);
    }
}
