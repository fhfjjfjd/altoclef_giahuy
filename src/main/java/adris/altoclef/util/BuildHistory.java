package adris.altoclef.util;

import adris.altoclef.AltoClef;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/**
 * Tracks build history to enable undo functionality.
 */
public class BuildHistory {
    
    // Represents a build operation
    public static class BuildOperation {
        public final Map<BlockPos, BlockState> _originalBlocks;  // Blocks before build
        public final Map<BlockPos, BlockState> _builtBlocks;     // Blocks after build
        public final String _schematicName;                      // Name of schematic built
        public final long _timestamp;                            // When build was completed
        
        public BuildOperation(String schematicName) {
            _originalBlocks = new HashMap<>();
            _builtBlocks = new HashMap<>();
            _schematicName = schematicName;
            _timestamp = System.currentTimeMillis();
        }
        
        public int getBlockCount() {
            return _builtBlocks.size();
        }
    }
    
    private static final int MAX_HISTORY_SIZE = 10; // Keep last 10 builds
    private final LinkedList<BuildOperation> _buildHistory = new LinkedList<>();
    
    /**
     * Record a build operation by storing original and built blocks
     */
    public synchronized void recordBuild(String schematicName, Map<BlockPos, BlockState> originalBlocks, Map<BlockPos, BlockState> builtBlocks) {
        BuildOperation operation = new BuildOperation(schematicName);
        operation._originalBlocks.putAll(originalBlocks);
        operation._builtBlocks.putAll(builtBlocks);
        
        _buildHistory.addFirst(operation); // Add to front (most recent first)
        
        // Keep history size reasonable
        while (_buildHistory.size() > MAX_HISTORY_SIZE) {
            _buildHistory.removeLast(); // Remove oldest
        }
    }
    
    /**
     * Get the most recent build operation
     */
    public synchronized BuildOperation getLastBuild() {
        if (_buildHistory.isEmpty()) {
            return null;
        }
        return _buildHistory.getFirst();
    }
    
    /**
     * Get build operation by index (0 = most recent, 1 = second most recent, etc.)
     */
    public synchronized BuildOperation getBuild(int index) {
        if (index < 0 || index >= _buildHistory.size()) {
            return null;
        }
        return _buildHistory.get(index);
    }
    
    /**
     * Get number of recorded builds
     */
    public synchronized int getBuildCount() {
        return _buildHistory.size();
    }
    
    /**
     * Clear all build history
     */
    public synchronized void clear() {
        _buildHistory.clear();
    }
    
    /**
     * Remove a specific build operation
     */
    public synchronized void removeBuild(int index) {
        if (index >= 0 && index < _buildHistory.size()) {
            _buildHistory.remove(index);
        }
    }
}