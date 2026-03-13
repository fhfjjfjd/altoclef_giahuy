package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.BuildHistory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * Task to undo the last build operation by reverting blocks to their original state.
 */
public class UndoBuildTask extends Task {
    
    private final int _buildIndex; // Which build to undo (0 = most recent, 1 = second most recent, etc.)
    private List<BlockPos> _blocksToProcess;
    private int _currentBlockIndex = 0;
    private boolean _initialized = false;
    
    public UndoBuildTask() {
        this(0); // Undo most recent build by default
    }
    
    public UndoBuildTask(int buildIndex) {
        _buildIndex = buildIndex;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.log("Preparing to undo build #" + _buildIndex);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (!_initialized) {
            // Get the build to undo
            BuildHistory.BuildOperation buildOp = mod.getBuildHistory().getBuild(_buildIndex);
            
            if (buildOp == null) {
                mod.log("No build found at index " + _buildIndex + " to undo.");
                return null; // Nothing to undo
            }
            
            mod.log("Undoing build: " + buildOp._schematicName + " (" + buildOp.getBlockCount() + " blocks)");
            
            // Prepare list of blocks to revert
            _blocksToProcess = new ArrayList<>(buildOp._builtBlocks.keySet());
            _initialized = true;
        }
        
        // If we've processed all blocks, we're done
        if (_currentBlockIndex >= _blocksToProcess.size()) {
            mod.log("Undo operation completed.");
            return null;
        }
        
        // Get the current block to process
        BlockPos currentPos = _blocksToProcess.get(_currentBlockIndex);
        BuildHistory.BuildOperation buildOp = mod.getBuildHistory().getBuild(_buildIndex);
        
        if (buildOp == null) {
            mod.log("Build operation disappeared during undo process.");
            return null;
        }
        
        BlockState originalState = buildOp._originalBlocks.get(currentPos);
        BlockState currentState = mod.getWorld().getBlockState(currentPos);
        BlockState targetState = (originalState != null) ? originalState : mod.getWorld().getRegistryManager().getCombinedDynamicRegistries().get(0).get(net.minecraft.util.registry.Registry.BLOCK_KEY).getEntry(0).map(e -> e.value().getDefaultState()).orElse(net.minecraft.block.Blocks.AIR.getDefaultState());
        
        // If the current block doesn't match what was built, skip it
        BlockState builtState = buildOp._builtBlocks.get(currentPos);
        if (!currentState.getBlock().equals(builtState.getBlock())) {
            // The block has been changed since the build, skip this position
            _currentBlockIndex++;
            return null;
        }
        
        // If the original state was air, we need to destroy the block
        if (originalState == null || originalState.isAir()) {
            return new DestroyBlockTask(currentPos);
        } else {
            // Otherwise, place the original block back
            // Check if we have the required item
            Block originalBlock = originalState.getBlock();
            if (mod.getInventoryTracker().hasItem(originalBlock.asItem())) {
                return new PlaceBlockTask(currentPos, originalBlock);
            } else {
                // Collect the required block first
                mod.log("Need to collect " + originalBlock.getName().getString() + " to restore at " + currentPos.toShortString());
                return TaskCatalogue.getItemTask(originalBlock.asItem(), 1);
            }
        }
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.log("Undo build task interrupted at block " + _currentBlockIndex + "/" + 
                (_blocksToProcess != null ? _blocksToProcess.size() : 0));
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof UndoBuildTask task) {
            return task._buildIndex == _buildIndex;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        if (!_initialized) {
            return "Preparing to undo build #" + _buildIndex;
        } else {
            return "Undoing build, processing block " + _currentBlockIndex + "/" + 
                   (_blocksToProcess != null ? _blocksToProcess.size() : 0);
        }
    }
    
    @Override
    public boolean isFinished(AltoClef mod) {
        return _initialized && _currentBlockIndex >= _blocksToProcess.size();
    }
}