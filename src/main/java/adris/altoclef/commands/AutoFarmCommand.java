package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.resources.AutoFarmTask;

import java.util.function.Consumer;

public class AutoFarmCommand extends Command {

    public AutoFarmCommand() throws CommandException {
        super("auto-farm", "Auto-farm crops. Usage: @auto-farm <crop> [count]. Crops: wheat, carrot, potato, beetroot, sugar_cane, melon, pumpkin, cactus, bamboo, sweet_berries, nether_wart, cocoa_beans",
                new Arg(String.class, "name"),
                new Arg(Integer.class, "count", 100, 1));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String resourceName = parser.get(String.class);
        int count = parser.get(Integer.class);

        mod.runUserTask(new AutoFarmTask(resourceName, count), this::finish);
    }
}
