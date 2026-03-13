package adris.altoclef.util;

import adris.altoclef.AltoClef;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages waypoints - saving and recalling named locations.
 */
public class WaypointManager {
    
    private final Map<String, Vec3d> _waypoints = new HashMap<>();
    
    /**
     * Save a waypoint at the current player position with the given name.
     */
    public void saveWaypoint(AltoClef mod, String name) {
        Vec3d pos = mod.getPlayer().getPos();
        _waypoints.put(name.toLowerCase(), pos);
        mod.log("Saved waypoint '" + name + "' at " + pos.x + ", " + pos.y + ", " + pos.z);
    }
    
    /**
     * Get the position of a saved waypoint.
     */
    public Vec3d getWaypoint(String name) {
        return _waypoints.get(name.toLowerCase());
    }
    
    /**
     * Check if a waypoint with the given name exists.
     */
    public boolean hasWaypoint(String name) {
        return _waypoints.containsKey(name.toLowerCase());
    }
    
    /**
     * Remove a waypoint.
     */
    public void removeWaypoint(String name) {
        if (_waypoints.remove(name.toLowerCase()) != null) {
            // Waypoint existed and was removed
        }
    }
    
    /**
     * Get all saved waypoint names.
     */
    public String[] getWaypointNames() {
        return _waypoints.keySet().toArray(new String[0]);
    }
    
    /**
     * Clear all waypoints.
     */
    public void clearWaypoints() {
        _waypoints.clear();
    }
    
    /**
     * Get the number of saved waypoints.
     */
    public int getWaypointCount() {
        return _waypoints.size();
    }
}