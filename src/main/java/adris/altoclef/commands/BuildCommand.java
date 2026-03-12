package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.DimensionAwareBuildTask;
import adris.altoclef.tasks.DimensionAwareBuildTask.BuildMode;

public class BuildCommand extends Command {
    public BuildCommand() throws CommandException {
        super("build", "Build schematic. Usage: @build <file.schem> [0/90/180/270] [auto/elevated/underground/surface]",
                new Arg(String.class, "filename", "", 0),
                new Arg(Integer.class, "rotation", 0, 0),
                new Arg(String.class, "mode", "auto", 0));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String name = "";
        int rotation = 0;
        String modeStr = "auto";

        try {
            name = parser.get(String.class);
        } catch (CommandException e) {
            Debug.logError("Usage: @build <file.schem> [0/90/180/270] [auto/elevated/underground/surface]");
            return;
        }
        try {
            rotation = parser.get(Integer.class);
        } catch (CommandException ignored) {
        }
        try {
            modeStr = parser.get(String.class);
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

        // Parse build mode
        BuildMode mode;
        switch (modeStr.toLowerCase()) {
            case "elevated":
            case "high":
            case "sky":
                mode = BuildMode.ELEVATED;
                break;
            case "underground":
            case "under":
            case "dig":
                mode = BuildMode.UNDERGROUND;
                break;
            case "surface":
            case "ground":
                mode = BuildMode.SURFACE;
                break;
            default:
                mode = BuildMode.AUTO;
                break;
        }

        if (rotationSteps != 0) {
            Debug.logMessage("Building " + name + " with " + rotation + "° rotation, mode=" + mode.name());
        } else {
            Debug.logMessage("Building " + name + ", mode=" + mode.name());
        }

        mod.runUserTask(new DimensionAwareBuildTask(name, rotationSteps, mode));
    }
}
