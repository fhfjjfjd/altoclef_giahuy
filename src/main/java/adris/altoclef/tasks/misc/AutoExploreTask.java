package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;

/**
 * Task to automatically explore new chunks.
 */
public class AutoExploreTask extends Task {
    
    private final Set<ChunkPos> _exploredChunks = new HashSet<>();
    private TimeoutWanderTask _wanderTask = new TimeoutWanderTask(10); // Wander for 10 seconds at a time
    private int _explorationRadius = 50; // Explore within 50 blocks radius initially
    
    public AutoExploreTask() {
        this(50);
    }
    
    public AutoExploreTask(int explorationRadius) {
        _explorationRadius = explorationRadius;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Starting auto-exploration...");
        // Record currently loaded chunks as already explored
        updateExploredChunks(mod);
        _wanderTask.resetWander();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Update our record of explored chunks
        updateExploredChunks(mod);
        
        // Check if there are new chunks to explore
        ChunkPos newChunk = findUnexploredChunk(mod);
        if (newChunk != null) {
            mod.log("Found unexplored chunk at: " + newChunk.x + ", " + newChunk.z);
            // For now, we'll just wander to explore - in a real implementation this would be more targeted
            return _wanderTask;
        } else {
            mod.log("No unexplored chunks found in current area. Expanding search radius...");
            // If no unexplored chunks nearby, expand the exploration radius
            _explorationRadius += 20;
            return _wanderTask;
        }
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.log("Auto-exploration task stopped.");
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof AutoExploreTask task) {
            return task._explorationRadius == _explorationRadius;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Auto-exploring, radius=" + _explorationRadius + ", explored chunks=" + _exploredChunks.size();
    }
    
    @Override
    public boolean isFinished(AltoClef mod) {
        // Auto-explore runs indefinitely until stopped
        return false;
    }
    
    /**
     * Update the set of explored chunks based on currently loaded chunks.
     */
    private void updateExploredChunks(AltoClef mod) {
        if (mod.getWorld() != null) {
            // Use the chunk tracker to get currently loaded chunks
            for (ChunkPos chunkPos : mod.getChunkTracker().getLoadedChunks()) {
                _exploredChunks.add(chunkPos);
            }
        }
    }
    
    /**
     * Find an unexplored chunk near the player.
     */
    private ChunkPos findUnexploredChunk(AltoClef mod) {
        if (mod.getPlayer() == null) return null;
        
        ChunkPos playerChunk = new ChunkPos(mod.getPlayer().getBlockPos());
        
        // Search in a square around the player
        int radiusInChunks = (int) Math.ceil(_explorationRadius / 16.0);
        
        for (int dx = -radiusInChunks; dx <= radiusInChunks; dx++) {
            for (int dz = -radiusInChunks; dz <= radiusInChunks; dz++) {
                ChunkPos checkPos = new ChunkPos(playerChunk.x + dx, playerChunk.z + dz);
                
                // Check if this chunk is within the exploration radius
                Vec3d chunkCenter = new Vec3d(
                    (checkPos.x << 4) + 8, 
                    mod.getPlayer().getY(), 
                    (checkPos.z << 4) + 8
                );
                
                if (mod.getPlayer().getPos().distanceTo(chunkCenter) <= _explorationRadius) {
                    if (!_exploredChunks.contains(checkPos)) {
                        return checkPos;
                    }
                }
            }
        }
        
        return null; // No unexplored chunks found
    }
}