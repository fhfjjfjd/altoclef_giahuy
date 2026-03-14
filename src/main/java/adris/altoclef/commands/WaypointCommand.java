package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.movement.GetToBlockTask;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class WaypointCommand extends Command {
    public WaypointCommand() throws CommandException {
        super("waypoint", "Manage waypoints. Usage: @waypoint save <name> OR @waypoint go <name> OR @waypoint list OR @waypoint delete <name>",
                new Arg(String.class, "action", "", 0),
                new Arg(String.class, "name", "", 1));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String action = "";
        String name = "";

        try {
            action = parser.get(String.class).toLowerCase();
        } catch (CommandException e) {
            Debug.logError("Usage: @waypoint save <name> OR @waypoint go <name> OR @waypoint list OR @waypoint delete <name>");
            return;
        }

        try {
            name = parser.get(String.class);
        } catch (CommandException ignored) {
            // Name is optional for some actions
        }

        switch (action) {
            case "save":
                if (name == null || name.trim().isEmpty()) {
                    Debug.logError("Usage: @waypoint save <name>");
                    return;
                }
                if (name.isEmpty()) {
                    Debug.logError("Waypoint name cannot be empty.");
                    return;
                }
                mod.getWaypointManager().saveWaypoint(mod, name);
                break;
                
            case "go":
            case "goto":
                if (name == null || name.trim().isEmpty()) {
                    Debug.logError("Usage: @waypoint go <name>");
                    return;
                }
                Vec3d waypointPos = mod.getWaypointManager().getWaypoint(name);
                if (waypointPos == null) {
                    Debug.logError("Waypoint '" + name + "' does not exist. Available waypoints: " + 
                                  String.join(", ", mod.getWaypointManager().getWaypointNames()));
                    return;
                }
                Debug.logMessage("Going to waypoint: " + name + " at " + waypointPos.x + ", " + waypointPos.y + ", " + waypointPos.z);
                mod.runUserTask(new GetToBlockTask(new BlockPos(waypointPos.x, waypointPos.y, waypointPos.z)));
                break;
                
            case "list":
                String[] waypoints = mod.getWaypointManager().getWaypointNames();
                if (waypoints.length == 0) {
                    Debug.logMessage("No waypoints saved.");
                } else {
                    Debug.logMessage("Saved waypoints: " + String.join(", ", waypoints));
                }
                break;
                
            case "delete":
            case "remove":
                if (name == null || name.trim().isEmpty()) {
                    Debug.logError("Usage: @waypoint delete <name>");
                    return;
                }
                if (mod.getWaypointManager().hasWaypoint(name)) {
                    mod.getWaypointManager().removeWaypoint(name);
                    Debug.logMessage("Deleted waypoint: " + name);
                } else {
                    Debug.logError("Waypoint '" + name + "' does not exist.");
                }
                break;
                
            default:
                Debug.logError("Unknown waypoint action: " + action + ". Use save, go, list, or delete.");
                Debug.logError("Usage: @waypoint save <name> OR @waypoint go <name> OR @waypoint list OR @waypoint delete <name>");
                break;
        }
    }
}