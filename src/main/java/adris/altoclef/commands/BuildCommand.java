package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.BuildInDimensionTask;
import adris.altoclef.util.Dimension;

public class BuildCommand extends Command {
    public BuildCommand() throws CommandException {
        super("build", "Build schematic. Usage: @build <file.schem> [rotation 0/90/180/270] [dimension current/overworld/nether/end]",
                new Arg(String.class, "filename", "", 0),
                new Arg(String.class, "arg2", "", 0, false),
                new Arg(String.class, "arg3", "", 0, false));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String name = parser.get(String.class);
        String arg2 = parser.get(String.class).trim();
        String arg3 = parser.get(String.class).trim();

        int rotationSteps = 0;
        Dimension targetDimension = null; // null = build in current dimension

        if (!arg2.isEmpty()) {
            if (isRotationToken(arg2)) {
                rotationSteps = toRotationSteps(Integer.parseInt(arg2));
            } else {
                targetDimension = parseDimension(arg2);
            }
        }

        if (!arg3.isEmpty()) {
            if (isRotationToken(arg3)) {
                rotationSteps = toRotationSteps(Integer.parseInt(arg3));
            } else {
                targetDimension = parseDimension(arg3);
            }
        }

        if (rotationSteps != 0) {
            Debug.logMessage("Building " + name + " with " + (rotationSteps * 90) + "° rotation");
        }
        if (targetDimension != null) {
            Debug.logMessage("Target build dimension: " + targetDimension);
        }

        mod.runUserTask(new BuildInDimensionTask(name, rotationSteps, targetDimension));
    }

    private boolean isRotationToken(String raw) {
        try {
            int value = Integer.parseInt(raw);
            return value == 0 || value == 90 || value == 180 || value == 270;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int toRotationSteps(int rotation) {
        return switch (rotation) {
            case 90 -> 1;
            case 180 -> 2;
            case 270 -> 3;
            default -> 0;
        };
    }

    private Dimension parseDimension(String raw) throws CommandException {
        String normalized = raw.trim().toLowerCase();
        return switch (normalized) {
            case "current", "here" -> null;
            case "overworld", "ow" -> Dimension.OVERWORLD;
            case "nether" -> Dimension.NETHER;
            case "end", "the_end" -> Dimension.END;
            default -> throw new CommandException("Unknown dimension '" + raw + "'. Use current/overworld/nether/end.");
        };
    }
}
