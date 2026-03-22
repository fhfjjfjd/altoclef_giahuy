package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.AutoBridgeTask;

public class AutoBridgeCommand extends Command {
    public AutoBridgeCommand() throws CommandException {
        super("autobridge", "Build a bridge in a direction. Usage: @autobridge <north/south/east/west> [length]",
                new Arg(String.class, "direction"),
                new Arg(Integer.class, "length", 20, 1));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String direction = parser.get(String.class);
        int length = parser.get(Integer.class);

        mod.log("Starting auto-bridge: " + direction + " for " + length + " blocks");
        mod.runUserTask(new AutoBridgeTask(direction, length), this::finish);
    }
}
