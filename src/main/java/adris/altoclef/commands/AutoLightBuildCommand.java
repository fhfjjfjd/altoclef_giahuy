package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.AutoLightBuildTask;
import net.minecraft.util.math.BlockPos;

public class AutoLightBuildCommand extends Command {
    public AutoLightBuildCommand() throws CommandException {
        super("autolightbuild", "Build schematic with automatic torch placement in dark areas. Usage: @autolightbuild <file.schem> [0/90/180/270] [x] [y] [z]",
                new Arg(String.class, "filename", "", 0),
                new Arg(Integer.class, "rotation", 0, 1),
                new Arg(Integer.class, "x", 0, 1),
                new Arg(Integer.class, "y", 0, 1),
                new Arg(Integer.class, "z", 0, 1));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String name = "";
        int rotation = 0;
        int x = 0, y = 0, z = 0;

        try {
            name = parser.get(String.class);
        } catch (CommandException e) {
            Debug.logError("Usage: @autolightbuild <file.schem> [0/90/180/270] [x] [y] [z]");
            return;
        }
        
        // Try to parse rotation and coordinates
        try {
            rotation = parser.get(Integer.class);
        } catch (CommandException ignored) {
        }
        
        try {
            x = parser.get(Integer.class);
            y = parser.get(Integer.class);
            z = parser.get(Integer.class);
        } catch (CommandException ignored) {
            // Use player position if coordinates not provided
            BlockPos playerPos = mod.getPlayer().getBlockPos();
            x = playerPos.getX();
            y = playerPos.getY();
            z = playerPos.getZ();
        }

        // Convert degrees to rotation steps (0-3)
        int rotationSteps;
        switch (rotation) {
            case 90:  rotationSteps = 1; break;
            case 180: rotationSteps = 2; break;
            case 270: rotationSteps = 3; break;
            default:  rotationSteps = 0; break;
        }

        Debug.logMessage("Building " + name + " with auto-lighting, rotation=" + rotation + "°");

        BlockPos startPos = new BlockPos(x, y, z);
        mod.runUserTask(new AutoLightBuildTask(name, startPos, 3, rotationSteps));
    }
}