package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.AutoMineOreTask;
import adris.altoclef.util.MiningRequirement;

public class AutoMineCommand extends Command {
    public AutoMineCommand() throws CommandException {
        super("automine", "Automatically mine ore veins. Usage: @automine [mining_level] [search_radius]",
                new Arg(String.class, "mining_level", "iron", 0),
                new Arg(Integer.class, "radius", 5, 1));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String miningLevelStr = "iron"; // Default
        int radius = 5; // Default radius

        try {
            miningLevelStr = parser.get(String.class).toLowerCase();
        } catch (CommandException ignored) {
            // Use default if not provided
        }
        
        try {
            radius = parser.get(Integer.class);
        } catch (CommandException ignored) {
            // Use default if not provided
        }

        MiningRequirement miningLevel = MiningRequirement.IRON; // Default
        switch (miningLevelStr) {
            case "hand":
            case "none":
                miningLevel = MiningRequirement.HAND;
                break;
            case "wood":
            case "wooden":
                miningLevel = MiningRequirement.WOOD;
                break;
            case "stone":
                miningLevel = MiningRequirement.STONE;
                break;
            case "iron":
                miningLevel = MiningRequirement.IRON;
                break;
            case "diamond":
                miningLevel = MiningRequirement.DIAMOND;
                break;
            default:
                Debug.logWarning("Unknown mining level: " + miningLevelStr + ", defaulting to iron");
                break;
        }

        if (radius <= 0) {
            Debug.logError("Radius must be positive. Using default value of 5.");
            radius = 5;
        }

        Debug.logMessage("Starting auto mine with mining level: " + miningLevel.name() + " and search radius: " + radius);
        mod.runUserTask(new AutoMineOreTask(miningLevel, radius));
    }
}