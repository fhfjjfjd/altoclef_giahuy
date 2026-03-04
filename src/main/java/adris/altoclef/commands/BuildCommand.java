package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.SchematicBuildTask;

public class BuildCommand extends Command {
    public BuildCommand() throws CommandException {
        super("build", "Build schematic with rotation. Usage: @build <file.schem> [0/90/180/270]",
                new Arg(String.class, "filename", "", 0),
                new Arg(Integer.class, "rotation", 0, 0));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String name = "";
        int rotation = 0;
        try {
            name = parser.get(String.class);
        } catch (CommandException e) {
            Debug.logError("Cannot parse parameter. Input format: '@build house.schem [0/90/180/270]'");
            return;
        }
        try {
            rotation = parser.get(Integer.class);
        } catch (CommandException ignored) {
        }

        // Convert degrees to rotation steps (0-3)
        int rotationSteps;
        switch (rotation) {
            case 90:  rotationSteps = 1; break;
            case 180: rotationSteps = 2; break;
            case 270: rotationSteps = 3; break;
            default:  rotationSteps = 0; break;
        }

        if (rotationSteps != 0) {
            Debug.logMessage("Building " + name + " with " + rotation + "° rotation");
        }

        mod.runUserTask(new SchematicBuildTask(name, rotationSteps));
    }
}
