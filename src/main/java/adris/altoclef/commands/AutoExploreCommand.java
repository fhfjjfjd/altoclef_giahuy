package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.tasks.movement.TimeoutWanderTask;

public class AutoExploreCommand extends Command {
    public AutoExploreCommand() {
        super("autoexplore", "Automatically explore new chunks");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) {
        mod.runUserTask(new TimeoutWanderTask(true), this::finish);
    }
}
