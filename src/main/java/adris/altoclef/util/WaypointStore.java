package adris.altoclef.util;

import adris.altoclef.Debug;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class WaypointStore {
    private static final Path PATH = Path.of("altoclef_waypoints.txt");
    private final Map<String, BlockPos> waypoints = new HashMap<>();

    public WaypointStore() {
        load();
    }

    public void saveWaypoint(String name, BlockPos pos) {
        waypoints.put(name.toLowerCase(), pos);
        flush();
    }

    public BlockPos getWaypoint(String name) {
        return waypoints.get(name.toLowerCase());
    }

    public boolean removeWaypoint(String name) {
        BlockPos removed = waypoints.remove(name.toLowerCase());
        flush();
        return removed != null;
    }

    public Map<String, BlockPos> getAllWaypoints() {
        return Collections.unmodifiableMap(waypoints);
    }

    private void load() {
        waypoints.clear();
        if (!Files.exists(PATH)) return;
        try (Stream<String> lines = Files.lines(PATH)) {
            lines.forEach(line -> {
                String[] p = line.split(":");
                if (p.length != 4) return;
                try {
                    int x = Integer.parseInt(p[1]);
                    int y = Integer.parseInt(p[2]);
                    int z = Integer.parseInt(p[3]);
                    waypoints.put(p[0].toLowerCase(), new BlockPos(x, y, z));
                } catch (NumberFormatException ignored) {
                }
            });
        } catch (IOException e) {
            Debug.logWarning("Failed to load waypoints: " + e.getMessage());
        }
    }

    private void flush() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, BlockPos> e : waypoints.entrySet()) {
            BlockPos p = e.getValue();
            sb.append(e.getKey()).append(":")
                    .append(p.getX()).append(":")
                    .append(p.getY()).append(":")
                    .append(p.getZ()).append("\n");
        }
        try {
            Files.writeString(PATH, sb.toString());
        } catch (IOException e) {
            Debug.logWarning("Failed to save waypoints: " + e.getMessage());
        }
    }
}
