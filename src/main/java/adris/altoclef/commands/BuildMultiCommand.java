package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.MultiSchematicBuildTask;
import adris.altoclef.util.BuildHistoryStore;

public class BuildMultiCommand extends Command {
    public BuildMultiCommand() throws CommandException {
        super("buildmulti", "Build multiple schematics: @buildmulti file1.schem,file2.schem [0/90/180/270]",
                new Arg(String.class, "files"),
                new Arg(Integer.class, "rotation", 0, 1));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String[] files = parser.get(String.class).split(",");
        int rotation = parser.get(Integer.class);
        int steps;
        switch (rotation) {
            case 90: steps = 1; break;
            case 180: steps = 2; break;
            case 270: steps = 3; break;
            default: steps = 0; break;
        }
        if (files.length > 0) {
            BuildHistoryStore.record(files[files.length - 1].trim(), mod.getPlayer().getBlockPos(), steps);
        }
        mod.runUserTask(new MultiSchematicBuildTask(files, steps), this::finish);
    }
}
