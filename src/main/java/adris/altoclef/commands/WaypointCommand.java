package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasks.movement.GetToBlockTask;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public class WaypointCommand extends Command {
    public WaypointCommand() throws CommandException {
        super("waypoint", "Waypoint system: @waypoint save/go/remove/list <name>",
                new Arg(String.class, "action"),
                new Arg(String.class, "name", "", 1));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String action = parser.get(String.class).toLowerCase();
        String name = parser.get(String.class);

        switch (action) {
            case "save":
                if (name.isEmpty()) throw new CommandException("Please provide waypoint name.");
                mod.getWaypointStore().saveWaypoint(name, mod.getPlayer().getBlockPos());
                mod.log("Saved waypoint '" + name + "'.");
                finish();
                break;
            case "go":
                if (name.isEmpty()) throw new CommandException("Please provide waypoint name.");
                BlockPos pos = mod.getWaypointStore().getWaypoint(name);
                if (pos == null) throw new CommandException("Waypoint not found: " + name);
                mod.runUserTask(new GetToBlockTask(pos), this::finish);
                break;
            case "remove":
            case "delete":
                if (name.isEmpty()) throw new CommandException("Please provide waypoint name.");
                mod.log(mod.getWaypointStore().removeWaypoint(name)
                        ? "Removed waypoint '" + name + "'."
                        : "Waypoint not found: " + name);
                finish();
                break;
            case "list":
                Map<String, BlockPos> all = mod.getWaypointStore().getAllWaypoints();
                if (all.isEmpty()) {
                    mod.log("No waypoints saved.");
                } else {
                    mod.log("Waypoints:");
                    all.forEach((k, v) -> mod.log(" - " + k + ": " + v.toShortString()));
                }
                finish();
                break;
            default:
                throw new CommandException("Unknown action. Use save/go/remove/list");
        }
    }
}
