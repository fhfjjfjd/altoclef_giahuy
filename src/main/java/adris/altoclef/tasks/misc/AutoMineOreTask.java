package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.Utils;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

/**
 * Task to automatically mine ore veins when found.
 */
public class AutoMineOreTask extends Task {
    
    private final MiningRequirement _miningRequirement;
    private final int _veinSearchRadius;
    
    // Ore blocks to look for
    private static final Block[] ORE_BLOCKS = {
        Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
        Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
        Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
        Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
        Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
        Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
        Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
        Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.NETHER_GOLD_ORE,
        Blocks.NETHER_QUARTZ_ORE
    };
    
    private BlockPos _currentVeinStart = null;
    private Queue<BlockPos> _veinBlocksToMine = new LinkedList<>();
    private Set<BlockPos> _minedBlocks = new HashSet<>();
    private Task _currentMiningTask = null;
    
    public AutoMineOreTask() {
        this(MiningRequirement.IRON, 5); // Default requirement and search radius
    }
    
    public AutoMineOreTask(MiningRequirement miningRequirement, int veinSearchRadius) {
        _miningRequirement = miningRequirement;
        _veinSearchRadius = veinSearchRadius;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Starting auto mine ore task...");
    }

    @Override
    protected Task onTick(AltoClef mod) {
        // Check if we have the required mining tool
        if (!mod.getInventoryTracker().miningRequirementMet(_miningRequirement)) {
            mod.log("Need better mining tool, collecting...");
            return new adris.altoclef.tasks.resources.SatisfyMiningRequirementTask(_miningRequirement);
        }
        
        // If we're currently mining a block, continue that task
        if (_currentMiningTask != null && !_currentMiningTask.isFinished(mod)) {
            return _currentMiningTask;
        }
        
        // If we have blocks in the queue to mine, mine the next one
        if (!_veinBlocksToMine.isEmpty()) {
            BlockPos nextBlock = _veinBlocksToMine.poll();
            if (nextBlock != null && !isBlockMined(mod, nextBlock)) {
                _currentMiningTask = new DestroyBlockTask(nextBlock);
                return _currentMiningTask;
            }
            // If the block was already mined or invalid, continue to next
            _currentMiningTask = null;
            return null;
        }
        
        // Look for a new ore vein to mine
        BlockPos newVeinStart = findNearbyOreVein(mod);
        if (newVeinStart != null) {
            mod.log("Found ore vein at: " + newVeinStart.toShortString());
            _currentVeinStart = newVeinStart;
            
            // Find all connected ore blocks in the vein
            List<BlockPos> veinBlocks = findConnectedOreVein(mod, newVeinStart);
            mod.log("Mining ore vein with " + veinBlocks.size() + " blocks");
            
            // Add all vein blocks to our queue, but skip ones we've already mined
            for (BlockPos block : veinBlocks) {
                if (!_minedBlocks.contains(block) && isBlockValidToMine(mod, block)) {
                    _veinBlocksToMine.add(block);
                    _minedBlocks.add(block); // Mark as going to be mined
                }
            }
            
            // Start mining the first block if available
            if (!_veinBlocksToMine.isEmpty()) {
                BlockPos nextBlock = _veinBlocksToMine.poll();
                _currentMiningTask = new DestroyBlockTask(nextBlock);
                return _currentMiningTask;
            }
        }
        
        // No ore veins found nearby, continue searching
        mod.log("No ore veins found nearby, continuing search...");
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.log("Auto mine ore task stopped.");
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof AutoMineOreTask task) {
            return task._miningRequirement == _miningRequirement && 
                   task._veinSearchRadius == _veinSearchRadius;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        if (_currentVeinStart != null) {
            return "Mining ore vein started at " + _currentVeinStart.toShortString() + 
                   ", " + _veinBlocksToMine.size() + " blocks left";
        } else {
            return "Searching for ore veins to mine...";
        }
    }
    
    @Override
    public boolean isFinished(AltoClef mod) {
        // Auto mine ore runs indefinitely until stopped
        return false;
    }
    
    /**
     * Find a nearby ore vein (a block that's part of an ore deposit).
     */
    private BlockPos findNearbyOreVein(AltoClef mod) {
        // Search in a cube around the player
        BlockPos playerPos = mod.getPlayer().getBlockPos();
        int radius = _veinSearchRadius;
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    
                    if (isOreBlock(mod, checkPos) && isBlockValidToMine(mod, checkPos)) {
                        return checkPos;
                    }
                }
            }
        }
        
        return null; // No ore vein found
    }
    
    /**
     * Find all connected ore blocks starting from the given position (flood fill algorithm).
     */
    private List<BlockPos> findConnectedOreVein(AltoClef mod, BlockPos start) {
        List<BlockPos> veinBlocks = new ArrayList<>();
        Queue<BlockPos> toCheck = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        
        toCheck.add(start);
        visited.add(start.toImmutable());
        
        while (!toCheck.isEmpty()) {
            BlockPos current = toCheck.poll();
            
            // Add to vein if it's a valid ore block
            if (isOreBlock(mod, current) && isBlockValidToMine(mod, current)) {
                veinBlocks.add(current);
                
                // Check all 6 adjacent blocks
                for (Direction direction : Direction.values()) {
                    BlockPos adjacent = current.offset(direction);
                    
                    if (!visited.contains(adjacent) && isOreBlock(mod, adjacent)) {
                        visited.add(adjacent.toImmutable());
                        toCheck.add(adjacent);
                    }
                }
            }
        }
        
        return veinBlocks;
    }
    
    /**
     * Check if a block position contains an ore block.
     */
    private boolean isOreBlock(AltoClef mod, BlockPos pos) {
        if (mod.getWorld() == null) {
            return false;
        }
        
        Block block = mod.getWorld().getBlockState(pos).getBlock();
        for (Block oreBlock : ORE_BLOCKS) {
            if (block == oreBlock) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a block is valid to mine (exists, not air, not already mined, etc.).
     */
    private boolean isBlockValidToMine(AltoClef mod, BlockPos pos) {
        if (mod.getWorld() == null) {
            return false;
        }
        
        // Check if it's still an ore block
        if (!isOreBlock(mod, pos)) {
            return false;
        }
        
        // Check if it's air (already mined)
        if (WorldHelper.isAir(mod, pos)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if a block has been mined (is air or was marked as mined).
     */
    private boolean isBlockMined(AltoClef mod, BlockPos pos) {
        // If it's air, it's already mined
        if (WorldHelper.isAir(mod, pos)) {
            return true;
        }
        
        // Check our internal tracking
        return _minedBlocks.contains(pos);
    }
}