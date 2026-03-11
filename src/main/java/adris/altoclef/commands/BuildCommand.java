package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.SchematicBuildTask;
import net.minecraft.util.math.BlockPos;

public class BuildCommand extends Command {
    public BuildCommand() throws CommandException {
        super("build", "Build schematic with rotation + vertical offset. Usage: @build <file.schem> [0/90/180/270] [yOffset]",
                new Arg(String.class, "filename", "", 0),
                new Arg(Integer.class, "rotation", 0, 0),
                new Arg(Integer.class, "yOffset", 0, 0));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String name = "";
        int rotation = 0;
        int yOffset = 0;
        try {
            name = parser.get(String.class);
        } catch (CommandException e) {
            Debug.logError("Cannot parse parameter. Input format: '@build house.schem [0/90/180/270] [yOffset]'");
            return;
        }
        try {
            rotation = parser.get(Integer.class);
        } catch (CommandException ignored) {
        }
        try {
            yOffset = parser.get(Integer.class);
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

        BlockPos startPos = mod.getPlayer().getBlockPos().add(0, yOffset, 0);
        if (rotationSteps != 0 || yOffset != 0) {
            Debug.logMessage("Building " + name + " with " + rotation + "° rotation at Y offset " + yOffset +
                    " (start: " + startPos.getX() + ", " + startPos.getY() + ", " + startPos.getZ() + ")");
        }

        mod.runUserTask(new SchematicBuildTask(name, startPos, 3, rotationSteps));
    }
}
