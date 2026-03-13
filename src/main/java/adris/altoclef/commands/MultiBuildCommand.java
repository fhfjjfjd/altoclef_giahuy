package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.MultiSchematicBuildTask;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.List;

public class MultiBuildCommand extends Command {
    public MultiBuildCommand() throws CommandException {
        super("multibuild", "Build multiple schematics in sequence. Usage: @multibuild <file1.schem>;<file2.schem>;... [x] [y] [z]",
                new Arg(String.class, "files", "", 0),
                new Arg(Integer.class, "x", 0, 1),
                new Arg(Integer.class, "y", 0, 1),
                new Arg(Integer.class, "z", 0, 1));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String filesStr = "";
        int x = 0, y = 0, z = 0;

        try {
            filesStr = parser.get(String.class);
        } catch (CommandException e) {
            Debug.logError("Usage: @multibuild <file1.schem>;[file2.schem];... [x] [y] [z]");
            Debug.logError("Example: @multibuild house.schem;garden.schem;fence.schem 10 64 10");
            return;
        }
        
        // Try to parse coordinates
        try {
            x = parser.get(Integer.class);
            y = parser.get(Integer.class);
            z = parser.get(Integer.class);
        } catch (CommandException ignored) {
            // Use default coordinates if not provided
            BlockPos playerPos = mod.getPlayer().getBlockPos();
            x = playerPos.getX();
            y = playerPos.getY();
            z = playerPos.getZ();
        }

        // Parse the file list (separated by semicolons)
        String[] fileArray = filesStr.split(";");
        List<String> fileList = Arrays.asList(fileArray);
        
        // Remove any empty entries
        fileList.removeIf(String::isEmpty);
        
        if (fileList.isEmpty()) {
            Debug.logError("No schematic files provided. Usage: @multibuild <file1.schem>;[file2.schem];...");
            return;
        }

        Debug.logMessage("Building " + fileList.size() + " schematics in sequence: " + String.join(", ", fileList));
        
        BlockPos startPos = new BlockPos(x, y, z);
        mod.runUserTask(new MultiSchematicBuildTask(fileList, startPos));
    }
}